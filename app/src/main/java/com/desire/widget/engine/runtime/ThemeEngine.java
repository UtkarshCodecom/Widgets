package com.desire.widget.engine.runtime;

import android.content.Context;

import com.desire.widget.engine.RenderTheme;
import com.desire.widget.engine.model.ThemeSpec;
import com.desire.widget.util.PreferenceManager;

/**
 * Resolves the user's currently-selected app theme into a {@link RenderTheme} for the engine.
 * Theme tokens ("@primary", "@surface"…) in widget specs resolve against this, so changing the
 * theme recolors every widget without modifying any widget JSON.
 */
public final class ThemeEngine {
    private ThemeEngine() {}

    public static RenderTheme current(Context context) {
        String id = PreferenceManager.getInstance(context).getThemeId();
        return RenderTheme.fromSpec(specFor(id));
    }

    public static ThemeSpec specFor(String id) {
        ThemeSpec t = new ThemeSpec();
        t.id = id;
        if (id == null) id = "dark_gold";
        switch (id) {
            case "midnight_blue":
                t.primary = "#00D4FF"; t.onPrimary = "#000000";
                t.background = "#050D14"; t.surface = "#0D1B2A";
                t.textPrimary = "#FFFFFFFF"; t.textSecondary = "#B3FFFFFF";
                break;
            case "rose_quartz":
                t.primary = "#FF6B9D"; t.onPrimary = "#000000";
                t.background = "#0F0709"; t.surface = "#1C1214";
                t.textPrimary = "#FFFFFFFF"; t.textSecondary = "#B3FFFFFF";
                break;
            case "forest_green":
                t.primary = "#00E676"; t.onPrimary = "#000000";
                t.background = "#040F08"; t.surface = "#0A1A0F";
                t.textPrimary = "#FFFFFFFF"; t.textSecondary = "#B3FFFFFF";
                break;
            case "amoled":
                t.primary = "#FFFFFF"; t.onPrimary = "#000000";
                t.background = "#000000"; t.surface = "#0A0A0A";
                t.textPrimary = "#FFFFFFFF"; t.textSecondary = "#B3FFFFFF";
                break;
            default: // dark_gold
                t.primary = "#FFD700"; t.onPrimary = "#000000";
                t.background = "#0D0D0D"; t.surface = "#1A1A1A";
                t.textPrimary = "#FFFFFFFF"; t.textSecondary = "#B3FFFFFF";
                break;
        }
        return t;
    }
}
