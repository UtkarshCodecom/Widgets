package com.desire.widget.ui.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.desire.widget.R;
import com.desire.widget.data.local.entity.ThemeEntity;
import com.desire.widget.util.ThemeManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder> {
    private List<ThemeEntity> themes = new ArrayList<>();
    private OnThemeClickListener listener;
    private int selectedPosition = 0;

    public interface OnThemeClickListener {
        void onThemeClick(ThemeEntity theme);
    }

    public void setOnThemeClickListener(OnThemeClickListener listener) {
        this.listener = listener;
    }

    public void setThemes(List<ThemeEntity> themes) {
        this.themes = themes != null ? themes : new ArrayList<>();
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
        holder.bind(themes.get(position), position == selectedPosition);
    }

    @Override
    public int getItemCount() { return themes.size(); }

    class ThemeViewHolder extends RecyclerView.ViewHolder {
        private final View swatch;
        private final View swatchAccent;
        private final View swatchSecondary;
        private final View swatchSurface;
        private final TextView name;
        private final TextView description;
        private final View selectedIndicator;

        ThemeViewHolder(@NonNull View itemView) {
            super(itemView);
            swatch = itemView.findViewById(R.id.theme_swatch);
            swatchAccent = itemView.findViewById(R.id.theme_swatch_accent);
            swatchSecondary = itemView.findViewById(R.id.theme_swatch_secondary);
            swatchSurface = itemView.findViewById(R.id.theme_swatch_surface);
            name = itemView.findViewById(R.id.theme_name);
            description = itemView.findViewById(R.id.theme_description);
            selectedIndicator = itemView.findViewById(R.id.theme_selected_indicator);
        }

        void bind(ThemeEntity theme, boolean selected) {
            name.setText(theme.getName());
            description.setText(theme.getDescription());

            int bgColor = 0xFF1A1A1A;
            int accentColor = 0xFFFFD700;
            int secondaryColor = 0xFFFF8C00;
            int surfaceColor = 0xFF2A2A2A;

            if (theme.getConfigJson() != null) {
                try {
                    Map<String, Object> cfg = new Gson().fromJson(theme.getConfigJson(),
                            new TypeToken<Map<String, Object>>(){}.getType());
                    if (cfg != null) {
                        Object bg = cfg.get("backgroundColor");
                        Object ac = cfg.get("accentColor");
                        if (bg != null) bgColor = tryParseColor(bg.toString(), bgColor);
                        if (ac != null) accentColor = tryParseColor(ac.toString(), accentColor);
                    }
                } catch (Exception ignored) {}
            }

            // Derive secondary / surface from bg and accent for visual variety
            secondaryColor = blendColor(accentColor, bgColor, 0.5f);
            surfaceColor = blendColor(bgColor, 0xFF333333, 0.4f);

            setRoundColor(swatch, bgColor, 14);
            setRoundColor(swatchAccent, accentColor, 6);
            setRoundColor(swatchSecondary, secondaryColor, 6);
            setRoundColor(swatchSurface, surfaceColor, 6);

            if (selectedIndicator != null) {
                selectedIndicator.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
                if (selected) selectedIndicator.setBackgroundColor(accentColor);
            }

            itemView.setOnClickListener(v -> {
                int old = selectedPosition;
                selectedPosition = getAdapterPosition();
                if (old != selectedPosition) {
                    notifyItemChanged(old);
                    notifyItemChanged(selectedPosition);
                }
                Context ctx = v.getContext();
                ThemeManager.save(ctx, theme.getId());
                if (listener != null) listener.onThemeClick(theme);
            });
        }

        private void setRoundColor(View view, int color, int radiusDp) {
            if (view == null) return;
            GradientDrawable d = new GradientDrawable();
            d.setColor(color);
            d.setCornerRadius(dpToPx(radiusDp));
            view.setBackground(d);
        }

        private int tryParseColor(String hex, int fallback) {
            try { return Color.parseColor(hex); } catch (Exception e) { return fallback; }
        }

        private int blendColor(int c1, int c2, float ratio) {
            int r = Math.round(Color.red(c1) * ratio + Color.red(c2) * (1 - ratio));
            int g = Math.round(Color.green(c1) * ratio + Color.green(c2) * (1 - ratio));
            int b = Math.round(Color.blue(c1) * ratio + Color.blue(c2) * (1 - ratio));
            return Color.rgb(
                    Math.min(255, Math.max(0, r)),
                    Math.min(255, Math.max(0, g)),
                    Math.min(255, Math.max(0, b)));
        }

        private int dpToPx(int dp) {
            return Math.round(dp * itemView.getResources().getDisplayMetrics().density);
        }
    }
}
