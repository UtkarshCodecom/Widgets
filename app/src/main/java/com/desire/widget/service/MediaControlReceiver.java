package com.desire.widget.service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;

import com.desire.widget.engine.runtime.EngineRenderer;

import java.util.List;

/**
 * Handles taps on the music widget's prev / play-pause / next overlay regions
 * ({@code widget_layout.xml}). Finds the currently active media session (same mechanism as
 * {@link MediaNotificationListener} — requires notification access) and issues the transport
 * command directly, so the buttons drawn on the widget bitmap actually control playback.
 */
public class MediaControlReceiver extends BroadcastReceiver {
    public static final String ACTION_CONTROL = "com.desire.widget.ACTION_MEDIA_CONTROL";
    public static final String EXTRA_COMMAND = "command";
    public static final String EXTRA_APPWIDGET_ID = "appwidget_id";

    public static final String CMD_PLAY_PAUSE = "play_pause";
    public static final String CMD_NEXT = "next";
    public static final String CMD_PREV = "prev";
    public static final String CMD_SEEK = "seek";
    /** 0..1 position within the track, used with CMD_SEEK. */
    public static final String EXTRA_FRACTION = "fraction";

    @Override
    public void onReceive(Context context, Intent intent) {
        String command = intent.getStringExtra(EXTRA_COMMAND);
        if (command == null) return;

        MediaController controller = activeController(context);
        if (controller != null) {
            MediaController.TransportControls tc = controller.getTransportControls();
            switch (command) {
                case CMD_PLAY_PAUSE:
                    android.media.session.PlaybackState state = controller.getPlaybackState();
                    boolean playing = state != null && state.getState() == android.media.session.PlaybackState.STATE_PLAYING;
                    if (playing) tc.pause(); else tc.play();
                    break;
                case CMD_NEXT:
                    tc.skipToNext();
                    break;
                case CMD_PREV:
                    tc.skipToPrevious();
                    break;
                case CMD_SEEK:
                    float frac = intent.getFloatExtra(EXTRA_FRACTION, -1f);
                    long dur = 0;
                    android.media.MediaMetadata md = controller.getMetadata();
                    if (md != null) dur = md.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION);
                    if (frac >= 0f && dur > 0) tc.seekTo((long) (frac * dur));
                    break;
                default:
                    break;
            }
        }

        // Re-render now AND again shortly after: the transport command is async, so the session's
        // state usually hasn't flipped yet when the first render runs. The delayed pass refreshes
        // the cached snapshot from the (now updated) session before drawing, flipping the
        // play/pause icon even if the media app is slow to repost its notification.
        final int appWidgetId = intent.getIntExtra(EXTRA_APPWIDGET_ID, -1);
        if (appWidgetId != -1) {
            EngineRenderer.render(context, new int[]{appWidgetId});
            final Context app = context.getApplicationContext();
            final PendingResult pending = goAsync();
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                try {
                    refreshCacheFromSession(app);
                    EngineRenderer.render(app, new int[]{appWidgetId});
                } finally {
                    pending.finish();
                }
            }, 600);
        }
    }

    /** Re-reads the active session into the now-playing cache (same fields the listener writes). */
    private void refreshCacheFromSession(Context context) {
        try {
            MediaController c = activeController(context);
            if (c == null) return;
            android.media.MediaMetadata md = c.getMetadata();
            android.media.session.PlaybackState ps = c.getPlaybackState();
            com.desire.widget.engine.data.CachedLiveDataSource cache =
                    new com.desire.widget.engine.data.CachedLiveDataSource(context);
            com.desire.widget.engine.data.NowPlaying np = cache.nowPlaying();
            np.playing = ps != null
                    && ps.getState() == android.media.session.PlaybackState.STATE_PLAYING;
            if (ps != null) np.positionMillis = Math.max(0, ps.getPosition());
            if (md != null) {
                long dur = md.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION);
                if (dur > 0) np.durationMillis = dur;
            }
            cache.putNowPlaying(np);
        } catch (Exception ignored) {
            // No notification access / session gone — the listener will catch up.
        }
    }

    private MediaController activeController(Context context) {
        try {
            MediaSessionManager msm = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (msm == null) return null;
            ComponentName listener = new ComponentName(context, MediaNotificationListener.class);
            List<MediaController> controllers = msm.getActiveSessions(listener);
            return (controllers != null && !controllers.isEmpty()) ? controllers.get(0) : null;
        } catch (Exception e) {
            return null; // notification access not granted, or no active session
        }
    }
}
