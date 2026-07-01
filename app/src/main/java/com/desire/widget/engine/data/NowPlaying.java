package com.desire.widget.engine.data;

/**
 * Current media state consumed by the {@code music} component. Populated by a provider or
 * overridden per-widget from component props for previews.
 */
public class NowPlaying {
    public String title = "Not Playing";
    public String artist = "—";
    public boolean playing = false;
    /** 0..1 track position. */
    public float progress = 0f;
}
