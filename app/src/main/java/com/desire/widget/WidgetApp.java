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
        instance = this;

        FirebaseApp.initializeApp(this);

        PreferenceManager.getInstance(this);
        WidgetRepository.getInstance(this);

        initRemoteConfig();
        initNotificationChannel();
        scheduleSyncWork();
        ClockTickReceiver.ensureScheduled(this);
        registerScreenReceiver();
    }

    private void registerScreenReceiver() {
        screenReceiver = new ClockTickReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
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
