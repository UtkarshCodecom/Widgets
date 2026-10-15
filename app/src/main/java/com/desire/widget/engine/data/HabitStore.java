package com.desire.widget.engine.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Per-widget-instance habit list. Each habit stores the SET of "yyyy-MM-dd" dates it was completed
 * on (not a manually-shifted array), so a rolling "last 7 days" view is always correct without any
 * background job to age the data.
 */
public final class HabitStore {
    private static final String PREFS = "habit_state";
    private static final Gson GSON = new Gson();
    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private HabitStore() {}

    public static class Habit {
        public String name;
        public Set<String> doneDates = new LinkedHashSet<>();
        public Habit() {}
        public Habit(String name) { this.name = name; }
    }

    public static String todayKey(long nowMillis) {
        return DAY_FORMAT.format(new Date(nowMillis));
    }

    public static List<Habit> getHabits(Context c, int id) {
        if (id < 0) {
            List<Habit> sample = new ArrayList<>();
            Habit h1 = new Habit("Drink water");
            h1.doneDates.add(todayKey(System.currentTimeMillis()));
            sample.add(h1);
            sample.add(new Habit("Read 10 pages"));
            return sample;
        }
        String json = prefs(c).getString("habits_" + id, null);
        if (json == null) return new ArrayList<>();
        try {
            List<Habit> list = GSON.fromJson(json, new TypeToken<List<Habit>>(){}.getType());
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void setHabits(Context c, int id, List<Habit> habits) {
        if (id < 0) return;
        prefs(c).edit().putString("habits_" + id, GSON.toJson(habits)).apply();
    }

    public static void clear(Context c, int id) {
        prefs(c).edit().remove("habits_" + id).apply();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
