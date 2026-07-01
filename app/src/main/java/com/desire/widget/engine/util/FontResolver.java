package com.desire.widget.engine.util;

import android.graphics.Typeface;

/**
 * Maps a font-family key to a {@link Typeface}. Uses the platform's built-in font families (no
 * bundled binaries needed), which cover the styles gallery widget apps expose: default sans, serif,
 * mono, condensed, light, medium, thin, black.
 */
public final class FontResolver {
    private FontResolver() {}

    /** Font keys offered in the Customize screen. */
    public static final String[] FAMILIES = {
            "default", "serif", "monospace", "condensed", "light", "medium", "thin", "black"
    };

    public static Typeface resolve(String family, boolean bold) {
        int style = bold ? Typeface.BOLD : Typeface.NORMAL;
        if (family == null || family.isEmpty() || "default".equalsIgnoreCase(family)) {
            return bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT;
        }
        switch (family.toLowerCase()) {
            case "serif": return Typeface.create(Typeface.SERIF, style);
            case "mono":
            case "monospace": return Typeface.create(Typeface.MONOSPACE, style);
            case "condensed": return Typeface.create("sans-serif-condensed", style);
            case "light": return Typeface.create("sans-serif-light", style);
            case "medium": return Typeface.create("sans-serif-medium", style);
            case "thin": return Typeface.create("sans-serif-thin", style);
            case "black": return Typeface.create("sans-serif-black", style);
            default: return Typeface.create(Typeface.SANS_SERIF, style);
        }
    }
}
