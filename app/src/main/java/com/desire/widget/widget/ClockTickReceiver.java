package com.desire.widget.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Fires every ~60 s to re-render installed home-screen widgets so clocks, batteries, and other
 * time-sensitive components stay current. Uses {@link AlarmManager#setExactAndAllowWhileIdle}
 * for sub-minute precision on Doze-restricted devices and also listens for screen-on / user
 * present broadcasts so widgets update immediately when the user returns to the home screen.
 */
public class ClockTickReceiver extends BroadcastReceiver {
    public static final String ACTION_CLOCK_TICK = "com.desire.widget.ACTION_CLOCK_TICK";
    private static final int REQUEST_CODE = 4001;
    private static final long INTERVAL_MS = 60_000L;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Always reschedule the next tick so the chain never breaks.
        ensureScheduled(context);

        String action = intent.getAction();
        boolean isTick = ACTION_CLOCK_TICK.equals(action);
        boolean isScreenOn = Intent.ACTION_SCREEN_ON.equals(action);
        boolean isUserPresent = Intent.ACTION_USER_PRESENT.equals(action);

        // On screen-on / user-present we render immediately (no need to wait for the alarm).
        // On CLOCK_TICK we also render. Time/timezone changes always trigger a re-render.
        if (isTick || isScreenOn || isUserPresent
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action)) {

            int[] ids = allWidgetIds(context);
            if (ids.length == 0) return;

            // goAsync() is only available for manifest-registered receivers. Dynamic receivers
            // (screen-on/user-present registered in WidgetApp) don't have a PendingResult, so
            // we render on a background thread directly.
            try {
                final PendingResult pendingResult = goAsync();
                com.desire.widget.engine.runtime.EngineRenderer.render(context, ids, pendingResult::finish);
            } catch (IllegalStateException e) {
                // Dynamic receiver — render on background thread.
                final int[] widgetIds = ids;
                com.desire.widget.util.AppExecutors.getInstance().diskIO().execute(() ->
                        com.desire.widget.engine.runtime.EngineRenderer.render(context, widgetIds));
            }
        }
    }

    /**
     * Schedules the next CLOCK_TICK broadcast. Safe to call multiple times — the existing
     * alarm is replaced. Uses {@code setExactAndAllowWhileIdle} on API 23+ for reliable
     * firing even in Doze mode.
     */
    public static void ensureScheduled(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        PendingIntent pendingIntent = tickIntent(context);
        long triggerAt = System.currentTimeMillis() + INTERVAL_MS;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+: check if we can schedule exact alarms
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        }
    }

    private static int[] allWidgetIds(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        Class<?>[] providers = {
                WidgetProvider.class, WidgetProvider2x1.class,
                WidgetProvider4x1.class, WidgetProvider4x2.class
        };
        java.util.ArrayList<Integer> all = new java.util.ArrayList<>();
        for (Class<?> provider : providers) {
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, provider));
            if (ids != null) {
                for (int id : ids) all.add(id);
            }
        }
        int[] result = new int[all.size()];
        for (int i = 0; i < result.length; i++) result[i] = all.get(i);
        return result;
    }

    private static PendingIntent tickIntent(Context context) {
        Intent intent = new Intent(context, ClockTickReceiver.class);
        intent.setAction(ACTION_CLOCK_TICK);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags);
    }
}
