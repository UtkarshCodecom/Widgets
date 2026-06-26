package com.desire.widget.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.desire.widget.data.local.entity.ThemeEntity;

import java.util.List;

@Dao
public interface ThemeDao {
    @Query("SELECT * FROM themes WHERE active = 1 ORDER BY is_default DESC, updated_at DESC")
    LiveData<List<ThemeEntity>> getAllThemes();

    @Query("SELECT * FROM themes WHERE id = :id")
    LiveData<ThemeEntity> getThemeById(String id);

    @Query("SELECT * FROM themes WHERE is_default = 1 LIMIT 1")
    LiveData<ThemeEntity> getDefaultTheme();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ThemeEntity> themes);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ThemeEntity theme);

    @Query("UPDATE themes SET is_default = 0")
    void clearDefaultFlag();

    @Query("DELETE FROM themes")
    void deleteAll();
}
