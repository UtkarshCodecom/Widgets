package com.desire.widget.ui.widgets;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.desire.widget.data.local.entity.CategoryEntity;
import com.desire.widget.data.local.entity.WidgetEntity;
import com.desire.widget.data.repository.WidgetRepository;
import com.desire.widget.ui.base.BaseViewModel;

import java.util.List;

public class WidgetsViewModel extends BaseViewModel {
    private final WidgetRepository repository;
    private final MutableLiveData<String> selectedCategoryId = new MutableLiveData<>("all");
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> showFavoritesOnly = new MutableLiveData<>(false);

    private LiveData<List<WidgetEntity>> widgets;
    private LiveData<List<CategoryEntity>> categories;

    public WidgetsViewModel(Application application) {
        repository = WidgetRepository.getInstance(application);
        categories = repository.getAllCategories();
        widgets = Transformations.switchMap(selectedCategoryId, categoryId -> {
            if ("all".equals(categoryId)) {
                return repository.getAllWidgets();
            } else {
                return repository.getWidgetsByCategory(categoryId);
            }
        });
    }

    public LiveData<List<WidgetEntity>> getWidgets() {
        LiveData<List<WidgetEntity>> source;
        if (Boolean.TRUE.equals(showFavoritesOnly.getValue())) {
            source = repository.getFavoriteWidgets();
        } else {
            String query = searchQuery.getValue();
            if (query != null && !query.isEmpty()) {
                source = repository.searchWidgets(query);
            } else {
                source = widgets;
            }
        }
        return source;
    }

    public LiveData<List<CategoryEntity>> getCategories() {
        return categories;
    }

    public void setSelectedCategory(String categoryId) {
        selectedCategoryId.setValue(categoryId);
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }

    public void setShowFavoritesOnly(boolean show) {
        showFavoritesOnly.setValue(show);
    }

    public void toggleFavorite(String widgetId, boolean isFavorite) {
        repository.toggleFavorite(widgetId, isFavorite);
    }

    public void refresh() {
        repository.syncAll();
    }
}
