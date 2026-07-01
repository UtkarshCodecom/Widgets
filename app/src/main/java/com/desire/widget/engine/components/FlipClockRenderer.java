package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.FontResolver;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * A flip-clock: two rounded "cards" (hours, minutes) each split by a seam line, with large digits.
 * props: cardColor, textColor, font, format24 (bool). Derived from {@code ctx.nowMillis}.
 */
public class FlipClockRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "flip_clock";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        int card = SpecColors.resolve(SpecProps.str(p, "cardColor", "#1C1C24"), ctx.theme, 0xFF1C1C24);
        int textColor = SpecColors.resolve(SpecProps.str(p, "textColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        boolean h24 = SpecProps.b(p, "format24", true);
        String font = SpecProps.str(p, "font", ctx.theme.fontFamily);

        String hh = new SimpleDateFormat(h24 ? "HH" : "hh", Locale.getDefault()).format(new Date(ctx.nowMillis));
        String mm = new SimpleDateFormat("mm", Locale.getDefault()).format(new Date(ctx.nowMillis));

        float gap = b.width() * 0.06f;
        float cardW = (b.width() - gap) / 2f;
        RectF left = new RectF(b.left, b.top, b.left + cardW, b.bottom);
        RectF right = new RectF(b.right - cardW, b.top, b.right, b.bottom);

        drawCard(canvas, left, card, textColor, hh, font);
        drawCard(canvas, right, card, textColor, mm, font);
    }

    private void drawCard(Canvas canvas, RectF r, int card, int textColor, String digits, String font) {
        float radius = r.width() * 0.14f;
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(card);
        canvas.drawRoundRect(r, radius, radius, bg);

        // seam line across the middle
        Paint seam = new Paint(Paint.ANTI_ALIAS_FLAG);
        seam.setColor(0x33000000);
        seam.setStrokeWidth(Math.max(1f, r.height() * 0.012f));
        canvas.drawLine(r.left, r.centerY(), r.right, r.centerY(), seam);

        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(textColor);
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setTypeface(FontResolver.resolve(font, true));
        tp.setTextSize(r.height() * 0.62f);
        Paint.FontMetrics fm = tp.getFontMetrics();
        canvas.drawText(digits, r.centerX(), r.centerY() - (fm.ascent + fm.descent) / 2f, tp);
    }
}
