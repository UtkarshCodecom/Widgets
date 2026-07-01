package com.desire.widget.engine.action;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Handles in-app action types that need to run code rather than start an Activity:
 * {@code toggle_setting} flips a boolean preference. Targeted explicitly (exported=false), so no
 * intent-filter is required. {@code broadcast} actions that name an external component bypass this
 * receiver entirely.
 */
public class EngineActionReceiver extends BroadcastReceiver {
    public static final String ACTION_TOGGLE_SETTING = "com.desire.widget.engine.TOGGLE_SETTING";
    public static final String EXTRA_KEY = "key";
    private static final String PREFS = "engine_settings";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        if (ACTION_TOGGLE_SETTING.equals(intent.getAction())) {
            String key = intent.getStringExtra(EXTRA_KEY);
            if (key == null || key.isEmpty()) return;
            SharedPreferences prefs = context.getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            boolean next = !prefs.getBoolean(key, false);
            prefs.edit().putBoolean(key, next).apply();
            // Phase 3 will trigger a widget re-render here so the toggle is reflected visually.
        }
    }

    public static boolean getSetting(Context context, String key, boolean def) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(key, def);
    }
}
