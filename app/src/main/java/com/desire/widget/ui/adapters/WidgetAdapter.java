package com.desire.widget.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.desire.widget.R;
import com.desire.widget.data.local.entity.WidgetEntity;

import java.util.ArrayList;
import java.util.List;

public class WidgetAdapter extends RecyclerView.Adapter<WidgetAdapter.WidgetViewHolder> {
    private List<WidgetEntity> widgets = new ArrayList<>();
    private OnWidgetClickListener listener;
    private OnFavoriteClickListener favoriteListener;

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
        this.widgets = widgets;
        notifyDataSetChanged();
    }

    public List<WidgetEntity> getWidgets() {
        return widgets;
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

    class WidgetViewHolder extends RecyclerView.ViewHolder {
        private final CardView card;
        private final ImageView thumbnail;
        private final ImageView favoriteIcon;
        private final TextView name;
        private final TextView description;
        private final TextView categoryBadge;
        private final TextView proBadge;
        private final TextView freeBadge;

        WidgetViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.widget_card);
            thumbnail = itemView.findViewById(R.id.widget_thumbnail);
            favoriteIcon = itemView.findViewById(R.id.favorite_icon);
            name = itemView.findViewById(R.id.widget_name);
            description = itemView.findViewById(R.id.widget_description);
            categoryBadge = itemView.findViewById(R.id.category_badge);
            proBadge = itemView.findViewById(R.id.pro_badge);
            freeBadge = itemView.findViewById(R.id.free_badge);
        }

        void bind(WidgetEntity widget) {
            name.setText(widget.getName());
            description.setText(widget.getDescription());

            if (widget.getCategoryName() != null) {
                categoryBadge.setVisibility(View.VISIBLE);
                categoryBadge.setText(widget.getCategoryName());
            } else {
                categoryBadge.setVisibility(View.GONE);
            }

            if (widget.isPro()) {
                proBadge.setVisibility(View.VISIBLE);
                freeBadge.setVisibility(View.GONE);
            } else {
                freeBadge.setVisibility(View.VISIBLE);
                proBadge.setVisibility(View.GONE);
            }

            favoriteIcon.setSelected(widget.isFavorite());
            favoriteIcon.setImageResource(widget.isFavorite() ?
                    R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

            if (widget.getThumbnailUrl() != null && !widget.getThumbnailUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(widget.getThumbnailUrl())
                        .placeholder(R.drawable.placeholder_widget)
                        .error(R.drawable.placeholder_widget)
                        .centerCrop()
                        .into(thumbnail);
            } else {
                thumbnail.setImageResource(R.drawable.placeholder_widget);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onWidgetClick(widget);
            });

            favoriteIcon.setOnClickListener(v -> {
                boolean newState = !widget.isFavorite();
                widget.setFavorite(newState);
                favoriteIcon.setSelected(newState);
                favoriteIcon.setImageResource(newState ?
                        R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                if (favoriteListener != null) {
                    favoriteListener.onFavoriteClick(widget, newState);
                }
            });
        }
    }
}
