package com.desire.widget.ui.admin;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.WebSettings;
import android.webkit.WebView;

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
import com.desire.widget.widget.WidgetSchema;
import androidx.core.content.ContextCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminDetailFragment extends Fragment {
    private static final String ARG_SECTION = "section";
    private static final int PICK_THUMBNAIL = 1001;
    private static final int PICK_PREVIEW = 1002;

    private String section;
    private AdminViewModel viewModel;
    private LinearLayout contentContainer;
    private LinearLayout formContainer;
    private View listView;
    private View formView;

    private String pendingThumbnailUrl;
    private String pendingPreviewUrl;
    private Uri thumbnailUri;
    private Uri previewUri;
    private boolean isThumbnailUploading;
    private boolean isPreviewUploading;

    private interface FormSaveAction {
        boolean save();
    }

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
        MaterialToolbar toolbar = view.findViewById(R.id.admin_detail_toolbar);

        title.setText(getSectionTitle());
        toolbar.setNavigationOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        addButton.setOnClickListener(v -> showAddForm());

        viewModel.getThumbnailUploadResult().observe(getViewLifecycleOwner(), url -> {
            if (url != null) {
                pendingThumbnailUrl = url;
                isThumbnailUploading = false;
                updateUploadLabel(R.id.txt_thumbnail_name, "Uploaded!", R.color.status_free);
            }
        });

        viewModel.getPreviewUploadResult().observe(getViewLifecycleOwner(), url -> {
            if (url != null) {
                pendingPreviewUrl = url;
                isPreviewUploading = false;
                updateUploadLabel(R.id.txt_preview_name, "Uploaded!", R.color.status_free);
            }
        });

        viewModel.getUploadError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                if (error.startsWith("Thumbnail")) {
                    isThumbnailUploading = false;
                    updateUploadLabel(R.id.txt_thumbnail_name, "Upload Failed", R.color.status_pro);
                } else if (error.startsWith("Preview")) {
                    isPreviewUploading = false;
                    updateUploadLabel(R.id.txt_preview_name, "Upload Failed", R.color.status_pro);
                }
            }
        });

        viewModel.getUploadProgress().observe(getViewLifecycleOwner(), progress -> {
            if (progress != null) {
                Toast.makeText(requireContext(), progress, Toast.LENGTH_SHORT).show();
            }
        });

        loadSectionData();
    }

    private void updateUploadLabel(int textViewId, String text, int colorRes) {
        View fv = formContainer.getChildAt(0);
        if (fv != null) {
            TextView tv = fv.findViewById(textViewId);
            if (tv != null) {
                tv.setText(text);
                tv.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
            }
        }
    }

    private String getSectionTitle() {
        switch (section) {
            case "Widgets": return "Widgets";
            case "Themes": return "Themes";
            case "Categories": return "Categories";
            case "Announcements": return "Announcements";
            case "Offers": return "Offers";
            case "FeatureFlags": return "Feature Flags";
            case "Analytics": return "Analytics";
            case "AppConfig": return "App Config";
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
            addEmptyView("No widgets yet. Tap + Add New to create your first widget!");
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

    private View createWidgetItemView(Widget w) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_admin_list, contentContainer, false);
        TextView title = view.findViewById(R.id.item_title);
        TextView subtitle = view.findViewById(R.id.item_subtitle);
        Button editBtn = view.findViewById(R.id.edit_button);
        Button deleteBtn = view.findViewById(R.id.delete_button);

        title.setText(w.getName());
        subtitle.setText(WidgetSchema.normalizeSize(w.getWidgetSize()) + " \u2022 " + (w.isPro() ? "PRO" : "FREE") + " \u2022 Downloads: " + w.getDownloadCount()
                + " \u2022 " + (w.isActive() ? "Active" : "Inactive"));

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
        subtitle.setText("Order: " + c.getOrder() + " \u2022 " + (c.isActive() ? "Active" : "Inactive"));

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
        subtitle.setText((t.isPro() ? "PRO" : "FREE") + " \u2022 "
                + (t.isDefault() ? "Default " : "") + (t.isActive() ? "Active" : "Inactive"));

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
        subtitle.setText(a.getType() + " \u2022 Priority: " + a.getPriority() + " \u2022 "
                + (a.isActive() ? "Active" : "Inactive"));

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
        subtitle.setText(o.getDiscountPercent() + "% OFF \u2022 Code: " + o.getCode() + " \u2022 "
                + (o.isActive() ? "Active" : "Inactive"));

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
        EditText latestVersion = view.findViewById(R.id.latest_version);
        EditText minVersion = view.findViewById(R.id.min_version);
        EditText forceUpdateMsg = view.findViewById(R.id.force_update_message);
        EditText maintenanceMsg = view.findViewById(R.id.maintenance_message);
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
        viewModel.clearUploadResults();
        pendingThumbnailUrl = existing != null ? existing.getThumbnailUrl() : null;
        pendingPreviewUrl = existing != null ? existing.getPreviewUrl() : null;
        thumbnailUri = null;
        previewUri = null;
        isThumbnailUploading = false;
        isPreviewUploading = false;

        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.form_widget, formContainer, false);
        setupWidgetDesignControls(view);
        showForm("Widget", view, () -> {
            Widget w = existing != null ? existing : new Widget();

            String name = ((EditText) view.findViewById(R.id.input_name)).getText().toString().trim();
            String html = ((EditText) view.findViewById(R.id.input_widget_html)).getText().toString().trim();
            String htmlError = WidgetSchema.htmlValidationError(html);
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a widget name", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (htmlError != null) {
                showHtmlError(view, htmlError);
                Toast.makeText(requireContext(), htmlError, Toast.LENGTH_LONG).show();
                return false;
            }
            if (isThumbnailUploading || isPreviewUploading) {
                Toast.makeText(requireContext(), "Please wait for uploads to finish", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (thumbnailUri != null && pendingThumbnailUrl == null) {
                Toast.makeText(requireContext(), "Thumbnail upload has not completed", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (previewUri != null && pendingPreviewUrl == null) {
                Toast.makeText(requireContext(), "Preview upload has not completed", Toast.LENGTH_SHORT).show();
                return false;
            }
            w.setName(name);
            w.setDescription(((EditText) view.findViewById(R.id.input_description)).getText().toString().trim());
            w.setCategoryName(((EditText) view.findViewById(R.id.input_category_name)).getText().toString().trim());
            w.setWidgetSize(selectedSpinnerValue(view, R.id.input_widget_size, WidgetSchema.SIZE_2X2));
            w.setPreviewStyle(selectedSpinnerValue(view, R.id.input_preview_style, WidgetSchema.STYLE_ICON));
            if (pendingThumbnailUrl != null) w.setThumbnailUrl(pendingThumbnailUrl);
            if (pendingPreviewUrl != null) w.setPreviewUrl(pendingPreviewUrl);
            w.setConfigJson(((EditText) view.findViewById(R.id.input_config_json)).getText().toString());
            w.setHtmlContent(html);
            w.setPro(((Switch) view.findViewById(R.id.input_is_pro)).isChecked());
            w.setFeatured(((Switch) view.findViewById(R.id.input_is_featured)).isChecked());
            w.setTrending(((Switch) view.findViewById(R.id.input_is_trending)).isChecked());
            w.setActive(true);
            if (w.getDownloadCount() == 0) w.setDownloadCount(0);
            if (w.getFavoriteCount() == 0) w.setFavoriteCount(0);
            if (w.getVersion() == 0) w.setVersion(1);

            viewModel.saveWidget(w);
            Toast.makeText(requireContext(), "Widget saved!", Toast.LENGTH_SHORT).show();
            return true;
        }, existing);

        view.findViewById(R.id.btn_preview_html).setOnClickListener(v -> renderAdminHtmlPreview(view));

        view.findViewById(R.id.btn_upload_thumbnail).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_THUMBNAIL);
        });

        view.findViewById(R.id.btn_upload_preview).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_PREVIEW);
        });
    }

    private void showCategoryForm(@Nullable Category existing) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.form_category, formContainer, false);
        showForm("Category", view, () -> {
            Category c = existing != null ? existing : new Category();
            String name = ((EditText) view.findViewById(R.id.input_name)).getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a category name", Toast.LENGTH_SHORT).show();
                return false;
            }
            c.setName(name);
            c.setIcon(((EditText) view.findViewById(R.id.input_icon)).getText().toString());
            c.setColor(((EditText) view.findViewById(R.id.input_color)).getText().toString());
            try {
                c.setOrder(Integer.parseInt(((EditText) view.findViewById(R.id.input_order)).getText().toString()));
            } catch (NumberFormatException ignored) {}
            c.setActive(((Switch) view.findViewById(R.id.input_active)).isChecked());
            viewModel.saveCategory(c);
            return true;
        }, existing);
    }

    private void showThemeForm(@Nullable Theme existing) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.form_theme, formContainer, false);
        showForm("Theme", view, () -> {
            Theme t = existing != null ? existing : new Theme();
            String name = ((EditText) view.findViewById(R.id.input_name)).getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a theme name", Toast.LENGTH_SHORT).show();
                return false;
            }
            t.setName(name);
            t.setDescription(((EditText) view.findViewById(R.id.input_description)).getText().toString());
            t.setThumbnailUrl(((EditText) view.findViewById(R.id.input_thumbnail_url)).getText().toString());
            t.setPro(((Switch) view.findViewById(R.id.input_is_pro)).isChecked());
            t.setDefault(((Switch) view.findViewById(R.id.input_is_default)).isChecked());
            t.setActive(true);
            viewModel.saveTheme(t);
            return true;
        }, existing);
    }

    private void showAnnouncementForm(@Nullable Announcement existing) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.form_announcement, formContainer, false);
        showForm("Announcement", view, () -> {
            Announcement a = existing != null ? existing : new Announcement();
            String title = ((EditText) view.findViewById(R.id.input_title)).getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter an announcement title", Toast.LENGTH_SHORT).show();
                return false;
            }
            a.setTitle(title);
            a.setMessage(((EditText) view.findViewById(R.id.input_message)).getText().toString());
            a.setType(((EditText) view.findViewById(R.id.input_type)).getText().toString());
            a.setActionLabel(((EditText) view.findViewById(R.id.input_action_label)).getText().toString());
            a.setActionUrl(((EditText) view.findViewById(R.id.input_action_url)).getText().toString());
            a.setImageUrl(((EditText) view.findViewById(R.id.input_image_url)).getText().toString());
            try {
                a.setPriority(Integer.parseInt(((EditText) view.findViewById(R.id.input_priority)).getText().toString()));
            } catch (NumberFormatException ignored) {}
            a.setStartAt(System.currentTimeMillis());
            a.setEndAt(System.currentTimeMillis() + 86400000L * 30);
            a.setActive(((Switch) view.findViewById(R.id.input_active)).isChecked());
            viewModel.saveAnnouncement(a);
            return true;
        }, existing);
    }

    private void showOfferForm(@Nullable Offer existing) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.form_offer, formContainer, false);
        showForm("Offer", view, () -> {
            Offer o = existing != null ? existing : new Offer();
            String title = ((EditText) view.findViewById(R.id.input_title)).getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter an offer title", Toast.LENGTH_SHORT).show();
                return false;
            }
            o.setTitle(title);
            o.setDescription(((EditText) view.findViewById(R.id.input_description)).getText().toString());
            o.setCode(((EditText) view.findViewById(R.id.input_code)).getText().toString());
            try {
                o.setDiscountPercent(Integer.parseInt(((EditText) view.findViewById(R.id.input_discount)).getText().toString()));
            } catch (NumberFormatException ignored) {}
            o.setImageUrl(((EditText) view.findViewById(R.id.input_image_url)).getText().toString());
            o.setStartAt(System.currentTimeMillis());
            o.setEndAt(System.currentTimeMillis() + 86400000L * 30);
            o.setActive(((Switch) view.findViewById(R.id.input_active)).isChecked());
            viewModel.saveOffer(o);
            return true;
        }, existing);
    }

    private void showForm(String title, View formContent, FormSaveAction saveAction, @Nullable Object existing) {
        formView.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
        formContainer.removeAllViews();
        formContainer.addView(formContent);
        ((Button) formView.findViewById(R.id.form_save_btn)).setText("Save " + title);

        if (existing != null) {
            populateForm(existing, formContent);
        }

        formView.findViewById(R.id.form_save_btn).setOnClickListener(v -> {
            if (saveAction.save()) {
                formView.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);
            }
        });
        formView.findViewById(R.id.form_cancel_btn).setOnClickListener(v -> {
            formView.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            if (requestCode == PICK_THUMBNAIL) {
                thumbnailUri = data.getData();
                pendingThumbnailUrl = null;
                isThumbnailUploading = true;
                viewModel.uploadThumbnail(thumbnailUri);
                updateUploadLabel(R.id.txt_thumbnail_name, "Uploading...", R.color.text_tertiary);
            } else if (requestCode == PICK_PREVIEW) {
                previewUri = data.getData();
                pendingPreviewUrl = null;
                isPreviewUploading = true;
                viewModel.uploadPreview(previewUri);
                updateUploadLabel(R.id.txt_preview_name, "Uploading...", R.color.text_tertiary);
            }
        }
    }

    private void populateForm(Object item, View formView) {
        if (item instanceof Widget) {
            Widget w = (Widget) item;
            setText(formView, R.id.input_name, w.getName());
            setText(formView, R.id.input_description, w.getDescription());
            setText(formView, R.id.input_category_name, w.getCategoryName());
            setSpinnerValue(formView, R.id.input_widget_size, WidgetSchema.normalizeSize(w.getWidgetSize()));
            setSpinnerValue(formView, R.id.input_preview_style, WidgetSchema.normalizeStyle(w.getPreviewStyle()));
            setText(formView, R.id.input_config_json, w.getConfigJson());
            setText(formView, R.id.input_widget_html, w.getHtmlContent());
            setChecked(formView, R.id.input_is_pro, w.isPro());
            setChecked(formView, R.id.input_is_featured, w.isFeatured());
            setChecked(formView, R.id.input_is_trending, w.isTrending());
            pendingThumbnailUrl = w.getThumbnailUrl();
            pendingPreviewUrl = w.getPreviewUrl();
            if (pendingThumbnailUrl != null) updateUploadLabel(R.id.txt_thumbnail_name, "Has image", R.color.status_free);
            if (pendingPreviewUrl != null) updateUploadLabel(R.id.txt_preview_name, "Has image", R.color.status_free);
            renderAdminHtmlPreview(formView);
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
        if (v instanceof EditText) {
            ((EditText) v).setText(text != null ? text : "");
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
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        tv.setTextSize(14);
        tv.setPadding(32, 32, 32, 32);
        contentContainer.addView(tv);
    }

    private void setupWidgetDesignControls(View view) {
        setupSpinner(view, R.id.input_widget_size, WidgetSchema.SIZE_OPTIONS);
        setupSpinner(view, R.id.input_preview_style, WidgetSchema.STYLE_OPTIONS);
        setSpinnerValue(view, R.id.input_widget_size, WidgetSchema.SIZE_2X2);
        setSpinnerValue(view, R.id.input_preview_style, WidgetSchema.STYLE_ICON);
        configureAdminWebView(view.findViewById(R.id.admin_html_preview));
    }

    private void setupSpinner(View parent, int id, String[] values) {
        Spinner spinner = parent.findViewById(id);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.item_admin_spinner, values);
        adapter.setDropDownViewResource(R.layout.item_admin_spinner);
        spinner.setAdapter(adapter);
    }

    private void setSpinnerValue(View parent, int id, String value) {
        Spinner spinner = parent.findViewById(id);
        if (spinner == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (String.valueOf(spinner.getItemAtPosition(i)).equals(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private String selectedSpinnerValue(View parent, int id, String fallback) {
        Spinner spinner = parent.findViewById(id);
        if (spinner == null || spinner.getSelectedItem() == null) return fallback;
        return String.valueOf(spinner.getSelectedItem());
    }

    private void renderAdminHtmlPreview(View parent) {
        EditText htmlInput = parent.findViewById(R.id.input_widget_html);
        WebView preview = parent.findViewById(R.id.admin_html_preview);
        String html = htmlInput != null ? htmlInput.getText().toString().trim() : "";
        String error = WidgetSchema.htmlValidationError(html);
        if (error != null) {
            showHtmlError(parent, error);
            if (preview != null) {
                preview.loadDataWithBaseURL(null, WidgetSchema.errorHtml(error), "text/html", "UTF-8", null);
            }
            return;
        }
        showHtmlError(parent, null);
        if (preview != null) {
            preview.loadDataWithBaseURL(null, WidgetSchema.documentForWebView(html), "text/html", "UTF-8", null);
        }
    }

    private void showHtmlError(View parent, @Nullable String error) {
        TextView errorView = parent.findViewById(R.id.txt_html_error);
        if (errorView == null) return;
        errorView.setText(error != null ? error : "");
        errorView.setVisibility(error != null ? View.VISIBLE : View.GONE);
    }

    private void configureAdminWebView(WebView webView) {
        if (webView == null) return;
        webView.setBackgroundColor(android.graphics.Color.BLACK);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setTextZoom(100);
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
