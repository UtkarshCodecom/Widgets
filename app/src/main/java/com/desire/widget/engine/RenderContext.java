package com.desire.widget.engine;

import com.desire.widget.engine.data.LiveDataSource;

/**
 * Everything a render pass needs beyond the spec itself: the target pixel size, the active theme,
 * the clock instant, an application Context for live-data components (battery, etc.), and an
 * optional {@link LiveDataSource} for weather/now-playing.
 *
 * <p>The same RenderContext shape is used for the gallery preview and the installed widget — only
 * the pixel size differs — which is the mechanism that keeps previews identical to real widgets.
 */
public class RenderContext {
    public final android.content.Context android;
    public final int widthPx;
    public final int heightPx;
    public final RenderTheme theme;
    public final long nowMillis;
    /** May be null; renderers fall back to component props when absent. */
    public final LiveDataSource live;

    public RenderContext(android.content.Context context, int widthPx, int heightPx,
                         RenderTheme theme, long nowMillis) {
        this(context, widthPx, heightPx, theme, nowMillis, null);
    }

    public RenderContext(android.content.Context context, int widthPx, int heightPx,
                         RenderTheme theme, long nowMillis, LiveDataSource live) {
        this.android = context.getApplicationContext();
        this.widthPx = widthPx;
        this.heightPx = heightPx;
        this.theme = theme != null ? theme : RenderTheme.defaultDark();
        this.nowMillis = nowMillis;
        this.live = live;
    }
}
