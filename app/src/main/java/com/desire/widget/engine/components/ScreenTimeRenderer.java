package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.data.ScreenTimeStore;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Today's screen time: the total plus the top apps as icon + bar rows. Needs Usage access; until
 * it's granted a placed widget explains how to enable it (the tap opens the settings page — see
 * WidgetTapActivity), while gallery previews show sample data.
 *
 * <p>props: rows (max app rows), accentColor, barColor, trackColor, textColor, subColor.
 */
public class ScreenTimeRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "screen_time";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        int accent = SpecColors.resolve(SpecProps.str(p, "accentColor", "@primary"), ctx.theme, ctx.theme.primary);
        int barColor = SpecColors.resolve(SpecProps.str(p, "barColor", "@primary"), ctx.theme, ctx.theme.primary);
        int track = SpecColors.resolve(SpecProps.str(p, "trackColor", "#1FFFFFFF"), ctx.theme, 0x1FFFFFFF);
        int textColor = SpecColors.resolve(SpecProps.str(p, "textColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        int subColor = SpecColors.resolve(SpecProps.str(p, "subColor", "@textSecondary"), ctx.theme, ctx.theme.textSecondary);
        int maxRows = Math.max(1, SpecProps.i(p, "rows", 3));

        ScreenTimeStore.Result r = ScreenTimeStore.today(ctx.android);
        long total;
        List<String> names = new ArrayList<>();
        List<Long> times = new ArrayList<>();
        List<String> pkgs = new ArrayList<>();
        if (r != null) {
            total = r.totalMillis;
            for (ScreenTimeStore.AppUsage u : r.top) {
                names.add(u.label);
                times.add(u.millis);
                pkgs.add(u.packageName);
            }
        } else if (ctx.appWidgetId < 0) {
            total = (3 * 60 + 42) * 60_000L; // gallery sample
            String[] n = {"Instagram", "YouTube", "WhatsApp"};
            long[] t = {84, 61, 38};
            for (int i = 0; i < n.length; i++) {
                names.add(n[i]);
                times.add(t[i] * 60_000L);
                pkgs.add(null);
            }
        } else {
            drawNeedsAccess(canvas, b, accent, textColor, subColor);
            return;
        }

        // Header: big total + caption.
        Paint big = paint(textColor, b.height() * 0.20f, true, Paint.Align.LEFT);
        canvas.drawText(ScreenTimeStore.format(total), b.left, b.top + b.height() * 0.19f, big);
        Paint cap = paint(accent, b.height() * 0.08f, true, Paint.Align.LEFT);
        canvas.drawText("SCREEN TIME TODAY", b.left, b.top + b.height() * 0.30f, cap);

        int rows = Math.min(maxRows, names.size());
        if (rows == 0) {
            Paint none = paint(subColor, b.height() * 0.09f, false, Paint.Align.LEFT);
            canvas.drawText("No app usage yet today", b.left, b.top + b.height() * 0.50f, none);
            return;
        }
        float listTop = b.top + b.height() * 0.38f;
        float rowH = (b.bottom - listTop) / rows;
        long max = Math.max(1, times.get(0));
        for (int i = 0; i < rows; i++) {
            float cy = listTop + rowH * (i + 0.5f);
            float iconS = Math.min(rowH * 0.72f, b.height() * 0.13f);
            drawAppIcon(canvas, ctx, pkgs.get(i), names.get(i), b.left, cy - iconS / 2f, iconS, barColor);

            float x = b.left + iconS * 1.35f;
            Paint name = paint(textColor, rowH * 0.30f, false, Paint.Align.LEFT);
            canvas.drawText(ellipsize(names.get(i), name, b.right - x - b.width() * 0.22f), x, cy - rowH * 0.06f, name);
            Paint dur = paint(subColor, rowH * 0.28f, true, Paint.Align.RIGHT);
            canvas.drawText(ScreenTimeStore.format(times.get(i)), b.right, cy - rowH * 0.06f, dur);

            float barTop = cy + rowH * 0.08f;
            float barH = Math.max(3f, rowH * 0.12f);
            Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
            tp.setColor(track);
            canvas.drawRoundRect(new RectF(x, barTop, b.right, barTop + barH), barH, barH, tp);
            Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG);
            fp.setColor(barColor);
            fp.setAlpha(255 - i * 55);
            float w = (b.right - x) * times.get(i) / (float) max;
            canvas.drawRoundRect(new RectF(x, barTop, x + Math.max(barH, w), barTop + barH), barH, barH, fp);
        }
    }

    private void drawNeedsAccess(Canvas canvas, RectF b, int accent, int textColor, int subColor) {
        Paint t = paint(textColor, b.height() * 0.13f, true, Paint.Align.CENTER);
        canvas.drawText("Screen time", b.centerX(), b.centerY() - b.height() * 0.06f, t);
        Paint s = paint(accent, b.height() * 0.08f, true, Paint.Align.CENTER);
        canvas.drawText("Tap to allow usage access", b.centerX(), b.centerY() + b.height() * 0.10f, s);
        Paint n = paint(subColor, b.height() * 0.065f, false, Paint.Align.CENTER);
        canvas.drawText("Stays on your device", b.centerX(), b.centerY() + b.height() * 0.22f, n);
    }

    private void drawAppIcon(Canvas canvas, RenderContext ctx, String pkg, String name, float left,
                             float top, float size, int fallback) {
        Drawable icon = null;
        if (pkg != null) {
            try {
                icon = ctx.android.getPackageManager().getApplicationIcon(pkg);
            } catch (Exception ignored) {}
        }
        if (icon != null) {
            icon.setBounds((int) left, (int) top, (int) (left + size), (int) (top + size));
            icon.draw(canvas);
            return;
        }
        Paint c = new Paint(Paint.ANTI_ALIAS_FLAG);
        c.setColor(fallback);
        canvas.drawRoundRect(new RectF(left, top, left + size, top + size), size * 0.28f, size * 0.28f, c);
        Paint l = paint(0xFFFFFFFF, size * 0.5f, true, Paint.Align.CENTER);
        Paint.FontMetrics fm = l.getFontMetrics();
        canvas.drawText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(),
                left + size / 2f, top + size / 2f - (fm.ascent + fm.descent) / 2f, l);
    }

    private Paint paint(int color, float size, boolean bold, Paint.Align align) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTextSize(size);
        p.setFakeBoldText(bold);
        p.setTextAlign(align);
        return p;
    }

    private String ellipsize(String s, Paint p, float maxW) {
        if (maxW <= 0) return "";
        if (p.measureText(s) <= maxW) return s;
        while (s.length() > 1 && p.measureText(s + "…") > maxW) s = s.substring(0, s.length() - 1);
        return s + "…";
    }
}
