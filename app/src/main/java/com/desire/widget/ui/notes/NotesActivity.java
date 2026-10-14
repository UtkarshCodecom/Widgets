package com.desire.widget.ui.notes;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
 * Simple full-screen editor for the Notes widget. Saving writes to {@link UserDataStore} and
 * re-renders any placed widgets so they update immediately.
 */
public class NotesActivity extends AppCompatActivity {
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
        title.setText("Notes");
        title.setTextColor(0xFFECEDF0);
        title.setTextSize(28f);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(appWidgetId >= 0 ? "This note belongs only to this widget" : "");
        subtitle.setTextColor(0xFF8A8F99);
        subtitle.setTextSize(13f);
        LinearLayout.LayoutParams stlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stlp.topMargin = dp(2);
        root.addView(subtitle, stlp);

        final EditText input = new EditText(this);
        input.setGravity(Gravity.TOP | Gravity.START);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(0xFF212429);
        cardBg.setCornerRadius(dp(18));
        cardBg.setStroke(1, 0x14FFFFFF);
        input.setBackground(cardBg);
        input.setTextColor(0xFFECEDF0);
        input.setTextSize(16f);
        input.setLineSpacing(dp(4), 1f);
        input.setHint("Write a note…");
        input.setHintTextColor(0x66FFFFFF);
        input.setPadding(dp(18), dp(18), dp(18), dp(18));
        input.setText(appWidgetId >= 0 ? UserDataStore.getNotes(this, appWidgetId) : "");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        lp.topMargin = dp(18);
        root.addView(input, lp);

        TextView save = new TextView(this);
        save.setText("SAVE");
        save.setGravity(Gravity.CENTER);
        save.setTextColor(Color.WHITE);
        save.setTypeface(save.getTypeface(), android.graphics.Typeface.BOLD);
        save.setLetterSpacing(0.06f);
        GradientDrawable saveBg = new GradientDrawable();
        saveBg.setColor(0xFF6C7BFF);
        saveBg.setCornerRadius(dp(16));
        save.setBackground(saveBg);
        save.setPadding(0, dp(17), 0, dp(17));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin = dp(16);
        save.setOnClickListener(v -> {
            UserDataStore.setNotes(this, appWidgetId, input.getText().toString());
            if (appWidgetId >= 0) {
                EngineRenderer.render(getApplicationContext(), new int[]{appWidgetId});
            }
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
            finish();
        });
        root.addView(save, sp);

        setContentView(root);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
