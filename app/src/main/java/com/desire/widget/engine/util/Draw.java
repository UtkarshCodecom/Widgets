package com.desire.widget.engine.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
 * Bitmap/Drawable drawing helpers shared by the image and app-shortcut renderers.
 */
public final class Draw {
    private Draw() {}

    public static Bitmap fromDrawable(Drawable d, int w, int h) {
        if (d == null) return null;
        if (d instanceof BitmapDrawable && ((BitmapDrawable) d).getBitmap() != null) {
            return ((BitmapDrawable) d).getBitmap();
        }
        w = Math.max(1, w);
        h = Math.max(1, h);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        d.setBounds(0, 0, w, h);
        d.draw(c);
        return bmp;
    }

    /** Draws {@code bmp} into {@code dst} using center-crop, clipped to a rounded rect. */
    public static void bitmapCenterCrop(Canvas canvas, Bitmap bmp, RectF dst, float corner) {
        if (bmp == null || bmp.getWidth() == 0 || bmp.getHeight() == 0) return;
        int bw = bmp.getWidth();
        int bh = bmp.getHeight();
        float scale = Math.max(dst.width() / bw, dst.height() / bh);
        float sw = dst.width() / scale;
        float sh = dst.height() / scale;
        float sx = (bw - sw) / 2f;
        float sy = (bh - sh) / 2f;
        Rect src = new Rect(Math.round(sx), Math.round(sy), Math.round(sx + sw), Math.round(sy + sh));

        int save = canvas.save();
        Path clip = new Path();
        clip.addRoundRect(dst, corner, corner, Path.Direction.CW);
        canvas.clipPath(clip);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setFilterBitmap(true);
        canvas.drawBitmap(bmp, src, dst, p);
        canvas.restoreToCount(save);
    }
}
