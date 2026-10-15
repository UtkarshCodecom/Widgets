package com.desire.widget.ui.widgets;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.desire.widget.R;
import com.desire.widget.data.local.entity.WidgetEntity;
import com.desire.widget.widget.WidgetSchema;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The widget gallery: category headers, half-width cards for small widgets, full-width cards for
 * wide ones and side-by-side feature cards for an odd one out (see {@link GalleryBuilder}).
 * Every visible preview is a {@link LivePreview}, ticked once a second while the screen is
 * resumed, so the clocks run in real time.
 */
public class GalleryAdapter extends ListAdapter<GalleryItem, RecyclerView.ViewHolder> {
    public static final int SPAN_COUNT = 2;

    public interface Listener {
        void onWidgetClick(WidgetEntity widget);
        void onFavoriteClick(WidgetEntity widget, boolean favorite);
        void onSeeAll(String categoryId);
    }

    private final Listener listener;
    private final Set<CardHolder> visible = new HashSet<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean live;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            for (CardHolder h : visible) h.preview.tick(now);
            // Land just after the next whole second so second hands step in sync with the clock.
            handler.postDelayed(this, 1000 - now % 1000 + 15);
        }
    };

    public GalleryAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    /** Start/stop the real-time tick (call from onResume/onPause). */
    public void setLive(boolean live) {
        if (this.live == live) return;
        this.live = live;
        handler.removeCallbacks(ticker);
        if (live) {
            for (CardHolder h : visible) h.preview.render(); // catch up after being paused
            handler.post(ticker);
        }
    }

    public int getSpanSize(int position) {
        if (position < 0 || position >= getItemCount()) return SPAN_COUNT;
        GalleryItem item = getItem(position);
        if (item.type == GalleryItem.CARD && !WidgetSchema.isWide(item.widget.getWidgetSize())) return 1;
        return SPAN_COUNT;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == GalleryItem.HEADER) {
            return new HeaderHolder(inf.inflate(R.layout.item_gallery_header, parent, false));
        }
        int layout = viewType == GalleryItem.FEATURE ? R.layout.item_widget_feature : R.layout.item_widget;
        return new CardHolder(inf.inflate(layout, parent, false), viewType == GalleryItem.FEATURE);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        GalleryItem item = getItem(position);
        if (holder instanceof HeaderHolder) ((HeaderHolder) holder).bind(item);
        else ((CardHolder) holder).bind(item.widget);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof CardHolder) visible.add((CardHolder) holder);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof CardHolder) visible.remove(holder);
    }

    // ------------------------------------------------------------------ holders

    class HeaderHolder extends RecyclerView.ViewHolder {
        private final TextView title, count, seeAll;

        HeaderHolder(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.header_title);
            count = v.findViewById(R.id.header_count);
            seeAll = v.findViewById(R.id.header_see_all);
        }

        void bind(GalleryItem item) {
            title.setText(item.title);
            count.setText(String.valueOf(item.count));
            seeAll.setVisibility(item.showSeeAll ? View.VISIBLE : View.GONE);
            seeAll.setOnClickListener(v -> listener.onSeeAll(item.categoryId));
        }
    }

    class CardHolder extends RecyclerView.ViewHolder {
        private final boolean feature;
        private final View previewArea;
        private final ImageView thumbnail;
        private final TextView name, size, description;
        private final ImageView favorite;
        final LivePreview preview;
        private int[] ratio = {1, 1};

        CardHolder(@NonNull View v, boolean feature) {
            super(v);
            this.feature = feature;
            previewArea = v.findViewById(R.id.preview_area);
            thumbnail = v.findViewById(R.id.widget_thumbnail);
            name = v.findViewById(R.id.widget_name);
            size = v.findViewById(R.id.widget_size);
            description = v.findViewById(R.id.widget_description);
            favorite = v.findViewById(R.id.favorite_icon);
            preview = new LivePreview(thumbnail);
            previewArea.addOnLayoutChangeListener((view, l, t, r, b, ol, ot, or, ob) -> applyRatio());
        }

        void bind(WidgetEntity w) {
            String sz = WidgetSchema.normalizeSize(w.getWidgetSize());
            int[] ratio = WidgetSchema.previewRatio(sz);
            sizePreview(ratio);

            name.setText(w.getName());
            String tag = sz.replace('x', '×');
            size.setText(w.isPro() ? "PRO · " + tag : tag);
            if (description != null) {
                String d = w.getDescription();
                description.setText(d != null ? d : "");
                description.setVisibility(d == null || d.isEmpty() ? View.GONE : View.VISIBLE);
            }
            setFavoriteIcon(w.isFavorite());

            String spec = w.getSpecJson();
            if (spec != null && !spec.trim().isEmpty()) {
                preview.bind(w.getId() + "#" + spec.hashCode(), spec, ratio);
            } else {
                thumbnail.setImageResource(R.drawable.placeholder_widget);
            }

            itemView.setOnClickListener(v -> listener.onWidgetClick(w));
            favorite.setOnClickListener(v -> {
                boolean next = !w.isFavorite();
                w.setFavorite(next);
                setFavoriteIcon(next);
                v.animate().scaleX(1.25f).scaleY(1.25f).setDuration(90).withEndAction(() ->
                        v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()).start();
                listener.onFavoriteClick(w, next);
            });
        }

        private void setFavoriteIcon(boolean fav) {
            favorite.setImageResource(fav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        }

        /** Preview box height = width × the widget's aspect ratio, so nothing is stretched. */
        private void sizePreview(int[] ratio) {
            this.ratio = ratio;
            if (previewArea.getWidth() > 0) {
                applyRatio();
            } else if (!feature) {
                // Before the first layout: a close estimate avoids a visible jump.
                float density = previewArea.getResources().getDisplayMetrics().density;
                previewArea.getLayoutParams().height = Math.round(WidgetSchema.previewHeightDp(
                        ratio[0] == ratio[1] ? WidgetSchema.SIZE_2X2 : WidgetSchema.SIZE_4X2) * density);
                previewArea.requestLayout();
            }
        }

        /** Runs on bind and after every layout (a recycled holder may change span/width). */
        private void applyRatio() {
            int width = previewArea.getWidth();
            if (width <= 0) return;
            int inner = width - previewArea.getPaddingLeft() - previewArea.getPaddingRight();
            int target = inner * ratio[1] / ratio[0] + previewArea.getPaddingTop() + previewArea.getPaddingBottom();
            ViewGroup.LayoutParams lp = previewArea.getLayoutParams();
            if (lp.height != target) {
                lp.height = target;
                // Never request a layout from inside a layout pass.
                previewArea.post(previewArea::requestLayout);
            }
        }
    }

    // ------------------------------------------------------------------ diffing

    private static final DiffUtil.ItemCallback<GalleryItem> DIFF = new DiffUtil.ItemCallback<GalleryItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull GalleryItem a, @NonNull GalleryItem b) {
            if (a.type != b.type) return false;
            if (a.type == GalleryItem.HEADER) return Objects.equals(a.categoryId, b.categoryId);
            return Objects.equals(a.widget.getId(), b.widget.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull GalleryItem a, @NonNull GalleryItem b) {
            if (a.type == GalleryItem.HEADER) {
                return Objects.equals(a.title, b.title) && a.count == b.count && a.showSeeAll == b.showSeeAll;
            }
            WidgetEntity x = a.widget, y = b.widget;
            return Objects.equals(x.getName(), y.getName())
                    && x.isFavorite() == y.isFavorite()
                    && x.isPro() == y.isPro()
                    && Objects.equals(x.getWidgetSize(), y.getWidgetSize())
                    && Objects.equals(x.getDescription(), y.getDescription())
                    && Objects.equals(x.getSpecJson(), y.getSpecJson());
        }
    };
}
