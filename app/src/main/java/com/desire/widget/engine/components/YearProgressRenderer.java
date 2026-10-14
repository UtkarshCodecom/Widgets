package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.ClockText;
import com.desire.widget.engine.util.FontResolver;
import com.desire.widget.engine.util.SpecProps;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

/**
 * How much of the day / week / month / year has passed: a label and percentage over a dot grid
 * (one dot per hour, day or day-of-year; passed = filled, now = accent), a bar, or a ring.
 *
 * <p>props: scope (day|week|month|year), style (dots|bar|ring), fillColor, emptyColor,
 * accentColor, textColor, subColor, font, label (override).
 */
public class YearProgressRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "year_progress";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        String scope = SpecProps.str(p, "scope", "month");
        String style = SpecProps.str(p, "style", "dots");
        int fill = ClockText.color(p, "fillColor", ctx.theme, ctx.theme.textPrimary);
        int empty = ClockText.color(p, "emptyColor", ctx.theme, (fill & 0x00FFFFFF) | 0x33000000);
        int accent = ClockText.color(p, "accentColor", ctx.theme, ctx.theme.primary);
        int text = ClockText.color(p, "textColor", ctx.theme, ctx.theme.textPrimary);
        int sub = ClockText.color(p, "subColor", ctx.theme, text);
        String font = SpecProps.str(p, "font", "medium");

        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(ctx.nowMillis);
        int units, done;
        float fraction;
        String label;
        float dayFrac = (now.get(Calendar.HOUR_OF_DAY) * 3600 + now.get(Calendar.MINUTE) * 60
                + now.get(Calendar.SECOND)) / 86400f;
        switch (scope) {
            case "day":
                units = 24;
                done = now.get(Calendar.HOUR_OF_DAY);
                fraction = dayFrac;
                label = "Today";
                break;
            case "week": {
                units = 7;
                done = (now.get(Calendar.DAY_OF_WEEK) - now.getFirstDayOfWeek() + 7) % 7;
                fraction = (done + dayFrac) / 7f;
                label = "This week";
                break;
            }
            case "year": {
                units = now.getActualMaximum(Calendar.DAY_OF_YEAR);
                done = now.get(Calendar.DAY_OF_YEAR) - 1;
                fraction = (done + dayFrac) / units;
                label = String.valueOf(now.get(Calendar.YEAR));
                break;
            }
            default: {
                units = now.getActualMaximum(Calendar.DAY_OF_MONTH);
                done = now.get(Calendar.DAY_OF_MONTH) - 1;
                fraction = (done + dayFrac) / units;
                label = new SimpleDateFormat("MMMM", Locale.getDefault()).format(now.getTime());
            }
        }
        String custom = SpecProps.str(p, "label", "");
        if (custom != null && !custom.isEmpty()) label = custom;
        String pct = Math.round(fraction * 100f) + "%";

        if ("ring".equals(style)) {
            float r = Math.min(b.width(), b.height()) / 2f * 0.86f;
            float sw = r * 0.16f;
            RectF oval = new RectF(b.centerX() - r + sw / 2f, b.centerY() - r + sw / 2f,
                    b.centerX() + r - sw / 2f, b.centerY() + r - sw / 2f);
            Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(sw);
            ring.setStrokeCap(Paint.Cap.ROUND);
            ring.setColor(empty);
            canvas.drawArc(oval, 0, 360, false, ring);
            ring.setColor(accent);
            canvas.drawArc(oval, -90, Math.max(1f, 360f * fraction), false, ring);
            Paint big = text(font, true, text, r * 0.5f, Paint.Align.CENTER);
            Paint.FontMetrics fm = big.getFontMetrics();
            canvas.drawText(pct, b.centerX(), b.centerY() - (fm.ascent + fm.descent) / 2f - r * 0.08f, big);
            Paint small = text(font, false, sub, r * 0.18f, Paint.Align.CENTER);
            canvas.drawText(label, b.centerX(), b.centerY() + r * 0.42f, small);
            return;
        }

        // Header: label left, percentage right.
        float headH = b.height() * 0.2f;
        Paint lp = text(font, true, accent, headH * 0.7f, Paint.Align.LEFT);
        Paint.FontMetrics hfm = lp.getFontMetrics();
        float headY = b.top + headH / 2f - (hfm.ascent + hfm.descent) / 2f;
        canvas.drawText(label, b.left, headY, lp);
        Paint pp = text(font, true, text, headH * 0.7f, Paint.Align.RIGHT);
        canvas.drawText(pct, b.right, headY, pp);

        RectF body = new RectF(b.left, b.top + headH * 1.35f, b.right, b.bottom);
        if ("bar".equals(style)) {
            float h = Math.min(body.height() * 0.5f, b.height() * 0.22f);
            RectF track = new RectF(body.left, body.centerY() - h / 2f, body.right, body.centerY() + h / 2f);
            Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
            tp.setColor(empty);
            canvas.drawRoundRect(track, h / 2f, h / 2f, tp);
            tp.setColor(accent);
            canvas.drawRoundRect(new RectF(track.left, track.top,
                    track.left + Math.max(h, track.width() * fraction), track.bottom), h / 2f, h / 2f, tp);
            return;
        }

        // Dots: pick columns so the grid is roughly as wide as the body allows.
        int cols = "week".equals(scope) ? 7 : "day".equals(scope) ? 6 : 7;
        if ("year".equals(scope)) {
            // ~365 dots: pick the column count that makes the grid match the body's shape.
            cols = Math.max(7, Math.round((float) Math.sqrt(units * body.width() / Math.max(1f, body.height()))));
        }
        int rows = (units + cols - 1) / cols;
        float pitch = Math.min(body.width() / cols, body.height() / rows);
        float gridW = pitch * cols, gridH = pitch * rows;
        float left = body.centerX() - gridW / 2f, top = body.top + (body.height() - gridH) / 2f;
        float r = pitch * ("year".equals(scope) ? 0.36f : 0.32f);
        Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (int i = 0; i < units; i++) {
            dot.setColor(i < done ? fill : i == done ? accent : empty);
            float cx = left + (i % cols + 0.5f) * pitch, cy = top + (i / cols + 0.5f) * pitch;
            canvas.drawCircle(cx, cy, i == done ? r * 1.15f : r, dot);
        }
    }

    private Paint text(String font, boolean bold, int color, float size, Paint.Align align) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTypeface(FontResolver.resolve(font, bold));
        p.setColor(color);
        p.setTextSize(size);
        p.setTextAlign(align);
        return p;
    }
}
