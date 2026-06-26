package com.desire.widget.ui.admin;

import android.app.Application;
import android.net.Uri;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.desire.widget.data.model.Announcement;
import com.desire.widget.data.model.AppConfig;
import com.desire.widget.data.model.Category;
import com.desire.widget.data.model.Offer;
import com.desire.widget.data.model.Theme;
import com.desire.widget.data.model.Widget;
import com.desire.widget.data.remote.FirebaseService;
import com.desire.widget.data.remote.R2Service;
import com.desire.widget.util.AppExecutors;
import com.desire.widget.util.Tasks;

import java.util.List;

public class AdminViewModel extends AndroidViewModel {
    private final FirebaseService firebaseService;
    private final R2Service r2Service;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>(null);

    private final MutableLiveData<List<Widget>> widgets = new MutableLiveData<>();
    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private final MutableLiveData<List<Theme>> themes = new MutableLiveData<>();
    private final MutableLiveData<List<Announcement>> announcements = new MutableLiveData<>();
    private final MutableLiveData<List<Offer>> offers = new MutableLiveData<>();
    private final MutableLiveData<AppConfig> appConfig = new MutableLiveData<>();
    private final MutableLiveData<String> uploadProgress = new MutableLiveData<>(null);
    private final MutableLiveData<String> thumbnailUploadResult = new MutableLiveData<>(null);
    private final MutableLiveData<String> previewUploadResult = new MutableLiveData<>(null);
    private final MutableLiveData<String> uploadError = new MutableLiveData<>(null);

    public AdminViewModel(Application application) {
        super(application);
        firebaseService = FirebaseService.getInstance();
        r2Service = R2Service.getInstance();
    }

    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public LiveData<List<Widget>> getWidgets() { return widgets; }
    public LiveData<List<Category>> getCategories() { return categories; }
    public LiveData<List<Theme>> getThemes() { return themes; }
    public LiveData<List<Announcement>> getAnnouncements() { return announcements; }
    public LiveData<List<Offer>> getOffers() { return offers; }
    public LiveData<AppConfig> getAppConfig() { return appConfig; }
    public LiveData<String> getUploadProgress() { return uploadProgress; }
    public LiveData<String> getThumbnailUploadResult() { return thumbnailUploadResult; }
    public LiveData<String> getPreviewUploadResult() { return previewUploadResult; }
    public LiveData<String> getUploadError() { return uploadError; }

    private void showLoading() { loading.postValue(true); }
    private void hideLoading() { loading.postValue(false); }
    private void setError(String message) { error.postValue(message); }

    public void loadAll() {
        loadWidgets();
        loadCategories();
        loadThemes();
        loadAnnouncements();
        loadOffers();
        loadAppConfig();
    }

    public void loadWidgets() {
        showLoading();
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                List<Widget> result = Tasks.await(firebaseService.getAllWidgets());
                widgets.postValue(result);
            } catch (Exception e) {
                setError("Failed to load widgets");
            } finally {
                hideLoading();
            }
        });
    }

    public void loadCategories() {
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                List<Category> result = Tasks.await(firebaseService.getAllCategories());
                categories.postValue(result);
            } catch (Exception e) {
                setError("Failed to load categories");
            }
        });
    }

    public void loadThemes() {
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                List<Theme> result = Tasks.await(firebaseService.getAllThemes());
                themes.postValue(result);
            } catch (Exception e) {
                setError("Failed to load themes");
            }
        });
    }

    public void loadAnnouncements() {
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                List<Announcement> result = Tasks.await(firebaseService.getAllAnnouncements());
                announcements.postValue(result);
            } catch (Exception e) {
                setError("Failed to load announcements");
            }
        });
    }

    public void loadOffers() {
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                List<Offer> result = Tasks.await(firebaseService.getAllOffers());
                offers.postValue(result);
            } catch (Exception e) {
                setError("Failed to load offers");
            }
        });
    }

    public void loadAppConfig() {
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                AppConfig result = Tasks.await(firebaseService.getAppConfig());
                appConfig.postValue(result);
            } catch (Exception e) {
                setError("Failed to load config");
            }
        });
    }

    public void saveWidget(Widget widget) {
        showLoading();
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                Tasks.await(firebaseService.saveWidget(widget));
                loadWidgets();
            } catch (Exception e) {
                setError("Failed to save widget");
            } finally {
                hideLoading();
            }
        });
    }

    public void deleteWidget(String id) {
        showLoading();
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                Tasks.await(firebaseService.deleteWidget(id));
                loadWidgets();
            } catch (Exception e) {
                setError("Failed to delete widget");
            } finally {
                hideLoading();
            }
        });
    }

    public void saveCategory(Category category) {
        showLoading();
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                Tasks.await(firebaseService.saveCategory(category));
                loadCategories();
            } catch (Exception e) {
                setError("Failed to save category");
            } finally {
                hideLoading();
            }
        });
    }

    public void deleteCategory(String id) {
        showLoading();
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                Tasks.await(firebaseService.deleteCategory(id));
                loadCategories();
            } catch (Exception e) {
                setError("Failed to delete category");
            } finally {
                hideLoading();
            }
        });
    }

    public void saveTheme(Theme theme) {
        showLoading();
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                Tasks.await(firebaseService.saveTheme(theme));
                loadThemes();
            } catch (Exception e) {
                setError("Failed to save theme");
            } finally {
                hideLoading();
            }
        });
    }

    public void deleteTheme(String id) {
        showLoading();
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                Tasks.await(firebaseService.deleteTheme(id));
                loadThemes();
            } catch (Exception e) {
                setError("Failed to delete theme");
            } finally {
                hideLoading();
            }
        });
    }

    public void saveAnnouncement(Announcement announcement) {
        showLoading();
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                Tasks.await(firebaseService.saveAnnouncement(announcement));
                loadAnnouncements();
            } catch (Exception e) {
                setError("Failed to save announcement");
            } finally {
                hideLoading();
            }
        });
    }

    public void deleteAnnouncement(String id) {
        showLoading();
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                Tasks.await(firebaseService.deleteAnnouncement(id));
                loadAnnouncements();
            } catch (Exception e) {
                setError("Failed to delete announcement");
            } finally {
                hideLoading();
            }
        });
    }

    public void saveOffer(Offer offer) {
        showLoading();
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                Tasks.await(firebaseService.saveOffer(offer));
                loadOffers();
            } catch (Exception e) {
                setError("Failed to save offer");
            } finally {
                hideLoading();
            }
        });
    }

    public void deleteOffer(String id) {
        showLoading();
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                Tasks.await(firebaseService.deleteOffer(id));
                loadOffers();
            } catch (Exception e) {
                setError("Failed to delete offer");
            } finally {
                hideLoading();
            }
        });
    }

    public void saveAppConfig(AppConfig config) {
        showLoading();
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                Tasks.await(firebaseService.saveAppConfig(config));
                loadAppConfig();
            } catch (Exception e) {
                setError("Failed to save config");
            } finally {
                hideLoading();
            }
        });
    }

    // R2 Upload methods
    public void uploadThumbnail(Uri fileUri) {
        uploadProgress.postValue("Uploading thumbnail...");
        uploadError.postValue(null);
        r2Service.uploadWidgetThumbnail(getApplication(), fileUri, new R2Service.UploadCallback() {
            @Override
            public void onSuccess(String publicUrl) {
                uploadProgress.postValue(null);
                thumbnailUploadResult.postValue(publicUrl);
            }

            @Override
            public void onError(String error) {
                uploadProgress.postValue(null);
                uploadError.postValue("Thumbnail: " + error);
            }
        });
    }

    public void uploadPreview(Uri fileUri) {
        uploadProgress.postValue("Uploading preview...");
        uploadError.postValue(null);
        r2Service.uploadWidgetPreview(getApplication(), fileUri, new R2Service.UploadCallback() {
            @Override
            public void onSuccess(String publicUrl) {
                uploadProgress.postValue(null);
                previewUploadResult.postValue(publicUrl);
            }

            @Override
            public void onError(String error) {
                uploadProgress.postValue(null);
                uploadError.postValue("Preview: " + error);
            }
        });
    }

    public void clearUploadResults() {
        thumbnailUploadResult.postValue(null);
        previewUploadResult.postValue(null);
        uploadError.postValue(null);
    }
}
