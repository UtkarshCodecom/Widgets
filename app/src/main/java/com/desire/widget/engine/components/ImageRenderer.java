package com.desire.widget.engine.components;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.bumptech.glide.Glide;
import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.Draw;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.util.Map;

/**
 * Draws an image, center-cropped into a rounded rect. props: source (http(s) URL, "asset://path",
 * or drawable resource name), cornerRadius (fraction of min side), tint (optional overlay).
 *
 * <p>Image bytes are fetched SYNCHRONOUSLY via Glide ({@code submit().get()}). The engine is
 * documented to run off the main thread, so this blocks a worker, never the UI. On any failure a
 * surface-colored placeholder is drawn so the widget never goes blank.
 */
public class ImageRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "image";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        String source = SpecProps.str(p, "source", "");
        float corner = SpecProps.f(p, "cornerRadius", 0.12f) * Math.min(b.width(), b.height());

        Bitmap bmp = load(ctx, source, (int) b.width(), (int) b.height());
        if (bmp != null) {
            Draw.bitmapCenterCrop(canvas, bmp, b, corner);
            String tint = SpecProps.str(p, "tint", "");
            if (!tint.isEmpty()) {
                Paint overlay = new Paint(Paint.ANTI_ALIAS_FLAG);
                overlay.setColor(SpecColors.resolve(tint, ctx.theme, 0));
                canvas.drawRoundRect(b, corner, corner, overlay);
            }
        } else {
            Paint placeholder = new Paint(Paint.ANTI_ALIAS_FLAG);
            placeholder.setColor(ctx.theme.surface);
            canvas.drawRoundRect(b, corner, corner, placeholder);
        }
    }

    private Bitmap load(RenderContext ctx, String source, int w, int h) {
        if (source == null || source.isEmpty()) return null;
        try {
            Object model;
            if (source.startsWith("http")) {
                model = source;
            } else if (source.startsWith("content://") || source.startsWith("file://")) {
                model = android.net.Uri.parse(source);
            } else if (source.startsWith("asset://")) {
                model = "file:///android_asset/" + source.substring("asset://".length());
            } else {
                int resId = ctx.android.getResources().getIdentifier(
                        source, "drawable", ctx.android.getPackageName());
                if (resId == 0) return null;
                model = resId;
            }
            return Glide.with(ctx.android)
                    .asBitmap()
                    .load(model)
                    .submit(Math.max(1, w), Math.max(1, h))
                    .get();
        } catch (Exception e) {
            return null;
        }
    }
}
