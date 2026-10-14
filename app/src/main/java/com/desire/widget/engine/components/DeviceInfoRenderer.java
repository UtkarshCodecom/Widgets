package com.desire.widget.engine.components;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Environment;
import android.os.StatFs;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.util.Locale;
import java.util.Map;

/**
 * Live device resources: RAM in use (ActivityManager.MemoryInfo) and internal storage used
 * (StatFs on the data partition). No permissions needed.
 *
 * <p>props: metric (memory|storage|both), style (ring|bar), memoryColor, storageColor,
 * trackColor, textColor, subColor.
 */
public class DeviceInfoRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "device_info";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        String metric = SpecProps.str(p, "metric", "memory");
        String style = SpecProps.str(p, "style", "both".equals(metric) ? "bar" : "ring");
        int memColor = SpecColors.resolve(SpecProps.str(p, "memoryColor", "@primary"), ctx.theme, ctx.theme.primary);
        int stoColor = SpecColors.resolve(SpecProps.str(p, "storageColor", "#4DD0E1"), ctx.theme, 0xFF4DD0E1);
        int track = SpecColors.resolve(SpecProps.str(p, "trackColor", "#22FFFFFF"), ctx.theme, 0x22FFFFFF);
        int textColor = SpecColors.resolve(SpecProps.str(p, "textColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        int subColor = SpecColors.resolve(SpecProps.str(p, "subColor", "@textSecondary"), ctx.theme, ctx.theme.textSecondary);

        long[] mem = memory(ctx.android);
        long[] sto = storage();

        if ("both".equals(metric)) {
            float rowH = b.height() / 2f;
            drawBarRow(canvas, new RectF(b.left, b.top, b.right, b.top + rowH), "RAM", mem, memColor, track, textColor, subColor);
            drawBarRow(canvas, new RectF(b.left, b.top + rowH, b.right, b.bottom), "Storage", sto, stoColor, track, textColor, subColor);
            return;
        }
        boolean isMem = !"storage".equals(metric);
        long[] v = isMem ? mem : sto;
        int color = isMem ? memColor : stoColor;
        String label = isMem ? "RAM" : "Storage";
        if ("bar".equals(style)) {
            drawBarRow(canvas, b, label, v, color, track, textColor, subColor);
        } else {
            drawRing(canvas, b, label, v, color, track, textColor, subColor);
        }
    }

    private void drawRing(Canvas canvas, RectF b, String label, long[] v, int color, int track,
                          int textColor, int subColor) {
        float frac = frac(v);
        float size = Math.min(b.width(), b.height() * 0.80f);
        float thick = size * 0.11f;
        float rad = size / 2f - thick / 2f;
        float cx = b.centerX();
        float cy = b.top + size / 2f;
        RectF oval = new RectF(cx - rad, cy - rad, cx + rad, cy + rad);

        Paint tp = stroke(track, thick);
        canvas.drawArc(oval, 0, 360, false, tp);
        Paint ap = stroke(color, thick);
        ap.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawArc(oval, -90f, Math.max(1f, 360f * frac), false, ap);

        Paint pct = text(textColor, size * 0.24f, true);
        drawCentered(canvas, Math.round(frac * 100) + "%", cx, cy - size * 0.05f, pct);
        Paint lbl = text(subColor, size * 0.11f, false);
        drawCentered(canvas, label, cx, cy + size * 0.16f, lbl);

        Paint detail = text(subColor, b.height() * 0.085f, false);
        drawCentered(canvas, gb(v[0]) + " / " + gb(v[1]) + " GB", cx, b.top + size + (b.bottom - b.top - size) / 2f, detail);
    }

    private void drawBarRow(Canvas canvas, RectF r, String label, long[] v, int color, int track,
                            int textColor, int subColor) {
        float frac = frac(v);
        float h = r.height();
        Paint lbl = new Paint(Paint.ANTI_ALIAS_FLAG);
        lbl.setColor(textColor);
        lbl.setFakeBoldText(true);
        lbl.setTextSize(h * 0.24f);
        canvas.drawText(label, r.left, r.top + h * 0.36f, lbl);

        Paint val = new Paint(Paint.ANTI_ALIAS_FLAG);
        val.setColor(subColor);
        val.setTextSize(h * 0.20f);
        val.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(gb(v[0]) + " / " + gb(v[1]) + " GB · " + Math.round(frac * 100) + "%",
                r.right, r.top + h * 0.36f, val);

        float barTop = r.top + h * 0.50f;
        float barH = h * 0.20f;
        RectF tr = new RectF(r.left, barTop, r.right, barTop + barH);
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(track);
        canvas.drawRoundRect(tr, barH / 2f, barH / 2f, tp);
        Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG);
        fp.setColor(color);
        canvas.drawRoundRect(new RectF(r.left, barTop, r.left + Math.max(barH, r.width() * frac), barTop + barH),
                barH / 2f, barH / 2f, fp);
    }

    // ------------------------------------------------------------------ data

    /** {used, total} bytes of RAM. */
    private long[] memory(Context ctx) {
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            return new long[]{mi.totalMem - mi.availMem, mi.totalMem};
        } catch (Exception e) {
            return new long[]{0, 0};
        }
    }

    /** {used, total} bytes of internal storage. */
    private long[] storage() {
        try {
            StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
            long total = fs.getTotalBytes();
            return new long[]{total - fs.getAvailableBytes(), total};
        } catch (Exception e) {
            return new long[]{0, 0};
        }
    }

    private float frac(long[] v) {
        return v[1] > 0 ? Math.max(0f, Math.min(1f, v[0] / (float) v[1])) : 0f;
    }

    private String gb(long bytes) {
        double g = bytes / 1_000_000_000d; // decimal GB, matching how devices advertise capacity
        return g >= 100 ? String.format(Locale.US, "%.0f", g) : String.format(Locale.US, "%.1f", g);
    }

    private Paint stroke(int color, float w) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(w);
        p.setColor(color);
        return p;
    }

    private Paint text(int color, float size, boolean bold) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTextAlign(Paint.Align.CENTER);
        p.setFakeBoldText(bold);
        p.setTextSize(size);
        return p;
    }

    private void drawCentered(Canvas canvas, String t, float x, float y, Paint p) {
        Paint.FontMetrics fm = p.getFontMetrics();
        canvas.drawText(t, x, y - (fm.ascent + fm.descent) / 2f, p);
    }
}
