package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.ClockText;
import com.desire.widget.engine.util.FontResolver;
import com.desire.widget.engine.util.SpecProps;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Time drawn on a dot matrix with a built-in 5x7 font: round LED dots ({@code style: led}) or
 * square pixels ({@code style: pixel}), optionally with the unlit dots faintly visible
 * ({@code ghost}). A second line (date) can sit above or below, in dots or regular type.
 *
 * <p>props: style, format (SimpleDateFormat; default HH:mm / hh:mm per the 12/24h setting),
 * colors (left→right gradient of lit dots), ghost, ghostColor, subFormat, subColor,
 * subPosition (top|bottom), subDots (true = dot font, false = regular text), subScale, font,
 * use24h, timezone.
 */
public class DotClockRenderer implements ComponentRenderer {
    private static final Map<Character, String[]> FONT = new HashMap<>();

    static {
        put('0', "01110", "10001", "10001", "10001", "10001", "10001", "01110");
        put('1', "00100", "01100", "00100", "00100", "00100", "00100", "01110");
        put('2', "01110", "10001", "00001", "00010", "00100", "01000", "11111");
        put('3', "11110", "00001", "00001", "01110", "00001", "00001", "11110");
        put('4', "00010", "00110", "01010", "10010", "11111", "00010", "00010");
        put('5', "11111", "10000", "11110", "00001", "00001", "10001", "01110");
        put('6', "00110", "01000", "10000", "11110", "10001", "10001", "01110");
        put('7', "11111", "00001", "00010", "00100", "01000", "01000", "01000");
        put('8', "01110", "10001", "10001", "01110", "10001", "10001", "01110");
        put('9', "01110", "10001", "10001", "01111", "00001", "00010", "01100");
        put('A', "01110", "10001", "10001", "11111", "10001", "10001", "10001");
        put('B', "11110", "10001", "10001", "11110", "10001", "10001", "11110");
        put('C', "01110", "10001", "10000", "10000", "10000", "10001", "01110");
        put('D', "11100", "10010", "10001", "10001", "10001", "10010", "11100");
        put('E', "11111", "10000", "10000", "11110", "10000", "10000", "11111");
        put('F', "11111", "10000", "10000", "11110", "10000", "10000", "10000");
        put('G', "01110", "10001", "10000", "10111", "10001", "10001", "01111");
        put('H', "10001", "10001", "10001", "11111", "10001", "10001", "10001");
        put('I', "111", "010", "010", "010", "010", "010", "111");
        put('J', "00111", "00010", "00010", "00010", "00010", "10010", "01100");
        put('K', "10001", "10010", "10100", "11000", "10100", "10010", "10001");
        put('L', "10000", "10000", "10000", "10000", "10000", "10000", "11111");
        put('M', "10001", "11011", "10101", "10101", "10001", "10001", "10001");
        put('N', "10001", "10001", "11001", "10101", "10011", "10001", "10001");
        put('O', "01110", "10001", "10001", "10001", "10001", "10001", "01110");
        put('P', "11110", "10001", "10001", "11110", "10000", "10000", "10000");
        put('Q', "01110", "10001", "10001", "10001", "10101", "10010", "01101");
        put('R', "11110", "10001", "10001", "11110", "10100", "10010", "10001");
        put('S', "01111", "10000", "10000", "01110", "00001", "00001", "11110");
        put('T', "11111", "00100", "00100", "00100", "00100", "00100", "00100");
        put('U', "10001", "10001", "10001", "10001", "10001", "10001", "01110");
        put('V', "10001", "10001", "10001", "10001", "10001", "01010", "00100");
        put('W', "10001", "10001", "10001", "10101", "10101", "10101", "01010");
        put('X', "10001", "10001", "01010", "00100", "01010", "10001", "10001");
        put('Y', "10001", "10001", "10001", "01010", "00100", "00100", "00100");
        put('Z', "11111", "00001", "00010", "00100", "01000", "10000", "11111");
        put(':', "0", "0", "1", "0", "1", "0", "0");
        put('.', "0", "0", "0", "0", "0", "0", "1");
        put(',', "00", "00", "00", "00", "00", "01", "10");
        put(' ', "00", "00", "00", "00", "00", "00", "00");
        put('-', "000", "000", "000", "111", "000", "000", "000");
        put('/', "00001", "00010", "00010", "00100", "01000", "01000", "10000");
        put('%', "11001", "11010", "00010", "00100", "01000", "01011", "10011");
        put('°', "010", "101", "010", "000", "000", "000", "000");
    }

    private static void put(char c, String... rows) {
        FONT.put(c, rows);
    }

    @Override
    public String type() {
        return "dot_clock";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        Calendar cal = ClockText.now(ctx, p);
        boolean use24 = ClockText.use24h(ctx, p);
        String main = format(cal, SpecProps.str(p, "format", use24 ? "HH:mm" : "hh:mm"));
        String subFmt = SpecProps.str(p, "subFormat", "");
        String sub = subFmt == null || subFmt.isEmpty() ? "" : format(cal, subFmt);
        boolean subTop = "top".equals(SpecProps.str(p, "subPosition", "bottom"));
        boolean subDots = SpecProps.b(p, "subDots", true);
        float subScale = SpecProps.f(p, "subScale", 0.42f);
        boolean pixel = "pixel".equals(SpecProps.str(p, "style", "led"));
        boolean ghost = SpecProps.b(p, "ghost", false);
        int[] cols = ClockText.colors(p, "colors", ctx.theme, new int[]{ctx.theme.textPrimary});
        int ghostColor = ClockText.color(p, "ghostColor", ctx.theme, (cols[0] & 0x00FFFFFF) | 0x1F000000);
        int subColor = ClockText.color(p, "subColor", ctx.theme, cols[0]);

        // Dot pitch: fit the main line (and the sub line at subScale) into the frame.
        int mainCols = columns(main);
        int subCols = sub.isEmpty() ? 0 : columns(sub);
        float subRows = sub.isEmpty() ? 0f : (subDots ? 7f * subScale : 7f * subScale * 1.15f) + 2.5f;
        float pitch = Math.min(b.width() * 0.96f / Math.max(mainCols, subDots ? subCols * subScale : 0f),
                b.height() * 0.92f / (7f + subRows));
        float blockH = (7f + subRows) * pitch;
        float top = b.centerY() - blockH / 2f;
        float mainTop = subTop && !sub.isEmpty() ? top + subRows * pitch : top;

        drawDots(canvas, main, b.centerX() - mainCols * pitch / 2f, mainTop, pitch, pixel, ghost,
                cols, ghostColor);

        if (!sub.isEmpty()) {
            float subTopY = subTop ? top : mainTop + 7f * pitch + 2.5f * pitch;
            if (subDots) {
                float sp = pitch * subScale;
                drawDots(canvas, sub, b.centerX() - subCols * sp / 2f, subTopY, sp, pixel, false,
                        new int[]{subColor}, 0);
            } else {
                Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
                tp.setTypeface(FontResolver.resolve(SpecProps.str(p, "font", "medium"), false));
                tp.setColor(subColor);
                tp.setTextAlign(Paint.Align.CENTER);
                tp.setLetterSpacing(SpecProps.f(p, "letterSpacing", 0.3f));
                tp.setTextSize(7f * subScale * pitch);
                float w = tp.measureText(sub);
                if (w > b.width() * 0.96f) tp.setTextSize(tp.getTextSize() * b.width() * 0.96f / w);
                Paint.FontMetrics fm = tp.getFontMetrics();
                float lineH = 7f * subScale * 1.15f * pitch;
                canvas.drawText(sub, b.centerX(), subTopY + lineH / 2f - (fm.ascent + fm.descent) / 2f, tp);
            }
        }
    }

    private String format(Calendar cal, String fmt) {
        try {
            SimpleDateFormat f = new SimpleDateFormat(fmt, Locale.getDefault());
            f.setTimeZone(cal.getTimeZone());
            return f.format(cal.getTime()).toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            return "";
        }
    }

    /** Matrix width of a string: glyph widths plus one blank column between glyphs. */
    private int columns(String text) {
        int n = 0;
        for (int i = 0; i < text.length(); i++) {
            String[] g = glyph(text.charAt(i));
            n += g[0].length() + (i < text.length() - 1 ? 1 : 0);
        }
        return Math.max(1, n);
    }

    private String[] glyph(char c) {
        String[] g = FONT.get(Character.toUpperCase(c));
        return g != null ? g : FONT.get(' ');
    }

    private void drawDots(Canvas c, String text, float left, float top, float pitch, boolean pixel,
                          boolean ghost, int[] cols, int ghostColor) {
        int total = columns(text);
        Paint on = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint off = new Paint(Paint.ANTI_ALIAS_FLAG);
        off.setColor(ghostColor);
        float r = pitch * 0.4f;
        float half = pitch * 0.45f;
        int col = 0;
        for (int i = 0; i < text.length(); i++) {
            String[] g = glyph(text.charAt(i));
            int w = g[0].length();
            for (int gx = 0; gx < w; gx++) {
                on.setColor(colorAt(cols, total <= 1 ? 0f : (col + gx) / (float) (total - 1)));
                float cx = left + (col + gx + 0.5f) * pitch;
                for (int gy = 0; gy < 7; gy++) {
                    boolean lit = g[gy].charAt(gx) == '1';
                    if (!lit && !ghost) continue;
                    float cy = top + (gy + 0.5f) * pitch;
                    Paint paint = lit ? on : off;
                    if (pixel) c.drawRect(cx - half, cy - half, cx + half, cy + half, paint);
                    else c.drawCircle(cx, cy, r, paint);
                }
            }
            col += w + 1;
        }
    }

    private static int colorAt(int[] cols, float t) {
        if (cols.length == 1) return cols[0];
        float f = Math.max(0f, Math.min(1f, t)) * (cols.length - 1);
        int i = Math.min(cols.length - 2, (int) f);
        float u = f - i;
        int a = cols[i], b = cols[i + 1];
        int ca = (int) (((a >>> 24) & 0xFF) + (((b >>> 24) & 0xFF) - ((a >>> 24) & 0xFF)) * u);
        int cr = (int) (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * u);
        int cg = (int) (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * u);
        int cb = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * u);
        return (ca << 24) | (cr << 16) | (cg << 8) | cb;
    }
}
