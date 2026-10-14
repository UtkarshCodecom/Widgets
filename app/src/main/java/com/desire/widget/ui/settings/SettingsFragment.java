package com.desire.widget.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.desire.widget.R;
import com.desire.widget.service.MediaListenerHelper;
import com.desire.widget.ui.admin.AdminFragment;
import com.desire.widget.util.LocationPermissionHelper;
import com.desire.widget.util.PreferenceManager;
import com.desire.widget.worker.WeatherWorker;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;

/**
 * Grouped-card settings screen: Widgets, Permissions, Performance, Support, About, Community.
 * Every row shows a real, live-checked status where one exists (permission granted/denied, cache
 * size) instead of being purely decorative.
 */
public class SettingsFragment extends Fragment {
    private SettingsViewModel viewModel;
    private PreferenceManager prefs;
    private TextView developerTapHint;
    private View developerSection;
    private View premiumBanner;
    private com.desire.widget.ui.adapters.ThemeAdapter themeAdapter;
    private int tapCount = 0;

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                refreshPermissionSubtitles();
                if (granted) {
                    androidx.work.WorkManager.getInstance(requireContext()).enqueue(
                            new androidx.work.OneTimeWorkRequest.Builder(WeatherWorker.class).build());
                    Snackbar.make(requireView(), "Location enabled — weather will update shortly", Snackbar.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> calendarPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                refreshPermissionSubtitles();
                if (granted) {
                    com.desire.widget.engine.runtime.EngineRenderer.render(requireContext(),
                            com.desire.widget.engine.runtime.EngineWidgetStore.allPlacedIds(requireContext()));
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);
        prefs = PreferenceManager.getInstance(requireContext());

        premiumBanner = view.findViewById(R.id.premium_banner);
        developerTapHint = view.findViewById(R.id.developer_tap_hint);
        developerSection = view.findViewById(R.id.developer_section);

        setupThemes(view);
        bindRows(view);
        refreshPermissionSubtitles();

        setupThemesObserver();

        View versionRow = view.findViewById(R.id.version_row);
        versionRow.setOnClickListener(v -> {
            viewModel.onVersionTap();
            tapCount++;
            int remaining = 7 - tapCount;
            if (remaining > 0 && remaining <= 5) {
                developerTapHint.setVisibility(View.VISIBLE);
                developerTapHint.setText(remaining + " taps remaining");
            }
            if (tapCount >= 7) {
                developerTapHint.setVisibility(View.GONE);
                developerSection.setVisibility(View.VISIBLE);
                Snackbar.make(view, "Developer mode unlocked!", Snackbar.LENGTH_SHORT).show();
            }
        });

        if (prefs.isDeveloperModeUnlocked()) {
            developerSection.setVisibility(View.VISIBLE);
        }
        if (prefs.isPremium()) {
            premiumBanner.setVisibility(View.GONE);
        }

        setupClickListeners(view);
        animateIn(view.findViewById(R.id.settings_content));
    }

    /** App colour-theme picker at the top of Settings. Selecting one persists it and recreates the
     *  activity so the whole app re-themes immediately (see {@code ThemeManager.apply}). */
    private void setupThemes(View view) {
        androidx.recyclerview.widget.RecyclerView recycler = view.findViewById(R.id.theme_recycler);
        if (recycler == null) return;
        themeAdapter = new com.desire.widget.ui.adapters.ThemeAdapter();
        themeAdapter.setOnThemeClickListener(theme -> requireActivity().recreate());
        recycler.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(
                requireContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
        recycler.setAdapter(themeAdapter);
    }

    private void setupThemesObserver() {
        if (themeAdapter == null) return;
        viewModel.getThemes().observe(getViewLifecycleOwner(),
                themes -> themeAdapter.setThemes(themes));
    }

    /** Sets icon glyph + title + optional subtitle for every row in one pass. */
    private void bindRows(View view) {
        bindRow(view, R.id.check_updates_row, "↻", "#5C6BC0", "Check for Updates", null);
        bindRow(view, R.id.whats_new_row, "✦", "#7E57C2", "What's New", null);
        bindRow(view, R.id.restore_purchases_row, "⟲", "#26A69A", "Restore Purchases", null);
        bindRow(view, R.id.temp_unit_row, "🌡", "#FF7043", "Temperature Unit", tempUnitLabel());

        bindRow(view, R.id.location_permission_row, "📍", "#42A5F5", "Location Access", "Checking…");
        bindRow(view, R.id.media_permission_row, "🎵", "#EC407A", "Now-Playing Access", "Checking…");
        bindRow(view, R.id.calendar_permission_row, "📅", "#AB47BC", "Calendar Access", "Checking…");
        bindRow(view, R.id.usage_access_row, "⏱", "#26C6DA", "Usage Access", "Checking…");
        bindRow(view, R.id.notification_permission_row, "🔔", "#FFA726", "Notifications", null);
        bindRow(view, R.id.battery_optimization_row, "🔋", "#66BB6A", "Battery Optimization", "Allow background updates");

        bindRow(view, R.id.clear_cache_row, "🗑", "#8D6E63", "Clear Cache", "Calculating…");
        bindRow(view, R.id.background_running_row, "⚙", "#78909C", "Background Running Guide", null);

        bindRow(view, R.id.faq_row, "❓", "#5C6BC0", "FAQ", null);
        bindRow(view, R.id.privacy_policy_row, "🔒", "#546E7A", "Privacy Policy", null);
        bindRow(view, R.id.report_bug_row, "🐞", "#EF5350", "Report a Bug", null);
        bindRow(view, R.id.support_row, "✉", "#42A5F5", "Contact Support", null);

        bindRow(view, R.id.about_row, "ℹ", "#7E57C2", "About", null);
        bindRow(view, R.id.social_row, "🔗", "#EC407A", "Social Links", null);
        bindRow(view, R.id.more_apps_row, "▦", "#26A69A", "More Apps", null);

        bindRow(view, R.id.rate_us_row, "★", "@color/yellow_accent", "Rate Us", null);
        bindRow(view, R.id.share_app_row, "↗", "#66BB6A", "Share App", null);

        bindRow(view, R.id.version_row, "⌘", "#5C6BC0", "App Version", viewModel.getVersionName());
        bindRow(view, R.id.admin_dashboard_row, "🛠", "#8D6E63", "Admin Dashboard", null);

        updateCacheSize(view);
    }

    private void bindRow(View parent, int rowId, String glyph, String colorHexOrRes, String title, @Nullable String subtitle) {
        View row = parent.findViewById(rowId);
        if (row == null) return;
        TextView icon = row.findViewById(R.id.row_icon);
        TextView titleView = row.findViewById(R.id.row_title);
        TextView subtitleView = row.findViewById(R.id.row_subtitle);
        if (icon != null) {
            icon.setText(glyph);
            try {
                int color = colorHexOrRes.startsWith("@color/")
                        ? androidx.core.content.ContextCompat.getColor(requireContext(), R.color.yellow_accent)
                        : android.graphics.Color.parseColor(colorHexOrRes);
                icon.getBackground().mutate().setTint(color);
            } catch (Exception ignored) {}
        }
        if (titleView != null) titleView.setText(title);
        if (subtitleView != null) {
            if (subtitle != null && !subtitle.isEmpty()) {
                subtitleView.setText(subtitle);
                subtitleView.setVisibility(View.VISIBLE);
            } else {
                subtitleView.setVisibility(View.GONE);
            }
        }
    }

    private void setRowSubtitle(View parent, int rowId, String subtitle) {
        View row = parent.findViewById(rowId);
        if (row == null) return;
        TextView subtitleView = row.findViewById(R.id.row_subtitle);
        if (subtitleView == null) return;
        subtitleView.setText(subtitle);
        subtitleView.setVisibility(View.VISIBLE);
    }

    private void refreshPermissionSubtitles() {
        View view = getView();
        if (view == null) return;
        boolean locGranted = LocationPermissionHelper.hasPermission(requireContext());
        setRowSubtitle(view, R.id.location_permission_row,
                locGranted ? "Granted — weather uses your real location" : "Tap to enable real weather");

        boolean mediaGranted = MediaListenerHelper.isEnabled(requireContext());
        setRowSubtitle(view, R.id.media_permission_row,
                mediaGranted ? "Granted — music widgets show live playback" : "Tap to enable music widget playback");

        boolean notifGranted = NotificationManagerCompat.from(requireContext()).areNotificationsEnabled();
        setRowSubtitle(view, R.id.notification_permission_row,
                notifGranted ? "Enabled" : "Tap to enable");

        boolean calGranted = androidx.core.content.ContextCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.READ_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        setRowSubtitle(view, R.id.calendar_permission_row,
                calGranted ? "Granted — agenda widgets show your events" : "Tap to show events on agenda widgets");

        boolean usageGranted = com.desire.widget.engine.data.ScreenTimeStore.hasAccess(requireContext());
        setRowSubtitle(view, R.id.usage_access_row,
                usageGranted ? "Granted — screen-time widgets are live" : "Tap to enable screen-time widgets");
    }

    private String tempUnitLabel() {
        return prefs.getBoolean(com.desire.widget.engine.components.WeatherRenderer.PREF_FAHRENHEIT, false)
                ? "Fahrenheit °F" : "Celsius °C";
    }

    private void updateCacheSize(View view) {
        try {
            long bytes = dirSize(requireContext().getCacheDir());
            String label = bytes > 1_000_000
                    ? String.format(java.util.Locale.US, "%.1f MB cached", bytes / 1_000_000f)
                    : String.format(java.util.Locale.US, "%d KB cached", bytes / 1000);
            setRowSubtitle(view, R.id.clear_cache_row, label);
        } catch (Exception ignored) {}
    }

    private long dirSize(File dir) {
        if (dir == null || !dir.exists()) return 0;
        long total = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        for (File f : files) total += f.isDirectory() ? dirSize(f) : f.length();
        return total;
    }

    /** Simple staggered fade + rise-in for a premium feel without any external animation library. */
    private void animateIn(View content) {
        if (!(content instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) content;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(24f);
            child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(Math.min(i * 18L, 260L))
                    .setDuration(260)
                    .start();
        }
    }

    private void setupClickListeners(View view) {
        view.findViewById(R.id.premium_banner).setOnClickListener(v ->
                Snackbar.make(view, "Premium features coming soon", Snackbar.LENGTH_SHORT).show());

        view.findViewById(R.id.check_updates_row).setOnClickListener(v -> {
            viewModel.checkUpdates();
            viewModel.isUpdateAvailable().observe(getViewLifecycleOwner(), available -> {
                viewModel.getUpdateMessage().observe(getViewLifecycleOwner(), message -> {
                    if (available) {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Update Available")
                                .setMessage(message)
                                .setPositiveButton("Update", (d, w) -> {
                                    try {
                                        startActivity(new Intent(Intent.ACTION_VIEW,
                                                Uri.parse("market://details?id=" + requireContext().getPackageName())));
                                    } catch (Exception e) {
                                        startActivity(new Intent(Intent.ACTION_VIEW,
                                                Uri.parse("https://play.google.com/store/apps/details?id="
                                                        + requireContext().getPackageName())));
                                    }
                                })
                                .setNegativeButton("Later", null)
                                .show();
                    } else {
                        Snackbar.make(view, message != null ? message : "No updates available",
                                Snackbar.LENGTH_SHORT).show();
                    }
                });
            });
        });

        view.findViewById(R.id.whats_new_row).setOnClickListener(v -> showWhatsNewDialog());

        view.findViewById(R.id.restore_purchases_row).setOnClickListener(v -> {
            viewModel.restorePurchases();
            viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
                if (!loading) {
                    Snackbar.make(view, "Purchases restored", Snackbar.LENGTH_SHORT).show();
                }
            });
        });

        view.findViewById(R.id.location_permission_row).setOnClickListener(v -> {
            if (LocationPermissionHelper.hasPermission(requireContext())) {
                Snackbar.make(view, "Location already enabled", Snackbar.LENGTH_SHORT).show();
            } else {
                locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        });

        view.findViewById(R.id.media_permission_row).setOnClickListener(v -> {
            MediaListenerHelper.openSettings(requireContext());
        });

        view.findViewById(R.id.calendar_permission_row).setOnClickListener(v -> {
            if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.READ_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(view, "Calendar already enabled", Snackbar.LENGTH_SHORT).show();
            } else {
                calendarPermissionLauncher.launch(android.Manifest.permission.READ_CALENDAR);
            }
        });

        view.findViewById(R.id.usage_access_row).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                Snackbar.make(view, "Find \"Widgets\" and turn on usage access", Snackbar.LENGTH_LONG).show();
            } catch (Exception e) {
                Snackbar.make(view, "Usage access settings aren't available on this device", Snackbar.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.temp_unit_row).setOnClickListener(v -> {
            String key = com.desire.widget.engine.components.WeatherRenderer.PREF_FAHRENHEIT;
            prefs.putBoolean(key, !prefs.getBoolean(key, false));
            setRowSubtitle(view, R.id.temp_unit_row, tempUnitLabel());
            com.desire.widget.engine.runtime.EngineRenderer.render(requireContext(),
                    com.desire.widget.engine.runtime.EngineWidgetStore.allPlacedIds(requireContext()));
        });

        view.findViewById(R.id.notification_permission_row).setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
            } else {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
                startActivity(intent);
            }
        });

        view.findViewById(R.id.battery_optimization_row).setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            startActivity(intent);
        });

        view.findViewById(R.id.clear_cache_row).setOnClickListener(v -> {
            Glide.get(requireContext()).clearMemory();
            com.desire.widget.util.AppExecutors.getInstance().diskIO().execute(() -> {
                Glide.get(requireContext()).clearDiskCache();
                deleteDir(requireContext().getCacheDir());
                requireActivity().runOnUiThread(() -> {
                    updateCacheSize(view);
                    Snackbar.make(view, "Cache cleared", Snackbar.LENGTH_SHORT).show();
                });
            });
        });

        view.findViewById(R.id.background_running_row).setOnClickListener(v -> {
            showGuideDialog("Background Running Guide",
                    "To keep Widgets running in background:\n\n" +
                    "1. Go to Settings > Apps > Widgets\n" +
                    "2. Tap on Battery\n" +
                    "3. Select 'Unrestricted'\n" +
                    "4. Enable 'Allow background activity'");
        });

        view.findViewById(R.id.faq_row).setOnClickListener(v ->
                showGuideDialog("FAQ", getString(R.string.faq_placeholder)));

        view.findViewById(R.id.privacy_policy_row).setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://desire.com/privacy"));
            startActivity(browserIntent);
        });

        view.findViewById(R.id.report_bug_row).setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:support@desire.com"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Widgets App Bug Report");
            startActivity(emailIntent);
        });

        view.findViewById(R.id.support_row).setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:support@desire.com"));
            startActivity(emailIntent);
        });

        view.findViewById(R.id.about_row).setOnClickListener(v -> showGuideDialog("About",
                "Widgets v" + viewModel.getVersionName() + "\n\n" +
                "A premium widget customization app.\n\n" +
                "© 2026 Desire Apps"));

        view.findViewById(R.id.social_row).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/desireapps"));
            startActivity(intent);
        });

        view.findViewById(R.id.more_apps_row).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/dev?id=desire"));
            startActivity(intent);
        });

        view.findViewById(R.id.rate_us_row).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + requireContext().getPackageName())));
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id="
                                + requireContext().getPackageName())));
            }
        });

        view.findViewById(R.id.share_app_row).setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "Check out Widgets app! https://play.google.com/store/apps/details?id="
                            + requireContext().getPackageName());
            startActivity(Intent.createChooser(shareIntent, "Share Widgets"));
        });

        view.findViewById(R.id.admin_dashboard_row).setOnClickListener(v -> openAdminSection());
    }

    private void deleteDir(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) deleteDir(f);
            else f.delete();
        }
    }

    private void showWhatsNewDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("What's New")
                .setMessage("Version 1.0\n\n" +
                        "• New widget customization studio\n" +
                        "• Real per-widget notes & to-do lists\n" +
                        "• Functional weather + music widgets\n" +
                        "• Performance improvements\n" +
                        "• Bug fixes")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showGuideDialog(String title, String content) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton("OK", null)
                .show();
    }

    private void openAdminSection() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new AdminFragment(), "Admin")
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Permission state may have changed while the user was in system Settings.
        refreshPermissionSubtitles();
    }
}
