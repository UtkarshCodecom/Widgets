package com.desire.widget;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import com.desire.widget.data.repository.WidgetRepository;
import com.desire.widget.util.PreferenceManager;
import com.desire.widget.widget.ClockTickReceiver;
import com.google.firebase.FirebaseApp;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.concurrent.TimeUnit;

import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

public class WidgetApp extends Application {
    private static WidgetApp instance;
    private ClockTickReceiver screenReceiver;

    public static WidgetApp getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        com.desire.widget.engine.runtime.StyleApplier.init(this); // "@wallpaper" colours
        instance = this;

        FirebaseApp.initializeApp(this);

        PreferenceManager.getInstance(this);
        WidgetRepository.getInstance(this);

        initRemoteConfig();
        initNotificationChannel();
        scheduleSyncWork();
        scheduleWeatherWork();
        com.desire.widget.billing.BillingManager.getInstance(this); // connect + restore Pro
        ClockTickReceiver.ensureScheduled(this);
        registerScreenReceiver();
        registerToggleStateListeners();
        // Screen is on while the app launches — start the per-second ticker for live widgets.
        com.desire.widget.widget.SecondTicker.start(this);
    }

    /**
     * Keeps quick-settings toggle widgets in sync the moment state changes (Wi-Fi, Bluetooth,
     * flight mode, location, DND, and the torch — which has no broadcast, only a callback). These
     * live only while the process is alive; the per-minute tick is the fallback.
     */
    private void registerToggleStateListeners() {
        android.content.BroadcastReceiver stateReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                com.desire.widget.service.ToggleReceiver.renderToggleWidgets(context);
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(android.net.wifi.WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        filter.addAction(android.location.LocationManager.PROVIDERS_CHANGED_ACTION);
        filter.addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED);
        filter.addAction(android.media.AudioManager.RINGER_MODE_CHANGED_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stateReceiver, filter);
        }

        // Auto-rotate and auto-brightness have no broadcast — watch the settings themselves, so a
        // change from the system's quick settings shows on the widget too.
        android.database.ContentObserver settingsObserver = new android.database.ContentObserver(
                new android.os.Handler(android.os.Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                com.desire.widget.service.ToggleReceiver.renderToggleWidgets(WidgetApp.this);
            }
        };
        try {
            getContentResolver().registerContentObserver(android.provider.Settings.System.getUriFor(
                    android.provider.Settings.System.ACCELEROMETER_ROTATION), false, settingsObserver);
            getContentResolver().registerContentObserver(android.provider.Settings.System.getUriFor(
                    android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE), false, settingsObserver);
        } catch (Exception ignored) {}

        try {
            android.hardware.camera2.CameraManager cm =
                    (android.hardware.camera2.CameraManager) getSystemService(Context.CAMERA_SERVICE);
            if (cm != null) {
                cm.registerTorchCallback(new android.hardware.camera2.CameraManager.TorchCallback() {
                    @Override
                    public void onTorchModeChanged(String cameraId, boolean enabled) {
                        String torch = com.desire.widget.service.ToggleReceiver.torchCameraId(WidgetApp.this);
                        if (cameraId.equals(torch)
                                && enabled != com.desire.widget.engine.data.ToggleState.isTorchOn(WidgetApp.this)) {
                            com.desire.widget.engine.data.ToggleState.setTorchOn(WidgetApp.this, enabled);
                            com.desire.widget.service.ToggleReceiver.renderToggleWidgets(WidgetApp.this);
                        }
                    }
                }, new android.os.Handler(android.os.Looper.getMainLooper()));
            }
        } catch (Exception ignored) {}
    }

    private void scheduleWeatherWork() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build();

        // Refresh shortly after launch, then every few hours.
        WorkManager.getInstance(this).enqueue(
                new androidx.work.OneTimeWorkRequest.Builder(com.desire.widget.worker.WeatherWorker.class)
                        .setConstraints(constraints)
                        .build());

        PeriodicWorkRequest weatherRequest = new PeriodicWorkRequest.Builder(
                com.desire.widget.worker.WeatherWorker.class, 3, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "weather_refresh", ExistingPeriodicWorkPolicy.KEEP, weatherRequest);
    }

    private void registerScreenReceiver() {
        screenReceiver = new ClockTickReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(screenReceiver, filter);
        }
    }

    private void initRemoteConfig() {
        FirebaseRemoteConfig config = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings settings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        config.setConfigSettingsAsync(settings);
        config.setDefaultsAsync(R.xml.remote_config_defaults);
        config.fetchAndActivate();
    }

    private void initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "widgets_sync",
                    "Widget Sync",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Widget data synchronization");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void scheduleSyncWork() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                com.desire.widget.worker.SyncWorker.class,
                1, TimeUnit.HOURS
        )
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "widget_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
        );
    }
}
