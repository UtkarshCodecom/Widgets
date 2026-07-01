package com.desire.widget.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private static final String PREF_NAME = "widgets_prefs";
    private static PreferenceManager instance;
    private final SharedPreferences prefs;

    private PreferenceManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized PreferenceManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferenceManager(context);
        }
        return instance;
    }

    public void putString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    public String getString(String key, String defaultValue) {
        return prefs.getString(key, defaultValue);
    }

    public void putBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }

    public void putInt(String key, int value) {
        prefs.edit().putInt(key, value).apply();
    }

    public int getInt(String key, int defaultValue) {
        return prefs.getInt(key, defaultValue);
    }

    public void putLong(String key, long value) {
        prefs.edit().putLong(key, value).apply();
    }

    public long getLong(String key, long defaultValue) {
        return prefs.getLong(key, defaultValue);
    }

    public void remove(String key) {
        prefs.edit().remove(key).apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    // Specific preferences
    public boolean isFirstLaunch() {
        return getBoolean("first_launch", true);
    }

    public void setFirstLaunch(boolean value) {
        putBoolean("first_launch", value);
    }

    public int getDeveloperTapCount() {
        return getInt("developer_tap_count", 0);
    }

    public void incrementDeveloperTapCount() {
        putInt("developer_tap_count", getDeveloperTapCount() + 1);
    }

    public boolean isDeveloperModeUnlocked() {
        return getBoolean("developer_mode", false);
    }

    public void setDeveloperModeUnlocked(boolean unlocked) {
        putBoolean("developer_mode", unlocked);
    }

    public String getThemeId() {
        return getString("app_theme_id", "dark_gold");
    }

    public void setThemeId(String themeId) {
        putString("app_theme_id", themeId);
    }

    public String getSavedConfigJson() {
        return getString("saved_config", null);
    }

    public void setSavedConfigJson(String json) {
        putString("saved_config", json);
    }

    public boolean isPremium() {
        return getBoolean("is_premium", false);
    }

    public void setPremium(boolean premium) {
        putBoolean("is_premium", premium);
    }

    public long getLastSyncTime() {
        return getLong("last_sync_time", 0);
    }

    public void setLastSyncTime(long time) {
        putLong("last_sync_time", time);
    }

    public String getLastUpdateCheckVersion() {
        return getString("last_update_check_version", "0");
    }

    public void setLastUpdateCheckVersion(String version) {
        putString("last_update_check_version", version);
    }
}
