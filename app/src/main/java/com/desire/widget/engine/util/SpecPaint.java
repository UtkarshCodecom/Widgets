package com.desire.widget.engine.util;

import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import com.desire.widget.engine.RenderTheme;

/**
 * Colour values that can also be gradients, for the props the editor's colour grid writes.
 *
 * <p>A gradient value is {@code "gradient:#RRGGBB,#RRGGBB[,angle]"} (angle in degrees, default
 * 135 = top-left → bottom-right). Anything else is a plain colour/token resolved by
 * {@link SpecColors}. Renderers that support gradients call {@link #apply}; the rest can call
 * {@link #solid} and get the gradient's first stop.
 */
public final class SpecPaint {
    public static final String PREFIX = "gradient:";

    private SpecPaint() {}

    public static boolean isGradient(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    public static String gradient(int start, int end) {
        return PREFIX + hex(start) + "," + hex(end);
    }

    /** Sets {@code paint}'s colour or gradient shader, spread over {@code bounds}. */
    public static void apply(Paint paint, String value, RectF bounds, RenderTheme theme, int fallback) {
        int[] stops = stops(value, theme);
        if (stops == null) {
            paint.setShader(null);
            paint.setColor(SpecColors.resolve(value, theme, fallback));
            return;
        }
        double ang = Math.toRadians(angle(value));
        float dx = (float) Math.cos(ang) * bounds.width() / 2f;
        float dy = (float) Math.sin(ang) * bounds.height() / 2f;
        paint.setColor(Color.WHITE); // the shader supplies the colour; keep alpha opaque
        paint.setShader(new LinearGradient(bounds.centerX() - dx, bounds.centerY() - dy,
                bounds.centerX() + dx, bounds.centerY() + dy, stops[0], stops[1], Shader.TileMode.CLAMP));
    }

    /** A single colour for renderers without gradient support (the gradient's first stop). */
    public static int solid(String value, RenderTheme theme, int fallback) {
        int[] stops = stops(value, theme);
        return stops != null ? stops[0] : SpecColors.resolve(value, theme, fallback);
    }

    private static int[] stops(String value, RenderTheme theme) {
        if (!isGradient(value)) return null;
        String[] parts = value.substring(PREFIX.length()).split(",");
        if (parts.length < 2) return null;
        int a = SpecColors.resolve(parts[0].trim(), theme, Color.WHITE);
        int b = SpecColors.resolve(parts[1].trim(), theme, a);
        return new int[]{a, b};
    }

    private static float angle(String value) {
        String[] parts = value.substring(PREFIX.length()).split(",");
        if (parts.length >= 3) {
            try {
                return Float.parseFloat(parts[2].trim());
            } catch (NumberFormatException ignored) {}
        }
        return 135f;
    }

    private static String hex(int c) {
        return String.format("#%08X", c);
    }
}
