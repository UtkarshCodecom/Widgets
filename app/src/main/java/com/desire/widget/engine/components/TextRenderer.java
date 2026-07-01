package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.FontResolver;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.util.Map;

/**
 * Multi-line, wrapping, vertically-centered text via StaticLayout (production text layout, not a
 * single drawText). props: text, color, fontSize (<=1 = fraction of bounds height, else px),
 * bold, align (left|center|right), maxLines.
 */
public class TextRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "text";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        String text = SpecProps.str(p, "text", "");
        if (text == null) text = "";
        int color = SpecColors.resolve(SpecProps.str(p, "color", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        float fs = SpecProps.f(p, "fontSize", 0.3f);
        float px = fs <= 1f ? fs * b.height() : fs;
        boolean bold = SpecProps.b(p, "bold", false);
        String align = SpecProps.str(p, "align", "left");
        int maxLines = Math.max(1, SpecProps.i(p, "maxLines", 2));

        TextPaint tp = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        tp.setColor(color);
        tp.setTextSize(Math.max(1f, px));
        String font = SpecProps.str(p, "font", ctx.theme.fontFamily);
        tp.setTypeface(FontResolver.resolve(font, bold));

        Layout.Alignment la = "center".equals(align) ? Layout.Alignment.ALIGN_CENTER
                : "right".equals(align) ? Layout.Alignment.ALIGN_OPPOSITE
                : Layout.Alignment.ALIGN_NORMAL;

        int width = Math.max(1, (int) b.width());
        StaticLayout layout = StaticLayout.Builder
                .obtain(text, 0, text.length(), tp, width)
                .setAlignment(la)
                .setMaxLines(maxLines)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setIncludePad(false)
                .build();

        float ty = b.top + Math.max(0f, (b.height() - layout.getHeight()) / 2f);
        canvas.save();
        canvas.translate(b.left, ty);
        layout.draw(canvas);
        canvas.restore();
    }
}
