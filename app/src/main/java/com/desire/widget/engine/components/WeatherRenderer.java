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
 * textColor, subColor, style (row | card — city, huge temperature, icon + condition | tiles — four
 * shaped tiles: now and the next three days; shapes / fills / textColors / font).
 */
public class WeatherRenderer implements ComponentRenderer {
    /** PreferenceManager key: show temperatures in °F instead of °C. */
    public static final String PREF_FAHRENHEIT = "temp_fahrenheit";

    @Override
    public String type() {
        return "weather";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        WeatherSnapshot w = ctx.live != null ? ctx.live.weather() : new WeatherSnapshot();
        // Sample props apply ONLY to gallery/customize previews (appWidgetId < 0). On a placed
        // widget the fetched snapshot always wins — letting these placeholder props override it
        // pinned the widget to the fake "24° City" forever (the old bug).
        if (ctx.appWidgetId < 0) {
            w.temp = SpecProps.f(p, "temp", (float) w.temp);
            w.condition = SpecProps.str(p, "condition", w.condition);
            w.location = SpecProps.str(p, "location", w.location);
            w.high = SpecProps.f(p, "high", (float) w.high);
            w.low = SpecProps.f(p, "low", (float) w.low);
            w.unit = SpecProps.str(p, "unit", w.unit);
        }
        // Settings → Temperature unit. Data is fetched in °C; convert for display.
        if (com.desire.widget.util.PreferenceManager.getInstance(ctx.android).getBoolean(PREF_FAHRENHEIT, false)) {
            w.temp = w.temp * 9.0 / 5.0 + 32.0;
            w.high = w.high * 9.0 / 5.0 + 32.0;
            w.low = w.low * 9.0 / 5.0 + 32.0;
        }

        int iconColor = SpecColors.resolve(SpecProps.str(p, "iconColor", "@primary"), ctx.theme, ctx.theme.primary);
        int textColor = SpecColors.resolve(SpecProps.str(p, "textColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        int subColor = SpecColors.resolve(SpecProps.str(p, "subColor", "@textSecondary"), ctx.theme, ctx.theme.textSecondary);
        boolean fahrenheit = com.desire.widget.util.PreferenceManager.getInstance(ctx.android)
                .getBoolean(PREF_FAHRENHEIT, false);
        String style = SpecProps.str(p, "style", "row");
        if ("card".equals(style)) {
            drawCard(canvas, b, p, w, iconColor, textColor, subColor);
            return;
        }
        if ("tiles".equals(style)) {
            drawTiles(canvas, b, p, ctx, w, fahrenheit);
            return;
        }

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
    // ------------------------------------------------------------------ card / tiles styles

    private static String describe(WeatherSnapshot w) {
        if (w.description != null && !w.description.isEmpty()) return w.description;
        switch (w.condition == null ? "clear" : w.condition) {
            case "clouds": return "Cloudy";
            case "rain": return "Rain";
            case "snow": return "Snow";
            case "storm": return "Thunderstorm";
            default: return "Clear";
        }
    }

    /** City on top, a huge temperature, then icon + condition — the tall "card" look. */
    private void drawCard(Canvas canvas, RectF b, java.util.Map<String, Object> p, WeatherSnapshot w,
                          int iconColor, int textColor, int subColor) {
        android.graphics.Typeface bold = com.desire.widget.engine.util.FontResolver.resolve(
                SpecProps.str(p, "font", "medium"), true);
        Paint loc = new Paint(Paint.ANTI_ALIAS_FLAG);
        loc.setTypeface(bold);
        loc.setColor(textColor);
        loc.setTextSize(b.height() * 0.11f);
        String city = w.location;
        while (city.length() > 1 && loc.measureText(city) > b.width()) city = city.substring(0, city.length() - 1);
        canvas.drawText(city, b.left, b.top + b.height() * 0.13f, loc);
        Paint temp = new Paint(Paint.ANTI_ALIAS_FLAG);
        temp.setTypeface(bold);
        temp.setColor(textColor);
        temp.setTextSize(b.height() * 0.36f);
        canvas.drawText(Math.round(w.temp) + "°", b.left - b.height() * 0.01f, b.top + b.height() * 0.5f, temp);
        drawIcon(canvas, w.condition, b.left + b.height() * 0.07f, b.top + b.height() * 0.68f, b.height() * 0.07f, iconColor);
        Paint cond = new Paint(Paint.ANTI_ALIAS_FLAG);
        cond.setColor(subColor);
        cond.setTextSize(b.height() * 0.1f);
        canvas.drawText(describe(w), b.left, b.top + b.height() * 0.9f, cond);
    }

    /** Four shaped tiles: now + the next three days (icon, temperature, day). */
    private void drawTiles(Canvas canvas, RectF b, java.util.Map<String, Object> p, RenderContext ctx,
                           WeatherSnapshot w, boolean fahrenheit) {
        java.util.List<WeatherSnapshot.Day> days = w.days();
        String[] conds = new String[4];
        double[] temps = new double[4];
        String[] labels = new String[4];
        conds[0] = w.condition;
        temps[0] = w.temp; // already converted above when °F is on
        labels[0] = "Now";
        java.text.SimpleDateFormat dayFmt = new java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault());
        if (days.size() >= 4 && ctx.appWidgetId >= 0) {
            for (int i = 1; i < 4; i++) {
                WeatherSnapshot.Day d = days.get(i);
                conds[i] = d.condition;
                temps[i] = fahrenheit ? d.high * 9.0 / 5.0 + 32.0 : d.high;
                labels[i] = dayFmt.format(new java.util.Date(d.epochSec * 1000L));
            }
        } else {
            String[] sc = {"rain", "clouds", "snow"};
            double[] st = {12, 12, -4};
            java.util.Calendar cal = java.util.Calendar.getInstance();
            for (int i = 1; i < 4; i++) {
                cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
                conds[i] = sc[i - 1];
                temps[i] = st[i - 1];
                labels[i] = dayFmt.format(cal.getTime());
            }
        }
        java.util.List<String> shapes = TimeTilesRenderer.strings(p, "shapes",
                new String[]{"cookie", "blob", "squircle", "rounded"});
        int[] fills = com.desire.widget.engine.util.ClockText.colors(p, "fills", ctx.theme,
                new int[]{0xFFEDEDF0, 0xFFE5322D, 0xFF8E9BF0, 0xFFEDEDF0});
        int[] inks = com.desire.widget.engine.util.ClockText.colors(p, "textColors", ctx.theme,
                new int[]{0xFF4A4A52, 0xFFFFE9E6, 0xFFE9ECFF, 0xFF4A4A52});
        float side = Math.min(b.width(), b.height());
        float cell = side / 2f;
        float left = b.centerX() - side / 2f, top = b.centerY() - side / 2f;
        Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setTypeface(com.desire.widget.engine.util.FontResolver.resolve(SpecProps.str(p, "font", "medium"), true));
        tp.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < 4; i++) {
            float inset = cell * 0.06f;
            RectF tile = new RectF(left + cell * (i % 2) + inset, top + cell * (i / 2) + inset,
                    left + cell * (i % 2) + cell - inset, top + cell * (i / 2) + cell - inset);
            fp.setColor(fills[i % fills.length]);
            canvas.drawPath(com.desire.widget.engine.util.Shapes.path(shapes.get(i % shapes.size()), tile), fp);
            int ink = inks[i % inks.length];
            drawIcon(canvas, conds[i], tile.centerX(), tile.top + tile.height() * 0.34f, tile.height() * 0.15f, ink);
            tp.setColor(ink);
            tp.setTextSize(tile.height() * 0.2f);
            canvas.drawText(Math.round(temps[i]) + "°", tile.centerX(), tile.top + tile.height() * 0.7f, tp);
            Paint lp = new Paint(tp);
            lp.setTypeface(com.desire.widget.engine.util.FontResolver.resolve(SpecProps.str(p, "font", "medium"), false));
            lp.setTextSize(tile.height() * 0.1f);
            lp.setAlpha(190);
            canvas.drawText(labels[i], tile.centerX(), tile.top + tile.height() * 0.85f, lp);
        }
    }
}
