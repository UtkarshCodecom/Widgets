package com.desire.widget.engine.model;

/**
 * A per-widget visual override chosen in the Customize screen. It is stored alongside the
 * {@link WidgetSpec} and applied at render time by StyleApplier, so the same design can be
 * recolored/re-styled per placement WITHOUT editing the widget's component JSON.
 *
 * <p>Null/blank color fields mean "keep the design's default" (theme token). This mirrors how
 * gallery widget apps let you tint a whole widget with a couple of taps.
 */
public class WidgetStyle {
    /** Overrides the "@primary" token when set. */
    public String accentColor;
    /** Overrides the "@textPrimary" token when set. */
    public String textColor;
    /** Base background color when set (else the design's own background is kept). */
    public String backgroundColor;
    /** Second gradient stop (gradient mode only). */
    public String gradientEndColor;
    /** solid | gradient | transparent */
    public String backgroundMode = "solid";
    /** 0..1 fraction of the shorter side; &lt; 0 keeps the design default. */
    public float cornerRadius = -1f;
    /** 0..1 applied to the background fill. */
    public float backgroundOpacity = 1f;
    /** Font family key (see FontResolver.FAMILIES); applies to all text in the widget. */
    public String fontFamily;
    /** content:// or file:// URI of a user-picked photo, injected into the first image component. */
    public String photoUri;

    public boolean isEmpty() {
        return isBlank(accentColor) && isBlank(textColor) && isBlank(backgroundColor)
                && cornerRadius < 0f && backgroundOpacity >= 1f
                && (backgroundMode == null || "solid".equals(backgroundMode))
                && isBlank(fontFamily) && isBlank(photoUri);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
