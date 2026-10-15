package com.desire.widget;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    // Immediately refresh weather now that we have a real location.
                    androidx.work.WorkManager.getInstance(this).enqueue(
                            new androidx.work.OneTimeWorkRequest.Builder(
                                    com.desire.widget.worker.WeatherWorker.class).build());
                }
            });

    public void requestLocationPermission() {
        if (com.desire.widget.util.LocationPermissionHelper.hasPermission(this)) return;
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
    }

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

        maybeShowOnboarding();
        checkAppConfig();
        maybeRequestLocationOnce();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshWeatherIfStale();
    }

    /**
     * Refresh weather whenever the app comes to the foreground (throttled to every 10 minutes).
     * Foreground is the one moment location is reliably available on OEM builds — background
     * refreshes with the screen locked can't get a fix and would reuse cached coordinates — so
     * this is what locks the weather onto the user's real city.
     */
    private void refreshWeatherIfStale() {
        if (!com.desire.widget.util.LocationPermissionHelper.hasPermission(this)) return;
        // Until one real location fix has been captured (WeatherWorker saves it), retry on every
        // app-open — cached data from a fallback city must not suppress getting the real one.
        boolean hasRealFix = getSharedPreferences("weather_worker", MODE_PRIVATE).contains("last_lat");
        long updatedAt = new com.desire.widget.engine.data.CachedLiveDataSource(this)
                .weather().updatedAtMillis;
        if (hasRealFix && System.currentTimeMillis() - updatedAt < 10 * 60_000L) return;
        androidx.work.WorkManager.getInstance(this).enqueue(
                new androidx.work.OneTimeWorkRequest.Builder(
                        com.desire.widget.worker.WeatherWorker.class).build());
    }

    /** Ask for location permission once (so weather widgets can use the real location), never nag. */
    private void maybeRequestLocationOnce() {
        com.desire.widget.util.PreferenceManager prefs = com.desire.widget.util.PreferenceManager.getInstance(this);
        if (prefs.getBoolean("asked_location_once", false)) return;
        prefs.putBoolean("asked_location_once", true);
        if (!com.desire.widget.util.LocationPermissionHelper.hasPermission(this)) {
            requestLocationPermission();
        }
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_widgets) {
            loadFragment(new WidgetsFragment(), "Widgets");
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

    private void maybeShowOnboarding() {
        if (com.desire.widget.util.PreferenceManager.getInstance(this).isFirstLaunch()) {
            startActivity(new Intent(this, com.desire.widget.ui.onboarding.OnboardingActivity.class));
        }
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