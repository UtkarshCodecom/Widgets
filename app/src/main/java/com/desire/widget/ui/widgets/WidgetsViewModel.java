package com.desire.widget.ui.widgets;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.desire.widget.data.local.entity.CategoryEntity;
import com.desire.widget.data.local.entity.WidgetEntity;
import com.desire.widget.data.repository.WidgetRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Gallery state: the whole catalog is observed once and filtered in memory (it's ~70 rows), so the
 * category chip, the search box and the favourites toggle can be combined freely and every change
 * re-emits {@link #getItems()} immediately.
 */
public class WidgetsViewModel extends AndroidViewModel {
    public static final String ALL = "all";

    private final WidgetRepository repository;
    private final LiveData<List<WidgetEntity>> allWidgets;
    private final LiveData<List<CategoryEntity>> categories;

    private final MutableLiveData<String> selectedCategory = new MutableLiveData<>(ALL);
    private final MutableLiveData<String> query = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> favoritesOnly = new MutableLiveData<>(false);
    private final MediatorLiveData<List<GalleryItem>> items = new MediatorLiveData<>();

    public WidgetsViewModel(Application application) {
        super(application);
        repository = WidgetRepository.getInstance(application);
        allWidgets = repository.getAllWidgets();
        categories = repository.getAllCategories();

        items.addSource(allWidgets, v -> rebuild());
        items.addSource(categories, v -> rebuild());
        items.addSource(selectedCategory, v -> rebuild());
        items.addSource(query, v -> rebuild());
        items.addSource(favoritesOnly, v -> rebuild());
    }

    private void rebuild() {
        List<WidgetEntity> widgets = allWidgets.getValue();
        if (widgets == null) return; // still loading
        List<CategoryEntity> cats = categories.getValue() != null ? categories.getValue() : Collections.emptyList();
        String cat = selectedCategory.getValue() != null ? selectedCategory.getValue() : ALL;
        String q = query.getValue() != null ? query.getValue().trim() : "";
        boolean favs = Boolean.TRUE.equals(favoritesOnly.getValue());

        List<WidgetEntity> filtered = new ArrayList<>();
        for (WidgetEntity w : widgets) {
            if (favs && !w.isFavorite()) continue;
            if (!ALL.equals(cat) && !cat.equals(w.getCategoryId())) continue;
            if (!GalleryBuilder.matches(w, q)) continue;
            filtered.add(w);
        }

        if (!q.isEmpty() || (favs && ALL.equals(cat))) {
            items.setValue(GalleryBuilder.grouped(filtered, cats));
        } else if (ALL.equals(cat)) {
            items.setValue(GalleryBuilder.overview(filtered, cats));
        } else {
            items.setValue(GalleryBuilder.flat(filtered));
        }
    }

    public LiveData<List<GalleryItem>> getItems() {
        return items;
    }

    public LiveData<List<CategoryEntity>> getCategories() {
        return categories;
    }

    public String getSelectedCategory() {
        return selectedCategory.getValue() != null ? selectedCategory.getValue() : ALL;
    }

    public boolean isFavoritesOnly() {
        return Boolean.TRUE.equals(favoritesOnly.getValue());
    }

    public boolean isSearching() {
        String q = query.getValue();
        return q != null && !q.trim().isEmpty();
    }

    public void setSelectedCategory(String categoryId) {
        selectedCategory.setValue(categoryId != null ? categoryId : ALL);
    }

    public void setSearchQuery(String q) {
        query.setValue(q != null ? q : "");
    }

    public void setShowFavoritesOnly(boolean show) {
        favoritesOnly.setValue(show);
    }

    public void toggleFavorite(String widgetId, boolean isFavorite) {
        repository.toggleFavorite(widgetId, isFavorite);
    }
}
