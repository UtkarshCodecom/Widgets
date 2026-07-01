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
 * A hairline rule. props: orientation (horizontal|vertical), color, thickness (px), rounded.
 */
public class DividerRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "divider";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        int color = SpecColors.resolve(SpecProps.str(p, "color", "@textSecondary"), ctx.theme, ctx.theme.textSecondary);
        String orientation = SpecProps.str(p, "orientation", "horizontal");
        float thickness = Math.max(1f, SpecProps.f(p, "thickness", 2f));

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStrokeWidth(thickness);
        if (SpecProps.b(p, "rounded", true)) paint.setStrokeCap(Paint.Cap.ROUND);

        if ("vertical".equals(orientation)) {
            float x = b.centerX();
            canvas.drawLine(x, b.top, x, b.bottom, paint);
        } else {
            float y = b.centerY();
            canvas.drawLine(b.left, y, b.right, y, paint);
        }
    }
}
