package com.desire.widget.engine.model;

/**
 * Position + size of a component, expressed in NORMALIZED units (0..1) relative to the widget
 * rectangle. The engine multiplies these by the real pixel size at render time, which is what
 * gives every widget automatic, resolution-independent scaling across 1x1 .. 4x4 and any
 * launcher-allocated size.
 */
public class Frame {
    public float x = 0f;
    public float y = 0f;
    public float w = 1f;
    public float h = 1f;

    public Frame() {}

    public Frame(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
}
