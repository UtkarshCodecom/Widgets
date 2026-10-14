package com.desire.widget.engine.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-widget-instance user content (notes / to-dos / QR), keyed by {@code appWidgetId} so every
 * placed widget keeps its own independent data — editing one note never touches another. Backed by
 * SharedPreferences and shared by the widget renderers and the in-app editors.
 *
 * <p>{@code appWidgetId < 0} means a gallery preview: getters return friendly sample content and
 * setters are no-ops.
 */
public final class UserDataStore {
    private static final String PREFS = "user_data";
    private static final Gson GSON = new Gson();

    private UserDataStore() {}

    public static class Todo {
        public String text;
        public boolean done;
        public Todo() {}
        public Todo(String text, boolean done) { this.text = text; this.done = done; }
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---- Notes ----
    public static String getNotes(Context c, int id) {
        if (id < 0) return "Your note appears here.\nTap the widget to edit.";
        return prefs(c).getString("notes_" + id, "");
    }

    public static void setNotes(Context c, int id, String notes) {
        if (id < 0) return;
        prefs(c).edit().putString("notes_" + id, notes != null ? notes : "").apply();
    }

    // ---- QR ----
    public static String getQr(Context c, int id) {
        if (id < 0) return "https://example.com";
        return prefs(c).getString("qr_" + id, "https://example.com");
    }

    public static void setQr(Context c, int id, String data) {
        if (id < 0) return;
        prefs(c).edit().putString("qr_" + id, data != null ? data : "").apply();
    }

    // ---- To-do ----
    public static List<Todo> getTodos(Context c, int id) {
        if (id < 0) {
            List<Todo> sample = new ArrayList<>();
            sample.add(new Todo("Buy groceries", false));
            sample.add(new Todo("Meeting at 7 PM", true));
            sample.add(new Todo("Call Rahul", false));
            return sample;
        }
        String json = prefs(c).getString("todos_" + id, null);
        if (json == null) return new ArrayList<>();
        try {
            List<Todo> list = GSON.fromJson(json, new TypeToken<List<Todo>>(){}.getType());
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void setTodos(Context c, int id, List<Todo> todos) {
        if (id < 0) return;
        prefs(c).edit().putString("todos_" + id, GSON.toJson(todos)).apply();
    }

    /** Called when a widget is removed, so its data doesn't linger. */
    public static void clear(Context c, int id) {
        prefs(c).edit()
                .remove("notes_" + id)
                .remove("qr_" + id)
                .remove("todos_" + id)
                .apply();
    }
}
