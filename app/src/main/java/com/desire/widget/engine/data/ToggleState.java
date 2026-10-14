package com.desire.widget.engine.data;

import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.desire.widget.R;

/**
 * Live on/off state for the quick-settings toggle widgets, read without special permissions
 * (Wi-Fi needs the normal ACCESS_WIFI_STATE). Returns {@code null} when the state genuinely can't
 * be read, so the renderer can show a neutral tile instead of a wrong one.
 */
public final class ToggleState {
    public static final String WIFI = "wifi";
    public static final String BLUETOOTH = "bluetooth";
    public static final String FLASHLIGHT = "flashlight";
    public static final String LOCATION = "location";
    public static final String DND = "dnd";
    public static final String AIRPLANE = "airplane";
    public static final String ROTATE = "rotate";
    public static final String DARK = "darkmode";
    public static final String DATA = "data";
    /** Ringer mode: Ring → Vibrate → Silent. "On" = a quiet mode is active. */
    public static final String SOUND = "sound";
    /** Adaptive (automatic) screen brightness. */
    public static final String AUTOBRIGHT = "autobright";

    private static final String TORCH_PREFS = "torch_state";

    private ToggleState() {}

    public static Boolean isOn(Context c, String kind) {
        try {
            ContentResolver cr = c.getContentResolver();
            switch (kind) {
                case WIFI: {
                    WifiManager wm = (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    return wm != null ? wm.isWifiEnabled() : null;
                }
                case BLUETOOTH: {
                    BluetoothManager bm = (BluetoothManager) c.getSystemService(Context.BLUETOOTH_SERVICE);
                    BluetoothAdapter a = bm != null ? bm.getAdapter() : null;
                    return a != null ? a.isEnabled() : null;
                }
                case FLASHLIGHT:
                    return isTorchOn(c);
                case LOCATION: {
                    LocationManager lm = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
                    if (lm == null) return null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) return lm.isLocationEnabled();
                    return Settings.Secure.getInt(cr, Settings.Secure.LOCATION_MODE,
                            Settings.Secure.LOCATION_MODE_OFF) != Settings.Secure.LOCATION_MODE_OFF;
                }
                case DND: {
                    NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (nm == null) return null;
                    int f = nm.getCurrentInterruptionFilter();
                    return f != NotificationManager.INTERRUPTION_FILTER_ALL
                            && f != NotificationManager.INTERRUPTION_FILTER_UNKNOWN;
                }
                case AIRPLANE:
                    return Settings.Global.getInt(cr, Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
                case ROTATE:
                    return Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0) == 1;
                case DARK: {
                    // System resources reflect the device-wide setting, not this app's theme.
                    int mode = Resources.getSystem().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                    return mode == Configuration.UI_MODE_NIGHT_YES;
                }
                case SOUND:
                    return ringerMode(c) != AudioManager.RINGER_MODE_NORMAL;
                case AUTOBRIGHT:
                    return Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                            == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
                case DATA: {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null;
                    TelephonyManager tm = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
                    return tm != null ? tm.isDataEnabled() : null;
                }
                default:
                    return null;
            }
        } catch (Exception e) { // SecurityException on some OEMs, missing radios, etc.
            return null;
        }
    }

    /** Ringer mode, or RINGER_MODE_NORMAL when it can't be read. */
    public static int ringerMode(Context c) {
        AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        return am != null ? am.getRingerMode() : AudioManager.RINGER_MODE_NORMAL;
    }

    /** Label reflecting the live state where it has more than on/off (the ringer). */
    public static String label(Context c, String kind) {
        if (SOUND.equals(kind)) {
            switch (ringerMode(c)) {
                case AudioManager.RINGER_MODE_VIBRATE: return "Vibrate";
                case AudioManager.RINGER_MODE_SILENT: return "Silent";
                default: return "Ring";
            }
        }
        return label(kind);
    }

    /** Icon reflecting the live state where it has more than on/off (the ringer). */
    public static int icon(Context c, String kind) {
        if (SOUND.equals(kind)) {
            switch (ringerMode(c)) {
                case AudioManager.RINGER_MODE_VIBRATE: return R.drawable.ic_tg_vibrate;
                case AudioManager.RINGER_MODE_SILENT: return R.drawable.ic_tg_silent;
                default: return R.drawable.ic_tg_ring;
            }
        }
        return icon(kind);
    }

    public static String label(String kind) {
        switch (kind) {
            case WIFI: return "Wi-Fi";
            case BLUETOOTH: return "Bluetooth";
            case FLASHLIGHT: return "Torch";
            case LOCATION: return "Location";
            case DND: return "DND";
            case AIRPLANE: return "Flight";
            case ROTATE: return "Rotate";
            case DARK: return "Dark";
            case DATA: return "Data";
            case SOUND: return "Sound";
            case AUTOBRIGHT: return "Auto";
            default: return kind;
        }
    }

    public static int icon(String kind) {
        switch (kind) {
            case WIFI: return R.drawable.ic_tg_wifi;
            case BLUETOOTH: return R.drawable.ic_tg_bluetooth;
            case FLASHLIGHT: return R.drawable.ic_tg_flashlight;
            case LOCATION: return R.drawable.ic_tg_location;
            case DND: return R.drawable.ic_tg_dnd;
            case AIRPLANE: return R.drawable.ic_tg_airplane;
            case ROTATE: return R.drawable.ic_tg_rotate;
            case DARK: return R.drawable.ic_tg_darkmode;
            case DATA: return R.drawable.ic_tg_data;
            case SOUND: return R.drawable.ic_tg_ring;
            case AUTOBRIGHT: return R.drawable.ic_tg_brightness;
            default: return R.drawable.ic_tg_wifi;
        }
    }

    // Torch state isn't queryable directly; we track it from our own toggles and the system
    // TorchCallback registered in WidgetApp.
    public static boolean isTorchOn(Context c) {
        return c.getApplicationContext().getSharedPreferences(TORCH_PREFS, Context.MODE_PRIVATE)
                .getBoolean("on", false);
    }

    public static void setTorchOn(Context c, boolean on) {
        c.getApplicationContext().getSharedPreferences(TORCH_PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean("on", on).apply();
    }
}
