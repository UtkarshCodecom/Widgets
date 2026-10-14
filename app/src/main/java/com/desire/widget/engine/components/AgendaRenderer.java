package com.desire.widget.engine.components;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.format.DateFormat;

import androidx.core.content.ContextCompat;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Upcoming calendar events (next 7 days) from the system calendar provider: a date header and
 * rows with the event's calendar colour, title and time. Needs READ_CALENDAR; until granted a
 * placed widget asks the user to tap (WidgetTapActivity routes that to the permission prompt),
 * while gallery previews show sample events.
 *
 * <p>props: title, maxEvents, days, accentColor, textColor, subColor.
 */
public class AgendaRenderer implements ComponentRenderer {
    private static final class Event {
        final String title;
        final long begin, end;
        final boolean allDay;
        final int color;

        Event(String title, long begin, long end, boolean allDay, int color) {
            this.title = title;
            this.begin = begin;
            this.end = end;
            this.allDay = allDay;
            this.color = color;
        }
    }

    @Override
    public String type() {
        return "agenda";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        int accent = SpecColors.resolve(SpecProps.str(p, "accentColor", "@primary"), ctx.theme, ctx.theme.primary);
        int textColor = SpecColors.resolve(SpecProps.str(p, "textColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        int subColor = SpecColors.resolve(SpecProps.str(p, "subColor", "@textSecondary"), ctx.theme, ctx.theme.textSecondary);
        int maxEvents = Math.max(1, SpecProps.i(p, "maxEvents", 4));
        int days = Math.max(1, SpecProps.i(p, "days", 7));

        // Header: weekday + date.
        Date today = new Date(ctx.nowMillis);
        Paint day = paint(accent, b.height() * 0.085f, true, Paint.Align.LEFT);
        canvas.drawText(new SimpleDateFormat("EEEE", Locale.getDefault()).format(today).toUpperCase(Locale.getDefault()),
                b.left, b.top + b.height() * 0.09f, day);
        Paint date = paint(textColor, b.height() * 0.15f, true, Paint.Align.LEFT);
        canvas.drawText(new SimpleDateFormat("d MMMM", Locale.getDefault()).format(today),
                b.left, b.top + b.height() * 0.24f, date);

        List<Event> events;
        boolean granted = ContextCompat.checkSelfPermission(ctx.android, Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
        if (granted) {
            events = query(ctx.android, ctx.nowMillis, days, maxEvents);
        } else if (ctx.appWidgetId < 0) {
            events = sample(ctx.nowMillis, accent);
        } else {
            Paint t = paint(textColor, b.height() * 0.09f, true, Paint.Align.LEFT);
            canvas.drawText("Show your upcoming events", b.left, b.top + b.height() * 0.48f, t);
            Paint a = paint(accent, b.height() * 0.075f, true, Paint.Align.LEFT);
            canvas.drawText("Tap to connect your calendar", b.left, b.top + b.height() * 0.60f, a);
            return;
        }

        float listTop = b.top + b.height() * 0.33f;
        if (events.isEmpty()) {
            Paint none = paint(subColor, b.height() * 0.085f, false, Paint.Align.LEFT);
            canvas.drawText("No events in the next " + days + " days", b.left, listTop + b.height() * 0.12f, none);
            return;
        }
        int rows = Math.min(events.size(), maxEvents);
        float rowH = Math.min((b.bottom - listTop) / rows, b.height() * 0.20f);
        boolean use24 = DateFormat.is24HourFormat(ctx.android);
        for (int i = 0; i < rows; i++) {
            Event e = events.get(i);
            float top = listTop + i * rowH;
            float pad = rowH * 0.12f;

            Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
            bar.setColor(e.color);
            float barW = Math.max(3f, rowH * 0.07f);
            canvas.drawRoundRect(new RectF(b.left, top + pad, b.left + barW, top + rowH - pad), barW, barW, bar);

            float x = b.left + barW * 3f;
            Paint title = paint(textColor, rowH * 0.34f, true, Paint.Align.LEFT);
            canvas.drawText(ellipsize(e.title, title, b.right - x), x, top + rowH * 0.46f, title);
            Paint when = paint(subColor, rowH * 0.27f, false, Paint.Align.LEFT);
            canvas.drawText(whenLabel(e, ctx.nowMillis, use24), x, top + rowH * 0.82f, when);
        }
    }

    private List<Event> query(Context c, long now, int days, int max) {
        List<Event> out = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        long end = start + days * 24L * 3600_000L;

        Uri.Builder ub = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(ub, start);
        ContentUris.appendId(ub, end);
        String[] proj = {
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.DISPLAY_COLOR,
        };
        // Still-running or future events only; hidden calendars excluded.
        String sel = CalendarContract.Instances.END + " > ? AND " + CalendarContract.Instances.VISIBLE + " = 1";
        try (Cursor cur = c.getContentResolver().query(ub.build(), proj, sel,
                new String[]{String.valueOf(now)}, CalendarContract.Instances.BEGIN + " ASC")) {
            while (cur != null && cur.moveToNext() && out.size() < max) {
                String title = cur.getString(0);
                out.add(new Event(title == null || title.trim().isEmpty() ? "(No title)" : title,
                        cur.getLong(1), cur.getLong(2), cur.getInt(3) == 1, 0xFF000000 | cur.getInt(4)));
            }
        } catch (Exception ignored) {
            // Permission revoked between check and query, provider missing, etc.
        }
        return out;
    }

    private List<Event> sample(long now, int accent) {
        List<Event> out = new ArrayList<>();
        long h = 3600_000L;
        out.add(new Event("Team stand-up", now + h, now + h + h / 2, false, accent));
        out.add(new Event("Lunch with Sam", now + 3 * h, now + 4 * h, false, 0xFF26C6DA));
        out.add(new Event("Gym", now + 26 * h, now + 27 * h, false, 0xFFFF7043));
        out.add(new Event("Mom's birthday", now + 48 * h, now + 72 * h, true, 0xFFAB47BC));
        return out;
    }

    private String whenLabel(Event e, long now, boolean use24) {
        String day = dayLabel(now, e.begin);
        if (e.allDay) return day + " · All day";
        SimpleDateFormat f = new SimpleDateFormat(use24 ? "HH:mm" : "h:mm a", Locale.getDefault());
        if (e.begin <= now && e.end > now) return "Now · until " + f.format(new Date(e.end));
        return day + " · " + f.format(new Date(e.begin)) + " – " + f.format(new Date(e.end));
    }

    private String dayLabel(long now, long t) {
        Calendar a = Calendar.getInstance();
        a.setTimeInMillis(now);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        int diff = (c.get(Calendar.YEAR) - a.get(Calendar.YEAR)) * 366
                + c.get(Calendar.DAY_OF_YEAR) - a.get(Calendar.DAY_OF_YEAR);
        if (diff <= 0) return "Today";
        if (diff == 1) return "Tomorrow";
        return new SimpleDateFormat("EEE d", Locale.getDefault()).format(new Date(t));
    }

    private Paint paint(int color, float size, boolean bold, Paint.Align align) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTextSize(size);
        p.setFakeBoldText(bold);
        p.setTextAlign(align);
        return p;
    }

    private String ellipsize(String s, Paint p, float maxW) {
        if (maxW <= 0) return "";
        if (p.measureText(s) <= maxW) return s;
        while (s.length() > 1 && p.measureText(s + "…") > maxW) s = s.substring(0, s.length() - 1);
        return s + "…";
    }
}
