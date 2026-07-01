package com.desire.widget.engine;

import android.graphics.Color;

import com.desire.widget.engine.model.ThemeSpec;

/**
 * Resolved (int-color) form of a {@link ThemeSpec}, ready for fast use during rendering.
 * Produced by the ThemeEngine and passed into every render call.
 */
public class RenderTheme {
    public int primary;
    public int onPrimary;
    public int background;
    public int surface;
    public int textPrimary;
    public int textSecondary;
    public String fontFamily;

    private static int parse(String s, int def) {
        try {
            return Color.parseColor(s);
        } catch (Exception e) {
            return def;
        }
    }

    public static RenderTheme fromSpec(ThemeSpec s) {
        if (s == null) return defaultDark();
        RenderTheme t = new RenderTheme();
        t.primary = parse(s.primary, 0xFFFFD700);
        t.onPrimary = parse(s.onPrimary, 0xFF000000);
        t.background = parse(s.background, 0xFF0D0D0D);
        t.surface = parse(s.surface, 0xFF1A1A1A);
        t.textPrimary = parse(s.textPrimary, 0xFFFFFFFF);
        t.textSecondary = parse(s.textSecondary, 0xB3FFFFFF);
        t.fontFamily = s.fontFamily;
        return t;
    }

    public static RenderTheme copy(RenderTheme s) {
        if (s == null) return defaultDark();
        RenderTheme t = new RenderTheme();
        t.primary = s.primary;
        t.onPrimary = s.onPrimary;
        t.background = s.background;
        t.surface = s.surface;
        t.textPrimary = s.textPrimary;
        t.textSecondary = s.textSecondary;
        t.fontFamily = s.fontFamily;
        return t;
    }

    public static RenderTheme defaultDark() {
        RenderTheme t = new RenderTheme();
        t.primary = 0xFFFFD700;
        t.onPrimary = 0xFF000000;
        t.background = 0xFF0D0D0D;
        t.surface = 0xFF1A1A1A;
        t.textPrimary = 0xFFFFFFFF;
        t.textSecondary = 0xB3FFFFFF;
        return t;
    }
}
