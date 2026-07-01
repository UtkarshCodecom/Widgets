package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.util.Map;

/**
 * Circular progress ring with a track, a rounded progress arc, and centered label.
 * props: progress (0..1), trackColor, progressColor, thickness (fraction of min side),
 * startAngle (deg, default -90 = top), centerText (defaults to N%), textColor, textSize.
 */
public class ProgressRingRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "progress_ring";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        float progress = clamp01(SpecProps.f(p, "progress", 0.6f));
        int track = SpecColors.resolve(SpecProps.str(p, "trackColor", "#33FFFFFF"), ctx.theme, 0x33FFFFFF);
        int prog = SpecColors.resolve(SpecProps.str(p, "progressColor", "@primary"), ctx.theme, ctx.theme.primary);

        float size = Math.min(b.width(), b.height());
        float thickness = SpecProps.f(p, "thickness", 0.12f) * size;
        float r = (size - thickness) / 2f;
        RectF oval = new RectF(b.centerX() - r, b.centerY() - r, b.centerX() + r, b.centerY() + r);
        float start = SpecProps.f(p, "startAngle", -90f);

        Paint trackPaint = arcPaint(track, thickness);
        canvas.drawArc(oval, 0, 360, false, trackPaint);

        Paint progPaint = arcPaint(prog, thickness);
        canvas.drawArc(oval, start, 360f * progress, false, progPaint);

        String center = SpecProps.str(p, "centerText", Math.round(progress * 100) + "%");
        if (center != null && !center.isEmpty()) {
            int tc = SpecColors.resolve(SpecProps.str(p, "textColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
            Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
            txt.setColor(tc);
            txt.setTextAlign(Paint.Align.CENTER);
            txt.setFakeBoldText(true);
            txt.setTextSize(size * SpecProps.f(p, "textSize", 0.22f));
            Paint.FontMetrics fm = txt.getFontMetrics();
            canvas.drawText(center, b.centerX(), b.centerY() - (fm.ascent + fm.descent) / 2f, txt);
        }
    }

    private Paint arcPaint(int color, float thickness) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(thickness);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(color);
        return paint;
    }

    private float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
