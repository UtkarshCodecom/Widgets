package com.desire.widget.debug;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.WidgetEngine;
import com.desire.widget.engine.data.CachedLiveDataSource;
import com.desire.widget.engine.model.WidgetSpec;
import com.desire.widget.engine.runtime.ThemeEngine;
import com.desire.widget.widget.WidgetSchema;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * DEBUG BUILDS ONLY (src/debug). Renders every catalog widget through the real engine — in gallery
 * preview mode (appWidgetId = -1) — into labelled contact sheets under
 * {@code cache/render_dump/sheet_N.png}, for visual QA without capturing the device screen.
 *
 * <pre>adb shell am broadcast -a com.desire.widget.debug.RENDER_DUMP -n com.desire.widget/.debug.RenderDumpReceiver</pre>
 * Add {@code --es specs id1,id2} to render specific spec files (e.g. ones not in the catalog yet).
 */
public class RenderDumpReceiver extends BroadcastReceiver {
    private static final String TAG = "RenderDump";
    private static final int COLS = 4, ROWS = 4, CELL = 340, LABEL = 34, GAP = 16;

    @Override
    public void onReceive(Context context, Intent intent) {
        final PendingResult pending = goAsync();
        final Context app = context.getApplicationContext();
        if ("com.desire.widget.debug.CAPTURE_SELF".equals(intent.getAction())) {
            SelfCapture.capture(app, intent, pending::finish);
            return;
        }
        final String only = intent.getStringExtra("specs");
        new Thread(() -> {
            try {
                dump(app, only);
            } catch (Exception e) {
                Log.e(TAG, "dump failed", e);
            } finally {
                pending.finish();
            }
        }).start();
    }

    private void dump(Context app, String only) throws Exception {
        JsonArray widgets;
        if (only != null && !only.trim().isEmpty()) {
            // Explicit spec ids (e.g. designs not in the catalog yet): size comes from designSize.
            widgets = new JsonArray();
            for (String id : only.split(",")) {
                id = id.trim();
                if (id.isEmpty()) continue;
                JsonObject spec = new Gson().fromJson(read(app, "seed/native/" + id + ".json"), JsonObject.class);
                JsonObject w = new JsonObject();
                w.addProperty("id", id);
                w.addProperty("widgetSize", spec.has("designSize") ? spec.get("designSize").getAsString() : "2x2");
                w.addProperty("specAsset", id + ".json");
                widgets.add(w);
            }
        } else {
            JsonObject catalog = new Gson().fromJson(read(app, "seed/native/catalog.json"), JsonObject.class);
            widgets = catalog.getAsJsonArray("widgets");
        }
        File dir = new File(app.getCacheDir(), "render_dump");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("no dump dir");
        File[] old = dir.listFiles();
        if (old != null) for (File f : old) //noinspection ResultOfMethodCallIgnored
            f.delete();

        WidgetEngine engine = new WidgetEngine();
        List<Bitmap> renders = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int failures = 0;
        for (JsonElement el : widgets) {
            JsonObject w = el.getAsJsonObject();
            String id = w.get("id").getAsString();
            String size = WidgetSchema.normalizeSize(w.get("widgetSize").getAsString());
            try {
                WidgetSpec spec = new Gson().fromJson(read(app, "seed/native/" + w.get("specAsset").getAsString()), WidgetSpec.class);
                int[] ratio = WidgetSchema.previewRatio(size);
                int pw = WidgetSchema.renderWidthPx(size);
                int ph = Math.max(1, pw * ratio[1] / ratio[0]);
                RenderContext rc = new RenderContext(app, pw, ph, ThemeEngine.current(app),
                        System.currentTimeMillis(), new CachedLiveDataSource(app));
                renders.add(engine.render(spec, rc));
                labels.add(id + "  " + size);
            } catch (Exception e) {
                failures++;
                Log.e(TAG, "render failed: " + id, e);
                renders.add(null);
                labels.add("FAILED " + id);
            }
        }

        int perSheet = COLS * ROWS;
        int sheets = (renders.size() + perSheet - 1) / perSheet;
        for (int s = 0; s < sheets; s++) {
            int sw = COLS * (CELL + GAP) + GAP, sh = ROWS * (CELL + LABEL + GAP) + GAP;
            Bitmap sheet = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(sheet);
            c.drawColor(0xFF3A3D44);
            Paint cellBg = new Paint();
            cellBg.setColor(0xFF4A4E57);
            Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
            text.setColor(0xFFFFFFFF);
            text.setTextSize(22f);
            for (int i = 0; i < perSheet; i++) {
                int idx = s * perSheet + i;
                if (idx >= renders.size()) break;
                int col = i % COLS, row = i / COLS;
                float x = GAP + col * (CELL + GAP), y = GAP + row * (CELL + LABEL + GAP);
                c.drawRect(x, y, x + CELL, y + CELL, cellBg);
                Bitmap b = renders.get(idx);
                if (b != null) {
                    float sc = Math.min(CELL / (float) b.getWidth(), CELL / (float) b.getHeight());
                    float dw = b.getWidth() * sc, dh = b.getHeight() * sc;
                    float dx = x + (CELL - dw) / 2f, dy = y + (CELL - dh) / 2f;
                    Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG);
                    fp.setFilterBitmap(true);
                    c.drawBitmap(b, null, new RectF(dx, dy, dx + dw, dy + dh), fp);
                }
                text.setColor(b == null ? 0xFFFF6B6B : 0xFFFFFFFF);
                c.drawText(labels.get(idx), x + 4, y + CELL + 26, text);
            }
            try (FileOutputStream out = new FileOutputStream(new File(dir, "sheet_" + (s + 1) + ".png"))) {
                sheet.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            sheet.recycle();
        }
        Log.i(TAG, "dumped " + renders.size() + " widgets into " + sheets + " sheets, failures=" + failures);
    }

    private String read(Context app, String path) throws Exception {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(app.getAssets().open(path), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        }
    }
}
