package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
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
 * A real Pomodoro timer: the accumulated running time from {@link TimerStateStore} is interpreted
 * as a position within a repeating work/break cycle (pure function of elapsed time, so the phase
 * and remaining time are always correct without needing a background job to flip phases). Tapping
 * the whole widget starts/pauses — see {@code EngineRenderer.hasTimerComponent}.
 * props: workMinutes (default 25), breakMinutes (default 5), workColor, breakColor, textColor.
 */
public class PomodoroRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "pomodoro";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        int workColor = SpecColors.resolve(SpecProps.str(p, "workColor", "@primary"), ctx.theme, ctx.theme.primary);
        int breakColor = SpecColors.resolve(SpecProps.str(p, "breakColor", "#66BB6A"), ctx.theme, 0xFF66BB6A);
        int textColor = SpecColors.resolve(SpecProps.str(p, "textColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        long workMillis = (long) SpecProps.f(p, "workMinutes", 25f) * 60_000L;
        long breakMillis = (long) SpecProps.f(p, "breakMinutes", 5f) * 60_000L;
        long cycleMillis = Math.max(1000, workMillis + breakMillis);

        boolean running = TimerStateStore.isRunning(ctx.android, ctx.appWidgetId);
        long elapsed = TimerStateStore.getElapsedMillis(ctx.android, ctx.appWidgetId, ctx.nowMillis);
        long inCycle = elapsed % cycleMillis;

        boolean isWork = inCycle < workMillis;
        long remaining = isWork ? (workMillis - inCycle) : (cycleMillis - inCycle);
        float progress = isWork
                ? 1f - (float) remaining / workMillis
                : 1f - (float) remaining / breakMillis;
        int phaseColor = isWork ? workColor : breakColor;

        // Progress ring
        float r = Math.min(b.width(), b.height()) * 0.36f;
        float thickness = r * 0.14f;
        RectF ring = new RectF(b.centerX() - r, b.centerY() - r * 0.9f, b.centerX() + r, b.centerY() - r * 0.9f + 2 * r);

        Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(thickness);
        track.setColor(textColor);
        track.setAlpha(40);
        canvas.drawArc(ring, 0, 360, false, track);

        Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(thickness);
        arc.setStrokeCap(Paint.Cap.ROUND);
        arc.setColor(phaseColor);
        canvas.drawArc(ring, -90, 360 * progress, false, arc);

        Paint time = new Paint(Paint.ANTI_ALIAS_FLAG);
        time.setColor(textColor);
        time.setTextAlign(Paint.Align.CENTER);
        time.setFakeBoldText(true);
        time.setTextSize(r * 0.5f);
        Paint.FontMetrics fm = time.getFontMetrics();
        canvas.drawText(formatRemaining(remaining), ring.centerX(), ring.centerY() - (fm.ascent + fm.descent) / 2f, time);

        Paint phase = new Paint(Paint.ANTI_ALIAS_FLAG);
        phase.setColor(phaseColor);
        phase.setTextAlign(Paint.Align.CENTER);
        phase.setFakeBoldText(true);
        phase.setTextSize(b.height() * 0.11f);
        canvas.drawText(isWork ? "FOCUS" : "BREAK", b.centerX(), b.top + b.height() * 0.12f, phase);

        Paint status = new Paint(Paint.ANTI_ALIAS_FLAG);
        status.setColor(textColor);
        status.setAlpha(140);
        status.setTextAlign(Paint.Align.CENTER);
        status.setTextSize(b.height() * 0.09f);
        canvas.drawText(running ? "Tap to pause" : "Tap to start", b.centerX(), b.bottom - b.height() * 0.06f, status);

        // Reset glyph (bottom-right third — the widget_ctrl_next overlay region taps here).
        float resetR = b.height() * 0.09f;
        Glyphs.drawReset(canvas, b.left + b.width() * 0.87f, b.bottom - b.height() * 0.13f, resetR, phaseColor);
    }

    private String formatRemaining(long millis) {
        long totalSec = (millis + 999) / 1000; // round up so it doesn't show 0:00 a second early
        long m = totalSec / 60;
        long sec = totalSec % 60;
        return String.format(Locale.US, "%02d:%02d", m, sec);
    }
}
