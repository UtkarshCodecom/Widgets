package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.ClockText;
import com.desire.widget.engine.util.FontResolver;
import com.desire.widget.engine.util.SpecProps;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * Expressive typographic clock: one component that lays the time out and then paints the glyphs
 * with a "treatment", so a few props produce very different looks.
 *
 * <p><b>layout</b>: row (HH:MM), stacked (HH over MM), grid (four digits in a 2x2 grid),
 * split (huge hour, minutes + AM/PM chip beside it), diagonal (hour top-left, minutes
 * bottom-right, overlapping — the hand-lettered look).
 *
 * <p><b>style</b>: solid, gradient (vertical blend of {@code colors}), holo (iridescent diagonal
 * blend), outline (stroke only), bubble (fat outline + 3D extrusion), candy (every digit its own
 * colour, outlined and slightly tilted), stencil (fills cut by two slits).
 *
 * <p>Other props: font, bold, scaleX (&lt;1 = tall condensed), colors / hourColors /
 * minuteColors, hourGradientTo / minuteGradientTo (bottom colour of gradient-type treatments),
 * outlineColor, shadowColor, sep (colon|bar|dots|none), sepColor, chipColor,
 * chipTextColor, use24h, timezone, fill (digit height as a fraction of the frame).
 */
public class StyledClockRenderer implements ComponentRenderer {
    private static final float[] CANDY_TILT = {-6f, 4f, 5f, -4f};

    @Override
    public String type() {
        return "styled_clock";
    }

    /** One positioned character. x is the left edge, base the baseline. */
    private static final class Glyph {
        final String ch;
        final int color;
        int to; // gradient end colour (0 = derive a lighter tint)
        final float x, base, rot;

        Glyph(String ch, int color, float x, float base, float rot) {
            this.ch = ch;
            this.color = color;
            this.x = x;
            this.base = base;
            this.rot = rot;
        }
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        int hourTo = ClockText.color(p, "hourGradientTo", ctx.theme, 0);
        int minuteTo = ClockText.color(p, "minuteGradientTo", ctx.theme, 0);
        Calendar cal = ClockText.now(ctx, p);
        boolean use24 = ClockText.use24h(ctx, p);
        String[] t = ClockText.hhmm(cal, use24);
        String layout = SpecProps.str(p, "layout", "row");
        String style = SpecProps.str(p, "style", "solid");

        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setTypeface(FontResolver.resolve(SpecProps.str(p, "font", "black"), SpecProps.b(p, "bold", true)));
        tp.setTextScaleX(Math.max(0.3f, Math.min(1.6f, SpecProps.f(p, "scaleX", 1f))));

        int[] all = ClockText.colors(p, "colors", ctx.theme, new int[]{ctx.theme.textPrimary});
        int[] hourCols = ClockText.colors(p, "hourColors", ctx.theme, all);
        int[] minCols = ClockText.colors(p, "minuteColors", ctx.theme, all);
        int outline = ClockText.color(p, "outlineColor", ctx.theme, 0xFF15161A);
        int shadow = ClockText.color(p, "shadowColor", ctx.theme, 0xFF0B0C0F);
        int sepColor = ClockText.color(p, "sepColor", ctx.theme, minCols[0]);
        boolean candy = "candy".equals(style);
        boolean fat = candy || "bubble".equals(style);

        // Digit height per unit text size, measured (digits have no descenders).
        Rect r = new Rect();
        tp.setTextSize(100f);
        tp.getTextBounds("0123456789", 0, 10, r);
        float hPer100 = Math.max(1f, r.height());
        // Part of the digit box below the baseline (script fonts' 7 and 9 descend); baselines are
        // raised by it so the whole glyph stays inside the frame.
        final float desc = Math.max(0f, r.bottom) / hPer100;

        List<Glyph> glyphs = new ArrayList<>();
        float digitH;
        float fill = SpecProps.f(p, "fill", fat ? 0.78f : 0.9f);

        switch (layout) {
            case "stacked": {
                float gap = SpecProps.f(p, "gap", candy ? -0.06f : 0.08f);
                digitH = b.height() * fill / (2f + gap);
                float size = 100f * digitH / hPer100;
                float advance = candy ? 0.86f : 1f;
                float w = Math.max(width(tp, t[0], size, advance), width(tp, t[1], size, advance));
                if (w > b.width() * 0.96f) {
                    size *= b.width() * 0.96f / w;
                    digitH = hPer100 * size / 100f;
                }
                tp.setTextSize(size);
                float top = b.centerY() - (digitH * (2f + gap)) / 2f;
                line(glyphs, tp, t[0], hourCols, 0, b.centerX(), top + digitH * (1f - desc), advance, candy);
                line(glyphs, tp, t[1], minCols, 2, b.centerX(), top + digitH * (2f + gap - desc), advance, candy);
                break;
            }
            case "grid": {
                float cw = b.width() / 2f, ch = b.height() / 2f;
                digitH = ch * fill;
                float size = 100f * digitH / hPer100;
                tp.setTextSize(size);
                float widest = tp.measureText("0");
                for (char c = '1'; c <= '9'; c++) widest = Math.max(widest, tp.measureText(String.valueOf(c)));
                if (widest > cw * 1.02f) {
                    size *= cw * 1.02f / widest;
                    tp.setTextSize(size);
                    digitH = hPer100 * size / 100f;
                }
                String d = t[0] + t[1];
                for (int i = 0; i < 4; i++) {
                    int[] cols = i < 2 ? hourCols : minCols;
                    float ccx = b.left + cw * (i % 2) + cw / 2f;
                    float ccy = b.top + ch * (i / 2) + ch / 2f;
                    String g = d.substring(i, i + 1);
                    float gw = tp.measureText(g);
                    glyphs.add(new Glyph(g, cols[i % cols.length], ccx - gw / 2f, ccy + digitH * (0.5f - desc),
                            candy ? CANDY_TILT[i] : 0f));
                }
                break;
            }
            case "split": {
                // Hour fills the height; minutes take the top 55% of a side column with a chip below.
                String chip = use24 ? new java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
                        .format(cal.getTime()).toUpperCase(java.util.Locale.getDefault()) : ClockText.amPm(cal);
                float hH = b.height() * 0.94f, mH = b.height() * 0.46f;
                float sizeH = 100f * hH / hPer100, sizeM = 100f * mH / hPer100;
                float wHour = width(tp, t[0], sizeH, 1f);
                float wMin = width(tp, t[1], sizeM, 1f);
                float gap = b.height() * 0.05f;
                float total = wHour + gap + wMin;
                float k = Math.min(1f, b.width() * 0.96f / total);
                sizeH *= k;
                sizeM *= k;
                hH *= k;
                mH *= k;
                wHour *= k;
                wMin *= k;
                gap *= k;
                total = wHour + gap + wMin;
                float left = b.centerX() - total / 2f;
                float top = b.centerY() - hH / 2f;
                tp.setTextSize(sizeH);
                line(glyphs, tp, t[0], hourCols, 0, left + wHour / 2f, top + hH * (1f - desc), 1f, false);
                tagEnds(glyphs, hourTo, hourTo);
                drawGlyphs(canvas, glyphs, tp, style, hH, outline, shadow);
                glyphs.clear();
                float colX = left + wHour + gap;
                tp.setTextSize(sizeM);
                line(glyphs, tp, t[1], minCols, 2, colX + wMin / 2f, top + mH * (1f - desc), 1f, false);
                tagEnds(glyphs, minuteTo, minuteTo);
                drawGlyphs(canvas, glyphs, tp, style, mH, outline, shadow);
                // Chip under the minutes.
                float chipTop = top + mH + hH * 0.08f;
                RectF chipBox = new RectF(colX, chipTop, colX + wMin, top + hH);
                Paint cp = new Paint(Paint.ANTI_ALIAS_FLAG);
                cp.setColor(ClockText.color(p, "chipColor", ctx.theme, 0x33FFFFFF));
                float cr = Math.min(chipBox.width(), chipBox.height()) * 0.28f;
                canvas.drawRoundRect(chipBox, cr, cr, cp);
                Paint ct = new Paint(Paint.ANTI_ALIAS_FLAG);
                ct.setTypeface(tp.getTypeface());
                ct.setTextScaleX(tp.getTextScaleX());
                ct.setColor(ClockText.color(p, "chipTextColor", ctx.theme, minCols[0]));
                ct.setTextAlign(Paint.Align.CENTER);
                ct.setTextSize(chipBox.height() * 0.62f);
                float cw = ct.measureText(chip);
                if (cw > chipBox.width() * 0.84f) ct.setTextSize(ct.getTextSize() * chipBox.width() * 0.84f / cw);
                Paint.FontMetrics fm = ct.getFontMetrics();
                canvas.drawText(chip, chipBox.centerX(), chipBox.centerY() - (fm.ascent + fm.descent) / 2f, ct);
                return;
            }
            case "diagonal": {
                digitH = b.height() * SpecProps.f(p, "fill", 0.6f);
                float size = 100f * digitH / hPer100;
                float wh = width(tp, t[0], size, 1f), wm = width(tp, t[1], size, 1f);
                float need = wh * 0.66f + wm;
                if (need > b.width() * 0.94f) {
                    size *= b.width() * 0.94f / need;
                    digitH = hPer100 * size / 100f;
                    wh = width(tp, t[0], size, 1f);
                    wm = width(tp, t[1], size, 1f);
                }
                tp.setTextSize(size);
                float span = wh * 0.66f + wm;
                float left = b.centerX() - span / 2f;
                float hourBase = b.top + (b.height() - digitH * 1.55f) / 2f + digitH * (1f - desc);
                float minBase = hourBase + digitH * 0.55f;
                line(glyphs, tp, t[0], hourCols, 0, left + wh / 2f, hourBase, 1f, false);
                line(glyphs, tp, t[1], minCols, 2, left + wh * 0.66f + wm / 2f, minBase, 1f, false);
                tagEnds(glyphs, hourTo, minuteTo);
                drawGlyphs(canvas, glyphs, tp, style, digitH, outline, shadow);
                // Two little dots between the hour and the minutes.
                Paint dp = new Paint(Paint.ANTI_ALIAS_FLAG);
                dp.setColor(sepColor);
                // Sit the dots just right of the hour, above where the minutes start.
                float dx = left + wh + digitH * 0.08f, dr = digitH * 0.05f;
                canvas.drawCircle(dx, hourBase - digitH * 0.7f, dr, dp);
                canvas.drawCircle(dx + dr * 0.8f, hourBase - digitH * 0.38f, dr, dp);
                return;
            }
            default: { // row
                String sep = SpecProps.str(p, "sep", "colon");
                digitH = b.height() * fill;
                float size = 100f * digitH / hPer100;
                float advance = candy ? 0.88f : 1f;
                float sepW = sepWidth(tp, sep, size, hPer100);
                float total = width(tp, t[0], size, advance) + sepW + width(tp, t[1], size, advance);
                if (total > b.width() * 0.96f) {
                    float k = b.width() * 0.96f / total;
                    size *= k;
                    digitH *= k;
                    sepW *= k;
                    total *= k;
                }
                tp.setTextSize(size);
                float base = b.centerY() + digitH * (0.5f - desc);
                float wh = width(tp, t[0], size, advance);
                float left = b.centerX() - total / 2f;
                line(glyphs, tp, t[0], hourCols, 0, left + wh / 2f, base, advance, candy);
                float mx = left + wh + sepW;
                line(glyphs, tp, t[1], minCols, 2, mx + width(tp, t[1], size, advance) / 2f, base, advance, candy);
                drawSeparator(canvas, sep, tp, left + wh, sepW, base, digitH, sepColor);
                break;
            }
        }
        tagEnds(glyphs, hourTo, minuteTo);
        drawGlyphs(canvas, glyphs, tp, style, digitH, outline, shadow);
    }

    /** Glyphs are added hours-first (two hour digits, then minutes); give each its gradient end. */
    private void tagEnds(List<Glyph> gs, int hourTo, int minuteTo) {
        int digit = 0;
        for (Glyph g : gs) {
            if (!Character.isDigit(g.ch.charAt(0))) continue;
            g.to = digit < 2 ? hourTo : minuteTo;
            digit++;
        }
    }

    // ------------------------------------------------------------------ layout helpers

    private float width(Paint tp, String text, float size, float advance) {
        float old = tp.getTextSize();
        tp.setTextSize(size);
        float w = 0f;
        for (int i = 0; i < text.length(); i++) {
            float cw = tp.measureText(text, i, i + 1);
            w += i == text.length() - 1 ? cw : cw * advance;
        }
        tp.setTextSize(old);
        return w;
    }

    /** Adds the characters of {@code text} centred on {@code cx}; colours cycle from {@code colorIndex}. */
    private void line(List<Glyph> out, Paint tp, String text, int[] cols, int colorIndex, float cx,
                      float base, float advance, boolean tilt) {
        float w = width(tp, text, tp.getTextSize(), advance);
        float x = cx - w / 2f;
        for (int i = 0; i < text.length(); i++) {
            String g = text.substring(i, i + 1);
            int idx = colorIndex + i;
            out.add(new Glyph(g, cols[(cols.length == 1 ? 0 : idx % cols.length)], x, base,
                    tilt ? CANDY_TILT[idx % CANDY_TILT.length] : 0f));
            x += tp.measureText(g) * advance;
        }
    }

    private float sepWidth(Paint tp, String sep, float size, float hPer100) {
        float digitH = hPer100 * size / 100f;
        switch (sep) {
            case "none": return digitH * 0.12f;
            case "bar": return digitH * 0.34f;
            case "dots": return digitH * 0.34f;
            default: {
                float old = tp.getTextSize();
                tp.setTextSize(size);
                float w = tp.measureText(":");
                tp.setTextSize(old);
                return w;
            }
        }
    }

    private void drawSeparator(Canvas c, String sep, Paint tp, float x, float w, float base, float digitH, int color) {
        Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG);
        sp.setColor(color);
        float cx = x + w / 2f;
        switch (sep) {
            case "none":
                return;
            case "bar": {
                float bw = digitH * 0.05f;
                c.drawRoundRect(new RectF(cx - bw / 2f, base - digitH * 0.66f, cx + bw / 2f, base - digitH * 0.34f), bw, bw, sp);
                return;
            }
            case "dots": {
                float r = digitH * 0.065f;
                c.drawCircle(cx, base - digitH * 0.7f, r, sp);
                c.drawCircle(cx, base - digitH * 0.3f, r, sp);
                return;
            }
            default: {
                Paint t = new Paint(tp);
                t.setShader(null);
                t.setStyle(Paint.Style.FILL);
                t.setColor(color);
                t.setTextAlign(Paint.Align.CENTER);
                c.drawText(":", cx, base - digitH * 0.06f, t);
            }
        }
    }

    // ------------------------------------------------------------------ treatments

    private void drawGlyphs(Canvas c, List<Glyph> gs, Paint tp, String style, float digitH, int outline, int shadow) {
        if (gs.isEmpty()) return;
        Paint fp = new Paint(tp);
        fp.setTextAlign(Paint.Align.LEFT);
        fp.setStyle(Paint.Style.FILL);
        switch (style) {
            case "outline": {
                fp.setStyle(Paint.Style.STROKE);
                fp.setStrokeWidth(Math.max(1.5f, digitH * 0.032f));
                fp.setStrokeJoin(Paint.Join.ROUND);
                for (Glyph g : gs) {
                    fp.setShader(vertical(g, digitH, g.color, end(g, 0.35f)));
                    c.drawText(g.ch, g.x, g.base, fp);
                }
                return;
            }
            case "bubble": {
                float depth = digitH * 0.07f;
                Paint sh = new Paint(fp);
                sh.setColor(shadow);
                sh.setStyle(Paint.Style.FILL_AND_STROKE);
                sh.setStrokeJoin(Paint.Join.ROUND);
                sh.setStrokeWidth(digitH * 0.08f);
                for (int k = 6; k >= 1; k--) {
                    float o = depth * k / 6f;
                    for (Glyph g : gs) c.drawText(g.ch, g.x + o, g.base + o, sh);
                }
                Paint ol = new Paint(sh);
                ol.setColor(outline);
                for (Glyph g : gs) c.drawText(g.ch, g.x, g.base, ol);
                for (Glyph g : gs) {
                    fp.setShader(vertical(g, digitH, lighten(g.color, 0.25f), g.color));
                    c.drawText(g.ch, g.x, g.base, fp);
                }
                return;
            }
            case "candy": {
                Paint ol = new Paint(fp);
                ol.setStyle(Paint.Style.FILL_AND_STROKE);
                ol.setStrokeJoin(Paint.Join.ROUND);
                ol.setStrokeWidth(digitH * 0.075f);
                ol.setColor(outline);
                for (Glyph g : gs) {
                    float gw = fp.measureText(g.ch);
                    int save = c.save();
                    c.rotate(g.rot, g.x + gw / 2f, g.base - digitH / 2f);
                    c.drawText(g.ch, g.x, g.base, ol);
                    fp.setShader(null);
                    fp.setColor(g.color);
                    c.drawText(g.ch, g.x, g.base, fp);
                    c.restoreToCount(save);
                }
                return;
            }
            case "stencil": {
                RectF box = box(gs, fp, digitH);
                int layer = c.saveLayer(box, null);
                for (Glyph g : gs) {
                    fp.setShader(vertical(g, digitH, g.color, end(g, 0.3f)));
                    c.drawText(g.ch, g.x, g.base, fp);
                }
                Paint clear = new Paint();
                clear.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                float th = digitH * 0.045f;
                for (Glyph g : gs) {
                    float gw = fp.measureText(g.ch);
                    for (float f : new float[]{0.34f, 0.68f}) {
                        float y = g.base - digitH * f;
                        c.drawRect(g.x - gw * 0.1f, y - th / 2f, g.x + gw * 1.1f, y + th / 2f, clear);
                    }
                }
                c.restoreToCount(layer);
                return;
            }
            case "holo": {
                RectF box = box(gs, fp, digitH);
                int[] holo = {0xFFE8F6FF, 0xFFF6D9FF, 0xFFFFFFFF, 0xFFD5E2FF, 0xFFFFE6F4};
                fp.setShader(new LinearGradient(box.left, box.top, box.right, box.bottom, holo, null, Shader.TileMode.CLAMP));
                for (Glyph g : gs) c.drawText(g.ch, g.x, g.base, fp);
                return;
            }
            case "gradient": {
                for (Glyph g : gs) {
                    fp.setShader(vertical(g, digitH, g.color, end(g, 0.45f)));
                    c.drawText(g.ch, g.x, g.base, fp);
                }
                return;
            }
            default:
                for (Glyph g : gs) {
                    fp.setShader(null);
                    fp.setColor(g.color);
                    c.drawText(g.ch, g.x, g.base, fp);
                }
        }
    }

    /** Bottom colour for gradient treatments: explicit per hour/minute, else a lighter tint. */
    private int end(Glyph g, float tint) {
        return g.to != 0 ? g.to : lighten(g.color, tint);
    }

    private Shader vertical(Glyph g, float digitH, int top, int bottom) {
        return new LinearGradient(0, g.base - digitH, 0, g.base, top, bottom, Shader.TileMode.CLAMP);
    }

    private RectF box(List<Glyph> gs, Paint fp, float digitH) {
        RectF box = null;
        for (Glyph g : gs) {
            RectF r = new RectF(g.x, g.base - digitH * 1.1f, g.x + fp.measureText(g.ch), g.base + digitH * 0.1f);
            if (box == null) box = r;
            else box.union(r);
        }
        return box;
    }

    private static int lighten(int c, float f) {
        int a = c >>> 24, r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
        r += (int) ((255 - r) * f);
        g += (int) ((255 - g) * f);
        b += (int) ((255 - b) * f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
