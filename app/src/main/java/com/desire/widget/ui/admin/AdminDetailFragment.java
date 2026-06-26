package com.desire.widget.ui.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.desire.widget.R;
import com.desire.widget.data.model.Announcement;
import com.desire.widget.data.model.AppConfig;
import com.desire.widget.data.model.Category;
import com.desire.widget.data.model.Offer;
import com.desire.widget.data.model.Theme;
import com.desire.widget.data.model.Widget;
import com.desire.widget.data.remote.FirebaseService;
import com.desire.widget.util.Tasks;
import com.desire.widget.util.AppExecutors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminDetailFragment extends Fragment {
    private static final String ARG_SECTION = "section";
    private String section;
    private AdminViewModel viewModel;
    private LinearLayout contentContainer;
    private LinearLayout formContainer;
    private View listView;
    private View formView;

    public static AdminDetailFragment newInstance(String section) {
        AdminDetailFragment fragment = new AdminDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SECTION, section);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            section = getArguments().getString(ARG_SECTION);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);

        TextView title = view.findViewById(R.id.section_title);
        contentContainer = view.findViewById(R.id.content_container);
        formContainer = view.findViewById(R.id.form_container);
        listView = view.findViewById(R.id.list_view);
        formView = view.findViewById(R.id.form_view);
        Button addButton = view.findViewById(R.id.add_button);
        Button backButton = view.findViewById(R.id.back_button);

        title.setText(getSectionTitle());
        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        addButton.setOnClickListener(v -> showAddForm());

        loadSectionData();
    }

    private String getSectionTitle() {
        switch (section) {
            case "Widgets": return "Widget Management";
            case "Themes": return "Theme Management";
            case "Categories": return "Category Management";
            case "Announcements": return "Announcements";
            case "Offers": return "Offers";
            case "FeatureFlags": return "Feature Flags";
            case "Analytics": return "Analytics";
            case "AppConfig": return "App Configuration";
            default: return section;
        }
    }

    private void loadSectionData() {
        switch (section) {
            case "Widgets":
                viewModel.getWidgets().observe(getViewLifecycleOwner(), this::renderWidgetList);
                viewModel.loadWidgets();
                break;
            case "Categories":
                viewModel.getCategories().observe(getViewLifecycleOwner(), this::renderCategoryList);
                viewModel.loadCategories();
                break;
            case "Themes":
                viewModel.getThemes().observe(getViewLifecycleOwner(), this::renderThemeList);
                viewModel.loadThemes();
                break;
            case "Announcements":
                viewModel.getAnnouncements().observe(getViewLifecycleOwner(), this::renderAnnouncementList);
                viewModel.loadAnnouncements();
                break;
            case "Offers":
                viewModel.getOffers().observe(getViewLifecycleOwner(), this::renderOfferList);
                viewModel.loadOffers();
                break;
            case "FeatureFlags":
                viewModel.getAppConfig().observe(getViewLifecycleOwner(), this::renderFeatureFlags);
                viewModel.loadAppConfig();
                break;
            case "Analytics":
                showAnalyticsView();
                break;
            case "AppConfig":
                viewModel.getAppConfig().observe(getViewLifecycleOwner(), this::renderAppConfig);
                viewModel.loadAppConfig();
                break;
        }
    }

    private void renderWidgetList(List<Widget> widgets) {
        contentContainer.removeAllViews();
        if (widgets == null || widgets.isEmpty()) {
            addEmptyView("No widgets yet. Tap + to add one.");
            return;
        }
        for (Widget w : widgets) {
            contentContainer.addView(createWidgetItemView(w));
        }
    }

    private void renderCategoryList(List<Category> categories) {
        contentContainer.removeAllViews();
        if (categories == null || categories.isEmpty()) {
            addEmptyView("No categories yet. Tap + to add one.");
            return;
        }
        for (Category c : categories) {
            contentContainer.addView(createCategoryItemView(c));
        }
    }

    private void renderThemeList(List<Theme> themes) {
        contentContainer.removeAllViews();
        if (themes == null || themes.isEmpty()) {
            addEmptyView("No themes yet. Tap + to add one.");
            return;
        }
        for (Theme t : themes) {
            contentContainer.addView(createThemeItemView(t));
        }
    }

    private void renderAnnouncementList(List<Announcement> announcements) {
        contentContainer.removeAllViews();
        if (announcements == null || announcements.isEmpty()) {
            addEmptyView("No announcements. Tap + to add one.");
            return;
        }
        for (Announcement a : announcements) {
            contentContainer.addView(createAnnouncementItemView(a));
        }
    }

    private void renderOfferList(List<Offer> offers) {
        contentContainer.removeAllViews();
        if (offers == null || offers.isEmpty()) {
            addEmptyView("No offers. Tap + to add one.");
            return;
        }
        for (Offer o : offers) {
            contentContainer.addView(createOfferItemView(o));
        }
    }

    private void renderFeatureFlags(AppConfig config) {
        contentContainer.removeAllViews();
        if (config == null || config.getFeatureFlags() == null) {
            addEmptyView("No feature flags configured.");
            return;
        }
        for (Map.Entry<String, Boolean> entry : config.getFeatureFlags().entrySet()) {
            contentContainer.addView(createFlagItemView(entry.getKey(), entry.getValue(), config));
        }
    }

    private void renderAppConfig(AppConfig config) {
        contentContainer.removeAllViews();
        if (config == null) {
            addEmptyView("No configuration found.");
            return;
        }
        contentContainer.addView(createAppConfigView(config));
    }

    private void showAnalyticsView() {
        contentContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View analyticsView = inflater.inflate(R.layout.view_analytics, contentContainer, false);
        contentContainer.addView(analyticsView);

        viewModel.getWidgets().observe(getViewLifecycleOwner(), widgets -> {
            if (widgets != null) {
                ((TextView) analyticsView.findViewById(R.id.total_widgets)).setText(String.valueOf(widgets.size()));
                long totalDownloads = 0;
                int proCount = 0;
                for (Widget w : widgets) {
                    totalDownloads += w.getDownloadCount();
                    if (w.isPro()) proCount++;
                }
                ((TextView) analyticsView.findViewById(R.id.total_downloads)).setText(String.valueOf(totalDownloads));
                ((TextView) analyticsView.findViewById(R.id.pro_widgets)).setText(String.valueOf(proCount));
                ((TextView) analyticsView.findViewById(R.id.free_widgets)).setText(String.valueOf(widgets.size() - proCount));
            }
        });
        viewModel.loadWidgets();
    }

    // ==================== ITEM VIEWS ====================

    private View createWidgetItemView(Widget w) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_admin_list, contentContainer, false);
        TextView title = view.findViewById(R.id.item_title);
        TextView subtitle = view.findViewById(R.id.item_subtitle);
        Button editBtn = view.findViewById(R.id.edit_button);
        Button deleteBtn = view.findViewById(R.id.delete_button);

        title.setText(w.getName());
        subtitle.setText((w.isPro() ? "PRO • " : "FREE • ") +
                "Downloads: " + w.getDownloadCount() + " • " +
                (w.isActive() ? "Active" : "Inactive"));

        editBtn.setOnClickListener(v -> showWidgetForm(w));
        deleteBtn.setOnClickListener(v -> confirmDelete("widget", w.getId(), () -> viewModel.deleteWidget(w.getId())));
        return view;
    }

    private View createCategoryItemView(Category c) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_admin_list, contentContainer, false);
        TextView title = view.findViewById(R.id.item_title);
        TextView subtitle = view.findViewById(R.id.item_subtitle);
        Button editBtn = view.findViewById(R.id.edit_button);
        Button deleteBtn = view.findViewById(R.id.delete_button);

        title.setText(c.getName());
        subtitle.setText("Order: " + c.getOrder() + " • " + (c.isActive() ? "Active" : "Inactive"));

        editBtn.setOnClickListener(v -> showCategoryForm(c));
        deleteBtn.setOnClickListener(v -> confirmDelete("category", c.getId(), () -> viewModel.deleteCategory(c.getId())));
        return view;
    }

    private View createThemeItemView(Theme t) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_admin_list, contentContainer, false);
        TextView title = view.findViewById(R.id.item_title);
        TextView subtitle = view.findViewById(R.id.item_subtitle);
        Button editBtn = view.findViewById(R.id.edit_button);
        Button deleteBtn = view.findViewById(R.id.delete_button);

        title.setText(t.getName());
        subtitle.setText((t.isPro() ? "PRO" : "FREE") + " • " +
                (t.isDefault() ? "Default" : "") + " • " +
                (t.isActive() ? "Active" : "Inactive"));

        editBtn.setOnClickListener(v -> showThemeForm(t));
        deleteBtn.setOnClickListener(v -> confirmDelete("theme", t.getId(), () -> viewModel.deleteTheme(t.getId())));
        return view;
    }

    private View createAnnouncementItemView(Announcement a) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_admin_list, contentContainer, false);
        TextView title = view.findViewById(R.id.item_title);
        TextView subtitle = view.findViewById(R.id.item_subtitle);
        Button editBtn = view.findViewById(R.id.edit_button);
        Button deleteBtn = view.findViewById(R.id.delete_button);

        title.setText(a.getTitle());
        subtitle.setText(a.getType() + " • Priority: " + a.getPriority() + " • " +
                (a.isActive() ? "Active" : "Inactive"));

        editBtn.setOnClickListener(v -> showAnnouncementForm(a));
        deleteBtn.setOnClickListener(v -> confirmDelete("announcement", a.getId(),
                () -> viewModel.deleteAnnouncement(a.getId())));
        return view;
    }

    private View createOfferItemView(Offer o) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_admin_list, contentContainer, false);
        TextView title = view.findViewById(R.id.item_title);
        TextView subtitle = view.findViewById(R.id.item_subtitle);
        Button editBtn = view.findViewById(R.id.edit_button);
        Button deleteBtn = view.findViewById(R.id.delete_button);

        title.setText(o.getTitle());
        subtitle.setText(o.getDiscountPercent() + "% OFF • Code: " + o.getCode() + " • " +
                (o.isActive() ? "Active" : "Inactive"));

        editBtn.setOnClickListener(v -> showOfferForm(o));
        deleteBtn.setOnClickListener(v -> confirmDelete("offer", o.getId(), () -> viewModel.deleteOffer(o.getId())));
        return view;
    }

    private View createFlagItemView(String key, boolean value, AppConfig config) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_feature_flag, contentContainer, false);
        TextView name = view.findViewById(R.id.flag_name);
        Switch toggle = view.findViewById(R.id.flag_toggle);
        name.setText(key);
        toggle.setChecked(value);
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Map<String, Boolean> flags = config.getFeatureFlags();
            if (flags == null) flags = new HashMap<>();
            flags.put(key, isChecked);
            config.setFeatureFlags(flags);
            viewModel.saveAppConfig(config);
        });
        return view;
    }

    private View createAppConfigView(AppConfig config) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.view_app_config, contentContainer, false);
        TextInputEditText latestVersion = view.findViewById(R.id.latest_version);
        TextInputEditText minVersion = view.findViewById(R.id.min_version);
        TextInputEditText forceUpdateMsg = view.findViewById(R.id.force_update_message);
        TextInputEditText maintenanceMsg = view.findViewById(R.id.maintenance_message);
        Switch forceUpdateToggle = view.findViewById(R.id.force_update_toggle);
        Switch maintenanceToggle = view.findViewById(R.id.maintenance_toggle);
        Button saveBtn = view.findViewById(R.id.save_config_btn);

        latestVersion.setText(String.valueOf(config.getLatestVersion()));
        minVersion.setText(String.valueOf(config.getMinSupportedVersion()));
        forceUpdateMsg.setText(config.getForceUpdateMessage());
        maintenanceMsg.setText(config.getMaintenanceMessage());
        forceUpdateToggle.setChecked(config.isForceUpdate());
        maintenanceToggle.setChecked(config.isMaintenanceMode());

        saveBtn.setOnClickListener(v -> {
            try {
                config.setLatestVersion(Integer.parseInt(latestVersion.getText().toString()));
                config.setMinSupportedVersion(Integer.parseInt(minVersion.getText().toString()));
            } catch (NumberFormatException ignored) {}
            config.setForceUpdate(forceUpdateToggle.isChecked());
            config.setMaintenanceMode(maintenanceToggle.isChecked());
            config.setForceUpdateMessage(forceUpdateMsg.getText().toString());
            config.setMaintenanceMessage(maintenanceMsg.getText().toString());
            viewModel.saveAppConfig(config);
            Toast.makeText(requireContext(), "Config saved", Toast.LENGTH_SHORT).show();
        });
        return view;
    }

    // ==================== FORMS ====================

    private void showAddForm() {
        switch (section) {
            case "Widgets": showWidgetForm(null); break;
            case "Categories": showCategoryForm(null); break;
            case "Themes": showThemeForm(null); break;
            case "Announcements": showAnnouncementForm(null); break;
            case "Offers": showOfferForm(null); break;
        }
    }

    private void showWidgetForm(@Nullable Widget existing) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.form_widget, formContainer, false);
        showForm("Widget", view, () -> {
            Widget w = existing != null ? existing : new Widget();
            w.setName(((TextInputEditText) view.findViewById(R.id.input_name)).getText().toString());
            w.setDescription(((TextInputEditText) view.findViewById(R.id.input_description)).getText().toString());
            w.setCategoryId(((TextInputEditText) view.findViewById(R.id.input_category_id)).getText().toString());
            w.setCategoryName(((TextInputEditText) view.findViewById(R.id.input_category_name)).getText().toString());
            w.setThumbnailUrl(((TextInputEditText) view.findViewById(R.id.input_thumbnail_url)).getText().toString());
            w.setPreviewUrl(((TextInputEditText) view.findViewById(R.id.input_preview_url)).getText().toString());
            w.setConfigJson(((TextInputEditText) view.findViewById(R.id.input_config_json)).getText().toString());
            w.setPro(((Switch) view.findViewById(R.id.input_is_pro)).isChecked());
            w.setFeatured(((Switch) view.findViewById(R.id.input_is_featured)).isChecked());
            w.setTrending(((Switch) view.findViewById(R.id.input_is_trending)).isChecked());
            w.setActive(true);
            viewModel.saveWidget(w);
        }, existing);
    }

    private void showCategoryForm(@Nullable Category existing) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.form_category, formContainer, false);
        showForm("Category", view, () -> {
            Category c = existing != null ? existing : new Category();
            c.setName(((TextInputEditText) view.findViewById(R.id.input_name)).getText().toString());
            c.setIcon(((TextInputEditText) view.findViewById(R.id.input_icon)).getText().toString());
            c.setColor(((TextInputEditText) view.findViewById(R.id.input_color)).getText().toString());
            try {
                c.setOrder(Integer.parseInt(((TextInputEditText) view.findViewById(R.id.input_order)).getText().toString()));
            } catch (NumberFormatException ignored) {}
            c.setActive(((Switch) view.findViewById(R.id.input_active)).isChecked());
            viewModel.saveCategory(c);
        }, existing);
    }

    private void showThemeForm(@Nullable Theme existing) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.form_theme, formContainer, false);
        showForm("Theme", view, () -> {
            Theme t = existing != null ? existing : new Theme();
            t.setName(((TextInputEditText) view.findViewById(R.id.input_name)).getText().toString());
            t.setDescription(((TextInputEditText) view.findViewById(R.id.input_description)).getText().toString());
            t.setThumbnailUrl(((TextInputEditText) view.findViewById(R.id.input_thumbnail_url)).getText().toString());
            t.setPro(((Switch) view.findViewById(R.id.input_is_pro)).isChecked());
            t.setDefault(((Switch) view.findViewById(R.id.input_is_default)).isChecked());
            t.setActive(true);
            viewModel.saveTheme(t);
        }, existing);
    }

    private void showAnnouncementForm(@Nullable Announcement existing) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.form_announcement, formContainer, false);
        showForm("Announcement", view, () -> {
            Announcement a = existing != null ? existing : new Announcement();
            a.setTitle(((TextInputEditText) view.findViewById(R.id.input_title)).getText().toString());
            a.setMessage(((TextInputEditText) view.findViewById(R.id.input_message)).getText().toString());
            a.setType(((TextInputEditText) view.findViewById(R.id.input_type)).getText().toString());
            a.setActionLabel(((TextInputEditText) view.findViewById(R.id.input_action_label)).getText().toString());
            a.setActionUrl(((TextInputEditText) view.findViewById(R.id.input_action_url)).getText().toString());
            a.setImageUrl(((TextInputEditText) view.findViewById(R.id.input_image_url)).getText().toString());
            try {
                a.setPriority(Integer.parseInt(((TextInputEditText) view.findViewById(R.id.input_priority)).getText().toString()));
            } catch (NumberFormatException ignored) {}
            a.setStartAt(System.currentTimeMillis());
            a.setEndAt(System.currentTimeMillis() + 86400000L * 30);
            a.setActive(((Switch) view.findViewById(R.id.input_active)).isChecked());
            viewModel.saveAnnouncement(a);
        }, existing);
    }

    private void showOfferForm(@Nullable Offer existing) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.form_offer, formContainer, false);
        showForm("Offer", view, () -> {
            Offer o = existing != null ? existing : new Offer();
            o.setTitle(((TextInputEditText) view.findViewById(R.id.input_title)).getText().toString());
            o.setDescription(((TextInputEditText) view.findViewById(R.id.input_description)).getText().toString());
            o.setCode(((TextInputEditText) view.findViewById(R.id.input_code)).getText().toString());
            try {
                o.setDiscountPercent(Integer.parseInt(((TextInputEditText) view.findViewById(R.id.input_discount)).getText().toString()));
            } catch (NumberFormatException ignored) {}
            o.setImageUrl(((TextInputEditText) view.findViewById(R.id.input_image_url)).getText().toString());
            o.setStartAt(System.currentTimeMillis());
            o.setEndAt(System.currentTimeMillis() + 86400000L * 30);
            o.setActive(((Switch) view.findViewById(R.id.input_active)).isChecked());
            viewModel.saveOffer(o);
        }, existing);
    }

    private void showForm(String title, View formContent, Runnable saveAction, @Nullable Object existing) {
        formView.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
        formContainer.removeAllViews();
        formContainer.addView(formContent);

        if (existing != null) {
            populateForm(existing, formContent);
        }

        formView.findViewById(R.id.form_save_btn).setOnClickListener(v -> {
            saveAction.run();
            formView.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        });
        formView.findViewById(R.id.form_cancel_btn).setOnClickListener(v -> {
            formView.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        });
    }

    private void populateForm(Object item, View formView) {
        if (item instanceof Widget) {
            Widget w = (Widget) item;
            setText(formView, R.id.input_name, w.getName());
            setText(formView, R.id.input_description, w.getDescription());
            setText(formView, R.id.input_category_id, w.getCategoryId());
            setText(formView, R.id.input_category_name, w.getCategoryName());
            setText(formView, R.id.input_thumbnail_url, w.getThumbnailUrl());
            setText(formView, R.id.input_preview_url, w.getPreviewUrl());
            setText(formView, R.id.input_config_json, w.getConfigJson());
            setChecked(formView, R.id.input_is_pro, w.isPro());
            setChecked(formView, R.id.input_is_featured, w.isFeatured());
            setChecked(formView, R.id.input_is_trending, w.isTrending());
        } else if (item instanceof Category) {
            Category c = (Category) item;
            setText(formView, R.id.input_name, c.getName());
            setText(formView, R.id.input_icon, c.getIcon());
            setText(formView, R.id.input_color, c.getColor());
            setText(formView, R.id.input_order, String.valueOf(c.getOrder()));
            setChecked(formView, R.id.input_active, c.isActive());
        } else if (item instanceof Theme) {
            Theme t = (Theme) item;
            setText(formView, R.id.input_name, t.getName());
            setText(formView, R.id.input_description, t.getDescription());
            setText(formView, R.id.input_thumbnail_url, t.getThumbnailUrl());
            setChecked(formView, R.id.input_is_pro, t.isPro());
            setChecked(formView, R.id.input_is_default, t.isDefault());
        } else if (item instanceof Announcement) {
            Announcement a = (Announcement) item;
            setText(formView, R.id.input_title, a.getTitle());
            setText(formView, R.id.input_message, a.getMessage());
            setText(formView, R.id.input_type, a.getType());
            setText(formView, R.id.input_action_label, a.getActionLabel());
            setText(formView, R.id.input_action_url, a.getActionUrl());
            setText(formView, R.id.input_image_url, a.getImageUrl());
            setText(formView, R.id.input_priority, String.valueOf(a.getPriority()));
            setChecked(formView, R.id.input_active, a.isActive());
        } else if (item instanceof Offer) {
            Offer o = (Offer) item;
            setText(formView, R.id.input_title, o.getTitle());
            setText(formView, R.id.input_description, o.getDescription());
            setText(formView, R.id.input_code, o.getCode());
            setText(formView, R.id.input_discount, String.valueOf(o.getDiscountPercent()));
            setText(formView, R.id.input_image_url, o.getImageUrl());
            setChecked(formView, R.id.input_active, o.isActive());
        }
    }

    private void setText(View parent, int id, String text) {
        View v = parent.findViewById(id);
        if (v instanceof TextInputEditText) {
            ((TextInputEditText) v).setText(text != null ? text : "");
        }
    }

    private void setChecked(View parent, int id, boolean checked) {
        View v = parent.findViewById(id);
        if (v instanceof Switch) {
            ((Switch) v).setChecked(checked);
        }
    }

    private void addEmptyView(String message) {
        TextView tv = new TextView(requireContext());
        tv.setText(message);
        tv.setTextColor(getResources().getColor(R.color.text_secondary));
        tv.setTextSize(14);
        tv.setPadding(32, 32, 32, 32);
        contentContainer.addView(tv);
    }

    private void confirmDelete(String type, String id, Runnable deleteAction) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete " + type)
                .setMessage("Are you sure you want to delete this " + type + "?")
                .setPositiveButton("Delete", (d, w) -> deleteAction.run())
                .setNegativeButton("Cancel", null)
                .show();
    }
}
