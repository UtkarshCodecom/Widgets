package com.desire.widget.ui.customize;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.desire.widget.R;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.model.WidgetSpec;
import com.desire.widget.engine.model.WidgetStyle;
import com.desire.widget.engine.runtime.EditorSchema;
import com.desire.widget.ui.widgets.WidgetInstaller;
import com.desire.widget.util.ThemeManager;
import com.desire.widget.widget.WidgetSchema;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The page between the gallery and the editor (the Muviz flow): a big live preview floating over
 * the user's own wallpaper, "Add to home screen", and a grid of looks for this widget — the
 * original design, its built-in styles and the user's own saved looks. ✎ / ＋ open the editor;
 * the editor's "ADD NEW" saves a look back here.
 */
public class WidgetDetailActivity extends AppCompatActivity {
    private static final Gson GSON = new Gson();

    private String specJson, name, size, widgetId;
    private int[] ratio;
    private final List<Look> looks = new ArrayList<>();
    private int selected = 0;

    private StyledPreview bigPreview;
    private LookAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Set<LookHolder> visible = new HashSet<>();

    /** One choice in the grid. {@code variantId} is set for the user's saved looks. */
    private static final class Look {
        final String name;
        final String styleJson;
        final String variantId;

        Look(String name, String styleJson, String variantId) {
            this.name = name;
            this.styleJson = styleJson;
            this.variantId = variantId;
        }
    }

    private final ActivityResultLauncher<Intent> editor = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                String id = result.getData() != null
                        ? result.getData().getStringExtra(CustomizeWidgetActivity.EXTRA_VARIANT_ID) : null;
                loadLooks(id);
            });

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            bigPreview.tick(now);
            for (LookHolder h : visible) h.preview.tick(now);
            handler.postDelayed(this, 1000 - now % 1000 + 15);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeManager.apply(this);
        super.onCreate(savedInstanceState);
        showWallpaperBehind(this);
        setContentView(R.layout.activity_widget_detail);

        specJson = getIntent().getStringExtra(CustomizeWidgetActivity.EXTRA_SPEC_JSON);
        name = getIntent().getStringExtra(CustomizeWidgetActivity.EXTRA_NAME);
        size = WidgetSchema.normalizeSize(getIntent().getStringExtra(CustomizeWidgetActivity.EXTRA_SIZE));
        widgetId = getIntent().getStringExtra(CustomizeWidgetActivity.EXTRA_WIDGET_ID);
        if (specJson == null || specJson.trim().isEmpty()) {
            finish();
            return;
        }
        ratio = WidgetSchema.previewRatio(size);

        ((TextView) findViewById(R.id.detail_name)).setText(
                (name != null ? name : "Widget") + "  ·  " + size.replace('x', '×'));
        bigPreview = new StyledPreview(findViewById(R.id.detail_preview), 900);

        findViewById(R.id.detail_back).setOnClickListener(v -> finish());
        findViewById(R.id.detail_add_home).setOnClickListener(v -> addToHome());
        findViewById(R.id.detail_edit).setOnClickListener(v -> openEditor(current(), false));
        findViewById(R.id.detail_new).setOnClickListener(v -> openEditor(current(), true));
        applyInsets();

        RecyclerView grid = findViewById(R.id.detail_variants);
        boolean wide = WidgetSchema.isWide(size);
        grid.setLayoutManager(new GridLayoutManager(this, wide ? 1 : 2));
        grid.setItemAnimator(null);
        adapter = new LookAdapter();
        grid.setAdapter(adapter);

        loadLooks(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(ticker);
        super.onPause();
    }

    /** Lets the user's wallpaper show through, edge to edge, like the home screen itself. */
    static void showWallpaperBehind(AppCompatActivity a) {
        a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        a.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowCompat.setDecorFitsSystemWindows(a.getWindow(), false);
        a.getWindow().setStatusBarColor(Color.TRANSPARENT);
        a.getWindow().setNavigationBarColor(a.getColor(R.color.dark_background));
    }

    private void applyInsets() {
        View back = findViewById(R.id.detail_back);
        View grid = findViewById(R.id.detail_variants);
        View fab = findViewById(R.id.detail_new);
        int gridBottom = grid.getPaddingBottom();
        ViewGroup.MarginLayoutParams fabLp = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
        int fabBottom = fabLp.bottomMargin;
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detail_root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ((ViewGroup.MarginLayoutParams) back.getLayoutParams()).topMargin = bars.top + dp(4);
            back.requestLayout();
            grid.setPadding(grid.getPaddingLeft(), grid.getPaddingTop(), grid.getPaddingRight(), gridBottom + bars.bottom);
            fabLp.bottomMargin = fabBottom + bars.bottom;
            fab.requestLayout();
            return insets;
        });
    }

    // ------------------------------------------------------------------ looks

    private void loadLooks(@Nullable String selectVariantId) {
        looks.clear();
        looks.add(new Look("Original", null, null));
        for (VariantStore.Variant v : VariantStore.list(this, widgetId)) {
            looks.add(new Look(v.name, v.styleJson, v.id));
        }
        WidgetSpec spec = parse();
        EditorSchema.StyleAxis axis = EditorSchema.styleAxis(spec, specJson);
        if (axis != null) {
            for (int i = 0; i < axis.options.size(); i++) {
                EditorSchema.Option o = axis.options.get(i);
                if (sameAsDesign(spec, axis, i)) continue; // already shown as "Original"
                WidgetStyle st = new WidgetStyle();
                st.props = new LinkedHashMap<>(o.props);
                st.styleOption = i;
                looks.add(new Look(o.name, GSON.toJson(st), null));
            }
        }

        selected = 0;
        if (selectVariantId != null) {
            for (int i = 0; i < looks.size(); i++) {
                if (selectVariantId.equals(looks.get(i).variantId)) selected = i;
            }
        }
        adapter.notifyDataSetChanged();
        showSelected();
    }

    /**
     * True when a built-in style option already matches the design's own props (a prop the design
     * leaves out counts as the axis's first option, which is each renderer's default).
     */
    private boolean sameAsDesign(WidgetSpec spec, EditorSchema.StyleAxis axis, int index) {
        if (spec == null || spec.components == null) return false;
        for (Map.Entry<String, Object> e : axis.options.get(index).props.entrySet()) {
            String key = e.getKey();
            int dot = key.indexOf('.');
            int ci;
            try {
                ci = Integer.parseInt(key.substring(0, dot));
            } catch (Exception ex) {
                return false;
            }
            if (ci < 0 || ci >= spec.components.size()) return false;
            ComponentSpec c = spec.components.get(ci);
            Object v = c != null && c.props != null ? c.props.get(key.substring(dot + 1)) : null;
            Object want = e.getValue();
            if (v == null) {
                if (index != 0) return false; // missing prop = renderer default = first option
            } else if (!String.valueOf(v).equals(String.valueOf(want))) {
                return false;
            }
        }
        return true;
    }

    private Look current() {
        return looks.isEmpty() ? new Look("Original", null, null) : looks.get(Math.min(selected, looks.size() - 1));
    }

    private void showSelected() {
        bigPreview.bind(specJson, current().styleJson, ratio);
    }

    private void select(int index) {
        if (index == selected) {
            openEditor(current(), false); // tapping the chosen look again edits it
            return;
        }
        int old = selected;
        selected = index;
        adapter.notifyItemChanged(old);
        adapter.notifyItemChanged(index);
        showSelected();
    }

    private void openEditor(Look from, boolean asNew) {
        Intent i = new Intent(this, CustomizeWidgetActivity.class);
        i.putExtra(CustomizeWidgetActivity.EXTRA_SPEC_JSON, specJson);
        i.putExtra(CustomizeWidgetActivity.EXTRA_NAME, name);
        i.putExtra(CustomizeWidgetActivity.EXTRA_SIZE, size);
        i.putExtra(CustomizeWidgetActivity.EXTRA_WIDGET_ID, widgetId);
        if (from.styleJson != null) i.putExtra(CustomizeWidgetActivity.EXTRA_STYLE_JSON, from.styleJson);
        if (!asNew && from.variantId != null) i.putExtra(CustomizeWidgetActivity.EXTRA_VARIANT_ID, from.variantId);
        editor.launch(i);
    }

    private void addToHome() {
        String id = widgetId != null ? widgetId : "custom_" + System.currentTimeMillis();
        WidgetInstaller.installSpecToHome(findViewById(R.id.detail_root), id, name, specJson, size,
                current().styleJson);
    }

    private void showLookMenu(int index) {
        Look look = looks.get(index);
        if (look.variantId == null) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(look.name)
                .setItems(new String[]{"Edit", "Rename", "Delete"}, (d, which) -> {
                    if (which == 0) {
                        openEditor(look, false);
                    } else if (which == 1) {
                        EditText input = new EditText(this);
                        input.setText(look.name);
                        input.setSelectAllOnFocus(true);
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("Rename")
                                .setView(input)
                                .setPositiveButton("Save", (dd, w) -> {
                                    String n = input.getText().toString().trim();
                                    if (!n.isEmpty()) VariantStore.rename(this, widgetId, look.variantId, n);
                                    loadLooks(look.variantId);
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    } else {
                        VariantStore.delete(this, widgetId, look.variantId);
                        loadLooks(null);
                        Toast.makeText(this, "Look deleted", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private WidgetSpec parse() {
        try {
            return GSON.fromJson(specJson, WidgetSpec.class);
        } catch (Exception e) {
            return null;
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // ------------------------------------------------------------------ grid

    private final class LookHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final ImageView image;
        final TextView label;
        final StyledPreview preview;

        LookHolder(View v) {
            super(v);
            card = v.findViewById(R.id.variant_card);
            image = v.findViewById(R.id.variant_preview);
            label = v.findViewById(R.id.variant_name);
            preview = new StyledPreview(image, 420);
        }
    }

    private final class LookAdapter extends RecyclerView.Adapter<LookHolder> {
        @NonNull
        @Override
        public LookHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_variant, parent, false);
            LookHolder h = new LookHolder(v);
            // Card preview keeps the widget's aspect: wide widgets get a shorter box.
            ViewGroup.LayoutParams lp = h.image.getLayoutParams();
            lp.height = dp(ratio[0] == ratio[1] ? 140 : ratio[0] >= 4 * ratio[1] ? 92 : 150);
            h.image.setLayoutParams(lp);
            return h;
        }

        @Override
        public void onBindViewHolder(@NonNull LookHolder h, int position) {
            Look look = looks.get(position);
            boolean sel = position == selected;
            h.label.setText(look.name);
            h.label.setTextColor(getColor(sel ? R.color.text_primary : R.color.text_secondary));
            int primary = themePrimary();
            h.card.setStrokeColor(ColorStateList.valueOf(sel ? primary : getColor(R.color.glass_stroke)));
            h.card.setStrokeWidth(dp(sel ? 2 : 1));
            h.preview.bind(specJson, look.styleJson, ratio);
            h.itemView.setOnClickListener(v -> select(h.getBindingAdapterPosition()));
            h.itemView.setOnLongClickListener(v -> {
                showLookMenu(h.getBindingAdapterPosition());
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return looks.size();
        }

        @Override
        public void onViewAttachedToWindow(@NonNull LookHolder h) {
            visible.add(h);
        }

        @Override
        public void onViewDetachedFromWindow(@NonNull LookHolder h) {
            visible.remove(h);
        }
    }

    private int themePrimary() {
        android.util.TypedValue tv = new android.util.TypedValue();
        return getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, tv, true) ? tv.data : 0xFF6C7BFF;
    }
}
