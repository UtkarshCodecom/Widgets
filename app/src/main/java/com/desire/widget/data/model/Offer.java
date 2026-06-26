package com.desire.widget.data.model;

import com.google.gson.annotations.SerializedName;

public class Offer {
    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("discountPercent")
    private int discountPercent;

    @SerializedName("code")
    private String code;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("startAt")
    private long startAt;

    @SerializedName("endAt")
    private long endAt;

    @SerializedName("active")
    private boolean active;

    @SerializedName("createdAt")
    private long createdAt;

    public Offer() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(int discountPercent) { this.discountPercent = discountPercent; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public long getStartAt() { return startAt; }
    public void setStartAt(long startAt) { this.startAt = startAt; }

    public long getEndAt() { return endAt; }
    public void setEndAt(long endAt) { this.endAt = endAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
