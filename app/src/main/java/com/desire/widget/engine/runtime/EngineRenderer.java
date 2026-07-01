package com.desire.widget.engine.runtime;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;

import com.desire.widget.MainActivity;
import com.desire.widget.R;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.RenderTheme;
import com.desire.widget.engine.WidgetEngine;
import com.desire.widget.engine.action.ActionCompiler;
import com.desire.widget.engine.data.CachedLiveDataSource;
import com.desire.widget.engine.model.WidgetSpec;
import com.desire.widget.util.AppExecutors;
import com.desire.widget.widget.WidgetSchema;
import com.google.gson.Gson;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Renders native {@link WidgetSpec}s into installed home-screen widgets. For each appWidgetId it
 * loads the bound spec, sizes the bitmap to the launcher's actual allocation, applies the current
 * theme + live data, renders via the shared {@link WidgetEngine}, and pushes the bitmap into the
 * RemoteViews ImageView with a compiled click PendingIntent.
 *
 * <p>Rendering is pure Canvas (no WebView), so it runs on a background executor — never blocking
 * the main thread. Bitmaps are cached per appWidgetId and reused when the size hasn't changed,
 * reducing GC pressure on frequent per-minute refreshes.
 */
public final class EngineRenderer {
    private static final Gson GSON = new Gson();
    private static final WidgetEngine ENGINE = new WidgetEngine();
    private static final ConcurrentHashMap<Integer, Bitmap> BITMAP_CACHE = new ConcurrentHashMap<>();

    private EngineRenderer() {}

    public static void render(Context context, int[] appWidgetIds) {
        render(context, appWidgetIds, null);
    }

    public static void render(Context context, int[] appWidgetIds, @Nullable Runnable onComplete) {
        if (appWidgetIds == null || appWidgetIds.length == 0) {
            if (onComplete != null) onComplete.run();
            return;
        }
        final Context app = context.getApplicationContext();
        final int[] ids = appWidgetIds.clone();
        final AtomicInteger remaining = new AtomicInteger(ids.length);
        for (int appWidgetId : ids) {
            AppExecutors.getInstance().diskIO().execute(() -> {
                try {
                    renderOne(app, appWidgetId);
                } catch (Exception ignored) {
                } finally {
                    if (remaining.decrementAndGet() == 0 && onComplete != null) onComplete.run();
                }
            });
        }
    }

    /** Remove cached bitmap for a deleted widget. */
    public static void evict(int appWidgetId) {
        Bitmap old = BITMAP_CACHE.remove(appWidgetId);
        if (old != null && !old.isRecycled()) old.recycle();
    }

    private static void renderOne(Context app, int appWidgetId) {
        String specJson = EngineWidgetStore.getSpecJson(app, appWidgetId);
        if (specJson == null || specJson.isEmpty()) return;

        WidgetSpec spec;
        try {
            spec = GSON.fromJson(specJson, WidgetSpec.class);
        } catch (Exception e) {
            return;
        }
        if (spec == null) return;

        String size = EngineWidgetStore.getSize(app, appWidgetId);
        int[] px = resolvePixelSize(app, appWidgetId, size);
        RenderTheme theme = ThemeEngine.current(app);

        RenderContext ctx = new RenderContext(app, px[0], px[1], theme,
                System.currentTimeMillis(), new CachedLiveDataSource(app));

        // Reuse bitmap if size matches to reduce GC pressure.
        int w = px[0], h = px[1];
        Bitmap cached = BITMAP_CACHE.get(appWidgetId);
        Bitmap bitmap;
        if (cached != null && !cached.isRecycled() && cached.getWidth() == w && cached.getHeight() == h) {
            bitmap = cached;
            bitmap.eraseColor(0);
        } else {
            if (cached != null && !cached.isRecycled()) cached.recycle();
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            BITMAP_CACHE.put(appWidgetId, bitmap);
        }

        ENGINE.render(spec, ctx, bitmap);

        RemoteViews views = new RemoteViews(app.getPackageName(), R.layout.widget_layout);
        views.setImageViewBitmap(R.id.widget_image, bitmap);
        views.setOnClickPendingIntent(R.id.widget_image, clickIntent(app, appWidgetId, spec));
        AppWidgetManager.getInstance(app).updateAppWidget(appWidgetId, views);
    }

    private static int[] resolvePixelSize(Context app, int appWidgetId, String size) {
        int w = WidgetSchema.renderWidthPx(size);
        int h = WidgetSchema.renderHeightPx(size);
        try {
            Bundle opts = AppWidgetManager.getInstance(app).getAppWidgetOptions(appWidgetId);
            float density = app.getResources().getDisplayMetrics().density;
            int wDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0);
            int hDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
            if (wDp > 20 && hDp > 20) {
                w = Math.round(wDp * density);
                h = Math.round(hDp * density);
            }
        } catch (Exception ignored) {}
        int max = 1600;
        w = Math.min(max, Math.max(1, w));
        h = Math.min(max, Math.max(1, h));
        return new int[]{w, h};
    }

    private static PendingIntent clickIntent(Context app, int appWidgetId, WidgetSpec spec) {
        PendingIntent pi = ActionCompiler.compile(app, appWidgetId, spec.action);
        if (pi != null) return pi;
        Intent intent = new Intent(app, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(app, appWidgetId, intent, flags);
    }
}
