package com.desire.widget.engine.model;

import java.util.HashMap;
import java.util.Map;

/**
 * A single drawable element of a widget. {@code type} selects the registered
 * {@link com.desire.widget.engine.ComponentRenderer}; {@code props} is a free-form bag the
 * renderer reads via SpecProps. Keeping props generic is what lets new component types be added
 * without changing this class or the engine.
 */
public class ComponentSpec {
    public String id;
    public String type;
    public Frame frame = new Frame();
    public Map<String, Object> props = new HashMap<>();
    public ActionSpec action;
}
