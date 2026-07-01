package com.desire.widget.engine.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-level widget document. This is the JSON contract: it is what the Studio edits, what Firebase
 * stores, what Room caches, and what the engine renders. The same WidgetSpec instance drives both
 * the RecyclerView preview bitmap and the installed home-screen widget, guaranteeing they are
 * pixel-identical.
 */
public class WidgetSpec {
    public String id;
    public String name;
    public int schemaVersion = 1;
    /** The size the widget was authored at, e.g. "2x2". Frames are normalized so this is metadata. */
    public String designSize = "2x2";
    public BackgroundSpec background = new BackgroundSpec();
    public List<ComponentSpec> components = new ArrayList<>();
    /** Optional widget-level primary tap action (Phase 3 wires this to a PendingIntent). */
    public ActionSpec action;
}
