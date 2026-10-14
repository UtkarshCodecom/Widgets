package com.desire.widget.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.desire.widget.engine.runtime.EngineRenderer;
import com.desire.widget.engine.runtime.EngineWidgetStore;

/**
 * Re-renders "live" widgets (analog clocks with a second hand, clocks showing seconds, music
 * visualizers) once per second WHILE THE SCREEN IS ON. Started on SCREEN_ON/USER_PRESENT and
 * stopped on SCREEN_OFF by {@link ClockTickReceiver}, so it costs nothing while the device is idle.
 *
 * <p>This is a best-effort ticker bound to the app process being alive; the per-minute alarm still
 * guarantees clocks stay correct even if the process was reclaimed.
 */
public final class SecondTicker {
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static boolean running;
    private static Runnable task;

    private SecondTicker() {}

    public static void start(Context context) {
        if (running) return;
        running = true;
        final Context app = context.getApplicationContext();
        task = new Runnable() {
            @Override
            public void run() {
                if (!running) return;
                int[] ids = secondWidgets(app);
                if (ids.length > 0) EngineRenderer.render(app, ids);
                if (running) HANDLER.postDelayed(this, 1000L);
            }
        };
        HANDLER.post(task);
    }

    public static void stop() {
        running = false;
        if (task != null) HANDLER.removeCallbacks(task);
    }

    private static int[] secondWidgets(Context app) {
        int[] all = EngineWidgetStore.allPlacedIds(app);
        // One cache read per tick (SharedPreferences is memory-backed after the first load).
        boolean musicPlaying =
                new com.desire.widget.engine.data.CachedLiveDataSource(app).nowPlaying().playing;
        int[] tmp = new int[all.length];
        int n = 0;
        for (int id : all) {
            String specJson = EngineWidgetStore.getSpecJson(app, id);
            if (needsSeconds(specJson)) {
                tmp[n++] = id;
            } else if (isRunningTimer(specJson) && com.desire.widget.engine.data.TimerStateStore.isRunning(app, id)) {
                tmp[n++] = id;
            } else if (musicPlaying && isMusic(specJson)) {
                // Re-render the music card each second while a track plays so the extrapolated
                // progress bar moves and the play/pause icon stays current.
                tmp[n++] = id;
            }
        }
        int[] out = new int[n];
        System.arraycopy(tmp, 0, out, 0, n);
        return out;
    }

    /** Matches the music card ({@code "music"}); the visualizer ({@code "music_visualizer"})
     *  is already covered by {@link #needsSeconds}. */
    private static boolean isMusic(String specJson) {
        return specJson != null && specJson.contains("\"music\"");
    }

    /** Cheap check: does this spec have something that changes every second? */
    private static boolean needsSeconds(String specJson) {
        if (specJson == null || specJson.isEmpty()) return false;
        return specJson.contains("analog_clock")
                || specJson.contains("music_visualizer")
                || specJson.contains("ss");   // a clock format that includes seconds
    }

    /** Stopwatch/Pomodoro only need per-second re-render while actually running. */
    private static boolean isRunningTimer(String specJson) {
        return specJson != null && (specJson.contains("\"stopwatch\"") || specJson.contains("\"pomodoro\""));
    }
}
