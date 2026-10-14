package com.desire.widget.engine.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.BatteryManager;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.util.Map;

/**
 * Live battery gauge: reads the real charge level (and charging state) from BatteryManager.
 * Turns red below 20%.
 *
 * <p>props: bodyColor, fillColor, lowColor, textColor, showPercent, previewLevel (used when the
 * system level is unavailable, e.g. emulator), and {@code style}:
 * <ul>
 *   <li>shell — battery outline with a fill and percent label (default)</li>
 *   <li>bars — big percent over five segmented cells</li>
 *   <li>glow — glowing horizontal capsule with the percent beside it</li>
 *   <li>ring — circular progress ring with the percent in the centre</li>
 * </ul>
 * A bolt is drawn while charging.
 */
public class BatteryRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "battery";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        int level = batteryLevel(ctx.android);
        if (level < 0) level = SpecProps.i(p, "previewLevel", 72);
        boolean charging = isCharging(ctx.android);
        float frac = Math.max(0f, Math.min(1f, level / 100f));

        int body = SpecColors.resolve(SpecProps.str(p, "bodyColor", "@textSecondary"), ctx.theme, ctx.theme.textSecondary);
        int fillLow = SpecColors.resolve(SpecProps.str(p, "lowColor", "#FF5252"), ctx.theme, 0xFFFF5252);
        int fillCol = SpecColors.resolve(SpecProps.str(p, "fillColor", "@primary"), ctx.theme, ctx.theme.primary);
        int textColor = SpecColors.resolve(SpecProps.str(p, "textColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        int used = (frac <= 0.2f && !charging) ? fillLow : fillCol;
        boolean showPercent = SpecProps.b(p, "showPercent", true);

        switch (SpecProps.str(p, "style", "shell")) {
            case "bars":
                drawBars(canvas, b, frac, level, charging, body, used, textColor, showPercent);
                break;
            case "glow":
                drawGlow(canvas, b, frac, level, charging, body, used, textColor, showPercent);
                break;
            case "ring":
                drawRing(canvas, b, frac, level, charging, body, used, textColor, showPercent);
                break;
            default:
                drawShell(canvas, b, frac, level, charging, body, used, textColor, showPercent);
                break;
        }
    }

    private void drawShell(Canvas canvas, RectF b, float frac, int level, boolean charging,
                           int body, int used, int textColor, boolean showPercent) {
        float capW = b.width() * 0.07f;
        RectF shell = new RectF(b.left, b.top + b.height() * 0.2f, b.right - capW, b.bottom - b.height() * 0.2f);
        float r = shell.height() * 0.18f;

        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(Math.max(2f, shell.height() * 0.07f));
        stroke.setColor(body);
        canvas.drawRoundRect(shell, r, r, stroke);

        Paint capPaint = fill(body);
        RectF cap = new RectF(shell.right + capW * 0.2f, b.centerY() - shell.height() * 0.18f,
                shell.right + capW, b.centerY() + shell.height() * 0.18f);
        canvas.drawRoundRect(cap, capW * 0.3f, capW * 0.3f, capPaint);

        float pad = stroke.getStrokeWidth() * 1.4f;
        RectF inner = new RectF(shell.left + pad, shell.top + pad, shell.right - pad, shell.bottom - pad);
        RectF fillRect = new RectF(inner.left, inner.top, inner.left + inner.width() * frac, inner.bottom);
        canvas.drawRoundRect(fillRect, r * 0.6f, r * 0.6f, fill(used));

        if (showPercent) {
            Paint txt = text(textColor, shell.height() * 0.5f);
            String label = (charging ? "⚡" : "") + level + "%";
            drawCentered(canvas, label, shell.centerX(), shell.centerY(), txt);
        }
    }

    private void drawBars(Canvas canvas, RectF b, float frac, int level, boolean charging,
                          int body, int used, int textColor, boolean showPercent) {
        if (showPercent) {
            Paint txt = text(textColor, b.height() * 0.34f);
            drawCentered(canvas, level + "%", b.centerX(), b.top + b.height() * 0.30f, txt);
        }
        int cells = 5;
        float gap = b.width() * 0.035f;
        float cellW = (b.width() - gap * (cells - 1)) / cells;
        float top = b.top + b.height() * 0.58f;
        float bottom = b.top + b.height() * 0.86f;
        float r = (bottom - top) * 0.25f;
        int filled = Math.round(frac * cells);
        if (level > 0 && filled == 0) filled = 1;
        for (int i = 0; i < cells; i++) {
            float left = b.left + i * (cellW + gap);
            Paint cp = fill(i < filled ? used : body);
            if (i >= filled) cp.setAlpha(60);
            canvas.drawRoundRect(new RectF(left, top, left + cellW, bottom), r, r, cp);
        }
        if (charging) drawBolt(canvas, b.right - b.width() * 0.08f, b.top + b.height() * 0.30f, b.height() * 0.13f, used);
    }

    private void drawGlow(Canvas canvas, RectF b, float frac, int level, boolean charging,
                          int body, int used, int textColor, boolean showPercent) {
        float textW = showPercent ? b.width() * 0.30f : 0f;
        float h = b.height() * 0.34f;
        RectF track = new RectF(b.left + h * 0.3f, b.centerY() - h / 2f, b.right - textW, b.centerY() + h / 2f);
        Paint tp = fill(body);
        tp.setAlpha(55);
        canvas.drawRoundRect(track, h / 2f, h / 2f, tp);

        RectF fillRect = new RectF(track.left, track.top, track.left + Math.max(h, track.width() * frac), track.bottom);
        Paint glow = fill(used);
        glow.setShadowLayer(h * 0.9f, 0f, 0f, used); // software canvas → real glow
        canvas.drawRoundRect(fillRect, h / 2f, h / 2f, glow);

        if (charging) drawBolt(canvas, fillRect.right - h * 0.55f, track.centerY(), h * 0.38f, 0xFFFFFFFF);
        if (showPercent) {
            Paint txt = text(textColor, b.height() * 0.36f);
            drawCentered(canvas, level + "%", b.right - textW / 2f, b.centerY(), txt);
        }
    }

    private void drawRing(Canvas canvas, RectF b, float frac, int level, boolean charging,
                          int body, int used, int textColor, boolean showPercent) {
        float size = Math.min(b.width(), b.height());
        float thick = size * 0.10f;
        float rad = size / 2f - thick;
        float cx = b.centerX(), cy = b.centerY();
        RectF oval = new RectF(cx - rad, cy - rad, cx + rad, cy + rad);

        Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(thick);
        track.setColor(body);
        track.setAlpha(55);
        canvas.drawArc(oval, 0, 360, false, track);

        Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(thick);
        arc.setStrokeCap(Paint.Cap.ROUND);
        arc.setColor(used);
        canvas.drawArc(oval, -90f, 360f * frac, false, arc);

        if (showPercent) {
            Paint txt = text(textColor, size * 0.24f);
            drawCentered(canvas, level + "%", cx, cy + (charging ? size * 0.06f : 0f), txt);
        }
        if (charging) drawBolt(canvas, cx, cy - size * 0.17f, size * 0.09f, used);
    }

    // ------------------------------------------------------------------ helpers

    private Paint fill(int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        return p;
    }

    private Paint text(int color, float size) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTextAlign(Paint.Align.CENTER);
        p.setFakeBoldText(true);
        p.setTextSize(size);
        return p;
    }

    private void drawCentered(Canvas canvas, String t, float x, float y, Paint p) {
        Paint.FontMetrics fm = p.getFontMetrics();
        canvas.drawText(t, x, y - (fm.ascent + fm.descent) / 2f, p);
    }

    /** A lightning bolt centred at (cx, cy) with half-height r. */
    private void drawBolt(Canvas canvas, float cx, float cy, float r, int color) {
        Path bolt = new Path();
        bolt.moveTo(cx + r * 0.15f, cy - r);
        bolt.lineTo(cx - r * 0.55f, cy + r * 0.12f);
        bolt.lineTo(cx - r * 0.02f, cy + r * 0.12f);
        bolt.lineTo(cx - r * 0.15f, cy + r);
        bolt.lineTo(cx + r * 0.55f, cy - r * 0.12f);
        bolt.lineTo(cx + r * 0.02f, cy - r * 0.12f);
        bolt.close();
        canvas.drawPath(bolt, fill(color));
    }

    private int batteryLevel(Context ctx) {
        try {
            BatteryManager bm = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
            if (bm != null) {
                int l = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                if (l >= 0 && l <= 100) return l;
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    private boolean isCharging(Context ctx) {
        try {
            BatteryManager bm = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
            return bm != null && bm.isCharging();
        } catch (Exception e) {
            return false;
        }
    }
}
