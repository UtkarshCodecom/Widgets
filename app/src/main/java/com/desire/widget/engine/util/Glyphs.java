package com.desire.widget.engine.util;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * Small shared vector glyphs drawn by multiple renderers, kept in one place so the same icon looks
 * identical everywhere (and isn't reimplemented per component).
 */
public final class Glyphs {
    private Glyphs() {}

    /**
     * A circular "reset"/refresh arrow centred at (cx, cy) with radius r: a rounded ~290° arc with a
     * filled arrowhead at its leading (top) end, pointing clockwise. Used by the Stopwatch and
     * Pomodoro widgets for the reset button (tap region {@code widget_ctrl_next} in
     * widget_layout.xml). Draws a soft translucent disc behind it so the control reads as a button.
     */
    public static void drawReset(Canvas canvas, float cx, float cy, float r, int color) {
        // Soft button backing.
        Paint disc = new Paint(Paint.ANTI_ALIAS_FLAG);
        disc.setColor(color);
        disc.setAlpha(28);
        canvas.drawCircle(cx, cy, r * 1.55f, disc);

        float stroke = Math.max(2.2f, r * 0.26f);
        Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(stroke);
        arc.setStrokeCap(Paint.Cap.ROUND);
        arc.setColor(color);

        // Arc starts at the top and sweeps clockwise, leaving a gap on the upper-left.
        float startAngle = -70f;
        float sweep = 290f;
        RectF oval = new RectF(cx - r, cy - r, cx + r, cy + r);
        canvas.drawArc(oval, startAngle, sweep, false, arc);

        // Arrowhead at the leading (start) end, pointing along the clockwise tangent.
        double a = Math.toRadians(startAngle);
        float tipX = cx + (float) Math.cos(a) * r;
        float tipY = cy + (float) Math.sin(a) * r;
        float tanX = -(float) Math.sin(a); // clockwise tangent direction
        float tanY = (float) Math.cos(a);
        float radX = (float) Math.cos(a);  // outward radial direction
        float radY = (float) Math.sin(a);
        float hl = r * 0.85f; // head length along the tangent
        float hw = r * 0.6f;  // head half-width along the radius

        Path head = new Path();
        head.moveTo(tipX + tanX * hl, tipY + tanY * hl);          // point
        head.lineTo(tipX + radX * hw, tipY + radY * hw);          // outer base
        head.lineTo(tipX - radX * hw, tipY - radY * hw);          // inner base
        head.close();

        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(color);
        canvas.drawPath(head, fill);
    }
}
