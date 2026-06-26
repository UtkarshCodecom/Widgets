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
import com.desire.widget.data.local.entity.ThemeEntity;

import java.util.ArrayList;
import java.util.List;

public class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder> {
    private List<ThemeEntity> themes = new ArrayList<>();
    private OnThemeClickListener listener;

    public interface OnThemeClickListener {
        void onThemeClick(ThemeEntity theme);
    }

    public void setOnThemeClickListener(OnThemeClickListener listener) {
        this.listener = listener;
    }

    public void setThemes(List<ThemeEntity> themes) {
        this.themes = themes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ThemeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_theme, parent, false);
        return new ThemeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ThemeViewHolder holder, int position) {
        holder.bind(themes.get(position));
    }

    @Override
    public int getItemCount() {
        return themes.size();
    }

    class ThemeViewHolder extends RecyclerView.ViewHolder {
        private final CardView card;
        private final ImageView thumbnail;
        private final TextView name;
        private final TextView description;
        private final TextView proBadge;
        private final TextView defaultBadge;

        ThemeViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.theme_card);
            thumbnail = itemView.findViewById(R.id.theme_thumbnail);
            name = itemView.findViewById(R.id.theme_name);
            description = itemView.findViewById(R.id.theme_description);
            proBadge = itemView.findViewById(R.id.pro_badge);
            defaultBadge = itemView.findViewById(R.id.default_badge);
        }

        void bind(ThemeEntity theme) {
            name.setText(theme.getName());
            description.setText(theme.getDescription());

            if (theme.isDefault()) {
                defaultBadge.setVisibility(View.VISIBLE);
            } else {
                defaultBadge.setVisibility(View.GONE);
            }

            if (theme.isPro()) {
                proBadge.setVisibility(View.VISIBLE);
            } else {
                proBadge.setVisibility(View.GONE);
            }

            if (theme.getThumbnailUrl() != null && !theme.getThumbnailUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(theme.getThumbnailUrl())
                        .placeholder(R.drawable.placeholder_widget)
                        .error(R.drawable.placeholder_widget)
                        .centerCrop()
                        .into(thumbnail);
            } else {
                thumbnail.setImageResource(R.drawable.placeholder_widget);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onThemeClick(theme);
            });
        }
    }
}
