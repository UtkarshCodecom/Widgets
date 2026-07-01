package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.util.Calendar;
import java.util.Map;

/**
 * Fully drawn analog clock: face, 12 ticks, hour/minute/second hands and a hub. Hands are derived
 * from {@code ctx.nowMillis}. props: faceColor, tickColor, handColor, secondColor, showSeconds.
 */
public class AnalogClockRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "analog_clock";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        float cx = b.centerX();
        float cy = b.centerY();
        float radius = Math.min(b.width(), b.height()) / 2f * 0.92f;

        int face = SpecColors.resolve(SpecProps.str(p, "faceColor", "@surface"), ctx.theme, ctx.theme.surface);
        int tick = SpecColors.resolve(SpecProps.str(p, "tickColor", "@textSecondary"), ctx.theme, ctx.theme.textSecondary);
        int hand = SpecColors.resolve(SpecProps.str(p, "handColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        int second = SpecColors.resolve(SpecProps.str(p, "secondColor", "@primary"), ctx.theme, ctx.theme.primary);

        Paint facePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        facePaint.setStyle(Paint.Style.FILL);
        facePaint.setColor(face);
        canvas.drawCircle(cx, cy, radius, facePaint);

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

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ctx.nowMillis);
        float hours = cal.get(Calendar.HOUR) + cal.get(Calendar.MINUTE) / 60f;
        float minutes = cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f;
        float seconds = cal.get(Calendar.SECOND);

        drawHand(canvas, cx, cy, hours / 12f * 360f, radius * 0.50f, radius * 0.045f, hand);
        drawHand(canvas, cx, cy, minutes / 60f * 360f, radius * 0.75f, radius * 0.030f, hand);
        if (SpecProps.b(p, "showSeconds", true)) {
            drawHand(canvas, cx, cy, seconds / 60f * 360f, radius * 0.82f, radius * 0.015f, second);
        }

        Paint hub = new Paint(Paint.ANTI_ALIAS_FLAG);
        hub.setColor(second);
        canvas.drawCircle(cx, cy, radius * 0.05f, hub);
    }

    private void drawHand(Canvas canvas, float cx, float cy, float deg, float length, float width, int color) {
        double a = Math.toRadians(deg);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStrokeWidth(width);
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(cx, cy, cx + (float) Math.sin(a) * length, cy - (float) Math.cos(a) * length, paint);
    }
}
