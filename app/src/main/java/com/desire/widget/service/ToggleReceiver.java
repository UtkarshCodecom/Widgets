package com.desire.widget.service;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;

import com.desire.widget.engine.data.ToggleState;
import com.desire.widget.engine.runtime.EngineRenderer;
import com.desire.widget.engine.runtime.EngineWidgetStore;
import com.desire.widget.ui.permission.ToggleSetupActivity;

/**
 * Quick-settings toggle taps. Every tile is routed at render time ({@link #route}) to the most
 * direct thing Android allows:
 * <ul>
 *   <li>{@link Route#DIRECT} — a broadcast here flips it instantly: torch, ringer, auto-rotate,
 *   auto-brightness, Do Not Disturb (once their one-time permission is granted), plus Wi-Fi and
 *   Bluetooth on the older Android versions that still allow it.</li>
 *   <li>{@link Route#SETUP} — the permission isn't granted yet: open {@link ToggleSetupActivity},
 *   which explains it, sends the user to the grant switch, and flips the toggle on return.</li>
 *   <li>{@link Route#SYSTEM} — Android forbids apps from flipping it (Wi-Fi on 10+, mobile data,
 *   location, flight mode, dark mode): open the system's own panel/dialog for it.</li>
 * </ul>
 * A broadcast receiver can't open an Activity on modern Android, so SETUP/SYSTEM are Activity
 * PendingIntents, never routed through here.
 */
public class ToggleReceiver extends BroadcastReceiver {
    public static final String ACTION_TOGGLE = "com.desire.widget.ACTION_QUICK_TOGGLE";
    public static final String EXTRA_KIND = "kind";

    public enum Route { DIRECT, SETUP, SYSTEM }

    /** What a one-time grant a SETUP route needs. */
    public enum Need { NONE, WRITE_SETTINGS, DND_ACCESS, NEARBY_DEVICES }

    public static PendingIntent pendingIntentFor(Context c, int appWidgetId, String kind) {
        int imm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | imm;
        // Unique data per (widget, kind) so PendingIntents never collapse together.
        Uri data = Uri.parse("toggle://" + appWidgetId + "/" + kind);
        switch (route(c, kind)) {
            case DIRECT: {
                Intent i = new Intent(c, ToggleReceiver.class)
                        .setAction(ACTION_TOGGLE)
                        .putExtra(EXTRA_KIND, kind)
                        .setData(data);
                return PendingIntent.getBroadcast(c, appWidgetId, i, flags);
            }
            case SETUP: {
                Intent i = new Intent(c, ToggleSetupActivity.class)
                        .putExtra(EXTRA_KIND, kind)
                        .setData(data)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                return PendingIntent.getActivity(c, appWidgetId, i, flags);
            }
            default: {
                Intent settings = systemIntent(c, kind).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                return PendingIntent.getActivity(c, ("settings:" + kind).hashCode(), settings, flags);
            }
        }
    }

    public static Route route(Context c, String kind) {
        switch (kind) {
            case ToggleState.FLASHLIGHT:
                return torchCameraId(c) != null ? Route.DIRECT : Route.SYSTEM;
            case ToggleState.ROTATE:
            case ToggleState.AUTOBRIGHT:
                return canWriteSettings(c) ? Route.DIRECT : Route.SETUP;
            case ToggleState.DND:
                return hasDndAccess(c) ? Route.DIRECT : Route.SETUP;
            case ToggleState.SOUND:
                // Leaving (or entering) Silent toggles DND, which needs DND access since Android 7.
                return ToggleState.ringerMode(c) == AudioManager.RINGER_MODE_SILENT && !hasDndAccess(c)
                        ? Route.SETUP : Route.DIRECT;
            case ToggleState.WIFI:
                return Build.VERSION.SDK_INT <= Build.VERSION_CODES.P ? Route.DIRECT : Route.SYSTEM;
            case ToggleState.BLUETOOTH:
                if (bluetooth(c) == null) return Route.SYSTEM;
                if (!hasNearbyDevices(c)) return Route.SETUP;
                return Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 ? Route.DIRECT : Route.SYSTEM;
            default:
                return Route.SYSTEM;
        }
    }

    public static Need need(Context c, String kind) {
        switch (kind) {
            case ToggleState.ROTATE:
            case ToggleState.AUTOBRIGHT:
                return canWriteSettings(c) ? Need.NONE : Need.WRITE_SETTINGS;
            case ToggleState.DND:
            case ToggleState.SOUND:
                return hasDndAccess(c) ? Need.NONE : Need.DND_ACCESS;
            case ToggleState.BLUETOOTH:
                return hasNearbyDevices(c) ? Need.NONE : Need.NEARBY_DEVICES;
            default:
                return Need.NONE;
        }
    }

    /**
     * The system screen for a toggle apps may not flip. On Android 10+ Wi-Fi and mobile data open
     * as a small panel over the home screen rather than the full Settings app.
     */
    public static Intent systemIntent(Context c, String kind) {
        boolean q = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
        switch (kind) {
            case ToggleState.WIFI:
                return new Intent(q ? Settings.Panel.ACTION_WIFI : Settings.ACTION_WIFI_SETTINGS);
            case ToggleState.DATA:
                return new Intent(q ? Settings.Panel.ACTION_INTERNET_CONNECTIVITY : Settings.ACTION_DATA_ROAMING_SETTINGS);
            case ToggleState.BLUETOOTH: {
                // Off → the system's small "Turn on Bluetooth?" dialog; on → Bluetooth settings.
                Boolean on = ToggleState.isOn(c, kind);
                return Boolean.TRUE.equals(on)
                        ? new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                        : new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            }
            case ToggleState.LOCATION:
                return new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            case ToggleState.AIRPLANE:
                return new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
            case ToggleState.DARK:
                return new Intent(Settings.ACTION_DISPLAY_SETTINGS);
            default:
                return new Intent(Settings.ACTION_SETTINGS);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String kind = intent.getStringExtra(EXTRA_KIND);
        if (kind == null) return;
        perform(context, kind);
        renderToggleWidgets(context);
    }

    /**
     * Flips one toggle. Returns a short result for a confirmation toast ("Vibrate", "Auto-rotate
     * on"), or null when nothing could be changed (permission revoked mid-flight, camera busy…).
     */
    public static String perform(Context context, String kind) {
        try {
            switch (kind) {
                case ToggleState.FLASHLIGHT: {
                    CameraManager cm = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
                    String id = torchCameraId(context);
                    if (cm == null || id == null) return null;
                    boolean next = !ToggleState.isTorchOn(context);
                    cm.setTorchMode(id, next);
                    ToggleState.setTorchOn(context, next);
                    return next ? "Torch on" : "Torch off";
                }
                case ToggleState.ROTATE: {
                    boolean next = !isSystemFlagOn(context, Settings.System.ACCELEROMETER_ROTATION);
                    Settings.System.putInt(context.getContentResolver(),
                            Settings.System.ACCELEROMETER_ROTATION, next ? 1 : 0);
                    return next ? "Auto-rotate on" : "Auto-rotate off";
                }
                case ToggleState.AUTOBRIGHT: {
                    boolean next = !Boolean.TRUE.equals(ToggleState.isOn(context, kind));
                    Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                            next ? Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                                    : Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                    return next ? "Auto-brightness on" : "Auto-brightness off";
                }
                case ToggleState.DND: {
                    NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (nm == null) return null;
                    boolean on = nm.getCurrentInterruptionFilter() != NotificationManager.INTERRUPTION_FILTER_ALL;
                    nm.setInterruptionFilter(on ? NotificationManager.INTERRUPTION_FILTER_ALL
                            : NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                    return on ? "Do Not Disturb off" : "Do Not Disturb on";
                }
                case ToggleState.SOUND: {
                    AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    if (am == null) return null;
                    int next;
                    switch (am.getRingerMode()) {
                        case AudioManager.RINGER_MODE_NORMAL:
                            next = AudioManager.RINGER_MODE_VIBRATE;
                            break;
                        case AudioManager.RINGER_MODE_VIBRATE:
                            // Silent switches DND on, which needs DND access — skip it without.
                            next = hasDndAccess(context) ? AudioManager.RINGER_MODE_SILENT
                                    : AudioManager.RINGER_MODE_NORMAL;
                            break;
                        default:
                            next = AudioManager.RINGER_MODE_NORMAL;
                            break;
                    }
                    am.setRingerMode(next);
                    return next == AudioManager.RINGER_MODE_VIBRATE ? "Vibrate"
                            : next == AudioManager.RINGER_MODE_SILENT ? "Silent" : "Ring";
                }
                case ToggleState.WIFI: {
                    WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if (wm == null) return null;
                    boolean next = !wm.isWifiEnabled();
                    //noinspection deprecation — still works up to Android 9, which is when we call it
                    return wm.setWifiEnabled(next) ? (next ? "Wi-Fi on" : "Wi-Fi off") : null;
                }
                case ToggleState.BLUETOOTH: {
                    BluetoothAdapter a = bluetooth(context);
                    if (a == null || !hasNearbyDevices(context)) return null;
                    boolean next = !a.isEnabled();
                    //noinspection deprecation,MissingPermission — Android ≤ 12 only, permission checked above
                    boolean ok = next ? a.enable() : a.disable();
                    return ok ? (next ? "Bluetooth on" : "Bluetooth off") : null;
                }
                default:
                    return null;
            }
        } catch (Exception e) {
            return null; // camera busy, permission revoked mid-flight, OEM restriction…
        }
    }

    // ------------------------------------------------------------------ permissions

    public static boolean canWriteSettings(Context c) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(c);
    }

    public static boolean hasDndAccess(Context c) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        return nm != null && nm.isNotificationPolicyAccessGranted();
    }

    /** BLUETOOTH_CONNECT ("Nearby devices") is a runtime permission from Android 12. */
    public static boolean hasNearbyDevices(Context c) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || c.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean isSystemFlagOn(Context c, String key) {
        return Settings.System.getInt(c.getContentResolver(), key, 0) == 1;
    }

    private static BluetoothAdapter bluetooth(Context c) {
        BluetoothManager bm = (BluetoothManager) c.getSystemService(Context.BLUETOOTH_SERVICE);
        return bm != null ? bm.getAdapter() : null;
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
