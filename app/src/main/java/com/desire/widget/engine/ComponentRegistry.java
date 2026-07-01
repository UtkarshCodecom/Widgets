package com.desire.widget.engine;

import com.desire.widget.engine.components.AnalogClockRenderer;
import com.desire.widget.engine.components.AppShortcutRenderer;
import com.desire.widget.engine.components.BatteryRenderer;
import com.desire.widget.engine.components.CalendarRenderer;
import com.desire.widget.engine.components.CountdownRenderer;
import com.desire.widget.engine.components.DigitalClockRenderer;
import com.desire.widget.engine.components.DividerRenderer;
import com.desire.widget.engine.components.FlipClockRenderer;
import com.desire.widget.engine.components.ImageRenderer;
import com.desire.widget.engine.components.MusicRenderer;
import com.desire.widget.engine.components.MusicVisualizerRenderer;
import com.desire.widget.engine.components.WordClockRenderer;
import com.desire.widget.engine.components.ProgressRingRenderer;
import com.desire.widget.engine.components.QrCodeRenderer;
import com.desire.widget.engine.components.ShapeRenderer;
import com.desire.widget.engine.components.TextRenderer;
import com.desire.widget.engine.components.WeatherRenderer;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps {@code ComponentSpec.type} -> {@link ComponentRenderer}. This is the extension point of the
 * whole engine: register a renderer here (or at runtime) and that component type becomes available
 * everywhere — gallery previews, Studio, and installed widgets — with zero other changes.
 */
public class ComponentRegistry {
    private final Map<String, ComponentRenderer> renderers = new HashMap<>();

    public ComponentRegistry register(ComponentRenderer r) {
        if (r != null && r.type() != null) renderers.put(r.type(), r);
        return this;
    }

    public ComponentRenderer get(String type) {
        return type == null ? null : renderers.get(type);
    }

    public boolean has(String type) {
        return type != null && renderers.containsKey(type);
    }

    /** The full built-in component set. Adding a type = one more {@code .register()} line. */
    public static ComponentRegistry createDefault() {
        return new ComponentRegistry()
                // Phase 1 primitives
                .register(new TextRenderer())
                .register(new ShapeRenderer())
                .register(new DividerRenderer())
                .register(new ProgressRingRenderer())
                .register(new DigitalClockRenderer())
                .register(new AnalogClockRenderer())
                .register(new BatteryRenderer())
                // Phase 2 rich components
                .register(new ImageRenderer())
                .register(new CalendarRenderer())
                .register(new WeatherRenderer())
                .register(new AppShortcutRenderer())
                .register(new MusicRenderer())
                .register(new CountdownRenderer())
                .register(new QrCodeRenderer())
                // Phase C rich components
                .register(new FlipClockRenderer())
                .register(new WordClockRenderer())
                .register(new MusicVisualizerRenderer());
    }
}
