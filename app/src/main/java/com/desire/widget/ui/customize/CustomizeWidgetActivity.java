package com.desire.widget.ui.customize;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.desire.widget.R;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.WidgetEngine;
import com.desire.widget.engine.data.CachedLiveDataSource;
import com.desire.widget.engine.model.WidgetSpec;
import com.desire.widget.engine.model.WidgetStyle;
import com.desire.widget.engine.runtime.StyleApplier;
import com.desire.widget.engine.runtime.ThemeEngine;
import com.desire.widget.ui.widgets.WidgetInstaller;
import com.desire.widget.util.AppExecutors;
import com.desire.widget.util.ThemeManager;
import com.desire.widget.widget.WidgetSchema;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Muviz-style widget customizer: a large live preview above a tabbed editor
 * (Style · City · Color 1/2/3 · Size · Alignment · Transparency · Photo). Every change updates the
 * preview through the same {@link WidgetEngine} that renders the installed widget, and the chosen
 * {@link WidgetStyle} is stored per placement and applied by {@link StyleApplier}.
 */
public class CustomizeWidgetActivity extends AppCompatActivity {
    public static final String EXTRA_SPEC_JSON = "spec_json";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_SIZE = "size";
    public static final String EXTRA_WIDGET_ID = "widget_id";
    public static final String EXTRA_APPWIDGET_ID = "app_widget_id";

    private static final WidgetEngine ENGINE = new WidgetEngine();
    private static final Gson GSON = new Gson();

    // A Muviz-like palette: neutrals then accents.
    private static final int[] PALETTE = {
            0xFF000000, 0xFF212121, 0xFF424242, 0xFF616161, 0xFF9E9E9E, 0xFFBDBDBD,
            0xFFE0E0E0, 0xFFFFFFFF, 0xFF1C2B3A, 0xFF2C3E50, 0xFF34495E, 0xFF5D6D7E,
            0xFFFFD700, 0xFFFF8C00, 0xFFFF5252, 0xFFFF6B9D, 0xFFAB47BC, 0xFF7C4DFF,
            0xFF448AFF, 0xFF00D4FF, 0xFF00E676, 0xFF1DE9B6, 0xFF00BFA5, 0xFFEEC900
    };

    private final AtomicInteger renderToken = new AtomicInteger();
    private final WidgetStyle style = new WidgetStyle();

    private ImageView preview;
    private LinearLayout tabsRow;
    private ViewGroup content;

    private String specJson;
    private String widgetName = "Widget";
    private String size = "2x2";
    private String widgetId;
    private int appWidgetId = -1;

    private WidgetSpec parsedSpec;
    private final List<String> tabs = new ArrayList<>();
    private int selectedTab = 0;
    /** Index into style.stickers of the sticker being edited/dragged, or -1. */
    private int selectedSticker = -1;
    /** While dragging, the sticker index excluded from the baked bitmap (shown as a ghost instead). */
    private int draggingSticker = -1;
    /** Lightweight overlay showing the dragged sticker so moving it is smooth (no per-frame render). */
    private ImageView stickerGhost;
    /** The dragged sticker's scale when the ghost appeared (base for the ghost's live scaleX/Y). */
    private float ghostBaseScale = 0.3f;
    /** Angle between two fingers on the previous move, for incremental rotation (NaN when not set). */
    private float lastTwoFingerAngle = Float.NaN;
    private ScaleGestureDetector scaleDetector;

    /** Adds picked photos to the widget's slideshow (one photo = static, more = they rotate). */
    private final ActivityResultLauncher<String[]> photoPicker =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                if (uris == null || uris.isEmpty()) return;
                if (style.photoUris == null) style.photoUris = new ArrayList<>();
                // Fold a legacy single photo into the list so nothing the user set is lost.
                if (style.photoUri != null && !style.photoUri.isEmpty() && !style.photoUris.contains(style.photoUri)) {
                    style.photoUris.add(0, style.photoUri);
                }
                style.photoUri = null;
                for (Uri uri : uris) {
                    try {
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception ignored) {}
                    if (style.photoUris.size() < 20 && !style.photoUris.contains(uri.toString())) {
                        style.photoUris.add(uri.toString());
                    }
                }
                render();
                selectTab(selectedTab);
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
        try {
            parsedSpec = GSON.fromJson(specJson, WidgetSpec.class);
        } catch (Exception e) {
            parsedSpec = null;
        }

        preloadStyleIfReconfigure();

        preview = findViewById(R.id.cw_preview);
        tabsRow = findViewById(R.id.cw_tabs);
        content = findViewById(R.id.cw_content);

        // Drag / pinch-scale / two-finger-rotate stickers directly on the preview (Stickers tab).
        scaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector d) {
                if (draggingSticker < 0 || style.stickers == null
                        || draggingSticker >= style.stickers.size()) return false;
                WidgetStyle.Sticker st = style.stickers.get(draggingSticker);
                st.scale = Math.max(0.1f, Math.min(1.5f, st.scale * d.getScaleFactor()));
                if (stickerGhost != null && ghostBaseScale > 0f) {
                    float s = st.scale / ghostBaseScale;
                    stickerGhost.setScaleX(s);
                    stickerGhost.setScaleY(s);
                }
                return true;
            }
        });
        preview.setOnTouchListener((v, event) -> handleStickerDrag(event));

        buildTabs();
        setupActions();

        if (appWidgetId != -1) {
            ((TextView) findViewById(R.id.cw_add_home)).setText("APPLY");
        }

        selectTab(0);
        preview.post(this::render);
    }

    private void preloadStyleIfReconfigure() {
        if (appWidgetId == -1) return;
        String saved = com.desire.widget.engine.runtime.EngineWidgetStore.getStyleJson(this, appWidgetId);
        if (saved == null || saved.isEmpty()) return;
        try {
            WidgetStyle s = GSON.fromJson(saved, WidgetStyle.class);
            if (s == null) return;
            style.accentColor = s.accentColor;
            style.textColor = s.textColor;
            style.backgroundColor = s.backgroundColor;
            style.gradientEndColor = s.gradientEndColor;
            style.backgroundMode = s.backgroundMode;
            style.cornerRadius = s.cornerRadius;
            style.backgroundOpacity = s.backgroundOpacity;
            style.fontFamily = s.fontFamily;
            style.photoUri = s.photoUri;
            style.photoUris = s.photoUris;
            style.photoIntervalMin = s.photoIntervalMin;
            style.contentScale = s.contentScale;
            style.alignX = s.alignX;
            style.alignY = s.alignY;
            style.styleVariant = s.styleVariant;
            style.cityName = s.cityName;
            style.stickers = s.stickers;
        } catch (Exception ignored) {}
    }

    // ---------------------------------------------------------------- Tabs

    private void buildTabs() {
        tabs.clear();
        tabs.add("Style");
        if (StyleApplier.hasWeather(parsedSpec)) tabs.add("City");
        tabs.add("Color 1");
        tabs.add("Color 2");
        tabs.add("Color 3");
        tabs.add("Background");
        tabs.add("Font");
        tabs.add("Size");
        tabs.add("Alignment");
        tabs.add("Transparency");
        tabs.add("Tap");
        tabs.add("Photo");
        tabs.add("Stickers");

        tabsRow.removeAllViews();
        for (int i = 0; i < tabs.size(); i++) {
            final int idx = i;
            TextView tab = new TextView(this);
            tab.setText(tabs.get(i));
            tab.setPadding(dp(14), dp(14), dp(14), dp(14));
            tab.setTextSize(14f);
            tab.setBackgroundResource(0);
            tab.setOnClickListener(v -> selectTab(idx));
            tabsRow.addView(tab);
        }
    }

    private void selectTab(int index) {
        selectedTab = index;
        for (int i = 0; i < tabsRow.getChildCount(); i++) {
            TextView tab = (TextView) tabsRow.getChildAt(i);
            boolean sel = i == index;
            tab.setTextColor(sel ? themeColor(androidx.appcompat.R.attr.colorPrimary, 0xFF00D4FF)
                    : withAlpha(onSurface(), 0.55f));
            tab.setTypeface(null, sel ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
        content.removeAllViews();
        content.addView(buildPanel(tabs.get(index)));
    }

    private View buildPanel(String name) {
        switch (name) {
            case "Style": return stylePanel();
            case "City": return cityPanel();
            case "Color 1": return colorPanel(1);
            case "Color 2": return colorPanel(2);
            case "Color 3": return colorPanel(3);
            case "Background": return backgroundPanel();
            case "Font": return fontPanel();
            case "Size": return sizePanel();
            case "Alignment": return alignmentPanel();
            case "Transparency": return transparencyPanel();
            case "Tap": return tapPanel();
            case "Photo": return photoPanel();
            case "Stickers": return stickersPanel();
            default: return new View(this);
        }
    }

    // ---------------------------------------------------------------- Panels

    private View stylePanel() {
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        grid.setPadding(dp(12), dp(12), dp(12), dp(12));

        // Colours derived from the user's own home screen: the current wallpaper (API 27+) and the
        // system Material You palette (API 31+). Snapshot at tap time.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            grid.addView(dynamicChip("🎨 Wallpaper", this::applyWallpaperColors));
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            grid.addView(dynamicChip("Material You", this::applyMaterialYou));
        }

        List<StylePresets.Preset> presets = new ArrayList<>(StylePresets.builtins());
        presets.addAll(StylePresets.custom(this));
        for (StylePresets.Preset p : presets) {
            TextView chip = pill(p.name, styleMatches(style, p.style));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.setMargins(dp(6), dp(6), dp(6), dp(6));
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> {
                copyStyle(style, p.style);
                render();
                selectTab(selectedTab);
            });
            if (p.custom) {
                chip.setOnLongClickListener(v -> {
                    StylePresets.deleteCustom(this, p.name);
                    selectTab(selectedTab);
                    return true;
                });
            }
            grid.addView(chip);
        }

        TextView save = pill("＋ Save", false);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.setMargins(dp(6), dp(6), dp(6), dp(6));
        save.setLayoutParams(lp);
        save.setOnClickListener(v -> promptSaveStyle());
        grid.addView(save);

        scroll.addView(grid);
        return scroll;
    }

    private TextView dynamicChip(String label, Runnable apply) {
        TextView chip = pill(label, false);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.setMargins(dp(6), dp(6), dp(6), dp(6));
        chip.setLayoutParams(lp);
        chip.setOnClickListener(v -> {
            apply.run();
            render();
            selectTab(selectedTab);
        });
        return chip;
    }

    /** A dark card tinted from the wallpaper's dominant colours, with a vivid accent. */
    private void applyWallpaperColors() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O_MR1) return;
        try {
            android.app.WallpaperColors wc = android.app.WallpaperManager.getInstance(this)
                    .getWallpaperColors(android.app.WallpaperManager.FLAG_SYSTEM);
            if (wc == null) {
                Toast.makeText(this, "Couldn't read wallpaper colours", Toast.LENGTH_SHORT).show();
                return;
            }
            int primary = wc.getPrimaryColor().toArgb();
            int secondary = wc.getSecondaryColor() != null ? wc.getSecondaryColor().toArgb() : primary;
            style.backgroundMode = "gradient";
            style.backgroundColor = toHex(scaleRgb(primary, 0.42f));
            style.gradientEndColor = toHex(scaleRgb(secondary, 0.20f));
            style.accentColor = toHex(vivid(primary));
            style.textColor = "#FFFFFF";
            style.backgroundOpacity = 1f;
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't read wallpaper colours", Toast.LENGTH_SHORT).show();
        }
    }

    /** The system dynamic (Material You) palette. */
    private void applyMaterialYou() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return;
        style.backgroundMode = "gradient";
        style.backgroundColor = toHex(getColor(android.R.color.system_accent1_800));
        style.gradientEndColor = toHex(getColor(android.R.color.system_neutral1_900));
        style.accentColor = toHex(getColor(android.R.color.system_accent1_200));
        style.textColor = toHex(getColor(android.R.color.system_neutral1_50));
        style.backgroundOpacity = 1f;
    }

    private static String toHex(int c) {
        return String.format("#%06X", 0xFFFFFF & c);
    }

    private static int scaleRgb(int c, float f) {
        return Color.rgb((int) (Color.red(c) * f), (int) (Color.green(c) * f), (int) (Color.blue(c) * f));
    }

    /** Same hue, pushed bright and saturated enough to read as an accent on a dark card. */
    private static int vivid(int c) {
        float[] hsv = new float[3];
        Color.colorToHSV(c, hsv);
        hsv[1] = Math.max(hsv[1], 0.55f);
        hsv[2] = Math.max(hsv[2], 0.90f);
        return Color.HSVToColor(hsv);
    }

    private void promptSaveStyle() {
        EditText input = new EditText(this);
        input.setHint("Style name");
        new MaterialAlertDialogBuilder(this)
                .setTitle("Save current as style")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) name = "My Style";
                    StylePresets.saveCustom(this, name, style);
                    Toast.makeText(this, "Style saved", Toast.LENGTH_SHORT).show();
                    selectTab(selectedTab);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** True when the current style's look-fields equal the preset's (used to mark the active chip). */
    private boolean styleMatches(WidgetStyle a, WidgetStyle b) {
        return eq(a.accentColor, b.accentColor)
                && eq(a.textColor, b.textColor)
                && eq(a.backgroundColor, b.backgroundColor)
                && eq(a.gradientEndColor, b.gradientEndColor)
                && eq(mode(a.backgroundMode), mode(b.backgroundMode))
                && a.cornerRadius == b.cornerRadius
                && a.backgroundOpacity == b.backgroundOpacity
                && eq(a.fontFamily, b.fontFamily)
                && a.contentScale == b.contentScale
                && a.gradientAngle == b.gradientAngle;
    }

    private String mode(String m) {
        return (m == null || m.isEmpty()) ? "keep" : m;
    }

    private boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private void copyStyle(WidgetStyle dst, WidgetStyle src) {
        dst.accentColor = src.accentColor;
        dst.textColor = src.textColor;
        dst.backgroundColor = src.backgroundColor;
        dst.gradientEndColor = src.gradientEndColor;
        dst.backgroundMode = src.backgroundMode;
        dst.cornerRadius = src.cornerRadius;
        dst.backgroundOpacity = src.backgroundOpacity;
        dst.fontFamily = src.fontFamily;
        dst.contentScale = src.contentScale;
        dst.alignX = src.alignX;
        dst.alignY = src.alignY;
        dst.styleVariant = src.styleVariant;
        dst.gradientAngle = src.gradientAngle;
        // keep dst.photoUri / cityName / tapPackage (per-widget bits)
    }

    private View backgroundPanel() {
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(14), dp(12), dp(14), dp(12));

        String mode = style.backgroundMode == null ? "keep" : style.backgroundMode;
        LinearLayout modeRow = new LinearLayout(this);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] modes = {"keep", "solid", "gradient", "transparent"};
        String[] labels = {"Design", "Solid", "Gradient", "Transparent"};
        for (int i = 0; i < modes.length; i++) {
            final String m = modes[i];
            TextView chip = pill(labels[i], mode.equals(m));
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.setMargins(0, 0, dp(8), 0);
            chip.setLayoutParams(clp);
            chip.setOnClickListener(v -> {
                style.backgroundMode = m;
                if ("keep".equals(m)) style.backgroundColor = null; // show the design's own bg
                render();
                selectTab(selectedTab);
            });
            modeRow.addView(chip);
        }
        col.addView(wrapScrollH(modeRow));

        if ("gradient".equals(mode)) {
            col.addView(sectionLabel("Gradient start = Color 3"));
            col.addView(sectionLabel("Gradient end color"));
            LinearLayout endRow = new LinearLayout(this);
            endRow.setOrientation(LinearLayout.HORIZONTAL);
            for (int color : PALETTE) {
                boolean sel = !isBlank(style.gradientEndColor) && matches(style.gradientEndColor, color);
                View sw = circleCellLinear(color, sel, () -> {
                    style.gradientEndColor = hex(color);
                    render();
                    selectTab(selectedTab);
                });
                endRow.addView(sw);
            }
            col.addView(wrapScrollH(endRow));

            col.addView(sectionLabel("Angle  " + Math.round(style.gradientAngle) + "°"));
            Slider angle = new Slider(this);
            angle.setValueFrom(0f);
            angle.setValueTo(360f);
            angle.setStepSize(15f);
            angle.setValue(snap(style.gradientAngle, 0f, 360f, 15f));
            angle.addOnChangeListener((s, v, u) -> { style.gradientAngle = v; render(); });
            col.addView(angle);
        }

        scroll.addView(col);
        return scroll;
    }

    private View tapPanel() {
        LinearLayout col = centeredColumn();
        String label = isBlank(style.tapPackage) ? "Choose app to open…" : ("Opens: " + appLabel(style.tapPackage));
        TextView pick = pill(label, false);
        pick.setOnClickListener(v -> showAppPicker());
        col.addView(pick);
        if (!isBlank(style.tapPackage)) {
            TextView clear = pill("Use widget's default", false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(12);
            clear.setLayoutParams(lp);
            clear.setOnClickListener(v -> { style.tapPackage = null; selectTab(selectedTab); });
            col.addView(clear);
        }
        return col;
    }

    private void showAppPicker() {
        android.content.pm.PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<android.content.pm.ResolveInfo> apps = pm.queryIntentActivities(main, 0);
        java.util.TreeMap<String, String> sorted = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (android.content.pm.ResolveInfo ri : apps) {
            String pkg = ri.activityInfo.packageName;
            String name = ri.loadLabel(pm).toString();
            if (!sorted.containsValue(pkg)) sorted.put(name, pkg);
        }
        final String[] names = sorted.keySet().toArray(new String[0]);
        final String[] pkgs = sorted.values().toArray(new String[0]);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Tap opens…")
                .setItems(names, (d, which) -> {
                    style.tapPackage = pkgs[which];
                    selectTab(selectedTab);
                })
                .show();
    }

    private String appLabel(String pkg) {
        try {
            android.content.pm.PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception e) {
            return pkg;
        }
    }

    private TextView sectionLabel(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(withAlpha(onSurface(), 0.7f));
        t.setTextSize(12f);
        t.setPadding(0, dp(12), 0, dp(4));
        return t;
    }

    private HorizontalScrollView wrapScrollH(View child) {
        HorizontalScrollView h = new HorizontalScrollView(this);
        h.setHorizontalScrollBarEnabled(false);
        h.addView(child);
        return h;
    }

    private View circleCellLinear(int color, boolean selected, Runnable onClick) {
        View v = new View(this);
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        d.setStroke(dp(selected ? 3 : 1), selected ? 0xFFFFFFFF : 0x33FFFFFF);
        v.setBackground(d);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(40), dp(40));
        lp.setMargins(0, dp(4), dp(10), dp(4));
        v.setLayoutParams(lp);
        v.setOnClickListener(x -> onClick.run());
        return v;
    }

    private View cityPanel() {
        LinearLayout col = centeredColumn();
        String current = style.cityName != null && !style.cityName.isEmpty()
                ? style.cityName : StyleApplier.weatherLocation(parsedSpec);
        TextView pill = pill((current == null || current.isEmpty() ? "Set city" : current) + "  ✎", false);
        pill.setOnClickListener(v -> {
            EditText input = new EditText(this);
            input.setHint("City name");
            input.setText(style.cityName != null ? style.cityName : current);
            new MaterialAlertDialogBuilder(this)
                    .setTitle("City")
                    .setView(input)
                    .setPositiveButton("Set", (d, w) -> {
                        style.cityName = input.getText().toString().trim();
                        render();
                        selectTab(selectedTab);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        col.addView(pill);
        return col;
    }

    private View colorPanel(int slot) {
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(6);
        grid.setPadding(dp(14), dp(12), dp(14), dp(12));

        String current = colorFor(slot);

        // "A" = auto (use design default)
        grid.addView(specialCell("A", isBlank(current), () -> { setColor(slot, null); render(); selectTab(selectedTab); }));
        // "+" = custom hex
        grid.addView(specialCell("+", false, () -> promptCustomColor(slot)));

        for (int color : PALETTE) {
            boolean sel = !isBlank(current) && matches(current, color);
            grid.addView(circleCell(color, sel, () -> {
                setColor(slot, hex(color));
                render();
                selectTab(selectedTab);
            }));
        }
        scroll.addView(grid);
        return scroll;
    }

    private View fontPanel() {
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setGravity(Gravity.CENTER_HORIZONTAL);
        wrap.setPadding(dp(16), dp(14), dp(16), dp(14));
        for (String family : com.desire.widget.engine.util.FontResolver.FAMILIES) {
            boolean sel = (family.equals(style.fontFamily))
                    || ("default".equals(family) && isBlank(style.fontFamily));
            TextView chip = pill(family, sel);
            chip.setTypeface(com.desire.widget.engine.util.FontResolver.resolve(family, false));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(5), 0, dp(5));
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> {
                style.fontFamily = "default".equals(family) ? null : family;
                render();
                selectTab(selectedTab);
            });
            wrap.addView(chip);
        }
        scroll.addView(wrap);
        return scroll;
    }

    private View sizePanel() {
        LinearLayout col = centeredColumn();
        TextView label = new TextView(this);
        label.setTextColor(onSurface());
        label.setText("Size  " + Math.round(style.contentScale * 100) + "%");
        col.addView(label);

        Slider slider = new Slider(this);
        slider.setValueFrom(0.6f);
        slider.setValueTo(1.3f);
        slider.setStepSize(0.05f);
        slider.setValue(snap(style.contentScale, 0.6f, 1.3f, 0.05f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(24), dp(12), dp(24), 0);
        slider.addOnChangeListener((s, v, u) -> {
            style.contentScale = v;
            label.setText("Size  " + Math.round(v * 100) + "%");
            render();
        });
        col.addView(slider, lp);
        return col;
    }

    private View transparencyPanel() {
        LinearLayout col = centeredColumn();
        TextView label = new TextView(this);
        label.setTextColor(onSurface());
        label.setText("Opacity  " + Math.round(style.backgroundOpacity * 100) + "%");
        col.addView(label);

        Slider slider = new Slider(this);
        slider.setValueFrom(0f);
        slider.setValueTo(1f);
        slider.setStepSize(0.05f);
        slider.setValue(snap(style.backgroundOpacity, 0f, 1f, 0.05f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(24), dp(12), dp(24), 0);
        slider.addOnChangeListener((s, v, u) -> {
            style.backgroundOpacity = v;
            label.setText("Opacity  " + Math.round(v * 100) + "%");
            render();
        });
        col.addView(slider, lp);
        return col;
    }

    private View alignmentPanel() {
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        grid.setUseDefaultMargins(true);
        FrameHolder holder = new FrameHolder();
        holder.wrap = centeredColumn();

        String[] glyphs = {"↖", "↑", "↗", "←", "●", "→", "↙", "↓", "↘"};
        int[][] coords = {{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {0, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}};
        for (int i = 0; i < glyphs.length; i++) {
            final int ax = coords[i][0], ay = coords[i][1];
            boolean sel = style.alignX == ax && style.alignY == ay;
            TextView cell = specialCell(glyphs[i], sel, () -> {
                style.alignX = ax;
                style.alignY = ay;
                render();
                selectTab(selectedTab);
            });
            grid.addView(cell);
        }
        holder.wrap.addView(grid);
        return holder.wrap;
    }

    /**
     * Photo / slideshow: add one or more photos. One photo is a static background (or fills the
     * widget's photo frame); two or more rotate on the chosen interval.
     */
    private View photoPanel() {
        LinearLayout col = centeredColumn();
        int count = style.photoUris != null ? style.photoUris.size()
                : (style.photoUri != null && !style.photoUri.isEmpty() ? 1 : 0);

        TextView info = new TextView(this);
        info.setText(count == 0 ? "Pick photos for the background — add several for a slideshow"
                : count == 1 ? "1 photo" : count + " photos · slideshow");
        info.setTextColor(withAlpha(onSurface(), 0.7f));
        info.setTextSize(13f);
        info.setGravity(Gravity.CENTER);
        info.setPadding(dp(12), 0, dp(12), dp(12));
        col.addView(info);

        LinearLayout actions = centeredRow();
        actions.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView add = pill(count == 0 ? "Add photos…" : "＋ Add more", false);
        add.setOnClickListener(v -> {
            try {
                photoPicker.launch(new String[]{"image/*"});
            } catch (Exception e) {
                Toast.makeText(this, "No photo picker available", Toast.LENGTH_SHORT).show();
            }
        });
        actions.addView(add);
        if (count > 0) {
            TextView clear = pill("Remove all", false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = dp(10);
            clear.setLayoutParams(lp);
            clear.setOnClickListener(v -> {
                style.photoUri = null;
                style.photoUris = null;
                render();
                selectTab(selectedTab);
            });
            actions.addView(clear);
        }
        col.addView(actions);

        if (count >= 2) {
            TextView every = new TextView(this);
            every.setText("Change every");
            every.setTextColor(withAlpha(onSurface(), 0.7f));
            every.setPadding(0, dp(16), 0, dp(8));
            col.addView(every);
            LinearLayout intervals = centeredRow();
            intervals.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            int[] mins = {15, 60, 360, 1440};
            String[] labels = {"15m", "1h", "6h", "1 day"};
            for (int i = 0; i < mins.length; i++) {
                final int m = mins[i];
                TextView chip = pill(labels[i], style.photoIntervalMin == m);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(dp(4), 0, dp(4), 0);
                chip.setLayoutParams(lp);
                chip.setOnClickListener(v -> {
                    style.photoIntervalMin = m;
                    render();
                    selectTab(selectedTab);
                });
                intervals.addView(chip);
            }
            col.addView(intervals);
        }
        return col;
    }

    // ---------------------------------------------------------------- Stickers

    private View stickersPanel() {
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(12), dp(10), dp(12), dp(12));

        TextView hint = new TextView(this);
        hint.setText("Tap a sticker to add it. On the preview: drag to move, pinch to resize, "
                + "twist two fingers to rotate.");
        hint.setTextColor(withAlpha(onSurface(), 0.7f));
        hint.setTextSize(13f);
        hint.setPadding(dp(4), 0, dp(4), dp(12));
        col.addView(hint);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        for (StickerCatalog.Sticker s : StickerCatalog.load(this)) {
            ImageView iv = new ImageView(this);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = dp(60);
            lp.height = dp(60);
            lp.setMargins(dp(6), dp(6), dp(6), dp(6));
            iv.setLayoutParams(lp);
            iv.setPadding(dp(6), dp(6), dp(6), dp(6));
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(14));
            bg.setColor(0x1FFFFFFF);
            iv.setBackground(bg);
            loadThumb(iv, s.source);
            iv.setOnClickListener(v -> {
                if (style.stickers == null) style.stickers = new ArrayList<>();
                style.stickers.add(new WidgetStyle.Sticker(s.source));
                selectedSticker = style.stickers.size() - 1;
                render();
                selectTab(selectedTab);
            });
            grid.addView(iv);
        }
        col.addView(grid);

        if (style.stickers != null && selectedSticker >= 0 && selectedSticker < style.stickers.size()) {
            WidgetStyle.Sticker sel = style.stickers.get(selectedSticker);

            TextView sizeLabel = new TextView(this);
            sizeLabel.setText("Selected sticker size");
            sizeLabel.setTextColor(onSurface());
            sizeLabel.setPadding(dp(4), dp(16), dp(4), dp(4));
            col.addView(sizeLabel);

            Slider slider = new Slider(this);
            slider.setValueFrom(0.1f);
            slider.setValueTo(1.2f);
            slider.setValue(Math.max(0.1f, Math.min(1.2f, sel.scale)));
            slider.addOnChangeListener((sl, value, fromUser) -> {
                sel.scale = value;
                render();
            });
            col.addView(slider);

            TextView remove = pill("Remove sticker", false);
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.topMargin = dp(12);
            remove.setLayoutParams(rlp);
            remove.setOnClickListener(v -> {
                style.stickers.remove(selectedSticker);
                selectedSticker = style.stickers.isEmpty() ? -1 : style.stickers.size() - 1;
                render();
                selectTab(selectedTab);
            });
            col.addView(remove);
        }

        scroll.addView(col);
        return scroll;
    }

    private void loadThumb(ImageView iv, String source) {
        try {
            if (source.startsWith("http")) {
                Glide.with(this).load(source).into(iv);
            } else if (source.startsWith("asset://")) {
                Glide.with(this).load("file:///android_asset/" + source.substring("asset://".length())).into(iv);
            } else if (source.startsWith("content://") || source.startsWith("file://")) {
                Glide.with(this).load(Uri.parse(source)).into(iv);
            } else {
                int id = getResources().getIdentifier(source, "drawable", getPackageName());
                if (id != 0) iv.setImageResource(id);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Manipulate the selected sticker directly on the preview (Stickers tab only): one finger drags,
     * two fingers pinch-to-scale and rotate. For 60fps we DON'T re-render the widget bitmap per
     * frame — the preview is baked once without the grabbed sticker, and a lightweight
     * {@link #stickerGhost} overlay of just that sticker is moved/scaled/rotated with cheap
     * draw-time transforms. A single full re-render bakes it in when the gesture ends.
     */
    private boolean handleStickerDrag(MotionEvent event) {
        if (selectedTab < 0 || selectedTab >= tabs.size() || !"Stickers".equals(tabs.get(selectedTab))) {
            return false;
        }
        if (style.stickers == null || style.stickers.isEmpty()) return false;

        scaleDetector.onTouchEvent(event); // pinch-to-scale (updates the ghost's scaleX/Y)

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                float[] norm = toNormalized(event.getX(), event.getY());
                if (norm == null) return false;
                int near = nearestSticker(norm[0], norm[1]);
                if (near < 0) return false; // touched empty space — ignore
                selectedSticker = near;
                startGhostDrag(near);
                return true;
            }
            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() >= 2) lastTwoFingerAngle = twoFingerAngle(event);
                return draggingSticker >= 0;
            case MotionEvent.ACTION_MOVE: {
                if (draggingSticker < 0) return true;
                WidgetStyle.Sticker st = style.stickers.get(draggingSticker);
                if (event.getPointerCount() >= 2) {
                    // Rotate by the change in angle between the two fingers, and keep the sticker
                    // centred between them (so pinch/rotate happens about the fingers).
                    float ang = twoFingerAngle(event);
                    if (!Float.isNaN(lastTwoFingerAngle)) {
                        st.rotation += ang - lastTwoFingerAngle;
                        if (stickerGhost != null) stickerGhost.setRotation(st.rotation);
                    }
                    lastTwoFingerAngle = ang;
                    moveGhostTo(st, (event.getX(0) + event.getX(1)) / 2f,
                            (event.getY(0) + event.getY(1)) / 2f);
                } else if (!scaleDetector.isInProgress()) {
                    moveGhostTo(st, event.getX(), event.getY());
                }
                return true;
            }
            case MotionEvent.ACTION_POINTER_UP:
                lastTwoFingerAngle = Float.NaN;
                return draggingSticker >= 0;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (draggingSticker < 0) return false;
                draggingSticker = -1;
                lastTwoFingerAngle = Float.NaN;
                render();               // bake the sticker back in; hides the ghost on completion
                selectTab(selectedTab); // refresh size/remove controls
                return true;
            default:
                return false;
        }
    }

    /** Moves the model + ghost so the sticker is centred at a preview-view point. */
    private void moveGhostTo(WidgetStyle.Sticker st, float viewX, float viewY) {
        float[] norm = toNormalized(viewX, viewY);
        if (norm != null) {
            st.x = Math.max(0f, Math.min(1f, norm[0]));
            st.y = Math.max(0f, Math.min(1f, norm[1]));
        }
        if (stickerGhost != null) {
            stickerGhost.setX(preview.getLeft() + viewX - stickerGhost.getWidth() / 2f);
            stickerGhost.setY(preview.getTop() + viewY - stickerGhost.getHeight() / 2f);
        }
    }

    private float twoFingerAngle(MotionEvent e) {
        return (float) Math.toDegrees(Math.atan2(e.getY(1) - e.getY(0), e.getX(1) - e.getX(0)));
    }

    /** Bakes the preview without the given sticker and shows its ghost overlay at its current spot. */
    private void startGhostDrag(int index) {
        WidgetStyle.Sticker st = style.stickers.get(index);
        android.graphics.drawable.Drawable dr = preview.getDrawable();
        if (dr == null) return;
        int iw = dr.getIntrinsicWidth();
        float[] mv = new float[9];
        preview.getImageMatrix().getValues(mv);
        float scale = mv[android.graphics.Matrix.MSCALE_X];
        int box = Math.max(1, Math.round(Math.max(0.05f, Math.min(1.5f, st.scale)) * iw * scale));

        ImageView g = ghost();
        g.setLayoutParams(new FrameLayout.LayoutParams(box, box));
        g.setScaleX(1f);
        g.setScaleY(1f);
        g.setRotation(st.rotation);
        ghostBaseScale = st.scale;
        loadThumb(g, st.source);
        float[] center = stickerToFrame(st.x, st.y);
        if (center != null) {
            g.setX(center[0] - box / 2f);
            g.setY(center[1] - box / 2f);
        }
        g.setVisibility(View.VISIBLE);
        g.bringToFront();

        draggingSticker = index;
        render(); // re-bake the preview WITHOUT this sticker (styleCopy drops draggingSticker)
    }

    private ImageView ghost() {
        if (stickerGhost == null) {
            stickerGhost = new ImageView(this);
            stickerGhost.setScaleType(ImageView.ScaleType.FIT_CENTER);
            stickerGhost.setVisibility(View.GONE);
            ((ViewGroup) preview.getParent()).addView(stickerGhost);
        }
        return stickerGhost;
    }

    /** Maps a normalized widget point to coordinates in the preview's parent (for the ghost). */
    private float[] stickerToFrame(float nx, float ny) {
        android.graphics.drawable.Drawable dr = preview.getDrawable();
        if (dr == null) return null;
        int iw = dr.getIntrinsicWidth();
        int ih = dr.getIntrinsicHeight();
        float[] mv = new float[9];
        preview.getImageMatrix().getValues(mv);
        float cx = preview.getLeft() + mv[android.graphics.Matrix.MTRANS_X] + nx * iw * mv[android.graphics.Matrix.MSCALE_X];
        float cy = preview.getTop() + mv[android.graphics.Matrix.MTRANS_Y] + ny * ih * mv[android.graphics.Matrix.MSCALE_Y];
        return new float[]{cx, cy};
    }

    /** Maps a touch point on the preview ImageView to normalized (0..1) widget coordinates. */
    private float[] toNormalized(float px, float py) {
        android.graphics.drawable.Drawable dr = preview.getDrawable();
        if (dr == null) return null;
        int iw = dr.getIntrinsicWidth();
        int ih = dr.getIntrinsicHeight();
        if (iw <= 0 || ih <= 0) return null;
        android.graphics.Matrix inverse = new android.graphics.Matrix();
        if (!preview.getImageMatrix().invert(inverse)) return null;
        float[] pts = {px, py};
        inverse.mapPoints(pts);
        return new float[]{pts[0] / iw, pts[1] / ih};
    }

    /** Nearest sticker to a normalized point, if within a grab radius; else -1. */
    private int nearestSticker(float nx, float ny) {
        int best = -1;
        float bestD = Float.MAX_VALUE;
        for (int i = 0; i < style.stickers.size(); i++) {
            WidgetStyle.Sticker s = style.stickers.get(i);
            float dx = s.x - nx, dy = s.y - ny;
            float d = dx * dx + dy * dy;
            if (d < bestD) { bestD = d; best = i; }
        }
        return bestD <= 0.09f ? best : -1; // within ~0.3 of normalized distance
    }

    private void promptCustomColor(int slot) {
        EditText input = new EditText(this);
        input.setHint("#RRGGBB");
        String cur = colorFor(slot);
        input.setText(cur != null ? cur : "#");
        new MaterialAlertDialogBuilder(this)
                .setTitle("Custom color")
                .setView(input)
                .setPositiveButton("Apply", (d, w) -> {
                    String hex = input.getText().toString().trim();
                    try {
                        Color.parseColor(hex);
                        setColor(slot, hex);
                        render();
                        selectTab(selectedTab);
                    } catch (Exception e) {
                        Toast.makeText(this, "Invalid color", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---------------------------------------------------------------- Color slots

    private String colorFor(int slot) {
        switch (slot) {
            case 1: return style.accentColor;
            case 2: return style.textColor;
            default: return style.backgroundColor;
        }
    }

    private void setColor(int slot, String hex) {
        switch (slot) {
            case 1: style.accentColor = hex; break;
            case 2: style.textColor = hex; break;
            default: style.backgroundColor = hex; break;
        }
    }

    // ---------------------------------------------------------------- Actions

    private void setupActions() {
        findViewById(R.id.cw_close).setOnClickListener(v -> finish());
        findViewById(R.id.cw_reset).setOnClickListener(v -> resetStyle());
        findViewById(R.id.cw_add_home).setOnClickListener(v -> addToHome());
        // Studio (advanced JSON editor) has been removed — hide its button.
        findViewById(R.id.cw_advanced).setVisibility(android.view.View.GONE);
    }

    private void addToHome() {
        String styleJson = style.isEmpty() ? null : GSON.toJson(style);
        String id = widgetId != null ? widgetId : "custom_" + System.currentTimeMillis();
        if (appWidgetId != -1) {
            com.desire.widget.engine.runtime.EngineWidgetStore.saveWidget(
                    this, appWidgetId, id, widgetName, specJson, size, styleJson);
            com.desire.widget.engine.runtime.EngineRenderer.render(this, new int[]{appWidgetId});
            Toast.makeText(this, "Widget updated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        WidgetInstaller.installSpecToHome(findViewById(R.id.cw_root), id, widgetName, specJson, size, styleJson);
    }

    private void resetStyle() {
        WidgetStyle fresh = new WidgetStyle();
        style.accentColor = null;
        style.textColor = null;
        style.backgroundColor = null;
        style.gradientEndColor = null;
        style.backgroundMode = null;
        style.cornerRadius = -1f;
        style.backgroundOpacity = fresh.backgroundOpacity;
        style.fontFamily = null;
        style.photoUri = null;
        style.photoUris = null;
        style.photoIntervalMin = fresh.photoIntervalMin;
        style.contentScale = fresh.contentScale;
        style.alignX = 0;
        style.alignY = 0;
        style.styleVariant = 0;
        style.cityName = null;
        style.stickers = null;
        selectedSticker = -1;
        render();
        selectTab(selectedTab);
    }


    // ---------------------------------------------------------------- Live clock

    /** Re-renders the preview in real time for clocks/visualizers, like the placed widget. */
    private final android.os.Handler liveHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private long liveInterval = com.desire.widget.ui.widgets.LivePreview.STATIC;
    private long lastLiveMinute = -1;
    private final Runnable liveTick = new Runnable() {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            boolean due = liveInterval == com.desire.widget.ui.widgets.LivePreview.EVERY_SECOND
                    || (liveInterval == com.desire.widget.ui.widgets.LivePreview.EVERY_MINUTE
                        && now / 60_000L != lastLiveMinute);
            // Never fight a sticker gesture: the drag ghost is drawn separately.
            if (due && draggingSticker < 0) {
                lastLiveMinute = now / 60_000L;
                render();
            }
            liveHandler.postDelayed(this, 1000 - now % 1000 + 15);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        try {
            liveInterval = com.desire.widget.ui.widgets.LivePreview.intervalOf(GSON.fromJson(specJson, WidgetSpec.class));
        } catch (Exception e) {
            liveInterval = com.desire.widget.ui.widgets.LivePreview.STATIC;
        }
        liveHandler.removeCallbacks(liveTick);
        if (liveInterval != com.desire.widget.ui.widgets.LivePreview.STATIC) liveHandler.post(liveTick);
    }

    @Override
    protected void onPause() {
        liveHandler.removeCallbacks(liveTick);
        super.onPause();
    }

    // ---------------------------------------------------------------- Render

    private void render() {
        WidgetSpec spec;
        try {
            spec = GSON.fromJson(specJson, WidgetSpec.class);
        } catch (Exception e) {
            return;
        }
        if (spec == null) return;

        int[] ratio = WidgetSchema.previewRatio(size);
        final int w = 720;
        final int h = Math.max(1, w * ratio[1] / ratio[0]);
        final int token = renderToken.incrementAndGet();
        final WidgetStyle styleCopy = GSON.fromJson(GSON.toJson(style), WidgetStyle.class);
        // While dragging, the ghost overlay stands in for this sticker — leave it out of the bitmap.
        if (draggingSticker >= 0 && styleCopy.stickers != null
                && draggingSticker < styleCopy.stickers.size()) {
            styleCopy.stickers.remove(draggingSticker);
        }
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
                if (result != null && token == renderToken.get()) {
                    preview.setImageBitmap(result);
                    // Once the sticker is baked back into the bitmap, drop the ghost overlay.
                    if (draggingSticker < 0 && stickerGhost != null) {
                        stickerGhost.setVisibility(View.GONE);
                    }
                }
            });
        });
    }

    // ---------------------------------------------------------------- View helpers

    private LinearLayout centeredColumn() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return col;
    }

    private LinearLayout centeredRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return row;
    }

    private TextView pill(String text, boolean selected) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setPadding(dp(20), dp(10), dp(20), dp(10));
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(22));
        d.setColor(selected ? themeColor(androidx.appcompat.R.attr.colorPrimary, 0xFF6C7BFF) : 0x1FFFFFFF);
        t.setBackground(d);
        t.setTextColor(selected ? themeColor(com.google.android.material.R.attr.colorOnPrimary, 0xFFFFFFFF) : onSurface());
        return t;
    }

    private View circleCell(int color, boolean selected, Runnable onClick) {
        View v = new View(this);
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        d.setStroke(dp(selected ? 3 : 1), selected ? 0xFFFFFFFF : 0x33FFFFFF);
        v.setBackground(d);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = dp(42);
        lp.height = dp(42);
        lp.setMargins(dp(6), dp(6), dp(6), dp(6));
        v.setLayoutParams(lp);
        v.setOnClickListener(x -> onClick.run());
        return v;
    }

    private TextView specialCell(String label, boolean selected, Runnable onClick) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setGravity(Gravity.CENTER);
        t.setTextColor(onSurface());
        t.setTextSize(18f);
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(0xFF2A2E36);
        d.setStroke(dp(selected ? 3 : 1), selected ? themeColor(androidx.appcompat.R.attr.colorPrimary, 0xFF6C7BFF) : 0x33FFFFFF);
        t.setBackground(d);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = dp(42);
        lp.height = dp(42);
        lp.setMargins(dp(6), dp(6), dp(6), dp(6));
        t.setLayoutParams(lp);
        t.setOnClickListener(x -> onClick.run());
        return t;
    }

    private boolean matches(String hex, int color) {
        try {
            return (Color.parseColor(hex) | 0xFF000000) == (color | 0xFF000000);
        } catch (Exception e) {
            return false;
        }
    }

    private int onSurface() {
        return themeColor(com.google.android.material.R.attr.colorOnSurface, 0xFFFFFFFF);
    }

    private int themeColor(int attr, int fallback) {
        try {
            android.util.TypedValue tv = new android.util.TypedValue();
            if (getTheme().resolveAttribute(attr, tv, true)) {
                return tv.data != 0 ? tv.data : fallback;
            }
        } catch (Exception ignored) {}
        return fallback;
    }

    private int withAlpha(int color, float frac) {
        int a = Math.round(Math.max(0f, Math.min(1f, frac)) * 255f);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private float snap(float v, float from, float to, float step) {
        v = Math.max(from, Math.min(to, v));
        return from + Math.round((v - from) / step) * step;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String hex(int color) {
        return String.format("#%08X", color);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    /** Small holder so alignment panel can nest a grid inside a centered column. */
    private static final class FrameHolder {
        LinearLayout wrap;
    }
}
