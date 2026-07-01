package com.desire.widget.ui.customize;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.desire.widget.R;
import com.desire.widget.ui.adapters.ThemeAdapter;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Map;

public class CustomizeFragment extends Fragment {
    private CustomizeViewModel viewModel;
    private ThemeAdapter themeAdapter;
    private LinearLayout studioPanel;
    private boolean isStudioVisible = false;
    private View previewWidget;
    private TextView previewText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_customize, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CustomizeViewModel.class);

        RecyclerView themeRecycler = view.findViewById(R.id.theme_recycler);
        studioPanel = view.findViewById(R.id.studio_panel);
        previewWidget = view.findViewById(R.id.preview_widget);
        previewText = view.findViewById(R.id.preview_text);
        TextView toggleStudioBtn = view.findViewById(R.id.toggle_studio_btn);
        TextView saveConfigBtn = view.findViewById(R.id.save_config_btn);
        TextView resetConfigBtn = view.findViewById(R.id.reset_config_btn);

        themeAdapter = new ThemeAdapter();
        themeAdapter.setOnThemeClickListener(theme -> {
            // Apply the app theme immediately — recreate so Android re-inflates with new theme.
            if (theme.getConfigJson() != null) {
                viewModel.loadConfigFromJson(theme.getConfigJson());
            }
            // ThemeAdapter already saved the ID; just recreate the activity.
            requireActivity().recreate();
        });
        themeRecycler.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        themeRecycler.setAdapter(themeAdapter);

        toggleStudioBtn.setOnClickListener(v -> {
            isStudioVisible = !isStudioVisible;
            studioPanel.setVisibility(isStudioVisible ? View.VISIBLE : View.GONE);
            toggleStudioBtn.setText(isStudioVisible ? "Close" : "Open");
        });

        saveConfigBtn.setOnClickListener(v -> {
            viewModel.saveConfig();
            // Re-render all placed home-screen widgets with the new studio config injected.
            viewModel.applyStudioToWidgets(requireContext());
            com.google.android.material.snackbar.Snackbar.make(view,
                    "Style saved and applied to widgets", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
        });

        resetConfigBtn.setOnClickListener(v -> viewModel.loadDefaultConfig());

        setupStudioControls(view);
        observeData();
    }

    private void setupStudioControls(View view) {
        EditText textColorInput = view.findViewById(R.id.text_color_input);
        EditText bgColorInput = view.findViewById(R.id.bg_color_input);
        EditText accentColorInput = view.findViewById(R.id.accent_color_input);
        EditText borderColorInput = view.findViewById(R.id.border_color_input);
        EditText shadowColorInput = view.findViewById(R.id.shadow_color_input);
        EditText fontFamilyInput = view.findViewById(R.id.font_family_input);
        EditText gradientStartInput = view.findViewById(R.id.gradient_start_input);
        EditText gradientEndInput = view.findViewById(R.id.gradient_end_input);

        Slider fontSizeSlider = view.findViewById(R.id.font_size_slider);
        Slider cornerRadiusSlider = view.findViewById(R.id.corner_radius_slider);
        Slider paddingSlider = view.findViewById(R.id.padding_slider);
        Slider opacitySlider = view.findViewById(R.id.opacity_slider);
        Slider borderWidthSlider = view.findViewById(R.id.border_width_slider);
        Slider shadowRadiusSlider = view.findViewById(R.id.shadow_radius_slider);
        Slider glassOpacitySlider = view.findViewById(R.id.glass_opacity_slider);

        SwitchMaterial glassEffectSwitch = view.findViewById(R.id.glass_effect_switch);
        SwitchMaterial gradientSwitch = view.findViewById(R.id.gradient_switch);

        TextView fontSizeLabel = view.findViewById(R.id.font_size_label);
        TextView cornerRadiusLabel = view.findViewById(R.id.corner_radius_label);
        TextView paddingLabel = view.findViewById(R.id.padding_label);
        TextView opacityLabel = view.findViewById(R.id.opacity_label);
        TextView borderWidthLabel = view.findViewById(R.id.border_width_label);
        TextView shadowRadiusLabel = view.findViewById(R.id.shadow_radius_label);
        TextView glassOpacityLabel = view.findViewById(R.id.glass_opacity_label);

        viewModel.getCurrentConfig().observe(getViewLifecycleOwner(), config -> {
            if (config == null) return;
            textColorInput.setText(str(config, "textColor"));
            bgColorInput.setText(str(config, "backgroundColor"));
            accentColorInput.setText(str(config, "accentColor"));
            borderColorInput.setText(str(config, "borderColor"));
            shadowColorInput.setText(str(config, "shadowColor"));
            fontFamilyInput.setText(str(config, "fontFamily"));
            gradientStartInput.setText(str(config, "gradientStart"));
            gradientEndInput.setText(str(config, "gradientEnd"));

            int fontSize = ints(config, "fontSize", 14);
            int corner = ints(config, "cornerRadius", 16);
            int padding = ints(config, "padding", 16);
            float opacity = floats(config, "opacity", 1.0f);
            int borderWidth = ints(config, "borderWidth", 0);
            int shadowRadius = ints(config, "shadowRadius", 8);
            float glassOpacity = floats(config, "glassOpacity", 0.2f);

            setSliderSafe(fontSizeSlider, fontSize);
            setSliderSafe(cornerRadiusSlider, corner);
            setSliderSafe(paddingSlider, padding);
            setSliderSafe(opacitySlider, opacity);
            setSliderSafe(borderWidthSlider, borderWidth);
            setSliderSafe(shadowRadiusSlider, shadowRadius);
            setSliderSafe(glassOpacitySlider, glassOpacity);

            if (fontSizeLabel != null) fontSizeLabel.setText(String.valueOf(fontSize));
            if (cornerRadiusLabel != null) cornerRadiusLabel.setText(String.valueOf(corner));
            if (paddingLabel != null) paddingLabel.setText(String.valueOf(padding));
            if (opacityLabel != null) opacityLabel.setText(Math.round(opacity * 100) + "%");
            if (borderWidthLabel != null) borderWidthLabel.setText(String.valueOf(borderWidth));
            if (shadowRadiusLabel != null) shadowRadiusLabel.setText(String.valueOf(shadowRadius));
            if (glassOpacityLabel != null) glassOpacityLabel.setText(Math.round(glassOpacity * 100) + "%");

            glassEffectSwitch.setChecked(bools(config, "glassEffect", false));
            gradientSwitch.setChecked(bools(config, "gradientEnabled", false));

            updatePreview(config);
        });

        textColorInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) viewModel.updateConfig("textColor", textColorInput.getText().toString());
        });
        bgColorInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) viewModel.updateConfig("backgroundColor", bgColorInput.getText().toString());
        });
        accentColorInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) viewModel.updateConfig("accentColor", accentColorInput.getText().toString());
        });
        borderColorInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) viewModel.updateConfig("borderColor", borderColorInput.getText().toString());
        });
        shadowColorInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) viewModel.updateConfig("shadowColor", shadowColorInput.getText().toString());
        });
        fontFamilyInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) viewModel.updateConfig("fontFamily", fontFamilyInput.getText().toString());
        });
        gradientStartInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) viewModel.updateConfig("gradientStart", gradientStartInput.getText().toString());
        });
        gradientEndInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) viewModel.updateConfig("gradientEnd", gradientEndInput.getText().toString());
        });

        fontSizeSlider.addOnChangeListener((s, v, fromUser) -> {
            if (fromUser) { viewModel.updateConfig("fontSize", (int) v); if (fontSizeLabel != null) fontSizeLabel.setText(String.valueOf((int) v)); }
        });
        cornerRadiusSlider.addOnChangeListener((s, v, fromUser) -> {
            if (fromUser) { viewModel.updateConfig("cornerRadius", (int) v); if (cornerRadiusLabel != null) cornerRadiusLabel.setText(String.valueOf((int) v)); }
        });
        paddingSlider.addOnChangeListener((s, v, fromUser) -> {
            if (fromUser) { viewModel.updateConfig("padding", (int) v); if (paddingLabel != null) paddingLabel.setText(String.valueOf((int) v)); }
        });
        opacitySlider.addOnChangeListener((s, v, fromUser) -> {
            if (fromUser) { viewModel.updateConfig("opacity", v); if (opacityLabel != null) opacityLabel.setText(Math.round(v * 100) + "%"); }
        });
        borderWidthSlider.addOnChangeListener((s, v, fromUser) -> {
            if (fromUser) { viewModel.updateConfig("borderWidth", (int) v); if (borderWidthLabel != null) borderWidthLabel.setText(String.valueOf((int) v)); }
        });
        shadowRadiusSlider.addOnChangeListener((s, v, fromUser) -> {
            if (fromUser) { viewModel.updateConfig("shadowRadius", (int) v); if (shadowRadiusLabel != null) shadowRadiusLabel.setText(String.valueOf((int) v)); }
        });
        glassOpacitySlider.addOnChangeListener((s, v, fromUser) -> {
            if (fromUser) { viewModel.updateConfig("glassOpacity", v); if (glassOpacityLabel != null) glassOpacityLabel.setText(Math.round(v * 100) + "%"); }
        });
        glassEffectSwitch.setOnCheckedChangeListener((btn, checked) -> viewModel.updateConfig("glassEffect", checked));
        gradientSwitch.setOnCheckedChangeListener((btn, checked) -> viewModel.updateConfig("gradientEnabled", checked));
    }

    private void updatePreview(Map<String, Object> config) {
        if (previewWidget == null) return;
        try {
            int corner = ints(config, "cornerRadius", 20);
            float opacity = floats(config, "opacity", 1f);
            boolean gradient = bools(config, "gradientEnabled", false);

            previewWidget.setAlpha(opacity);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dpToPx(corner));

            if (gradient) {
                String s = str(config, "gradientStart");
                String e = str(config, "gradientEnd");
                try {
                    bg.setOrientation(GradientDrawable.Orientation.TL_BR);
                    bg.setColors(new int[]{Color.parseColor(s.isEmpty() ? "#FFD700" : s),
                            Color.parseColor(e.isEmpty() ? "#FF8C00" : e)});
                } catch (Exception ignored) { bg.setColor(0xFF1A1A1A); }
            } else {
                try {
                    String bgHex = str(config, "backgroundColor");
                    bg.setColor(Color.parseColor(bgHex.isEmpty() ? "#1A1A1A" : bgHex));
                } catch (Exception ignored) { bg.setColor(0xFF1A1A1A); }
            }

            int bw = ints(config, "borderWidth", 0);
            if (bw > 0) {
                try { bg.setStroke(bw, Color.parseColor(str(config, "borderColor"))); }
                catch (Exception ignored) {}
            }

            boolean glass = bools(config, "glassEffect", false);
            previewWidget.setBackground(glass ? null : bg);
            if (glass) previewWidget.setBackgroundResource(R.drawable.bg_glass);
            previewWidget.setElevation(dpToPx(ints(config, "shadowRadius", 8)) / 2f);

            if (previewText != null) {
                try { previewText.setTextColor(Color.parseColor(str(config, "accentColor"))); }
                catch (Exception ignored) { previewText.setTextColor(Color.WHITE); }
            }
        } catch (Exception ignored) {}
    }

    private void observeData() {
        viewModel.getThemes().observe(getViewLifecycleOwner(), themes -> themeAdapter.setThemes(themes));
    }

    private void setSliderSafe(Slider slider, float value) {
        float from = slider.getValueFrom();
        float to = slider.getValueTo();
        float step = slider.getStepSize();
        float v = Math.max(from, Math.min(to, value));
        if (step > 0f) {
            v = from + Math.round((v - from) / step) * step;
            v = Math.max(from, Math.min(to, v));
        }
        slider.setValue(v);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private String str(Map<String, Object> c, String key) {
        Object v = c.get(key); return v != null ? v.toString() : "";
    }
    private int ints(Map<String, Object> c, String key, int def) {
        Object v = c.get(key); return v instanceof Number ? ((Number) v).intValue() : def;
    }
    private float floats(Map<String, Object> c, String key, float def) {
        Object v = c.get(key); return v instanceof Number ? ((Number) v).floatValue() : def;
    }
    private boolean bools(Map<String, Object> c, String key, boolean def) {
        Object v = c.get(key); return v instanceof Boolean ? (Boolean) v : def;
    }
}
