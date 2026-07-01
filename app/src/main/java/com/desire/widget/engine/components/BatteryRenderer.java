package com.desire.widget.engine.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.BatteryManager;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.util.Map;

/**
 * Live battery gauge: reads the real charge level from BatteryManager and draws a battery shell,
 * cap, fill, and percent label. Turns red below 20%. props: bodyColor, fillColor, lowColor,
 * textColor, showPercent, previewLevel (used when the system level is unavailable, e.g. emulator).
 */
public class BatteryRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "battery";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        int level = batteryLevel(ctx.android);
        if (level < 0) level = SpecProps.i(p, "previewLevel", 72);
        float frac = Math.max(0f, Math.min(1f, level / 100f));

        int body = SpecColors.resolve(SpecProps.str(p, "bodyColor", "@textSecondary"), ctx.theme, ctx.theme.textSecondary);
        int fillLow = SpecColors.resolve(SpecProps.str(p, "lowColor", "#FF5252"), ctx.theme, 0xFFFF5252);
        int fillCol = SpecColors.resolve(SpecProps.str(p, "fillColor", "@primary"), ctx.theme, ctx.theme.primary);
        int used = frac <= 0.2f ? fillLow : fillCol;

        float capW = b.width() * 0.07f;
        RectF shell = new RectF(b.left, b.top + b.height() * 0.2f, b.right - capW, b.bottom - b.height() * 0.2f);
        float r = shell.height() * 0.18f;

        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(Math.max(2f, shell.height() * 0.07f));
        stroke.setColor(body);
        canvas.drawRoundRect(shell, r, r, stroke);

        Paint capPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        capPaint.setColor(body);
        RectF cap = new RectF(shell.right + capW * 0.2f, b.centerY() - shell.height() * 0.18f,
                shell.right + capW, b.centerY() + shell.height() * 0.18f);
        canvas.drawRoundRect(cap, capW * 0.3f, capW * 0.3f, capPaint);

        float pad = stroke.getStrokeWidth() * 1.4f;
        RectF inner = new RectF(shell.left + pad, shell.top + pad, shell.right - pad, shell.bottom - pad);
        RectF fillRect = new RectF(inner.left, inner.top, inner.left + inner.width() * frac, inner.bottom);
        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(used);
        canvas.drawRoundRect(fillRect, r * 0.6f, r * 0.6f, fillPaint);

        if (SpecProps.b(p, "showPercent", true)) {
            int tc = SpecColors.resolve(SpecProps.str(p, "textColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
            Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
            txt.setColor(tc);
            txt.setTextAlign(Paint.Align.CENTER);
            txt.setFakeBoldText(true);
            txt.setTextSize(shell.height() * 0.5f);
            Paint.FontMetrics fm = txt.getFontMetrics();
            canvas.drawText(level + "%", shell.centerX(), shell.centerY() - (fm.ascent + fm.descent) / 2f, txt);
        }
    }

    private int batteryLevel(Context ctx) {
        try {
            BatteryManager bm = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
            if (bm != null) {
                int l = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                if (l >= 0 && l <= 100) return l;
            }
        } catch (Exception ignored) {
        }
        return -1;
    }
}
