package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.util.Map;

/**
 * Draws the current moon phase from pure date arithmetic (no network, no location needed): a
 * synodic-month calculation against a known new-moon reference epoch, then a lit/dark disc drawn
 * with a moving-terminator-ellipse technique. props: litColor, darkColor, textColor, showLabel.
 */
public class MoonPhaseRenderer implements ComponentRenderer {
    // A known new moon reference: 2000-01-06 18:14 UTC.
    private static final long REFERENCE_NEW_MOON_MILLIS = 947182440000L;
    private static final double SYNODIC_MONTH_DAYS = 29.53058867;
    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

    private static final String[] PHASE_NAMES = {
            "New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous",
            "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent"
    };

    @Override
    public String type() {
        return "moon_phase";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        int lit = SpecColors.resolve(SpecProps.str(p, "litColor", "#F0F0E6"), ctx.theme, 0xFFF0F0E6);
        int dark = SpecColors.resolve(SpecProps.str(p, "darkColor", "#14171C"), ctx.theme, 0xFF14171C);
        int textColor = SpecColors.resolve(SpecProps.str(p, "textColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        boolean showLabel = SpecProps.b(p, "showLabel", true);

        float k = phaseFraction(ctx.nowMillis); // 0..1: 0/1=new, 0.5=full
        String label = phaseName(k);

        float labelH = showLabel ? b.height() * 0.22f : 0f;
        float diskD = Math.min(b.width(), b.height() - labelH) * 0.86f;
        float cx = b.centerX();
        float cy = b.top + (b.height() - labelH) / 2f;
        float r = diskD / 2f;

        drawMoon(canvas, cx, cy, r, k, lit, dark);

        if (showLabel) {
            Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
            text.setColor(textColor);
            text.setTextAlign(Paint.Align.CENTER);
            text.setFakeBoldText(true);
            text.setTextSize(labelH * 0.42f);
            Paint.FontMetrics fm = text.getFontMetrics();
            float ty = b.bottom - labelH * 0.5f - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(label, cx, ty, text);
        }
    }

    /** 0..1 across the synodic month; 0 and 1 are new moon, 0.5 is full moon. */
    private float phaseFraction(long nowMillis) {
        double daysSinceRef = (nowMillis - REFERENCE_NEW_MOON_MILLIS) / (double) DAY_MILLIS;
        double age = daysSinceRef % SYNODIC_MONTH_DAYS;
        if (age < 0) age += SYNODIC_MONTH_DAYS;
        return (float) (age / SYNODIC_MONTH_DAYS);
    }

    private String phaseName(float k) {
        int index = Math.round(k * 8) % 8;
        return PHASE_NAMES[index];
    }

    /**
     * Draws the lit disc using a "moving terminator ellipse" technique: the boundary between light
     * and dark is the right half of the moon's circle combined with an ellipse whose horizontal
     * radius shrinks from +r (new moon, no light) through 0 (quarter, half lit) to -r (full moon,
     * fully lit); waning phases mirror the same shape horizontally.
     */
    private void drawMoon(Canvas canvas, float cx, float cy, float r, float k, int litColor, int darkColor) {
        Paint darkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        darkPaint.setColor(darkColor);
        canvas.drawCircle(cx, cy, r, darkPaint);

        boolean waxing = k <= 0.5f;
        float kk = waxing ? k : (1f - k); // mirror index, always in [0, 0.5]
        float angle = kk * (float) (2 * Math.PI);
        float rx = r * (float) Math.cos(angle); // r -> 0 -> -r as kk goes 0 -> 0.5

        Path lit = new Path();
        RectF circleRect = new RectF(cx - r, cy - r, cx + r, cy + r);
        lit.moveTo(cx, cy - r);
        lit.arcTo(circleRect, -90, 180); // right half of the circle, top -> right -> bottom

        RectF ellipseRect = new RectF(cx - Math.abs(rx), cy - r, cx + Math.abs(rx), cy + r);
        if (rx >= 0) {
            lit.arcTo(ellipseRect, 90, 180);  // crescent: close via the ellipse's LEFT arc
        } else {
            lit.arcTo(ellipseRect, 90, -180); // gibbous: close via the ellipse's RIGHT arc
        }
        lit.close();

        Paint litPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        litPaint.setColor(litColor);

        int save = canvas.save();
        if (!waxing) canvas.scale(-1, 1, cx, cy); // waning phases mirror so the shadow grows from the other side
        canvas.drawPath(lit, litPaint);
        canvas.restoreToCount(save);
    }
}
