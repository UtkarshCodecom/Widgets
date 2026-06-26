package com.desire.widget.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.desire.widget.data.local.entity.CategoryEntity;

import java.util.List;

@Dao
public interface CategoryDao {
    @Query("SELECT * FROM categories WHERE is_active = 1 ORDER BY sort_order ASC")
    LiveData<List<CategoryEntity>> getAllCategories();

    @Query("SELECT * FROM categories WHERE id = :id")
    LiveData<CategoryEntity> getCategoryById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CategoryEntity> categories);

    @Query("DELETE FROM categories")
    void deleteAll();
}
