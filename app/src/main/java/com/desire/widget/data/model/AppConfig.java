package com.desire.widget.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class AppConfig {
    @SerializedName("id")
    private String id;

    @SerializedName("latestVersion")
    private int latestVersion;

    @SerializedName("forceUpdate")
    private boolean forceUpdate;

    @SerializedName("forceUpdateMessage")
    private String forceUpdateMessage;

    @SerializedName("maintenanceMode")
    private boolean maintenanceMode;

    @SerializedName("maintenanceMessage")
    private String maintenanceMessage;

    @SerializedName("minSupportedVersion")
    private int minSupportedVersion;

    @SerializedName("featureFlags")
    private Map<String, Boolean> featureFlags;

    @SerializedName("trendingWidgetIds")
    private java.util.List<String> trendingWidgetIds;

    @SerializedName("featuredWidgetIds")
    private java.util.List<String> featuredWidgetIds;

    @SerializedName("updatedAt")
    private long updatedAt;

    public AppConfig() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getLatestVersion() { return latestVersion; }
    public void setLatestVersion(int latestVersion) { this.latestVersion = latestVersion; }

    public boolean isForceUpdate() { return forceUpdate; }
    public void setForceUpdate(boolean forceUpdate) { this.forceUpdate = forceUpdate; }

    public String getForceUpdateMessage() { return forceUpdateMessage; }
    public void setForceUpdateMessage(String forceUpdateMessage) { this.forceUpdateMessage = forceUpdateMessage; }

    public boolean isMaintenanceMode() { return maintenanceMode; }
    public void setMaintenanceMode(boolean maintenanceMode) { this.maintenanceMode = maintenanceMode; }

    public String getMaintenanceMessage() { return maintenanceMessage; }
    public void setMaintenanceMessage(String maintenanceMessage) { this.maintenanceMessage = maintenanceMessage; }

    public int getMinSupportedVersion() { return minSupportedVersion; }
    public void setMinSupportedVersion(int minSupportedVersion) { this.minSupportedVersion = minSupportedVersion; }

    public Map<String, Boolean> getFeatureFlags() { return featureFlags; }
    public void setFeatureFlags(Map<String, Boolean> featureFlags) { this.featureFlags = featureFlags; }

    public java.util.List<String> getTrendingWidgetIds() { return trendingWidgetIds; }
    public void setTrendingWidgetIds(java.util.List<String> trendingWidgetIds) { this.trendingWidgetIds = trendingWidgetIds; }

    public java.util.List<String> getFeaturedWidgetIds() { return featuredWidgetIds; }
    public void setFeaturedWidgetIds(java.util.List<String> featuredWidgetIds) { this.featuredWidgetIds = featuredWidgetIds; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
