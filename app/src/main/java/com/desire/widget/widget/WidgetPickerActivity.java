package com.desire.widget.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

import com.desire.widget.engine.runtime.EngineRenderer;
import com.desire.widget.engine.runtime.EngineWidgetStore;
import com.desire.widget.ui.customize.CustomizeWidgetActivity;

/**
 * The widget configure activity. On a fresh add (pin flow) it commits the pending spec and renders.
 * When the launcher re-launches it to <b>reconfigure</b> an already-placed widget, it opens the
 * visual Customize screen bound to that appWidgetId so the user can restyle it in place.
 */
public class WidgetPickerActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int appWidgetId = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        String existing = EngineWidgetStore.getSpecJson(this, appWidgetId);
        if (existing != null && !existing.isEmpty()) {
            // Reconfigure an existing widget in the visual editor.
            Intent i = new Intent(this, CustomizeWidgetActivity.class);
            i.putExtra(CustomizeWidgetActivity.EXTRA_SPEC_JSON, existing);
            i.putExtra(CustomizeWidgetActivity.EXTRA_NAME, EngineWidgetStore.getName(this, appWidgetId));
            i.putExtra(CustomizeWidgetActivity.EXTRA_SIZE, EngineWidgetStore.getSize(this, appWidgetId));
            i.putExtra(CustomizeWidgetActivity.EXTRA_APPWIDGET_ID, appWidgetId);
            startActivity(i);
        } else {
            // Fresh add: commit the spec chosen before the pin flow, then render.
            EngineWidgetStore.commitPending(this, appWidgetId);
            EngineRenderer.render(this, new int[]{appWidgetId});
        }
        setResult(RESULT_OK, getIntent());
        finish();
    }
}
