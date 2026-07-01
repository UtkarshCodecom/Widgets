package com.desire.widget.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.desire.widget.data.local.WidgetDatabase;
import com.desire.widget.data.local.dao.CategoryDao;
import com.desire.widget.data.local.dao.ThemeDao;
import com.desire.widget.data.local.dao.WidgetDao;
import com.desire.widget.data.local.entity.CategoryEntity;
import com.desire.widget.data.local.entity.ThemeEntity;
import com.desire.widget.data.local.entity.WidgetEntity;
import com.desire.widget.data.model.Category;
import com.desire.widget.data.model.Theme;
import com.desire.widget.data.model.Widget;
import com.desire.widget.data.remote.FirebaseService;
import com.desire.widget.util.AppExecutors;
import com.desire.widget.util.Tasks;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WidgetRepository {
    private static WidgetRepository instance;

    private final WidgetDao widgetDao;
    private final CategoryDao categoryDao;
    private final ThemeDao themeDao;
    private final FirebaseService firebaseService;
    private final AppExecutors executors;
    private final Gson gson;

    private WidgetRepository(Context context) {
        WidgetDatabase db = WidgetDatabase.getInstance(context);
        widgetDao = db.widgetDao();
        categoryDao = db.categoryDao();
        themeDao = db.themeDao();
        firebaseService = FirebaseService.getInstance();
        executors = AppExecutors.getInstance();
        gson = new Gson();
        final Context appContext = context.getApplicationContext();
        executors.diskIO().execute(() -> {
            com.desire.widget.data.local.LocalSeeder.seedIfEmpty(appContext, widgetDao, categoryDao, themeDao);
        });
    }

    public static synchronized WidgetRepository getInstance(Context context) {
        if (instance == null) {
            instance = new WidgetRepository(context.getApplicationContext());
        }
        return instance;
    }

    // ==================== WIDGETS ====================

    public LiveData<List<WidgetEntity>> getAllWidgets() {
        return widgetDao.getAllWidgets();
    }

    public LiveData<List<WidgetEntity>> getWidgetsByCategory(String categoryId) {
        return widgetDao.getWidgetsByCategory(categoryId);
    }

    public LiveData<List<WidgetEntity>> getFavoriteWidgets() {
        return widgetDao.getFavoriteWidgets();
    }

    public LiveData<List<WidgetEntity>> searchWidgets(String query) {
        return widgetDao.searchWidgets(query);
    }

    public LiveData<List<WidgetEntity>> getFeaturedWidgets() {
        return widgetDao.getFeaturedWidgets();
    }

    public LiveData<List<WidgetEntity>> getTrendingWidgets() {
        return widgetDao.getTrendingWidgets();
    }

    public LiveData<WidgetEntity> getWidgetById(String id) {
        return widgetDao.getWidgetById(id);
    }

    public void toggleFavorite(String id, boolean isFavorite) {
        executors.diskIO().execute(() -> widgetDao.setFavorite(id, isFavorite));
    }

    /** Persist a user-created/edited native widget so it appears in the gallery. */
    public void saveUserWidget(WidgetEntity widget) {
        executors.diskIO().execute(() -> widgetDao.insert(widget));
    }

    public void syncWidgetsFromFirebase() {
        executors.diskIO().execute(() -> {
            try {
                List<Widget> firebaseWidgets = Tasks.await(firebaseService.getAllWidgets());
                // Don't wipe the bundled seed when Firestore has no content yet.
                if (firebaseWidgets == null || firebaseWidgets.isEmpty()) return;
                
                // Sort locally (descending by updatedAt) since Firestore query ordering 
                // might exclude documents with missing fields.
                firebaseWidgets.sort((w1, w2) -> {
                    long t1 = w1.getUpdatedAt() > 0 ? w1.getUpdatedAt() : w1.getCreatedAt();
                    long t2 = w2.getUpdatedAt() > 0 ? w2.getUpdatedAt() : w2.getCreatedAt();
                    return Long.compare(t2, t1);
                });

                List<WidgetEntity> entities = new ArrayList<>();
                for (Widget w : firebaseWidgets) {
                    WidgetEntity entity = new WidgetEntity();
                    entity.setId(w.getId());
                    entity.setName(w.getName());
                    entity.setDescription(w.getDescription());
                    entity.setCategoryId(w.getCategoryId());
                    entity.setCategoryName(w.getCategoryName());
                    entity.setThumbnailUrl(w.getThumbnailUrl());
                    entity.setPreviewUrl(w.getPreviewUrl());
                    entity.setConfigJson(w.getConfigJson());
                    entity.setHtmlContent(w.getHtmlContent());
                    entity.setSpecJson(w.getSpecJson());
                    entity.setWidgetSize(w.getWidgetSize());
                    entity.setPreviewStyle(w.getPreviewStyle());
                    entity.setPro(w.isPro());
                    entity.setFeatured(w.isFeatured());
                    entity.setTrending(w.isTrending());
                    entity.setDownloadCount(w.getDownloadCount());
                    entity.setVersion(w.getVersion());
                    
                    long timestamp = w.getUpdatedAt() > 0 ? w.getUpdatedAt() : w.getCreatedAt();
                    entity.setUpdatedAt(timestamp);
                    entity.setActive(w.isActive());

                    WidgetEntity existing = widgetDao.getWidgetByIdSync(w.getId());
                    if (existing != null) {
                        entity.setFavorite(existing.isFavorite());
                    }

                    entities.add(entity);
                }
                // Merge (upsert) rather than wipe, so the bundled seed and user-created
                // ("My Widgets") entries are preserved alongside published widgets.
                widgetDao.insertAll(entities);
            } catch (Exception e) {
                android.util.Log.e("WidgetRepository", "Sync failed", e);
            }
        });
    }

    // ==================== CATEGORIES ====================

    public LiveData<List<CategoryEntity>> getAllCategories() {
        return categoryDao.getAllCategories();
    }

    public void syncCategoriesFromFirebase() {
        executors.diskIO().execute(() -> {
            try {
                List<Category> firebaseCategories = Tasks.await(firebaseService.getAllCategories());
                if (firebaseCategories == null || firebaseCategories.isEmpty()) return;
                List<CategoryEntity> entities = new ArrayList<>();
                for (Category c : firebaseCategories) {
                    CategoryEntity entity = new CategoryEntity();
                    entity.setId(c.getId());
                    entity.setName(c.getName());
                    entity.setIcon(c.getIcon());
                    entity.setColor(c.getColor());
                    entity.setOrder(c.getOrder());
                    entity.setActive(c.isActive());
                    entities.add(entity);
                }
                categoryDao.deleteAll();
                categoryDao.insertAll(entities);
            } catch (Exception e) {
                android.util.Log.e("WidgetRepository", "Category sync failed", e);
            }
        });
    }

    // ==================== THEMES ====================

    public LiveData<List<ThemeEntity>> getAllThemes() {
        return themeDao.getAllThemes();
    }

    public LiveData<ThemeEntity> getDefaultTheme() {
        return themeDao.getDefaultTheme();
    }

    public void syncThemesFromFirebase() {
        executors.diskIO().execute(() -> {
            try {
                List<Theme> firebaseThemes = Tasks.await(firebaseService.getAllThemes());
                if (firebaseThemes == null) return;
                List<ThemeEntity> entities = new ArrayList<>();
                for (Theme t : firebaseThemes) {
                    ThemeEntity entity = new ThemeEntity();
                    entity.setId(t.getId());
                    entity.setName(t.getName());
                    entity.setDescription(t.getDescription());
                    entity.setThumbnailUrl(t.getThumbnailUrl());
                    entity.setPro(t.isPro());
                    entity.setDefault(t.isDefault());
                    entity.setConfigJson(t.getConfig() != null ? gson.toJson(t.getConfig()) : null);
                    entity.setUpdatedAt(t.getUpdatedAt());
                    entity.setActive(t.isActive());
                    entities.add(entity);
                }
                themeDao.deleteAll();
                themeDao.insertAll(entities);
            } catch (Exception e) {
                android.util.Log.e("WidgetRepository", "Theme sync failed", e);
            }
        });
    }

    // ==================== FIREBASE HELPERS ====================

    /**
     * Admin-only: sync from Firestore. Not called from the normal user flow.
     * The app is fully local-first; LocalSeeder seeds everything on first run.
     */
    public void syncAll() {
        // No-op in normal user flow. Call syncAllFromFirebase() from AdminFragment only.
    }

    public void syncAllFromFirebase() {
        syncWidgetsFromFirebase();
        syncCategoriesFromFirebase();
        syncThemesFromFirebase();
    }

    // Helper to convert map string to map
    public Map<String, Object> getConfigMap(String configJson) {
        if (configJson == null || configJson.isEmpty()) return null;
        try {
            return gson.fromJson(configJson, new TypeToken<Map<String, Object>>(){}.getType());
        } catch (Exception e) {
            return null;
        }
    }
}
