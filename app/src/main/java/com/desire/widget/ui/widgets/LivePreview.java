package com.desire.widget.ui.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.WidgetEngine;
import com.desire.widget.engine.data.CachedLiveDataSource;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.model.WidgetSpec;
import com.desire.widget.engine.runtime.ThemeEngine;
import com.desire.widget.engine.util.SpecProps;
import com.google.gson.Gson;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Renders a widget spec into an ImageView and keeps it ticking in real time while it's on screen,
 * so clocks in the gallery move exactly like the placed widget (sweeping second hands, dancing
 * visualizer bars, minute flips).
 *
 * <p>Each slot double-buffers: it draws the next frame into the bitmap that is NOT on screen on a
 * background thread, then swaps — no per-tick allocation, no tearing of the visible frame.
 */
public final class LivePreview {
    /** Nothing in the spec changes with time. */
    public static final long STATIC = 0L;
    public static final long EVERY_SECOND = 1_000L;
    public static final long EVERY_MINUTE = 60_000L;

    private static final WidgetEngine ENGINE = new WidgetEngine();
    private static final Gson GSON = new Gson();
    private static final ExecutorService RENDERER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "widget-preview");
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final int MAX_PX = 720;

    private final ImageView view;
    private Bitmap front, back;
    private boolean busy, dirty;
    private int token;

    private String key;
    private String specJson;
    private WidgetSpec spec;
    private int[] ratio = {1, 1};
    private long interval = STATIC;
    private long lastRenderedMinute = -1;

    public LivePreview(ImageView view) {
        this.view = view;
    }

    /** How often a spec needs re-rendering to look live. */
    public static long intervalOf(WidgetSpec spec) {
        if (spec == null || spec.components == null) return STATIC;
        long best = STATIC;
        for (ComponentSpec c : spec.components) {
            if (c == null || c.type == null) continue;
            switch (c.type) {
                case "analog_clock":
                    if (SpecProps.b(c.props, "showSeconds", true)) return EVERY_SECOND;
                    best = EVERY_MINUTE;
                    break;
                case "pie_clock":
                case "music_visualizer":
                case "bezel_clock":
                case "year_progress":
                    return EVERY_SECOND;
                case "clock": {
                    String f = SpecProps.str(c.props, "format", "HH:mm");
                    if (f.contains("s")) return EVERY_SECOND;
                    best = EVERY_MINUTE;
                    break;
                }
                case "flip_clock":
                case "word_clock":
                case "stacked_clock":
                case "countdown":
                case "next_alarm":
                case "styled_clock":
                case "dot_clock":
                case "time_tiles":
                case "week_strip":
                    best = EVERY_MINUTE;
                    break;
                default:
                    break;
            }
        }
        return best;
    }

    /**
     * Shows {@code specJson} in the view. Re-binding the same key keeps the current frame (no
     * flash when the list refreshes, e.g. after toggling a favourite).
     */
    public void bind(String key, String specJson, int[] ratio) {
        if (key != null && key.equals(this.key) && (front != null || busy)) return;
        this.key = key;
        this.specJson = specJson;
        this.spec = null;
        this.interval = STATIC;
        this.ratio = ratio;
        this.lastRenderedMinute = -1;
        token++;
        view.setImageBitmap(null);
        if (front != null) { // the old frame is off screen now; reuse it as a back buffer
            if (back == null) back = front;
            front = null;
        }
        render();
    }

    /** Called once a second by the owner while this preview is visible. */
    public void tick(long now) {
        if (interval == EVERY_SECOND) {
            render();
        } else if (interval == EVERY_MINUTE && now / EVERY_MINUTE != lastRenderedMinute) {
            render();
        }
    }

    public void render() {
        if (specJson == null) return;
        if (busy) {
            dirty = true;
            return;
        }
        int w = view.getWidth() - view.getPaddingLeft() - view.getPaddingRight();
        if (w <= 0) { // not laid out yet — try again right after layout
            view.post(this::render);
            return;
        }
        w = Math.min(w, MAX_PX);
        int h = Math.max(1, w * ratio[1] / ratio[0]);
        if (back == null || back.getWidth() != w || back.getHeight() != h) {
            back = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        }

        final Bitmap target = back;
        final int myToken = token;
        final String json = specJson;
        final WidgetSpec parsed = spec;
        final Context app = view.getContext().getApplicationContext();
        final int fw = w, fh = h;
        busy = true;
        RENDERER.execute(() -> {
            WidgetSpec s = parsed;
            boolean ok;
            try {
                if (s == null) s = GSON.fromJson(json, WidgetSpec.class);
                long now = System.currentTimeMillis();
                RenderContext rc = new RenderContext(app, fw, fh, ThemeEngine.current(app), now,
                        new CachedLiveDataSource(app));
                target.eraseColor(Color.TRANSPARENT);
                ENGINE.render(s, rc, target);
                ok = true;
            } catch (Exception e) {
                ok = false;
            }
            final WidgetSpec result = s;
            final boolean success = ok;
            MAIN.post(() -> onRendered(myToken, target, result, success));
        });
    }

    private void onRendered(int myToken, Bitmap target, WidgetSpec parsed, boolean ok) {
        busy = false;
        if (myToken == token) {
            if (spec == null && parsed != null) {
                spec = parsed;
                interval = intervalOf(parsed);
            }
            if (ok) {
                view.setImageBitmap(target);
                back = front;
                front = target;
                lastRenderedMinute = System.currentTimeMillis() / EVERY_MINUTE;
            }
        }
        if (dirty) {
            dirty = false;
            render();
        }
    }
}
