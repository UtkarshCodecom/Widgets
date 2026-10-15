package com.desire.widget.ui.widgets;

import com.desire.widget.data.local.entity.CategoryEntity;
import com.desire.widget.data.local.entity.WidgetEntity;
import com.desire.widget.widget.WidgetSchema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns the flat widget list into gallery rows. Two rules keep the grid tidy:
 * <ul>
 *   <li><b>Grouped</b> — widgets of one category always sit together under their header, in the
 *   catalog's curated order (digital clocks next to digital clocks, dials next to dials).</li>
 *   <li><b>No holes</b> — small widgets come first, two per row, then the full-width ones. If a
 *   group has an odd number of small widgets, the last one becomes a full-width feature card so no
 *   row is left half empty.</li>
 * </ul>
 */
public final class GalleryBuilder {
    /** On the "All" overview each category shows a short preview; "See all" opens the rest. */
    private static final int OVERVIEW_SMALL = 4;
    private static final int OVERVIEW_WIDE = 2;

    private GalleryBuilder() {}

    /** The "All" overview: every category as a header plus a short, gap-free preview. */
    public static List<GalleryItem> overview(List<WidgetEntity> widgets, List<CategoryEntity> categories) {
        Map<String, List<WidgetEntity>> groups = group(widgets, categories);
        List<GalleryItem> out = new ArrayList<>();
        for (Map.Entry<String, List<WidgetEntity>> e : groups.entrySet()) {
            List<WidgetEntity> list = e.getValue();
            if (list.isEmpty()) continue;
            List<WidgetEntity> small = new ArrayList<>();
            List<WidgetEntity> wide = new ArrayList<>();
            split(list, small, wide);

            // OVERVIEW_SMALL is even, so an odd count only happens when the whole group is odd —
            // pack() then turns the last one into a feature card.
            int smallShown = Math.min(OVERVIEW_SMALL, small.size());
            int wideShown = Math.min(smallShown == 0 ? OVERVIEW_WIDE + 1 : OVERVIEW_WIDE, wide.size());
            boolean more = smallShown + wideShown < list.size();

            out.add(GalleryItem.header(titleOf(e.getKey(), list, categories), e.getKey(), list.size(), more));
            pack(small.subList(0, smallShown), wide.subList(0, wideShown), out);
        }
        return out;
    }

    /** One category, a search result or the favourites: everything, packed without holes. */
    public static List<GalleryItem> flat(List<WidgetEntity> widgets) {
        List<WidgetEntity> small = new ArrayList<>();
        List<WidgetEntity> wide = new ArrayList<>();
        split(widgets, small, wide);
        List<GalleryItem> out = new ArrayList<>();
        pack(small, wide, out);
        return out;
    }

    /** Search results grouped by category, so a query like "clock" still reads in sections. */
    public static List<GalleryItem> grouped(List<WidgetEntity> widgets, List<CategoryEntity> categories) {
        List<GalleryItem> out = new ArrayList<>();
        for (Map.Entry<String, List<WidgetEntity>> e : group(widgets, categories).entrySet()) {
            if (e.getValue().isEmpty()) continue;
            out.add(GalleryItem.header(titleOf(e.getKey(), e.getValue(), categories), e.getKey(),
                    e.getValue().size(), false));
            out.addAll(flat(e.getValue()));
        }
        return out;
    }

    public static boolean matches(WidgetEntity w, String query) {
        if (query == null || query.trim().isEmpty()) return true;
        String q = query.trim().toLowerCase(Locale.ROOT);
        return contains(w.getName(), q) || contains(w.getDescription(), q)
                || contains(w.getCategoryName(), q);
    }

    // ------------------------------------------------------------------ internals

    private static void pack(List<WidgetEntity> small, List<WidgetEntity> wide, List<GalleryItem> out) {
        int pairs = small.size() / 2 * 2;
        for (int i = 0; i < pairs; i++) out.add(GalleryItem.card(small.get(i)));
        if (pairs < small.size()) out.add(GalleryItem.feature(small.get(pairs)));
        for (WidgetEntity w : wide) out.add(GalleryItem.card(w));
    }

    private static void split(List<WidgetEntity> list, List<WidgetEntity> small, List<WidgetEntity> wide) {
        for (WidgetEntity w : list) {
            if (WidgetSchema.isWide(w.getWidgetSize())) wide.add(w);
            else small.add(w);
        }
    }

    /** Category id → widgets, in category order; unknown categories collect at the end. */
    private static Map<String, List<WidgetEntity>> group(List<WidgetEntity> widgets, List<CategoryEntity> categories) {
        Map<String, List<WidgetEntity>> groups = new LinkedHashMap<>();
        if (categories != null) {
            for (CategoryEntity c : categories) groups.put(c.getId(), new ArrayList<>());
        }
        if (widgets == null) return groups;
        for (WidgetEntity w : widgets) {
            String id = w.getCategoryId() != null ? w.getCategoryId() : "other";
            List<WidgetEntity> list = groups.get(id);
            if (list == null) {
                list = new ArrayList<>();
                groups.put(id, list);
            }
            list.add(w);
        }
        return groups;
    }

    private static String titleOf(String categoryId, List<WidgetEntity> list, List<CategoryEntity> categories) {
        if (categories != null) {
            for (CategoryEntity c : categories) {
                if (c.getId().equals(categoryId)) return c.getName();
            }
        }
        String name = list.isEmpty() ? null : list.get(0).getCategoryName();
        return name != null && !name.isEmpty() ? name : "More";
    }

    private static boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }
}
