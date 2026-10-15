package com.desire.widget.ui.notes;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.desire.widget.engine.data.UserDataStore;
import com.desire.widget.engine.runtime.EngineRenderer;
import com.desire.widget.engine.runtime.EngineWidgetStore;
import com.desire.widget.util.ThemeManager;

/**
 * Editor for the QR widget. Enter any text/URL, or a Wi-Fi string
 * ({@code WIFI:S:<ssid>;T:WPA;P:<password>;;}). Saved to {@link UserDataStore} and re-rendered.
 */
public class QrActivity extends AppCompatActivity {
    public static final String EXTRA_APPWIDGET_ID = "appwidget_id";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeManager.apply(this);
        super.onCreate(savedInstanceState);
        final int appWidgetId = getIntent().getIntExtra(EXTRA_APPWIDGET_ID, -1);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(22));
        root.setFitsSystemWindows(true);

        TextView title = new TextView(this);
        title.setText("QR content");
        title.setTextColor(0xFFECEDF0);
        title.setTextSize(28f);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText("A URL, text, or Wi-Fi:\nWIFI:S:MyNetwork;T:WPA;P:password;;");
        hint.setTextColor(0x99FFFFFF);
        hint.setTextSize(12f);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = dp(6);
        root.addView(hint, hlp);

        final EditText input = new EditText(this);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setBackgroundColor(0x14FFFFFF);
        input.setTextColor(0xFFECEDF0);
        input.setHintTextColor(0x66FFFFFF);
        input.setPadding(dp(14), dp(14), dp(14), dp(14));
        input.setText(appWidgetId >= 0 ? UserDataStore.getQr(this, appWidgetId) : "");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        lp.topMargin = dp(16);
        root.addView(input, lp);

        TextView save = new TextView(this);
        save.setText("SAVE");
        save.setGravity(Gravity.CENTER);
        save.setTextColor(Color.WHITE);
        save.setTypeface(save.getTypeface(), android.graphics.Typeface.BOLD);
        save.setBackgroundColor(0xFF6C7BFF);
        save.setPadding(0, dp(16), 0, dp(16));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(16);
        save.setOnClickListener(v -> {
            UserDataStore.setQr(this, appWidgetId, input.getText().toString().trim());
            if (appWidgetId >= 0) {
                EngineRenderer.render(getApplicationContext(), new int[]{appWidgetId});
            }
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
            finish();
        });
        root.addView(save, slp);

        setContentView(root);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
