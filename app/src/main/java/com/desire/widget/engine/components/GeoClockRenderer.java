package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.ClockText;
import com.desire.widget.engine.util.SpecProps;

import java.util.Calendar;
import java.util.Map;

/**
 * "Constructed" clock: every digit is built from straight strokes and arcs on a 1 x 1.5 grid
 * (no font), with a small accent ring placed on each digit, laid out as HH over MM.
 *
 * <p>props: color (strokes), accentColor (rings), stroke (width as a fraction of a digit's
 * width), layout (stacked|row), use24h, timezone.
 */
public class GeoClockRenderer implements ComponentRenderer {
    /** Accent ring centre per digit, in digit units (x 0..1, y 0..1.5). */
    private static final float[][] RING = {
            {0.78f, 0.42f}, {0.22f, 0.32f}, {0.22f, 0.36f}, {0.78f, 1.08f}, {0.26f, 0.9f},
            {0.24f, 0.18f}, {0.72f, 0.42f}, {0.8f, 0.2f}, {0.24f, 1.08f}, {0.26f, 0.44f}};

    @Override
    public String type() {
        return "geo_clock";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        Calendar cal = ClockText.now(ctx, p);
        String[] t = ClockText.hhmm(cal, ClockText.use24h(ctx, p));
        int color = ClockText.color(p, "color", ctx.theme, ctx.theme.textPrimary);
        int accent = ClockText.color(p, "accentColor", ctx.theme, ctx.theme.primary);
        boolean row = "row".equals(SpecProps.str(p, "layout", "stacked"));

        // Digit box w x 1.5w; gap 0.18w between digits and between lines.
        float gap = 0.18f;
        float unitsW = row ? 4f + 3f * gap + 0.3f : 2f + gap;
        float unitsH = row ? 1.5f : 3f + gap;
        float w = Math.min(b.width() * 0.94f / unitsW, b.height() * 0.94f / unitsH);
        float left = b.centerX() - unitsW * w / 2f, top = b.centerY() - unitsH * w / 2f;

        Paint st = new Paint(Paint.ANTI_ALIAS_FLAG);
        st.setStyle(Paint.Style.STROKE);
        st.setStrokeCap(Paint.Cap.ROUND);
        st.setStrokeJoin(Paint.Join.ROUND);
        st.setStrokeWidth(w * SpecProps.f(p, "stroke", 0.075f));
        st.setColor(color);
        Paint ring = new Paint(st);
        ring.setColor(accent);

        String d = t[0] + t[1];
        for (int i = 0; i < 4; i++) {
            float x, y;
            if (row) {
                x = left + (i * (1f + gap) + (i >= 2 ? 0.3f : 0f)) * w;
                y = top;
            } else {
                x = left + (i % 2) * (1f + gap) * w;
                y = top + (i / 2) * (1.5f + gap) * w;
            }
            int digit = d.charAt(i) - '0';
            canvas.drawPath(digitPath(digit, x, y, w), st);
            float[] rc = RING[digit];
            canvas.drawCircle(x + rc[0] * w, y + rc[1] * w, w * 0.2f, ring);
        }
        if (row) {
            Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
            dot.setColor(accent);
            float cx = left + (2f + 2f * gap - gap / 2f + 0.15f) * w;
            canvas.drawCircle(cx, top + 0.5f * w, w * 0.07f, dot);
            canvas.drawCircle(cx, top + 1.0f * w, w * 0.07f, dot);
        }
    }

    /** Strokes for one digit in a box at (x, y) of size w x 1.5w. */
    private Path digitPath(int n, float x, float y, float w) {
        Path p = new Path();
        float r = w / 2f;            // half width
        float mid = y + 0.75f * w;   // vertical middle
        float bot = y + 1.5f * w;
        switch (n) {
            case 0:
                p.addRoundRect(new RectF(x, y, x + w, bot), r, r, Path.Direction.CW);
                p.moveTo(x + r, y + 0.2f * w);
                p.lineTo(x + r, bot - 0.2f * w);
                break;
            case 1:
                p.moveTo(x + 0.25f * w, y + 0.3f * w);
                p.lineTo(x + 0.6f * w, y);
                p.lineTo(x + 0.6f * w, bot);
                p.moveTo(x + 0.2f * w, bot);
                p.lineTo(x + w, bot);
                break;
            case 2:
                p.addArc(new RectF(x, y, x + w, y + w), 180f, 200f);
                p.moveTo(x + w * 0.96f, y + 0.62f * w);
                p.lineTo(x, bot);
                p.lineTo(x + w, bot);
                break;
            case 3:
                p.addArc(new RectF(x, y, x + w, y + 0.75f * w), 200f, 250f);
                p.addArc(new RectF(x, mid, x + w, bot), 270f, 250f);
                break;
            case 4:
                p.moveTo(x + 0.72f * w, bot);
                p.lineTo(x + 0.72f * w, y);
                p.lineTo(x, y + 1.05f * w);
                p.lineTo(x + w, y + 1.05f * w);
                break;
            case 5:
                p.moveTo(x + w, y);
                p.lineTo(x + 0.05f * w, y);
                p.lineTo(x + 0.05f * w, y + 0.62f * w);
                p.addArc(new RectF(x, y + 0.5f * w, x + w, bot), 230f, 260f);
                break;
            case 6:
                p.addOval(new RectF(x, y + 0.5f * w, x + w, bot), Path.Direction.CW);
                p.moveTo(x + 0.85f * w, y + 0.05f * w);
                p.quadTo(x, y + 0.2f * w, x, y + w);
                break;
            case 7:
                p.moveTo(x, y);
                p.lineTo(x + w, y);
                p.lineTo(x + 0.3f * w, bot);
                p.moveTo(x + 0.25f * w, mid);
                p.lineTo(x + 0.85f * w, mid);
                break;
            case 8:
                p.addOval(new RectF(x + 0.06f * w, y, x + 0.94f * w, y + 0.7f * w), Path.Direction.CW);
                p.addOval(new RectF(x, y + 0.7f * w, x + w, bot), Path.Direction.CW);
                break;
            default: // 9
                p.addOval(new RectF(x, y, x + w, y + w), Path.Direction.CW);
                p.moveTo(x + w, y + r);
                p.quadTo(x + w, bot, x + 0.15f * w, bot - 0.05f * w);
                break;
        }
        return p;
    }
}
