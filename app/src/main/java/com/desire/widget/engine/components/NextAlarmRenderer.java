package com.desire.widget.engine.components;

import android.app.AlarmManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.format.DateFormat;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * The user's next scheduled alarm, from {@link AlarmManager#getNextAlarmClock()} (no permission
 * needed): a bell glyph, the alarm time and a day label (Today / Tomorrow / weekday) plus a
 * "in 6h 20m" countdown. Shows "No alarm set" when none is scheduled.
 *
 * <p>props: layout (stack|row), accentColor, textColor, subColor.
 */
public class NextAlarmRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "next_alarm";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        int accent = SpecColors.resolve(SpecProps.str(p, "accentColor", "@primary"), ctx.theme, ctx.theme.primary);
        int textColor = SpecColors.resolve(SpecProps.str(p, "textColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        int subColor = SpecColors.resolve(SpecProps.str(p, "subColor", "@textSecondary"), ctx.theme, ctx.theme.textSecondary);
        boolean row = "row".equals(SpecProps.str(p, "layout", "stack"));

        long trigger = nextAlarm(ctx.android);
        // Gallery preview with no alarm scheduled: show a representative sample instead of empty.
        if (trigger <= 0 && ctx.appWidgetId < 0) trigger = ctx.nowMillis + (8 * 60 + 20) * 60_000L;

        String time, day, until;
        if (trigger > 0) {
            boolean use24 = DateFormat.is24HourFormat(ctx.android);
            time = new SimpleDateFormat(use24 ? "HH:mm" : "h:mm a", Locale.getDefault()).format(new Date(trigger));
            day = dayLabel(ctx.nowMillis, trigger);
            until = "in " + duration(trigger - ctx.nowMillis);
        } else {
            time = "No alarm";
            day = "Tap to set one";
            until = "";
        }

        if (row) {
            float iconR = b.height() * 0.30f;
            drawBell(canvas, b.left + iconR * 1.1f, b.centerY(), iconR, accent);
            float x = b.left + iconR * 2.6f;
            Paint tp = paint(textColor, b.height() * 0.40f, true, Paint.Align.LEFT);
            canvas.drawText(time, x, b.centerY() + b.height() * 0.06f, tp);
            Paint sp = paint(subColor, b.height() * 0.20f, false, Paint.Align.RIGHT);
            canvas.drawText(day, b.right, b.centerY() - b.height() * 0.04f, sp);
            if (!until.isEmpty()) canvas.drawText(until, b.right, b.centerY() + b.height() * 0.24f, sp);
        } else {
            float iconR = b.height() * 0.13f;
            drawBell(canvas, b.centerX(), b.top + b.height() * 0.18f, iconR, accent);
            Paint tp = paint(textColor, b.height() * 0.22f, true, Paint.Align.CENTER);
            float w = tp.measureText(time);
            if (w > b.width() * 0.96f) tp.setTextSize(tp.getTextSize() * b.width() * 0.96f / w);
            drawCentered(canvas, time, b.centerX(), b.top + b.height() * 0.52f, tp);
            Paint dp = paint(accent, b.height() * 0.11f, true, Paint.Align.CENTER);
            drawCentered(canvas, day, b.centerX(), b.top + b.height() * 0.72f, dp);
            if (!until.isEmpty()) {
                Paint up = paint(subColor, b.height() * 0.09f, false, Paint.Align.CENTER);
                drawCentered(canvas, until, b.centerX(), b.top + b.height() * 0.86f, up);
            }
        }
    }

    private long nextAlarm(Context c) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            AlarmManager.AlarmClockInfo info = am != null ? am.getNextAlarmClock() : null;
            return info != null ? info.getTriggerTime() : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private String dayLabel(long now, long t) {
        Calendar a = Calendar.getInstance();
        a.setTimeInMillis(now);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        int diff = (c.get(Calendar.YEAR) - a.get(Calendar.YEAR)) * 366
                + c.get(Calendar.DAY_OF_YEAR) - a.get(Calendar.DAY_OF_YEAR);
        if (diff == 0) return "Today";
        if (diff == 1) return "Tomorrow";
        return new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date(t));
    }

    private String duration(long ms) {
        long mins = Math.max(0, ms / 60_000L);
        long h = mins / 60, m = mins % 60;
        if (h >= 24) return (h / 24) + "d " + (h % 24) + "h";
        return h > 0 ? h + "h " + m + "m" : m + "m";
    }

    /** Simple alarm-bell glyph centred at (cx, cy) with radius r. */
    private void drawBell(Canvas canvas, float cx, float cy, float r, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        Path bell = new Path();
        bell.moveTo(cx - r * 0.75f, cy + r * 0.45f);
        bell.cubicTo(cx - r * 0.55f, cy + r * 0.2f, cx - r * 0.62f, cy - r * 0.9f, cx, cy - r * 0.9f);
        bell.cubicTo(cx + r * 0.62f, cy - r * 0.9f, cx + r * 0.55f, cy + r * 0.2f, cx + r * 0.75f, cy + r * 0.45f);
        bell.close();
        canvas.drawPath(bell, p);
        canvas.drawRoundRect(new RectF(cx - r * 0.85f, cy + r * 0.40f, cx + r * 0.85f, cy + r * 0.58f), r * 0.1f, r * 0.1f, p);
        canvas.drawCircle(cx, cy + r * 0.78f, r * 0.17f, p);
        canvas.drawCircle(cx, cy - r * 0.98f, r * 0.11f, p);
    }

    private Paint paint(int color, float size, boolean bold, Paint.Align align) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTextSize(size);
        p.setFakeBoldText(bold);
        p.setTextAlign(align);
        return p;
    }

    private void drawCentered(Canvas canvas, String t, float x, float y, Paint p) {
        Paint.FontMetrics fm = p.getFontMetrics();
        canvas.drawText(t, x, y - (fm.ascent + fm.descent) / 2f, p);
    }
}
