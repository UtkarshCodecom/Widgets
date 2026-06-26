package com.desire.widget.ui.settings;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import android.content.pm.PackageManager;

import com.desire.widget.data.model.AppConfig;
import com.desire.widget.data.remote.FirebaseService;
import com.desire.widget.ui.base.BaseViewModel;
import com.desire.widget.util.PreferenceManager;
import com.desire.widget.util.Tasks;

public class SettingsViewModel extends BaseViewModel {
    private final Application application;
    private final PreferenceManager prefs;
    private final MutableLiveData<Integer> developerTapCount = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> developerMode = new MutableLiveData<>(false);
    private final MutableLiveData<AppConfig> appConfig = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> updateAvailable = new MutableLiveData<>(false);
    private final MutableLiveData<String> updateMessage = new MutableLiveData<>(null);

    public SettingsViewModel(Application application) {
        this.application = application;
        prefs = PreferenceManager.getInstance(application);
        developerTapCount.setValue(prefs.getDeveloperTapCount());
        developerMode.setValue(prefs.isDeveloperModeUnlocked());
    }

    public int getVersionCode() {
        try {
            return application.getPackageManager()
                    .getPackageInfo(application.getPackageName(), 0).versionCode;
        } catch (Exception e) {
            return 1;
        }
    }
    public String getVersionName() {
        try {
            return application.getPackageManager()
                    .getPackageInfo(application.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0";
        }
    }

    public LiveData<Integer> getDeveloperTapCount() { return developerTapCount; }
    public LiveData<Boolean> isDeveloperMode() { return developerMode; }
    public LiveData<AppConfig> getAppConfig() { return appConfig; }
    public LiveData<Boolean> isUpdateAvailable() { return updateAvailable; }
    public LiveData<String> getUpdateMessage() { return updateMessage; }

    public boolean isPremium() { return prefs.isPremium(); }

    public void onVersionTap() {
        int count = prefs.getDeveloperTapCount() + 1;
        prefs.incrementDeveloperTapCount();
        developerTapCount.setValue(count);

        if (count >= 7 && !prefs.isDeveloperModeUnlocked()) {
            prefs.setDeveloperModeUnlocked(true);
            developerMode.setValue(true);
        }
    }

    public void checkUpdates() {
        showLoading();
        com.desire.widget.util.AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                AppConfig config = Tasks.await(FirebaseService.getInstance().getAppConfig());
                if (config != null) {
                    appConfig.postValue(config);
                    int currentVersion = getVersionCode();
                    if (config.getLatestVersion() > currentVersion) {
                        updateAvailable.postValue(true);
                        updateMessage.postValue(config.getForceUpdateMessage() != null ?
                                config.getForceUpdateMessage() : "A new version is available");
                    } else {
                        updateAvailable.postValue(false);
                        updateMessage.postValue("No updates available");
                    }
                }
            } catch (Exception e) {
                setError("Failed to check updates");
            } finally {
                hideLoading();
            }
        });
    }

    public void restorePurchases() {
        showLoading();
        com.desire.widget.util.AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                Thread.sleep(1000);
                prefs.setPremium(true);
                hideLoading();
            } catch (Exception e) {
                setError("Restore failed");
                hideLoading();
            }
        });
    }
}
