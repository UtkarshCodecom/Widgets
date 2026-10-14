package com.desire.widget.widget;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.desire.widget.MainActivity;
import com.desire.widget.engine.action.ActionCompiler;
import com.desire.widget.engine.model.WidgetSpec;
import com.desire.widget.engine.model.WidgetStyle;
import com.desire.widget.engine.runtime.EngineWidgetStore;
import com.google.gson.Gson;

/**
 * Invisible trampoline that every widget tap goes through. Because it is OUR component, the tap
 * PendingIntent always resolves (so a tap never silently does nothing). It figures out the target
 * — the user-chosen "Tap opens" app, else the design's action — launches it, and always falls back
 * to opening the app. This is far more reliable than pointing the widget's PendingIntent straight
 * at another app's (possibly unresolvable) implicit intent.
 */
public class WidgetTapActivity extends Activity {
    public static final String EXTRA_APPWIDGET_ID = "appwidget_id";
    private static final Gson GSON = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);

        int appWidgetId = getIntent().getIntExtra(EXTRA_APPWIDGET_ID, -1);
        Intent target = resolveTarget(appWidgetId);

        try {
            if (target != null) {
                target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                android.util.Log.i("WidgetTap", "id=" + appWidgetId + " launching "
                        + (target.getComponent() != null ? target.getComponent().flattenToShortString()
                        : String.valueOf(target.getPackage() != null ? target.getPackage() : target.getAction())));
                startActivity(target);
            } else {
                android.util.Log.w("WidgetTap", "id=" + appWidgetId + " no target -> opening app");
                openApp();
            }
        } catch (Exception e) {
            android.util.Log.w("WidgetTap", "id=" + appWidgetId + " launch failed -> opening app", e);
            openApp();
        }
        finish();
        overridePendingTransition(0, 0);
    }

    private Intent resolveTarget(int appWidgetId) {
        if (appWidgetId < 0) return null;
        try {
            // 1) User-chosen "Tap opens" app.
            String styleJson = EngineWidgetStore.getStyleJson(this, appWidgetId);
            if (styleJson != null && !styleJson.isEmpty()) {
                WidgetStyle style = GSON.fromJson(styleJson, WidgetStyle.class);
                if (style != null && style.tapPackage != null && !style.tapPackage.isEmpty()) {
                    Intent launch = getPackageManager().getLaunchIntentForPackage(style.tapPackage);
                    if (launch != null) return launch;
                }
            }
            // 2) The design's own action.
            String specJson = EngineWidgetStore.getSpecJson(this, appWidgetId);
            if (specJson != null && !specJson.isEmpty()) {
                WidgetSpec spec = GSON.fromJson(specJson, WidgetSpec.class);
                if (spec != null) {
                    // Data widgets that are still locked: the first tap unlocks them.
                    if (hasComponent(spec, "agenda") && checkSelfPermission(
                            android.Manifest.permission.READ_CALENDAR) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        return new Intent(this, com.desire.widget.ui.permission.PermissionRequestActivity.class)
                                .putExtra(com.desire.widget.ui.permission.PermissionRequestActivity.EXTRA_PERMISSION,
                                        android.Manifest.permission.READ_CALENDAR)
                                .putExtra(com.desire.widget.ui.permission.PermissionRequestActivity.EXTRA_REASON,
                                        "Allow Calendar in App info → Permissions to show your events");
                    }
                    if (hasComponent(spec, "screen_time")
                            && !com.desire.widget.engine.data.ScreenTimeStore.hasAccess(this)) {
                        android.widget.Toast.makeText(this, "Find \"Widgets\" and turn on usage access",
                                android.widget.Toast.LENGTH_LONG).show();
                        return new Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS);
                    }
                    // Any widget that shows weather opens the weather app (same as the sunrise/sunset
                    // widget). This also upgrades older placements whose stored action still points
                    // at weather.com, without the user needing to re-add the widget.
                    if (hasComponent(spec, "weather")) {
                        com.desire.widget.engine.model.ActionSpec weather =
                                new com.desire.widget.engine.model.ActionSpec();
                        weather.type = "weather";
                        return ActionCompiler.buildTargetIntent(this, weather);
                    }
                    if (spec.action != null) {
                        // Instance-aware editors: open the editor bound to THIS widget's id.
                        Intent editor = editorIntent(spec.action.type, appWidgetId);
                        if (editor != null) return editor;
                        return ActionCompiler.buildTargetIntent(this, spec.action);
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private boolean hasComponent(WidgetSpec spec, String type) {
        if (spec.components == null) return false;
        for (com.desire.widget.engine.model.ComponentSpec c : spec.components) {
            if (c != null && type.equals(c.type)) return true;
        }
        return false;
    }

    private Intent editorIntent(String type, int appWidgetId) {
        Class<?> cls;
        if ("edit_notes".equals(type)) {
            cls = com.desire.widget.ui.notes.NotesActivity.class;
        } else if ("edit_todo".equals(type)) {
            cls = com.desire.widget.ui.notes.TodoActivity.class;
        } else if ("edit_qr".equals(type)) {
            cls = com.desire.widget.ui.notes.QrActivity.class;
        } else if ("edit_bmi".equals(type)) {
            cls = com.desire.widget.ui.notes.BmiActivity.class;
        } else if ("edit_habits".equals(type)) {
            cls = com.desire.widget.ui.notes.HabitActivity.class;
        } else {
            return null;
        }
        Intent i = new Intent(this, cls);
        i.putExtra("appwidget_id", appWidgetId);
        return i;
    }

    private void openApp() {
        try {
            startActivity(new Intent(this, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
        } catch (Exception ignored) {}
    }
}
