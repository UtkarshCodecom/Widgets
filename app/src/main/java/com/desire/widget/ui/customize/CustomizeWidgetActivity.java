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
    /** Optional: the look to start from (a WidgetStyle as JSON). */
    public static final String EXTRA_STYLE_JSON = "style_json";
    /** Optional: the saved look being edited; also returned when a look is saved. */
    public static final String EXTRA_VARIANT_ID = "variant_id";

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
    private WidgetStyle style = new WidgetStyle();
    /** The saved look being edited (from the widget page), or null for a new one. */
    private String variantId;
    /** Widget-specific tabs: the "Clock Style" looks and one colour tab per part. */
    private com.desire.widget.engine.runtime.EditorSchema.StyleAxis axis;
    private List<com.desire.widget.engine.runtime.EditorSchema.ColorSlot> slots = new ArrayList<>();
    private final List<java.util.function.Supplier<View>> panels = new ArrayList<>();

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
        WidgetDetailActivity.showWallpaperBehind(this); // preview over the user's own wallpaper
        setContentView(R.layout.activity_customize_widget);

        specJson = getIntent().getStringExtra(EXTRA_SPEC_JSON);
        if (getIntent().getStringExtra(EXTRA_NAME) != null) widgetName = getIntent().getStringExtra(EXTRA_NAME);
        size = WidgetSchema.normalizeSize(getIntent().getStringExtra(EXTRA_SIZE));
        widgetId = getIntent().getStringExtra(EXTRA_WIDGET_ID);
        appWidgetId = getIntent().getIntExtra(EXTRA_APPWIDGET_ID, -1);
        variantId = getIntent().getStringExtra(EXTRA_VARIANT_ID);

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
        seedStyle(getIntent().getStringExtra(EXTRA_STYLE_JSON));

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

        ((TextView) findViewById(R.id.cw_add_home)).setText(primaryLabel());
        applyInsets();

        selectTab(0);
        preview.post(this::render);
    }

    private void preloadStyleIfReconfigure() {
        if (appWidgetId == -1) return;
        seedStyle(com.desire.widget.engine.runtime.EngineWidgetStore.getStyleJson(this, appWidgetId));
    }

    /** Starts editing from a saved style (a placed widget's, or a look from the widget page). */
    private void seedStyle(String json) {
        if (json == null || json.isEmpty()) return;
        try {
            WidgetStyle s = GSON.fromJson(json, WidgetStyle.class);
            if (s != null) style = s;
        } catch (Exception ignored) {}
    }

    /** Status bar above the preview, navigation bar below the action row. */
    private void applyInsets() {
        View area = findViewById(R.id.cw_preview_area);
        View actions = findViewById(R.id.cw_actions);
        int areaTop = area.getPaddingTop();
        int actionsBottom = actions.getPaddingBottom();
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cw_root), (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars()
                            | androidx.core.view.WindowInsetsCompat.Type.ime());
            area.setPadding(area.getPaddingLeft(), areaTop + bars.top, area.getPaddingRight(), area.getPaddingBottom());
            actions.setPadding(actions.getPaddingLeft(), actions.getPaddingTop(), actions.getPaddingRight(),
                    actionsBottom + bars.bottom);
            return insets;
        });
    }

    /** Opened from the widget page → saves a look there; otherwise pins/applies directly. */
    private boolean savesLook() {
        return appWidgetId == -1 && getCallingActivity() != null && widgetId != null;
    }

    private String primaryLabel() {
        if (appWidgetId != -1) return "✓  APPLY";
        if (savesLook()) return variantId != null ? "✓  SAVE" : "＋  ADD NEW";
        return "＋  ADD TO HOME";
    }

    // ---------------------------------------------------------------- Tabs

    private void buildTabs() {
        tabs.clear();
        panels.clear();
        axis = com.desire.widget.engine.runtime.EditorSchema.styleAxis(parsedSpec, specJson);
        slots = com.desire.widget.engine.runtime.EditorSchema.colorSlots(parsedSpec);

        if (axis != null) addTab(axis.label, this::stylePanel);
        for (com.desire.widget.engine.runtime.EditorSchema.ColorSlot slot : slots) {
            addTab(slot.label, () -> colorPanel(new SlotTarget(slot)));
        }
        if (slots.isEmpty()) {
            addTab("Accent Color", () -> colorPanel(new ThemeTarget(true)));
            addTab("Text Color", () -> colorPanel(new ThemeTarget(false)));
        }
        addTab("Background", this::backgroundPanel);
        addTab("Font", this::fontPanel);
        addTab("Size", this::sizePanel);
        addTab("Alignment", this::alignmentPanel);
        addTab("Transparency", this::transparencyPanel);
        addTab("Themes", this::themesPanel);
        if (StyleApplier.hasWeather(parsedSpec)) addTab("City", this::cityPanel);
        addTab("Photo", this::photoPanel);
        addTab("Stickers", this::stickersPanel);
        addTab("Tap", this::tapPanel);

        tabsRow.removeAllViews();
        for (int i = 0; i < tabs.size(); i++) {
            final int idx = i;
            TextView tab = new TextView(this);
            tab.setText(tabs.get(i));
            tab.setPadding(dp(16), dp(14), dp(16), dp(16));
            tab.setTextSize(15f);
            tab.setLetterSpacing(0.02f);
            tab.setOnClickListener(v -> selectTab(idx));
            tabsRow.addView(tab);
        }
    }

    private void addTab(String label, java.util.function.Supplier<View> panel) {
        tabs.add(label);
        panels.add(panel);
    }

    private void selectTab(int index) {
        if (index < 0 || index >= panels.size()) index = 0;
        selectedTab = index;
        for (int i = 0; i < tabsRow.getChildCount(); i++) {
            TextView tab = (TextView) tabsRow.getChildAt(i);
            boolean sel = i == index;
            tab.setTextColor(sel ? onSurface() : withAlpha(onSurface(), 0.5f));
            tab.setTypeface(null, sel ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            tab.setBackgroundResource(sel ? R.drawable.bg_tab_selected : 0);
        }
        View tab = tabsRow.getChildAt(index);
        if (tab != null) {
            View scroller = findViewById(R.id.cw_tabs_scroll);
            scroller.post(() -> ((HorizontalScrollView) scroller).smoothScrollTo(
                    Math.max(0, tab.getLeft() - dp(40)), 0));
        }
        content.removeAllViews();
        content.addView(panels.get(index).get());
    }

    // ---------------------------------------------------------------- Panels

    /** Our colour themes: wallpaper / Material You, built-in palettes and the user's saved ones. */
    private View themesPanel() {
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
                clearSlotColors(); // a theme recolours everything; drop per-part colours
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
            clearSlotColors();
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

    // ---------------------------------------------------------------- Style looks ("Style 1…N")

    /** The design's own looks as live thumbnails, like Muviz's "Style 1 … Style 4". */
    private View stylePanel() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(8), dp(12), dp(8));
        row.setMinimumWidth(getResources().getDisplayMetrics().widthPixels);

        int[] ratio = WidgetSchema.previewRatio(size);
        int boxH = dp(ratio[0] >= 4 * ratio[1] ? 70 : 140);
        int boxW = Math.max(dp(110), boxH * ratio[0] / ratio[1]);
        int current = currentOption();
        for (int i = 0; i < axis.options.size(); i++) {
            final int index = i;
            com.desire.widget.engine.runtime.EditorSchema.Option o = axis.options.get(i);
            boolean sel = i == current;

            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.setMargins(dp(6), 0, dp(6), 0);
            cell.setLayoutParams(clp);

            ImageView thumb = new ImageView(this);
            thumb.setScaleType(ImageView.ScaleType.FIT_CENTER);
            thumb.setPadding(dp(8), dp(8), dp(8), dp(8));
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(18));
            bg.setColor(0x14FFFFFF);
            bg.setStroke(dp(sel ? 2 : 1), sel ? themeColor(androidx.appcompat.R.attr.colorPrimary, 0xFF6C7BFF) : 0x22FFFFFF);
            thumb.setBackground(bg);
            cell.addView(thumb, new LinearLayout.LayoutParams(boxW, boxH));
            new StyledPreview(thumb, 360).bind(specJson, GSON.toJson(withOption(index)), ratio);

            TextView label = pill(o.name, sel);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            llp.topMargin = dp(10);
            label.setLayoutParams(llp);
            cell.addView(label);

            View.OnClickListener pick = v -> {
                applyOption(index);
                render();
                selectTab(selectedTab);
            };
            thumb.setOnClickListener(pick);
            label.setOnClickListener(pick);
            row.addView(cell);
        }
        scroll.addView(row);
        return scroll;
    }

    /** Index of the active look: explicit choice, else the one matching the current props. */
    private int currentOption() {
        if (style.styleOption >= 0 && style.styleOption < axis.options.size()) return style.styleOption;
        for (int i = 0; i < axis.options.size(); i++) {
            boolean all = true;
            for (java.util.Map.Entry<String, Object> e : axis.options.get(i).props.entrySet()) {
                Object v = style.props != null ? style.props.get(e.getKey()) : null;
                if (v == null || !String.valueOf(v).equals(String.valueOf(e.getValue()))) all = false;
            }
            if (all) return i;
        }
        // No override yet: highlight the look the design itself uses.
        if (style.props == null || style.props.isEmpty()) {
            for (int i = 0; i < axis.options.size(); i++) {
                boolean all = true;
                for (java.util.Map.Entry<String, Object> e : axis.options.get(i).props.entrySet()) {
                    String k = e.getKey();
                    int dot = k.indexOf('.');
                    Object v = null;
                    try {
                        int ci = Integer.parseInt(k.substring(0, dot));
                        if (parsedSpec != null && parsedSpec.components != null && ci < parsedSpec.components.size()
                                && parsedSpec.components.get(ci).props != null) {
                            v = parsedSpec.components.get(ci).props.get(k.substring(dot + 1));
                        }
                    } catch (Exception ignored) {}
                    if (v == null ? i != 0 : !String.valueOf(v).equals(String.valueOf(e.getValue()))) all = false;
                }
                if (all) return i;
            }
        }
        return -1;
    }

    /** A copy of the current style with look {@code index} applied (for its thumbnail). */
    private WidgetStyle withOption(int index) {
        WidgetStyle copy = GSON.fromJson(GSON.toJson(style), WidgetStyle.class);
        WidgetStyle saved = style;
        style = copy;
        applyOption(index);
        style = saved;
        return copy;
    }

    private void applyOption(int index) {
        if (style.props == null) style.props = new java.util.LinkedHashMap<>();
        for (String k : axis.keys) style.props.remove(k);
        style.props.putAll(axis.options.get(index).props);
        style.styleOption = index;
    }

    // ---------------------------------------------------------------- Colour tabs

    /** What a colour tab edits: one part of the design, or a theme-wide colour. */
    private interface ColorTarget {
        String get();
        void set(String value); // null = the design's own colour
        boolean gradients();
    }

    /** One part of the design ("Hour Color" → every prop listed in the slot). */
    private final class SlotTarget implements ColorTarget {
        final com.desire.widget.engine.runtime.EditorSchema.ColorSlot slot;

        SlotTarget(com.desire.widget.engine.runtime.EditorSchema.ColorSlot slot) {
            this.slot = slot;
        }

        @Override public String get() {
            Object v = style.props != null ? style.props.get(slot.keys.get(0)) : null;
            return v instanceof String ? (String) v : null;
        }

        @Override public void set(String value) {
            if (style.props == null) style.props = new java.util.LinkedHashMap<>();
            for (String k : slot.keys) {
                if (value == null) style.props.remove(k);
                else style.props.put(k, value);
            }
        }

        @Override public boolean gradients() {
            return slot.gradients;
        }
    }

    /** Fallback for designs without named parts: the theme accent / text colour. */
    private final class ThemeTarget implements ColorTarget {
        final boolean accent;

        ThemeTarget(boolean accent) {
            this.accent = accent;
        }

        @Override public String get() {
            return accent ? style.accentColor : style.textColor;
        }

        @Override public void set(String value) {
            if (accent) style.accentColor = value;
            else style.textColor = value;
        }

        @Override public boolean gradients() {
            return false;
        }
    }

    /** Gradient swatches (start, end). */
    private static final int[][] GRADIENTS = {
            {0xFFFF5F6D, 0xFFFFC371}, {0xFFF7971E, 0xFFFFD200}, {0xFFFC466B, 0xFF3F5EFB},
            {0xFF8E2DE2, 0xFF4A00E0}, {0xFF00C9FF, 0xFF92FE9D}, {0xFF43E97B, 0xFF38F9D7},
            {0xFFFA709A, 0xFFFEE140}, {0xFF30CFD0, 0xFF330867}, {0xFFA18CD1, 0xFFFBC2EB},
            {0xFFFF9A9E, 0xFFFECFEF}, {0xFFE0E0E0, 0xFF8E8E8E}, {0xFFF5F7FA, 0xFFC3CFE2},
    };

    /** Muviz-style swatch grid: ＋ custom, A original, ✦ wallpaper, gradients, then solids. */
    private View colorPanel(ColorTarget target) {
        String current = target.get();
        GridLayout grid = swatchGrid();
        grid.addView(swatch(SWATCH_CUSTOM, 0, 0, false, () -> pickCustomColor(current, hex -> {
            target.set(hex);
            render();
            selectTab(selectedTab);
        })));
        grid.addView(swatch(SWATCH_ORIGINAL, 0, 0, isBlank(current), () -> setAndRefresh(target, null)));
        grid.addView(swatch(SWATCH_WALLPAPER, StyleApplier.wallpaperAccent(), 0,
                StyleApplier.WALLPAPER.equals(current), () -> setAndRefresh(target, StyleApplier.WALLPAPER)));
        if (target.gradients()) {
            for (int[] g : GRADIENTS) {
                String value = com.desire.widget.engine.util.SpecPaint.gradient(g[0], g[1]);
                grid.addView(swatch(SWATCH_GRADIENT, g[0], g[1], value.equalsIgnoreCase(current),
                        () -> setAndRefresh(target, value)));
            }
        }
        for (int color : PALETTE) {
            grid.addView(swatch(SWATCH_SOLID, color, 0, !isBlank(current) && matches(current, color),
                    () -> setAndRefresh(target, hex(color))));
        }
        return wrapGrid(grid, "A  original colour   ·   ✦  follows your wallpaper");
    }

    private void setAndRefresh(ColorTarget target, String value) {
        target.set(value);
        render();
        selectTab(selectedTab);
    }

    /** Drops per-part colours (used when a whole theme is applied). Keeps the chosen look. */
    private void clearSlotColors() {
        if (style.props == null) return;
        for (com.desire.widget.engine.runtime.EditorSchema.ColorSlot slot : slots) {
            for (String k : slot.keys) style.props.remove(k);
        }
    }

    private static final int SWATCH_SOLID = 0, SWATCH_GRADIENT = 1, SWATCH_CUSTOM = 2,
            SWATCH_ORIGINAL = 3, SWATCH_WALLPAPER = 4;

    private GridLayout swatchGrid() {
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setPadding(dp(10), dp(4), dp(10), dp(16));
        return grid;
    }

    private View wrapGrid(View grid, String hint) {
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        if (hint != null) {
            TextView t = new TextView(this);
            t.setText(hint);
            t.setTextSize(12f);
            t.setTextColor(withAlpha(onSurface(), 0.5f));
            t.setGravity(Gravity.CENTER);
            t.setPadding(dp(12), dp(12), dp(12), dp(2));
            col.addView(t);
        }
        col.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        scroll.addView(col);
        return scroll;
    }

    /** One 56dp circle in a 4-column grid; the selected one gets a white ring. */
    private View swatch(int kind, int c1, int c2, boolean selected, Runnable onClick) {
        FrameLayout cell = new FrameLayout(this);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
        lp.width = 0;
        lp.height = dp(76);
        cell.setLayoutParams(lp);

        TextView dot = new TextView(this);
        dot.setGravity(Gravity.CENTER);
        dot.setTextSize(22f);
        dot.setTextColor(Color.WHITE);
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        switch (kind) {
            case SWATCH_GRADIENT:
                d.setOrientation(GradientDrawable.Orientation.TL_BR);
                d.setColors(new int[]{c1, c2});
                break;
            case SWATCH_CUSTOM:
                d.setColor(0xFF15171B);
                dot.setText("＋");
                dot.setTextSize(26f);
                break;
            case SWATCH_ORIGINAL:
                d.setOrientation(GradientDrawable.Orientation.TL_BR);
                d.setColors(new int[]{0xFFFF3D9A, 0xFF6C2BFF});
                dot.setText("A");
                dot.setTypeface(null, android.graphics.Typeface.BOLD);
                break;
            case SWATCH_WALLPAPER:
                d.setColor(c1);
                dot.setText("✦");
                break;
            default:
                d.setColor(c1);
                break;
        }
        boolean ring = selected || kind == SWATCH_CUSTOM;
        d.setStroke(dp(ring ? 3 : 1), ring ? 0xFFFFFFFF : 0x33FFFFFF);
        dot.setBackground(d);
        FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.CENTER);
        cell.addView(dot, dlp);
        if (selected && kind != SWATCH_CUSTOM) {
            // Thin gap + outer ring, so the selection reads on any colour.
            View halo = new View(this);
            GradientDrawable h = new GradientDrawable();
            h.setShape(GradientDrawable.OVAL);
            h.setStroke(dp(2), themeColor(androidx.appcompat.R.attr.colorPrimary, 0xFF6C7BFF));
            halo.setBackground(h);
            cell.addView(halo, new FrameLayout.LayoutParams(dp(66), dp(66), Gravity.CENTER));
        }
        cell.setOnClickListener(v -> onClick.run());
        return cell;
    }

    /** Hue / saturation / brightness sliders with a live swatch and an editable hex code. */
    private void pickCustomColor(String current, java.util.function.Consumer<String> onPick) {
        int start = 0xFF6C7BFF;
        try {
            if (!isBlank(current) && current.startsWith("#")) start = Color.parseColor(current);
        } catch (Exception ignored) {}
        final float[] hsv = new float[3];
        Color.colorToHSV(start, hsv);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(22), dp(12), dp(22), dp(4));
        View sample = new View(this);
        GradientDrawable sd = new GradientDrawable();
        sd.setCornerRadius(dp(16));
        sample.setBackground(sd);
        col.addView(sample, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        EditText hexInput = new EditText(this);
        hexInput.setSingleLine(true);
        col.addView(hexInput);

        final boolean[] syncing = {false};
        Runnable refresh = () -> {
            int c = Color.HSVToColor(hsv);
            sd.setColor(c);
            syncing[0] = true;
            hexInput.setText(hex(c));
            syncing[0] = false;
        };
        String[] names = {"Hue", "Saturation", "Brightness"};
        float[] max = {360f, 1f, 1f};
        Slider[] sliders = new Slider[3];
        for (int i = 0; i < 3; i++) {
            final int k = i;
            col.addView(sectionLabel(names[i]));
            Slider sl = new Slider(this);
            sl.setValueFrom(0f);
            sl.setValueTo(max[i]);
            sl.setValue(Math.max(0f, Math.min(max[i], hsv[i])));
            sl.addOnChangeListener((x, v, user) -> {
                if (!user) return;
                hsv[k] = v;
                refresh.run();
            });
            sliders[i] = sl;
            col.addView(sl);
        }
        hexInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence a, int b, int c, int d) {}
            @Override public void onTextChanged(CharSequence a, int b, int c, int d) {}
            @Override public void afterTextChanged(android.text.Editable e) {
                if (syncing[0]) return;
                try {
                    int c = Color.parseColor(e.toString().trim());
                    Color.colorToHSV(c, hsv);
                    sd.setColor(c);
                    for (int i = 0; i < 3; i++) sliders[i].setValue(Math.max(0f, Math.min(max[i], hsv[i])));
                } catch (Exception ignored) {}
            }
        });
        refresh.run();

        new MaterialAlertDialogBuilder(this)
                .setTitle("Custom colour")
                .setView(col)
                .setPositiveButton("Apply", (dlg, w) -> onPick.accept(hex(Color.HSVToColor(hsv))))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---------------------------------------------------------------- Background

    /** Background: Original / Solid / Gradient / None, a swatch grid, gradient angle, corners. */
    private View backgroundPanel() {
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, dp(10), 0, dp(12));

        String mode = style.backgroundMode == null ? "keep" : style.backgroundMode;
        LinearLayout modeRow = centeredRow();
        modeRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        String[] modes = {"keep", "solid", "gradient", "transparent"};
        String[] labels = {"Original", "Solid", "Gradient", "None"};
        for (int i = 0; i < modes.length; i++) {
            final String m = modes[i];
            TextView chip = pill(labels[i], mode.equals(m));
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.setMargins(dp(4), 0, dp(4), 0);
            chip.setLayoutParams(clp);
            chip.setOnClickListener(v -> {
                style.backgroundMode = m;
                if ("keep".equals(m)) {
                    style.backgroundColor = null; // show the design's own background
                    style.gradientEndColor = null;
                }
                render();
                selectTab(selectedTab);
            });
            modeRow.addView(chip);
        }
        col.addView(modeRow);

        GridLayout grid = swatchGrid();
        grid.setPadding(dp(10), dp(8), dp(10), dp(8));
        boolean original = "keep".equals(mode) && isBlank(style.backgroundColor);
        grid.addView(swatch(SWATCH_CUSTOM, 0, 0, false, () -> pickCustomColor(style.backgroundColor, h -> {
            style.backgroundMode = "solid";
            style.backgroundColor = h;
            render();
            selectTab(selectedTab);
        })));
        grid.addView(swatch(SWATCH_ORIGINAL, 0, 0, original, () -> {
            style.backgroundMode = "keep";
            style.backgroundColor = null;
            style.gradientEndColor = null;
            render();
            selectTab(selectedTab);
        }));
        int wall = StyleApplier.wallpaperAccent();
        grid.addView(swatch(SWATCH_WALLPAPER, scaleRgb(wall, 0.35f), 0, false, () -> {
            style.backgroundMode = "gradient";
            style.backgroundColor = toHex(scaleRgb(wall, 0.45f));
            style.gradientEndColor = toHex(scaleRgb(wall, 0.15f));
            render();
            selectTab(selectedTab);
        }));
        for (int[] g : GRADIENTS) {
            boolean sel = "gradient".equals(mode) && matches(style.backgroundColor, g[0])
                    && matches(style.gradientEndColor, g[1]);
            grid.addView(swatch(SWATCH_GRADIENT, g[0], g[1], sel, () -> {
                style.backgroundMode = "gradient";
                style.backgroundColor = hex(g[0]);
                style.gradientEndColor = hex(g[1]);
                render();
                selectTab(selectedTab);
            }));
        }
        for (int color : PALETTE) {
            boolean sel = "solid".equals(mode) && matches(style.backgroundColor, color);
            grid.addView(swatch(SWATCH_SOLID, color, 0, sel, () -> {
                style.backgroundMode = "solid";
                style.backgroundColor = hex(color);
                render();
                selectTab(selectedTab);
            }));
        }
        col.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout sliders = new LinearLayout(this);
        sliders.setOrientation(LinearLayout.VERTICAL);
        sliders.setPadding(dp(22), 0, dp(22), 0);
        if ("gradient".equals(mode)) {
            TextView angleLabel = sectionLabel("Gradient angle  " + Math.round(style.gradientAngle) + "°");
            sliders.addView(angleLabel);
            Slider angle = new Slider(this);
            angle.setValueFrom(0f);
            angle.setValueTo(360f);
            angle.setStepSize(15f);
            angle.setValue(snap(style.gradientAngle, 0f, 360f, 15f));
            angle.addOnChangeListener((sl, v, u) -> {
                style.gradientAngle = v;
                angleLabel.setText("Gradient angle  " + Math.round(v) + "°");
                render();
            });
            sliders.addView(angle);
        }
        float corner = style.cornerRadius >= 0f ? style.cornerRadius : designCorner();
        TextView cornerLabel = sectionLabel("Corners  " + Math.round(corner * 200) + "%");
        sliders.addView(cornerLabel);
        Slider corners = new Slider(this);
        corners.setValueFrom(0f);
        corners.setValueTo(0.5f);
        corners.setStepSize(0.02f);
        corners.setValue(snap(corner, 0f, 0.5f, 0.02f));
        corners.addOnChangeListener((sl, v, u) -> {
            style.cornerRadius = v;
            cornerLabel.setText("Corners  " + Math.round(v * 200) + "%");
            render();
        });
        sliders.addView(corners);
        col.addView(sliders);

        scroll.addView(col);
        return scroll;
    }

    private float designCorner() {
        return parsedSpec != null && parsedSpec.background != null ? parsedSpec.background.cornerRadius : 0.16f;
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

    // ---------------------------------------------------------------- Actions

    private void setupActions() {
        findViewById(R.id.cw_close).setOnClickListener(v -> finish());
        findViewById(R.id.cw_reset).setOnClickListener(v -> resetStyle());
        findViewById(R.id.cw_add_home).setOnClickListener(v -> {
            if (savesLook()) saveLook();
            else addToHome();
        });
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

    /** "ADD NEW" / "SAVE": stores this look on the widget page and returns there. */
    private void saveLook() {
        String json = GSON.toJson(style);
        String id = variantId;
        if (id != null) {
            VariantStore.update(this, widgetId, id, json);
            Toast.makeText(this, "Look saved", Toast.LENGTH_SHORT).show();
        } else {
            id = VariantStore.add(this, widgetId, json).id;
            Toast.makeText(this, "Added to your looks", Toast.LENGTH_SHORT).show();
        }
        setResult(RESULT_OK, new Intent().putExtra(EXTRA_VARIANT_ID, id));
        finish();
    }

    private void resetStyle() {
        WidgetStyle fresh = new WidgetStyle();
        style.props = null;
        style.styleOption = -1;
        style.tapPackage = null;
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
