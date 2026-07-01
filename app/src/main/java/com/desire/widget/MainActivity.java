package com.desire.widget;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.desire.widget.data.model.AppConfig;
import com.desire.widget.data.remote.FirebaseService;
import com.desire.widget.ui.customize.CustomizeFragment;
import com.desire.widget.ui.settings.SettingsFragment;
import com.desire.widget.ui.settings.SettingsViewModel;
import com.desire.widget.ui.widgets.WidgetsFragment;
import com.desire.widget.util.AppExecutors;
import com.desire.widget.util.ThemeManager;
import com.desire.widget.util.Tasks;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigation;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply selected app theme BEFORE super.onCreate so the theme takes effect from the start.
        ThemeManager.apply(this);
        super.onCreate(savedInstanceState);

        // Tell the system we'll handle insets ourselves
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);

        // Status/nav bar colors come from the active theme (set in Theme_Widgets variants).

        bottomNavigation = findViewById(R.id.bottom_navigation);

        // Push content down by status bar height and up by nav bar height
        View rootView = findViewById(R.id.root_layout);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() |
                            WindowInsetsCompat.Type.displayCutout()
            );
            // Top padding keeps content below status bar / notch
            v.setPadding(insets.left, insets.top, insets.right, 0);

            // Bottom margin on nav bar keeps it above gesture bar
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) bottomNavigation.getLayoutParams();
            params.bottomMargin = insets.bottom;
            bottomNavigation.setLayoutParams(params);

            return WindowInsetsCompat.CONSUMED;
        });

        fragmentManager = getSupportFragmentManager();
        new ViewModelProvider(this).get(SettingsViewModel.class);

        bottomNavigation.setOnItemSelectedListener(this::onNavigationItemSelected);

        if (savedInstanceState == null) {
            loadFragment(new WidgetsFragment(), "Widgets");
        }

        checkAppConfig();
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_widgets) {
            loadFragment(new WidgetsFragment(), "Widgets");
            return true;
        } else if (id == R.id.nav_customize) {
            loadFragment(new CustomizeFragment(), "Customize");
            return true;
        } else if (id == R.id.nav_settings) {
            loadFragment(new SettingsFragment(), "Settings");
            return true;
        }
        return false;
    }

    private void loadFragment(Fragment fragment, String tag) {
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .commit();
    }

    private void checkAppConfig() {
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                AppConfig config = Tasks.await(FirebaseService.getInstance().getAppConfig());
                if (config != null) {
                    if (config.isMaintenanceMode()) {
                        runOnUiThread(() -> showMaintenanceDialog(config.getMaintenanceMessage()));
                        return;
                    }
                    int currentVersion = getPackageManager()
                            .getPackageInfo(getPackageName(), 0).versionCode;
                    if (config.isForceUpdate() && config.getLatestVersion() > currentVersion) {
                        runOnUiThread(() -> showForceUpdateDialog(config.getForceUpdateMessage()));
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    private void showMaintenanceDialog(String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Under Maintenance")
                .setMessage(message != null ? message : "Please try again later.")
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> finish())
                .show();
    }

    private void showForceUpdateDialog(String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Update Available")
                .setMessage(message != null ? message : "A new version is required.")
                .setCancelable(false)
                .setPositiveButton("Update", (d, w) -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=" + getPackageName())));
                    } catch (Exception e) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
                    }
                })
                .show();
    }
}