package com.desire.widget.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.desire.widget.engine.runtime.EngineRenderer;
import com.desire.widget.engine.runtime.EngineWidgetStore;

/**
 * Re-renders calendar agenda widgets when the calendar provider reports a change (an event was
 * added, edited, or synced), so the list doesn't wait for the next minute tick.
 */
public class CalendarChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int[] all = EngineWidgetStore.allPlacedIds(context);
        int[] tmp = new int[all.length];
        int n = 0;
        for (int id : all) {
            String spec = EngineWidgetStore.getSpecJson(context, id);
            if (spec != null && spec.contains("\"agenda\"")) tmp[n++] = id;
        }
        if (n == 0) return;
        int[] ids = new int[n];
        System.arraycopy(tmp, 0, ids, 0, n);
        EngineRenderer.render(context, ids);
    }
}
