package com.desire.widget.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.desire.widget.data.repository.WidgetRepository;
import com.desire.widget.util.PreferenceManager;

public class SyncWorker extends Worker {
    private static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting sync work");
        try {
            WidgetRepository repository = WidgetRepository.getInstance(getApplicationContext());
            // Pull published widgets/categories/themes from Firestore and merge into Room.
            // Each sync is a no-op when its Firestore collection is empty, so this is safe.
            repository.syncAllFromFirebase();

            PreferenceManager.getInstance(getApplicationContext())
                    .setLastSyncTime(System.currentTimeMillis());

            Log.d(TAG, "Sync completed successfully");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Sync failed", e);
            return Result.retry();
        }
    }
}
