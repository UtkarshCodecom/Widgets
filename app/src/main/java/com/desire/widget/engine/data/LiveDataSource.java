package com.desire.widget.engine.data;

/**
 * Supplies live, device-/network-derived values to renderers without coupling them to any
 * concrete data source. Implementations are typically cache-backed (written by a WorkManager sync
 * job) so rendering stays synchronous and never blocks on the network.
 *
 * <p>Renderers treat the returned values as defaults and let component props override individual
 * fields, which keeps Studio previews fully controllable.
 */
public interface LiveDataSource {
    WeatherSnapshot weather();

    NowPlaying nowPlaying();
}
