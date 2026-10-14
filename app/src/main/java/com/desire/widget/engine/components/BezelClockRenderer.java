package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.ClockText;
import com.desire.widget.engine.util.FontResolver;
import com.desire.widget.engine.util.SpecProps;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

/**
 * Digital time inside a round watch face with a 60-step bezel (dots or ticks). Bezel marks up to
 * the current second light up in the accent colour, so the face "sweeps" like a seconds hand.
 * An optional world-clock line ("TYO +3:30") sits above the time and the date below it.
 *
 * <p>props: faceColor, bezel (dots|ticks), bezelColor, accentColor, sweep (light the bezel up to
 * the current second), textColor, subColor, city, timezone, dateFormat, font, use24h.
 */
public class BezelClockRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "bezel_clock";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        Calendar cal = ClockText.now(ctx, p);
        boolean use24 = ClockText.use24h(ctx, p);
        String[] t = ClockText.hhmm(cal, use24);

        float cx = b.centerX(), cy = b.centerY();
        float r = Math.min(b.width(), b.height()) / 2f * 0.98f;
        int face = ClockText.color(p, "faceColor", ctx.theme, 0xFF0B0B0D);
        int bezelColor = ClockText.color(p, "bezelColor", ctx.theme, 0x66FFFFFF);
        int accent = ClockText.color(p, "accentColor", ctx.theme, ctx.theme.primary);
        int text = ClockText.color(p, "textColor", ctx.theme, ctx.theme.textPrimary);
        int sub = ClockText.color(p, "subColor", ctx.theme, text);
        boolean ticks = "ticks".equals(SpecProps.str(p, "bezel", "dots"));
        boolean sweep = SpecProps.b(p, "sweep", true);
        int second = cal.get(Calendar.SECOND);

        Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG);
        fp.setColor(face);
        canvas.drawCircle(cx, cy, r, fp);

        Paint mark = new Paint(Paint.ANTI_ALIAS_FLAG);
        mark.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 60; i++) {
            boolean major = i % 5 == 0;
            boolean lit = sweep && i <= second;
            mark.setColor(lit ? accent : bezelColor);
            double a = Math.toRadians(i * 6);
            float sx = (float) Math.sin(a), sy = -(float) Math.cos(a);
            if (ticks) {
                mark.setStrokeWidth(r * (major ? 0.028f : 0.014f));
                float outer = r * 0.93f, inner = r * (major ? 0.8f : 0.86f);
                canvas.drawLine(cx + sx * inner, cy + sy * inner, cx + sx * outer, cy + sy * outer, mark);
            } else {
                float rr = r * 0.88f;
                canvas.drawCircle(cx + sx * rr, cy + sy * rr, r * (major ? 0.034f : 0.022f), mark);
            }
        }

        // Time: condensed bold, sized to ~44% of the face height and ~62% of its width.
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setTypeface(FontResolver.resolve(SpecProps.str(p, "font", "condensed"), true));
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setColor(text);
        String time = t[0] + ":" + t[1];
        tp.setTextSize(r * 0.62f);
        float w = tp.measureText(time);
        if (w > r * 1.2f) tp.setTextSize(tp.getTextSize() * r * 1.2f / w);
        Paint.FontMetrics fm = tp.getFontMetrics();
        canvas.drawText(time, cx, cy - (fm.ascent + fm.descent) / 2f, tp);

        Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG);
        sp.setTypeface(FontResolver.resolve(SpecProps.str(p, "subFont", "medium"), true));
        sp.setTextAlign(Paint.Align.CENTER);
        sp.setColor(sub);
        sp.setTextSize(r * 0.13f);
        Paint.FontMetrics sfm = sp.getFontMetrics();

        String city = SpecProps.str(p, "city", "");
        if (city != null && !city.isEmpty()) {
            String off = ClockText.offsetLabel(SpecProps.str(p, "timezone", ""), ctx.nowMillis);
            String label = city.toUpperCase(Locale.getDefault()) + (off.isEmpty() ? "" : " " + off);
            canvas.drawText(label, cx, cy - r * 0.42f - (sfm.ascent + sfm.descent) / 2f, sp);
        }
        String dateFmt = SpecProps.str(p, "dateFormat", "EEE, dd MMM");
        if (dateFmt != null && !dateFmt.isEmpty()) {
            SimpleDateFormat f = new SimpleDateFormat(dateFmt, Locale.getDefault());
            f.setTimeZone(cal.getTimeZone());
            String date = f.format(cal.getTime()).toUpperCase(Locale.getDefault());
            canvas.drawText(date, cx, cy + r * 0.42f - (sfm.ascent + sfm.descent) / 2f, sp);
        }
    }
}
