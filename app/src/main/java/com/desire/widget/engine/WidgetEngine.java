package com.desire.widget.engine;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

import com.desire.widget.engine.model.BackgroundSpec;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.model.Frame;
import com.desire.widget.engine.model.WidgetSpec;
import com.desire.widget.engine.util.SpecColors;

/**
 * The single source of truth for turning a {@link WidgetSpec} into a {@link Bitmap}.
 *
 * <p>This exact method is called by BOTH the RecyclerView gallery preview and the installed
 * home-screen widget (via RemoteViews ImageView). Because the only difference between the two is
 * the pixel size in {@link RenderContext}, previews are guaranteed pixel-identical to the real
 * widget — the core requirement of the architecture.
 *
 * <p>Threading: rendering is synchronous and CPU/Canvas-bound. Call it off the main thread
 * (WorkManager, a background executor, or Glide's decode thread). It allocates one ARGB_8888
 * bitmap of the requested size.
 */
public class WidgetEngine {
    private final ComponentRegistry registry;

    public WidgetEngine() {
        this(ComponentRegistry.createDefault());
    }

    public WidgetEngine(ComponentRegistry registry) {
        this.registry = registry != null ? registry : ComponentRegistry.createDefault();
    }

    public ComponentRegistry registry() {
        return registry;
    }

    /** Render into a freshly-allocated bitmap (used by gallery previews). */
    public Bitmap render(WidgetSpec spec, RenderContext ctx) {
        int w = Math.max(1, ctx.widthPx);
        int h = Math.max(1, ctx.heightPx);
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        render(spec, ctx, bitmap);
        return bitmap;
    }

    /** Render into a pre-allocated bitmap (used by installed widgets to reduce GC). */
    public void render(WidgetSpec spec, RenderContext ctx, Bitmap target) {
        int w = Math.max(1, ctx.widthPx);
        int h = Math.max(1, ctx.heightPx);
        Canvas canvas = new Canvas(target);

        drawBackground(canvas, spec != null ? spec.background : null, ctx);

        if (spec != null && spec.components != null) {
            for (ComponentSpec cs : spec.components) {
                if (cs == null || cs.type == null) continue;
                ComponentRenderer renderer = registry.get(cs.type);
                if (renderer == null) continue;
                RectF bounds = denormalize(cs.frame, w, h);
                int save = canvas.save();
                canvas.clipRect(bounds);
                try {
                    renderer.render(canvas, bounds, cs, ctx);
                } catch (Exception ignored) {
                }
                canvas.restoreToCount(save);
            }
        }
    }

    private RectF denormalize(Frame f, int w, int h) {
        if (f == null) return new RectF(0, 0, w, h);
        return new RectF(f.x * w, f.y * h, (f.x + f.w) * w, (f.y + f.h) * h);
    }

    private void drawBackground(Canvas canvas, BackgroundSpec bg, RenderContext ctx) {
        float w = ctx.widthPx;
        float h = ctx.heightPx;
        float cr = (bg != null ? bg.cornerRadius : 0.08f) * Math.min(w, h);
        RectF rect = new RectF(0, 0, w, h);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (bg != null && bg.gradientStart != null && bg.gradientEnd != null) {
            int c0 = SpecColors.resolve(bg.gradientStart, ctx.theme, ctx.theme.surface);
            int c1 = SpecColors.resolve(bg.gradientEnd, ctx.theme, ctx.theme.background);
            double ang = Math.toRadians(bg.gradientAngle);
            float dx = (float) Math.cos(ang);
            float dy = (float) Math.sin(ang);
            paint.setShader(new LinearGradient(
                    w / 2f - dx * w / 2f, h / 2f - dy * h / 2f,
                    w / 2f + dx * w / 2f, h / 2f + dy * h / 2f,
                    c0, c1, Shader.TileMode.CLAMP));
        } else {
            int fill = bg != null ? SpecColors.resolve(bg.fill, ctx.theme, ctx.theme.surface) : ctx.theme.surface;
            paint.setColor(fill);
        }
        canvas.drawRoundRect(rect, cr, cr, paint);

        Path clip = new Path();
        clip.addRoundRect(rect, cr, cr, Path.Direction.CW);
        canvas.clipPath(clip);
    }
}
