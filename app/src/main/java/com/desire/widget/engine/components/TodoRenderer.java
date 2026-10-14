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
 * props: title, titleColor, textColor, accentColor, doneColor, font, listRows.
 *
 * <p><b>Tappable list mode</b> ({@code listRows} = 5 or 9, frame = the whole widget): the widget
 * is divided into {@link #STRIPS} equal horizontal strips — the same strips as the transparent tap
 * overlay in {@code widget_layout_list.xml} — and the header and every row are laid out on strip
 * boundaries ({@link #layoutFor}), so EngineRenderer can wire each row's strips to tick off that
 * exact task right on the home screen.
 */
public class TodoRenderer implements ComponentRenderer {
    /** Number of equal tap strips in widget_layout_list.xml. */
    public static final int STRIPS = 20;

    /**
     * {headerStrips, rowStrips} for a list of {@code rows} rows. 5 rows suit 2x2/4x2 (header 4
     * strips, rows 3 each, 1 strip of bottom padding); 9 rows suit 4x4 (header 2, rows 2 each).
     */
    public static int[] layoutFor(int rows) {
        if (rows >= 9) return new int[]{2, 2};
        return new int[]{4, 3};
    }

    /** Rows actually showing a task (the last row becomes "+N more" when the list overflows). */
    public static int visibleTaskRows(int rows, int total) {
        return total > rows ? rows - 1 : Math.min(rows, total);
    }

    @Override
    public String type() {
        return "todo";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        int listRows = (int) SpecProps.f(s.props, "listRows", 0f);
        if (listRows > 0) {
            renderList(canvas, b, s, ctx, listRows);
            return;
        }
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

    private void renderList(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx, int rows) {
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

        int[] lay = layoutFor(rows);
        float strip = b.height() / STRIPS;
        float padX = Math.min(b.width(), b.height()) * 0.075f;
        float left = b.left + padX, right = b.right - padX;

        // ---- Header: title pill · progress chip, centred in the header strips ----
        float headerH = lay[0] * strip;
        float pillH = Math.min(headerH * 0.62f, strip * 3.2f);
        float headerCy = b.top + headerH * 0.56f;
        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setTypeface(FontResolver.resolve(font, true));
        titlePaint.setTextSize(pillH * 0.56f);
        String titleText = title != null ? title.trim() : "TO-DO";
        float padPill = pillH * 0.42f;
        RectF pill = new RectF(left, headerCy - pillH / 2f,
                left + titlePaint.measureText(titleText) + padPill * 2f, headerCy + pillH / 2f);
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(accent);
        canvas.drawRoundRect(pill, pillH * 0.38f, pillH * 0.38f, fill);
        titlePaint.setColor(onColor(accent));
        Paint.FontMetrics tfm = titlePaint.getFontMetrics();
        canvas.drawText(titleText, pill.left + padPill, pill.centerY() - (tfm.ascent + tfm.descent) / 2f, titlePaint);

        if (total > 0) {
            String prog = done + "/" + total;
            Paint chipText = new Paint(Paint.ANTI_ALIAS_FLAG);
            chipText.setTypeface(FontResolver.resolve(font, true));
            chipText.setTextSize(pillH * 0.5f);
            float cw = chipText.measureText(prog);
            RectF chip = new RectF(right - cw - padPill * 1.6f, pill.top, right, pill.bottom);
            Paint chipBg = new Paint(Paint.ANTI_ALIAS_FLAG);
            chipBg.setColor(accent);
            chipBg.setAlpha(42);
            canvas.drawRoundRect(chip, pillH * 0.38f, pillH * 0.38f, chipBg);
            chipText.setColor(accent);
            Paint.FontMetrics cfm = chipText.getFontMetrics();
            canvas.drawText(prog, chip.left + padPill * 0.8f, chip.centerY() - (cfm.ascent + cfm.descent) / 2f, chipText);

            // Slim progress line between the header and the list.
            float barY = b.top + headerH - strip * 0.35f;
            float barH = Math.max(2f, strip * 0.16f);
            Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
            track.setColor(accent);
            track.setAlpha(36);
            canvas.drawRoundRect(new RectF(left, barY, right, barY + barH), barH, barH, track);
            if (done > 0) {
                Paint prog2 = new Paint(Paint.ANTI_ALIAS_FLAG);
                prog2.setColor(accent);
                canvas.drawRoundRect(new RectF(left, barY, left + (right - left) * done / total, barY + barH),
                        barH, barH, prog2);
            }
        }

        // ---- Rows on strip boundaries ----
        float rowH = lay[1] * strip;
        float listTop = b.top + headerH;
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setTypeface(FontResolver.resolve(font, false));
        text.setTextSize(Math.min(rowH * 0.42f, strip * 1.35f));
        Paint.FontMetrics fm = text.getFontMetrics();

        if (total == 0) {
            text.setColor(textColor);
            text.setAlpha(130);
            drawRowText(canvas, "Tap to add your first task", left, listTop + rowH * 0.5f, right - left, text, fm);
            return;
        }

        int shown = visibleTaskRows(rows, total);
        for (int i = 0; i < shown; i++) {
            UserDataStore.Todo t = todos.get(i);
            float rowTop = listTop + i * rowH;
            float cy = rowTop + rowH * 0.5f;
            if (i % 2 == 0) {
                Paint band = new Paint(Paint.ANTI_ALIAS_FLAG);
                band.setColor(accent);
                band.setAlpha(16);
                float inset = rowH * 0.1f;
                canvas.drawRoundRect(new RectF(left - padX * 0.45f, rowTop + inset, right + padX * 0.45f,
                        rowTop + rowH - inset), rowH * 0.3f, rowH * 0.3f, band);
            }
            float box = Math.min(rowH * 0.5f, strip * 1.5f);
            RectF boxRect = new RectF(left, cy - box / 2f, left + box, cy + box / 2f);
            drawCheckbox(canvas, boxRect, t.done, accent);

            text.setColor(t.done ? doneColor : textColor);
            text.setAlpha(t.done ? 165 : 255);
            text.setStrikeThruText(t.done);
            float textLeft = boxRect.right + box * 0.55f;
            drawRowText(canvas, t.text != null ? t.text : "", textLeft, cy, right - textLeft, text, fm);
        }
        if (shown < total) {
            text.setStrikeThruText(false);
            text.setColor(accent);
            text.setAlpha(255);
            drawRowText(canvas, "+" + (total - shown) + " more · tap to open", left,
                    listTop + shown * rowH + rowH * 0.5f, right - left, text, fm);
        }
    }

    private void drawCheckbox(Canvas canvas, RectF boxRect, boolean checked, int accent) {
        float box = boxRect.width();
        float r = box * 0.3f;
        if (checked) {
            Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
            fill.setColor(accent);
            canvas.drawRoundRect(boxRect, r, r, fill);
            Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
            tick.setStyle(Paint.Style.STROKE);
            tick.setStrokeCap(Paint.Cap.ROUND);
            tick.setStrokeJoin(Paint.Join.ROUND);
            tick.setStrokeWidth(Math.max(2f, box * 0.14f));
            tick.setColor(onColor(accent));
            float cy = boxRect.centerY();
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
            outline.setAlpha(170);
            RectF inset = new RectF(boxRect);
            float half = outline.getStrokeWidth() / 2f;
            inset.inset(half, half);
            canvas.drawRoundRect(inset, r, r, outline);
        }
    }

    private void drawRowText(Canvas canvas, String s, float x, float cy, float maxW, Paint paint, Paint.FontMetrics fm) {
        canvas.drawText(ellipsize(s, paint, maxW), x, cy - (fm.ascent + fm.descent) / 2f, paint);
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
