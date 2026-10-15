package com.desire.widget.engine.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Per-widget-instance running-timer state shared by the Stopwatch and Pomodoro widgets: whether
 * it's running, when the current run started, and how much time had already accumulated before
 * that. Elapsed time is always derived from real timestamps (never a counter that needs ticking),
 * so it stays correct even if the process was killed while running.
 */
public final class TimerStateStore {
    private static final String PREFS = "timer_state";

    private TimerStateStore() {}

    public static boolean isRunning(Context c, int id) {
        return id >= 0 && prefs(c).getBoolean("running_" + id, false);
    }

    /** Total elapsed milliseconds right now, whether running or paused. */
    public static long getElapsedMillis(Context c, int id, long nowMillis) {
        if (id < 0) return 0;
        SharedPreferences p = prefs(c);
        long accumulated = p.getLong("accumulated_" + id, 0);
        if (p.getBoolean("running_" + id, false)) {
            long startedAt = p.getLong("startedAt_" + id, nowMillis);
            accumulated += Math.max(0, nowMillis - startedAt);
        }
        return accumulated;
    }

    /** Starts if paused, pauses (freezing the elapsed total) if running. */
    public static void toggle(Context c, int id, long nowMillis) {
        if (id < 0) return;
        SharedPreferences p = prefs(c);
        boolean running = p.getBoolean("running_" + id, false);
        SharedPreferences.Editor e = p.edit();
        if (running) {
            // Pausing: freeze the accumulated elapsed time.
            e.putLong("accumulated_" + id, getElapsedMillis(c, id, nowMillis));
            e.putBoolean("running_" + id, false);
        } else {
            e.putLong("startedAt_" + id, nowMillis);
            e.putBoolean("running_" + id, true);
        }
        e.apply();
    }

    public static void reset(Context c, int id) {
        if (id < 0) return;
        prefs(c).edit()
                .putLong("accumulated_" + id, 0)
                .putBoolean("running_" + id, false)
                .remove("startedAt_" + id)
                .apply();
    }

    public static void clear(Context c, int id) {
        prefs(c).edit()
                .remove("running_" + id)
                .remove("startedAt_" + id)
                .remove("accumulated_" + id)
                .apply();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
