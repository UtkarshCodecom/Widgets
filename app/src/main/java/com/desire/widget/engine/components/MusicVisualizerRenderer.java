package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.util.Random;

/**
 * A row of equalizer bars (Muviz-style). Bar heights are pseudo-random, seeded by the current
 * time bucket so they visibly change on each refresh. props: bars (count), barColor, gap (0..1 of
 * bar width). Note: home-screen widgets redraw on refresh, not at audio frame-rate, so this is a
 * lively-but-not-realtime visualizer; the in-app preview can animate it.
 */
public class MusicVisualizerRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "music_visualizer";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        int bars = Math.max(3, SpecProps.i(s.props, "bars", 7));
        int color = SpecColors.resolve(SpecProps.str(s.props, "barColor", "@primary"), ctx.theme, ctx.theme.primary);
        float gapFrac = SpecProps.f(s.props, "gap", 0.4f);

        float slot = b.width() / bars;
        float barW = slot / (1f + gapFrac);
        float radius = barW * 0.4f;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);

        Random rnd = new Random(ctx.nowMillis / 800L); // changes ~every refresh
        for (int i = 0; i < bars; i++) {
            float hFrac = 0.25f + rnd.nextFloat() * 0.75f;
            float barH = b.height() * hFrac;
            float left = b.left + i * slot + (slot - barW) / 2f;
            float top = b.bottom - barH;
            RectF r = new RectF(left, top, left + barW, b.bottom);
            canvas.drawRoundRect(r, radius, radius, paint);
        }
    }
}
