package com.desire.widget.engine.util;

import android.text.format.DateFormat;

import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.RenderTheme;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Shared helpers for the typographic clocks: the current time in an optional {@code timezone},
 * the user's 12/24-hour preference (overridable with {@code use24h}), zero-padded parts, a
 * "+5:30"-style offset label for world clocks, and colour-list props.
 */
public final class ClockText {
    private ClockText() {}

    public static Calendar now(RenderContext ctx, Map<String, Object> p) {
        String tz = SpecProps.str(p, "timezone", "");
        Calendar cal = tz != null && !tz.isEmpty()
                ? Calendar.getInstance(TimeZone.getTimeZone(tz))
                : Calendar.getInstance();
        cal.setTimeInMillis(ctx.nowMillis);
        return cal;
    }

    public static boolean use24h(RenderContext ctx, Map<String, Object> p) {
        return p != null && p.containsKey("use24h")
                ? SpecProps.b(p, "use24h", true)
                : DateFormat.is24HourFormat(ctx.android);
    }

    /** {hh, mm} — hour in the chosen 12/24h mode, both zero-padded. */
    public static String[] hhmm(Calendar cal, boolean use24) {
        int hour = use24 ? cal.get(Calendar.HOUR_OF_DAY) : cal.get(Calendar.HOUR);
        if (!use24 && hour == 0) hour = 12;
        return new String[]{
                String.format(Locale.US, "%02d", hour),
                String.format(Locale.US, "%02d", cal.get(Calendar.MINUTE))};
    }

    public static String amPm(Calendar cal) {
        return cal.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
    }

    /** Offset of {@code tz} relative to the device zone, e.g. "+5:30", "-3", "" when equal. */
    public static String offsetLabel(String tz, long now) {
        if (tz == null || tz.isEmpty()) return "";
        int diffMin = (TimeZone.getTimeZone(tz).getOffset(now) - TimeZone.getDefault().getOffset(now)) / 60000;
        if (diffMin == 0) return "";
        String sign = diffMin > 0 ? "+" : "-";
        int a = Math.abs(diffMin);
        return a % 60 == 0 ? sign + (a / 60) : String.format(Locale.US, "%s%d:%02d", sign, a / 60, a % 60);
    }

    /** A colour-list prop ("colors": ["#fff", "@primary"]) resolved to ints, or {@code def}. */
    public static int[] colors(Map<String, Object> p, String key, RenderTheme theme, int[] def) {
        Object v = p != null ? p.get(key) : null;
        if (!(v instanceof List)) return def;
        List<Integer> out = new ArrayList<>();
        for (Object o : (List<?>) v) {
            if (o == null) continue;
            out.add(SpecColors.resolve(String.valueOf(o), theme, 0xFFFFFFFF));
        }
        if (out.isEmpty()) return def;
        int[] arr = new int[out.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = out.get(i);
        return arr;
    }

    public static int color(Map<String, Object> p, String key, RenderTheme theme, int def) {
        String v = SpecProps.str(p, key, "");
        return v == null || v.isEmpty() ? def : SpecColors.resolve(v, theme, def);
    }
}
