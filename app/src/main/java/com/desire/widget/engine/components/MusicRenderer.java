package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.data.NowPlaying;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.util.Map;

/**
 * Now-playing card: track title, artist, a progress bar, and a prev / play-or-pause / next control
 * row drawn natively with Canvas. Reads {@link com.desire.widget.engine.data.LiveDataSource} when
 * present; props override individual fields.
 *
 * <p>props: title, artist, playing, progress (0..1), accentColor, textColor, subColor.
 */
public class MusicRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "music";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        NowPlaying np = ctx.live != null ? ctx.live.nowPlaying() : new NowPlaying();
        np.title = SpecProps.str(p, "title", np.title);
        np.artist = SpecProps.str(p, "artist", np.artist);
        np.playing = SpecProps.b(p, "playing", np.playing);
        np.progress = Math.max(0f, Math.min(1f, SpecProps.f(p, "progress", np.progress)));

        int accent = SpecColors.resolve(SpecProps.str(p, "accentColor", "@primary"), ctx.theme, ctx.theme.primary);
        int textColor = SpecColors.resolve(SpecProps.str(p, "textColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        int subColor = SpecColors.resolve(SpecProps.str(p, "subColor", "@textSecondary"), ctx.theme, ctx.theme.textSecondary);

        float pad = b.height() * 0.12f;

        Paint title = new Paint(Paint.ANTI_ALIAS_FLAG);
        title.setColor(textColor);
        title.setFakeBoldText(true);
        title.setTextSize(b.height() * 0.2f);
        canvas.drawText(ellipsize(np.title, 22), b.left + pad, b.top + b.height() * 0.27f, title);

        Paint artist = new Paint(Paint.ANTI_ALIAS_FLAG);
        artist.setColor(subColor);
        artist.setTextSize(b.height() * 0.14f);
        canvas.drawText(ellipsize(np.artist, 26), b.left + pad, b.top + b.height() * 0.47f, artist);

        // Progress bar
        float barY = b.top + b.height() * 0.62f;
        float barLeft = b.left + pad;
        float barRight = b.right - pad;
        float barH = b.height() * 0.05f;
        Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        track.setColor(subColor);
        track.setAlpha(90);
        RectF trackRect = new RectF(barLeft, barY, barRight, barY + barH);
        canvas.drawRoundRect(trackRect, barH, barH, track);
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(accent);
        RectF fillRect = new RectF(barLeft, barY, barLeft + (barRight - barLeft) * np.progress, barY + barH);
        canvas.drawRoundRect(fillRect, barH, barH, fill);

        // Controls row
        float ctrlY = b.top + b.height() * 0.84f;
        float r = b.height() * 0.1f;
        float cx = b.centerX();
        drawPrevNext(canvas, cx - r * 3.2f, ctrlY, r * 0.8f, textColor, true);
        if (np.playing) {
            drawPause(canvas, cx, ctrlY, r, accent);
        } else {
            drawPlay(canvas, cx, ctrlY, r, accent);
        }
        drawPrevNext(canvas, cx + r * 3.2f, ctrlY, r * 0.8f, textColor, false);
    }

    private void drawPlay(Canvas canvas, float cx, float cy, float r, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        Path tri = new Path();
        tri.moveTo(cx - r * 0.5f, cy - r * 0.7f);
        tri.lineTo(cx + r * 0.7f, cy);
        tri.lineTo(cx - r * 0.5f, cy + r * 0.7f);
        tri.close();
        canvas.drawPath(tri, p);
    }

    private void drawPause(Canvas canvas, float cx, float cy, float r, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        float bw = r * 0.32f;
        canvas.drawRoundRect(new RectF(cx - r * 0.55f, cy - r * 0.7f, cx - r * 0.55f + bw, cy + r * 0.7f), bw * 0.3f, bw * 0.3f, p);
        canvas.drawRoundRect(new RectF(cx + r * 0.23f, cy - r * 0.7f, cx + r * 0.23f + bw, cy + r * 0.7f), bw * 0.3f, bw * 0.3f, p);
    }

    private void drawPrevNext(Canvas canvas, float cx, float cy, float r, int color, boolean prev) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        int dir = prev ? -1 : 1;
        Path t1 = new Path();
        t1.moveTo(cx, cy - r * 0.7f);
        t1.lineTo(cx + dir * r * 0.7f, cy);
        t1.lineTo(cx, cy + r * 0.7f);
        t1.close();
        canvas.drawPath(t1, p);
        Path t2 = new Path();
        t2.moveTo(cx + dir * r * 0.7f, cy - r * 0.7f);
        t2.lineTo(cx + dir * r * 1.4f, cy);
        t2.lineTo(cx + dir * r * 0.7f, cy + r * 0.7f);
        t2.close();
        canvas.drawPath(t2, p);
    }

    private String ellipsize(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
