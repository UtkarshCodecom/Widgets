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
    /** Epoch millis for today's sunrise/sunset at the resolved location, or 0 if not yet computed. */
    public long sunriseMillis = 0;
    public long sunsetMillis = 0;
    /** Epoch ms when this snapshot was cached; 0 = no real weather has ever been fetched. */
    public long updatedAtMillis = 0;
}
