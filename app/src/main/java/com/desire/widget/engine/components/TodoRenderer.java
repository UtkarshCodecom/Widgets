package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.data.UserDataStore;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.FontResolver;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.util.List;
import java.util.Map;

/**
 * Renders the user's to-do list (from {@link UserDataStore}) as a colourful checklist: an accent
 * "title pill", a done/total progress chip, rounded checkbox tiles with a checkmark when done,
 * alternating row tints, and a strikethrough for completed items.
 * props: title, titleColor, textColor, accentColor, doneColor, font.
 */
public class TodoRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "todo";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        int accent = SpecColors.resolve(SpecProps.str(p, "accentColor",
                SpecProps.str(p, "titleColor", "@primary")), ctx.theme, ctx.theme.primary);
        int textColor = SpecColors.resolve(SpecProps.str(p, "textColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        int doneColor = SpecColors.resolve(SpecProps.str(p, "doneColor", "@textSecondary"), ctx.theme, ctx.theme.textSecondary);
        String title = SpecProps.str(p, "title", "TO-DO");
        String font = SpecProps.str(p, "font", ctx.theme.fontFamily);

        List<UserDataStore.Todo> todos = UserDataStore.getTodos(ctx.android, ctx.appWidgetId);
        int total = todos.size();
        int done = 0;
        for (UserDataStore.Todo t : todos) if (t.done) done++;

        // ---- Header: title pill + progress chip ----
        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setTypeface(FontResolver.resolve(font, true));
        float titleSize = b.height() * 0.14f;
        titlePaint.setTextSize(titleSize);
        String titleText = title != null ? title.trim() : "TO-DO";
        float tw = titlePaint.measureText(titleText);
        float pillH = titleSize * 1.55f;
        float pillPadX = pillH * 0.4f;
        RectF pill = new RectF(b.left, b.top, b.left + tw + pillPadX * 2f, b.top + pillH);
        Paint pillBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        pillBg.setColor(accent);
        canvas.drawRoundRect(pill, pillH * 0.35f, pillH * 0.35f, pillBg);
        titlePaint.setColor(onColor(accent));
        Paint.FontMetrics pfm = titlePaint.getFontMetrics();
        canvas.drawText(titleText, pill.left + pillPadX, pill.centerY() - (pfm.ascent + pfm.descent) / 2f, titlePaint);

        if (total > 0) {
            String prog = done + "/" + total;
            Paint chipText = new Paint(Paint.ANTI_ALIAS_FLAG);
            chipText.setTypeface(FontResolver.resolve(font, true));
            chipText.setTextSize(titleSize * 0.82f);
            float cw = chipText.measureText(prog);
            float chipPad = pillH * 0.32f;
            RectF chip = new RectF(b.right - cw - chipPad * 2f, pill.top, b.right, pill.top + pillH);
            Paint chipBg = new Paint(Paint.ANTI_ALIAS_FLAG);
            chipBg.setColor(accent);
            chipBg.setAlpha(40);
            canvas.drawRoundRect(chip, pillH * 0.35f, pillH * 0.35f, chipBg);
            chipText.setColor(accent);
            Paint.FontMetrics cfm = chipText.getFontMetrics();
            canvas.drawText(prog, chip.left + chipPad, chip.centerY() - (cfm.ascent + cfm.descent) / 2f, chipText);
        }

        // ---- Rows ----
        float listTop = pill.bottom + b.height() * 0.05f;
        float rowH = b.height() * 0.175f;
        float textSize = rowH * 0.5f;

        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setTypeface(FontResolver.resolve(font, false));
        text.setTextSize(textSize);

        int max = (int) ((b.bottom - listTop) / rowH);
        if (total == 0) {
            text.setColor(textColor);
            text.setAlpha(120);
            Paint.FontMetrics fm = text.getFontMetrics();
            canvas.drawText("Tap to add a task…", b.left, listTop + rowH * 0.5f - (fm.ascent + fm.descent) / 2f, text);
            return;
        }

        for (int i = 0; i < total && i < max; i++) {
            UserDataStore.Todo t = todos.get(i);
            float rowTop = listTop + i * rowH;
            float cy = rowTop + rowH * 0.5f;

            // Alternating row tint.
            if (i % 2 == 1) {
                Paint band = new Paint(Paint.ANTI_ALIAS_FLAG);
                band.setColor(accent);
                band.setAlpha(14);
                float inset = rowH * 0.08f;
                canvas.drawRoundRect(new RectF(b.left, rowTop + inset, b.right, rowTop + rowH - inset),
                        rowH * 0.22f, rowH * 0.22f, band);
            }

            // Checkbox tile.
            float box = rowH * 0.44f;
            float bx = b.left + box * 0.55f;
            RectF boxRect = new RectF(bx - box / 2f, cy - box / 2f, bx + box / 2f, cy + box / 2f);
            float boxR = box * 0.28f;
            if (t.done) {
                Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
                fill.setColor(accent);
                canvas.drawRoundRect(boxRect, boxR, boxR, fill);
                // Checkmark.
                Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
                tick.setStyle(Paint.Style.STROKE);
                tick.setStrokeCap(Paint.Cap.ROUND);
                tick.setStrokeJoin(Paint.Join.ROUND);
                tick.setStrokeWidth(Math.max(2f, box * 0.14f));
                tick.setColor(onColor(accent));
                Path path = new Path();
                path.moveTo(boxRect.left + box * 0.24f, cy + box * 0.02f);
                path.lineTo(boxRect.left + box * 0.42f, cy + box * 0.2f);
                path.lineTo(boxRect.left + box * 0.78f, cy - box * 0.22f);
                canvas.drawPath(path, tick);
            } else {
                Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
                outline.setStyle(Paint.Style.STROKE);
                outline.setStrokeWidth(Math.max(2f, box * 0.12f));
                outline.setColor(accent);
                outline.setAlpha(160);
                canvas.drawRoundRect(boxRect, boxR, boxR, outline);
            }

            // Label.
            text.setColor(t.done ? doneColor : textColor);
            text.setAlpha(t.done ? 170 : 255);
            text.setStrikeThruText(t.done);
            String label = t.text != null ? t.text : "";
            float textLeft = boxRect.right + box * 0.6f;
            Paint.FontMetrics fm = text.getFontMetrics();
            canvas.drawText(ellipsize(label, text, b.right - textLeft), textLeft, cy - (fm.ascent + fm.descent) / 2f, text);
        }
    }

    private int onColor(int bg) {
        double lum = (0.299 * Color.red(bg) + 0.587 * Color.green(bg) + 0.114 * Color.blue(bg)) / 255.0;
        return lum > 0.6 ? 0xFF1A1A1A : 0xFFFFFFFF;
    }

    private String ellipsize(String s, Paint paint, float maxWidth) {
        if (maxWidth <= 0) return "";
        if (paint.measureText(s) <= maxWidth) return s;
        while (s.length() > 1 && paint.measureText(s + "…") > maxWidth) s = s.substring(0, s.length() - 1);
        return s + "…";
    }
}
