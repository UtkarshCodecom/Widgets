package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.util.Calendar;
import java.util.Map;

/**
 * Mini month calendar for the current month, with today highlighted in an accent disc. Lays out a
 * weekday header row + up to 6 week rows, sized to fit the bounds. props: accentColor, textColor,
 * headerColor, onAccentColor, showHeader.
 */
public class CalendarRenderer implements ComponentRenderer {
    private static final String[] WEEKDAYS = {"S", "M", "T", "W", "T", "F", "S"};

    @Override
    public String type() {
        return "calendar";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        int accent = SpecColors.resolve(SpecProps.str(p, "accentColor", "@primary"), ctx.theme, ctx.theme.primary);
        int text = SpecColors.resolve(SpecProps.str(p, "textColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        int header = SpecColors.resolve(SpecProps.str(p, "headerColor", "@textSecondary"), ctx.theme, ctx.theme.textSecondary);
        int onAccent = SpecColors.resolve(SpecProps.str(p, "onAccentColor", "@onPrimary"), ctx.theme, ctx.theme.onPrimary);
        boolean showHeader = SpecProps.b(p, "showHeader", true);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ctx.nowMillis);
        int today = cal.get(Calendar.DAY_OF_MONTH);

        Calendar first = (Calendar) cal.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);
        int leadOffset = first.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY; // 0..6
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int weeks = (int) Math.ceil((leadOffset + daysInMonth) / 7.0);

        int headerRows = showHeader ? 1 : 0;
        int totalRows = weeks + headerRows;
        float cellW = b.width() / 7f;
        float cellH = b.height() / totalRows;
        float cellMin = Math.min(cellW, cellH);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextAlign(Paint.Align.CENTER);

        if (showHeader) {
            paint.setColor(header);
            paint.setTextSize(cellMin * 0.42f);
            for (int c = 0; c < 7; c++) {
                float cx = b.left + cellW * (c + 0.5f);
                float cy = b.top + cellH * 0.5f;
                drawCentered(canvas, WEEKDAYS[c], cx, cy, paint);
            }
        }

        Paint disc = new Paint(Paint.ANTI_ALIAS_FLAG);
        disc.setColor(accent);

        paint.setTextSize(cellMin * 0.46f);
        for (int day = 1; day <= daysInMonth; day++) {
            int index = leadOffset + (day - 1);
            int row = index / 7;
            int col = index % 7;
            float cx = b.left + cellW * (col + 0.5f);
            float cy = b.top + cellH * (row + headerRows + 0.5f);
            if (day == today) {
                canvas.drawCircle(cx, cy, cellMin * 0.42f, disc);
                paint.setColor(onAccent);
                paint.setFakeBoldText(true);
            } else {
                paint.setColor(text);
                paint.setFakeBoldText(false);
            }
            drawCentered(canvas, String.valueOf(day), cx, cy, paint);
        }
    }

    private void drawCentered(Canvas canvas, String s, float cx, float cy, Paint paint) {
        Paint.FontMetrics fm = paint.getFontMetrics();
        canvas.drawText(s, cx, cy - (fm.ascent + fm.descent) / 2f, paint);
    }
}
