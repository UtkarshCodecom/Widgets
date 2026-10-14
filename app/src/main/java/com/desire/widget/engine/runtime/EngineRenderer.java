package com.desire.widget.engine.runtime;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
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
import com.desire.widget.engine.model.WidgetStyle;
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
 * the main thread. Every render allocates a fresh {@link Bitmap} (never mutates a previously-sent
 * one — see the note in {@link #renderOneOrThrow}); the prior bitmap for that widget is recycled
 * only after the new one has been handed off.
 */
public final class EngineRenderer {
    private static final String TAG = "EngineRenderer";
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
                } catch (Exception e) {
                    // renderOne() already catches internally and pushes a visible error state;
                    // this is a last-ditch safety net so a truly unexpected throw is still logged.
                    Log.e(TAG, "Unhandled render failure for widget " + appWidgetId, e);
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
        try {
            renderOneOrThrow(app, appWidgetId);
        } catch (Exception e) {
            // Never leave a widget with NO update at all (that shows as a blank/white rectangle
            // forever, since the launcher's un-populated initial layout is transparent). Always
            // push something visible, and log the real cause so it's actually debuggable.
            Log.e(TAG, "Render failed for widget " + appWidgetId, e);
            try {
                pushError(app, appWidgetId, e);
            } catch (Exception ignored2) {}
        }
    }

    private static void renderOneOrThrow(Context app, int appWidgetId) {
        String specJson = EngineWidgetStore.getSpecJson(app, appWidgetId);
        if (specJson == null || specJson.isEmpty()) {
            // Orphaned widget (e.g. prefs wiped on reinstall): show a tappable placeholder that
            // opens the app, instead of a dead/blank widget that does nothing on tap.
            pushPlaceholder(app, appWidgetId);
            return;
        }

        WidgetSpec spec = GSON.fromJson(specJson, WidgetSpec.class);
        if (spec == null) {
            Log.e(TAG, "Spec parsed to null for widget " + appWidgetId + ", json=" + specJson);
            pushPlaceholder(app, appWidgetId);
            return;
        }

        String size = EngineWidgetStore.getSize(app, appWidgetId);
        int[] px = resolvePixelSize(app, appWidgetId, size);

        // Apply the per-widget visual style (colors / background / opacity chosen in Customize).
        WidgetStyle style = null;
        String styleJson = EngineWidgetStore.getStyleJson(app, appWidgetId);
        if (styleJson != null && !styleJson.isEmpty()) {
            try {
                style = GSON.fromJson(styleJson, WidgetStyle.class);
            } catch (Exception ignored) {}
        }
        RenderTheme theme = StyleApplier.apply(spec, style, ThemeEngine.current(app));
        injectCalendarMonthOffset(spec, app, appWidgetId);

        RenderContext ctx = new RenderContext(app, px[0], px[1], theme,
                System.currentTimeMillis(), new CachedLiveDataSource(app));
        ctx.appWidgetId = appWidgetId; // per-instance data (notes/todo/qr)

        // Always allocate a FRESH bitmap for every render — never reuse/mutate a bitmap that was
        // already handed to updateAppWidget(). RemoteViews' bitmap transport can share memory
        // (ashmem) with the host rather than eagerly copying it, especially for a widget sitting
        // on a launcher page that isn't currently visible (which all of ours are, per
        // `dumpsys appwidget` — visible=false). Erasing/redrawing into that same Bitmap object on
        // the next render cycle can race with the host reading it, corrupting or blanking the
        // widget with no exception on our side to catch. A fresh allocation per render (~1-2MB,
        // GC'd right after) is a trivial cost for a once-a-minute render and removes this whole
        // class of bug.
        int w = px[0], h = px[1];
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        ENGINE.render(spec, ctx, bitmap);

        // Recycle the previous bitmap for this widget now that the new one is ready to send — by
        // this point the old one has already completed a prior updateAppWidget() call, so it's
        // safe to drop.
        Bitmap old = BITMAP_CACHE.put(appWidgetId, bitmap);
        if (old != null && !old.isRecycled()) old.recycle();

        // To-Do lists: a strip overlay so every row can be ticked off right on the home screen.
        int todoRows = todoListRows(spec);
        if (todoRows > 0) {
            pushTodoList(app, appWidgetId, todoRows, bitmap);
            return;
        }

        // Toggle widgets need a tap target per tile → the 2x4 grid layout instead of the standard
        // overlay. Everything else keeps widget_layout.xml (calendar nav / music / timer regions).
        boolean grid = hasToggleComponent(spec);
        RemoteViews views = new RemoteViews(app.getPackageName(),
                grid ? R.layout.widget_layout_grid : R.layout.widget_layout);
        views.setImageViewBitmap(R.id.widget_image, bitmap);
        // Stopwatch/Pomodoro widgets: the whole-widget tap starts/pauses the timer instead of
        // opening an app — that IS their primary action.
        PendingIntent click = hasTimerComponent(spec) ? timerToggleIntent(app, appWidgetId) : tapIntent(app, appWidgetId);
        // Set on both the image and the root so every launcher delivers the tap.
        views.setOnClickPendingIntent(R.id.widget_image, click);
        views.setOnClickPendingIntent(R.id.widget_root, click);

        if (grid) {
            wireToggleGrid(app, appWidgetId, spec, views);
            AppWidgetManager.getInstance(app).updateAppWidget(appWidgetId, views);
            return;
        }

        // Interactive tap regions layered over the bitmap (see widget_layout.xml). Only wired for
        // the widget types that need them; for every other widget the cells stay un-clickable and
        // taps fall through to the whole-widget action set above.
        if (hasMusicComponent(spec)) {
            views.setOnClickPendingIntent(R.id.widget_ctrl_prev,
                    mediaControlIntent(app, appWidgetId, com.desire.widget.service.MediaControlReceiver.CMD_PREV));
            views.setOnClickPendingIntent(R.id.widget_ctrl_playpause,
                    mediaControlIntent(app, appWidgetId, com.desire.widget.service.MediaControlReceiver.CMD_PLAY_PAUSE));
            views.setOnClickPendingIntent(R.id.widget_ctrl_next,
                    mediaControlIntent(app, appWidgetId, com.desire.widget.service.MediaControlReceiver.CMD_NEXT));
            // Tap-to-seek: 8 zones over the progress bar, each seeking to the centre of its band.
            for (int i = 0; i < SEEK_CELL_IDS.length; i++) {
                float fraction = (i + 0.5f) / SEEK_CELL_IDS.length;
                views.setOnClickPendingIntent(SEEK_CELL_IDS[i], seekIntent(app, appWidgetId, i, fraction));
            }
        } else if (hasTimerComponent(spec)) {
            // Whole-widget tap already toggles start/pause; the bottom-right region resets instead,
            // matching the reset glyph the Stopwatch/Pomodoro renderers draw there.
            views.setOnClickPendingIntent(R.id.widget_ctrl_next, timerResetIntent(app, appWidgetId));
        }

        if (hasCalendarTitleBar(spec)) {
            views.setOnClickPendingIntent(R.id.widget_cal_prev,
                    calendarNavIntent(app, appWidgetId, com.desire.widget.service.CalendarNavReceiver.CMD_PREV));
            views.setOnClickPendingIntent(R.id.widget_cal_today,
                    calendarNavIntent(app, appWidgetId, com.desire.widget.service.CalendarNavReceiver.CMD_TODAY));
            views.setOnClickPendingIntent(R.id.widget_cal_next,
                    calendarNavIntent(app, appWidgetId, com.desire.widget.service.CalendarNavReceiver.CMD_NEXT));
        }

        AppWidgetManager.getInstance(app).updateAppWidget(appWidgetId, views);
    }

    /** Last-resort visible state: a red-flagged error card with the exception message, so a bug
     *  is at least VISIBLE (never a silent blank/white rectangle) and reportable by the user. */
    private static void pushError(Context app, int appWidgetId, Exception cause) {
        int[] px = resolvePixelSize(app, appWidgetId, "2x2");
        Bitmap bmp = Bitmap.createBitmap(px[0], px[1], Bitmap.Config.ARGB_8888);
        android.graphics.Canvas c = new android.graphics.Canvas(bmp);
        android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF2A1414);
        float cr = Math.min(px[0], px[1]) * 0.12f;
        c.drawRoundRect(new android.graphics.RectF(0, 0, px[0], px[1]), cr, cr, p);
        p.setColor(0xFFEF5350);
        p.setTextAlign(android.graphics.Paint.Align.CENTER);
        p.setTextSize(Math.min(px[0], px[1]) * 0.08f);
        String msg = "Widget error — tap to reopen";
        android.graphics.Paint.FontMetrics fm = p.getFontMetrics();
        c.drawText(msg, px[0] / 2f, px[1] / 2f - (fm.ascent + fm.descent) / 2f, p);

        RemoteViews views = new RemoteViews(app.getPackageName(), R.layout.widget_layout);
        views.setImageViewBitmap(R.id.widget_image, bmp);
        PendingIntent open = appIntent(app, appWidgetId);
        views.setOnClickPendingIntent(R.id.widget_image, open);
        views.setOnClickPendingIntent(R.id.widget_root, open);
        AppWidgetManager.getInstance(app).updateAppWidget(appWidgetId, views);
    }

    /** {@code listRows} of a tappable to-do list component, or 0 when the spec has none. */
    private static int todoListRows(WidgetSpec spec) {
        if (spec.components == null) return 0;
        for (com.desire.widget.engine.model.ComponentSpec c : spec.components) {
            if (c != null && "todo".equals(c.type)) {
                return (int) com.desire.widget.engine.util.SpecProps.f(c.props, "listRows", 0f);
            }
        }
        return 0;
    }

    private static final int[] STRIP_IDS = {
            R.id.widget_strip_0, R.id.widget_strip_1, R.id.widget_strip_2, R.id.widget_strip_3,
            R.id.widget_strip_4, R.id.widget_strip_5, R.id.widget_strip_6, R.id.widget_strip_7,
            R.id.widget_strip_8, R.id.widget_strip_9, R.id.widget_strip_10, R.id.widget_strip_11,
            R.id.widget_strip_12, R.id.widget_strip_13, R.id.widget_strip_14, R.id.widget_strip_15,
            R.id.widget_strip_16, R.id.widget_strip_17, R.id.widget_strip_18, R.id.widget_strip_19,
    };

    /**
     * widget_layout_list: each task row's strips tick that task (by id); the header, empty rows and
     * "+N more" stay unwired, so those taps reach widget_root and open the editor.
     */
    private static void pushTodoList(Context app, int appWidgetId, int rows, Bitmap bitmap) {
        RemoteViews views = new RemoteViews(app.getPackageName(), R.layout.widget_layout_list);
        views.setImageViewBitmap(R.id.widget_image, bitmap);
        PendingIntent open = tapIntent(app, appWidgetId);
        views.setOnClickPendingIntent(R.id.widget_image, open);
        views.setOnClickPendingIntent(R.id.widget_root, open);

        java.util.List<com.desire.widget.engine.data.UserDataStore.Todo> todos =
                com.desire.widget.engine.data.UserDataStore.getTodos(app, appWidgetId);
        int[] lay = com.desire.widget.engine.components.TodoRenderer.layoutFor(rows);
        int shown = com.desire.widget.engine.components.TodoRenderer.visibleTaskRows(rows, todos.size());
        for (int row = 0; row < shown; row++) {
            PendingIntent tick = com.desire.widget.service.TodoToggleReceiver.pendingIntent(
                    app, appWidgetId, row, todos.get(row).id);
            int first = lay[0] + row * lay[1];
            for (int k = 0; k < lay[1] && first + k < STRIP_IDS.length; k++) {
                views.setOnClickPendingIntent(STRIP_IDS[first + k], tick);
            }
        }
        AppWidgetManager.getInstance(app).updateAppWidget(appWidgetId, views);
    }

    private static boolean hasToggleComponent(WidgetSpec spec) {
        if (spec.components == null) return false;
        for (com.desire.widget.engine.model.ComponentSpec c : spec.components) {
            if (c != null && "toggle".equals(c.type)) return true;
        }
        return false;
    }

    /** The 8 grid tap cells of widget_layout_grid.xml, row-major (row 0 = top). */
    private static final int[] GRID_CELL_IDS = {
            R.id.widget_grid_0, R.id.widget_grid_1, R.id.widget_grid_2, R.id.widget_grid_3,
            R.id.widget_grid_4, R.id.widget_grid_5, R.id.widget_grid_6, R.id.widget_grid_7
    };

    /**
     * Gives each toggle its own tap target: every grid cell (4 columns x 2 rows) whose centre lies
     * inside a toggle's frame gets that toggle's PendingIntent. So a 4x1 row of four toggles maps
     * to the four columns, a 2x2 grid of four maps to quadrants, and a single 1x1 toggle owns all
     * cells. Cells covering no toggle stay un-clickable (taps fall through to the root).
     */
    private static void wireToggleGrid(Context app, int appWidgetId, WidgetSpec spec, RemoteViews views) {
        for (int cell = 0; cell < GRID_CELL_IDS.length; cell++) {
            float cellCx = ((cell % 4) + 0.5f) / 4f;
            float cellCy = ((cell / 4) + 0.5f) / 2f;
            for (com.desire.widget.engine.model.ComponentSpec c : spec.components) {
                if (c == null || !"toggle".equals(c.type) || c.frame == null) continue;
                com.desire.widget.engine.model.Frame f = c.frame;
                if (cellCx >= f.x && cellCx <= f.x + f.w && cellCy >= f.y && cellCy <= f.y + f.h) {
                    String kind = c.props != null && c.props.get("kind") != null
                            ? c.props.get("kind").toString() : com.desire.widget.engine.data.ToggleState.WIFI;
                    views.setOnClickPendingIntent(GRID_CELL_IDS[cell],
                            com.desire.widget.service.ToggleReceiver.pendingIntentFor(app, appWidgetId, kind));
                    break;
                }
            }
        }
    }

    private static boolean hasMusicComponent(WidgetSpec spec) {
        if (spec.components == null) return false;
        for (com.desire.widget.engine.model.ComponentSpec c : spec.components) {
            if (c != null && "music".equals(c.type)) return true;
        }
        return false;
    }

    private static boolean hasTimerComponent(WidgetSpec spec) {
        if (spec.components == null) return false;
        for (com.desire.widget.engine.model.ComponentSpec c : spec.components) {
            if (c != null && ("stopwatch".equals(c.type) || "pomodoro".equals(c.type))) return true;
        }
        return false;
    }

    private static PendingIntent timerToggleIntent(Context app, int appWidgetId) {
        return timerIntent(app, appWidgetId, null, appWidgetId * 10 + 8);
    }

    private static PendingIntent timerResetIntent(Context app, int appWidgetId) {
        return timerIntent(app, appWidgetId,
                com.desire.widget.service.TimerControlReceiver.CMD_RESET, appWidgetId * 10 + 9);
    }

    /**
     * Toggle and reset both target {@link com.desire.widget.service.TimerControlReceiver} with the
     * same action and differ only by an extra — which {@code PendingIntent} equality ignores — so
     * they MUST use distinct request codes to stay independent.
     */
    private static PendingIntent timerIntent(Context app, int appWidgetId, String command, int requestCode) {
        Intent intent = new Intent(app, com.desire.widget.service.TimerControlReceiver.class);
        intent.setAction(com.desire.widget.service.TimerControlReceiver.ACTION_TOGGLE);
        intent.putExtra(com.desire.widget.service.TimerControlReceiver.EXTRA_APPWIDGET_ID, appWidgetId);
        if (command != null) {
            intent.putExtra(com.desire.widget.service.TimerControlReceiver.EXTRA_COMMAND, command);
        }
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(app, requestCode, intent, flags);
    }

    private static boolean hasCalendarTitleBar(WidgetSpec spec) {
        if (spec.components == null) return false;
        for (com.desire.widget.engine.model.ComponentSpec c : spec.components) {
            if (c != null && "calendar".equals(c.type) && c.props != null
                    && com.desire.widget.engine.util.SpecProps.b(c.props, "showTitleBar", false)) {
                return true;
            }
        }
        return false;
    }

    /** Overwrites every calendar component's monthOffset with this widget's live navigation state. */
    private static void injectCalendarMonthOffset(WidgetSpec spec, Context app, int appWidgetId) {
        if (spec.components == null) return;
        int offset = com.desire.widget.engine.data.CalendarStateStore.getMonthOffset(app, appWidgetId);
        for (com.desire.widget.engine.model.ComponentSpec c : spec.components) {
            if (c != null && "calendar".equals(c.type)) {
                if (c.props == null) c.props = new java.util.HashMap<>();
                c.props.put("monthOffset", offset);
            }
        }
    }

    private static PendingIntent calendarNavIntent(Context app, int appWidgetId, String command) {
        Intent intent = new Intent(app, com.desire.widget.service.CalendarNavReceiver.class);
        intent.setAction(com.desire.widget.service.CalendarNavReceiver.ACTION_NAV);
        intent.putExtra(com.desire.widget.service.CalendarNavReceiver.EXTRA_COMMAND, command);
        intent.putExtra(com.desire.widget.service.CalendarNavReceiver.EXTRA_APPWIDGET_ID, appWidgetId);
        int cmdIndex = com.desire.widget.service.CalendarNavReceiver.CMD_PREV.equals(command) ? 0
                : com.desire.widget.service.CalendarNavReceiver.CMD_TODAY.equals(command) ? 1 : 2;
        int requestCode = appWidgetId * 10 + 3 + cmdIndex; // offset past the music command codes (0-2)
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(app, requestCode, intent, flags);
    }

    private static PendingIntent mediaControlIntent(Context app, int appWidgetId, String command) {
        Intent intent = new Intent(app, com.desire.widget.service.MediaControlReceiver.class);
        intent.setAction(com.desire.widget.service.MediaControlReceiver.ACTION_CONTROL);
        intent.putExtra(com.desire.widget.service.MediaControlReceiver.EXTRA_COMMAND, command);
        intent.putExtra(com.desire.widget.service.MediaControlReceiver.EXTRA_APPWIDGET_ID, appWidgetId);
        // Distinct request code per (widget, command) so all three PendingIntents stay independent.
        int cmdIndex = com.desire.widget.service.MediaControlReceiver.CMD_PREV.equals(command) ? 0
                : com.desire.widget.service.MediaControlReceiver.CMD_PLAY_PAUSE.equals(command) ? 1 : 2;
        int requestCode = appWidgetId * 10 + cmdIndex;
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(app, requestCode, intent, flags);
    }

    /** The 8 seek tap-zones over the music progress bar (left→right), from widget_layout.xml. */
    private static final int[] SEEK_CELL_IDS = {
            R.id.widget_seek_0, R.id.widget_seek_1, R.id.widget_seek_2, R.id.widget_seek_3,
            R.id.widget_seek_4, R.id.widget_seek_5, R.id.widget_seek_6, R.id.widget_seek_7
    };

    private static PendingIntent seekIntent(Context app, int appWidgetId, int cellIndex, float fraction) {
        Intent intent = new Intent(app, com.desire.widget.service.MediaControlReceiver.class);
        intent.setAction(com.desire.widget.service.MediaControlReceiver.ACTION_CONTROL);
        intent.putExtra(com.desire.widget.service.MediaControlReceiver.EXTRA_COMMAND,
                com.desire.widget.service.MediaControlReceiver.CMD_SEEK);
        intent.putExtra(com.desire.widget.service.MediaControlReceiver.EXTRA_FRACTION, fraction);
        intent.putExtra(com.desire.widget.service.MediaControlReceiver.EXTRA_APPWIDGET_ID, appWidgetId);
        // Seek PendingIntents share the receiver+action with the transport buttons (which ignore
        // extras for equality), so they need request codes far from the transport range
        // (appWidgetId*10 + 0..2) to stay independent.
        int requestCode = 1_000_000 + appWidgetId * 10 + cellIndex;
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(app, requestCode, intent, flags);
    }

    /**
     * Every tap goes to our own {@link com.desire.widget.widget.WidgetTapActivity}, which always
     * resolves and then launches the real target. This is the reliable path — a widget PendingIntent
     * aimed straight at another app's implicit intent silently no-ops when it doesn't resolve.
     */
    private static PendingIntent tapIntent(Context app, int appWidgetId) {
        Intent intent = new Intent(app, com.desire.widget.widget.WidgetTapActivity.class);
        intent.putExtra(com.desire.widget.widget.WidgetTapActivity.EXTRA_APPWIDGET_ID, appWidgetId);
        // Unique data so each widget's PendingIntent is distinct.
        intent.setData(android.net.Uri.parse("widgettap://" + appWidgetId));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(app, appWidgetId, intent, flags);
    }

    private static void pushPlaceholder(Context app, int appWidgetId) {
        int[] px = resolvePixelSize(app, appWidgetId, "2x2");
        Bitmap bmp = Bitmap.createBitmap(px[0], px[1], Bitmap.Config.ARGB_8888);
        android.graphics.Canvas c = new android.graphics.Canvas(bmp);
        android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFFEDF0F5);
        float cr = Math.min(px[0], px[1]) * 0.12f;
        c.drawRoundRect(new android.graphics.RectF(0, 0, px[0], px[1]), cr, cr, p);
        p.setColor(0xFF5C6470);
        p.setTextAlign(android.graphics.Paint.Align.CENTER);
        p.setTextSize(Math.min(px[0], px[1]) * 0.09f);
        android.graphics.Paint.FontMetrics fm = p.getFontMetrics();
        c.drawText("Tap to set up", px[0] / 2f, px[1] / 2f - (fm.ascent + fm.descent) / 2f, p);

        RemoteViews views = new RemoteViews(app.getPackageName(), R.layout.widget_layout);
        views.setImageViewBitmap(R.id.widget_image, bmp);
        PendingIntent open = appIntent(app, appWidgetId);
        views.setOnClickPendingIntent(R.id.widget_image, open);
        views.setOnClickPendingIntent(R.id.widget_root, open);
        AppWidgetManager.getInstance(app).updateAppWidget(appWidgetId, views);
    }

    private static PendingIntent appIntent(Context app, int appWidgetId) {
        Intent intent = new Intent(app, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(app, appWidgetId, intent, flags);
    }

    private static int[] resolvePixelSize(Context app, int appWidgetId, String size) {
        float density = app.getResources().getDisplayMetrics().density;
        int[] ratio = WidgetSchema.previewRatio(size);
        int wDp = 0;
        try {
            Bundle opts = AppWidgetManager.getInstance(app).getAppWidgetOptions(appWidgetId);
            // MAX_WIDTH is the widget's allocated width; MIN_WIDTH as a fallback.
            wDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0);
            if (wDp <= 20) wDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
        } catch (Exception ignored) {}

        int w = wDp > 20 ? Math.round(wDp * density) : WidgetSchema.renderWidthPx(size);
        // Derive height from the DESIGN aspect ratio, NOT the launcher's tall MAX_HEIGHT bounding
        // box — otherwise a 2x2 renders portrait and its content overflows/clips.
        int h = Math.round(w * (float) ratio[1] / ratio[0]);

        int max = 1600;
        w = Math.min(max, Math.max(1, w));
        h = Math.min(max, Math.max(1, h));
        return new int[]{w, h};
    }

}
