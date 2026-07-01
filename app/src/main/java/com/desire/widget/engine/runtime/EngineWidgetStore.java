package com.desire.widget.engine.runtime;

import android.content.Context;
import android.content.SharedPreferences;

import com.desire.widget.widget.WidgetSchema;

import java.util.ArrayList;
import java.util.List;

/**
 * Persists which native {@code WidgetSpec} (JSON) is bound to each installed appWidgetId, plus a
 * "pending" slot used while the launcher's pin flow is in flight. This is the native replacement
 * for the HTML-based WidgetPrefsHelper.
 */
public final class EngineWidgetStore {
    private static final String PREFS = "engine_widget_store";
    private static final String P_ID = "pending_id";
    private static final String P_NAME = "pending_name";
    private static final String P_SPEC = "pending_spec";
    private static final String P_SIZE = "pending_size";
    private static final String P_STYLE = "pending_style";

    private EngineWidgetStore() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String key(int appWidgetId, String suffix) {
        return "w_" + appWidgetId + "_" + suffix;
    }

    public static void savePending(Context c, String widgetId, String name, String specJson, String size) {
        savePending(c, widgetId, name, specJson, size, null);
    }

    public static void savePending(Context c, String widgetId, String name, String specJson,
                                   String size, String styleJson) {
        prefs(c).edit()
                .putString(P_ID, widgetId)
                .putString(P_NAME, name != null ? name : "Widget")
                .putString(P_SPEC, specJson != null ? specJson : "")
                .putString(P_SIZE, WidgetSchema.normalizeSize(size))
                .putString(P_STYLE, styleJson != null ? styleJson : "")
                .apply();
    }

    public static void commitPending(Context c, int appWidgetId) {
        SharedPreferences p = prefs(c);
        saveWidget(c, appWidgetId,
                p.getString(P_ID, ""),
                p.getString(P_NAME, "Widget"),
                p.getString(P_SPEC, ""),
                p.getString(P_SIZE, WidgetSchema.SIZE_2X2),
                p.getString(P_STYLE, ""));
    }

    public static void saveWidget(Context c, int appWidgetId, String widgetId, String name,
                                  String specJson, String size) {
        saveWidget(c, appWidgetId, widgetId, name, specJson, size, null);
    }

    public static void saveWidget(Context c, int appWidgetId, String widgetId, String name,
                                  String specJson, String size, String styleJson) {
        prefs(c).edit()
                .putString(key(appWidgetId, "id"), widgetId)
                .putString(key(appWidgetId, "name"), name)
                .putString(key(appWidgetId, "spec"), specJson != null ? specJson : "")
                .putString(key(appWidgetId, "size"), WidgetSchema.normalizeSize(size))
                .putString(key(appWidgetId, "style"), styleJson != null ? styleJson : "")
                .apply();
    }

    public static String getStyleJson(Context c, int appWidgetId) {
        return prefs(c).getString(key(appWidgetId, "style"), "");
    }

    public static String getSpecJson(Context c, int appWidgetId) {
        return prefs(c).getString(key(appWidgetId, "spec"), "");
    }

    public static String getSize(Context c, int appWidgetId) {
        return WidgetSchema.normalizeSize(prefs(c).getString(key(appWidgetId, "size"), WidgetSchema.SIZE_2X2));
    }

    public static String getName(Context c, int appWidgetId) {
        return prefs(c).getString(key(appWidgetId, "name"), "Widget");
    }

    public static void clear(Context c, int appWidgetId) {
        prefs(c).edit()
                .remove(key(appWidgetId, "id"))
                .remove(key(appWidgetId, "name"))
                .remove(key(appWidgetId, "spec"))
                .remove(key(appWidgetId, "size"))
                .remove(key(appWidgetId, "style"))
                .apply();
    }

    public static int[] allPlacedIds(Context c) {
        SharedPreferences p = prefs(c);
        List<Integer> ids = new ArrayList<>();
        for (String k : p.getAll().keySet()) {
            if (k.startsWith("w_") && k.endsWith("_spec")) {
                try {
                    int id = Integer.parseInt(k.substring(2, k.length() - "_spec".length()));
                    if (!p.getString(k, "").isEmpty()) ids.add(id);
                } catch (NumberFormatException ignored) {}
            }
        }
        int[] out = new int[ids.size()];
        for (int i = 0; i < out.length; i++) out[i] = ids.get(i);
        return out;
    }
}
