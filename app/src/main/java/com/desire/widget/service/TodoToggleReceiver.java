package com.desire.widget.service;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import com.desire.widget.engine.data.UserDataStore;
import com.desire.widget.engine.runtime.EngineRenderer;

/**
 * Ticks a to-do on/off straight from a row on the home-screen widget. The task is addressed by its
 * stable id (not its row), so a tap still hits the right task if the list changed in between.
 */
public class TodoToggleReceiver extends BroadcastReceiver {
    public static final String ACTION = "com.desire.widget.ACTION_TODO_TOGGLE";
    public static final String EXTRA_APPWIDGET_ID = "appwidget_id";
    public static final String EXTRA_TODO_ID = "todo_id";

    public static PendingIntent pendingIntent(Context c, int appWidgetId, int row, long todoId) {
        Intent i = new Intent(c, TodoToggleReceiver.class)
                .setAction(ACTION)
                .putExtra(EXTRA_APPWIDGET_ID, appWidgetId)
                .putExtra(EXTRA_TODO_ID, todoId)
                // filterEquals ignores extras: unique data per (widget, row) keeps them apart.
                .setData(Uri.parse("todo://" + appWidgetId + "/" + row));
        int flags = PendingIntent.FLAG_UPDATE_CURRENT
                | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        return PendingIntent.getBroadcast(c, 3_000_000 + appWidgetId * 32 + row, i, flags);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int appWidgetId = intent.getIntExtra(EXTRA_APPWIDGET_ID, -1);
        long todoId = intent.getLongExtra(EXTRA_TODO_ID, -1);
        if (appWidgetId < 0 || todoId < 0) return;
        if (UserDataStore.toggleTodo(context, appWidgetId, todoId)) {
            EngineRenderer.render(context.getApplicationContext(), new int[]{appWidgetId});
        }
    }
}
