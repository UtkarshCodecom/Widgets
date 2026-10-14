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
 *   <li>bold — black face, huge 12/3/6/9 numerals and wedge indices</li>
 *   <li>radial — light face, hour rays from the hub, even numerals turned along the radius</li>
 *   <li>sport — coloured face, big 12/9/6 numerals and a date window at 3</li>
 *   <li>stadium — fills a wide frame: a pill-shaped face with a sunburst of rays, optional
 *       12/3/6/9 numerals and a day/date label</li>
 *   <li>ios / iosSquare — 1–12 (or 12/3/6/9 on a squircle face) with a city name and offset for
 *       world clocks</li>
 *   <li>outline — big outlined 12/3/6/9 numerals and a fat rounded hour hand</li>
 * </ul>
 * World clocks: {@code timezone} (e.g. "Australia/Sydney") moves the hands, {@code city} adds the
 * label and the offset from local time.
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

        Calendar cal = com.desire.widget.engine.util.ClockText.now(ctx, p);
        float hours = cal.get(Calendar.HOUR) + cal.get(Calendar.MINUTE) / 60f;
        float minutes = cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f;
        float seconds = cal.get(Calendar.SECOND);
        float hourDeg = hours / 12f * 360f;
        float minDeg = minutes / 60f * 360f;
        float secDeg = seconds / 60f * 360f;

        switch (style) {
            case "bold":
            case "radial":
            case "sport":
            case "stadium":
            case "ios":
            case "iosSquare":
            case "outline":
                new Extra(canvas, b, p, ctx, cal, face, tick, hand, second, numeral, accent, showSeconds)
                        .draw(style, hourDeg, minDeg, secDeg);
                return;
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
    /** The expressive dial styles (kept together so the original styles stay untouched). */
    private final class Extra {
        final Canvas c;
        final RectF b;
        final Map<String, Object> p;
        final RenderContext ctx;
        final Calendar cal;
        final int face, tick, hand, second, numeral, accent;
        final boolean showSeconds;
        final float cx, cy, radius;

        Extra(Canvas c, RectF b, Map<String, Object> p, RenderContext ctx, Calendar cal, int face,
              int tick, int hand, int second, int numeral, int accent, boolean showSeconds) {
            this.c = c;
            this.b = b;
            this.p = p;
            this.ctx = ctx;
            this.cal = cal;
            this.face = face;
            this.tick = tick;
            this.hand = hand;
            this.second = second;
            this.numeral = numeral;
            this.accent = accent;
            this.showSeconds = showSeconds;
            cx = b.centerX();
            cy = b.centerY();
            radius = Math.min(b.width(), b.height()) / 2f * 0.96f;
        }

        void draw(String style, float hourDeg, float minDeg, float secDeg) {
            switch (style) {
                case "bold": bold(hourDeg, minDeg, secDeg); break;
                case "radial": radial(hourDeg, minDeg, secDeg); break;
                case "sport": sport(hourDeg, minDeg, secDeg); break;
                case "stadium": stadium(hourDeg, minDeg, secDeg); break;
                case "outline": outline(hourDeg, minDeg, secDeg); break;
                default: ios("iosSquare".equals(style), hourDeg, minDeg, secDeg); break;
            }
        }

        private Typeface font(String key, boolean bold) {
            return com.desire.widget.engine.util.FontResolver.resolve(SpecProps.str(p, "font", key), bold);
        }

        private Paint numeralPaint(float size, boolean bold, String fontKey) {
            Paint np = text(numeral, size, bold);
            np.setTypeface(font(fontKey, bold));
            return np;
        }

        private void bold(float hourDeg, float minDeg, float secDeg) {
            drawFace(c, cx, cy, radius, face);
            Paint np = numeralPaint(radius * 0.44f, false, "light");
            float nr = radius * 0.62f;
            drawCenteredText(c, "12", cx, cy - nr, np);
            drawCenteredText(c, "3", cx + nr, cy, np);
            drawCenteredText(c, "6", cx, cy + nr, np);
            drawCenteredText(c, "9", cx - nr, cy, np);
            Paint wp = fill(tick);
            for (int i = 0; i < 12; i++) {
                if (i % 3 == 0) continue;
                // Wedge pointing at the centre, wide at the rim.
                double a = Math.toRadians(i * 30);
                float sx = (float) Math.sin(a), sy = -(float) Math.cos(a);
                float px = -sy, py = sx;
                float outer = radius * 0.95f, inner = radius * 0.7f;
                float wo = radius * 0.07f, wi = radius * 0.025f;
                Path w = new Path();
                w.moveTo(cx + sx * outer + px * wo, cy + sy * outer + py * wo);
                w.lineTo(cx + sx * outer - px * wo, cy + sy * outer - py * wo);
                w.lineTo(cx + sx * inner - px * wi, cy + sy * inner - py * wi);
                w.lineTo(cx + sx * inner + px * wi, cy + sy * inner + py * wi);
                w.close();
                c.drawPath(w, wp);
            }
            outlinedHand(hourDeg, radius * 0.5f, radius * 0.075f);
            outlinedHand(minDeg, radius * 0.78f, radius * 0.055f);
            if (showSeconds) drawHand(c, cx, cy, secDeg, radius * 0.86f, radius * 0.014f, radius * 0.16f, second);
            drawHub(c, cx, cy, radius * 0.05f, hand, second);
        }

        /** Light hand with a dark rim so it reads over big numerals. */
        private void outlinedHand(float deg, float len, float w) {
            drawHand(c, cx, cy, deg, len, w + radius * 0.03f, 0f, face);
            drawHand(c, cx, cy, deg, len, w, 0f, hand);
        }

        private void radial(float hourDeg, float minDeg, float secDeg) {
            drawFace(c, cx, cy, radius, face);
            Paint ray = new Paint(Paint.ANTI_ALIAS_FLAG);
            ray.setColor(tick);
            ray.setStrokeWidth(Math.max(1f, radius * 0.008f));
            for (int i = 0; i < 12; i++) {
                if (i % 2 == 0) continue; // even hours carry numerals
                double a = Math.toRadians(i * 30);
                float sx = (float) Math.sin(a), sy = -(float) Math.cos(a);
                c.drawLine(cx + sx * radius * 0.12f, cy + sy * radius * 0.12f,
                        cx + sx * radius * 0.97f, cy + sy * radius * 0.97f, ray);
            }
            Paint fine = new Paint(ray);
            fine.setStrokeWidth(Math.max(1f, radius * 0.006f));
            for (int i = 0; i < 60; i++) {
                if (i % 5 == 0) continue;
                double a = Math.toRadians(i * 6);
                float sx = (float) Math.sin(a), sy = -(float) Math.cos(a);
                c.drawLine(cx + sx * radius * 0.9f, cy + sy * radius * 0.9f,
                        cx + sx * radius * 0.96f, cy + sy * radius * 0.96f, fine);
            }
            Paint np = numeralPaint(radius * 0.34f, false, "condensed");
            for (int h = 2; h <= 12; h += 2) {
                float deg = h * 30f;
                float[] pt = onCircle(cx, cy, radius * 0.7f, deg);
                // Turn along the radius, flipping the lower half so nothing reads upside down.
                float rot = deg % 360f;
                if (rot > 90f && rot < 270f) rot -= 180f;
                int save = c.save();
                c.rotate(rot, pt[0], pt[1]);
                drawCenteredText(c, String.valueOf(h), pt[0], pt[1], np);
                c.restoreToCount(save);
            }
            drawHand(c, cx, cy, hourDeg, radius * 0.5f, radius * 0.05f, 0f, hand);
            drawHand(c, cx, cy, minDeg, radius * 0.8f, radius * 0.035f, 0f, hand);
            if (showSeconds) drawHand(c, cx, cy, secDeg, radius * 0.86f, radius * 0.012f, radius * 0.16f, second);
            drawHub(c, cx, cy, radius * 0.04f, hand, face);
        }

        private void sport(float hourDeg, float minDeg, float secDeg) {
            drawFace(c, cx, cy, radius, face);
            Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
            tp.setColor(tick);
            tp.setStrokeCap(Paint.Cap.ROUND);
            for (int i = 0; i < 60; i++) {
                boolean major = i % 5 == 0;
                if (major && (i == 0 || i == 30 || i == 45 || i == 15)) continue;
                double a = Math.toRadians(i * 6);
                float sx = (float) Math.sin(a), sy = -(float) Math.cos(a);
                tp.setColor(major ? accent : tick);
                tp.setStrokeWidth(radius * (major ? 0.04f : 0.012f));
                float inner = radius * (major ? 0.74f : 0.88f);
                c.drawLine(cx + sx * inner, cy + sy * inner, cx + sx * radius * 0.94f, cy + sy * radius * 0.94f, tp);
            }
            Paint np = numeralPaint(radius * 0.4f, true, "default");
            np.setColor(accent);
            drawCenteredText(c, "12", cx, cy - radius * 0.62f, np);
            drawCenteredText(c, "6", cx, cy + radius * 0.62f, np);
            drawCenteredText(c, "9", cx - radius * 0.64f, cy, np);
            // Date window at 3 o'clock.
            float ww = radius * 0.3f, wh = radius * 0.2f, wx = cx + radius * 0.62f;
            RectF win = new RectF(wx - ww / 2f, cy - wh / 2f, wx + ww / 2f, cy + wh / 2f);
            Paint wp = new Paint(Paint.ANTI_ALIAS_FLAG);
            wp.setStyle(Paint.Style.STROKE);
            wp.setStrokeWidth(radius * 0.018f);
            wp.setColor(numeral);
            c.drawRoundRect(win, wh * 0.3f, wh * 0.3f, wp);
            Paint dp = numeralPaint(wh * 0.7f, true, "default");
            dp.setColor(numeral);
            drawCenteredText(c, String.valueOf(cal.get(Calendar.DAY_OF_MONTH)), win.centerX(), win.centerY(), dp);
            drawHand(c, cx, cy, hourDeg, radius * 0.5f, radius * 0.1f, 0f, accent);
            drawHand(c, cx, cy, minDeg, radius * 0.8f, radius * 0.04f, 0f, hand);
            if (showSeconds) drawHand(c, cx, cy, secDeg, radius * 0.86f, radius * 0.014f, radius * 0.18f, second);
            drawHub(c, cx, cy, radius * 0.05f, hand, face);
        }

        /** Distance from the centre to the stadium edge along a clock angle. */
        private float stadiumReach(RectF st, float sx, float sy) {
            float hw = st.width() / 2f, hh = st.height() / 2f;
            float rr = Math.min(hw, hh);
            float straight = Math.max(hw, hh) - rr; // half-length of the flat edges
            boolean wide = hw >= hh;
            // Sample along the ray (cheap and exact enough for drawing).
            float lo = 0f, hi = Math.max(hw, hh) * 1.5f;
            for (int k = 0; k < 24; k++) {
                float mid = (lo + hi) / 2f;
                float x = sx * mid, y = sy * mid;
                float ax = Math.abs(x), ay = Math.abs(y);
                boolean inside;
                if (wide) {
                    float dx = Math.max(0f, ax - straight);
                    inside = dx * dx + ay * ay <= rr * rr;
                } else {
                    float dy = Math.max(0f, ay - straight);
                    inside = ax * ax + dy * dy <= rr * rr;
                }
                if (inside) lo = mid;
                else hi = mid;
            }
            return lo;
        }

        private void stadium(float hourDeg, float minDeg, float secDeg) {
            RectF st = new RectF(b.left + b.width() * 0.02f, b.top + b.height() * 0.02f,
                    b.right - b.width() * 0.02f, b.bottom - b.height() * 0.02f);
            float rr = Math.min(st.width(), st.height()) / 2f;
            if ((face >>> 24) != 0) c.drawRoundRect(st, rr, rr, fill(face));
            boolean numerals = SpecProps.b(p, "showNumerals", true);
            Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
            tp.setStrokeCap(Paint.Cap.ROUND);
            float inset = rr * 0.08f;
            for (int i = 0; i < 60; i++) {
                boolean major = i % 5 == 0;
                boolean quarter = i % 15 == 0;
                if (numerals && quarter) continue;
                double a = Math.toRadians(i * 6);
                float sx = (float) Math.sin(a), sy = -(float) Math.cos(a);
                float reach = stadiumReach(st, sx, sy) - inset;
                float inner = reach - (major ? rr * 0.5f : rr * 0.14f);
                tp.setColor(major ? hand : tick);
                tp.setStrokeWidth(rr * (major ? 0.03f : 0.012f));
                c.drawLine(cx + sx * inner, cy + sy * inner, cx + sx * reach, cy + sy * reach, tp);
            }
            if (numerals) {
                Paint np = numeralPaint(rr * 0.36f, true, "default");
                np.setColor(accent);
                for (int h = 3; h <= 12; h += 3) {
                    double a = Math.toRadians(h * 30);
                    float sx = (float) Math.sin(a), sy = -(float) Math.cos(a);
                    float reach = stadiumReach(st, sx, sy) - inset - rr * 0.32f;
                    drawCenteredText(c, String.valueOf(h), cx + sx * reach, cy + sy * reach, np);
                }
                // Short rays either side of 3 and 9 frame the numerals.
                Paint lp = new Paint(tp);
                lp.setColor(hand);
                lp.setStrokeWidth(rr * 0.03f);
                float r3 = stadiumReach(st, 1f, 0f) - inset;
                c.drawLine(cx + r3 - rr * 0.18f, cy, cx + r3, cy, lp);
                c.drawLine(cx - r3, cy, cx - r3 + rr * 0.18f, cy, lp);
            }
            if (SpecProps.b(p, "showDate", true)) {
                String d = new java.text.SimpleDateFormat("EEE d", java.util.Locale.getDefault())
                        .format(cal.getTime()).toUpperCase(java.util.Locale.getDefault());
                Paint dp = numeralPaint(rr * 0.16f, true, "default");
                dp.setColor(accent);
                drawCenteredText(c, d, cx + st.width() * 0.22f, cy, dp);
            }
            float hr = rr * 0.92f;
            drawHand(c, cx, cy, hourDeg, hr * 0.55f, rr * 0.06f, 0f, hand);
            drawHand(c, cx, cy, minDeg, hr * 0.85f, rr * 0.04f, 0f, hand);
            if (showSeconds) drawHand(c, cx, cy, secDeg, hr * 0.95f, rr * 0.014f, rr * 0.16f, second);
            drawHub(c, cx, cy, rr * 0.05f, second, face);
        }

        private void ios(boolean square, float hourDeg, float minDeg, float secDeg) {
            float r = radius;
            if (square) {
                RectF sq = new RectF(cx - r, cy - r, cx + r, cy + r);
                c.drawPath(com.desire.widget.engine.util.Shapes.path("squircle", sq), fill(face));
                Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
                tp.setColor(tick);
                tp.setStrokeCap(Paint.Cap.ROUND);
                for (int i = 0; i < 60; i++) {
                    boolean major = i % 5 == 0;
                    tp.setStrokeWidth(r * (major ? 0.022f : 0.009f));
                    float[] o = onSquare(cx, cy, r * 0.92f, i * 6);
                    float[] in = onSquare(cx, cy, r * (major ? 0.8f : 0.86f), i * 6);
                    c.drawLine(in[0], in[1], o[0], o[1], tp);
                }
                drawNumerals(c, cx, cy, r * 0.62f, r * 0.3f, numeral, true, false);
            } else {
                drawFace(c, cx, cy, r, face);
                drawMinuteTicks(c, cx, cy, r, tick);
                drawNumerals(c, cx, cy, r * 0.7f, r * 0.2f, numeral, false, true);
            }
            String city = SpecProps.str(p, "city", "");
            if (city != null && !city.isEmpty()) {
                Paint lp = numeralPaint(r * 0.15f, true, "medium");
                lp.setColor(tick);
                drawCenteredText(c, city.toUpperCase(java.util.Locale.getDefault()), cx, cy - r * 0.3f, lp);
                String off = com.desire.widget.engine.util.ClockText.offsetLabel(
                        SpecProps.str(p, "timezone", ""), ctx.nowMillis);
                if (!off.isEmpty()) drawCenteredText(c, off, cx, cy + r * 0.32f, lp);
            }
            drawHand(c, cx, cy, hourDeg, r * 0.5f, r * 0.065f, 0f, hand);
            drawHand(c, cx, cy, minDeg, r * 0.78f, r * 0.045f, 0f, hand);
            if (showSeconds) drawHand(c, cx, cy, secDeg, r * 0.86f, r * 0.012f, r * 0.18f, second);
            drawHub(c, cx, cy, r * 0.045f, second, face);
        }

        private void outline(float hourDeg, float minDeg, float secDeg) {
            if ((face >>> 24) != 0) drawFace(c, cx, cy, radius, face);
            Paint np = numeralPaint(radius * 0.52f, true, "black");
            np.setStyle(Paint.Style.STROKE);
            np.setStrokeJoin(Paint.Join.ROUND);
            np.setStrokeWidth(radius * 0.028f);
            float nr = radius * 0.64f;
            drawCenteredText(c, "12", cx, cy - nr, np);
            drawCenteredText(c, "3", cx + nr * 1.02f, cy, np);
            drawCenteredText(c, "6", cx, cy + nr, np);
            drawCenteredText(c, "9", cx - nr * 1.02f, cy, np);
            drawHand(c, cx, cy, hourDeg, radius * 0.44f, radius * 0.13f, 0f, accent);
            drawHand(c, cx, cy, minDeg, radius * 0.76f, radius * 0.035f, 0f, hand);
            if (showSeconds) drawHand(c, cx, cy, secDeg, radius * 0.86f, radius * 0.012f, radius * 0.1f, second);
            drawHub(c, cx, cy, radius * 0.04f, hand, accent);
        }
    }
}
