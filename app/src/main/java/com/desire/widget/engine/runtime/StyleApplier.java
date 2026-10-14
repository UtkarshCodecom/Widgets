package com.desire.widget.engine.runtime;

import android.graphics.Color;

import com.desire.widget.engine.RenderTheme;
import com.desire.widget.engine.model.BackgroundSpec;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.model.Frame;
import com.desire.widget.engine.model.WidgetSpec;
import com.desire.widget.engine.model.WidgetStyle;
import com.desire.widget.engine.util.SpecColors;

import java.util.HashMap;

/**
 * Applies a per-widget {@link WidgetStyle} at render time (the Muviz-style editor overrides):
 * derives a {@link RenderTheme} from Color 1/2/3, adjusts the background (keeping the design's
 * gradient/solid unless the user changes it), applies transparency, font, photo, a city label,
 * a style variant, and a content Size/Alignment transform — all WITHOUT editing the component JSON.
 *
 * <p>Pass a freshly-parsed spec (callers re-parse per render); this mutates it in place.
 */
public final class StyleApplier {
    private StyleApplier() {}

    public static RenderTheme apply(WidgetSpec spec, WidgetStyle style, RenderTheme base) {
        RenderTheme theme = RenderTheme.copy(base);
        if (style == null) return theme;

        if (notBlank(style.accentColor)) theme.primary = parse(style.accentColor, theme.primary);
        if (notBlank(style.textColor)) {
            theme.textPrimary = parse(style.textColor, theme.textPrimary);
            theme.textSecondary = withAlpha(theme.textPrimary, 0.7f);
        }
        if (notBlank(style.backgroundColor)) theme.surface = parse(style.backgroundColor, theme.surface);
        if (notBlank(style.fontFamily)) theme.fontFamily = style.fontFamily;

        if (spec == null) return theme;

        injectPhoto(spec, style);
        injectCity(spec, style);
        recolor(spec, style);
        applyBackground(spec, style, theme);
        applyLayout(spec, style);
        injectStickers(spec, style); // after applyLayout so stickers aren't scaled/nudged with content
        return theme;
    }

    /**
     * Directly recolors component props from the Color 1 (accent) / Color 2 (text) overrides.
     * Designs mostly use literal colors (not "@tokens"), so token resolution alone wouldn't change
     * anything — this makes the color pickers visibly work on every widget.
     */
    private static void recolor(WidgetSpec spec, WidgetStyle style) {
        if (spec.components == null) return;
        boolean t = notBlank(style.textColor);
        boolean a = notBlank(style.accentColor);
        if (!t && !a) return;
        for (ComponentSpec c : spec.components) {
            if (c == null || c.type == null) continue;
            if (c.props == null) c.props = new HashMap<>();
            if (t) {
                switch (c.type) {
                    case "text": case "clock": case "word_clock":
                        c.props.put("color", style.textColor); break;
                    case "flip_clock":
                        c.props.put("textColor", style.textColor); break;
                    case "countdown":
                        c.props.put("timeColor", style.textColor);
                        c.props.put("labelColor", style.textColor); break;
                    case "weather": case "battery": case "progress_ring":
                        c.props.put("textColor", style.textColor); break;
                    case "analog_clock":
                        c.props.put("handColor", style.textColor);
                        c.props.put("tickColor", style.textColor); break;
                    default: break;
                }
            }
            if (a) {
                switch (c.type) {
                    case "progress_ring": c.props.put("progressColor", style.accentColor); break;
                    case "analog_clock": c.props.put("secondColor", style.accentColor); break;
                    case "divider": c.props.put("color", style.accentColor); break;
                    case "music": c.props.put("accentColor", style.accentColor); break;
                    case "music_visualizer": c.props.put("barColor", style.accentColor); break;
                    case "countdown": c.props.put("accentColor", style.accentColor); break;
                    case "weather": c.props.put("iconColor", style.accentColor); break;
                    default: break;
                }
            }
        }
    }

    private static void applyBackground(WidgetSpec spec, WidgetStyle style, RenderTheme theme) {
        if (spec.background == null) spec.background = new BackgroundSpec();
        BackgroundSpec bg = spec.background;
        float op = clamp01(style.backgroundOpacity);
        String mode = style.backgroundMode == null ? "keep" : style.backgroundMode;

        if (style.styleVariant == 1 && style.cornerRadius < 0f) {
            bg.cornerRadius = 0.32f; // "Style 2": softer, more rounded card
        }

        if ("transparent".equals(mode)) {
            bg.fill = "#00000000";
            bg.gradientStart = null;
            bg.gradientEnd = null;
        } else if ("keep".equals(mode) && !notBlank(style.backgroundColor)) {
            // No background color chosen: preserve the design's own gradient/solid, apply opacity.
            if (bg.gradientStart != null && bg.gradientEnd != null) {
                int start = SpecColors.resolve(bg.gradientStart, theme, theme.surface);
                int end = SpecColors.resolve(bg.gradientEnd, theme, theme.background);
                bg.gradientStart = hex(withAlpha(start, op));
                bg.gradientEnd = hex(withAlpha(end, op));
            } else {
                int baseColor = SpecColors.resolve(bg.fill, theme, theme.surface);
                bg.fill = hex(withAlpha(baseColor, op));
            }
        } else if ("gradient".equals(mode)) {
            int start = notBlank(style.backgroundColor) ? parse(style.backgroundColor, theme.surface) : theme.surface;
            int end = notBlank(style.gradientEndColor) ? parse(style.gradientEndColor, start) : theme.background;
            bg.gradientStart = hex(withAlpha(start, op));
            bg.gradientEnd = hex(withAlpha(end, op));
            bg.gradientAngle = style.gradientAngle;
        } else {
            // solid — OR "keep" with an explicit Color 3 chosen (a picked color means solid, not a
            // lingering gradient, which was the "Color 3 still looks like a gradient" bug).
            int baseColor = notBlank(style.backgroundColor)
                    ? parse(style.backgroundColor, theme.surface)
                    : SpecColors.resolve(bg.fill, theme, theme.surface);
            bg.gradientStart = null;
            bg.gradientEnd = null;
            bg.fill = hex(withAlpha(baseColor, op));
        }
        if (style.cornerRadius >= 0f) bg.cornerRadius = style.cornerRadius;
    }

    /** Scales content around the center (Size) and nudges it (Alignment). */
    private static void applyLayout(WidgetSpec spec, WidgetStyle style) {
        float s = style.contentScale;
        float ox = style.alignX * 0.12f;
        float oy = style.alignY * 0.12f;
        if (s == 1f && ox == 0f && oy == 0f) return;
        if (spec.components == null) return;
        for (ComponentSpec c : spec.components) {
            if (c == null || c.frame == null) continue;
            Frame f = c.frame;
            float cx = f.x + f.w / 2f;
            float cy = f.y + f.h / 2f;
            float ncx = 0.5f + (cx - 0.5f) * s + ox;
            float ncy = 0.5f + (cy - 0.5f) * s + oy;
            f.w = f.w * s;
            f.h = f.h * s;
            f.x = ncx - f.w / 2f;
            f.y = ncy - f.h / 2f;
        }
    }

    private static void injectPhoto(WidgetSpec spec, WidgetStyle style) {
        // Slideshow: the frame for "now" (renders run every minute, so it advances on its own).
        String photo = style.currentPhoto(System.currentTimeMillis());
        if (!notBlank(photo)) return;
        if (spec.components == null) spec.components = new java.util.ArrayList<>();
        // A widget that already has an image component (e.g. a photo frame) reuses it.
        for (ComponentSpec c : spec.components) {
            if (c != null && "image".equals(c.type)) {
                if (c.props == null) c.props = new HashMap<>();
                c.props.put("source", photo);
                return;
            }
        }
        // Otherwise turn the photo into a full-bleed background BEHIND everything. The engine
        // already clips to the rounded card, so no per-image corner is needed.
        ComponentSpec bg = new ComponentSpec();
        bg.type = "image";
        bg.frame = new Frame(0f, 0f, 1f, 1f);
        bg.props = new HashMap<>();
        bg.props.put("source", photo);
        bg.props.put("scaleType", "crop");
        bg.props.put("cornerRadius", 0f);
        spec.components.add(0, bg);
    }

    /**
     * Appends each placed sticker as a "fit" image on top of the widget. The frame is the WHOLE
     * widget (so a rotated sticker's corners aren't clipped by a tight frame rect); position, size
     * and rotation travel as props the ImageRenderer applies: cx/cy (normalized centre), boxW
     * (square box width as a fraction of the widget width), rotation (degrees).
     */
    private static void injectStickers(WidgetSpec spec, WidgetStyle style) {
        if (style.stickers == null || style.stickers.isEmpty()) return;
        if (spec.components == null) spec.components = new java.util.ArrayList<>();
        for (WidgetStyle.Sticker st : style.stickers) {
            if (st == null || !notBlank(st.source)) continue;
            ComponentSpec c = new ComponentSpec();
            c.type = "image";
            c.frame = new Frame(0f, 0f, 1f, 1f);
            c.props = new HashMap<>();
            c.props.put("source", st.source);
            c.props.put("scaleType", "fit");
            c.props.put("cx", st.x);
            c.props.put("cy", st.y);
            c.props.put("boxW", Math.max(0.05f, Math.min(1.5f, st.scale)));
            c.props.put("rotation", st.rotation);
            spec.components.add(c);
        }
    }

    private static void injectCity(WidgetSpec spec, WidgetStyle style) {
        if (!notBlank(style.cityName) || spec.components == null) return;
        for (ComponentSpec c : spec.components) {
            if (c != null && "weather".equals(c.type)) {
                if (c.props == null) c.props = new HashMap<>();
                c.props.put("location", style.cityName);
                return;
            }
        }
    }

    /** Reads the current weather location label from the spec (for the City tab default). */
    public static String weatherLocation(WidgetSpec spec) {
        if (spec == null || spec.components == null) return "";
        for (ComponentSpec c : spec.components) {
            if (c != null && "weather".equals(c.type) && c.props != null) {
                Object v = c.props.get("location");
                if (v != null) return v.toString();
            }
        }
        return "";
    }

    public static boolean hasWeather(WidgetSpec spec) {
        if (spec == null || spec.components == null) return false;
        for (ComponentSpec c : spec.components) {
            if (c != null && "weather".equals(c.type)) return true;
        }
        return false;
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
