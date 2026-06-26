package com.desire.widget.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.desire.widget.data.model.AppConfig;
import com.desire.widget.data.remote.FirebaseService;
import com.desire.widget.util.PreferenceManager;

public class UpdateCheckWorker extends Worker {
    private static final String TAG = "UpdateCheckWorker";

    public UpdateCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Checking for updates");
        try {
            AppConfig config = com.desire.widget.util.Tasks.await(FirebaseService.getInstance().getAppConfig());
            if (config != null) {
                int currentVersion = getApplicationContext().getPackageManager()
                        .getPackageInfo(getApplicationContext().getPackageName(), 0).versionCode;
                PreferenceManager.getInstance(getApplicationContext())
                        .setLastUpdateCheckVersion(String.valueOf(config.getLatestVersion()));

                if (config.isForceUpdate() && config.getLatestVersion() > currentVersion) {
                    Log.d(TAG, "Force update required");
                } else if (config.getLatestVersion() > currentVersion) {
                    Log.d(TAG, "Update available");
                }
            }
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Update check failed", e);
            return Result.retry();
        }
    }
}
