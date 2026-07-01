package com.desire.widget.engine.util;

import android.graphics.Color;

import com.desire.widget.engine.RenderTheme;

/**
 * Resolves a spec color string to an int. Supports literal colors ("#RRGGBB" / "#AARRGGBB") and
 * theme tokens ("@primary", "@onPrimary", "@background", "@surface", "@textPrimary",
 * "@textSecondary"). Token resolution at render time is how a theme recolors a widget without the
 * widget JSON ever changing.
 */
public final class SpecColors {
    private SpecColors() {}

    public static int resolve(String value, RenderTheme theme, int fallback) {
        if (value == null || value.isEmpty()) return fallback;
        try {
            if (value.charAt(0) == '@') {
                switch (value.substring(1)) {
                    case "primary": return theme.primary;
                    case "onPrimary": return theme.onPrimary;
                    case "background": return theme.background;
                    case "surface": return theme.surface;
                    case "textPrimary": return theme.textPrimary;
                    case "textSecondary": return theme.textSecondary;
                    default: return fallback;
                }
            }
            return Color.parseColor(value);
        } catch (Exception e) {
            return fallback;
        }
    }
}
