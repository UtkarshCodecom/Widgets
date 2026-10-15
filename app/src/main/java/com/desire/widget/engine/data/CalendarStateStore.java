package com.desire.widget.engine.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Per-widget-instance calendar navigation state: how many months the displayed grid is offset
 * from the current month. Keyed by {@code appWidgetId} so navigating one calendar widget never
 * affects another. Reset to 0 ("today") by {@code CalendarNavReceiver}.
 */
public final class CalendarStateStore {
    private static final String PREFS = "calendar_state";

    private CalendarStateStore() {}

    public static int getMonthOffset(Context c, int appWidgetId) {
        if (appWidgetId < 0) return 0;
        return prefs(c).getInt("offset_" + appWidgetId, 0);
    }

    public static void setMonthOffset(Context c, int appWidgetId, int offset) {
        if (appWidgetId < 0) return;
        // Clamp to a sane window so repeated taps can't drift into absurd dates.
        offset = Math.max(-60, Math.min(60, offset));
        prefs(c).edit().putInt("offset_" + appWidgetId, offset).apply();
    }

    public static void clear(Context c, int appWidgetId) {
        prefs(c).edit().remove("offset_" + appWidgetId).apply();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
