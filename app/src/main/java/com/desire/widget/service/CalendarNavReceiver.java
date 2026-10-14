package com.desire.widget.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.desire.widget.engine.data.CalendarStateStore;
import com.desire.widget.engine.runtime.EngineRenderer;

/**
 * Handles taps on the calendar widget's prev / today / next overlay regions
 * ({@code widget_layout.xml}), updates that widget's own month offset in
 * {@link CalendarStateStore}, and re-renders just that widget.
 */
public class CalendarNavReceiver extends BroadcastReceiver {
    public static final String ACTION_NAV = "com.desire.widget.ACTION_CALENDAR_NAV";
    public static final String EXTRA_COMMAND = "command";
    public static final String EXTRA_APPWIDGET_ID = "appwidget_id";

    public static final String CMD_PREV = "prev";
    public static final String CMD_NEXT = "next";
    public static final String CMD_TODAY = "today";

    @Override
    public void onReceive(Context context, Intent intent) {
        int appWidgetId = intent.getIntExtra(EXTRA_APPWIDGET_ID, -1);
        String command = intent.getStringExtra(EXTRA_COMMAND);
        if (appWidgetId == -1 || command == null) return;

        int current = CalendarStateStore.getMonthOffset(context, appWidgetId);
        switch (command) {
            case CMD_PREV:
                CalendarStateStore.setMonthOffset(context, appWidgetId, current - 1);
                break;
            case CMD_NEXT:
                CalendarStateStore.setMonthOffset(context, appWidgetId, current + 1);
                break;
            case CMD_TODAY:
                CalendarStateStore.setMonthOffset(context, appWidgetId, 0);
                break;
            default:
                return;
        }
        EngineRenderer.render(context, new int[]{appWidgetId});
    }
}
