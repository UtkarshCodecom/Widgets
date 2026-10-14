package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.ClockText;
import com.desire.widget.engine.util.FontResolver;
import com.desire.widget.engine.util.Shapes;
import com.desire.widget.engine.util.SpecProps;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Four shaped tiles in a 2x2 grid (hour, minute, date, weekday by default), each with its own
 * expressive shape and colour — the playful "shape library" clock.
 *
 * <p>props: shapes (list of {@link Shapes} names), fills, textColors, formats (SimpleDateFormat per
 * tile; "HOUR" = hour in the 12/24h setting), font, gap (fraction of a cell), use24h, timezone.
 */
public class TimeTilesRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "time_tiles";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        Calendar cal = ClockText.now(ctx, p);
        boolean use24 = ClockText.use24h(ctx, p);
        List<String> shapes = strings(p, "shapes", new String[]{"cookie", "squircle", "circle", "blob"});
        List<String> formats = strings(p, "formats", new String[]{"HOUR", "mm", "dd", "EEE"});
        int[] fills = ClockText.colors(p, "fills", ctx.theme,
                new int[]{0xFFE9E9EC, 0xFFE9E9EC, 0xFF0B0B0D, 0xFF0B0B0D});
        int[] texts = ClockText.colors(p, "textColors", ctx.theme,
                new int[]{0xFF1B1C20, 0xFF1B1C20, 0xFFF2F2F4, 0xFFF2F2F4});
        float gap = SpecProps.f(p, "gap", 0.06f);

        float side = Math.min(b.width(), b.height());
        float cell = side / 2f;
        float left = b.centerX() - side / 2f, top = b.centerY() - side / 2f;
        Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setTypeface(FontResolver.resolve(SpecProps.str(p, "font", "medium"), true));
        tp.setTextAlign(Paint.Align.CENTER);

        for (int i = 0; i < 4; i++) {
            float x = left + cell * (i % 2), y = top + cell * (i / 2);
            float inset = cell * gap;
            RectF tile = new RectF(x + inset, y + inset, x + cell - inset, y + cell - inset);
            fp.setColor(fills[i % fills.length]);
            canvas.drawPath(Shapes.path(shapes.get(i % shapes.size()), tile), fp);

            String text = format(cal, formats.get(i % formats.size()), use24);
            tp.setColor(texts[i % texts.length]);
            tp.setTextSize(tile.height() * (text.length() <= 2 ? 0.42f : 0.3f));
            float w = tp.measureText(text);
            if (w > tile.width() * 0.74f) tp.setTextSize(tp.getTextSize() * tile.width() * 0.74f / w);
            Paint.FontMetrics fm = tp.getFontMetrics();
            canvas.drawText(text, tile.centerX(), tile.centerY() - (fm.ascent + fm.descent) / 2f, tp);
        }
    }

    private String format(Calendar cal, String fmt, boolean use24) {
        if ("HOUR".equals(fmt)) return ClockText.hhmm(cal, use24)[0];
        try {
            SimpleDateFormat f = new SimpleDateFormat(fmt, Locale.getDefault());
            f.setTimeZone(cal.getTimeZone());
            return f.format(cal.getTime()).toUpperCase(Locale.getDefault());
        } catch (Exception e) {
            return "";
        }
    }

    static List<String> strings(Map<String, Object> p, String key, String[] def) {
        List<String> out = new ArrayList<>();
        Object v = p != null ? p.get(key) : null;
        if (v instanceof List) for (Object o : (List<?>) v) if (o != null) out.add(String.valueOf(o));
        if (out.isEmpty()) for (String d : def) out.add(d);
        return out;
    }
}
