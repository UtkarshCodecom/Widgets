package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.data.HabitStore;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.FontResolver;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shows each habit's name with a rolling 7-day dot row (filled = completed that day), read from
 * {@link HabitStore}. Tap opens the habit editor (action type {@code edit_habits}).
 * props: titleColor, textColor, accentColor, trackColor, font.
 */
public class HabitTrackerRenderer implements ComponentRenderer {
    private static final SimpleDateFormat DAY_KEY = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    public String type() {
        return "habit_tracker";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        int titleColor = SpecColors.resolve(SpecProps.str(p, "titleColor", "@primary"), ctx.theme, ctx.theme.primary);
        int textColor = SpecColors.resolve(SpecProps.str(p, "textColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        int accent = SpecColors.resolve(SpecProps.str(p, "accentColor", "@primary"), ctx.theme, ctx.theme.primary);
        int track = SpecColors.resolve(SpecProps.str(p, "trackColor", "#33FFFFFF"), ctx.theme, 0x33FFFFFF);
        String font = SpecProps.str(p, "font", ctx.theme.fontFamily);

        float headH = b.height() * 0.18f;
        Paint head = new Paint(Paint.ANTI_ALIAS_FLAG);
        head.setColor(titleColor);
        head.setTypeface(FontResolver.resolve(font, true));
        head.setTextSize(headH * 0.65f);
        canvas.drawText("HABITS", b.left, b.top + headH * 0.72f, head);

        List<HabitStore.Habit> habits = HabitStore.getHabits(ctx.android, ctx.appWidgetId);
        String[] last7 = last7DayKeys(ctx.nowMillis);

        float rowH = (b.height() - headH) / Math.max(1, Math.min(habits.size(), 4));
        float y = b.top + headH;

        Paint name = new Paint(Paint.ANTI_ALIAS_FLAG);
        name.setColor(textColor);
        name.setTypeface(FontResolver.resolve(font, false));
        name.setTextAlign(Paint.Align.LEFT);

        Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);

        int shown = Math.min(habits.size(), 4);
        for (int i = 0; i < shown; i++) {
            HabitStore.Habit habit = habits.get(i);
            float cy = y + rowH * 0.5f;

            name.setTextSize(rowH * 0.32f);
            String label = habit.name != null ? habit.name : "";
            Paint.FontMetrics nfm = name.getFontMetrics();
            canvas.drawText(ellipsize(label, name, b.width() * 0.42f), b.left, cy - (nfm.ascent + nfm.descent) / 2f, name);

            float dotArea = b.width() * 0.55f;
            float dotR = Math.min(rowH * 0.16f, dotArea / 7f * 0.32f);
            float spacing = dotArea / 7f;
            float startX = b.right - dotArea;
            for (int d = 0; d < 7; d++) {
                boolean done = habit.doneDates != null && habit.doneDates.contains(last7[d]);
                dot.setColor(done ? accent : track);
                canvas.drawCircle(startX + spacing * (d + 0.5f), cy, dotR, dot);
            }
            y += rowH;
        }

        if (habits.isEmpty()) {
            Paint empty = new Paint(Paint.ANTI_ALIAS_FLAG);
            empty.setColor(textColor);
            empty.setAlpha(150);
            empty.setTextSize(b.height() * 0.14f);
            canvas.drawText("Tap to add a habit", b.left, b.top + headH + rowH * 0.5f, empty);
        }
    }

    private String[] last7DayKeys(long nowMillis) {
        String[] keys = new String[7];
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(nowMillis);
        for (int i = 6; i >= 0; i--) {
            keys[i] = DAY_KEY.format(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
        return keys;
    }

    private String ellipsize(String s, Paint paint, float maxWidth) {
        if (paint.measureText(s) <= maxWidth) return s;
        while (s.length() > 1 && paint.measureText(s + "…") > maxWidth) s = s.substring(0, s.length() - 1);
        return s + "…";
    }
}
