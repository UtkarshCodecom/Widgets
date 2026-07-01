package com.desire.widget.ui.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.View;

import com.desire.widget.data.local.entity.WidgetEntity;
import com.desire.widget.data.remote.FirebaseService;
import com.desire.widget.engine.runtime.EngineWidgetStore;
import com.desire.widget.util.AppExecutors;
import com.desire.widget.util.Tasks;
import com.desire.widget.widget.WidgetPinCallback;
import com.desire.widget.widget.WidgetSchema;
import com.google.android.material.snackbar.Snackbar;

/**
 * Sends a widget straight to the launcher's "add to home screen" (pin) flow. There is no in-app
 * preview step — the system pin dialog is the only confirmation the user sees.
 */
public final class WidgetInstaller {

    private WidgetInstaller() {}

    public static void installToHome(View anchor, WidgetEntity widget) {
        Context context = anchor.getContext();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Snackbar.make(anchor, "Adding widgets needs Android 8.0+", Snackbar.LENGTH_LONG).show();
            return;
        }

        AppWidgetManager manager = context.getSystemService(AppWidgetManager.class);
        if (manager == null || !manager.isRequestPinAppWidgetSupported()) {
            Snackbar.make(anchor, "Your launcher does not support adding widgets automatically",
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        String specJson = widget.getSpecJson();
        if (specJson == null || specJson.trim().isEmpty()) {
            Snackbar.make(anchor, "This widget has no native definition yet", Snackbar.LENGTH_LONG).show();
            return;
        }

        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                Tasks.await(FirebaseService.getInstance().incrementWidgetDownload(widget.getId()));
            } catch (Exception ignored) {}
        });

        installSpecToHome(anchor, widget.getId(), widget.getName(), specJson, widget.getWidgetSize());
    }

    /**
     * Pins a widget defined by a raw native spec (used by the Studio and the gallery).
     */
    public static void installSpecToHome(View anchor, String widgetId, String name,
                                         String specJson, String sizeRaw) {
        Context context = anchor.getContext();
        if (specJson == null || specJson.trim().isEmpty()) {
            Snackbar.make(anchor, "Widget has no definition", Snackbar.LENGTH_LONG).show();
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Snackbar.make(anchor, "Adding widgets needs Android 8.0+", Snackbar.LENGTH_LONG).show();
            return;
        }
        AppWidgetManager manager = context.getSystemService(AppWidgetManager.class);
        if (manager == null || !manager.isRequestPinAppWidgetSupported()) {
            Snackbar.make(anchor, "Your launcher does not support adding widgets automatically",
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        String size = WidgetSchema.normalizeSize(sizeRaw);
        EngineWidgetStore.savePending(context, widgetId, name, specJson, size);

        Intent callbackIntent = new Intent(context, WidgetPinCallback.class);
        callbackIntent.setAction(WidgetPinCallback.ACTION_WIDGET_PINNED);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        PendingIntent callback = PendingIntent.getBroadcast(context, 2001, callbackIntent, flags);
        manager.requestPinAppWidget(WidgetSchema.providerComponent(context, size), null, callback);
    }
}
