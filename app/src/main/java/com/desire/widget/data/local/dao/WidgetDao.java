package com.desire.widget.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.desire.widget.data.local.entity.WidgetEntity;

import java.util.List;

@Dao
public interface WidgetDao {
    @Query("SELECT * FROM widgets WHERE active = 1 ORDER BY updated_at DESC")
    LiveData<List<WidgetEntity>> getAllWidgets();

    @Query("SELECT * FROM widgets WHERE active = 1 AND category_id = :categoryId ORDER BY updated_at DESC")
    LiveData<List<WidgetEntity>> getWidgetsByCategory(String categoryId);

    @Query("SELECT * FROM widgets WHERE active = 1 AND is_favorite = 1 ORDER BY updated_at DESC")
    LiveData<List<WidgetEntity>> getFavoriteWidgets();

    @Query("SELECT * FROM widgets WHERE active = 1 AND (name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') ORDER BY updated_at DESC")
    LiveData<List<WidgetEntity>> searchWidgets(String query);

    @Query("SELECT * FROM widgets WHERE active = 1 AND is_featured = 1 ORDER BY updated_at DESC")
    LiveData<List<WidgetEntity>> getFeaturedWidgets();

    @Query("SELECT * FROM widgets WHERE active = 1 AND is_trending = 1 ORDER BY download_count DESC")
    LiveData<List<WidgetEntity>> getTrendingWidgets();

    @Query("SELECT * FROM widgets WHERE id = :id")
    LiveData<WidgetEntity> getWidgetById(String id);

    @Query("SELECT * FROM widgets WHERE id = :id")
    WidgetEntity getWidgetByIdSync(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<WidgetEntity> widgets);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WidgetEntity widget);

    @Update
    void update(WidgetEntity widget);

    @Query("UPDATE widgets SET is_favorite = :isFavorite WHERE id = :id")
    void setFavorite(String id, boolean isFavorite);

    @Query("DELETE FROM widgets WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM widgets")
    void deleteAll();
}
