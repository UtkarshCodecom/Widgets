package com.desire.widget.engine.model;

import java.util.Map;

/**
 * Declarative click action attached to a component or the widget root. Compiled into a
 * {@code PendingIntent} by the ActionCompiler (Phase 2). Package/class names are stored as data,
 * never hardcoded, so the same spec is portable across devices and installs.
 */
public class ActionSpec {
    /** launch_app | open_activity | open_url | toggle_setting | broadcast */
    public String type;
    public String packageName;
    public String className;
    /** url for open_url, setting key for toggle_setting, action string for broadcast */
    public String data;
    public Map<String, String> extras;
}
