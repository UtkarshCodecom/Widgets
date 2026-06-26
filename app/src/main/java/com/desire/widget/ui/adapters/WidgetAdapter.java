package com.desire.widget.ui.adapters;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.desire.widget.R;
import com.desire.widget.data.local.entity.WidgetEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WidgetAdapter extends RecyclerView.Adapter<WidgetAdapter.WidgetViewHolder> {
    private final int gridSpanCount;
    private List<WidgetEntity> widgets = new ArrayList<>();
    private OnWidgetClickListener listener;
    private OnFavoriteClickListener favoriteListener;

    public WidgetAdapter() {
        this(2);
    }

    public WidgetAdapter(int gridSpanCount) {
        this.gridSpanCount = gridSpanCount;
    }

    public interface OnWidgetClickListener {
        void onWidgetClick(WidgetEntity widget);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(WidgetEntity widget, boolean isFavorite);
    }

    public void setOnWidgetClickListener(OnWidgetClickListener listener) {
        this.listener = listener;
    }

    public void setOnFavoriteClickListener(OnFavoriteClickListener listener) {
        this.favoriteListener = listener;
    }

    public void setWidgets(List<WidgetEntity> widgets) {
        this.widgets = widgets != null ? widgets : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<WidgetEntity> getWidgets() {
        return widgets;
    }

    public int getSpanSize(int position) {
        if (position < 0 || position >= widgets.size()) return 1;
        return isWide(widgets.get(position).getWidgetSize()) ? gridSpanCount : 1;
    }

    @NonNull
    @Override
    public WidgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_widget, parent, false);
        return new WidgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WidgetViewHolder holder, int position) {
        WidgetEntity widget = widgets.get(position);
        holder.bind(widget);
    }

    @Override
    public int getItemCount() {
        return widgets.size();
    }

    private boolean isWide(String size) {
        String normalized = normalizeSize(size);
        return normalized.equals("4x1") || normalized.equals("1x4")
                || normalized.equals("4x2") || normalized.equals("2x1")
                || normalized.equals("1x2");
    }

    private String normalizeSize(String size) {
        if (size == null || size.trim().isEmpty()) return "2x2";
        return size.trim().toLowerCase(Locale.US).replace(" ", "");
    }

    class WidgetViewHolder extends RecyclerView.ViewHolder {
        private final CardView card;
        private final FrameLayout previewArea;
        private final ImageView thumbnail;
        private final LinearLayout generatedPreview;
        private final ImageView favoriteIcon;
        private final TextView name;
        private final TextView meta;
        private final TextView starBadge;
        private final TextView sizeBadge;

        WidgetViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.widget_card);
            previewArea = itemView.findViewById(R.id.preview_area);
            thumbnail = itemView.findViewById(R.id.widget_thumbnail);
            generatedPreview = itemView.findViewById(R.id.generated_preview);
            favoriteIcon = itemView.findViewById(R.id.favorite_icon);
            name = itemView.findViewById(R.id.widget_name);
            meta = itemView.findViewById(R.id.widget_meta);
            starBadge = itemView.findViewById(R.id.star_badge);
            sizeBadge = itemView.findViewById(R.id.size_badge);
        }

        void bind(WidgetEntity widget) {
            String size = normalizeSize(widget.getWidgetSize());
            String style = normalizeStyle(widget);
            boolean wide = isWide(size);
            applyCardHeight(size, wide);

            name.setText(widget.getName());
            meta.setText(size.toUpperCase(Locale.US) + "  " + (widget.isPro() ? "PRO" : "FREE"));
            starBadge.setVisibility(widget.isPro() ? View.VISIBLE : View.GONE);
            sizeBadge.setText(sizeBadgeText(size));

            favoriteIcon.setSelected(widget.isFavorite());
            favoriteIcon.setImageResource(widget.isFavorite()
                    ? R.drawable.ic_heart_filled
                    : R.drawable.ic_heart_outline);

            String imageUrl = widget.getThumbnailUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                generatedPreview.setVisibility(View.GONE);
                thumbnail.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_widget)
                        .error(R.drawable.placeholder_widget)
                        .centerCrop()
                        .into(thumbnail);
            } else {
                thumbnail.setVisibility(View.GONE);
                generatedPreview.setVisibility(View.VISIBLE);
                renderGeneratedPreview(widget, size, style);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onWidgetClick(widget);
            });

            favoriteIcon.setOnClickListener(v -> {
                boolean newState = !widget.isFavorite();
                widget.setFavorite(newState);
                favoriteIcon.setSelected(newState);
                favoriteIcon.setImageResource(newState
                        ? R.drawable.ic_heart_filled
                        : R.drawable.ic_heart_outline);
                if (favoriteListener != null) {
                    favoriteListener.onFavoriteClick(widget, newState);
                }
            });
        }

        private void applyCardHeight(String size, boolean wide) {
            ViewGroup.LayoutParams params = previewArea.getLayoutParams();
            if (wide) {
                params.height = dp(size.equals("4x2") ? 210 : 142);
            } else if (size.equals("1x1")) {
                params.height = dp(122);
            } else {
                params.height = dp(168);
            }
            previewArea.setLayoutParams(params);
        }

        private void renderGeneratedPreview(WidgetEntity widget, String size, String style) {
            generatedPreview.removeAllViews();
            generatedPreview.setGravity(Gravity.CENTER);
            generatedPreview.setOrientation(LinearLayout.VERTICAL);

            if (style.contains("clock_digital")) {
                addDigitalClock(size);
            } else if (style.contains("clock_analog")) {
                addAnalogClock(size);
            } else if (style.contains("bar")) {
                addAiBar(widget, size);
            } else if (style.contains("folder")) {
                addIconGrid(size.equals("4x2") ? 10 : 4);
            } else {
                addIconGrid(size.equals("2x2") ? 1 : 4);
            }
        }

        private void addAiBar(WidgetEntity widget, String size) {
            generatedPreview.setOrientation(LinearLayout.HORIZONTAL);
            generatedPreview.setGravity(Gravity.CENTER_VERTICAL);
            addTextTile(firstWord(widget.getName()), 118, 56, 15, true);
            addSpacer(18, 1);
            addIconTile("✦", 50);
            addIconTile("◉", 50);
            addIconTile("◒", 50);
        }

        private void addIconGrid(int count) {
            int columns = count > 4 ? 5 : 2;
            int rows = (int) Math.ceil(count / (float) columns);
            String[] icons = {"✦", "◎", "◉", "✺", "◇", "◒", "▣", "●", "◌", "✣"};
            generatedPreview.setOrientation(LinearLayout.VERTICAL);
            generatedPreview.setGravity(Gravity.CENTER);
            for (int row = 0; row < rows; row++) {
                LinearLayout rowLayout = new LinearLayout(itemView.getContext());
                rowLayout.setGravity(Gravity.CENTER);
                rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                generatedPreview.addView(rowLayout, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                for (int col = 0; col < columns; col++) {
                    int index = row * columns + col;
                    if (index >= count) break;
                    TextView tile = makeTile(icons[index % icons.length], count > 4 ? 42 : 66, count > 4 ? 22 : 34, false);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(count > 4 ? 44 : 72), dp(count > 4 ? 44 : 72));
                    params.setMargins(dp(4), dp(4), dp(4), dp(4));
                    rowLayout.addView(tile, params);
                }
            }
        }

        private void addDigitalClock(String size) {
            String text = size.equals("2x2") ? "12\n36" : "12:36";
            TextView clock = new TextView(itemView.getContext());
            clock.setGravity(Gravity.CENTER);
            clock.setIncludeFontPadding(false);
            clock.setText(text);
            clock.setTextColor(Color.WHITE);
            clock.setTextSize(size.equals("2x2") ? 44 : 38);
            clock.setTypeface(Typeface.DEFAULT_BOLD);
            clock.setBackgroundResource(R.drawable.bg_widget_preview_black);
            generatedPreview.addView(clock, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }

        private void addAnalogClock(String size) {
            FrameLayout clock = new FrameLayout(itemView.getContext());
            TextView face = new TextView(itemView.getContext());
            face.setGravity(Gravity.CENTER);
            face.setText("12\n\n9      3\n\n6");
            face.setTextColor(Color.WHITE);
            face.setTextSize(22);
            face.setTypeface(Typeface.DEFAULT_BOLD);
            clock.addView(face, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            View hand = new View(itemView.getContext());
            hand.setBackgroundColor(itemView.getContext().getColor(R.color.yellow_accent));
            FrameLayout.LayoutParams handParams = new FrameLayout.LayoutParams(dp(3), dp(size.equals("2x2") ? 82 : 64));
            handParams.gravity = Gravity.CENTER;
            handParams.bottomMargin = dp(18);
            clock.addView(hand, handParams);
            generatedPreview.addView(clock, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }

        private void addTextTile(String text, int width, int height, int textSize, boolean accent) {
            TextView tile = makeTile(text, width, textSize, accent);
            generatedPreview.addView(tile, new LinearLayout.LayoutParams(dp(width), dp(height)));
        }

        private void addIconTile(String text, int size) {
            TextView tile = makeTile(text, size, 24, false);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(size), dp(size));
            params.setMargins(dp(5), 0, dp(5), 0);
            generatedPreview.addView(tile, params);
        }

        private TextView makeTile(String text, int size, int textSize, boolean accent) {
            TextView tile = new TextView(itemView.getContext());
            tile.setGravity(Gravity.CENTER);
            tile.setIncludeFontPadding(false);
            tile.setText(text);
            tile.setTextColor(accent ? Color.BLACK : Color.WHITE);
            tile.setTextSize(textSize);
            tile.setTypeface(Typeface.DEFAULT_BOLD);
            tile.setBackgroundResource(accent ? R.drawable.bg_premium_pill : R.drawable.bg_widget_tile);
            return tile;
        }

        private void addSpacer(int width, int height) {
            View spacer = new View(itemView.getContext());
            generatedPreview.addView(spacer, new LinearLayout.LayoutParams(dp(width), dp(height)));
        }

        private String normalizeStyle(WidgetEntity widget) {
            String style = widget.getPreviewStyle();
            if (style != null && !style.trim().isEmpty()) {
                return style.trim().toLowerCase(Locale.US);
            }
            String category = widget.getCategoryName() != null ? widget.getCategoryName().toLowerCase(Locale.US) : "";
            String nameValue = widget.getName() != null ? widget.getName().toLowerCase(Locale.US) : "";
            if (category.contains("clock digital") || nameValue.contains("digital")) return "clock_digital";
            if (category.contains("clock analog") || nameValue.contains("analog")) return "clock_analog";
            if (nameValue.contains("bar")) return "ai_bar";
            if (nameValue.contains("folder")) return "folder";
            return "icon";
        }

        private String firstWord(String value) {
            if (value == null || value.trim().isEmpty()) return "ChatGPT";
            String[] parts = value.trim().split("\\s+");
            return parts.length > 0 ? parts[0] : value;
        }

        private String sizeBadgeText(String size) {
            if (size.length() > 0) return size.substring(0, 1);
            return "2";
        }

        private int dp(int value) {
            return Math.round(value * itemView.getResources().getDisplayMetrics().density);
        }
    }
}
