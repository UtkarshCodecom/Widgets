package com.desire.widget.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;

import com.desire.widget.engine.runtime.EngineRenderer;
import com.desire.widget.engine.runtime.EngineWidgetStore;

public class WidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        ClockTickReceiver.ensureScheduled(context);
        // Some launchers don't reliably fire the pin success callback, which would leave the widget
        // blank (showing the wallpaper). If a freshly-added widget has no spec yet, commit the one
        // chosen just before the pin flow so it always renders.
        for (int id : appWidgetIds) {
            if (EngineWidgetStore.getSpecJson(context, id).isEmpty()) {
                EngineWidgetStore.commitPending(context, id);
            }
        }
        final PendingResult pendingResult = goAsync();
        EngineRenderer.render(context, appWidgetIds, pendingResult::finish);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        ClockTickReceiver.ensureScheduled(context);
        EngineRenderer.render(context, new int[]{appWidgetId});
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        ClockTickReceiver.ensureScheduled(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            EngineWidgetStore.clear(context, appWidgetId);
            EngineRenderer.evict(appWidgetId);
            // Drop this widget's own notes/to-do/QR/BMI/habit/timer content and calendar navigation
            // state so a reused appWidgetId (Android recycles ids) never inherits deleted data.
            com.desire.widget.engine.data.UserDataStore.clear(context, appWidgetId);
            com.desire.widget.engine.data.CalendarStateStore.clear(context, appWidgetId);
            com.desire.widget.engine.data.BmiStateStore.clear(context, appWidgetId);
            com.desire.widget.engine.data.HabitStore.clear(context, appWidgetId);
            com.desire.widget.engine.data.TimerStateStore.clear(context, appWidgetId);
        }
    }
}
