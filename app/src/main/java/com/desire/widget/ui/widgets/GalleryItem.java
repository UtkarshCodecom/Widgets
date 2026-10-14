package com.desire.widget.ui.widgets;

import com.desire.widget.data.local.entity.WidgetEntity;

/**
 * One row of the widget gallery grid: a category header, a widget card (half width for small
 * sizes, full width for wide ones) or a "feature" card — a full-width, side-by-side card used for a
 * small widget that would otherwise sit alone in its row and leave an empty half.
 */
public final class GalleryItem {
    public static final int HEADER = 0;
    public static final int CARD = 1;
    public static final int FEATURE = 2;

    public final int type;
    public final WidgetEntity widget;   // CARD / FEATURE
    public final String title;          // HEADER
    public final String categoryId;     // HEADER: target of "See all"
    public final int count;             // HEADER: widgets in the category
    public final boolean showSeeAll;    // HEADER

    private GalleryItem(int type, WidgetEntity widget, String title, String categoryId,
                        int count, boolean showSeeAll) {
        this.type = type;
        this.widget = widget;
        this.title = title;
        this.categoryId = categoryId;
        this.count = count;
        this.showSeeAll = showSeeAll;
    }

    public static GalleryItem header(String title, String categoryId, int count, boolean showSeeAll) {
        return new GalleryItem(HEADER, null, title, categoryId, count, showSeeAll);
    }

    public static GalleryItem card(WidgetEntity widget) {
        return new GalleryItem(CARD, widget, null, null, 0, false);
    }

    public static GalleryItem feature(WidgetEntity widget) {
        return new GalleryItem(FEATURE, widget, null, null, 0, false);
    }
}
