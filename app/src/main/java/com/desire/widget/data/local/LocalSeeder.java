package com.desire.widget.data.local;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.desire.widget.data.local.dao.CategoryDao;
import com.desire.widget.data.local.dao.ThemeDao;
import com.desire.widget.data.local.dao.WidgetDao;
import com.desire.widget.data.local.entity.CategoryEntity;
import com.desire.widget.data.local.entity.ThemeEntity;
import com.desire.widget.data.local.entity.WidgetEntity;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class LocalSeeder {
    private static final String TAG = "LocalSeeder";
    private static final String CATALOG = "seed/native/catalog.json";
    private static final String WIDGET_DIR = "seed/native/";
    /** Bump whenever catalog widgets are added/changed so existing installs pick them up. */
    private static final int SEED_VERSION = 4;
    private static final String SEED_PREFS = "seed_meta";
    private static final String KEY_SEED_VERSION = "seed_version";

    private LocalSeeder() {}

    public static void seedIfEmpty(Context context, WidgetDao widgetDao,
                                   CategoryDao categoryDao, ThemeDao themeDao) {
        try {
            if (themeDao.getCountSync() == 0) seedThemes(themeDao);

            if (widgetDao.getWidgetCountSync() == 0) {
                seedWidgets(context, widgetDao, categoryDao);      // fresh install: seed everything
            } else if (seedVersion(context) < SEED_VERSION) {
                syncWidgets(context, widgetDao, categoryDao);      // upgrade: add new widgets only
            }
            setSeedVersion(context, SEED_VERSION);
        } catch (Exception e) {
            Log.e(TAG, "Seeding failed", e);
        }
    }

    /**
     * Additively syncs the catalog into an already-seeded DB: inserts widgets that don't exist yet
     * and refreshes the content of ones that do — WITHOUT touching user state (favorites, download
     * counts). This is how new designs reach existing users without a reinstall or data wipe.
     */
    private static void syncWidgets(Context context, WidgetDao widgetDao,
                                    CategoryDao categoryDao) throws Exception {
        Catalog catalog = new Gson().fromJson(readAsset(context, CATALOG), Catalog.class);
        if (catalog == null || catalog.widgets == null || catalog.widgets.isEmpty()) return;

        if (catalog.categories != null) categoryDao.insertAll(buildCategories(catalog));

        long now = System.currentTimeMillis();
        int total = catalog.widgets.size();
        int added = 0, updated = 0;
        for (int i = 0; i < total; i++) {
            Wid w = catalog.widgets.get(i);
            String spec;
            try {
                spec = readAsset(context, WIDGET_DIR + w.specAsset);
            } catch (Exception ex) {
                Log.w(TAG, "Missing widget spec asset: " + w.specAsset);
                continue;
            }
            if (widgetDao.getWidgetByIdSync(w.id) == null) {
                widgetDao.insert(buildEntity(w, spec, now, i, total));
                added++;
            } else {
                widgetDao.syncExisting(w.id, spec, w.name, w.description, w.categoryId,
                        w.categoryName, w.widgetSize, w.isPro, w.isFeatured, w.isTrending);
                updated++;
            }
        }
        Log.i(TAG, "Catalog sync: +" + added + " new, " + updated + " refreshed");
    }

    private static int seedVersion(Context context) {
        return context.getSharedPreferences(SEED_PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_SEED_VERSION, 0);
    }

    private static void setSeedVersion(Context context, int version) {
        context.getSharedPreferences(SEED_PREFS, Context.MODE_PRIVATE)
                .edit().putInt(KEY_SEED_VERSION, version).apply();
    }

    private static void seedWidgets(Context context, WidgetDao widgetDao,
                                    CategoryDao categoryDao) throws Exception {
        Catalog catalog = new Gson().fromJson(readAsset(context, CATALOG), Catalog.class);
        if (catalog == null || catalog.widgets == null || catalog.widgets.isEmpty()) return;

        List<CategoryEntity> categories = buildCategories(catalog);

        long now = System.currentTimeMillis();
        int total = catalog.widgets.size();
        List<WidgetEntity> widgets = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            Wid w = catalog.widgets.get(i);
            String spec;
            try {
                spec = readAsset(context, WIDGET_DIR + w.specAsset);
            } catch (Exception ex) {
                Log.w(TAG, "Missing widget spec asset: " + w.specAsset);
                continue;
            }
            widgets.add(buildEntity(w, spec, now, i, total));
        }

        if (!categories.isEmpty()) categoryDao.insertAll(categories);
        widgetDao.insertAll(widgets);
        Log.i(TAG, "Seeded " + widgets.size() + " widgets, " + categories.size() + " categories");
    }

    private static List<CategoryEntity> buildCategories(Catalog catalog) {
        List<CategoryEntity> categories = new ArrayList<>();
        if (catalog.categories == null) return categories;
        for (Cat c : catalog.categories) {
            CategoryEntity e = new CategoryEntity();
            e.setId(c.id);
            e.setName(c.name);
            e.setIcon(c.icon);
            e.setColor(c.color);
            e.setOrder(c.order);
            e.setActive(true);
            categories.add(e);
        }
        return categories;
    }

    private static WidgetEntity buildEntity(Wid w, String spec, long now, int i, int total) {
        WidgetEntity e = new WidgetEntity();
        e.setId(w.id);
        e.setName(w.name);
        e.setDescription(w.description);
        e.setCategoryId(w.categoryId);
        e.setCategoryName(w.categoryName);
        e.setSpecJson(spec);
        e.setWidgetSize(w.widgetSize);
        e.setPro(w.isPro);
        e.setFeatured(w.isFeatured);
        e.setTrending(w.isTrending);
        e.setConfigJson("{}");
        e.setUpdatedAt(now - i * 1000L);
        e.setDownloadCount((total - i) * 250L);
        e.setVersion(1);
        e.setActive(true);
        return e;
    }

    private static void seedThemes(ThemeDao themeDao) {
        List<ThemeEntity> themes = new ArrayList<>();
        long now = System.currentTimeMillis();

        // Dark Gold (default)
        themes.add(makeTheme("dark_gold", "Dark Gold", "Classic dark with gold accents",
                "{\"textColor\":\"#FFFFFF\",\"backgroundColor\":\"#1A1A1A\",\"accentColor\":\"#FFD700\"," +
                "\"cornerRadius\":20,\"padding\":16,\"opacity\":1.0,\"glassEffect\":false," +
                "\"gradientEnabled\":false,\"borderWidth\":0,\"shadowRadius\":12,\"fontSize\":14," +
                "\"fontFamily\":\"sans-serif\"}", true, now));

        // Midnight Blue
        themes.add(makeTheme("midnight_blue", "Midnight Blue", "Deep ocean blue with cyan glow",
                "{\"textColor\":\"#FFFFFF\",\"backgroundColor\":\"#0D1B2A\",\"accentColor\":\"#00D4FF\"," +
                "\"cornerRadius\":20,\"padding\":16,\"opacity\":1.0,\"glassEffect\":false," +
                "\"gradientEnabled\":true,\"gradientStart\":\"#0D1B2A\",\"gradientEnd\":\"#1A3A5C\"," +
                "\"borderWidth\":1,\"borderColor\":\"#2A5C8A\",\"shadowRadius\":12,\"fontSize\":14," +
                "\"fontFamily\":\"sans-serif\"}", false, now - 1000));

        // Rose Quartz
        themes.add(makeTheme("rose_quartz", "Rose Quartz", "Warm pink with soft gradients",
                "{\"textColor\":\"#FFFFFF\",\"backgroundColor\":\"#1C1214\",\"accentColor\":\"#FF6B9D\"," +
                "\"cornerRadius\":24,\"padding\":16,\"opacity\":1.0,\"glassEffect\":true," +
                "\"glassOpacity\":0.15,\"gradientEnabled\":true," +
                "\"gradientStart\":\"#2D1520\",\"gradientEnd\":\"#1A1220\"," +
                "\"borderWidth\":1,\"borderColor\":\"#3D1A2A\",\"shadowRadius\":16,\"fontSize\":14," +
                "\"fontFamily\":\"sans-serif\"}", false, now - 2000));

        // Forest Green
        themes.add(makeTheme("forest_green", "Forest", "Deep green with emerald accents",
                "{\"textColor\":\"#FFFFFF\",\"backgroundColor\":\"#0A1A0F\",\"accentColor\":\"#00E676\"," +
                "\"cornerRadius\":16,\"padding\":16,\"opacity\":1.0,\"glassEffect\":false," +
                "\"gradientEnabled\":true,\"gradientStart\":\"#0A1A0F\",\"gradientEnd\":\"#1A3020\"," +
                "\"borderWidth\":1,\"borderColor\":\"#1A4A28\",\"shadowRadius\":10,\"fontSize\":14," +
                "\"fontFamily\":\"monospace\"}", false, now - 3000));

        // Pure Black AMOLED
        themes.add(makeTheme("amoled", "Pure Black", "AMOLED black saves battery",
                "{\"textColor\":\"#FFFFFF\",\"backgroundColor\":\"#000000\",\"accentColor\":\"#FFFFFF\"," +
                "\"cornerRadius\":14,\"padding\":12,\"opacity\":1.0,\"glassEffect\":false," +
                "\"gradientEnabled\":false,\"borderWidth\":1,\"borderColor\":\"#222222\"," +
                "\"shadowRadius\":0,\"fontSize\":14,\"fontFamily\":\"sans-serif\"}", false, now - 4000));

        themeDao.insertAll(themes);
        Log.i(TAG, "Seeded " + themes.size() + " themes");
    }

    private static ThemeEntity makeTheme(String id, String name, String desc,
                                          String config, boolean isDefault, long ts) {
        ThemeEntity e = new ThemeEntity();
        e.setId(id);
        e.setName(name);
        e.setDescription(desc);
        e.setConfigJson(config);
        e.setDefault(isDefault);
        e.setPro(false);
        e.setUpdatedAt(ts);
        e.setActive(true);
        return e;
    }

    private static String readAsset(Context context, String path) throws Exception {
        AssetManager assets = context.getAssets();
        try (InputStream in = assets.open(path);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        }
    }

    private static class Catalog {
        List<Cat> categories;
        List<Wid> widgets;
    }
    private static class Cat {
        String id, name, icon, color;
        int order;
    }
    private static class Wid {
        String id, name, description, categoryId, categoryName, widgetSize, specAsset;
        boolean isPro, isFeatured, isTrending;
    }
}
