package com.desire.widget.engine.util;

import java.util.Map;

/**
 * Typed, null-safe accessors over a component's free-form {@code props} map. Gson deserializes
 * JSON numbers as Double, so these helpers normalize Number/String inputs to the requested type.
 */
public final class SpecProps {
    private SpecProps() {}

    public static String str(Map<String, Object> p, String key, String def) {
        if (p == null) return def;
        Object v = p.get(key);
        return v != null ? String.valueOf(v) : def;
    }

    public static float f(Map<String, Object> p, String key, float def) {
        if (p == null) return def;
        Object v = p.get(key);
        if (v instanceof Number) return ((Number) v).floatValue();
        try {
            return v != null ? Float.parseFloat(v.toString()) : def;
        } catch (Exception e) {
            return def;
        }
    }

    public static int i(Map<String, Object> p, String key, int def) {
        if (p == null) return def;
        Object v = p.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return v != null ? Integer.parseInt(v.toString()) : def;
        } catch (Exception e) {
            return def;
        }
    }

    public static boolean b(Map<String, Object> p, String key, boolean def) {
        if (p == null) return def;
        Object v = p.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        return v != null ? Boolean.parseBoolean(v.toString()) : def;
    }
}
