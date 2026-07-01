package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Digital clock / formatted date-time. Renders the instant in {@code ctx.nowMillis}, so the
 * minute-tick refresh in Phase 3 advances it. props: format (SimpleDateFormat pattern), color,
 * fontSize (<=1 = fraction of height), bold, align.
 */
public class DigitalClockRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "clock";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        String fmt = SpecProps.str(p, "format", "HH:mm");
        String text;
        try {
            text = new SimpleDateFormat(fmt, Locale.getDefault()).format(new Date(ctx.nowMillis));
        } catch (Exception e) {
            text = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(ctx.nowMillis));
        }

        int color = SpecColors.resolve(SpecProps.str(p, "color", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        String align = SpecProps.str(p, "align", "center");
        float fs = SpecProps.f(p, "fontSize", 0.5f);
        float px = fs <= 1f ? fs * b.height() : fs;
        boolean bold = SpecProps.b(p, "bold", true);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setTextSize(Math.max(1f, px));
        paint.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        paint.setTextAlign("left".equals(align) ? Paint.Align.LEFT
                : "right".equals(align) ? Paint.Align.RIGHT : Paint.Align.CENTER);

        float x = "left".equals(align) ? b.left : "right".equals(align) ? b.right : b.centerX();
        Paint.FontMetrics fm = paint.getFontMetrics();
        canvas.drawText(text, x, b.centerY() - (fm.ascent + fm.descent) / 2f, paint);
    }
}
