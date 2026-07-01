package com.desire.widget.engine.model;

/**
 * A global theme. The ThemeEngine turns this into a {@link com.desire.widget.engine.RenderTheme}
 * which the engine uses to resolve "@token" colors at render time. Themes are applied WITHOUT
 * mutating any WidgetSpec — the same widget JSON renders under any theme.
 */
public class ThemeSpec {
    public String id;
    public String name;
    public String primary = "#FFD700";
    public String onPrimary = "#000000";
    public String background = "#0D0D0D";
    public String surface = "#1A1A1A";
    public String textPrimary = "#FFFFFFFF";
    public String textSecondary = "#B3FFFFFF";
    public String fontFamily;
}
