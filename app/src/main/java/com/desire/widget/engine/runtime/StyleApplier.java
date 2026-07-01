package com.desire.widget.engine.runtime;

import android.graphics.Color;

import com.desire.widget.engine.RenderTheme;
import com.desire.widget.engine.model.BackgroundSpec;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.model.WidgetStyle;
import com.desire.widget.engine.util.SpecColors;

import java.util.HashMap;

/**
 * Applies a per-widget {@link WidgetStyle} at render time: it derives a {@link RenderTheme} from
 * the chosen accent/text/background colors (so "@token" colors in the design recolor) and rewrites
 * the spec's {@link BackgroundSpec} for the chosen background mode, opacity, and corner radius.
 *
 * <p>The component JSON itself is never touched — only the theme and background — which is what
 * lets one design be re-styled per placement.
 */
public final class StyleApplier {
    private StyleApplier() {}

    /**
     * @return a RenderTheme reflecting the style; also mutates {@code spec.background} in place.
     *         Pass a freshly-parsed spec (callers re-parse per render), never a shared instance.
     */
    public static RenderTheme apply(com.desire.widget.engine.model.WidgetSpec spec,
                                    WidgetStyle style, RenderTheme base) {
        RenderTheme theme = RenderTheme.copy(base);
        if (style == null) return theme;

        if (notBlank(style.accentColor)) theme.primary = parse(style.accentColor, theme.primary);
        if (notBlank(style.textColor)) {
            theme.textPrimary = parse(style.textColor, theme.textPrimary);
            theme.textSecondary = withAlpha(theme.textPrimary, 0.7f);
        }
        if (notBlank(style.backgroundColor)) theme.surface = parse(style.backgroundColor, theme.surface);
        if (notBlank(style.fontFamily)) theme.fontFamily = style.fontFamily;

        // Inject a user-picked photo into the first image component, if any.
        if (spec != null && notBlank(style.photoUri) && spec.components != null) {
            for (ComponentSpec c : spec.components) {
                if (c != null && "image".equals(c.type)) {
                    if (c.props == null) c.props = new HashMap<>();
                    c.props.put("source", style.photoUri);
                    break;
                }
            }
        }

        if (spec != null) {
            if (spec.background == null) spec.background = new BackgroundSpec();
            BackgroundSpec bg = spec.background;
            String mode = style.backgroundMode == null ? "solid" : style.backgroundMode;
            float op = clamp01(style.backgroundOpacity);

            int baseColor = notBlank(style.backgroundColor)
                    ? parse(style.backgroundColor, theme.surface)
                    : SpecColors.resolve(bg.fill, theme, theme.surface);

            if ("transparent".equals(mode)) {
                bg.fill = "#00000000";
                bg.gradientStart = null;
                bg.gradientEnd = null;
            } else if ("gradient".equals(mode)) {
                int end = notBlank(style.gradientEndColor)
                        ? parse(style.gradientEndColor, baseColor) : baseColor;
                bg.gradientStart = hex(withAlpha(baseColor, op));
                bg.gradientEnd = hex(withAlpha(end, op));
            } else {
                bg.gradientStart = null;
                bg.gradientEnd = null;
                bg.fill = hex(withAlpha(baseColor, op));
            }
            if (style.cornerRadius >= 0f) bg.cornerRadius = style.cornerRadius;
        }
        return theme;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static int parse(String hex, int fallback) {
        try {
            return Color.parseColor(hex);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static int withAlpha(int color, float frac) {
        int a = Math.round(Math.max(0f, Math.min(1f, frac)) * 255f);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static String hex(int color) {
        return String.format("#%08X", color);
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
