package com.desire.widget.data.remote;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.desire.widget.data.model.Widget;
import com.desire.widget.util.AppExecutors;
import com.desire.widget.util.Tasks;

/**
 * Admin-side publishing: pushes a native widget spec (and optional thumbnail) to Firebase so it
 * appears for all users on the next sync. Thumbnails go to Storage, the spec document goes to
 * Firestore. Runs entirely off the main thread.
 */
public final class AdminPublisher {
    public interface Callback {
        void onResult(boolean success, String message);
    }

    private AdminPublisher() {}

    public static void publishSpec(String widgetId, String name, String description,
                                   String categoryId, String categoryName, String size,
                                   String specJson, boolean featured, boolean pro,
                                   @Nullable Uri thumbnail, Callback callback) {
        AppExecutors.getInstance().networkIO().execute(() -> {
            boolean ok;
            String message;
            try {
                FirebaseService fb = FirebaseService.getInstance();

                String thumbUrl = null;
                if (thumbnail != null) {
                    String path = "thumbnails/" + widgetId + ".png";
                    Tasks.await(fb.uploadFile(path, thumbnail));
                    Uri url = Tasks.await(fb.getDownloadUrl(path));
                    thumbUrl = url != null ? url.toString() : null;
                }

                Widget w = new Widget();
                w.setId(widgetId);
                w.setName(name);
                w.setDescription(description);
                w.setCategoryId(categoryId);
                w.setCategoryName(categoryName);
                w.setWidgetSize(size);
                w.setSpecJson(specJson);
                w.setThumbnailUrl(thumbUrl);
                w.setFeatured(featured);
                w.setPro(pro);
                w.setActive(true);
                w.setVersion(1);

                Tasks.await(fb.saveWidget(w));
                ok = true;
                message = "Published to Firebase";
            } catch (Exception e) {
                ok = false;
                message = "Publish failed: " + e.getMessage();
            }
            final boolean fOk = ok;
            final String fMessage = message;
            AppExecutors.getInstance().mainThread().execute(() -> callback.onResult(fOk, fMessage));
        });
    }
}
