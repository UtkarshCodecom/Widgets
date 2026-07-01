package com.desire.widget.engine.data;

/**
 * A point-in-time weather reading consumed by the {@code weather} component. Populated by a
 * provider (cache/WorkManager sync) or overridden per-widget from component props for previews.
 */
public class WeatherSnapshot {
    public double temp = 24;
    /** clear | clouds | rain | snow | storm */
    public String condition = "clear";
    public String location = "—";
    public double high = 27;
    public double low = 18;
    public String unit = "°";
}
