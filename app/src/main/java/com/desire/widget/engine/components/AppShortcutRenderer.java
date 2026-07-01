package com.desire.widget.engine.components;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.Draw;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.util.Map;

/**
 * App icon tile with optional label. Loads the real launcher icon for {@code packageName} via
 * PackageManager; if the app is not installed/visible, draws an accent tile with the first letter
 * as a fallback so the widget never breaks. The tap action is supplied separately via the
 * component's {@code action} (launch_app) and compiled by ActionCompiler in Phase 3.
 *
 * <p>props: packageName, label, showLabel, cornerRadius (fraction of icon side), iconBg, textColor.
 */
public class AppShortcutRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "app_shortcut";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        String pkg = SpecProps.str(p, "packageName", "");
        String label = SpecProps.str(p, "label", appLabelFallback(pkg));
        boolean showLabel = SpecProps.b(p, "showLabel", true);
        int textColor = SpecColors.resolve(SpecProps.str(p, "textColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);

        float labelH = showLabel ? b.height() * 0.22f : 0f;
        float iconSide = Math.min(b.width(), b.height() - labelH);
        float iconLeft = b.centerX() - iconSide / 2f;
        float iconTop = b.top + (b.height() - labelH - iconSide) / 2f;
        RectF iconRect = new RectF(iconLeft, iconTop, iconLeft + iconSide, iconTop + iconSide);
        float corner = SpecProps.f(p, "cornerRadius", 0.25f) * iconSide;

        Bitmap icon = loadIcon(ctx, pkg, (int) iconSide);
        if (icon != null) {
            Draw.bitmapCenterCrop(canvas, icon, iconRect, corner);
        } else {
            int bg = SpecColors.resolve(SpecProps.str(p, "iconBg", "@primary"), ctx.theme, ctx.theme.primary);
            Paint tile = new Paint(Paint.ANTI_ALIAS_FLAG);
            tile.setColor(bg);
            canvas.drawRoundRect(iconRect, corner, corner, tile);
            Paint letter = new Paint(Paint.ANTI_ALIAS_FLAG);
            letter.setColor(ctx.theme.onPrimary);
            letter.setFakeBoldText(true);
            letter.setTextAlign(Paint.Align.CENTER);
            letter.setTextSize(iconSide * 0.5f);
            Paint.FontMetrics fm = letter.getFontMetrics();
            String ch = (label != null && !label.isEmpty()) ? label.substring(0, 1).toUpperCase() : "?";
            canvas.drawText(ch, iconRect.centerX(), iconRect.centerY() - (fm.ascent + fm.descent) / 2f, letter);
        }

        if (showLabel && label != null && !label.isEmpty()) {
            Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
            text.setColor(textColor);
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(labelH * 0.62f);
            canvas.drawText(ellipsize(label, 14), b.centerX(), b.bottom - labelH * 0.25f, text);
        }
    }

    private Bitmap loadIcon(RenderContext ctx, String pkg, int side) {
        if (pkg == null || pkg.isEmpty()) return null;
        try {
            Drawable d = ctx.android.getPackageManager().getApplicationIcon(pkg);
            return Draw.fromDrawable(d, side, side);
        } catch (Exception e) {
            return null; // not installed / not visible -> fallback tile
        }
    }

    private String appLabelFallback(String pkg) {
        if (pkg == null || pkg.isEmpty()) return "App";
        String[] parts = pkg.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : pkg;
    }

    private String ellipsize(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
