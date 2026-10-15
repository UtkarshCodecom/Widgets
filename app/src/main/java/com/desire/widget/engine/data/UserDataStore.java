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
        /** Stable identity, so a tap on a widget row toggles the right task even if the list changed. */
        public long id;
        public String text;
        public boolean done;
        public Todo() {}
        public Todo(String text, boolean done) { this.text = text; this.done = done; }
    }

    /** Sticky-note paper colours offered by the Notes editor ("" = the widget's own background). */
    public static final String[] NOTE_PAPERS = {
            "", "#FFE58F", "#FFB8CB", "#B9F2D3", "#BFE2FF", "#D8C8FF", "#FFD2AE", "#2A2D33"
    };
    public static final String[] NOTE_PAPER_NAMES = {
            "Default", "Lemon", "Rose", "Mint", "Sky", "Lilac", "Peach", "Charcoal"
    };
    /** Note text size steps: small / medium / large. */
    public static final float[] NOTE_SIZES = {0.85f, 1f, 1.22f};

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
        prefs(c).edit()
                .putString("notes_" + id, notes != null ? notes : "")
                .putLong("notes_updated_" + id, System.currentTimeMillis())
                .apply();
    }

    /** When the note was last edited (0 = never / preview). */
    public static long getNotesUpdatedAt(Context c, int id) {
        if (id < 0) return 0L;
        return prefs(c).getLong("notes_updated_" + id, 0L);
    }

    /** Paper colour hex chosen in the editor, or "" for the widget's own background. */
    public static String getNotePaper(Context c, int id) {
        if (id < 0) return "";
        return prefs(c).getString("notes_paper_" + id, "");
    }

    public static void setNotePaper(Context c, int id, String hex) {
        if (id < 0) return;
        prefs(c).edit().putString("notes_paper_" + id, hex != null ? hex : "").apply();
    }

    /** Index into {@link #NOTE_SIZES}; medium by default. */
    public static int getNoteSize(Context c, int id) {
        if (id < 0) return 1;
        return Math.max(0, Math.min(NOTE_SIZES.length - 1, prefs(c).getInt("notes_size_" + id, 1)));
    }

    public static void setNoteSize(Context c, int id, int sizeIndex) {
        if (id < 0) return;
        prefs(c).edit().putInt("notes_size_" + id, sizeIndex).apply();
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
            sample.add(new Todo("Gym — leg day", false));
            sample.add(new Todo("Pay electricity bill", true));
            sample.add(new Todo("Book train tickets", false));
            assignIds(sample);
            return sample;
        }
        String json = prefs(c).getString("todos_" + id, null);
        if (json == null) return new ArrayList<>();
        try {
            List<Todo> list = GSON.fromJson(json, new TypeToken<List<Todo>>(){}.getType());
            if (list == null) return new ArrayList<>();
            assignIds(list);
            return list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /** Gives tasks saved before ids existed (or just added) a unique id. */
    public static void assignIds(List<Todo> list) {
        long max = 0;
        for (Todo t : list) max = Math.max(max, t.id);
        for (Todo t : list) if (t.id == 0) t.id = ++max;
    }

    /** Flips one task by id (used by the tappable widget rows). Returns false if it's gone. */
    public static synchronized boolean toggleTodo(Context c, int widgetId, long todoId) {
        if (widgetId < 0) return false;
        List<Todo> list = getTodos(c, widgetId);
        for (Todo t : list) {
            if (t.id == todoId) {
                t.done = !t.done;
                prefs(c).edit().putString("todos_" + widgetId, GSON.toJson(list)).commit();
                return true;
            }
        }
        return false;
    }

    public static void setTodos(Context c, int id, List<Todo> todos) {
        if (id < 0) return;
        assignIds(todos);
        prefs(c).edit().putString("todos_" + id, GSON.toJson(todos)).apply();
    }

    /** Called when a widget is removed, so its data doesn't linger. */
    public static void clear(Context c, int id) {
        prefs(c).edit()
                .remove("notes_" + id)
                .remove("qr_" + id)
                .remove("todos_" + id)
                .remove("notes_updated_" + id)
                .remove("notes_paper_" + id)
                .remove("notes_size_" + id)
                .apply();
    }
}
