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
    /** Human condition text ("Partly cloudy"); empty when unknown. */
    public String description = "";
    /**
     * Daily forecast packed as "epochSec,condition,high,low;…" (today first) so it persists as one
     * preference string; read it with {@link #days()}.
     */
    public String forecast = "";

    public static final class Day {
        public final long epochSec;
        public final String condition;
        public final double high, low;

        public Day(long epochSec, String condition, double high, double low) {
            this.epochSec = epochSec;
            this.condition = condition;
            this.high = high;
            this.low = low;
        }
    }

    public java.util.List<Day> days() {
        java.util.List<Day> out = new java.util.ArrayList<>();
        if (forecast == null || forecast.isEmpty()) return out;
        for (String part : forecast.split(";")) {
            String[] f = part.split(",");
            if (f.length < 4) continue;
            try {
                out.add(new Day(Long.parseLong(f[0]), f[1], Double.parseDouble(f[2]), Double.parseDouble(f[3])));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }
}
