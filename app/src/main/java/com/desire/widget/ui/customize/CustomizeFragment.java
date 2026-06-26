package com.desire.widget.ui.customize;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Map;

public class CustomizeFragment extends Fragment {
    private CustomizeViewModel viewModel;
    private ThemeAdapter themeAdapter;
    private View configPanel;
    private View studioPanel;
    private boolean isStudioVisible = false;
    private View previewWidget;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_customize, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CustomizeViewModel.class);

        RecyclerView themeRecycler = view.findViewById(R.id.theme_recycler);
        configPanel = view.findViewById(R.id.config_panel);
        studioPanel = view.findViewById(R.id.studio_panel);
        previewWidget = view.findViewById(R.id.preview_widget);
        Button toggleStudioBtn = view.findViewById(R.id.toggle_studio_btn);
        Button saveConfigBtn = view.findViewById(R.id.save_config_btn);
        Button resetConfigBtn = view.findViewById(R.id.reset_config_btn);

        themeAdapter = new ThemeAdapter();
        themeAdapter.setOnThemeClickListener(theme -> {
            if (theme.getConfigJson() != null) {
                viewModel.loadConfigFromJson(theme.getConfigJson());
            }
        });
        themeRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        themeRecycler.setAdapter(themeAdapter);

        toggleStudioBtn.setOnClickListener(v -> {
            isStudioVisible = !isStudioVisible;
            studioPanel.setVisibility(isStudioVisible ? View.VISIBLE : View.GONE);
            toggleStudioBtn.setText(isStudioVisible ? "Hide Studio" : "Widget Studio");
        });

        saveConfigBtn.setOnClickListener(v -> {
            viewModel.saveConfig();
            com.google.android.material.snackbar.Snackbar.make(view,
                    "Configuration saved", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
        });

        resetConfigBtn.setOnClickListener(v -> {
            viewModel.loadDefaultConfig();
        });

        setupStudioControls(view);

        observeData();
        viewModel.refresh();
    }

    private void setupStudioControls(View view) {
        TextInputEditText textColorInput = view.findViewById(R.id.text_color_input);
        TextInputEditText bgColorInput = view.findViewById(R.id.bg_color_input);
        TextInputEditText accentColorInput = view.findViewById(R.id.accent_color_input);

        Slider fontSizeSlider = view.findViewById(R.id.font_size_slider);
        Slider cornerRadiusSlider = view.findViewById(R.id.corner_radius_slider);
        Slider paddingSlider = view.findViewById(R.id.padding_slider);
        Slider opacitySlider = view.findViewById(R.id.opacity_slider);
        Slider borderWidthSlider = view.findViewById(R.id.border_width_slider);
        Slider shadowRadiusSlider = view.findViewById(R.id.shadow_radius_slider);
        Slider glassOpacitySlider = view.findViewById(R.id.glass_opacity_slider);

        SwitchMaterial glassEffectSwitch = view.findViewById(R.id.glass_effect_switch);
        SwitchMaterial gradientSwitch = view.findViewById(R.id.gradient_switch);
        TextInputEditText gradientStartInput = view.findViewById(R.id.gradient_start_input);
        TextInputEditText gradientEndInput = view.findViewById(R.id.gradient_end_input);

        TextInputEditText borderColorInput = view.findViewById(R.id.border_color_input);
        TextInputEditText shadowColorInput = view.findViewById(R.id.shadow_color_input);
        TextInputEditText fontFamilyInput = view.findViewById(R.id.font_family_input);

        viewModel.getCurrentConfig().observe(getViewLifecycleOwner(), config -> {
            if (config == null) return;

            textColorInput.setText(getStringConfig(config, "textColor"));
            bgColorInput.setText(getStringConfig(config, "backgroundColor"));
            accentColorInput.setText(getStringConfig(config, "accentColor"));
            fontSizeSlider.setValue(getIntConfig(config, "fontSize", 14));
            cornerRadiusSlider.setValue(getIntConfig(config, "cornerRadius", 16));
            paddingSlider.setValue(getIntConfig(config, "padding", 16));
            opacitySlider.setValue(getFloatConfig(config, "opacity", 1.0f));
            borderWidthSlider.setValue(getIntConfig(config, "borderWidth", 0));
            borderColorInput.setText(getStringConfig(config, "borderColor"));
            shadowRadiusSlider.setValue(getIntConfig(config, "shadowRadius", 8));
            shadowColorInput.setText(getStringConfig(config, "shadowColor"));
            glassEffectSwitch.setChecked(getBoolConfig(config, "glassEffect", false));
            glassOpacitySlider.setValue(getFloatConfig(config, "glassOpacity", 0.2f));
            gradientSwitch.setChecked(getBoolConfig(config, "gradientEnabled", false));
            gradientStartInput.setText(getStringConfig(config, "gradientStart"));
            gradientEndInput.setText(getStringConfig(config, "gradientEnd"));
            fontFamilyInput.setText(getStringConfig(config, "fontFamily"));

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
        fontSizeSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) viewModel.updateConfig("fontSize", (int) value);
        });
        cornerRadiusSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) viewModel.updateConfig("cornerRadius", (int) value);
        });
        paddingSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) viewModel.updateConfig("padding", (int) value);
        });
        opacitySlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) viewModel.updateConfig("opacity", value);
        });
        borderWidthSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) viewModel.updateConfig("borderWidth", (int) value);
        });
        borderColorInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) viewModel.updateConfig("borderColor", borderColorInput.getText().toString());
        });
        shadowRadiusSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) viewModel.updateConfig("shadowRadius", (int) value);
        });
        shadowColorInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) viewModel.updateConfig("shadowColor", shadowColorInput.getText().toString());
        });
        glassEffectSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.updateConfig("glassEffect", isChecked);
        });
        glassOpacitySlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) viewModel.updateConfig("glassOpacity", value);
        });
        gradientSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.updateConfig("gradientEnabled", isChecked);
        });
        gradientStartInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) viewModel.updateConfig("gradientStart", gradientStartInput.getText().toString());
        });
        gradientEndInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) viewModel.updateConfig("gradientEnd", gradientEndInput.getText().toString());
        });
        fontFamilyInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) viewModel.updateConfig("fontFamily", fontFamilyInput.getText().toString());
        });
    }

    private void updatePreview(Map<String, Object> config) {
        if (previewWidget == null) return;
        // Apply config to preview widget
        try {
            String bgColor = getStringConfig(config, "backgroundColor");
            String textColor = getStringConfig(config, "textColor");
            int cornerRadius = getIntConfig(config, "cornerRadius", 16);
            int padding = getIntConfig(config, "padding", 16);
            float opacity = getFloatConfig(config, "opacity", 1.0f);

            previewWidget.setAlpha(opacity);
            previewWidget.setPadding(
                    dpToPx(padding), dpToPx(padding),
                    dpToPx(padding), dpToPx(padding));

            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setCornerRadius(dpToPx(cornerRadius));

            boolean hasGradient = getBoolConfig(config, "gradientEnabled", false);
            if (hasGradient) {
                String startColor = getStringConfig(config, "gradientStart");
                String endColor = getStringConfig(config, "gradientEnd");
                try {
                    int start = android.graphics.Color.parseColor(startColor);
                    int end = android.graphics.Color.parseColor(endColor);
                    drawable.setOrientation(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM);
                    drawable.setColors(new int[]{start, end});
                } catch (Exception e) {
                    drawable.setColor(android.graphics.Color.parseColor(bgColor));
                }
            } else {
                try {
                    drawable.setColor(android.graphics.Color.parseColor(bgColor));
                } catch (Exception e) {
                    drawable.setColor(0xFF1A1A1A);
                }
            }

            int borderWidth = getIntConfig(config, "borderWidth", 0);
            if (borderWidth > 0) {
                String borderColor = getStringConfig(config, "borderColor");
                try {
                    drawable.setStroke(dpToPx(borderWidth), android.graphics.Color.parseColor(borderColor));
                } catch (Exception ignored) {}
            }

            boolean glassEffect = getBoolConfig(config, "glassEffect", false);
            if (glassEffect) {
                previewWidget.setBackgroundResource(R.drawable.bg_glass);
            } else {
                previewWidget.setBackground(drawable);
            }

            int shadowRadius = getIntConfig(config, "shadowRadius", 8);
            previewWidget.setElevation(dpToPx(shadowRadius) / 4f);

            TextView previewText = previewWidget.findViewById(R.id.preview_text);
            if (previewText != null) {
                try {
                    previewText.setTextColor(android.graphics.Color.parseColor(textColor));
                } catch (Exception e) {
                    previewText.setTextColor(0xFFFFFFFF);
                }
                int fontSize = getIntConfig(config, "fontSize", 14);
                previewText.setTextSize(fontSize);
            }

        } catch (Exception ignored) {}
    }

    private void observeData() {
        viewModel.getThemes().observe(getViewLifecycleOwner(), themes -> {
            themeAdapter.setThemes(themes);
        });
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private String getStringConfig(Map<String, Object> config, String key) {
        Object val = config.get(key);
        return val != null ? val.toString() : "";
    }

    private int getIntConfig(Map<String, Object> config, String key, int def) {
        Object val = config.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        return def;
    }

    private float getFloatConfig(Map<String, Object> config, String key, float def) {
        Object val = config.get(key);
        if (val instanceof Number) return ((Number) val).floatValue();
        return def;
    }

    private boolean getBoolConfig(Map<String, Object> config, String key, boolean def) {
        Object val = config.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        return def;
    }
}
