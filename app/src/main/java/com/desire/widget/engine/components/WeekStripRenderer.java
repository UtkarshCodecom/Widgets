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
 * This week at a glance: month name and the week's event count on top, then the seven days
 * (two-letter weekday over the date) with today in an accent disc and a dot under every day that
 * has events (when calendar access is granted; previews show sample dots).
 *
 * <p>props: accentColor, onAccentColor, textColor, subColor, weekendColor, font, showEvents.
 */
public class WeekStripRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "week_strip";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        int accent = ClockText.color(p, "accentColor", ctx.theme, ctx.theme.primary);
        int onAccent = ClockText.color(p, "onAccentColor", ctx.theme, 0xFF101114);
        int text = ClockText.color(p, "textColor", ctx.theme, ctx.theme.textPrimary);
        int sub = ClockText.color(p, "subColor", ctx.theme, ctx.theme.textSecondary);
        int weekend = ClockText.color(p, "weekendColor", ctx.theme, text);
        boolean showEvents = SpecProps.b(p, "showEvents", true);
        String font = SpecProps.str(p, "font", "medium");

        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(ctx.nowMillis);
        Calendar start = (Calendar) now.clone();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        int back = (start.get(Calendar.DAY_OF_WEEK) - start.getFirstDayOfWeek() + 7) % 7;
        start.add(Calendar.DAY_OF_MONTH, -back);

        int[] counts = AgendaRenderer.eventsPerDay(ctx.android, start.getTimeInMillis(), 7);
        if (ctx.appWidgetId < 0) counts = new int[]{0, 2, 0, 1, 0, 1, 0}; // preview sample
        int total = 0;
        for (int c : counts) total += c;

        // Header.
        float headH = b.height() * 0.3f;
        Paint month = new Paint(Paint.ANTI_ALIAS_FLAG);
        month.setTypeface(FontResolver.resolve(font, true));
        month.setColor(text);
        month.setTextSize(headH * 0.62f);
        Paint.FontMetrics mfm = month.getFontMetrics();
        float headY = b.top + headH / 2f - (mfm.ascent + mfm.descent) / 2f;
        canvas.drawText(new SimpleDateFormat("MMMM", Locale.getDefault()).format(now.getTime()),
                b.left + b.width() * 0.03f, headY, month);
        if (showEvents && total > 0) {
            Paint ev = new Paint(month);
            ev.setTypeface(FontResolver.resolve(font, false));
            ev.setColor(sub);
            ev.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(total + (total == 1 ? " event" : " events"), b.right - b.width() * 0.03f, headY, ev);
        }

        // Days.
        float colW = b.width() / 7f;
        float labelY = b.top + b.height() * 0.5f;
        float dateCy = b.top + b.height() * 0.76f;
        float discR = Math.min(colW * 0.42f, b.height() * 0.15f);
        Paint lp = new Paint(Paint.ANTI_ALIAS_FLAG);
        lp.setTypeface(FontResolver.resolve(font, false));
        lp.setTextAlign(Paint.Align.CENTER);
        lp.setTextSize(b.height() * 0.13f);
        Paint dp = new Paint(lp);
        dp.setTypeface(FontResolver.resolve(font, true));
        dp.setTextSize(b.height() * 0.16f);
        Paint disc = new Paint(Paint.ANTI_ALIAS_FLAG);
        disc.setColor(accent);
        Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        SimpleDateFormat wd = new SimpleDateFormat("EEE", Locale.getDefault());
        Calendar day = (Calendar) start.clone();
        for (int i = 0; i < 7; i++) {
            float cx = b.left + colW * (i + 0.5f);
            boolean isToday = day.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                    && day.get(Calendar.YEAR) == now.get(Calendar.YEAR);
            int dow = day.get(Calendar.DAY_OF_WEEK);
            boolean isWeekend = dow == Calendar.SATURDAY || dow == Calendar.SUNDAY;
            String label = wd.format(day.getTime());
            label = label.length() > 2 ? label.substring(0, 2) : label;
            lp.setColor(isToday ? accent : (isWeekend ? weekend : text));
            canvas.drawText(label, cx, labelY, lp);
            if (isToday) canvas.drawCircle(cx, dateCy, discR, disc);
            dp.setColor(isToday ? onAccent : (isWeekend ? weekend : text));
            Paint.FontMetrics fm = dp.getFontMetrics();
            canvas.drawText(String.valueOf(day.get(Calendar.DAY_OF_MONTH)), cx,
                    dateCy - (fm.ascent + fm.descent) / 2f, dp);
            if (showEvents && counts[i] > 0) {
                dot.setColor(isToday ? accent : sub);
                canvas.drawCircle(cx, dateCy + discR * 1.25f, Math.max(2f, b.height() * 0.018f), dot);
            }
            day.add(Calendar.DAY_OF_MONTH, 1);
        }
    }
}
