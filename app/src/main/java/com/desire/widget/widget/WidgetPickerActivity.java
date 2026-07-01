package com.desire.widget.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.os.Bundle;

import com.desire.widget.engine.runtime.EngineRenderer;
import com.desire.widget.engine.runtime.EngineWidgetStore;

public class WidgetPickerActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int appWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            EngineWidgetStore.commitPending(this, appWidgetId);
            EngineRenderer.render(this, new int[]{appWidgetId});
            setResult(RESULT_OK, getIntent());
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }
}
