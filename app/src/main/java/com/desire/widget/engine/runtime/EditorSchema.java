package com.desire.widget.engine.runtime;

import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.model.WidgetSpec;
import com.desire.widget.engine.util.SpecProps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Works out the widget-specific editor tabs for a design, Muviz style: a "Clock Style" tab with
 * Style 1…N looks, and one colour tab per meaningful part ("Hour Color", "Minute Color",
 * "Hand Color"…) instead of generic Color 1/2/3.
 *
 * <p>Every edit is a prop override keyed {@code "<componentIndex>.<prop>"} (stored in
 * {@code WidgetStyle.props} and applied by {@link StyleApplier}), so this works for any design
 * without editing its JSON. A design can also declare its own looks:
 * <pre>"styles": [ { "name": "Neon", "props": { "0.color": "#39FF14", "1.style": "outline" } } ]</pre>
 */
public final class EditorSchema {
    private static final com.google.gson.Gson GSON = new com.google.gson.Gson();

    private EditorSchema() {}

    /** One "Style N" look: the prop overrides that produce it. */
    public static final class Option {
        public final String name;
        public final Map<String, Object> props;

        Option(String name, Map<String, Object> props) {
            this.name = name;
            this.props = props;
        }
    }

    /** The "Clock Style" tab: a label and its looks. {@link #keys} = every key any option sets. */
    public static final class StyleAxis {
        public final String label;
        public final List<Option> options;
        public final Set<String> keys = new LinkedHashSet<>();

        StyleAxis(String label, List<Option> options) {
            this.label = label;
            this.options = options;
            for (Option o : options) keys.addAll(o.props.keySet());
        }
    }

    /** One colour tab: which props it recolours and whether gradients are supported there. */
    public static final class ColorSlot {
        public final String label;
        public final List<String> keys = new ArrayList<>();
        public boolean gradients;
        /** The design's own value for the first target (null = renderer default). */
        public String designValue;

        ColorSlot(String label) {
            this.label = label;
        }
    }

    private static final int MAX_COLOR_SLOTS = 4;

    // ------------------------------------------------------------------ style axis

    public static StyleAxis styleAxis(WidgetSpec spec, String rawJson) {
        StyleAxis declared = declaredStyles(rawJson);
        if (declared != null) return declared;
        if (spec == null || spec.components == null) return null;

        for (int i = 0; i < spec.components.size(); i++) {
            ComponentSpec c = spec.components.get(i);
            if (c == null || c.type == null) continue;
            String k = i + ".";
            switch (c.type) {
                case "stacked_clock":
                    return new StyleAxis("Clock Style", list(
                            opt("Style 1", k + "hourStyle", "fill", k + "minuteStyle", "fill"),
                            opt("Style 2", k + "hourStyle", "outline", k + "minuteStyle", "outline"),
                            opt("Style 3", k + "hourStyle", "fill", k + "minuteStyle", "outline"),
                            opt("Style 4", k + "hourStyle", "outline", k + "minuteStyle", "fill")));
                case "styled_clock":
                    return numbered("Clock Style", k + "style",
                            "solid", "outline", "gradient", "holo", "stencil", "bubble", "candy");
                case "analog_clock":
                    return numbered("Dial Style", k + "dialStyle",
                            "classic", "numerals", "square", "pilot", "minimal", "filled",
                            "bold", "radial", "sport", "outline");
                case "dot_clock":
                    return numbered("Clock Style", k + "style", "led", "pixel");
                default:
                    break;
            }
        }

        // Digital clocks: filled or outlined digits on every time-showing clock.
        List<String> timeClocks = new ArrayList<>();
        List<String> toggles = new ArrayList<>();
        for (int i = 0; i < spec.components.size(); i++) {
            ComponentSpec c = spec.components.get(i);
            if (c == null || c.type == null) continue;
            if ("clock".equals(c.type) && isTimeFormat(SpecProps.str(c.props, "format", "HH:mm"))) {
                timeClocks.add(i + ".style");
            } else if ("toggle".equals(c.type)) {
                toggles.add(i + ".shape");
            }
        }
        if (!timeClocks.isEmpty()) {
            return new StyleAxis("Clock Style", list(
                    optAll("Style 1", timeClocks, "fill"),
                    optAll("Style 2", timeClocks, "outline")));
        }
        if (!toggles.isEmpty()) {
            return new StyleAxis("Button Style", list(
                    optAll("Style 1", toggles, "rounded"),
                    optAll("Style 2", toggles, "circle")));
        }
        return null;
    }

    private static StyleAxis declaredStyles(String rawJson) {
        if (rawJson == null || !rawJson.contains("\"styles\"")) return null;
        try {
            JsonObject root = JsonParser.parseString(rawJson).getAsJsonObject();
            if (!root.has("styles") || !root.get("styles").isJsonArray()) return null;
            JsonArray arr = root.getAsJsonArray("styles");
            List<Option> options = new ArrayList<>();
            int n = 0;
            for (JsonElement el : arr) {
                n++;
                if (!el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();
                String name = o.has("name") ? o.get("name").getAsString() : "Style " + n;
                Map<String, Object> props = new LinkedHashMap<>();
                if (o.has("props") && o.get("props").isJsonObject()) {
                    // Keep JSON types (Boolean / Double / List / String) exactly as declared.
                    for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("props").entrySet()) {
                        props.put(e.getKey(), GSON.fromJson(e.getValue(), Object.class));
                    }
                }
                options.add(new Option(name, props));
            }
            if (options.size() < 2) return null;
            String label = root.has("styleLabel") ? root.get("styleLabel").getAsString()
                    : rawJson.contains("clock\"") ? "Clock Style" : "Style";
            return new StyleAxis(label, options);
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ colour slots

    public static List<ColorSlot> colorSlots(WidgetSpec spec) {
        Map<String, ColorSlot> byLabel = new LinkedHashMap<>();
        if (spec == null || spec.components == null) return new ArrayList<>();
        for (int i = 0; i < spec.components.size(); i++) {
            ComponentSpec c = spec.components.get(i);
            if (c == null || c.type == null) continue;
            for (String[] part : partsOf(c)) {
                String prop = part[0], label = part[1];
                boolean gradient = part.length > 2;
                ColorSlot slot = byLabel.get(label);
                if (slot == null) {
                    if (byLabel.size() >= MAX_COLOR_SLOTS) continue;
                    slot = new ColorSlot(label);
                    slot.gradients = gradient;
                    Object v = c.props != null ? c.props.get(prop) : null;
                    slot.designValue = v instanceof String ? (String) v
                            : v instanceof List && !((List<?>) v).isEmpty() ? String.valueOf(((List<?>) v).get(0)) : null;
                    byLabel.put(label, slot);
                } else {
                    slot.gradients &= gradient;
                }
                slot.keys.add(i + "." + prop);
            }
        }
        return new ArrayList<>(byLabel.values());
    }

    /** {prop, label[, "g" = gradient-capable]} for the parts of a component worth recolouring. */
    private static List<String[]> partsOf(ComponentSpec c) {
        String fmt = SpecProps.str(c.props, "format", "HH:mm");
        switch (c.type) {
            case "clock":
                return Collections.singletonList(new String[]{"color", clockLabel(fmt), "g"});
            case "text":
            case "word_clock":
                return Collections.singletonList(new String[]{"color", "Text Color", "g"});
            case "stacked_clock":
                return list(new String[]{"hourColor", "Hour Color", "g"},
                        new String[]{"minuteColor", "Minute Color", "g"});
            case "styled_clock":
                return list(new String[]{"hourColors", "Hour Color", "g"},
                        new String[]{"minuteColors", "Minute Color", "g"});
            case "pie_clock":
                return list(new String[]{"hourColor", "Hour Color"},
                        new String[]{"minuteColor", "Minute Color"});
            case "analog_clock":
                return list(new String[]{"handColor", "Hand Color"},
                        new String[]{"secondColor", "Second Hand"},
                        new String[]{"faceColor", "Dial Color"});
            case "bezel_clock":
                return list(new String[]{"bezelColor", "Bezel Color"},
                        new String[]{"accentColor", "Accent Color"});
            case "flip_clock":
                return list(new String[]{"textColor", "Digit Color"},
                        new String[]{"cardColor", "Card Color"});
            case "countdown":
                return list(new String[]{"timeColor", "Time Color"},
                        new String[]{"accentColor", "Accent Color"});
            case "battery":
            case "device_battery":
                return list(new String[]{"fillColor", "Fill Color"},
                        new String[]{"textColor", "Text Color"});
            case "device_info":
                return list(new String[]{"memoryColor", "Memory Color"},
                        new String[]{"storageColor", "Storage Color"});
            case "progress_ring":
                return list(new String[]{"progressColor", "Ring Color"},
                        new String[]{"textColor", "Text Color"});
            case "music_visualizer":
                return Collections.singletonList(new String[]{"barColor", "Bar Color"});
            case "weather":
                return list(new String[]{"textColor", "Text Color"},
                        new String[]{"iconColor", "Icon Color"});
            case "moon_phase":
                return Collections.singletonList(new String[]{"litColor", "Moon Color"});
            case "pomodoro":
                return list(new String[]{"workColor", "Focus Color"},
                        new String[]{"breakColor", "Break Color"});
            case "toggle":
                return list(new String[]{"onColor", "On Color"},
                        new String[]{"offColor", "Off Color"});
            case "image":
            case "shape":
            case "divider":
            case "qr_code":
            case "time_tiles":
                return Collections.emptyList();
            default:
                // music, calendar, agenda, todo, notes, next_alarm, stopwatch, screen_time…
                return list(new String[]{"accentColor", "Accent Color"},
                        new String[]{"textColor", "Text Color"});
        }
    }

    // ------------------------------------------------------------------ helpers

    private static boolean isTimeFormat(String fmt) {
        return fmt != null && (fmt.contains("H") || fmt.contains("h") || fmt.contains("m"));
    }

    private static String clockLabel(String fmt) {
        if (isTimeFormat(fmt)) return "Time Color";
        if (fmt != null && fmt.contains("E")) return "Day Color";
        return "Date Color";
    }

    private static StyleAxis numbered(String label, String key, String... values) {
        List<Option> options = new ArrayList<>();
        for (int i = 0; i < values.length; i++) options.add(opt("Style " + (i + 1), key, values[i]));
        return new StyleAxis(label, options);
    }

    private static Option opt(String name, String... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return new Option(name, m);
    }

    private static Option optAll(String name, List<String> keys, String value) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (String k : keys) m.put(k, value);
        return new Option(name, m);
    }

    @SafeVarargs
    private static <T> List<T> list(T... items) {
        List<T> out = new ArrayList<>();
        Collections.addAll(out, items);
        return out;
    }
}
