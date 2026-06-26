package com.desire.widget.ui.customize;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.desire.widget.data.local.entity.ThemeEntity;
import com.desire.widget.data.repository.WidgetRepository;
import com.desire.widget.ui.base.BaseViewModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomizeViewModel extends BaseViewModel {
    private final WidgetRepository repository;
    private final Application application;
    private final MutableLiveData<Map<String, Object>> currentConfig = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<String> selectedThemeId = new MutableLiveData<>(null);
    private final Gson gson = new Gson();

    public CustomizeViewModel(Application application) {
        this.application = application;
        repository = WidgetRepository.getInstance(application);
        loadDefaultConfig();
    }

    public LiveData<List<ThemeEntity>> getThemes() {
        return repository.getAllThemes();
    }

    public void setSelectedTheme(String themeId) {
        selectedThemeId.setValue(themeId);
    }

    public LiveData<ThemeEntity> getDefaultTheme() {
        return repository.getDefaultTheme();
    }

    public void loadDefaultConfig() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("textColor", "#FFFFFF");
        defaults.put("backgroundColor", "#FF1A1A1A");
        defaults.put("accentColor", "#FFD700");
        defaults.put("fontFamily", "sans-serif");
        defaults.put("fontSize", 14);
        defaults.put("cornerRadius", 16);
        defaults.put("padding", 16);
        defaults.put("opacity", 1.0f);
        defaults.put("borderWidth", 0);
        defaults.put("borderColor", "#33FFFFFF");
        defaults.put("shadowRadius", 8);
        defaults.put("shadowColor", "#40000000");
        defaults.put("glassEffect", false);
        defaults.put("glassOpacity", 0.2f);
        defaults.put("iconShape", "rounded");
        defaults.put("clockStyle", "digital");
        defaults.put("dateFormat", "EEE, MMM d");
        defaults.put("gradientStart", "#FFD700");
        defaults.put("gradientEnd", "#FF8C00");
        defaults.put("gradientEnabled", false);
        currentConfig.setValue(defaults);
    }

    public LiveData<Map<String, Object>> getCurrentConfig() {
        return currentConfig;
    }

    public void updateConfig(String key, Object value) {
        Map<String, Object> config = currentConfig.getValue();
        if (config != null) {
            config.put(key, value);
            currentConfig.setValue(new HashMap<>(config));
        }
    }

    public void saveConfig() {
        Map<String, Object> config = currentConfig.getValue();
        if (config != null) {
            String json = gson.toJson(config);
            com.desire.widget.util.PreferenceManager.getInstance(
                    application
            ).setSavedConfigJson(json);
        }
    }

    public void loadConfigFromJson(String json) {
        try {
            Map<String, Object> config = gson.fromJson(json,
                    new TypeToken<Map<String, Object>>(){}.getType());
            if (config != null) {
                currentConfig.setValue(config);
            }
        } catch (Exception e) {
            loadDefaultConfig();
        }
    }

    public void refresh() {
        repository.syncAll();
    }
}
