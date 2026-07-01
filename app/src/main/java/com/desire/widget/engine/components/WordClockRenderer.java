package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.FontResolver;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.util.Calendar;
import java.util.Map;

/**
 * Tells the time in words ("HALF PAST TEN"), rounded to the nearest 5 minutes. props: color, font.
 */
public class WordClockRenderer implements ComponentRenderer {
    private static final String[] NUM = {
            "twelve", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
            "ten", "eleven", "twelve"
    };
    private static final String[] MINS = {
            "o'clock", "five past", "ten past", "quarter past", "twenty past", "twenty five past",
            "half past", "twenty five to", "twenty to", "quarter to", "ten to", "five to", "o'clock"
    };

    @Override
    public String type() {
        return "word_clock";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        int color = SpecColors.resolve(SpecProps.str(p, "color", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        String font = SpecProps.str(p, "font", ctx.theme.fontFamily);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ctx.nowMillis);
        int hour = cal.get(Calendar.HOUR); // 0..11
        int minute = cal.get(Calendar.MINUTE);
        int bucket = Math.round(minute / 5f); // 0..12

        int displayHour = hour;
        if (bucket >= 7) displayHour = (hour + 1) % 12; // "to" the next hour
        String phrase = (bucket == 0 || bucket == 12)
                ? NUM[displayHour] + " o'clock"
                : MINS[bucket] + " " + NUM[displayHour];
        phrase = phrase.toUpperCase();

        TextPaint tp = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        tp.setColor(color);
        tp.setTypeface(FontResolver.resolve(font, true));
        tp.setTextSize(b.height() * 0.26f);

        int width = Math.max(1, (int) b.width());
        StaticLayout layout = StaticLayout.Builder
                .obtain(phrase, 0, phrase.length(), tp, width)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setIncludePad(false)
                .build();
        float ty = b.top + Math.max(0f, (b.height() - layout.getHeight()) / 2f);
        canvas.save();
        canvas.translate(b.left, ty);
        layout.draw(canvas);
        canvas.restore();
    }
}
