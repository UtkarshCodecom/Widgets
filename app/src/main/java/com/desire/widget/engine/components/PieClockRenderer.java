package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.format.DateFormat;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

/**
 * Sector clock: the hour is a filled pie wedge sweeping from 12 o'clock, the minutes are a thick
 * outer arc, and a centre disc shows the digital time. Reads at a glance like a progress dial.
 *
 * <p>props: hourColor, minuteColor, trackColor, centerColor, textColor, showDigits.
 */
public class PieClockRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "pie_clock";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        int hourColor = SpecColors.resolve(SpecProps.str(p, "hourColor", "@primary"), ctx.theme, ctx.theme.primary);
        int minColor = SpecColors.resolve(SpecProps.str(p, "minuteColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        int trackColor = SpecColors.resolve(SpecProps.str(p, "trackColor", "#22FFFFFF"), ctx.theme, 0x22FFFFFF);
        int centerColor = SpecColors.resolve(SpecProps.str(p, "centerColor", "@surface"), ctx.theme, ctx.theme.surface);
        int textColor = SpecColors.resolve(SpecProps.str(p, "textColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ctx.nowMillis);
        float hours = cal.get(Calendar.HOUR) + cal.get(Calendar.MINUTE) / 60f;
        float minutes = cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f;

        float size = Math.min(b.width(), b.height());
        float cx = b.centerX(), cy = b.centerY();
        float outerR = size / 2f * 0.96f;
        float ringW = size * 0.09f;
        float ringR = outerR - ringW / 2f;
        float pieR = ringR - ringW * 0.9f;

        // Minute ring: track + arc.
        RectF ringOval = new RectF(cx - ringR, cy - ringR, cx + ringR, cy + ringR);
        Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(ringW);
        track.setColor(trackColor);
        canvas.drawArc(ringOval, 0, 360, false, track);
        Paint minArc = new Paint(Paint.ANTI_ALIAS_FLAG);
        minArc.setStyle(Paint.Style.STROKE);
        minArc.setStrokeWidth(ringW);
        minArc.setStrokeCap(Paint.Cap.ROUND);
        minArc.setColor(minColor);
        canvas.drawArc(ringOval, -90f, Math.max(1f, minutes / 60f * 360f), false, minArc);

        // Hour pie: faint full disc + solid wedge.
        RectF pieOval = new RectF(cx - pieR, cy - pieR, cx + pieR, cy + pieR);
        Paint pieTrack = new Paint(Paint.ANTI_ALIAS_FLAG);
        pieTrack.setColor(trackColor);
        canvas.drawOval(pieOval, pieTrack);
        Paint wedge = new Paint(Paint.ANTI_ALIAS_FLAG);
        wedge.setColor(hourColor);
        canvas.drawArc(pieOval, -90f, Math.max(1f, hours / 12f * 360f), true, wedge);

        if (SpecProps.b(p, "showDigits", true)) {
            float discR = pieR * 0.58f;
            Paint disc = new Paint(Paint.ANTI_ALIAS_FLAG);
            disc.setColor(centerColor);
            canvas.drawCircle(cx, cy, discR, disc);

            boolean use24 = DateFormat.is24HourFormat(ctx.android);
            int h = use24 ? cal.get(Calendar.HOUR_OF_DAY) : cal.get(Calendar.HOUR);
            if (!use24 && h == 0) h = 12;
            String time = String.format(Locale.US, use24 ? "%02d:%02d" : "%d:%02d", h, cal.get(Calendar.MINUTE));
            Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
            txt.setColor(textColor);
            txt.setTextAlign(Paint.Align.CENTER);
            txt.setFakeBoldText(true);
            txt.setTextSize(discR * 0.62f);
            float w = txt.measureText(time);
            if (w > discR * 1.7f) txt.setTextSize(txt.getTextSize() * discR * 1.7f / w);
            Paint.FontMetrics fm = txt.getFontMetrics();
            canvas.drawText(time, cx, cy - (fm.ascent + fm.descent) / 2f, txt);
        }
    }
}
