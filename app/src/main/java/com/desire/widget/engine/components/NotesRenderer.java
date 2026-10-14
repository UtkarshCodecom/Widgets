package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;

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
 * lines. props: title, titleColor, textColor, accentColor, font, paper (default paper colour),
 * inset (padding as a fraction of the short side, for full-bleed frames), showEdited.
 * The user's paper colour and text size (Notes editor) are read per widget from UserDataStore.
 */
public class NotesRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "notes";
    }

    @Override
    public void render(Canvas canvas, RectF frame, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        int accent = SpecColors.resolve(SpecProps.str(p, "accentColor",
                SpecProps.str(p, "titleColor", "@primary")), ctx.theme, ctx.theme.primary);
        int textColor = SpecColors.resolve(SpecProps.str(p, "textColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        String title = SpecProps.str(p, "title", "NOTES");
        String font = SpecProps.str(p, "font", ctx.theme.fontFamily);

        // Paper: the colour picked in the editor wins, then the spec's default paper. It fills the
        // whole frame (full-bleed specs use frame 0,0,1,1 + "inset"; the engine clips the corners).
        String paperHex = UserDataStore.getNotePaper(ctx.android, ctx.appWidgetId);
        if (paperHex == null || paperHex.isEmpty()) paperHex = SpecProps.str(p, "paper", "");
        if (paperHex != null && !paperHex.isEmpty()) {
            int paper = SpecColors.resolve(paperHex, ctx.theme, 0xFFFFE58F);
            Paint pp = new Paint(Paint.ANTI_ALIAS_FLAG);
            pp.setColor(paper);
            canvas.drawRect(frame, pp);
            textColor = onColor(paper) == 0xFFFFFFFF ? 0xFFF1F2F4 : 0xFF2B2722;
            if (onColor(paper) != 0xFFFFFFFF) accent = shade(paper, 0.42f); // deeper tone of the paper
        }
        float insetPx = SpecProps.f(p, "inset", 0f) * Math.min(frame.width(), frame.height());
        RectF b = new RectF(frame.left + insetPx, frame.top + insetPx * 0.9f,
                frame.right - insetPx, frame.bottom - insetPx * 0.8f);

        String notes = UserDataStore.getNotes(ctx.android, ctx.appWidgetId);
        boolean empty = notes == null || notes.trim().isEmpty();
        if (empty) notes = "Tap to write a note…";
        float sizeScale = UserDataStore.NOTE_SIZES[UserDataStore.getNoteSize(ctx.android, ctx.appWidgetId)];
        // Height of a 2x2 worth of content, so 4x4 notes don't get giant text.
        float unit = Math.min(b.height(), b.width() * 1.05f);

        float pad = unit * 0.06f;

        // Folded dog-ear corner (top-right) for the sticky-note feel.
        float ear = Math.min(frame.width(), frame.height()) * 0.16f;
        Paint earPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        earPaint.setColor(accent);
        earPaint.setAlpha(45);
        Path earPath = new Path();
        earPath.moveTo(frame.right - ear, frame.top);
        earPath.lineTo(frame.right, frame.top);
        earPath.lineTo(frame.right, frame.top + ear);
        earPath.close();
        canvas.drawPath(earPath, earPaint);

        // Title pill: rounded accent chip with the title in a readable on-colour.
        TextPaint titlePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        titlePaint.setTypeface(FontResolver.resolve(font, true));
        float titleSize = unit * 0.13f;
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

        // "Edited 5m ago" footer (placed widgets only).
        long edited = UserDataStore.getNotesUpdatedAt(ctx.android, ctx.appWidgetId);
        float bottom = b.bottom;
        if (!empty && edited > 0 && SpecProps.b(p, "showEdited", true)) {
            Paint foot = new Paint(Paint.ANTI_ALIAS_FLAG);
            foot.setTypeface(FontResolver.resolve(font, false));
            foot.setTextSize(unit * 0.075f);
            foot.setColor(textColor);
            foot.setAlpha(120);
            foot.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("Edited " + ago(edited, ctx.nowMillis), b.right, b.bottom, foot);
            bottom = b.bottom - unit * 0.11f;
        }

        // Body area below the pill.
        float bodyTop = pill.bottom + pad;
        TextPaint body = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        body.setColor(textColor);
        if (empty) body.setAlpha(120);
        body.setTypeface(FontResolver.resolve(font, false));
        float bodySize = unit * 0.115f * sizeScale;
        body.setTextSize(bodySize);
        float lineH = bodySize * 1.32f;

        // Faint ruled lines behind the body.
        Paint rule = new Paint(Paint.ANTI_ALIAS_FLAG);
        rule.setColor(accent);
        rule.setAlpha(26);
        rule.setStrokeWidth(Math.max(1f, unit * 0.006f));
        for (float ly = bodyTop + lineH; ly < bottom - lineH * 0.2f; ly += lineH) {
            canvas.drawLine(b.left, ly, b.right, ly, rule);
        }

        CharSequence text = empty ? notes : styled(notes);
        int width = Math.max(1, (int) b.width());
        int maxLines = Math.max(1, (int) ((bottom - bodyTop) / lineH));
        StaticLayout layout = StaticLayout.Builder
                .obtain(text, 0, text.length(), body, width)
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

    /**
     * Light markup: "- " / "* " lines become bullets, "[ ] " / "[x] " become checkboxes (done ones
     * dimmed and struck through), and a short first line of a multi-line note is bold, like a title.
     */
    static CharSequence styled(String notes) {
        String[] lines = notes.split("\n", -1);
        SpannableStringBuilder sb = new SpannableStringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int start = sb.length();
            String trimmed = line.trim();
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                sb.append("•  ").append(trimmed.substring(2));
            } else if (trimmed.startsWith("[ ] ") || trimmed.startsWith("[] ")) {
                sb.append("☐  ").append(trimmed.substring(trimmed.indexOf(']') + 2));
            } else if (trimmed.toLowerCase(java.util.Locale.ROOT).startsWith("[x] ")) {
                sb.append("☑  ").append(trimmed.substring(4));
                sb.setSpan(new StrikethroughSpan(), start + 3, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new android.text.style.CharacterStyle() {
                    @Override
                    public void updateDrawState(TextPaint tp) {
                        tp.setAlpha(Math.round(tp.getAlpha() * 0.55f));
                    }
                }, start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                sb.append(line);
            }
            if (i == 0 && lines.length > 1 && trimmed.length() > 0 && trimmed.length() <= 40) {
                sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (i < lines.length - 1) sb.append('\n');
        }
        return sb;
    }

    private static String ago(long then, long now) {
        long min = Math.max(0, (now - then) / 60_000L);
        if (min < 1) return "just now";
        if (min < 60) return min + "m ago";
        long h = min / 60;
        if (h < 24) return h + "h ago";
        if (h < 48) return "yesterday";
        return new java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault()).format(new java.util.Date(then));
    }

    private static int shade(int c, float f) {
        return Color.rgb(Math.round(Color.red(c) * f), Math.round(Color.green(c) * f), Math.round(Color.blue(c) * f));
    }

    /** Black or white, whichever is more readable on the given background colour. */
    private int onColor(int bg) {
        double lum = (0.299 * Color.red(bg) + 0.587 * Color.green(bg) + 0.114 * Color.blue(bg)) / 255.0;
        return lum > 0.6 ? 0xFF1A1A1A : 0xFFFFFFFF;
    }
}
