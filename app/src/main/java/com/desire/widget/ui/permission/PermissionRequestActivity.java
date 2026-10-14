package com.desire.widget.ui.permission;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import com.desire.widget.engine.runtime.EngineRenderer;
import com.desire.widget.engine.runtime.EngineWidgetStore;

/**
 * Invisible host for a runtime-permission prompt launched from a widget tap (e.g. the calendar
 * agenda asking for READ_CALENDAR). Shows the system dialog, re-renders every placed widget so the
 * newly unlocked data appears immediately, then finishes. If the user previously chose "Don't ask
 * again", the system won't show a dialog, so we open the app's settings page instead.
 */
public class PermissionRequestActivity extends Activity {
    public static final String EXTRA_PERMISSION = "permission";
    public static final String EXTRA_REASON = "reason";
    private static final int REQUEST = 41;

    private String permission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permission = getIntent().getStringExtra(EXTRA_PERMISSION);
        if (permission == null) {
            finish();
            return;
        }
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            refreshAndFinish();
            return;
        }
        if (savedInstanceState == null) requestPermissions(new String[]{permission}, REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST) return;
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (!granted && !shouldShowRequestPermissionRationale(permission)) {
            // Permanently denied (or blocked by policy): the dialog can't be shown again.
            String reason = getIntent().getStringExtra(EXTRA_REASON);
            Toast.makeText(this, reason != null ? reason : "Allow the permission in App info → Permissions",
                    Toast.LENGTH_LONG).show();
            try {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception ignored) {}
        }
        refreshAndFinish();
    }

    private void refreshAndFinish() {
        EngineRenderer.render(this, EngineWidgetStore.allPlacedIds(this));
        finish();
        overridePendingTransition(0, 0);
    }
}
