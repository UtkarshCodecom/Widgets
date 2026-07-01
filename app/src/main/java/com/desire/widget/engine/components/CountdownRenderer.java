package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Counts down to a target instant relative to {@code ctx.nowMillis}, so the minute-tick refresh
 * advances it. Shows an optional label, the remaining time, and an optional progress bar.
 *
 * <p>props: targetMillis (epoch ms) OR targetDate ("yyyy-MM-dd HH:mm"); label; fromMillis (start
 * for progress); showProgress; labelColor; timeColor; accentColor.
 */
public class CountdownRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "countdown";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        long target = resolveTarget(p);
        long remaining = Math.max(0L, target - ctx.nowMillis);

        String label = SpecProps.str(p, "label", "Countdown");
        int labelColor = SpecColors.resolve(SpecProps.str(p, "labelColor", "@textSecondary"), ctx.theme, ctx.theme.textSecondary);
        int timeColor = SpecColors.resolve(SpecProps.str(p, "timeColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        int accent = SpecColors.resolve(SpecProps.str(p, "accentColor", "@primary"), ctx.theme, ctx.theme.primary);
        boolean showProgress = SpecProps.b(p, "showProgress", true);

        Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(labelColor);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(b.height() * 0.13f);
        canvas.drawText(label, b.centerX(), b.top + b.height() * 0.24f, labelPaint);

        Paint timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        timePaint.setColor(timeColor);
        timePaint.setFakeBoldText(true);
        timePaint.setTextAlign(Paint.Align.CENTER);
        timePaint.setTextSize(b.height() * 0.26f);
        Paint.FontMetrics fm = timePaint.getFontMetrics();
        float ty = b.top + b.height() * (showProgress ? 0.52f : 0.6f) - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(format(remaining), b.centerX(), ty, timePaint);

        if (showProgress) {
            long from = (long) SpecProps.f(p, "fromMillis", 0f);
            float progress = 1f;
            if (target > from && from > 0) {
                progress = 1f - (float) remaining / (float) (target - from);
                progress = Math.max(0f, Math.min(1f, progress));
            }
            float barY = b.top + b.height() * 0.78f;
            float barLeft = b.left + b.width() * 0.12f;
            float barRight = b.right - b.width() * 0.12f;
            float barH = b.height() * 0.07f;
            Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
            track.setColor(labelColor);
            track.setAlpha(80);
            canvas.drawRoundRect(new RectF(barLeft, barY, barRight, barY + barH), barH, barH, track);
            Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
            fill.setColor(accent);
            canvas.drawRoundRect(new RectF(barLeft, barY, barLeft + (barRight - barLeft) * progress, barY + barH), barH, barH, fill);
        }
    }

    private long resolveTarget(Map<String, Object> p) {
        float ms = SpecProps.f(p, "targetMillis", 0f);
        if (ms > 0) return (long) ms;
        String date = SpecProps.str(p, "targetDate", "");
        if (!date.isEmpty()) {
            try {
                Date d = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse(date);
                if (d != null) return d.getTime();
            } catch (ParseException ignored) {
            }
        }
        return System.currentTimeMillis();
    }

    private String format(long remaining) {
        long totalSec = remaining / 1000L;
        long days = totalSec / 86400L;
        long hours = (totalSec % 86400L) / 3600L;
        long mins = (totalSec % 3600L) / 60L;
        if (days > 0) {
            return String.format(Locale.US, "%dd %02d:%02d", days, hours, mins);
        }
        long secs = totalSec % 60L;
        return String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs);
    }
}
