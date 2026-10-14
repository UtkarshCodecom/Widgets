package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.data.UserDataStore;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.FontResolver;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.util.Map;

/**
 * Renders the user's saved notes (from {@link UserDataStore}) as a colourful sticky-note style card:
 * a filled accent "title pill", a folded dog-ear corner, and the wrapped note body over faint ruled
 * lines. props: title, titleColor, textColor, accentColor, font.
 */
public class NotesRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "notes";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        int accent = SpecColors.resolve(SpecProps.str(p, "accentColor",
                SpecProps.str(p, "titleColor", "@primary")), ctx.theme, ctx.theme.primary);
        int textColor = SpecColors.resolve(SpecProps.str(p, "textColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        String title = SpecProps.str(p, "title", "NOTES");
        String font = SpecProps.str(p, "font", ctx.theme.fontFamily);

        String notes = UserDataStore.getNotes(ctx.android, ctx.appWidgetId);
        boolean empty = notes == null || notes.trim().isEmpty();
        if (empty) notes = "Tap to add a note…";

        float pad = b.height() * 0.06f;

        // Folded dog-ear corner (top-right) for the sticky-note feel.
        float ear = Math.min(b.width(), b.height()) * 0.16f;
        Paint earPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        earPaint.setColor(accent);
        earPaint.setAlpha(45);
        Path earPath = new Path();
        earPath.moveTo(b.right - ear, b.top);
        earPath.lineTo(b.right, b.top);
        earPath.lineTo(b.right, b.top + ear);
        earPath.close();
        canvas.drawPath(earPath, earPaint);

        // Title pill: rounded accent chip with the title in a readable on-colour.
        TextPaint titlePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        titlePaint.setTypeface(FontResolver.resolve(font, true));
        float titleSize = b.height() * 0.15f;
        titlePaint.setTextSize(titleSize);
        String titleText = title != null ? title.trim() : "NOTES";
        float tw = titlePaint.measureText(titleText);
        float pillH = titleSize * 1.6f;
        float pillPadX = pillH * 0.42f;
        RectF pill = new RectF(b.left, b.top, b.left + tw + pillPadX * 2f, b.top + pillH);
        Paint pillBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        pillBg.setColor(accent);
        canvas.drawRoundRect(pill, pillH * 0.35f, pillH * 0.35f, pillBg);
        titlePaint.setColor(onColor(accent));
        Paint.FontMetrics tfm = titlePaint.getFontMetrics();
        canvas.drawText(titleText, pill.left + pillPadX, pill.centerY() - (tfm.ascent + tfm.descent) / 2f, titlePaint);

        // Body area below the pill.
        float bodyTop = pill.bottom + pad;
        TextPaint body = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        body.setColor(textColor);
        if (empty) body.setAlpha(120);
        body.setTypeface(FontResolver.resolve(font, false));
        float bodySize = b.height() * 0.125f;
        body.setTextSize(bodySize);
        float lineH = bodySize * 1.32f;

        // Faint ruled lines behind the body.
        Paint rule = new Paint(Paint.ANTI_ALIAS_FLAG);
        rule.setColor(accent);
        rule.setAlpha(22);
        rule.setStrokeWidth(Math.max(1f, b.height() * 0.006f));
        for (float ly = bodyTop + lineH; ly < b.bottom - lineH * 0.2f; ly += lineH) {
            canvas.drawLine(b.left, ly, b.right, ly, rule);
        }

        int width = Math.max(1, (int) b.width());
        int maxLines = Math.max(1, (int) ((b.bottom - bodyTop) / lineH));
        StaticLayout layout = StaticLayout.Builder
                .obtain(notes, 0, notes.length(), body, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setMaxLines(maxLines)
                .setLineSpacing(lineH - bodySize * 1.15f, 1f)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setIncludePad(false)
                .build();
        canvas.save();
        canvas.translate(b.left, bodyTop);
        layout.draw(canvas);
        canvas.restore();
    }

    /** Black or white, whichever is more readable on the given background colour. */
    private int onColor(int bg) {
        double lum = (0.299 * Color.red(bg) + 0.587 * Color.green(bg) + 0.114 * Color.blue(bg)) / 255.0;
        return lum > 0.6 ? 0xFF1A1A1A : 0xFFFFFFFF;
    }
}
