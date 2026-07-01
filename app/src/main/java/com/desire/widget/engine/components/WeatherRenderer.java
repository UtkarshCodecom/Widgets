package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.data.WeatherSnapshot;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.util.Map;

/**
 * Weather card with a natively-drawn condition icon (no image assets), big temperature, location,
 * and hi/lo. Reads {@link com.desire.widget.engine.data.LiveDataSource} when present; component
 * props override individual fields so Studio previews are fully controllable.
 *
 * <p>props: temp, condition (clear|clouds|rain|snow|storm), location, high, low, unit, iconColor,
 * textColor, subColor.
 */
public class WeatherRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "weather";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        WeatherSnapshot w = ctx.live != null ? ctx.live.weather() : new WeatherSnapshot();
        // Per-component prop overrides
        w.temp = SpecProps.f(p, "temp", (float) w.temp);
        w.condition = SpecProps.str(p, "condition", w.condition);
        w.location = SpecProps.str(p, "location", w.location);
        w.high = SpecProps.f(p, "high", (float) w.high);
        w.low = SpecProps.f(p, "low", (float) w.low);
        w.unit = SpecProps.str(p, "unit", w.unit);

        int iconColor = SpecColors.resolve(SpecProps.str(p, "iconColor", "@primary"), ctx.theme, ctx.theme.primary);
        int textColor = SpecColors.resolve(SpecProps.str(p, "textColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        int subColor = SpecColors.resolve(SpecProps.str(p, "subColor", "@textSecondary"), ctx.theme, ctx.theme.textSecondary);

        float iconSize = Math.min(b.height() * 0.7f, b.width() * 0.4f);
        float iconCx = b.left + iconSize * 0.65f;
        float iconCy = b.top + b.height() * 0.42f;
        drawIcon(canvas, w.condition, iconCx, iconCy, iconSize / 2f, iconColor);

        float textLeft = b.left + iconSize * 1.25f;
        Paint temp = new Paint(Paint.ANTI_ALIAS_FLAG);
        temp.setColor(textColor);
        temp.setFakeBoldText(true);
        temp.setTextSize(b.height() * 0.34f);
        Paint.FontMetrics fm = temp.getFontMetrics();
        float tempY = b.top + b.height() * 0.42f - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(Math.round(w.temp) + w.unit, textLeft, tempY, temp);

        Paint sub = new Paint(Paint.ANTI_ALIAS_FLAG);
        sub.setColor(subColor);
        sub.setTextSize(b.height() * 0.13f);
        canvas.drawText(w.location, textLeft, b.top + b.height() * 0.66f, sub);
        canvas.drawText("H:" + Math.round(w.high) + w.unit + "  L:" + Math.round(w.low) + w.unit,
                textLeft, b.top + b.height() * 0.86f, sub);
    }

    private void drawIcon(Canvas canvas, String condition, float cx, float cy, float r, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        if (condition == null) condition = "clear";
        switch (condition) {
            case "clouds":
                drawCloud(canvas, cx, cy, r, p);
                break;
            case "rain":
                drawCloud(canvas, cx, cy - r * 0.15f, r * 0.9f, p);
                drawDrops(canvas, cx, cy + r * 0.55f, r, color);
                break;
            case "snow":
                drawCloud(canvas, cx, cy - r * 0.15f, r * 0.9f, p);
                drawFlakes(canvas, cx, cy + r * 0.55f, r, color);
                break;
            case "storm":
                drawCloud(canvas, cx, cy - r * 0.15f, r * 0.9f, p);
                drawBolt(canvas, cx, cy + r * 0.5f, r, color);
                break;
            default:
                drawSun(canvas, cx, cy, r, p);
                break;
        }
    }

    private void drawSun(Canvas canvas, float cx, float cy, float r, Paint p) {
        float core = r * 0.55f;
        Paint ray = new Paint(p);
        ray.setStrokeWidth(r * 0.12f);
        ray.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45);
            float x0 = cx + (float) Math.cos(a) * core * 1.35f;
            float y0 = cy + (float) Math.sin(a) * core * 1.35f;
            float x1 = cx + (float) Math.cos(a) * r;
            float y1 = cy + (float) Math.sin(a) * r;
            canvas.drawLine(x0, y0, x1, y1, ray);
        }
        canvas.drawCircle(cx, cy, core, p);
    }

    private void drawCloud(Canvas canvas, float cx, float cy, float r, Paint p) {
        canvas.drawCircle(cx - r * 0.45f, cy + r * 0.1f, r * 0.45f, p);
        canvas.drawCircle(cx + r * 0.45f, cy + r * 0.1f, r * 0.4f, p);
        canvas.drawCircle(cx, cy - r * 0.2f, r * 0.55f, p);
        RectF base = new RectF(cx - r * 0.85f, cy + r * 0.05f, cx + r * 0.85f, cy + r * 0.55f);
        canvas.drawRoundRect(base, r * 0.25f, r * 0.25f, p);
    }

    private void drawDrops(Canvas canvas, float cx, float cy, float r, int color) {
        Paint drop = new Paint(Paint.ANTI_ALIAS_FLAG);
        drop.setColor(color);
        drop.setStrokeWidth(r * 0.1f);
        drop.setStrokeCap(Paint.Cap.ROUND);
        for (int i = -1; i <= 1; i++) {
            float x = cx + i * r * 0.4f;
            canvas.drawLine(x, cy, x - r * 0.12f, cy + r * 0.3f, drop);
        }
    }

    private void drawFlakes(Canvas canvas, float cx, float cy, float r, int color) {
        Paint f = new Paint(Paint.ANTI_ALIAS_FLAG);
        f.setColor(color);
        for (int i = -1; i <= 1; i++) {
            canvas.drawCircle(cx + i * r * 0.4f, cy + r * 0.15f, r * 0.08f, f);
        }
    }

    private void drawBolt(Canvas canvas, float cx, float cy, float r, int color) {
        Paint bolt = new Paint(Paint.ANTI_ALIAS_FLAG);
        bolt.setColor(color);
        Path path = new Path();
        path.moveTo(cx + r * 0.1f, cy - r * 0.2f);
        path.lineTo(cx - r * 0.2f, cy + r * 0.2f);
        path.lineTo(cx, cy + r * 0.2f);
        path.lineTo(cx - r * 0.1f, cy + r * 0.5f);
        path.lineTo(cx + r * 0.25f, cy + r * 0.05f);
        path.lineTo(cx + r * 0.05f, cy + r * 0.05f);
        path.close();
        canvas.drawPath(path, bolt);
    }
}
