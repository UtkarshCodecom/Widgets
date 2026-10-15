package com.desire.widget.service;

import android.content.ComponentName;
import android.graphics.Bitmap;
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

import java.io.File;
import java.io.FileOutputStream;
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
                        // Raw position + duration so MusicRenderer can extrapolate the moving
                        // progress bar between notification updates (see NowPlaying.updatedAtMillis).
                        np.positionMillis = Math.max(0, ps.getPosition());
                        np.durationMillis = dur;
                        np.progress = Math.max(0f, Math.min(1f, ps.getPosition() / (float) dur));
                    }
                    np.albumArtPath = cacheAlbumArt(md);
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

    /**
     * Saves the track's album art (if the session provides one) to a small cache file the
     * MusicRenderer can load synchronously during a render pass. Overwrites the same file each
     * time so storage never grows.
     */
    private String cacheAlbumArt(MediaMetadata md) {
        try {
            Bitmap art = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            if (art == null) art = md.getBitmap(MediaMetadata.METADATA_KEY_ART);
            if (art == null) return null;

            int size = 256;
            Bitmap scaled = Bitmap.createScaledBitmap(art, size, size, true);
            File file = new File(getCacheDir(), "now_playing_art.png");
            try (FileOutputStream out = new FileOutputStream(file)) {
                scaled.compress(Bitmap.CompressFormat.PNG, 90, out);
            }
            return file.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }
}
