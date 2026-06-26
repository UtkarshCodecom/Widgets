package com.desire.widget.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class Widget {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("categoryId")
    private String categoryId;

    @SerializedName("categoryName")
    private String categoryName;

    @SerializedName("thumbnailUrl")
    private String thumbnailUrl;

    @SerializedName("previewUrl")
    private String previewUrl;

    @SerializedName("configUrl")
    private String configUrl;

    @SerializedName("configJson")
    private String configJson;

    @SerializedName("widgetSize")
    private String widgetSize;

    @SerializedName("previewStyle")
    private String previewStyle;

    @SerializedName("isPro")
    private boolean isPro;

    @SerializedName("isFeatured")
    private boolean isFeatured;

    @SerializedName("isTrending")
    private boolean isTrending;

    @SerializedName("downloadCount")
    private long downloadCount;

    @SerializedName("favoriteCount")
    private long favoriteCount;

    @SerializedName("tags")
    private List<String> tags;

    @SerializedName("version")
    private int version;

    @SerializedName("createdAt")
    private long createdAt;

    @SerializedName("updatedAt")
    private long updatedAt;

    @SerializedName("active")
    private boolean active;

    @SerializedName("metadata")
    private Map<String, Object> metadata;

    public Widget() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getPreviewUrl() { return previewUrl; }
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }

    public String getConfigUrl() { return configUrl; }
    public void setConfigUrl(String configUrl) { this.configUrl = configUrl; }

    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }

    public String getWidgetSize() { return widgetSize; }
    public void setWidgetSize(String widgetSize) { this.widgetSize = widgetSize; }

    public String getPreviewStyle() { return previewStyle; }
    public void setPreviewStyle(String previewStyle) { this.previewStyle = previewStyle; }

    public boolean isPro() { return isPro; }
    public void setPro(boolean pro) { isPro = pro; }

    public boolean isFeatured() { return isFeatured; }
    public void setFeatured(boolean featured) { isFeatured = featured; }

    public boolean isTrending() { return isTrending; }
    public void setTrending(boolean trending) { isTrending = trending; }

    public long getDownloadCount() { return downloadCount; }
    public void setDownloadCount(long downloadCount) { this.downloadCount = downloadCount; }

    public long getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(long favoriteCount) { this.favoriteCount = favoriteCount; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
