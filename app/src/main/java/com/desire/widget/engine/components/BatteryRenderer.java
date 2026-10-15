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
 *   <li>dots — device name + percent over a 10x4 grid of cells (one per 2.5%)</li>
 *   <li>pills — percent, eight tall pill cells and the time left / to full</li>
 *   <li>fill — a big card filled to the level with percent, device name and time to full</li>
 *   <li>glowCard — percent and time to full above a glowing capsule</li>
 * </ul>
 * A bolt is drawn while charging. {@code trackColor} tints the unfilled part, {@code subColor}
 * the secondary text, {@code font} the typeface.
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
            case "dots":
            case "pills":
            case "fill":
            case "glowCard":
                new Rich(canvas, b, p, ctx, level, frac, charging, body, used, textColor)
                        .draw(SpecProps.str(p, "style", "shell"));
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
        drawGlowFill(canvas, fillRect, h / 2f, used, h * 0.6f);

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
    /**
     * A filled capsule with a soft halo: a blurred copy (BlurMaskFilter) under the solid fill.
     * setShadowLayer() left a dark box around the glow on some renders, this does not.
     */
    private static void drawGlowFill(Canvas c, RectF r, float radius, int color, float blur) {
        Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
        halo.setColor((color & 0x00FFFFFF) | 0xAA000000);
        halo.setMaskFilter(new android.graphics.BlurMaskFilter(blur, android.graphics.BlurMaskFilter.Blur.NORMAL));
        c.drawRoundRect(r, radius, radius, halo);
        Paint solid = new Paint(Paint.ANTI_ALIAS_FLAG);
        solid.setColor(color);
        c.drawRoundRect(r, radius, radius, solid);
    }

    // ------------------------------------------------------------------ rich styles

    /** "35 min to full", "~6 h 20 min left", "Fully charged" — from BatteryManager. */
    static String statusText(Context c, int level, boolean charging) {
        BatteryManager bm = (BatteryManager) c.getSystemService(Context.BATTERY_SERVICE);
        if (charging) {
            if (level >= 100) return "Fully charged";
            if (bm != null && android.os.Build.VERSION.SDK_INT >= 28) {
                long ms = bm.computeChargeTimeRemaining();
                if (ms > 0) return duration(ms) + " to full";
            }
            return "Charging";
        }
        if (bm != null) {
            try {
                // Instantaneous estimate: remaining charge / current draw.
                long counter = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER); // µAh
                long current = Math.abs(bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)); // µA
                if (counter > 0 && current > 1000) {
                    double hours = counter / (double) current;
                    if (hours > 0.2 && hours < 200) return "~" + duration((long) (hours * 3600_000L)) + " left";
                }
            } catch (Exception ignored) {
            }
        }
        return "On battery";
    }

    private static String duration(long ms) {
        long min = Math.max(1, ms / 60_000L);
        if (min < 60) return min + " min";
        long h = min / 60, m = min % 60;
        return m == 0 ? h + " h" : h + " h " + m + " min";
    }

    /** The user's device name ("Dora's M21"), falling back to the model. */
    static String deviceName(Context c) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 25) {
                String n = android.provider.Settings.Global.getString(c.getContentResolver(),
                        android.provider.Settings.Global.DEVICE_NAME);
                if (n != null && !n.trim().isEmpty()) return n.trim();
            }
        } catch (Exception ignored) {
        }
        return android.os.Build.MODEL;
    }

    private final class Rich {
        final Canvas c;
        final RectF b;
        final Map<String, Object> p;
        final RenderContext ctx;
        final int level;
        final float frac;
        final boolean charging;
        final int body, used, text, sub, track;
        final String font;

        Rich(Canvas c, RectF b, Map<String, Object> p, RenderContext ctx, int level, float frac,
             boolean charging, int body, int used, int text) {
            this.c = c;
            this.b = b;
            this.p = p;
            this.ctx = ctx;
            this.level = level;
            this.frac = frac;
            this.charging = charging;
            this.body = body;
            this.used = used;
            this.text = text;
            this.sub = SpecColors.resolve(SpecProps.str(p, "subColor", ""), ctx.theme, (text & 0x00FFFFFF) | 0xB3000000);
            this.track = SpecColors.resolve(SpecProps.str(p, "trackColor", ""), ctx.theme, (body & 0x00FFFFFF) | 0x40000000);
            this.font = SpecProps.str(p, "font", "medium");
        }

        void draw(String style) {
            switch (style) {
                case "dots": dots(); break;
                case "pills": pills(); break;
                case "fill": fillCard(); break;
                default: glowCard(); break;
            }
        }

        private Paint tp(int color, float size, boolean bold, Paint.Align align) {
            Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
            t.setTypeface(com.desire.widget.engine.util.FontResolver.resolve(font, bold));
            t.setColor(color);
            t.setTextSize(size);
            t.setTextAlign(align);
            return t;
        }

        /** "⚡ 80%" with a drawn bolt, left- or right-aligned at (x, baseline). Returns its width. */
        private float percent(float x, float baseline, float size, int color, boolean alignRight) {
            Paint t = tp(color, size, true, Paint.Align.LEFT);
            String pct = level + "%";
            float boltW = charging ? size * 0.55f : 0f;
            float w = boltW + t.measureText(pct);
            float left = alignRight ? x - w : x;
            if (charging) drawBolt(c, left + boltW * 0.42f, baseline - size * 0.36f, size * 0.42f, color);
            c.drawText(pct, left + boltW, baseline, t);
            return w;
        }

        private String fit(String s, Paint t, float max) {
            if (t.measureText(s) <= max) return s;
            while (s.length() > 1 && t.measureText(s + "…") > max) s = s.substring(0, s.length() - 1);
            return s + "…";
        }

        private void dots() {
            int cols = 10, rows = 4, total = cols * rows;
            float headH = b.height() * 0.17f;
            float pitch = Math.min(b.width() / cols, (b.height() - headH * 1.5f) / rows);
            // Header + grid as one block, centred vertically.
            float blockH = headH * 1.5f + pitch * rows;
            float blockTop = b.centerY() - blockH / 2f;
            float base = blockTop + headH * 0.82f;
            float pw = percent(b.right, base, headH * 0.8f, used, true);
            Paint name = tp(SpecColors.resolve(SpecProps.str(p, "nameColor", ""), ctx.theme, used),
                    headH * 0.72f, false, Paint.Align.LEFT);
            String dn = SpecProps.str(p, "name", deviceName(ctx.android));
            c.drawText(fit(dn, name, b.width() - pw - headH * 0.4f), b.left, base, name);
            int lit = Math.round(frac * total);
            if (level > 0 && lit == 0) lit = 1;
            float side = pitch * 0.56f;
            float left = b.centerX() - pitch * cols / 2f, top = blockTop + headH * 1.5f;
            Paint on = fill(used), off = fill(track);
            for (int i = 0; i < total; i++) {
                float cx = left + (i % cols + 0.5f) * pitch, cy = top + (i / cols + 0.5f) * pitch;
                RectF cell = new RectF(cx - side / 2f, cy - side / 2f, cx + side / 2f, cy + side / 2f);
                c.drawRoundRect(cell, side * 0.25f, side * 0.25f, i < lit ? on : off);
            }
        }

        private void pills() {
            float size = b.height() * 0.17f;
            Paint probe = tp(text, size, true, Paint.Align.LEFT);
            float w = (charging ? size * 0.55f : 0f) + probe.measureText(level + "%");
            percent(b.centerX() - w / 2f, b.top + b.height() * 0.2f, size, text, false);
            int cells = 8;
            float area = b.width() * 0.86f;
            float gap = area * 0.03f;
            float cw = (area - gap * (cells - 1)) / cells;
            float top = b.top + b.height() * 0.32f, bottom = b.top + b.height() * 0.7f;
            float left = b.centerX() - area / 2f;
            float exact = frac * cells;
            for (int i = 0; i < cells; i++) {
                RectF cell = new RectF(left + i * (cw + gap), top, left + i * (cw + gap) + cw, bottom);
                float r = cw / 2f;
                c.drawRoundRect(cell, r, r, fill(track));
                float part = Math.max(0f, Math.min(1f, exact - i));
                if (part <= 0f) continue;
                if (part >= 1f) {
                    c.drawRoundRect(cell, r, r, fill(used));
                } else {
                    // The partly-full cell fills from the bottom.
                    int save = c.save();
                    c.clipRect(cell.left, cell.bottom - cell.height() * Math.max(part, 0.25f), cell.right, cell.bottom);
                    c.drawRoundRect(cell, r, r, fill(used));
                    c.restoreToCount(save);
                }
            }
            Paint st = tp(sub, b.height() * 0.1f, false, Paint.Align.CENTER);
            c.drawText(statusText(ctx.android, level, charging), b.centerX(), b.top + b.height() * 0.88f, st);
        }

        private void fillCard() {
            float r = Math.min(b.width(), b.height()) * 0.24f;
            c.drawRoundRect(b, r, r, fill(track));
            float fw = Math.max(r * 2f, b.width() * frac);
            int save = c.save();
            c.clipRect(b.left, b.top, b.left + fw, b.bottom);
            c.drawRoundRect(b, r, r, fill(used));
            c.restoreToCount(save);
            float x = b.left + b.width() * 0.09f;
            float big = b.height() * 0.22f;
            percent(x, b.top + b.height() * 0.38f, big, text, false);
            Paint name = tp(text, b.height() * 0.13f, true, Paint.Align.LEFT);
            float maxW = b.width() * 0.84f;
            c.drawText(fit(SpecProps.str(p, "name", deviceName(ctx.android)), name, maxW), x, b.top + b.height() * 0.62f, name);
            Paint st = tp(sub, b.height() * 0.11f, false, Paint.Align.LEFT);
            c.drawText(fit(statusText(ctx.android, level, charging), st, maxW), x, b.top + b.height() * 0.8f, st);
        }

        private void glowCard() {
            // Keep the bar well inside the frame so its halo fades out instead of being clipped.
            float x = b.left + b.width() * 0.1f;
            percent(x, b.top + b.height() * 0.26f, b.height() * 0.17f, text, false);
            Paint st = tp(sub, b.height() * 0.1f, false, Paint.Align.LEFT);
            c.drawText(statusText(ctx.android, level, charging), x, b.top + b.height() * 0.42f, st);
            float h = b.height() * 0.22f;
            RectF tr = new RectF(x, b.top + b.height() * 0.56f, b.right - b.width() * 0.1f, b.top + b.height() * 0.56f + h);
            float rr = h * 0.32f;
            c.drawRoundRect(tr, rr, rr, fill(track));
            RectF fr = new RectF(tr.left, tr.top, tr.left + Math.max(h, tr.width() * frac), tr.bottom);
            drawGlowFill(c, fr, rr, used, h * 0.42f);
            // A slim notch near the right end of the fill, like a battery terminal highlight.
            Paint notch = fill((text & 0x00FFFFFF) | 0x55000000);
            float nx = fr.right - h * 0.3f;
            c.drawRoundRect(new RectF(nx - h * 0.025f, tr.top + h * 0.3f, nx + h * 0.025f, tr.bottom - h * 0.3f), h, h, notch);
        }
    }
}
