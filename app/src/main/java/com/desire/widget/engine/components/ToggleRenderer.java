package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.data.ToggleState;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.util.Map;

/**
 * A quick-settings tile (Wi-Fi, Bluetooth, torch, location, DND, flight mode, auto-rotate, dark
 * mode, mobile data) that shows the LIVE state: filled accent when on, muted when off. Taps are
 * wired by EngineRenderer through the grid overlay (see widget_layout_grid.xml / ToggleReceiver).
 *
 * <p>props: kind, label (override), shape (rounded|circle), onColor, offColor, iconOnColor,
 * iconOffColor, textColor, showLabel, previewOn (gallery preview state).
 */
public class ToggleRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "toggle";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        String kind = SpecProps.str(p, "kind", ToggleState.WIFI);
        Boolean state = ToggleState.isOn(ctx.android, kind);
        if (ctx.appWidgetId < 0 && p != null && p.containsKey("previewOn")) {
            state = SpecProps.b(p, "previewOn", false);
        }
        boolean on = state != null && state;

        int onColor = SpecColors.resolve(SpecProps.str(p, "onColor", "@primary"), ctx.theme, ctx.theme.primary);
        int offColor = SpecColors.resolve(SpecProps.str(p, "offColor", "#26FFFFFF"), ctx.theme, 0x26FFFFFF);
        int iconOn = SpecColors.resolve(SpecProps.str(p, "iconOnColor", "#FFFFFF"), ctx.theme, 0xFFFFFFFF);
        int iconOff = SpecColors.resolve(SpecProps.str(p, "iconOffColor", "@textSecondary"), ctx.theme, ctx.theme.textSecondary);
        int textColor = SpecColors.resolve(SpecProps.str(p, "textColor", "@textSecondary"), ctx.theme, ctx.theme.textSecondary);
        boolean showLabel = SpecProps.b(p, "showLabel", true);
        boolean circle = "circle".equals(SpecProps.str(p, "shape", "rounded"));

        float labelH = showLabel ? b.height() * 0.24f : 0f;
        float tileSize = Math.min(b.width() * 0.86f, (b.height() - labelH) * 0.92f);
        float cx = b.centerX();
        float cy = b.top + (b.height() - labelH) / 2f;
        RectF tile = new RectF(cx - tileSize / 2f, cy - tileSize / 2f, cx + tileSize / 2f, cy + tileSize / 2f);

        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(on ? onColor : offColor);
        if (circle) {
            canvas.drawCircle(cx, cy, tileSize / 2f, bg);
        } else {
            float r = tileSize * 0.30f;
            canvas.drawRoundRect(tile, r, r, bg);
        }

        Drawable icon = ContextCompat.getDrawable(ctx.android, ToggleState.icon(kind));
        if (icon != null) {
            icon = icon.mutate();
            icon.setTint(on ? iconOn : iconOff);
            float is = tileSize * 0.46f;
            icon.setBounds((int) (cx - is / 2f), (int) (cy - is / 2f), (int) (cx + is / 2f), (int) (cy + is / 2f));
            icon.draw(canvas);
        }

        if (showLabel) {
            Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
            tp.setColor(on ? onColor : textColor);
            tp.setTextAlign(Paint.Align.CENTER);
            tp.setFakeBoldText(on);
            tp.setTextSize(labelH * 0.62f);
            String label = SpecProps.str(p, "label", ToggleState.label(kind));
            float w = tp.measureText(label);
            if (w > b.width() * 0.98f) tp.setTextSize(tp.getTextSize() * b.width() * 0.98f / w);
            Paint.FontMetrics fm = tp.getFontMetrics();
            canvas.drawText(label, cx, b.bottom - labelH / 2f - (fm.ascent + fm.descent) / 2f, tp);
        }
    }
}
