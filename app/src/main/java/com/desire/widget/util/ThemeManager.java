package com.desire.widget.util;

import android.app.Activity;
import android.content.Context;

import com.desire.widget.R;

public final class ThemeManager {
    private ThemeManager() {}

    public static int getThemeRes(String themeId) {
        if (themeId == null) return R.style.Theme_Widgets;
        switch (themeId) {
            case "midnight_blue": return R.style.Theme_Widgets_MidnightBlue;
            case "rose_quartz":   return R.style.Theme_Widgets_Rose;
            case "forest_green":  return R.style.Theme_Widgets_Forest;
            case "amoled":        return R.style.Theme_Widgets_AMOLED;
            default:              return R.style.Theme_Widgets; // dark_gold
        }
    }

    public static void apply(Activity activity) {
        String id = PreferenceManager.getInstance(activity).getThemeId();
        activity.setTheme(getThemeRes(id));
    }

    public static void save(Context context, String themeId) {
        PreferenceManager.getInstance(context).setThemeId(themeId);
    }
}
