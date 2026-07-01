package com.desire.widget.widget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.desire.widget.engine.runtime.EngineRenderer;
import com.desire.widget.engine.runtime.EngineWidgetStore;

public class WidgetPinCallback extends BroadcastReceiver {
    public static final String ACTION_WIDGET_PINNED = "com.desire.widget.ACTION_WIDGET_PINNED";

    @Override
    public void onReceive(Context context, Intent intent) {
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return;

        EngineWidgetStore.commitPending(context, appWidgetId);
        ClockTickReceiver.ensureScheduled(context);

        final PendingResult pendingResult = goAsync();
        EngineRenderer.render(context, new int[]{appWidgetId}, pendingResult::finish);
    }
}
