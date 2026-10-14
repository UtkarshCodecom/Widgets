package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.data.TimerStateStore;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.Glyphs;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.util.Locale;
import java.util.Map;

/**
 * A real stopwatch: elapsed time is derived from {@link TimerStateStore} timestamps (never drifts,
 * survives process death). Tapping the whole widget starts/pauses it — see
 * {@code EngineRenderer.hasTimerComponent} and {@code TimerControlReceiver}.
 * props: textColor, accentColor.
 */
public class StopwatchRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "stopwatch";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        int textColor = SpecColors.resolve(SpecProps.str(p, "textColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        int accent = SpecColors.resolve(SpecProps.str(p, "accentColor", "@primary"), ctx.theme, ctx.theme.primary);

        int subColor = SpecColors.resolve(SpecProps.str(p, "subColor", "@textSecondary"), ctx.theme, ctx.theme.textSecondary);
        boolean running = TimerStateStore.isRunning(ctx.android, ctx.appWidgetId);
        long elapsed = TimerStateStore.getElapsedMillis(ctx.android, ctx.appWidgetId, ctx.nowMillis);

        Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
        label.setColor(accent);
        label.setTextAlign(Paint.Align.CENTER);
        label.setFakeBoldText(true);
        label.setTextSize(b.height() * 0.13f);
        canvas.drawText(running ? "RUNNING" : "TAP TO START", b.centerX(), b.top + b.height() * 0.2f, label);

        Paint time = new Paint(Paint.ANTI_ALIAS_FLAG);
        time.setColor(textColor);
        time.setTextAlign(Paint.Align.CENTER);
        time.setFakeBoldText(true);
        time.setTextSize(b.height() * 0.3f);
        Paint.FontMetrics fm = time.getFontMetrics();
        canvas.drawText(format(elapsed), b.centerX(), b.centerY() - (fm.ascent + fm.descent) / 2f, time);

        // Play/pause glyph (bottom-centre — whole-widget tap toggles it).
        float r = b.height() * 0.11f;
        float cy = b.bottom - b.height() * 0.2f;
        Paint icon = new Paint(Paint.ANTI_ALIAS_FLAG);
        icon.setColor(accent);
        if (running) {
            float bw = r * 0.32f;
            canvas.drawRoundRect(new RectF(b.centerX() - r * 0.55f, cy - r * 0.7f, b.centerX() - r * 0.55f + bw, cy + r * 0.7f), bw * 0.3f, bw * 0.3f, icon);
            canvas.drawRoundRect(new RectF(b.centerX() + r * 0.23f, cy - r * 0.7f, b.centerX() + r * 0.23f + bw, cy + r * 0.7f), bw * 0.3f, bw * 0.3f, icon);
        } else {
            Path tri = new Path();
            tri.moveTo(b.centerX() - r * 0.5f, cy - r * 0.7f);
            tri.lineTo(b.centerX() + r * 0.7f, cy);
            tri.lineTo(b.centerX() - r * 0.5f, cy + r * 0.7f);
            tri.close();
            canvas.drawPath(tri, icon);
        }

        // Reset glyph (bottom-right third — the widget_ctrl_next overlay region taps here).
        Glyphs.drawReset(canvas, b.left + b.width() * 0.85f, cy, r * 0.82f, subColor);
    }

    private String format(long millis) {
        long totalSec = millis / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long sec = totalSec % 60;
        if (h > 0) return String.format(Locale.US, "%d:%02d:%02d", h, m, sec);
        return String.format(Locale.US, "%02d:%02d", m, sec);
    }
}
