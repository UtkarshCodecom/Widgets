package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.data.WeatherSnapshot;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Shows today's sunrise and sunset times, fetched from Open-Meteo's daily API by
 * {@link com.desire.widget.worker.WeatherWorker} alongside the weather and read here from
 * {@link com.desire.widget.engine.data.LiveDataSource}. props: sunColor, textColor, subColor.
 */
public class SunriseSunsetRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "sunrise_sunset";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        int sunColor = SpecColors.resolve(SpecProps.str(p, "sunColor", "#FFA726"), ctx.theme, 0xFFFFA726);
        int textColor = SpecColors.resolve(SpecProps.str(p, "textColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        int subColor = SpecColors.resolve(SpecProps.str(p, "subColor", "@textSecondary"), ctx.theme, ctx.theme.textSecondary);

        WeatherSnapshot w = ctx.live != null ? ctx.live.weather() : new WeatherSnapshot();
        SimpleDateFormat fmt = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String sunrise = w.sunriseMillis > 0 ? fmt.format(new Date(w.sunriseMillis)) : "—";
        String sunset = w.sunsetMillis > 0 ? fmt.format(new Date(w.sunsetMillis)) : "—";

        float half = b.width() / 2f;
        drawColumn(canvas, new RectF(b.left, b.top, b.left + half, b.bottom), true, sunrise, sunColor, textColor, subColor);
        drawColumn(canvas, new RectF(b.left + half, b.top, b.right, b.bottom), false, sunset, sunColor, textColor, subColor);
    }

    private void drawColumn(Canvas canvas, RectF col, boolean rise, String time, int sunColor, int textColor, int subColor) {
        float cx = col.centerX();
        float iconR = col.width() * 0.22f;
        float iconCy = col.top + col.height() * 0.32f;

        // Horizon line
        Paint horizon = new Paint(Paint.ANTI_ALIAS_FLAG);
        horizon.setColor(subColor);
        horizon.setAlpha(120);
        horizon.setStrokeWidth(Math.max(1.5f, iconR * 0.08f));
        canvas.drawLine(cx - iconR * 1.3f, iconCy, cx + iconR * 1.3f, iconCy, horizon);

        // Sun: half-disc above the horizon for sunrise, below for sunset.
        Paint sun = new Paint(Paint.ANTI_ALIAS_FLAG);
        sun.setColor(sunColor);
        RectF sunRect = new RectF(cx - iconR, iconCy - iconR, cx + iconR, iconCy + iconR);
        canvas.drawArc(sunRect, rise ? 180 : 0, 180, true, sun);

        // Arrow indicating rising/setting.
        Paint arrow = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrow.setColor(sunColor);
        arrow.setStrokeWidth(Math.max(2f, iconR * 0.12f));
        float ax = cx + iconR * 1.7f;
        float ay1 = rise ? iconCy + iconR * 0.6f : iconCy - iconR * 0.6f;
        float ay2 = rise ? iconCy - iconR * 0.6f : iconCy + iconR * 0.6f;
        canvas.drawLine(ax, ay1, ax, ay2, arrow);
        canvas.drawLine(ax, ay2, ax - (rise ? -1 : 1) * iconR * 0.3f, ay2 + (rise ? 1 : -1) * iconR * 0.35f, arrow);

        Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
        label.setColor(subColor);
        label.setTextAlign(Paint.Align.CENTER);
        label.setTextSize(col.height() * 0.12f);
        canvas.drawText(rise ? "SUNRISE" : "SUNSET", cx, col.top + col.height() * 0.6f, label);

        Paint value = new Paint(Paint.ANTI_ALIAS_FLAG);
        value.setColor(textColor);
        value.setTextAlign(Paint.Align.CENTER);
        value.setFakeBoldText(true);
        value.setTextSize(col.height() * 0.17f);
        canvas.drawText(time, cx, col.top + col.height() * 0.8f, value);
    }
}
