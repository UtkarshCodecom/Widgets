package com.desire.widget.ui.base;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * Base ViewModel for ViewModels that do NOT need Application context.
 * ViewModels that need Application must extend AndroidViewModel directly.
 */
public class BaseViewModel extends ViewModel {
    protected final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    protected final MutableLiveData<String> error = new MutableLiveData<>(null);

    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<String> getError() { return error; }

    protected void showLoading() { loading.postValue(true); }
    protected void hideLoading() { loading.postValue(false); }
    protected void setError(String message) { error.postValue(message); }
}