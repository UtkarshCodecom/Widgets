package com.desire.widget.ui.widgets;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.desire.widget.data.local.entity.CategoryEntity;
import com.desire.widget.data.local.entity.WidgetEntity;
import com.desire.widget.data.repository.WidgetRepository;

import java.util.List;

public class WidgetsViewModel extends AndroidViewModel {
    private final WidgetRepository repository;
    private final MutableLiveData<String> selectedCategoryId = new MutableLiveData<>("all");
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> showFavoritesOnly = new MutableLiveData<>(false);

    private final LiveData<List<WidgetEntity>> widgets;
    private final LiveData<List<CategoryEntity>> categories;

    public WidgetsViewModel(Application application) {
        super(application);
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
        if (Boolean.TRUE.equals(showFavoritesOnly.getValue())) {
            return repository.getFavoriteWidgets();
        } else {
            String query = searchQuery.getValue();
            if (query != null && !query.isEmpty()) {
                return repository.searchWidgets(query);
            } else {
                return widgets;
            }
        }
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