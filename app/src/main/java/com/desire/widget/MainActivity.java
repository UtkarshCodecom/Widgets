package com.desire.widget;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.desire.widget.data.model.AppConfig;
import com.desire.widget.data.remote.FirebaseService;
import com.desire.widget.ui.customize.CustomizeFragment;
import com.desire.widget.ui.settings.SettingsFragment;
import com.desire.widget.ui.settings.SettingsViewModel;
import com.desire.widget.ui.widgets.WidgetsFragment;
import com.desire.widget.util.Tasks;
import com.desire.widget.util.AppExecutors;
import com.google.android.datatransport.BuildConfig;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigation;
    private FragmentManager fragmentManager;
    private SettingsViewModel settingsViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dark_background));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dark_background));

        fragmentManager = getSupportFragmentManager();
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        bottomNavigation = findViewById(R.id.bottom_navigation);
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

                    int currentVersion = BuildConfig.VERSION_CODE;
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
