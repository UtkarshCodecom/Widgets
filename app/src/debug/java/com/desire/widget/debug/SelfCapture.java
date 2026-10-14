package com.desire.widget.debug;

import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.PixelCopy;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;

/**
 * DEBUG BUILDS ONLY (src/debug). Captures THIS app's own foreground window — via PixelCopy on our
 * Activity's window, never the device screen — so layout QA never sees other apps, notifications
 * or the status bar. Output: {@code cache/render_dump/screen_<label>.png}.
 *
 * <p>Registered as a ContentProvider only to get a hook into app start-up (to track our resumed
 * Activity); it serves no data.
 *
 * <pre>adb shell am broadcast -a com.desire.widget.debug.CAPTURE_SELF --es label gallery \
 *     -n com.desire.widget/.debug.RenderDumpReceiver</pre>
 */
public class SelfCapture extends ContentProvider {
    private static final String TAG = "SelfCapture";
    private static WeakReference<Activity> resumed = new WeakReference<>(null);

    @Override
    public boolean onCreate() {
        Context c = getContext();
        if (c != null && c.getApplicationContext() instanceof Application) {
            ((Application) c.getApplicationContext()).registerActivityLifecycleCallbacks(new Tracker());
        }
        return true;
    }

    /** Called by RenderDumpReceiver for the CAPTURE_SELF action. */
    static void capture(Context app, Intent intent, Runnable done) {
        String label = intent.getStringExtra("label");
        final String name = "screen_" + (label != null ? label.replaceAll("[^a-zA-Z0-9_-]", "") : "app") + ".png";
        new Handler(Looper.getMainLooper()).post(() -> {
            Activity a = resumed.get();
            if (a == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                Log.w(TAG, "no resumed activity of ours (or API < 26) — nothing captured");
                done.run();
                return;
            }
            View decor = a.getWindow().getDecorView();
            if (decor.getWidth() <= 0 || decor.getHeight() <= 0) {
                done.run();
                return;
            }
            Bitmap bmp = Bitmap.createBitmap(decor.getWidth(), decor.getHeight(), Bitmap.Config.ARGB_8888);
            PixelCopy.request(a.getWindow(), bmp, result -> {
                if (result == PixelCopy.SUCCESS) {
                    File dir = new File(app.getCacheDir(), "render_dump");
                    //noinspection ResultOfMethodCallIgnored
                    dir.mkdirs();
                    try (FileOutputStream out = new FileOutputStream(new File(dir, name))) {
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                        Log.i(TAG, "captured " + a.getClass().getSimpleName() + " → " + name);
                    } catch (Exception e) {
                        Log.e(TAG, "save failed", e);
                    }
                } else {
                    Log.w(TAG, "PixelCopy failed: " + result);
                }
                done.run();
            }, new Handler(Looper.getMainLooper()));
        });
    }

    private static final class Tracker implements Application.ActivityLifecycleCallbacks {
        @Override public void onActivityResumed(@NonNull Activity a) { resumed = new WeakReference<>(a); }
        @Override public void onActivityPaused(@NonNull Activity a) {
            if (resumed.get() == a) resumed = new WeakReference<>(null);
        }
        @Override public void onActivityCreated(@NonNull Activity a, @Nullable Bundle b) {}
        @Override public void onActivityStarted(@NonNull Activity a) {}
        @Override public void onActivityStopped(@NonNull Activity a) {}
        @Override public void onActivitySaveInstanceState(@NonNull Activity a, @NonNull Bundle b) {}
        @Override public void onActivityDestroyed(@NonNull Activity a) {}
    }

    @Nullable @Override public Cursor query(@NonNull Uri u, @Nullable String[] p, @Nullable String s, @Nullable String[] a, @Nullable String o) { return null; }
    @Nullable @Override public String getType(@NonNull Uri uri) { return null; }
    @Nullable @Override public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) { return null; }
    @Override public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] a) { return 0; }
    @Override public int update(@NonNull Uri uri, @Nullable ContentValues v, @Nullable String s, @Nullable String[] a) { return 0; }
}
