package com.desire.widget.ui.customize;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.RenderTheme;
import com.desire.widget.engine.WidgetEngine;
import com.desire.widget.engine.data.CachedLiveDataSource;
import com.desire.widget.engine.model.WidgetSpec;
import com.desire.widget.engine.model.WidgetStyle;
import com.desire.widget.engine.runtime.StyleApplier;
import com.desire.widget.engine.runtime.ThemeEngine;
import com.desire.widget.ui.widgets.LivePreview;
import com.google.gson.Gson;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A widget preview WITH a user style applied (variant cards, style thumbnails, the big preview on
 * the widget page). Renders off the UI thread into two alternating bitmaps, and can be ticked
 * once a second so clocks run live.
 */
final class StyledPreview {
    private static final WidgetEngine ENGINE = new WidgetEngine();
    private static final Gson GSON = new Gson();
    private static final ExecutorService RENDERER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "styled-preview");
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final ImageView view;
    private final int maxPx;
    private Bitmap front, back;
    private boolean busy, dirty;
    private int token;

    private String specJson, styleJson;
    private int[] ratio = {1, 1};
    private long interval = LivePreview.STATIC;
    private long lastMinute = -1;

    StyledPreview(ImageView view, int maxPx) {
        this.view = view;
        this.maxPx = maxPx;
    }

    void bind(String specJson, String styleJson, int[] ratio) {
        boolean same = eq(specJson, this.specJson) && eq(styleJson, this.styleJson);
        this.specJson = specJson;
        this.styleJson = styleJson;
        this.ratio = ratio;
        if (same && front != null) return;
        token++;
        try {
            interval = LivePreview.intervalOf(GSON.fromJson(specJson, WidgetSpec.class));
        } catch (Exception e) {
            interval = LivePreview.STATIC;
        }
        render();
    }

    void tick(long now) {
        if (interval == LivePreview.EVERY_SECOND
                || (interval == LivePreview.EVERY_MINUTE && now / 60_000L != lastMinute)) {
            render();
        }
    }

    void render() {
        if (specJson == null) return;
        if (busy) {
            dirty = true;
            return;
        }
        int w = view.getWidth() - view.getPaddingLeft() - view.getPaddingRight();
        if (w <= 0) {
            view.post(this::render);
            return;
        }
        int h = view.getHeight() - view.getPaddingTop() - view.getPaddingBottom();
        // Fit the widget's aspect inside the view.
        int rw = w, rh = Math.max(1, w * ratio[1] / ratio[0]);
        if (h > 0 && rh > h) {
            rh = h;
            rw = Math.max(1, h * ratio[0] / ratio[1]);
        }
        float scale = Math.min(1f, maxPx / (float) Math.max(rw, rh));
        rw = Math.max(1, Math.round(rw * scale));
        rh = Math.max(1, Math.round(rh * scale));
        if (back == null || back.getWidth() != rw || back.getHeight() != rh) {
            back = Bitmap.createBitmap(rw, rh, Bitmap.Config.ARGB_8888);
        }

        final Bitmap target = back;
        final int myToken = token;
        final String spec = specJson, style = styleJson;
        final Context app = view.getContext().getApplicationContext();
        final int fw = rw, fh = rh;
        busy = true;
        RENDERER.execute(() -> {
            boolean ok;
            try {
                WidgetSpec s = GSON.fromJson(spec, WidgetSpec.class);
                RenderTheme theme = ThemeEngine.current(app);
                if (style != null && !style.isEmpty()) {
                    theme = StyleApplier.apply(s, GSON.fromJson(style, WidgetStyle.class), theme);
                }
                RenderContext rc = new RenderContext(app, fw, fh, theme, System.currentTimeMillis(),
                        new CachedLiveDataSource(app));
                target.eraseColor(Color.TRANSPARENT);
                ENGINE.render(s, rc, target);
                ok = true;
            } catch (Exception e) {
                ok = false;
            }
            final boolean success = ok;
            MAIN.post(() -> {
                busy = false;
                if (success && myToken == token) {
                    view.setImageBitmap(target);
                    back = front;
                    front = target;
                    lastMinute = System.currentTimeMillis() / 60_000L;
                } else if (success) {
                    dirty = true; // finished an outdated frame: draw the current one
                }
                if (dirty) {
                    dirty = false;
                    render();
                }
            });
        });
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
