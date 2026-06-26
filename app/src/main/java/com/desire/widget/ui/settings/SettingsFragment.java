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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.desire.widget.R;
import com.desire.widget.ui.admin.AdminFragment;
import com.desire.widget.util.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;
import java.util.Map;

public class SettingsFragment extends Fragment {
    private SettingsViewModel viewModel;
    private PreferenceManager prefs;
    private TextView developerTapHint;
    private View developerSection;
    private View premiumBanner;
    private int tapCount = 0;

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
        View versionRow = view.findViewById(R.id.version_row);
        TextView versionValue = view.findViewById(R.id.version_value);

        versionValue.setText(viewModel.getVersionName());

        setRowTitles(view);

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
    }

    private void setRowTitles(View view) {
        Map<Integer, String> rowTitles = new HashMap<>();
        rowTitles.put(R.id.check_updates_row, "Check Updates");
        rowTitles.put(R.id.whats_new_row, "What's New");
        rowTitles.put(R.id.restore_purchases_row, "Restore Purchases");
        rowTitles.put(R.id.notification_permission_row, "Notification Permission");
        rowTitles.put(R.id.background_running_row, "Background Running Guide");
        rowTitles.put(R.id.battery_optimization_row, "Battery Optimization Guide");
        rowTitles.put(R.id.faq_row, "FAQ");
        rowTitles.put(R.id.privacy_policy_row, "Privacy Policy");
        rowTitles.put(R.id.report_bug_row, "Report Bug");
        rowTitles.put(R.id.support_row, "Support");
        rowTitles.put(R.id.about_row, "About");
        rowTitles.put(R.id.social_row, "Social Links");
        rowTitles.put(R.id.more_apps_row, "More Apps");
        rowTitles.put(R.id.rate_us_row, "Rate Us");
        rowTitles.put(R.id.share_app_row, "Share App");
        rowTitles.put(R.id.admin_dashboard_row, "Dashboard");

        for (Map.Entry<Integer, String> entry : rowTitles.entrySet()) {
            View row = view.findViewById(entry.getKey());
            if (row != null) {
                TextView title = row.findViewById(R.id.row_title);
                if (title != null) {
                    title.setText(entry.getValue());
                }
            }
        }
    }

    private void setupClickListeners(View view) {
        view.findViewById(R.id.premium_banner).setOnClickListener(v -> {
            Snackbar.make(view, "Premium features coming soon", Snackbar.LENGTH_SHORT).show();
        });

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

        view.findViewById(R.id.whats_new_row).setOnClickListener(v -> {
            showWhatsNewDialog();
        });

        view.findViewById(R.id.restore_purchases_row).setOnClickListener(v -> {
            viewModel.restorePurchases();
            viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
                if (!loading) {
                    Snackbar.make(view, "Purchases restored", Snackbar.LENGTH_SHORT).show();
                }
            });
        });

        view.findViewById(R.id.notification_permission_row).setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
            } else {
                Snackbar.make(view, "Notifications enabled", Snackbar.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.background_running_row).setOnClickListener(v -> {
            showGuideDialog("Background Running Guide",
                    "To keep Widgets running in background:\n\n" +
                    "1. Go to Settings > Apps > Widgets\n" +
                    "2. Tap on Battery\n" +
                    "3. Select 'Unrestricted'\n" +
                    "4. Enable 'Allow background activity'");
        });

        view.findViewById(R.id.battery_optimization_row).setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            startActivity(intent);
        });

        view.findViewById(R.id.faq_row).setOnClickListener(v -> {
            showGuideDialog("FAQ", getString(R.string.faq_placeholder));
        });

        view.findViewById(R.id.privacy_policy_row).setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://desire.com/privacy"));
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

        view.findViewById(R.id.about_row).setOnClickListener(v -> {
            showGuideDialog("About",
                    "Widgets v" + viewModel.getVersionName() + "\n\n" +
                    "A premium widget customization app.\n\n" +
                    "© 2026 Desire Apps");
        });

        view.findViewById(R.id.social_row).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://instagram.com/desireapps"));
            startActivity(intent);
        });

        view.findViewById(R.id.more_apps_row).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/dev?id=desire"));
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

        // Developer section
        view.findViewById(R.id.admin_dashboard_row).setOnClickListener(v -> {
            openAdminSection();
        });
    }

    private void showWhatsNewDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("What's New")
                .setMessage("Version 1.0\n\n" +
                        "• New widget customization studio\n" +
                        "• Premium dark theme with yellow accent\n" +
                        "• Dynamic widget loading from cloud\n" +
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
}
