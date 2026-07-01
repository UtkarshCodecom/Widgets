package com.desire.widget.ui.studio;

import com.desire.widget.engine.model.BackgroundSpec;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.model.Frame;
import com.desire.widget.engine.model.WidgetSpec;

import java.util.HashMap;
import java.util.Map;

/**
 * Sensible starting specs for the Studio: a blank widget plus a reasonable default for each
 * component type the palette can add.
 */
public final class StudioDefaults {
    private StudioDefaults() {}

    /** The component types offered in the Studio palette, in display order. */
    public static final String[] PALETTE = {
            "text", "clock", "analog_clock", "calendar", "weather", "battery", "progress_ring",
            "image", "app_shortcut", "shape", "divider", "music", "countdown", "qr_code"
    };

    public static WidgetSpec blank(String size) {
        WidgetSpec spec = new WidgetSpec();
        spec.id = "studio_" + System.currentTimeMillis();
        spec.name = "My Widget";
        spec.designSize = size != null ? size : "2x2";
        spec.background = new BackgroundSpec();
        spec.background.fill = "@surface";
        spec.background.cornerRadius = 0.14f;
        spec.components.add(component("text"));
        return spec;
    }

    public static ComponentSpec component(String type) {
        ComponentSpec c = new ComponentSpec();
        c.type = type;
        c.id = type + "_" + (System.currentTimeMillis() % 100000);
        c.frame = new Frame(0.1f, 0.1f, 0.8f, 0.3f);
        c.props = new HashMap<>();
        Map<String, Object> p = c.props;
        switch (type) {
            case "text":
                p.put("text", "Text"); p.put("fontSize", 0.4); p.put("align", "center"); p.put("color", "@textPrimary");
                break;
            case "clock":
                c.frame = new Frame(0.08f, 0.3f, 0.84f, 0.4f);
                p.put("format", "HH:mm"); p.put("fontSize", 0.8); p.put("align", "center"); p.put("bold", true); p.put("color", "@textPrimary");
                break;
            case "analog_clock":
                c.frame = new Frame(0.2f, 0.2f, 0.6f, 0.6f);
                p.put("secondColor", "@primary");
                break;
            case "calendar":
                c.frame = new Frame(0.05f, 0.1f, 0.9f, 0.8f);
                p.put("accentColor", "@primary");
                break;
            case "weather":
                c.frame = new Frame(0.08f, 0.2f, 0.84f, 0.5f);
                p.put("condition", "clear"); p.put("temp", 24); p.put("location", "City"); p.put("high", 27); p.put("low", 18);
                break;
            case "battery":
                c.frame = new Frame(0.1f, 0.3f, 0.8f, 0.4f);
                break;
            case "progress_ring":
                c.frame = new Frame(0.25f, 0.2f, 0.5f, 0.6f);
                p.put("progress", 0.6); p.put("progressColor", "@primary");
                break;
            case "image":
                c.frame = new Frame(0.2f, 0.2f, 0.6f, 0.6f);
                p.put("source", "");
                break;
            case "app_shortcut":
                c.frame = new Frame(0.3f, 0.2f, 0.4f, 0.6f);
                p.put("packageName", ""); p.put("label", "App");
                break;
            case "shape":
                p.put("shape", "rounded"); p.put("fill", "@primary"); p.put("cornerRadius", 0.2);
                break;
            case "divider":
                c.frame = new Frame(0.1f, 0.48f, 0.8f, 0.04f);
                p.put("orientation", "horizontal"); p.put("color", "@textSecondary"); p.put("thickness", 3);
                break;
            case "music":
                c.frame = new Frame(0.05f, 0.1f, 0.9f, 0.8f);
                p.put("title", "Song"); p.put("artist", "Artist"); p.put("playing", true); p.put("progress", 0.4);
                break;
            case "countdown":
                c.frame = new Frame(0.08f, 0.2f, 0.84f, 0.6f);
                p.put("label", "Event"); p.put("targetDate", "2027-01-01 00:00");
                break;
            case "qr_code":
                c.frame = new Frame(0.2f, 0.15f, 0.6f, 0.6f);
                p.put("data", "https://example.com"); p.put("foreground", "@textPrimary"); p.put("background", "@surface");
                break;
            default:
                break;
        }
        return c;
    }
}
