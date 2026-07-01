package com.desire.widget.worker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
        try {
            double[] loc = resolveLocation(ctx);
            WeatherSnapshot snapshot = fetch(loc[0], loc[1]);
            if (snapshot == null) return Result.retry();
            snapshot.location = reverseGeocode(ctx, loc[0], loc[1]);

            new CachedLiveDataSource(ctx).putWeather(snapshot);
            EngineRenderer.render(ctx, EngineWidgetStore.allPlacedIds(ctx));
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    private double[] resolveLocation(Context ctx) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                Location l = Tasks.await(LocationServices.getFusedLocationProviderClient(ctx).getLastLocation());
                if (l != null) return new double[]{l.getLatitude(), l.getLongitude()};
            } catch (Exception ignored) {}
        }
        return new double[]{DEFAULT_LAT, DEFAULT_LON};
    }

    private WeatherSnapshot fetch(double lat, double lon) throws Exception {
        String url = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
                        "&current=temperature_2m,weather_code" +
                        "&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1",
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
