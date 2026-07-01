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
 * Filled/stroked primitive. props: shape (rect|rounded|pill|circle), fill, stroke, strokeWidth
 * (fraction of min side), cornerRadius (fraction of min side, rounded only).
 */
public class ShapeRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "shape";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        String shape = SpecProps.str(p, "shape", "rounded");
        int fill = SpecColors.resolve(SpecProps.str(p, "fill", "@primary"), ctx.theme, ctx.theme.primary);
        int stroke = SpecColors.resolve(SpecProps.str(p, "stroke", ""), ctx.theme, 0);
        float minSide = Math.min(b.width(), b.height());
        float strokeW = SpecProps.f(p, "strokeWidth", 0f) * minSide;

        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(fill);

        if ("circle".equals(shape)) {
            float r = minSide / 2f;
            canvas.drawCircle(b.centerX(), b.centerY(), r, fillPaint);
            if (strokeW > 0) {
                canvas.drawCircle(b.centerX(), b.centerY(), r - strokeW / 2f, strokePaint(stroke, strokeW));
            }
        } else {
            float cr = "rect".equals(shape) ? 0f
                    : "pill".equals(shape) ? minSide / 2f
                    : SpecProps.f(p, "cornerRadius", 0.15f) * minSide;
            canvas.drawRoundRect(b, cr, cr, fillPaint);
            if (strokeW > 0) {
                RectF inset = new RectF(b.left + strokeW / 2f, b.top + strokeW / 2f,
                        b.right - strokeW / 2f, b.bottom - strokeW / 2f);
                canvas.drawRoundRect(inset, cr, cr, strokePaint(stroke, strokeW));
            }
        }
    }

    private Paint strokePaint(int color, float width) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(width);
        p.setColor(color);
        return p;
    }
}
