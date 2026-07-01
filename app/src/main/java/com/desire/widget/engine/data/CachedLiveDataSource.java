package com.desire.widget.engine.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Reads the latest weather / now-playing snapshot from SharedPreferences. A background sync
 * (WorkManager, Phase 6) writes these keys; rendering only ever reads the cache, so it stays
 * synchronous and offline-safe. Missing keys fall back to the model defaults.
 */
public class CachedLiveDataSource implements LiveDataSource {
    private static final String PREFS = "engine_live";
    private final SharedPreferences prefs;

    public CachedLiveDataSource(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public WeatherSnapshot weather() {
        WeatherSnapshot w = new WeatherSnapshot();
        w.temp = prefs.getFloat("weather_temp", (float) w.temp);
        w.condition = prefs.getString("weather_condition", w.condition);
        w.location = prefs.getString("weather_location", w.location);
        w.high = prefs.getFloat("weather_high", (float) w.high);
        w.low = prefs.getFloat("weather_low", (float) w.low);
        w.unit = prefs.getString("weather_unit", w.unit);
        return w;
    }

    @Override
    public NowPlaying nowPlaying() {
        NowPlaying n = new NowPlaying();
        n.title = prefs.getString("np_title", n.title);
        n.artist = prefs.getString("np_artist", n.artist);
        n.playing = prefs.getBoolean("np_playing", n.playing);
        n.progress = prefs.getFloat("np_progress", n.progress);
        return n;
    }

    /** Write helpers used by the sync job (Phase 6). */
    public void putWeather(WeatherSnapshot w) {
        prefs.edit()
                .putFloat("weather_temp", (float) w.temp)
                .putString("weather_condition", w.condition)
                .putString("weather_location", w.location)
                .putFloat("weather_high", (float) w.high)
                .putFloat("weather_low", (float) w.low)
                .putString("weather_unit", w.unit)
                .apply();
    }

    public void putNowPlaying(NowPlaying n) {
        prefs.edit()
                .putString("np_title", n.title)
                .putString("np_artist", n.artist)
                .putBoolean("np_playing", n.playing)
                .putFloat("np_progress", n.progress)
                .apply();
    }
}
