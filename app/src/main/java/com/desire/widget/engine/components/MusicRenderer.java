package com.desire.widget.engine.components;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

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
 * <p>props: title, artist, playing, progress (0..1), accentColor, textColor, subColor,
 * style (card|vinyl — the art slot drawn as a turning record), theme (default|albumArt — tint the
 * card from the cover art's most vivid colour).
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
        if (ctx.appWidgetId < 0) {
            // Gallery/customize preview: no real session — show the spec's sample content.
            // On a PLACED widget the live session state always wins; letting these placeholder
            // props override it froze the play/pause icon and the progress bar (the old bug).
            np.title = SpecProps.str(p, "title", np.title);
            np.artist = SpecProps.str(p, "artist", np.artist);
            np.playing = SpecProps.b(p, "playing", np.playing);
            np.progress = Math.max(0f, Math.min(1f, SpecProps.f(p, "progress", np.progress)));
        } else if (np.durationMillis > 0) {
            // Extrapolate the position since the last session update so the bar moves while
            // playing instead of freezing at the last notification's position.
            long pos = np.positionMillis
                    + (np.playing ? Math.max(0, ctx.nowMillis - np.updatedAtMillis) : 0);
            np.progress = Math.max(0f, Math.min(1f, pos / (float) np.durationMillis));
        }

        int accent = SpecColors.resolve(SpecProps.str(p, "accentColor", "@primary"), ctx.theme, ctx.theme.primary);
        int textColor = SpecColors.resolve(SpecProps.str(p, "textColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        int subColor = SpecColors.resolve(SpecProps.str(p, "subColor", "@textSecondary"), ctx.theme, ctx.theme.textSecondary);
        boolean vinyl = "vinyl".equals(SpecProps.str(p, "style", "card"));

        float pad = b.height() * 0.12f;
        float textLeft = b.left + pad;

        Bitmap art = loadArt(np.albumArtPath);

        // Album-art theme: tint the whole card from the cover's most vivid colour so every song
        // gets its own look; falls back to the design's colours when nothing is playing.
        if ("albumArt".equals(SpecProps.str(p, "theme", "default")) && art != null) {
            int base = vividColor(np.albumArtPath, art);
            Paint backdrop = new Paint(Paint.ANTI_ALIAS_FLAG);
            backdrop.setShader(new android.graphics.LinearGradient(b.left, b.top, b.right, b.bottom,
                    shade(base, 0.55f), shade(base, 0.22f), Shader.TileMode.CLAMP));
            float corner = b.height() * 0.12f;
            canvas.drawRoundRect(b, corner, corner, backdrop);
            accent = lighten(base);
            subColor = (textColor & 0x00FFFFFF) | 0xB3000000;
        }

        float artSize = b.height() * 0.56f;
        RectF artRect = new RectF(b.left + pad, b.top + pad, b.left + pad + artSize, b.top + pad + artSize);
        if (vinyl) {
            // The art slot becomes a record that turns while playing (angle from track position).
            long pos = np.positionMillis
                    + (np.playing && np.updatedAtMillis > 0 ? Math.max(0, ctx.nowMillis - np.updatedAtMillis) : 0);
            float angle = ctx.appWidgetId < 0 ? 24f : (pos / 1000f * 24f) % 360f;
            drawVinyl(canvas, artRect, art, angle, accent);
            textLeft = artRect.right + pad * 0.8f;
        } else if (art != null) {
            // Album art thumbnail, if the currently playing session provided one.
            drawRoundedBitmap(canvas, art, artRect, artSize * 0.18f);
            textLeft = artRect.right + pad * 0.8f;
        }

        Paint title = new Paint(Paint.ANTI_ALIAS_FLAG);
        title.setColor(textColor);
        title.setFakeBoldText(true);
        title.setTextSize(b.height() * 0.2f);
        canvas.drawText(ellipsize(np.title, 18), textLeft, b.top + b.height() * 0.27f, title);

        Paint artist = new Paint(Paint.ANTI_ALIAS_FLAG);
        artist.setColor(subColor);
        artist.setTextSize(b.height() * 0.14f);
        canvas.drawText(ellipsize(np.artist, 22), textLeft, b.top + b.height() * 0.47f, artist);

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
        float fillX = barLeft + (barRight - barLeft) * np.progress;
        RectF fillRect = new RectF(barLeft, barY, fillX, barY + barH);
        canvas.drawRoundRect(fillRect, barH, barH, fill);

        // Draggable-looking thumb at the play position — signals the bar is tappable to seek.
        float thumbR = barH * 1.8f;
        canvas.drawCircle(fillX, barY + barH / 2f, thumbR, fill);

        // Controls row — spread prev / play-pause / next across the width so they line up with the
        // widget_ctrl_* tap regions (left / centre / right thirds in widget_layout.xml). Clustering
        // them near the centre (the old layout) meant prev & next fell inside the centre third on a
        // wide 4x2 widget, so only play/pause was ever tappable.
        float ctrlY = b.top + b.height() * 0.84f;
        float r = b.height() * 0.12f;
        float playCx = b.centerX();
        float prevCx = b.left + b.width() * 0.16f;
        float nextCx = b.left + b.width() * 0.84f;

        drawPrevNext(canvas, prevCx, ctrlY, r * 0.8f, textColor, true);

        // Filled accent disc behind play/pause so it reads as the primary button.
        Paint disc = new Paint(Paint.ANTI_ALIAS_FLAG);
        disc.setColor(accent);
        disc.setAlpha(38);
        canvas.drawCircle(playCx, ctrlY, r * 1.5f, disc);
        if (np.playing) {
            drawPause(canvas, playCx, ctrlY, r, accent);
        } else {
            drawPlay(canvas, playCx, ctrlY, r, accent);
        }

        drawPrevNext(canvas, nextCx, ctrlY, r * 0.8f, textColor, false);
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

    /** A vinyl record in {@code slot}: black disc, grooves, the album art as a circular label. */
    private void drawVinyl(Canvas canvas, RectF slot, Bitmap art, float angle, int accent) {
        float cx = slot.centerX(), cy = slot.centerY();
        float r = slot.width() / 2f;
        Paint disc = new Paint(Paint.ANTI_ALIAS_FLAG);
        disc.setColor(0xFF111114);
        canvas.drawCircle(cx, cy, r, disc);

        Paint groove = new Paint(Paint.ANTI_ALIAS_FLAG);
        groove.setStyle(Paint.Style.STROKE);
        groove.setStrokeWidth(Math.max(1f, r * 0.012f));
        groove.setColor(0x26FFFFFF);
        for (float gr = r * 0.95f; gr > r * 0.5f; gr -= r * 0.07f) canvas.drawCircle(cx, cy, gr, groove);

        // A soft sheen so the disc reads as glossy.
        Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
        sheen.setStyle(Paint.Style.STROKE);
        sheen.setStrokeWidth(r * 0.18f);
        sheen.setColor(0x14FFFFFF);
        RectF sheenOval = new RectF(cx - r * 0.75f, cy - r * 0.75f, cx + r * 0.75f, cy + r * 0.75f);
        canvas.drawArc(sheenOval, 200f, 50f, false, sheen);

        float lr = r * 0.44f;
        RectF label = new RectF(cx - lr, cy - lr, cx + lr, cy + lr);
        int save = canvas.save();
        canvas.rotate(angle, cx, cy);
        if (art != null) {
            Path clip = new Path();
            clip.addCircle(cx, cy, lr, Path.Direction.CW);
            canvas.clipPath(clip);
            drawRoundedBitmap(canvas, art, label, 0f);
        } else {
            Paint lp = new Paint(Paint.ANTI_ALIAS_FLAG);
            lp.setColor(accent);
            canvas.drawCircle(cx, cy, lr, lp);
            Paint mark = new Paint(Paint.ANTI_ALIAS_FLAG);
            mark.setColor(0x55000000);
            canvas.drawRect(cx - lr * 0.08f, cy - lr * 0.8f, cx + lr * 0.08f, cy - lr * 0.35f, mark);
        }
        canvas.restoreToCount(save);

        Paint hole = new Paint(Paint.ANTI_ALIAS_FLAG);
        hole.setColor(0xFF111114);
        canvas.drawCircle(cx, cy, r * 0.06f, hole);
    }

    // ------------------------------------------------------------------ album-art colour

    private static final java.util.Map<String, Integer> ART_COLORS = new java.util.HashMap<>();

    /** Most vivid colour in the art (cached per file + modification time). */
    private static int vividColor(String path, Bitmap art) {
        String key = path + "@" + new java.io.File(path).lastModified();
        synchronized (ART_COLORS) {
            Integer cached = ART_COLORS.get(key);
            if (cached != null) return cached;
        }
        Bitmap small = Bitmap.createScaledBitmap(art, 24, 24, true);
        float[] hsv = new float[3];
        float bestScore = -1f;
        int best = 0xFF5C6BC0;
        long rs = 0, gs = 0, bs = 0;
        int n = 0;
        for (int y = 0; y < small.getHeight(); y++) {
            for (int x = 0; x < small.getWidth(); x++) {
                int c = small.getPixel(x, y);
                android.graphics.Color.colorToHSV(c, hsv);
                rs += android.graphics.Color.red(c);
                gs += android.graphics.Color.green(c);
                bs += android.graphics.Color.blue(c);
                n++;
                // Favour saturated, mid-bright pixels (avoid near-black / near-white).
                float score = hsv[1] * (1f - Math.abs(hsv[2] - 0.65f));
                if (hsv[2] > 0.22f && score > bestScore) {
                    bestScore = score;
                    best = c;
                }
            }
        }
        if (bestScore < 0.12f && n > 0) { // greyscale cover → use its average tone
            best = android.graphics.Color.rgb((int) (rs / n), (int) (gs / n), (int) (bs / n));
        }
        best |= 0xFF000000;
        synchronized (ART_COLORS) {
            if (ART_COLORS.size() > 24) ART_COLORS.clear();
            ART_COLORS.put(key, best);
        }
        return best;
    }

    /** Multiply RGB by {@code f} (0..1) → darker shade of the same hue. */
    private static int shade(int c, float f) {
        return android.graphics.Color.rgb(
                (int) (android.graphics.Color.red(c) * f),
                (int) (android.graphics.Color.green(c) * f),
                (int) (android.graphics.Color.blue(c) * f));
    }

    /** Ensures an accent is bright enough to read on the darkened backdrop. */
    private static int lighten(int c) {
        float[] hsv = new float[3];
        android.graphics.Color.colorToHSV(c, hsv);
        hsv[2] = Math.max(hsv[2], 0.85f);
        hsv[1] = Math.min(hsv[1], 0.75f);
        return android.graphics.Color.HSVToColor(hsv);
    }

    private Bitmap loadArt(String path) {
        if (path == null || path.isEmpty()) return null;
        try {
            return BitmapFactory.decodeFile(path);
        } catch (Exception e) {
            return null;
        }
    }

    private void drawRoundedBitmap(Canvas canvas, Bitmap bmp, RectF dst, float corner) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new android.graphics.BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        android.graphics.Matrix m = new android.graphics.Matrix();
        float scale = Math.max(dst.width() / bmp.getWidth(), dst.height() / bmp.getHeight());
        m.setScale(scale, scale);
        m.postTranslate(dst.left, dst.top);
        paint.getShader().setLocalMatrix(m);
        canvas.drawRoundRect(dst, corner, corner, paint);
    }

    private String ellipsize(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
