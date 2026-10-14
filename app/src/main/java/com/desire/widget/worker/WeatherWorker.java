package com.desire.widget.worker;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.desire.widget.engine.data.CachedLiveDataSource;
import com.desire.widget.engine.data.WeatherSnapshot;
import com.desire.widget.engine.runtime.EngineRenderer;
import com.desire.widget.engine.runtime.EngineWidgetStore;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Tasks;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Fetches current weather from Open-Meteo (free, no API key) for the device location and caches it
 * via {@link CachedLiveDataSource}, then re-renders placed widgets so weather components update.
 * Falls back to a default location when permission/location is unavailable.
 */
public class WeatherWorker extends Worker {
    private static final double DEFAULT_LAT = 40.7128;   // New York fallback
    private static final double DEFAULT_LON = -74.0060;

    public WeatherWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();

        // Surface WHY we're using an approximate location, instead of silently guessing NYC.
        // Prefer the last coordinates that actually resolved over the default city.
        double[] cached = loadLastCoords(ctx);
        double fallbackLat = cached != null ? cached[0] : DEFAULT_LAT;
        double fallbackLon = cached != null ? cached[1] : DEFAULT_LON;
        if (!com.desire.widget.util.LocationPermissionHelper.hasPermission(ctx)) {
            return fetchAndLabel(ctx, fallbackLat, fallbackLon, "Enable location");
        }
        if (!com.desire.widget.util.LocationPermissionHelper.isLocationEnabled(ctx)) {
            return fetchAndLabel(ctx, fallbackLat, fallbackLon, "Location off");
        }

        try {
            double[] loc = resolveLocation(ctx);
            WeatherSnapshot snapshot = fetch(loc[0], loc[1]);
            if (snapshot == null) return Result.retry(); // offline: keep showing the last cache
            snapshot.location = reverseGeocode(ctx, loc[0], loc[1]);

            new CachedLiveDataSource(ctx).putWeather(snapshot);
            EngineRenderer.render(ctx, EngineWidgetStore.allPlacedIds(ctx));
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    /** Permission/GPS is off: still show real conditions for a default city, but label why. */
    private Result fetchAndLabel(Context ctx, double lat, double lon, String label) {
        try {
            WeatherSnapshot snapshot = fetch(lat, lon);
            if (snapshot == null) return Result.retry();
            snapshot.location = label;
            new CachedLiveDataSource(ctx).putWeather(snapshot);
            EngineRenderer.render(ctx, EngineWidgetStore.allPlacedIds(ctx));
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    private double[] resolveLocation(Context ctx) {
        try {
            com.google.android.gms.location.FusedLocationProviderClient client =
                    LocationServices.getFusedLocationProviderClient(ctx);
            Location l = Tasks.await(client.getLastLocation());
            android.util.Log.i("WeatherWorker", "lastLocation=" + l);
            // getLastLocation() is null when the device has no recent fix (common — nothing else has
            // asked for location lately). Actively request a single current location so we don't
            // silently fall back to a stale/default location while permission is actually granted.
            if (l == null) {
                l = Tasks.await(client.getCurrentLocation(
                        com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY, null));
                android.util.Log.i("WeatherWorker", "currentLocation=" + l);
            }
            if (l != null) {
                saveLastCoords(ctx, l.getLatitude(), l.getLongitude());
                return new double[]{l.getLatitude(), l.getLongitude()};
            }
        } catch (Exception e) {
            android.util.Log.w("WeatherWorker", "resolveLocation failed", e);
        }
        // No fix right now (locked screen / background restriction / airplane mode). Reuse the
        // last coordinates that DID resolve instead of resetting the user to the New York default
        // — a background refresh must never replace good local weather with a fallback city.
        double[] cached = loadLastCoords(ctx);
        if (cached != null) {
            android.util.Log.i("WeatherWorker", "using cached coords");
            return cached;
        }
        android.util.Log.w("WeatherWorker", "falling back to default NYC");
        return new double[]{DEFAULT_LAT, DEFAULT_LON};
    }

    private void saveLastCoords(Context ctx, double lat, double lon) {
        ctx.getSharedPreferences("weather_worker", Context.MODE_PRIVATE).edit()
                .putLong("last_lat", Double.doubleToRawLongBits(lat))
                .putLong("last_lon", Double.doubleToRawLongBits(lon))
                .apply();
    }

    private double[] loadLastCoords(Context ctx) {
        android.content.SharedPreferences p =
                ctx.getSharedPreferences("weather_worker", Context.MODE_PRIVATE);
        if (!p.contains("last_lat")) return null;
        return new double[]{
                Double.longBitsToDouble(p.getLong("last_lat", 0)),
                Double.longBitsToDouble(p.getLong("last_lon", 0))};
    }

    private WeatherSnapshot fetch(double lat, double lon) throws Exception {
        String url = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
                        "&current=temperature_2m,weather_code" +
                        "&daily=temperature_2m_max,temperature_2m_min,sunrise,sunset" +
                        "&timezone=auto&forecast_days=1&timeformat=unixtime",
                lat, lon);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        try {
            if (conn.getResponseCode() != 200) return null;
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }
            JSONObject root = new JSONObject(sb.toString());
            JSONObject current = root.getJSONObject("current");
            JSONObject daily = root.getJSONObject("daily");

            WeatherSnapshot s = new WeatherSnapshot();
            s.temp = current.getDouble("temperature_2m");
            s.condition = mapCondition(current.getInt("weather_code"));
            s.high = daily.getJSONArray("temperature_2m_max").getDouble(0);
            s.low = daily.getJSONArray("temperature_2m_min").getDouble(0);
            s.unit = "°";

            // Open-Meteo returns sunrise/sunset as UTC unix seconds (timeformat=unixtime); these are
            // the real astronomical times for the location and far more accurate than a local
            // approximation. Guard the array access in case a response omits them.
            if (daily.has("sunrise") && daily.getJSONArray("sunrise").length() > 0) {
                s.sunriseMillis = daily.getJSONArray("sunrise").getLong(0) * 1000L;
            }
            if (daily.has("sunset") && daily.getJSONArray("sunset").length() > 0) {
                s.sunsetMillis = daily.getJSONArray("sunset").getLong(0) * 1000L;
            }
            return s;
        } finally {
            conn.disconnect();
        }
    }

    /** WMO weather codes → our condition buckets. */
    private String mapCondition(int code) {
        if (code == 0) return "clear";
        if (code <= 48) return "clouds";
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return "rain";
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return "snow";
        if (code >= 95) return "storm";
        return "clouds";
    }

    private String reverseGeocode(Context ctx, double lat, double lon) {
        try {
            Geocoder geocoder = new Geocoder(ctx, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address a = addresses.get(0);
                if (a.getLocality() != null) return a.getLocality();
                if (a.getSubAdminArea() != null) return a.getSubAdminArea();
                if (a.getAdminArea() != null) return a.getAdminArea();
            }
        } catch (Exception ignored) {}
        return "My Location";
    }
}
