package com.desire.widget.engine;

import android.graphics.Canvas;
import android.graphics.RectF;

import com.desire.widget.engine.model.ComponentSpec;

/**
 * Contract for a single widget component type. Implementations are stateless and draw into the
 * provided {@code bounds} (already denormalized to pixels and clipped by the engine).
 *
 * <p>Adding a new widget component = implement this interface + register it once. No existing
 * renderer or the engine itself needs to change.
 */
public interface ComponentRenderer {
    /** The {@code ComponentSpec.type} string this renderer handles, e.g. "text", "battery". */
    String type();

    void render(Canvas canvas, RectF bounds, ComponentSpec spec, RenderContext ctx);
}
