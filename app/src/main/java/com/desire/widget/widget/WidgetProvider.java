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
        }
    }
}
