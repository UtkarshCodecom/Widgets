package com.desire.widget.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import com.desire.widget.engine.data.ToggleState;
import com.desire.widget.engine.runtime.EngineRenderer;
import com.desire.widget.engine.runtime.EngineWidgetStore;

/**
 * Quick-settings toggle taps. Android only lets a third-party app flip a few of these directly:
 * the torch (CameraManager), auto-rotate (with WRITE_SETTINGS) and Do Not Disturb (with
 * notification-policy access). Everything else — Wi-Fi, mobile data, Bluetooth, location, flight
 * mode, dark mode — opens the official system panel/settings page instead.
 *
 * <p>{@link #pendingIntentFor} decides which at render time, so the widget always gets a
 * PendingIntent that works: a broadcast to this receiver for direct toggles, or an Activity
 * PendingIntent (which the launcher may start) for settings pages. A broadcast receiver can't
 * open an Activity itself on modern Android, so we never route settings through here.
 */
public class ToggleReceiver extends BroadcastReceiver {
    public static final String ACTION_TOGGLE = "com.desire.widget.ACTION_QUICK_TOGGLE";
    public static final String EXTRA_KIND = "kind";

    public static PendingIntent pendingIntentFor(Context c, int appWidgetId, String kind) {
        int imm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | imm;
        if (canToggleDirectly(c, kind)) {
            Intent i = new Intent(c, ToggleReceiver.class)
                    .setAction(ACTION_TOGGLE)
                    .putExtra(EXTRA_KIND, kind)
                    // Unique data per (widget, kind) so PendingIntents never collapse together.
                    .setData(Uri.parse("toggle://" + appWidgetId + "/" + kind));
            return PendingIntent.getBroadcast(c, appWidgetId, i, flags);
        }
        Intent settings = settingsIntent(c, kind).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(c, ("settings:" + kind).hashCode(), settings, flags);
    }

    private static boolean canToggleDirectly(Context c, String kind) {
        switch (kind) {
            case ToggleState.FLASHLIGHT:
                return torchCameraId(c) != null;
            case ToggleState.ROTATE:
                return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(c);
            case ToggleState.DND: {
                NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
                return nm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && nm.isNotificationPolicyAccessGranted();
            }
            default:
                return false;
        }
    }

    /** The system page for a toggle we can't flip (or the grant screen for one we could). */
    private static Intent settingsIntent(Context c, String kind) {
        boolean q = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
        switch (kind) {
            case ToggleState.WIFI:
                return new Intent(q ? Settings.Panel.ACTION_WIFI : Settings.ACTION_WIFI_SETTINGS);
            case ToggleState.DATA:
                return new Intent(q ? Settings.Panel.ACTION_INTERNET_CONNECTIVITY : Settings.ACTION_DATA_ROAMING_SETTINGS);
            case ToggleState.BLUETOOTH:
                return new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            case ToggleState.LOCATION:
                return new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            case ToggleState.AIRPLANE:
                return new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
            case ToggleState.DARK:
                return new Intent(Settings.ACTION_DISPLAY_SETTINGS);
            case ToggleState.ROTATE: // not yet allowed to write settings → grant screen
                return new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + c.getPackageName()));
            case ToggleState.DND: // no policy access yet → grant screen
                return new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            default:
                return new Intent(Settings.ACTION_SETTINGS);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String kind = intent.getStringExtra(EXTRA_KIND);
        if (kind == null) return;
        try {
            switch (kind) {
                case ToggleState.FLASHLIGHT: {
                    CameraManager cm = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
                    String id = torchCameraId(context);
                    if (cm != null && id != null) {
                        boolean next = !ToggleState.isTorchOn(context);
                        cm.setTorchMode(id, next);
                        ToggleState.setTorchOn(context, next);
                    }
                    break;
                }
                case ToggleState.ROTATE: {
                    boolean on = Settings.System.getInt(context.getContentResolver(),
                            Settings.System.ACCELEROMETER_ROTATION, 0) == 1;
                    Settings.System.putInt(context.getContentResolver(),
                            Settings.System.ACCELEROMETER_ROTATION, on ? 0 : 1);
                    break;
                }
                case ToggleState.DND: {
                    NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (nm != null) {
                        boolean on = nm.getCurrentInterruptionFilter() != NotificationManager.INTERRUPTION_FILTER_ALL;
                        nm.setInterruptionFilter(on ? NotificationManager.INTERRUPTION_FILTER_ALL
                                : NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                    }
                    break;
                }
                default:
                    break;
            }
        } catch (Exception ignored) {
            // Camera busy, permission revoked mid-flight, etc. — the next render shows the truth.
        }
        renderToggleWidgets(context);
    }

    /** Re-renders every placed widget that contains a toggle (cheap no-op if there are none). */
    public static void renderToggleWidgets(Context context) {
        int[] all = EngineWidgetStore.allPlacedIds(context);
        int[] tmp = new int[all.length];
        int n = 0;
        for (int id : all) {
            String spec = EngineWidgetStore.getSpecJson(context, id);
            if (spec != null && spec.contains("\"toggle\"")) tmp[n++] = id;
        }
        if (n == 0) return;
        int[] ids = new int[n];
        System.arraycopy(tmp, 0, ids, 0, n);
        EngineRenderer.render(context, ids);
    }

    /** First camera with a flash unit, or null when the device has no torch. */
    public static String torchCameraId(Context c) {
        try {
            CameraManager cm = (CameraManager) c.getSystemService(Context.CAMERA_SERVICE);
            if (cm == null) return null;
            for (String id : cm.getCameraIdList()) {
                Boolean flash = cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (Boolean.TRUE.equals(flash)) return id;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
