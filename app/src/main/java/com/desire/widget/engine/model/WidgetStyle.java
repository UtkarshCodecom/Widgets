package com.desire.widget.engine.model;

/**
 * A per-widget visual override chosen in the Customize screen (mirrors the Muviz editor tabs). It
 * is stored alongside the {@link WidgetSpec} and applied at render time by StyleApplier, so the
 * same design can be re-styled per placement WITHOUT editing the widget's component JSON.
 *
 * <p>Blank/default fields mean "keep the design's default".
 */
public class WidgetStyle {
    /** Color 1 → overrides the "@primary" token. */
    public String accentColor;
    /** Color 2 → overrides the "@textPrimary" token. */
    public String textColor;
    /** Color 3 → base background color. */
    public String backgroundColor;
    public String gradientEndColor;
    /** null/"keep" | solid | gradient | transparent */
    public String backgroundMode;
    /** 0..1 fraction of the shorter side; &lt; 0 keeps the design default. */
    public float cornerRadius = -1f;
    /** Transparency tab: 0..1 applied to the background fill. */
    public float backgroundOpacity = 1f;
    /** Font family key (see FontResolver.FAMILIES). */
    public String fontFamily;
    /** content:// or file:// URI of a user-picked photo (legacy single photo). */
    public String photoUri;
    /** Photo slideshow: when non-empty, the widget cycles through these instead of photoUri. */
    public java.util.List<String> photoUris;
    /** Minutes each slideshow photo stays up before the next one. */
    public int photoIntervalMin = 60;
    /** Size tab: scales widget content around its center (1 = original). */
    public float contentScale = 1f;
    /** Alignment tab: -1/0/1 nudge of content on each axis. */
    public int alignX = 0;
    public int alignY = 0;
    /** Style tab: 0 = design default, 1 = alternate look. */
    public int styleVariant = 0;
    /** City tab: overrides the location label on weather widgets. */
    public String cityName;
    /** Gradient angle in degrees (gradient background mode). */
    public float gradientAngle = 135f;
    /** Tap tab: package to open when the widget is tapped (overrides the design's action). */
    public String tapPackage;
    /** Stickers the user placed on this widget (admin-provided art, positioned by dragging). */
    public java.util.List<Sticker> stickers;
    /**
     * Per-component prop overrides from the editor's widget-specific tabs ("Hour Color",
     * "Clock Style"…), keyed {@code "<componentIndex>.<prop>"} → value (String, Boolean, Number or
     * List, as in the spec JSON). Indices refer to the design's own component list. Colour values
     * may be gradients (see SpecPaint) or "@wallpaper".
     */
    public java.util.Map<String, Object> props;
    /** Which built-in "Style N" option is active (-1 = the design's own look). UI state only. */
    public int styleOption = -1;

    /**
     * A placed sticker: an admin-provided image drawn on top of the widget at a user-chosen
     * position and size. Coordinates are normalized (0..1) to the widget so they scale with it.
     */
    public static class Sticker {
        /** drawable name, "asset://stickers/foo.png", "http(s)://…", or "content://…". */
        public String source;
        /** Normalized centre of the sticker within the widget. */
        public float x = 0.5f;
        public float y = 0.5f;
        /** Width as a fraction of the widget width (height follows the image's aspect ratio). */
        public float scale = 0.3f;
        /** Clockwise rotation in degrees. */
        public float rotation = 0f;

        public Sticker() {}

        public Sticker(String source) { this.source = source; }
    }

    public boolean isEmpty() {
        return isBlank(accentColor) && isBlank(textColor) && isBlank(backgroundColor)
                && isBlank(gradientEndColor)
                && cornerRadius < 0f && backgroundOpacity >= 1f
                && isKeepMode()
                && isBlank(fontFamily) && isBlank(photoUri)
                && (photoUris == null || photoUris.isEmpty())
                && contentScale == 1f && alignX == 0 && alignY == 0
                && styleVariant == 0 && isBlank(cityName) && isBlank(tapPackage)
                && (stickers == null || stickers.isEmpty())
                && (props == null || props.isEmpty());
    }

    /** The photo to show right now: the current slideshow frame, else the single photo. */
    public String currentPhoto(long nowMillis) {
        if (photoUris != null && !photoUris.isEmpty()) {
            long slot = nowMillis / (Math.max(1, photoIntervalMin) * 60_000L);
            return photoUris.get((int) (slot % photoUris.size()));
        }
        return photoUri;
    }

    public boolean isKeepMode() {
        return backgroundMode == null || "keep".equals(backgroundMode);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
