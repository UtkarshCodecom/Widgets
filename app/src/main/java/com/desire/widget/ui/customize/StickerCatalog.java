package com.desire.widget.ui.customize;

import android.content.Context;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The set of stickers a user can drop onto a widget. Read from the admin-editable asset manifest
 * {@code assets/stickers/stickers.json}; each entry's {@code source} is a drawable resource name,
 * an {@code asset://stickers/<file>} path, or an {@code https://…} URL (so the catalog can be
 * extended with bundled art or images hosted on R2/Firebase without code changes). Falls back to a
 * built-in set if the manifest is missing or unreadable.
 */
public final class StickerCatalog {
    private static final String MANIFEST = "stickers/stickers.json";

    private StickerCatalog() {}

    public static class Sticker {
        public String name;
        public String source;
    }

    private static class Manifest {
        List<Sticker> stickers;
    }

    public static List<Sticker> load(Context context) {
        try (InputStream in = context.getAssets().open(MANIFEST);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            Manifest m = new Gson().fromJson(sb.toString(), Manifest.class);
            if (m != null && m.stickers != null && !m.stickers.isEmpty()) {
                List<Sticker> out = new ArrayList<>();
                for (Sticker s : m.stickers) {
                    // Skip entries whose art isn't present yet (e.g. a manifest reference to a
                    // bundled PNG that hasn't been dropped in), so no broken tile is shown.
                    if (s != null && exists(context, s.source)) out.add(s);
                }
                if (!out.isEmpty()) return out;
            }
        } catch (Exception ignored) {
            // fall through to defaults
        }
        return defaults();
    }

    /** True if a sticker's source is resolvable now (remote/uri assumed valid; asset/drawable checked). */
    private static boolean exists(Context ctx, String source) {
        if (source == null) return false;
        String src = source.trim();
        if (src.isEmpty()) return false;
        if (src.startsWith("http") || src.startsWith("content://") || src.startsWith("file://")) {
            return true;
        }
        if (src.startsWith("asset://")) {
            try (InputStream in = ctx.getAssets().open(src.substring("asset://".length()))) {
                return in != null;
            } catch (Exception e) {
                return false;
            }
        }
        return ctx.getResources().getIdentifier(src, "drawable", ctx.getPackageName()) != 0;
    }

    private static List<Sticker> defaults() {
        String[][] d = {
                {"Heart", "sticker_heart"}, {"Star", "sticker_star"}, {"Smile", "sticker_smile"},
                {"Sun", "sticker_sun"}, {"Cloud", "sticker_cloud"}, {"Crown", "sticker_crown"}
        };
        List<Sticker> out = new ArrayList<>();
        for (String[] row : d) {
            Sticker s = new Sticker();
            s.name = row[0];
            s.source = row[1];
            out.add(s);
        }
        return out;
    }
}
