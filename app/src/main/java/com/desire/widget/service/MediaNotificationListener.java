package com.desire.widget.service;

import android.content.ComponentName;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.desire.widget.engine.data.CachedLiveDataSource;
import com.desire.widget.engine.data.NowPlaying;
import com.desire.widget.engine.runtime.EngineRenderer;
import com.desire.widget.engine.runtime.EngineWidgetStore;

import java.util.List;

/**
 * Reads the active media session (title/artist/position/playing) via {@link MediaSessionManager}
 * and caches it for the {@code music} component, then re-renders placed widgets. Requires the user
 * to grant notification access; see {@link MediaListenerHelper}.
 */
public class MediaNotificationListener extends NotificationListenerService {
    @Override
    public void onListenerConnected() {
        update();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        update();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        update();
    }

    private void update() {
        try {
            MediaSessionManager msm = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
            if (msm == null) return;
            ComponentName cn = new ComponentName(this, MediaNotificationListener.class);
            List<MediaController> controllers = msm.getActiveSessions(cn);

            NowPlaying np = new NowPlaying();
            if (controllers != null && !controllers.isEmpty()) {
                MediaController c = controllers.get(0);
                MediaMetadata md = c.getMetadata();
                PlaybackState ps = c.getPlaybackState();
                if (md != null) {
                    np.title = text(md, MediaMetadata.METADATA_KEY_TITLE, "Unknown");
                    np.artist = text(md, MediaMetadata.METADATA_KEY_ARTIST, "");
                    long dur = md.getLong(MediaMetadata.METADATA_KEY_DURATION);
                    if (ps != null && dur > 0) {
                        np.progress = Math.max(0f, Math.min(1f, ps.getPosition() / (float) dur));
                    }
                }
                np.playing = ps != null && ps.getState() == PlaybackState.STATE_PLAYING;
            }

            new CachedLiveDataSource(this).putNowPlaying(np);
            EngineRenderer.render(this, EngineWidgetStore.allPlacedIds(this));
        } catch (Exception ignored) {
            // getActiveSessions throws until the user enables notification access — ignore.
        }
    }

    private String text(MediaMetadata md, String key, String def) {
        CharSequence cs = md.getText(key);
        return cs != null ? cs.toString() : def;
    }
}
