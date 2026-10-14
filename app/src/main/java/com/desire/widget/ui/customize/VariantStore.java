package com.desire.widget.ui.customize;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/**
 * The user's saved looks for each catalog widget ("ADD NEW" in the editor), shown as variant
 * cards on the widget page. Stored per catalog widget id as a JSON list in SharedPreferences.
 */
public final class VariantStore {
    private static final String PREFS = "widget_variants";
    private static final Gson GSON = new Gson();

    private VariantStore() {}

    public static class Variant {
        public String id;
        public String name;
        public String styleJson;
        public long createdAt;
    }

    public static List<Variant> list(Context c, String widgetId) {
        if (widgetId == null) return new ArrayList<>();
        String json = prefs(c).getString(widgetId, null);
        if (json == null) return new ArrayList<>();
        try {
            List<Variant> out = GSON.fromJson(json, new TypeToken<List<Variant>>() {}.getType());
            return out != null ? out : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /** Saves a new look and returns it (newest first in the list). */
    public static Variant add(Context c, String widgetId, String styleJson) {
        List<Variant> all = list(c, widgetId);
        Variant v = new Variant();
        v.createdAt = System.currentTimeMillis();
        v.id = "v" + v.createdAt;
        v.name = "My style " + (all.size() + 1);
        v.styleJson = styleJson;
        all.add(0, v);
        save(c, widgetId, all);
        return v;
    }

    public static void update(Context c, String widgetId, String variantId, String styleJson) {
        List<Variant> all = list(c, widgetId);
        for (Variant v : all) if (v.id.equals(variantId)) v.styleJson = styleJson;
        save(c, widgetId, all);
    }

    public static void rename(Context c, String widgetId, String variantId, String name) {
        List<Variant> all = list(c, widgetId);
        for (Variant v : all) if (v.id.equals(variantId)) v.name = name;
        save(c, widgetId, all);
    }

    public static void delete(Context c, String widgetId, String variantId) {
        List<Variant> all = list(c, widgetId);
        List<Variant> keep = new ArrayList<>();
        for (Variant v : all) if (!v.id.equals(variantId)) keep.add(v);
        save(c, widgetId, keep);
    }

    private static void save(Context c, String widgetId, List<Variant> all) {
        prefs(c).edit().putString(widgetId, GSON.toJson(all)).apply();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
