package com.desire.widget.engine.components;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.Draw;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.util.Map;

/**
 * Draws an image. props:
 * <ul>
 *   <li>source — http(s) URL, "asset://path", "content://…"/"file://…", or a drawable resource name.</li>
 *   <li>scaleType — "crop" (default): center-crop into a rounded rect (photos/backgrounds);
 *       "fit": preserve aspect ratio, centred, transparent, no crop or corners (stickers).</li>
 *   <li>cornerRadius — fraction of the min side (crop mode only).</li>
 *   <li>tint — optional color overlay (crop mode only).</li>
 * </ul>
 *
 * <p>Image bytes are fetched SYNCHRONOUSLY (Glide {@code submit().get()} for remote/asset/content
 * sources, or the drawable rasterised directly for resource names so vector stickers render
 * crisply). The engine runs off the main thread, so this blocks a worker, never the UI. On failure
 * a surface-colored placeholder is drawn in crop mode, and nothing in fit mode (so a missing
 * sticker just disappears instead of leaving a grey box on top of the widget).
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
        boolean fit = "fit".equals(SpecProps.str(p, "scaleType", "crop"));

        // Sticker mode: a "fit" image positioned by cx/cy/boxW props within the (full-widget) frame,
        // optionally rotated. Drawn in a square box centred at (cx,cy) so scale is consistent.
        if (fit && p != null && p.containsKey("cx")) {
            float side = SpecProps.f(p, "boxW", 0.3f) * b.width();
            float cx = b.left + SpecProps.f(p, "cx", 0.5f) * b.width();
            float cy = b.top + SpecProps.f(p, "cy", 0.5f) * b.height();
            float rotation = SpecProps.f(p, "rotation", 0f);
            Bitmap sb = load(ctx, source, (int) side, (int) side);
            if (sb == null) return;
            RectF box = new RectF(cx - side / 2f, cy - side / 2f, cx + side / 2f, cy + side / 2f);
            int save = canvas.save();
            if (rotation != 0f) canvas.rotate(rotation, cx, cy);
            drawFit(canvas, sb, box);
            canvas.restoreToCount(save);
            return;
        }

        Bitmap bmp = load(ctx, source, (int) b.width(), (int) b.height());

        if (fit) {
            if (bmp != null) drawFit(canvas, bmp, b);
            return; // stickers: no placeholder, no corners, keep transparency
        }

        float corner = SpecProps.f(p, "cornerRadius", 0.12f) * Math.min(b.width(), b.height());
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

    /** Draws the bitmap scaled to fit inside b, centred, aspect preserved, alpha kept. */
    private void drawFit(Canvas canvas, Bitmap bmp, RectF b) {
        float scale = Math.min(b.width() / bmp.getWidth(), b.height() / bmp.getHeight());
        float dw = bmp.getWidth() * scale;
        float dh = bmp.getHeight() * scale;
        float left = b.centerX() - dw / 2f;
        float top = b.centerY() - dh / 2f;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);
        canvas.drawBitmap(bmp, null, new RectF(left, top, left + dw, top + dh), paint);
    }

    private Bitmap load(RenderContext ctx, String source, int w, int h) {
        if (source == null || source.isEmpty()) return null;
        w = Math.max(1, w);
        h = Math.max(1, h);
        try {
            if (source.startsWith("http") || source.startsWith("content://")
                    || source.startsWith("file://")) {
                Object model = source.startsWith("http") ? source : android.net.Uri.parse(source);
                return Glide.with(ctx.android).asBitmap().load(model).submit(w, h).get();
            }
            if (source.startsWith("asset://")) {
                String model = "file:///android_asset/" + source.substring("asset://".length());
                return Glide.with(ctx.android).asBitmap().load(model).submit(w, h).get();
            }
            // Drawable resource name — rasterise directly so vector stickers stay crisp and keep
            // their intrinsic aspect ratio (Glide's vector support is inconsistent).
            int resId = ctx.android.getResources().getIdentifier(
                    source, "drawable", ctx.android.getPackageName());
            if (resId == 0) return null;
            Drawable d = ContextCompat.getDrawable(ctx.android, resId);
            if (d == null) return null;
            int iw = d.getIntrinsicWidth() > 0 ? d.getIntrinsicWidth() : w;
            int ih = d.getIntrinsicHeight() > 0 ? d.getIntrinsicHeight() : h;
            float sc = Math.min(w / (float) iw, h / (float) ih);
            int bw = Math.max(1, Math.round(iw * sc));
            int bh = Math.max(1, Math.round(ih * sc));
            Bitmap bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            d.setBounds(0, 0, bw, bh);
            d.draw(c);
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }
}
