package com.desire.widget.ui.studio;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.desire.widget.R;
import com.desire.widget.data.local.entity.WidgetEntity;
import com.desire.widget.data.repository.WidgetRepository;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.WidgetEngine;
import com.desire.widget.engine.data.CachedLiveDataSource;
import com.desire.widget.engine.model.WidgetSpec;
import com.desire.widget.engine.runtime.ThemeEngine;
import com.desire.widget.ui.widgets.WidgetInstaller;
import com.desire.widget.util.AppExecutors;
import com.desire.widget.util.ThemeManager;
import com.desire.widget.widget.WidgetSchema;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Widget Studio: a JSON editor with an instant native preview. The preview is rendered by the
 * very same {@link WidgetEngine} that draws installed widgets, so what you see is exactly what gets
 * pinned to the home screen. A component palette appends ready-made components; the JSON stays the
 * single source of truth and can be hand-edited.
 */
public class StudioActivity extends AppCompatActivity {
    public static final String EXTRA_SPEC_JSON = "spec_json";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_SIZE = "size";
    public static final String EXTRA_WIDGET_ID = "widget_id";

    private static final String[] SIZES = {"1x1", "2x2", "2x1", "4x1", "4x2"};
    private static final WidgetEngine ENGINE = new WidgetEngine();
    private static final Gson PLAIN = new Gson();
    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().create();

    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicInteger renderToken = new AtomicInteger();

    private EditText json;
    private ImageView preview;
    private TextView error;
    private Spinner sizeSpinner;
    private LinearLayout palette;

    private String currentSize = "2x2";
    private String widgetId;
    private String widgetName = "My Widget";
    private Runnable pendingRender;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeManager.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_studio);

        json = findViewById(R.id.studio_json);
        preview = findViewById(R.id.studio_preview);
        error = findViewById(R.id.studio_error);
        sizeSpinner = findViewById(R.id.studio_size_spinner);
        palette = findViewById(R.id.studio_palette);

        currentSize = WidgetSchema.normalizeSize(getIntent().getStringExtra(EXTRA_SIZE));
        widgetId = getIntent().getStringExtra(EXTRA_WIDGET_ID);
        if (getIntent().getStringExtra(EXTRA_NAME) != null) widgetName = getIntent().getStringExtra(EXTRA_NAME);

        setupSizeSpinner();
        setupPalette();
        setupActions();

        String startSpec = getIntent().getStringExtra(EXTRA_SPEC_JSON);
        if (startSpec != null && !startSpec.trim().isEmpty()) {
            json.setText(pretty(startSpec));
        } else {
            json.setText(PRETTY.toJson(StudioDefaults.blank(currentSize)));
        }

        json.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { scheduleRender(); }
        });

        preview.post(this::renderPreview);
    }

    private void setupSizeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, SIZES);
        sizeSpinner.setAdapter(adapter);
        for (int i = 0; i < SIZES.length; i++) {
            if (SIZES[i].equals(currentSize)) { sizeSpinner.setSelection(i); break; }
        }
        sizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                currentSize = SIZES[pos];
                renderPreview();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupPalette() {
        int pad = dp(10);
        for (String type : StudioDefaults.PALETTE) {
            TextView chip = new TextView(this);
            chip.setText(type);
            chip.setTextColor(0xFFFFFFFF);
            chip.setTextSize(13f);
            chip.setPadding(dp(14), pad, dp(14), pad);
            chip.setBackgroundResource(R.drawable.bg_widget_tile);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp(8), 0);
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> addComponent(type));
            palette.addView(chip);
        }
    }

    private void setupActions() {
        findViewById(R.id.studio_apply).setOnClickListener(v -> {
            if (renderPreview()) toast("Applied");
        });
        findViewById(R.id.studio_format).setOnClickListener(v -> {
            WidgetSpec spec = parse();
            if (spec != null) json.setText(PRETTY.toJson(spec));
            else toast("Cannot format invalid JSON");
        });
        findViewById(R.id.studio_reset).setOnClickListener(v ->
                json.setText(PRETTY.toJson(StudioDefaults.blank(currentSize))));
        findViewById(R.id.studio_add_home).setOnClickListener(v -> addToHome());
        View saveBtn = findViewById(R.id.studio_save);
        saveBtn.setOnClickListener(v -> save());
        // Admin: long-press Save to publish the spec to Firebase (developer mode only).
        saveBtn.setOnLongClickListener(v -> {
            if (com.desire.widget.util.PreferenceManager.getInstance(this).isDeveloperModeUnlocked()) {
                publish();
                return true;
            }
            return false;
        });
    }

    private void publish() {
        WidgetSpec spec = parse();
        if (spec == null) { toast("Fix the JSON first"); return; }
        String id = spec.id != null ? spec.id : "studio_" + System.currentTimeMillis();
        String name = spec.name != null ? spec.name : widgetName;
        toast("Publishing…");
        com.desire.widget.data.remote.AdminPublisher.publishSpec(
                id, name, "Published from Studio", "mine", "My Widgets", currentSize,
                json.getText().toString(), false, false, null,
                (success, message) -> toast(message));
    }

    private void addComponent(String type) {
        WidgetSpec spec = parse();
        if (spec == null) { toast("Fix the JSON first"); return; }
        spec.components.add(StudioDefaults.component(type));
        json.setText(PRETTY.toJson(spec)); // triggers re-render via watcher
    }

    private void scheduleRender() {
        if (pendingRender != null) main.removeCallbacks(pendingRender);
        pendingRender = this::renderPreview;
        main.postDelayed(pendingRender, 300);
    }

    /** @return true if the JSON parsed and a render was kicked off. */
    private boolean renderPreview() {
        WidgetSpec spec = parse();
        if (spec == null) {
            error.setVisibility(View.VISIBLE);
            return false;
        }
        error.setVisibility(View.GONE);

        int[] ratio = WidgetSchema.previewRatio(currentSize);
        final int w = 720;
        final int h = Math.max(1, w * ratio[1] / ratio[0]);
        final int token = renderToken.incrementAndGet();
        final android.content.Context appCtx = getApplicationContext();

        AppExecutors.getInstance().diskIO().execute(() -> {
            Bitmap bmp;
            try {
                RenderContext rc = new RenderContext(appCtx, w, h, ThemeEngine.current(appCtx),
                        System.currentTimeMillis(), new CachedLiveDataSource(appCtx));
                bmp = ENGINE.render(spec, rc);
            } catch (Exception e) {
                bmp = null;
            }
            final Bitmap result = bmp;
            main.post(() -> {
                if (result != null && token == renderToken.get()) preview.setImageBitmap(result);
            });
        });
        return true;
    }

    private WidgetSpec parse() {
        try {
            WidgetSpec spec = PLAIN.fromJson(json.getText().toString(), WidgetSpec.class);
            return (spec != null && spec.components != null) ? spec : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void addToHome() {
        WidgetSpec spec = parse();
        if (spec == null) { toast("Fix the JSON first"); return; }
        String id = spec.id != null ? spec.id : "studio_" + System.currentTimeMillis();
        String name = spec.name != null ? spec.name : widgetName;
        WidgetInstaller.installSpecToHome(findViewById(R.id.studio_root), id, name,
                json.getText().toString(), currentSize);
    }

    private void save() {
        WidgetSpec spec = parse();
        if (spec == null) { toast("Fix the JSON first"); return; }
        String specJson = json.getText().toString();
        String id = widgetId != null ? widgetId : (spec.id != null ? spec.id : "studio_" + System.currentTimeMillis());
        String name = spec.name != null ? spec.name : widgetName;

        WidgetEntity e = new WidgetEntity();
        e.setId(id);
        e.setName(name);
        e.setDescription("Created in Studio");
        e.setCategoryId("mine");
        e.setCategoryName("My Widgets");
        e.setSpecJson(specJson);
        e.setWidgetSize(currentSize);
        e.setConfigJson("{}");
        e.setUpdatedAt(System.currentTimeMillis());
        e.setVersion(1);
        e.setActive(true);
        WidgetRepository.getInstance(this).saveUserWidget(e);
        toast("Saved to your widgets");
    }

    private String pretty(String rawJson) {
        try {
            Object o = PLAIN.fromJson(rawJson, Object.class);
            return PRETTY.toJson(o);
        } catch (Exception e) {
            return rawJson;
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
