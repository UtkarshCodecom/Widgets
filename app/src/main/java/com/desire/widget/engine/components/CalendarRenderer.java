package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

/**
 * Mini month calendar with today highlighted in an accent disc and weekends tinted. Optionally
 * draws a title bar (‹ Month YYYY ›) whose three thirds line up with the real prev/today/next tap
 * regions in {@code widget_layout.xml} — EngineRenderer attaches PendingIntents there only for
 * specs that actually contain a calendar component, and injects the live {@code monthOffset} from
 * {@link com.desire.widget.engine.data.CalendarStateStore} before each render.
 *
 * <p>props: accentColor, textColor, headerColor, onAccentColor, weekendColor, showHeader,
 * showTitleBar, monthOffset (months from the current one; overwritten per-render by the engine for
 * widgets with real navigation).
 */
public class CalendarRenderer implements ComponentRenderer {
    private static final String[] WEEKDAYS = {"S", "M", "T", "W", "T", "F", "S"};

    @Override
    public String type() {
        return "calendar";
    }

    @Override
    public void render(Canvas canvas, RectF bounds, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        int accent = SpecColors.resolve(SpecProps.str(p, "accentColor", "@primary"), ctx.theme, ctx.theme.primary);
        int text = SpecColors.resolve(SpecProps.str(p, "textColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        int header = SpecColors.resolve(SpecProps.str(p, "headerColor", "@textSecondary"), ctx.theme, ctx.theme.textSecondary);
        int onAccent = SpecColors.resolve(SpecProps.str(p, "onAccentColor", "@onPrimary"), ctx.theme, ctx.theme.onPrimary);
        int weekend = SpecColors.resolve(SpecProps.str(p, "weekendColor", "@primary"), ctx.theme, ctx.theme.primary);
        boolean showHeader = SpecProps.b(p, "showHeader", true);
        boolean showTitleBar = SpecProps.b(p, "showTitleBar", false);
        int monthOffset = SpecProps.i(p, "monthOffset", 0);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ctx.nowMillis);
        if (monthOffset != 0) cal.add(Calendar.MONTH, monthOffset);
        int today = (monthOffset == 0) ? cal.get(Calendar.DAY_OF_MONTH) : -1;

        RectF b = bounds;
        if (showTitleBar) {
            float titleH = bounds.height() * 0.2f;
            RectF titleRect = new RectF(bounds.left, bounds.top, bounds.right, bounds.top + titleH);
            drawTitleBar(canvas, titleRect, cal, accent, text);
            b = new RectF(bounds.left, bounds.top + titleH, bounds.right, bounds.bottom);
        }

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
            paint.setTextSize(cellMin * 0.42f);
            for (int c = 0; c < 7; c++) {
                boolean isWeekend = c == 0 || c == 6;
                paint.setColor(isWeekend ? weekend : header);
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
            boolean isWeekendCol = col == 0 || col == 6;
            if (day == today) {
                canvas.drawCircle(cx, cy, cellMin * 0.42f, disc);
                paint.setColor(onAccent);
                paint.setFakeBoldText(true);
            } else {
                paint.setColor(isWeekendCol ? weekend : text);
                paint.setFakeBoldText(false);
            }
            drawCentered(canvas, String.valueOf(day), cx, cy, paint);
        }
    }

    /** Draws "‹  Month YYYY  ›" across thirds that line up with the real tap regions in the layout. */
    private void drawTitleBar(Canvas canvas, RectF titleRect, Calendar cal, int accent, int text) {
        float thirdW = titleRect.width() / 3f;
        float cy = titleRect.centerY();

        Paint chevron = new Paint(Paint.ANTI_ALIAS_FLAG);
        chevron.setColor(accent);
        chevron.setTextAlign(Paint.Align.CENTER);
        chevron.setFakeBoldText(true);
        chevron.setTextSize(titleRect.height() * 0.55f);
        drawCentered(canvas, "‹", titleRect.left + thirdW * 0.5f, cy, chevron);
        drawCentered(canvas, "›", titleRect.left + thirdW * 2.5f, cy, chevron);

        Paint title = new Paint(Paint.ANTI_ALIAS_FLAG);
        title.setColor(text);
        title.setTextAlign(Paint.Align.CENTER);
        title.setFakeBoldText(true);
        title.setTextSize(titleRect.height() * 0.34f);
        String label = new SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(cal.getTime());
        drawCentered(canvas, label, titleRect.left + thirdW * 1.5f, cy, title);
    }

    private void drawCentered(Canvas canvas, String s, float cx, float cy, Paint paint) {
        Paint.FontMetrics fm = paint.getFontMetrics();
        canvas.drawText(s, cx, cy - (fm.ascent + fm.descent) / 2f, paint);
    }
}
