package com.desire.widget.data.remote;

import android.net.Uri;
import android.util.Log;

import com.desire.widget.data.model.Announcement;
import com.desire.widget.data.model.AppConfig;
import com.desire.widget.data.model.Category;
import com.desire.widget.data.model.Offer;
import com.desire.widget.data.model.Theme;
import com.desire.widget.data.model.Widget;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class FirebaseService {
    private static final String TAG = "FirebaseService";
    private static FirebaseService instance;

    private final FirebaseFirestore db;
    private final FirebaseStorage storage;

    private FirebaseService() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public static synchronized FirebaseService getInstance() {
        if (instance == null) {
            instance = new FirebaseService();
        }
        return instance;
    }

    // ==================== WIDGETS ====================

    public Task<List<Widget>> getAllWidgets() {
        return db.collection("widgets")
                .whereEqualTo("active", true)
                .get()
                .continueWith(task -> {
                    List<Widget> widgets = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            Widget widget = doc.toObject(Widget.class);
                            if (widget != null) {
                                widget.setId(doc.getId());
                                widget.setHtmlContent(doc.getString("widgetHtml"));
                                widgets.add(widget);
                            }
                        }
                    } else if (!task.isSuccessful()) {
                        Log.e(TAG, "Error getting all widgets", task.getException());
                    }
                    return widgets;
                });
    }

    public Task<List<Widget>> getWidgetsByCategory(String categoryId) {
        return db.collection("widgets")
                .whereEqualTo("active", true)
                .whereEqualTo("categoryId", categoryId)
                .get()
                .continueWith(task -> {
                    List<Widget> widgets = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            Widget widget = doc.toObject(Widget.class);
                            if (widget != null) {
                                widget.setId(doc.getId());
                                widget.setHtmlContent(doc.getString("widgetHtml"));
                                widgets.add(widget);
                            }
                        }
                    } else if (!task.isSuccessful()) {
                        Log.e(TAG, "Error getting widgets by category", task.getException());
                    }
                    return widgets;
                });
    }

    public Task<Widget> getWidgetById(String id) {
        return db.collection("widgets").document(id).get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Widget widget = task.getResult().toObject(Widget.class);
                        if (widget != null) {
                            widget.setId(task.getResult().getId());
                            widget.setHtmlContent(task.getResult().getString("widgetHtml"));
                        }
                        return widget;
                    }
                    return null;
                });
    }

    public Task<Void> saveWidget(Widget widget) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", widget.getName());
        data.put("description", widget.getDescription());
        data.put("categoryId", widget.getCategoryId());
        data.put("categoryName", widget.getCategoryName());
        data.put("thumbnailUrl", widget.getThumbnailUrl());
        data.put("previewUrl", widget.getPreviewUrl());
        data.put("configUrl", widget.getConfigUrl());
        data.put("configJson", widget.getConfigJson());
        data.put("widgetHtml", widget.getHtmlContent());
        data.put("specJson", widget.getSpecJson());
        data.put("widgetSize", widget.getWidgetSize());
        data.put("previewStyle", widget.getPreviewStyle());
        data.put("isPro", widget.isPro());
        data.put("isFeatured", widget.isFeatured());
        data.put("isTrending", widget.isTrending());
        data.put("downloadCount", widget.getDownloadCount());
        data.put("favoriteCount", widget.getFavoriteCount());
        data.put("tags", widget.getTags());
        data.put("version", widget.getVersion());
        data.put("active", widget.isActive());
        data.put("updatedAt", System.currentTimeMillis());

        if (widget.getId() != null && !widget.getId().isEmpty()) {
            return db.collection("widgets").document(widget.getId()).set(data);
        } else {
            data.put("createdAt", System.currentTimeMillis());
            return db.collection("widgets").add(data).continueWith(task -> null);
        }
    }

    public Task<Void> deleteWidget(String id) {
        return db.collection("widgets").document(id).delete();
    }

    public Task<Void> incrementWidgetDownload(String id) {
        return db.collection("widgets").document(id)
                .update("downloadCount", com.google.firebase.firestore.FieldValue.increment(1));
    }

    // ==================== CATEGORIES ====================

    public Task<List<Category>> getAllCategories() {
        return db.collection("categories")
                .whereEqualTo("isActive", true)
                .orderBy("order", Query.Direction.ASCENDING)
                .get()
                .continueWith(task -> {
                    List<Category> categories = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            Category category = doc.toObject(Category.class);
                            if (category != null) {
                                category.setId(doc.getId());
                                categories.add(category);
                            }
                        }
                    }
                    return categories;
                });
    }

    public Task<Void> saveCategory(Category category) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", category.getName());
        data.put("icon", category.getIcon());
        data.put("color", category.getColor());
        data.put("order", category.getOrder());
        data.put("isActive", category.isActive());

        if (category.getId() != null && !category.getId().isEmpty()) {
            return db.collection("categories").document(category.getId()).set(data);
        } else {
            data.put("createdAt", System.currentTimeMillis());
            return db.collection("categories").add(data).continueWith(task -> null);
        }
    }

    public Task<Void> deleteCategory(String id) {
        return db.collection("categories").document(id).delete();
    }

    // ==================== THEMES ====================

    public Task<List<Theme>> getAllThemes() {
        return db.collection("themes")
                .whereEqualTo("active", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .continueWith(task -> {
                    List<Theme> themes = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            Theme theme = doc.toObject(Theme.class);
                            if (theme != null) {
                                theme.setId(doc.getId());
                                themes.add(theme);
                            }
                        }
                    }
                    return themes;
                });
    }

    public Task<Void> saveTheme(Theme theme) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", theme.getName());
        data.put("description", theme.getDescription());
        data.put("thumbnailUrl", theme.getThumbnailUrl());
        data.put("isPro", theme.isPro());
        data.put("isDefault", theme.isDefault());
        data.put("config", theme.getConfig());
        data.put("active", theme.isActive());
        data.put("updatedAt", System.currentTimeMillis());

        if (theme.getId() != null && !theme.getId().isEmpty()) {
            return db.collection("themes").document(theme.getId()).set(data);
        } else {
            data.put("createdAt", System.currentTimeMillis());
            return db.collection("themes").add(data).continueWith(task -> null);
        }
    }

    public Task<Void> deleteTheme(String id) {
        return db.collection("themes").document(id).delete();
    }

    // ==================== APP CONFIG ====================

    public Task<AppConfig> getAppConfig() {
        return db.collection("appConfig").document("config")
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        AppConfig config = task.getResult().toObject(AppConfig.class);
                        if (config != null) {
                            config.setId(task.getResult().getId());
                        }
                        return config;
                    }
                    return null;
                });
    }

    public Task<Void> saveAppConfig(AppConfig config) {
        Map<String, Object> data = new HashMap<>();
        data.put("latestVersion", config.getLatestVersion());
        data.put("forceUpdate", config.isForceUpdate());
        data.put("forceUpdateMessage", config.getForceUpdateMessage());
        data.put("maintenanceMode", config.isMaintenanceMode());
        data.put("maintenanceMessage", config.getMaintenanceMessage());
        data.put("minSupportedVersion", config.getMinSupportedVersion());
        data.put("featureFlags", config.getFeatureFlags());
        data.put("trendingWidgetIds", config.getTrendingWidgetIds());
        data.put("featuredWidgetIds", config.getFeaturedWidgetIds());
        data.put("updatedAt", System.currentTimeMillis());
        return db.collection("appConfig").document("config").set(data);
    }

    // ==================== ANNOUNCEMENTS ====================

    public Task<List<Announcement>> getActiveAnnouncements() {
        long now = System.currentTimeMillis();
        return db.collection("announcements")
                .whereEqualTo("active", true)
                .whereLessThanOrEqualTo("startAt", now)
                .whereGreaterThanOrEqualTo("endAt", now)
                .orderBy("priority", Query.Direction.DESCENDING)
                .get()
                .continueWith(task -> {
                    List<Announcement> announcements = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            Announcement a = doc.toObject(Announcement.class);
                            if (a != null) {
                                a.setId(doc.getId());
                                announcements.add(a);
                            }
                        }
                    }
                    return announcements;
                });
    }

    public Task<List<Announcement>> getAllAnnouncements() {
        return db.collection("announcements")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .continueWith(task -> {
                    List<Announcement> announcements = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            Announcement a = doc.toObject(Announcement.class);
                            if (a != null) {
                                a.setId(doc.getId());
                                announcements.add(a);
                            }
                        }
                    }
                    return announcements;
                });
    }

    public Task<Void> saveAnnouncement(Announcement announcement) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", announcement.getTitle());
        data.put("message", announcement.getMessage());
        data.put("type", announcement.getType());
        data.put("actionUrl", announcement.getActionUrl());
        data.put("actionLabel", announcement.getActionLabel());
        data.put("imageUrl", announcement.getImageUrl());
        data.put("priority", announcement.getPriority());
        data.put("startAt", announcement.getStartAt());
        data.put("endAt", announcement.getEndAt());
        data.put("active", announcement.isActive());

        if (announcement.getId() != null && !announcement.getId().isEmpty()) {
            return db.collection("announcements").document(announcement.getId()).set(data);
        } else {
            data.put("createdAt", System.currentTimeMillis());
            return db.collection("announcements").add(data).continueWith(task -> null);
        }
    }

    public Task<Void> deleteAnnouncement(String id) {
        return db.collection("announcements").document(id).delete();
    }

    // ==================== OFFERS ====================

    public Task<List<Offer>> getActiveOffers() {
        long now = System.currentTimeMillis();
        return db.collection("offers")
                .whereEqualTo("active", true)
                .whereLessThanOrEqualTo("startAt", now)
                .whereGreaterThanOrEqualTo("endAt", now)
                .get()
                .continueWith(task -> {
                    List<Offer> offers = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            Offer offer = doc.toObject(Offer.class);
                            if (offer != null) {
                                offer.setId(doc.getId());
                                offers.add(offer);
                            }
                        }
                    }
                    return offers;
                });
    }

    public Task<List<Offer>> getAllOffers() {
        return db.collection("offers")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .continueWith(task -> {
                    List<Offer> offers = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            Offer offer = doc.toObject(Offer.class);
                            if (offer != null) {
                                offer.setId(doc.getId());
                                offers.add(offer);
                            }
                        }
                    }
                    return offers;
                });
    }

    public Task<Void> saveOffer(Offer offer) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", offer.getTitle());
        data.put("description", offer.getDescription());
        data.put("discountPercent", offer.getDiscountPercent());
        data.put("code", offer.getCode());
        data.put("imageUrl", offer.getImageUrl());
        data.put("startAt", offer.getStartAt());
        data.put("endAt", offer.getEndAt());
        data.put("active", offer.isActive());

        if (offer.getId() != null && !offer.getId().isEmpty()) {
            return db.collection("offers").document(offer.getId()).set(data);
        } else {
            data.put("createdAt", System.currentTimeMillis());
            return db.collection("offers").add(data).continueWith(task -> null);
        }
    }

    public Task<Void> deleteOffer(String id) {
        return db.collection("offers").document(id).delete();
    }

    // ==================== STORAGE ====================

    public StorageReference getStorageReference() {
        return storage.getReference();
    }

    public UploadTask uploadFile(String path, Uri fileUri) {
        StorageReference ref = storage.getReference().child(path);
        return ref.putFile(fileUri);
    }

    public Task<Uri> getDownloadUrl(String path) {
        return storage.getReference().child(path).getDownloadUrl();
    }

    public Task<Void> deleteFile(String path) {
        return storage.getReference().child(path).delete();
    }
}
