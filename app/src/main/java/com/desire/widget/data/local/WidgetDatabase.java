package com.desire.widget.data.local;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import com.desire.widget.data.local.dao.CategoryDao;
import com.desire.widget.data.local.dao.ThemeDao;
import com.desire.widget.data.local.dao.WidgetDao;
import com.desire.widget.data.local.entity.CategoryEntity;
import com.desire.widget.data.local.entity.ThemeEntity;
import com.desire.widget.data.local.entity.WidgetEntity;

@Database(entities = {
        WidgetEntity.class,
        CategoryEntity.class,
        ThemeEntity.class
}, version = 1, exportSchema = false)
public abstract class WidgetDatabase extends RoomDatabase {
    private static volatile WidgetDatabase INSTANCE;

    public abstract WidgetDao widgetDao();
    public abstract CategoryDao categoryDao();
    public abstract ThemeDao themeDao();

    public static WidgetDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (WidgetDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            WidgetDatabase.class,
                            "widgets_db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
