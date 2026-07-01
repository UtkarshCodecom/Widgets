package com.desire.widget.engine.model;

/**
 * Widget canvas background. Solid fill OR a two-stop linear gradient. Colors may be literals
 * ("#RRGGBB" / "#AARRGGBB") or theme tokens ("@surface", "@background"). cornerRadius is a
 * fraction of the widget's shorter side so it scales with size.
 */
public class BackgroundSpec {
    public String fill = "@surface";
    public String gradientStart;
    public String gradientEnd;
    public float gradientAngle = 135f;
    public float cornerRadius = 0.08f;
    public String imageUrl;
    public float opacity = 1f;
}
