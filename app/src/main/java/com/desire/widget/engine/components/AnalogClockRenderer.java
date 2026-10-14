package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.util.Calendar;
import java.util.Map;

/**
 * Fully drawn analog clock. Hands are derived from {@code ctx.nowMillis}.
 *
 * <p>props: faceColor, tickColor, handColor, secondColor, showSeconds, numeralColor, accentColor,
 * and {@code dialStyle}:
 * <ul>
 *   <li>classic — 12 ticks and stick hands (default)</li>
 *   <li>numerals — 60 minute ticks with 1–12 numerals</li>
 *   <li>square — rounded-square face, 12/3/6/9 numerals, dot markers</li>
 *   <li>pilot — bold 12/3/6/9 numerals, triangle index at 12, sword hands</li>
 *   <li>minimal — dot indices, slim hands, an orbiting seconds dot instead of a hand</li>
 *   <li>filled — solid accent face with bar indices and contrasting hands</li>
 * </ul>
 */
public class AnalogClockRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "analog_clock";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        String style = SpecProps.str(p, "dialStyle", "classic");
        float cx = b.centerX();
        float cy = b.centerY();
        float radius = Math.min(b.width(), b.height()) / 2f * 0.92f;

        int face = SpecColors.resolve(SpecProps.str(p, "faceColor", "@surface"), ctx.theme, ctx.theme.surface);
        int tick = SpecColors.resolve(SpecProps.str(p, "tickColor", "@textSecondary"), ctx.theme, ctx.theme.textSecondary);
        int hand = SpecColors.resolve(SpecProps.str(p, "handColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        int second = SpecColors.resolve(SpecProps.str(p, "secondColor", "@primary"), ctx.theme, ctx.theme.primary);
        int numeral = SpecColors.resolve(SpecProps.str(p, "numeralColor", ""), ctx.theme, tick);
        int accent = SpecColors.resolve(SpecProps.str(p, "accentColor", "@primary"), ctx.theme, ctx.theme.primary);
        boolean showSeconds = SpecProps.b(p, "showSeconds", true);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ctx.nowMillis);
        float hours = cal.get(Calendar.HOUR) + cal.get(Calendar.MINUTE) / 60f;
        float minutes = cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f;
        float seconds = cal.get(Calendar.SECOND);
        float hourDeg = hours / 12f * 360f;
        float minDeg = minutes / 60f * 360f;
        float secDeg = seconds / 60f * 360f;

        switch (style) {
            case "numerals":
                drawFace(canvas, cx, cy, radius, face);
                drawMinuteTicks(canvas, cx, cy, radius, tick);
                drawNumerals(canvas, cx, cy, radius * 0.70f, radius * 0.20f, numeral, false, true);
                drawHand(canvas, cx, cy, hourDeg, radius * 0.48f, radius * 0.06f, 0f, hand);
                drawHand(canvas, cx, cy, minDeg, radius * 0.74f, radius * 0.04f, 0f, hand);
                if (showSeconds) drawHand(canvas, cx, cy, secDeg, radius * 0.86f, radius * 0.014f, radius * 0.16f, second);
                drawHub(canvas, cx, cy, radius * 0.05f, second, hand);
                break;

            case "square": {
                float half = radius * 1.02f;
                RectF sq = new RectF(cx - half, cy - half, cx + half, cy + half);
                Paint fp = fill(face);
                canvas.drawRoundRect(sq, half * 0.42f, half * 0.42f, fp);
                Paint dot = fill(tick);
                for (int i = 0; i < 12; i++) {
                    if (i % 3 == 0) continue;
                    float[] pt = onSquare(cx, cy, half * 0.80f, i * 30);
                    canvas.drawCircle(pt[0], pt[1], radius * 0.035f, dot);
                }
                drawNumerals(canvas, cx, cy, radius * 0.76f, radius * 0.26f, numeral, true, false);
                drawHand(canvas, cx, cy, hourDeg, radius * 0.50f, radius * 0.07f, 0f, hand);
                drawHand(canvas, cx, cy, minDeg, radius * 0.76f, radius * 0.05f, 0f, hand);
                if (showSeconds) drawHand(canvas, cx, cy, secDeg, radius * 0.84f, radius * 0.016f, radius * 0.14f, second);
                drawHub(canvas, cx, cy, radius * 0.055f, second, hand);
                break;
            }

            case "pilot": {
                drawFace(canvas, cx, cy, radius, face);
                drawMinuteTicks(canvas, cx, cy, radius, tick);
                // Triangle index with two dots at 12 — the classic aviator marker.
                Paint ap = fill(accent);
                Path tri = new Path();
                float ty = cy - radius * 0.84f;
                tri.moveTo(cx, ty + radius * 0.16f);
                tri.lineTo(cx - radius * 0.09f, ty);
                tri.lineTo(cx + radius * 0.09f, ty);
                tri.close();
                canvas.drawPath(tri, ap);
                canvas.drawCircle(cx - radius * 0.14f, ty + radius * 0.02f, radius * 0.022f, ap);
                canvas.drawCircle(cx + radius * 0.14f, ty + radius * 0.02f, radius * 0.022f, ap);
                Paint np = text(numeral, radius * 0.30f, true);
                drawCenteredText(canvas, "3", cx + radius * 0.66f, cy, np);
                drawCenteredText(canvas, "6", cx, cy + radius * 0.64f, np);
                drawCenteredText(canvas, "9", cx - radius * 0.66f, cy, np);
                drawSword(canvas, cx, cy, hourDeg, radius * 0.50f, radius * 0.11f, hand);
                drawSword(canvas, cx, cy, minDeg, radius * 0.78f, radius * 0.08f, hand);
                if (showSeconds) drawHand(canvas, cx, cy, secDeg, radius * 0.86f, radius * 0.016f, radius * 0.18f, second);
                drawHub(canvas, cx, cy, radius * 0.06f, second, hand);
                break;
            }

            case "minimal": {
                if ((face >>> 24) != 0) drawFace(canvas, cx, cy, radius, face);
                Paint dp = fill(tick);
                for (int i = 0; i < 12; i++) {
                    double a = Math.toRadians(i * 30);
                    float r = radius * 0.84f;
                    float dr = (i % 3 == 0) ? radius * 0.05f : radius * 0.025f;
                    canvas.drawCircle(cx + (float) Math.sin(a) * r, cy - (float) Math.cos(a) * r, dr, dp);
                }
                drawHand(canvas, cx, cy, hourDeg, radius * 0.46f, radius * 0.045f, 0f, hand);
                drawHand(canvas, cx, cy, minDeg, radius * 0.70f, radius * 0.025f, 0f, hand);
                if (showSeconds) {
                    // An orbiting dot on the rim stands in for a seconds hand.
                    double a = Math.toRadians(secDeg);
                    float r = radius * 0.84f;
                    canvas.drawCircle(cx + (float) Math.sin(a) * r, cy - (float) Math.cos(a) * r, radius * 0.06f, fill(second));
                }
                canvas.drawCircle(cx, cy, radius * 0.04f, fill(hand));
                break;
            }

            case "filled": {
                drawFace(canvas, cx, cy, radius, accent);
                Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG);
                bp.setColor(face);
                bp.setStrokeCap(Paint.Cap.ROUND);
                for (int i = 0; i < 12; i++) {
                    double a = Math.toRadians(i * 30);
                    boolean major = i % 3 == 0;
                    bp.setStrokeWidth(radius * (major ? 0.06f : 0.03f));
                    float outer = radius * 0.86f;
                    float inner = radius * (major ? 0.68f : 0.76f);
                    canvas.drawLine(cx + (float) Math.sin(a) * inner, cy - (float) Math.cos(a) * inner,
                            cx + (float) Math.sin(a) * outer, cy - (float) Math.cos(a) * outer, bp);
                }
                drawHand(canvas, cx, cy, hourDeg, radius * 0.48f, radius * 0.08f, 0f, hand);
                drawHand(canvas, cx, cy, minDeg, radius * 0.72f, radius * 0.055f, 0f, hand);
                if (showSeconds) drawHand(canvas, cx, cy, secDeg, radius * 0.82f, radius * 0.018f, radius * 0.14f, second);
                drawHub(canvas, cx, cy, radius * 0.06f, second, face);
                break;
            }

            default: // classic
                drawFace(canvas, cx, cy, radius, face);
                Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                tickPaint.setColor(tick);
                tickPaint.setStrokeWidth(radius * 0.03f);
                tickPaint.setStrokeCap(Paint.Cap.ROUND);
                for (int i = 0; i < 12; i++) {
                    double a = Math.toRadians(i * 30);
                    float outer = radius * 0.96f;
                    float inner = radius * 0.84f;
                    canvas.drawLine(
                            cx + (float) Math.sin(a) * inner, cy - (float) Math.cos(a) * inner,
                            cx + (float) Math.sin(a) * outer, cy - (float) Math.cos(a) * outer, tickPaint);
                }
                drawHand(canvas, cx, cy, hourDeg, radius * 0.50f, radius * 0.045f, 0f, hand);
                drawHand(canvas, cx, cy, minDeg, radius * 0.75f, radius * 0.030f, 0f, hand);
                if (showSeconds) drawHand(canvas, cx, cy, secDeg, radius * 0.82f, radius * 0.015f, 0f, second);
                Paint hub = fill(second);
                canvas.drawCircle(cx, cy, radius * 0.05f, hub);
                break;
        }
    }

    // ------------------------------------------------------------------ helpers

    private Paint fill(int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        return p;
    }

    private Paint text(int color, float size, boolean bold) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(size);
        p.setTypeface(Typeface.create(Typeface.SANS_SERIF, bold ? Typeface.BOLD : Typeface.NORMAL));
        return p;
    }

    private void drawFace(Canvas canvas, float cx, float cy, float radius, int color) {
        canvas.drawCircle(cx, cy, radius, fill(color));
    }

    /** 60 fine minute ticks with heavier hour ticks. */
    private void drawMinuteTicks(Canvas canvas, float cx, float cy, float radius, int color) {
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(color);
        tp.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 60; i++) {
            boolean major = i % 5 == 0;
            double a = Math.toRadians(i * 6);
            tp.setStrokeWidth(radius * (major ? 0.025f : 0.010f));
            float outer = radius * 0.96f;
            float inner = radius * (major ? 0.86f : 0.91f);
            canvas.drawLine(cx + (float) Math.sin(a) * inner, cy - (float) Math.cos(a) * inner,
                    cx + (float) Math.sin(a) * outer, cy - (float) Math.cos(a) * outer, tp);
        }
    }

    /** Numerals 1–12 (or only 12/3/6/9) placed on a circle or a square. */
    private void drawNumerals(Canvas canvas, float cx, float cy, float r, float size, int color,
                              boolean onSquare, boolean all) {
        Paint np = text(color, size, true);
        for (int i = 1; i <= 12; i++) {
            if (!all && i % 3 != 0) continue;
            float[] pt = onSquare ? onSquare(cx, cy, r, i * 30) : onCircle(cx, cy, r, i * 30);
            drawCenteredText(canvas, String.valueOf(i), pt[0], pt[1], np);
        }
    }

    private float[] onCircle(float cx, float cy, float r, float deg) {
        double a = Math.toRadians(deg);
        return new float[]{cx + (float) Math.sin(a) * r, cy - (float) Math.cos(a) * r};
    }

    /** Point at a given clock angle on the perimeter of a square of half-size r. */
    private float[] onSquare(float cx, float cy, float r, float deg) {
        double a = Math.toRadians(deg);
        float dx = (float) Math.sin(a);
        float dy = -(float) Math.cos(a);
        float m = Math.max(Math.abs(dx), Math.abs(dy));
        return new float[]{cx + dx / m * r, cy + dy / m * r};
    }

    private void drawCenteredText(Canvas canvas, String t, float x, float y, Paint p) {
        Paint.FontMetrics fm = p.getFontMetrics();
        canvas.drawText(t, x, y - (fm.ascent + fm.descent) / 2f, p);
    }

    /** Stick hand with an optional counter-weight tail behind the centre. */
    private void drawHand(Canvas canvas, float cx, float cy, float deg, float length, float width,
                          float tail, int color) {
        double a = Math.toRadians(deg);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStrokeWidth(width);
        paint.setStrokeCap(Paint.Cap.ROUND);
        float sx = (float) Math.sin(a);
        float sy = -(float) Math.cos(a);
        canvas.drawLine(cx - sx * tail, cy - sy * tail, cx + sx * length, cy + sy * length, paint);
    }

    /** Tapered "sword" hand (pilot style). */
    private void drawSword(Canvas canvas, float cx, float cy, float deg, float length, float width, int color) {
        double a = Math.toRadians(deg);
        float sx = (float) Math.sin(a);
        float sy = -(float) Math.cos(a);
        float px = -sy, py = sx; // perpendicular
        Path path = new Path();
        path.moveTo(cx + px * width / 2f, cy + py * width / 2f);
        path.lineTo(cx + sx * length * 0.82f + px * width / 2f, cy + sy * length * 0.82f + py * width / 2f);
        path.lineTo(cx + sx * length, cy + sy * length);
        path.lineTo(cx + sx * length * 0.82f - px * width / 2f, cy + sy * length * 0.82f - py * width / 2f);
        path.lineTo(cx - px * width / 2f, cy - py * width / 2f);
        path.close();
        canvas.drawPath(path, fill(color));
    }

    private void drawHub(Canvas canvas, float cx, float cy, float r, int outer, int inner) {
        canvas.drawCircle(cx, cy, r, fill(outer));
        canvas.drawCircle(cx, cy, r * 0.45f, fill(inner));
    }
}
