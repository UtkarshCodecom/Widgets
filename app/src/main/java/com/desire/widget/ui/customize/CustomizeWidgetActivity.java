package com.desire.widget.ui.customize;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.desire.widget.engine.util.FontResolver;

import com.desire.widget.R;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.WidgetEngine;
import com.desire.widget.engine.data.CachedLiveDataSource;
import com.desire.widget.engine.model.WidgetSpec;
import com.desire.widget.engine.model.WidgetStyle;
import com.desire.widget.engine.runtime.StyleApplier;
import com.desire.widget.engine.runtime.ThemeEngine;
import com.desire.widget.ui.studio.StudioActivity;
import com.desire.widget.ui.widgets.WidgetInstaller;
import com.desire.widget.util.AppExecutors;
import com.desire.widget.util.ThemeManager;
import com.desire.widget.widget.WidgetSchema;
import com.google.android.material.slider.Slider;
import com.google.gson.Gson;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Muviz-style visual customizer: pick a design → tweak accent/text colors, background style,
 * corner radius, and opacity with a live native preview → Add to Home. The preview is produced by
 * the same {@link WidgetEngine} that renders the installed widget, so it is pixel-identical.
 * The chosen {@link WidgetStyle} is stored per placement and applied at render time by
 * {@link StyleApplier} — the design's component JSON is never modified.
 */
public class CustomizeWidgetActivity extends AppCompatActivity {
    public static final String EXTRA_SPEC_JSON = "spec_json";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_SIZE = "size";
    public static final String EXTRA_WIDGET_ID = "widget_id";
    /** When set, Customize edits an already-placed widget in place ("Apply") instead of pinning. */
    public static final String EXTRA_APPWIDGET_ID = "app_widget_id";

    private static final WidgetEngine ENGINE = new WidgetEngine();
    private static final Gson GSON = new Gson();

    private static final int[] PALETTE = {
            0xFFFFFFFF, 0xFF000000, 0xFFFFD700, 0xFFFF6B9D, 0xFF00D4FF, 0xFF00E676,
            0xFFFF5252, 0xFFFF8C00, 0xFFAB47BC, 0xFF448AFF, 0xFF1DE9B6, 0xFFB0BEC5
    };

    private final AtomicInteger renderToken = new AtomicInteger();
    private final WidgetStyle style = new WidgetStyle();

    private ImageView preview;
    private View bgColorSection;
    private View gradientSection;
    private final TextView[] modeChips = new TextView[3];

    private String specJson;
    private String widgetName = "Widget";
    private String size = "2x2";
    private String widgetId;
    private int appWidgetId = -1;

    private final ActivityResultLauncher<String[]> photoPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try {
                    getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {}
                style.photoUri = uri.toString();
                render();
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeManager.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize_widget);

        specJson = getIntent().getStringExtra(EXTRA_SPEC_JSON);
        if (getIntent().getStringExtra(EXTRA_NAME) != null) widgetName = getIntent().getStringExtra(EXTRA_NAME);
        size = WidgetSchema.normalizeSize(getIntent().getStringExtra(EXTRA_SIZE));
        widgetId = getIntent().getStringExtra(EXTRA_WIDGET_ID);
        appWidgetId = getIntent().getIntExtra(EXTRA_APPWIDGET_ID, -1);

        if (specJson == null || specJson.trim().isEmpty()) {
            finish();
            return;
        }

        // Reconfigure mode: preload the widget's saved style so the preview matches the placed one.
        if (appWidgetId != -1) {
            String savedStyle = com.desire.widget.engine.runtime.EngineWidgetStore.getStyleJson(this, appWidgetId);
            if (savedStyle != null && !savedStyle.isEmpty()) {
                try {
                    WidgetStyle s = GSON.fromJson(savedStyle, WidgetStyle.class);
                    if (s != null) {
                        style.accentColor = s.accentColor;
                        style.textColor = s.textColor;
                        style.backgroundColor = s.backgroundColor;
                        style.gradientEndColor = s.gradientEndColor;
                        style.backgroundMode = s.backgroundMode != null ? s.backgroundMode : "solid";
                        style.cornerRadius = s.cornerRadius;
                        style.backgroundOpacity = s.backgroundOpacity;
                        style.fontFamily = s.fontFamily;
                        style.photoUri = s.photoUri;
                    }
                } catch (Exception ignored) {}
            }
        }

        preview = findViewById(R.id.cw_preview);
        bgColorSection = findViewById(R.id.cw_bg_color_section);
        gradientSection = findViewById(R.id.cw_gradient_section);

        style.backgroundMode = "solid";
        style.cornerRadius = 0.14f;
        style.backgroundOpacity = 1f;

        buildSwatchRow(findViewById(R.id.cw_accent_swatches), c -> { style.accentColor = hex(c); render(); });
        buildSwatchRow(findViewById(R.id.cw_text_swatches), c -> { style.textColor = hex(c); render(); });
        buildSwatchRow(findViewById(R.id.cw_bg_swatches), c -> { style.backgroundColor = hex(c); render(); });
        buildSwatchRow(findViewById(R.id.cw_gradient_swatches), c -> { style.gradientEndColor = hex(c); render(); });

        buildModeChips();
        buildFontChips();
        setupSliders();
        setupActions();

        if (appWidgetId != -1) {
            ((TextView) findViewById(R.id.cw_add_home)).setText("Apply");
        }

        preview.post(this::render);
    }

    private void setupSliders() {
        Slider corner = findViewById(R.id.cw_corner_slider);
        corner.addOnChangeListener((s, v, fromUser) -> { style.cornerRadius = v; render(); });
        Slider opacity = findViewById(R.id.cw_opacity_slider);
        opacity.addOnChangeListener((s, v, fromUser) -> { style.backgroundOpacity = v; render(); });
    }

    private void setupActions() {
        findViewById(R.id.cw_add_home).setOnClickListener(v -> addToHome());
        findViewById(R.id.cw_reset).setOnClickListener(v -> resetStyle());
        findViewById(R.id.cw_advanced).setOnClickListener(v -> openAdvanced());
        findViewById(R.id.cw_photo).setOnClickListener(v -> {
            try {
                photoPicker.launch(new String[]{"image/*"});
            } catch (Exception e) {
                android.widget.Toast.makeText(this, "No photo picker available", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void buildFontChips() {
        LinearLayout row = findViewById(R.id.cw_font_row);
        for (String family : FontResolver.FAMILIES) {
            TextView chip = new TextView(this);
            chip.setText(family);
            chip.setTypeface(FontResolver.resolve(family, false));
            chip.setTextColor(0xFFFFFFFF);
            chip.setPadding(dp(14), dp(9), dp(14), dp(9));
            chip.setBackgroundResource(R.drawable.bg_widget_tile);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp(8), 0);
            chip.setLayoutParams(lp);
            chip.setAlpha(0.55f);
            chip.setOnClickListener(v -> {
                for (int i = 0; i < row.getChildCount(); i++) row.getChildAt(i).setAlpha(0.55f);
                chip.setAlpha(1f);
                style.fontFamily = "default".equals(family) ? null : family;
                render();
            });
            row.addView(chip);
        }
    }

    private void buildModeChips() {
        LinearLayout row = findViewById(R.id.cw_bg_mode_row);
        String[] modes = {"solid", "gradient", "transparent"};
        String[] labels = {"Solid", "Gradient", "Transparent"};
        for (int i = 0; i < modes.length; i++) {
            final String mode = modes[i];
            TextView chip = new TextView(this);
            chip.setText(labels[i]);
            chip.setTextColor(0xFFFFFFFF);
            chip.setPadding(dp(16), dp(9), dp(16), dp(9));
            chip.setBackgroundResource(R.drawable.bg_widget_tile);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp(8), 0);
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> selectMode(mode));
            modeChips[i] = chip;
            row.addView(chip);
        }
        selectMode("solid");
    }

    private void selectMode(String mode) {
        style.backgroundMode = mode;
        for (TextView chip : modeChips) {
            boolean sel = chip.getText().toString().equalsIgnoreCase(mode);
            chip.setAlpha(sel ? 1f : 0.5f);
        }
        bgColorSection.setVisibility("transparent".equals(mode) ? View.GONE : View.VISIBLE);
        gradientSection.setVisibility("gradient".equals(mode) ? View.VISIBLE : View.GONE);
        render();
    }

    private void buildSwatchRow(LinearLayout container, OnColor onColor) {
        final View[] selected = {null};
        for (int color : PALETTE) {
            View swatch = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(38), dp(38));
            lp.setMargins(0, 0, dp(10), 0);
            swatch.setLayoutParams(lp);
            swatch.setBackground(swatchDrawable(color, false));
            swatch.setOnClickListener(v -> {
                if (selected[0] != null) {
                    selected[0].setBackground(swatchDrawable((Integer) selected[0].getTag(), false));
                }
                selected[0] = v;
                v.setBackground(swatchDrawable(color, true));
                onColor.pick(color);
            });
            swatch.setTag(color);
            container.addView(swatch);
        }
    }

    private GradientDrawable swatchDrawable(int color, boolean selected) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        int ring = selected ? 0xFFFFFFFF : 0x33FFFFFF;
        d.setStroke(dp(selected ? 3 : 1), ring);
        return d;
    }

    private void render() {
        WidgetSpec spec;
        try {
            spec = GSON.fromJson(specJson, WidgetSpec.class); // fresh copy each render
        } catch (Exception e) {
            return;
        }
        if (spec == null) return;

        int[] ratio = WidgetSchema.previewRatio(size);
        final int w = 720;
        final int h = Math.max(1, w * ratio[1] / ratio[0]);
        final int token = renderToken.incrementAndGet();
        final WidgetStyle styleCopy = GSON.fromJson(GSON.toJson(style), WidgetStyle.class);
        final WidgetSpec specForRender = spec;
        final android.content.Context appCtx = getApplicationContext();

        AppExecutors.getInstance().diskIO().execute(() -> {
            Bitmap bmp;
            try {
                com.desire.widget.engine.RenderTheme theme =
                        StyleApplier.apply(specForRender, styleCopy, ThemeEngine.current(appCtx));
                RenderContext rc = new RenderContext(appCtx, w, h, theme,
                        System.currentTimeMillis(), new CachedLiveDataSource(appCtx));
                bmp = ENGINE.render(specForRender, rc);
            } catch (Exception e) {
                bmp = null;
            }
            final Bitmap result = bmp;
            preview.post(() -> {
                if (result != null && token == renderToken.get()) preview.setImageBitmap(result);
            });
        });
    }

    private void addToHome() {
        String styleJson = style.isEmpty() ? null : GSON.toJson(style);
        if (appWidgetId != -1) {
            // Reconfigure: update the placed widget in place and re-render.
            String id = widgetId != null ? widgetId : "custom_" + System.currentTimeMillis();
            com.desire.widget.engine.runtime.EngineWidgetStore.saveWidget(
                    this, appWidgetId, id, widgetName, specJson, size, styleJson);
            com.desire.widget.engine.runtime.EngineRenderer.render(this, new int[]{appWidgetId});
            android.widget.Toast.makeText(this, "Widget updated", android.widget.Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String id = widgetId != null ? widgetId : "custom_" + System.currentTimeMillis();
        WidgetInstaller.installSpecToHome(findViewById(R.id.cw_root), id, widgetName, specJson, size, styleJson);
    }

    private void resetStyle() {
        style.accentColor = null;
        style.textColor = null;
        style.backgroundColor = null;
        style.gradientEndColor = null;
        style.backgroundMode = "solid";
        style.cornerRadius = 0.14f;
        style.backgroundOpacity = 1f;
        recreate();
    }

    private void openAdvanced() {
        Intent intent = new Intent(this, StudioActivity.class);
        intent.putExtra(StudioActivity.EXTRA_SPEC_JSON, specJson);
        intent.putExtra(StudioActivity.EXTRA_NAME, widgetName);
        intent.putExtra(StudioActivity.EXTRA_SIZE, size);
        intent.putExtra(StudioActivity.EXTRA_WIDGET_ID, widgetId);
        startActivity(intent);
    }

    private String hex(int color) {
        return String.format("#%08X", color);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private interface OnColor {
        void pick(int color);
    }
}
