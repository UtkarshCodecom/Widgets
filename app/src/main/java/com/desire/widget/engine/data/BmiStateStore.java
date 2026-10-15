package com.desire.widget.engine.data;

import android.content.Context;
import android.content.SharedPreferences;

/** Per-widget-instance height/weight for the BMI widget. */
public final class BmiStateStore {
    private static final String PREFS = "bmi_state";

    private BmiStateStore() {}

    public static float getHeightCm(Context c, int id) {
        return id < 0 ? 170f : prefs(c).getFloat("height_" + id, 0f);
    }

    public static float getWeightKg(Context c, int id) {
        return id < 0 ? 68f : prefs(c).getFloat("weight_" + id, 0f);
    }

    public static void set(Context c, int id, float heightCm, float weightKg) {
        if (id < 0) return;
        prefs(c).edit().putFloat("height_" + id, heightCm).putFloat("weight_" + id, weightKg).apply();
    }

    public static void clear(Context c, int id) {
        prefs(c).edit().remove("height_" + id).remove("weight_" + id).apply();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
