package com.desire.widget.engine.data;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Battery levels of connected Bluetooth accessories (earbuds, headphones, watches…).
 *
 * <p>Android reports accessory battery (HFP / BLE battery service) through
 * {@code BluetoothDevice#getBatteryLevel()}, which is not part of the public SDK, so it is read
 * reflectively and every failure simply hides that device. Needs BLUETOOTH_CONNECT on Android 12+.
 */
public final class BluetoothBattery {
    public static final String KIND_HEADPHONES = "headphones";
    public static final String KIND_WATCH = "watch";
    public static final String KIND_PHONE = "phone";
    public static final String KIND_OTHER = "bluetooth";

    public static final class Device {
        public final String name;
        public final int level;
        public final String kind;

        public Device(String name, int level, String kind) {
            this.name = name;
            this.level = level;
            this.kind = kind;
        }
    }

    private BluetoothBattery() {}

    public static boolean hasPermission(Context c) {
        return Build.VERSION.SDK_INT < 31 || ContextCompat.checkSelfPermission(c,
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    /** Connected accessories that report a battery level (empty when unavailable). */
    @SuppressLint("MissingPermission") // guarded by hasPermission()
    public static List<Device> connected(Context c) {
        List<Device> out = new ArrayList<>();
        if (!hasPermission(c)) return out;
        try {
            BluetoothManager bm = (BluetoothManager) c.getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter adapter = bm != null ? bm.getAdapter() : null;
            if (adapter == null || !adapter.isEnabled()) return out;
            Method isConnected = BluetoothDevice.class.getMethod("isConnected");
            Method battery = BluetoothDevice.class.getMethod("getBatteryLevel");
            for (BluetoothDevice d : adapter.getBondedDevices()) {
                try {
                    if (!(Boolean) isConnected.invoke(d)) continue;
                    int level = (Integer) battery.invoke(d);
                    if (level < 0 || level > 100) continue;
                    String name = Build.VERSION.SDK_INT >= 30 && d.getAlias() != null ? d.getAlias() : d.getName();
                    out.add(new Device(name != null ? name : "Bluetooth device", level, kind(d)));
                } catch (Exception ignored) {
                    // One odd device must not hide the others.
                }
            }
        } catch (Exception ignored) {
            // Hidden API unavailable on this ROM → no accessories, phone-only.
        }
        return out;
    }

    @SuppressLint("MissingPermission")
    private static String kind(BluetoothDevice d) {
        BluetoothClass bc = d.getBluetoothClass();
        if (bc == null) return KIND_OTHER;
        switch (bc.getMajorDeviceClass()) {
            case BluetoothClass.Device.Major.AUDIO_VIDEO: return KIND_HEADPHONES;
            case BluetoothClass.Device.Major.WEARABLE: return KIND_WATCH;
            case BluetoothClass.Device.Major.PHONE: return KIND_PHONE;
            default: return KIND_OTHER;
        }
    }
}
