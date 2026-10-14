package com.desire.widget.ui.customize;

import android.content.Context;
import android.content.SharedPreferences;

import com.desire.widget.engine.model.WidgetStyle;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Built-in style presets (each a genuinely different look) plus user-saved custom styles.
 * A preset applies a set of {@link WidgetStyle} fields on top of the current style.
 */
public final class StylePresets {
    private static final String PREFS = "custom_styles";
    private static final Gson GSON = new Gson();

    private StylePresets() {}

    public static class Preset {
        public final String name;
        public final WidgetStyle style;
        public final boolean custom;
        Preset(String name, WidgetStyle style, boolean custom) {
            this.name = name;
            this.style = style;
            this.custom = custom;
        }
    }

    /** Distinct built-in looks. "Default" clears overrides back to the design. */
    public static List<Preset> builtins() {
        List<Preset> list = new ArrayList<>();
        list.add(new Preset("Default", new WidgetStyle(), false));

        WidgetStyle card = new WidgetStyle();
        card.backgroundMode = "solid";
        card.backgroundColor = "#181820";
        card.cornerRadius = 0.28f;
        list.add(new Preset("Card", card, false));

        WidgetStyle glass = new WidgetStyle();
        glass.backgroundMode = "solid";
        glass.backgroundColor = "#40FFFFFF";
        glass.backgroundOpacity = 0.35f;
        glass.cornerRadius = 0.22f;
        list.add(new Preset("Glass", glass, false));

        WidgetStyle mono = new WidgetStyle();
        mono.backgroundMode = "solid";
        mono.backgroundColor = "#0A0A0A";
        mono.fontFamily = "monospace";
        mono.textColor = "#00E676";
        list.add(new Preset("Mono", mono, false));

        WidgetStyle sunset = new WidgetStyle();
        sunset.backgroundMode = "gradient";
        sunset.backgroundColor = "#FF512F";
        sunset.gradientEndColor = "#DD2476";
        sunset.gradientAngle = 135f;
        sunset.textColor = "#FFFFFF";
        list.add(new Preset("Sunset", sunset, false));

        WidgetStyle ocean = new WidgetStyle();
        ocean.backgroundMode = "gradient";
        ocean.backgroundColor = "#2980B9";
        ocean.gradientEndColor = "#141E30";
        ocean.gradientAngle = 160f;
        ocean.textColor = "#FFFFFF";
        list.add(new Preset("Ocean", ocean, false));

        return list;
    }

    public static List<Preset> custom(Context ctx) {
        List<Preset> list = new ArrayList<>();
        Map<String, String> saved = read(ctx);
        for (Map.Entry<String, String> e : saved.entrySet()) {
            try {
                WidgetStyle s = GSON.fromJson(e.getValue(), WidgetStyle.class);
                if (s != null) list.add(new Preset(e.getKey(), s, true));
            } catch (Exception ignored) {}
        }
        return list;
    }

    public static void saveCustom(Context ctx, String name, WidgetStyle style) {
        Map<String, String> saved = read(ctx);
        // Don't persist per-widget-specific bits (photo/city) in a reusable style.
        WidgetStyle copy = GSON.fromJson(GSON.toJson(style), WidgetStyle.class);
        copy.photoUri = null;
        copy.cityName = null;
        copy.tapPackage = null;
        saved.put(name, GSON.toJson(copy));
        prefs(ctx).edit().putString("map", GSON.toJson(saved)).apply();
    }

    public static void deleteCustom(Context ctx, String name) {
        Map<String, String> saved = read(ctx);
        saved.remove(name);
        prefs(ctx).edit().putString("map", GSON.toJson(saved)).apply();
    }

    private static Map<String, String> read(Context ctx) {
        String json = prefs(ctx).getString("map", null);
        if (json == null) return new LinkedHashMap<>();
        try {
            Map<String, String> m = GSON.fromJson(json, new TypeToken<LinkedHashMap<String, String>>(){}.getType());
            return m != null ? m : new LinkedHashMap<>();
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
