package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.format.DateFormat;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.FontResolver;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

/**
 * Big two-line clock: hours stacked over minutes in two colours, digits sized to fill the frame
 * (the lock-screen "stacked" clock look). Follows the system 12/24-hour setting unless
 * {@code use24h} is given.
 *
 * <p>props: hourColor, minuteColor, use24h, font, align (left|center), gap (space between lines
 * as a fraction of the digit height, default 0.08).
 */
public class StackedClockRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "stacked_clock";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        int hourColor = SpecColors.resolve(SpecProps.str(p, "hourColor", "@primary"), ctx.theme, ctx.theme.primary);
        int minColor = SpecColors.resolve(SpecProps.str(p, "minuteColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        boolean use24 = p != null && p.containsKey("use24h")
                ? SpecProps.b(p, "use24h", true)
                : DateFormat.is24HourFormat(ctx.android);
        String align = SpecProps.str(p, "align", "center");
        // Space between the two lines as a fraction of the digit height (tight, like a lock screen).
        float gapFrac = Math.max(0f, Math.min(0.4f, SpecProps.f(p, "gap", 0.08f)));

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ctx.nowMillis);
        int hour = use24 ? cal.get(Calendar.HOUR_OF_DAY) : cal.get(Calendar.HOUR);
        if (!use24 && hour == 0) hour = 12;
        String hh = String.format(Locale.US, "%02d", hour);
        String mm = String.format(Locale.US, "%02d", cal.get(Calendar.MINUTE));

        Typeface tf = FontResolver.resolve(SpecProps.str(p, "font", ctx.theme.fontFamily), true);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(tf);
        paint.setTextAlign("left".equals(align) ? Paint.Align.LEFT : Paint.Align.CENTER);

        // Size from the MEASURED digit height (digits have no descenders, so their bounds are the
        // whole glyph): two lines + gap fill 96% of the frame, then shrink if "00" is too wide.
        android.graphics.Rect bounds = new android.graphics.Rect();
        paint.setTextSize(100f);
        paint.getTextBounds("0123456789", 0, 10, bounds);
        float digitPer100 = Math.max(1f, bounds.height());
        float digitH = b.height() * 0.96f / (2f + gapFrac);
        paint.setTextSize(100f * digitH / digitPer100);
        float w = Math.max(paint.measureText(hh), paint.measureText(mm));
        if (w > b.width() * 0.98f) {
            paint.setTextSize(paint.getTextSize() * b.width() * 0.98f / w);
            digitH = digitPer100 * paint.getTextSize() / 100f;
        }

        float x = "left".equals(align) ? b.left : b.centerX();
        float gap = digitH * gapFrac;
        float top = b.centerY() - (digitH * 2f + gap) / 2f;

        paint.setColor(hourColor);
        canvas.drawText(hh, x, top + digitH, paint);
        paint.setColor(minColor);
        canvas.drawText(mm, x, top + digitH * 2f + gap, paint);
    }
}
