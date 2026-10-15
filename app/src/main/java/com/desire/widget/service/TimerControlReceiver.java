package com.desire.widget.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.desire.widget.engine.data.TimerStateStore;
import com.desire.widget.engine.runtime.EngineRenderer;

/**
 * Whole-widget tap handler for the Stopwatch and Pomodoro widgets: toggles start/pause via
 * {@link TimerStateStore} and re-renders that widget. Used INSTEAD OF the generic
 * {@code WidgetTapActivity} trampoline for widgets whose spec contains a "stopwatch" or "pomodoro"
 * component — see {@code EngineRenderer.hasTimerComponent}.
 */
public class TimerControlReceiver extends BroadcastReceiver {
    public static final String ACTION_TOGGLE = "com.desire.widget.ACTION_TIMER_TOGGLE";
    public static final String EXTRA_APPWIDGET_ID = "appwidget_id";
    /** Optional command: "reset" resets the timer; anything else (or absent) toggles start/pause. */
    public static final String EXTRA_COMMAND = "command";
    public static final String CMD_RESET = "reset";

    @Override
    public void onReceive(Context context, Intent intent) {
        int appWidgetId = intent.getIntExtra(EXTRA_APPWIDGET_ID, -1);
        if (appWidgetId == -1) return;
        if (CMD_RESET.equals(intent.getStringExtra(EXTRA_COMMAND))) {
            TimerStateStore.reset(context, appWidgetId);
        } else {
            TimerStateStore.toggle(context, appWidgetId, System.currentTimeMillis());
        }
        EngineRenderer.render(context, new int[]{appWidgetId});
    }
}
