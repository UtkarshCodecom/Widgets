package com.desire.widget.engine.data;

/**
 * Current media state consumed by the {@code music} component. Populated by a provider or
 * overridden per-widget from component props for previews.
 */
public class NowPlaying {
    public String title = "Not Playing";
    public String artist = "—";
    public boolean playing = false;
    /** 0..1 track position at {@link #updatedAtMillis} (fallback when duration is unknown). */
    public float progress = 0f;
    /** Track position/duration in ms at {@link #updatedAtMillis}; 0 when the session omits them. */
    public long positionMillis = 0;
    public long durationMillis = 0;
    /** Epoch ms when this snapshot was cached; 0 = no media session data has ever been cached.
     *  While playing, renderers extrapolate position + (now - updatedAt) so the progress bar
     *  keeps moving between notification updates. */
    public long updatedAtMillis = 0;
    /** Absolute file path to a cached album-art PNG, or null if none is available. */
    public String albumArtPath = null;
}
