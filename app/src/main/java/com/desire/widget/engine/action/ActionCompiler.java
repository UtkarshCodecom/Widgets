package com.desire.widget.engine.action;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import com.desire.widget.engine.model.ActionSpec;

import java.util.Map;

/**
 * Turns a declarative {@link ActionSpec} into a {@link PendingIntent} ready to attach to a
 * RemoteViews click target. Package and class names travel in the spec (never hardcoded), so the
 * same widget definition is portable across devices and installs.
 *
 * <p>Returns {@code null} when the action is unrepresentable (e.g. launch_app for an app that is
 * not installed); callers fall back to opening the host app.
 */
public final class ActionCompiler {
    private ActionCompiler() {}

    public static PendingIntent compile(Context context, int requestCode, ActionSpec action) {
        if (action == null || action.type == null) return null;
        Intent intent = buildIntent(context, action);
        if (intent == null) return null;

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        boolean broadcast = "broadcast".equals(action.type) || "toggle_setting".equals(action.type);
        if (broadcast) {
            return PendingIntent.getBroadcast(context, requestCode, intent, flags);
        }
        return PendingIntent.getActivity(context, requestCode, intent, flags);
    }

    private static Intent buildIntent(Context context, ActionSpec a) {
        switch (a.type) {
            case "launch_app": {
                if (a.packageName == null) return null;
                Intent launch = context.getPackageManager().getLaunchIntentForPackage(a.packageName);
                if (launch != null) launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                return launch; // null if not installed -> caller falls back
            }
            case "open_activity": {
                if (a.packageName == null || a.className == null) return null;
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(a.packageName, a.className));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                applyExtras(intent, a.extras);
                return intent;
            }
            case "open_url": {
                if (a.data == null || a.data.isEmpty()) return null;
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(a.data));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                return intent;
            }
            case "toggle_setting": {
                Intent intent = new Intent(context, EngineActionReceiver.class);
                intent.setAction(EngineActionReceiver.ACTION_TOGGLE_SETTING);
                intent.putExtra(EngineActionReceiver.EXTRA_KEY, a.data);
                return intent;
            }
            case "broadcast": {
                if (a.data == null || a.data.isEmpty()) return null;
                Intent intent = new Intent(a.data);
                if (a.packageName != null) intent.setPackage(a.packageName);
                applyExtras(intent, a.extras);
                return intent;
            }
            default:
                return null;
        }
    }

    private static void applyExtras(Intent intent, Map<String, String> extras) {
        if (extras == null) return;
        for (Map.Entry<String, String> e : extras.entrySet()) {
            intent.putExtra(e.getKey(), e.getValue());
        }
    }
}
