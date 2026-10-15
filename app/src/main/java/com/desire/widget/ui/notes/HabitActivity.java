package com.desire.widget.ui.notes;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.desire.widget.engine.data.HabitStore;
import com.desire.widget.engine.runtime.EngineRenderer;
import com.desire.widget.util.ThemeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor for the Habit Tracker widget: add habits, toggle today's completion, delete a habit.
 * Bound to a single {@code appWidgetId} — every habit list is independent per placed widget.
 */
public class HabitActivity extends AppCompatActivity {
    public static final String EXTRA_APPWIDGET_ID = "appwidget_id";
    private static final int ACCENT = 0xFF6C7BFF;

    private List<HabitStore.Habit> habits;
    private LinearLayout list;
    private int appWidgetId = -1;
    private String todayKey;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeManager.apply(this);
        super.onCreate(savedInstanceState);
        appWidgetId = getIntent().getIntExtra(EXTRA_APPWIDGET_ID, -1);
        habits = appWidgetId >= 0 ? HabitStore.getHabits(this, appWidgetId) : new ArrayList<>();
        todayKey = HabitStore.todayKey(System.currentTimeMillis());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(16));
        root.setFitsSystemWindows(true);

        TextView title = new TextView(this);
        title.setText("Habits");
        title.setTextColor(0xFFECEDF0);
        title.setTextSize(28f);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title);

        LinearLayout addRow = new LinearLayout(this);
        addRow.setOrientation(LinearLayout.HORIZONTAL);
        addRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams arlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        arlp.topMargin = dp(18);
        final EditText input = new EditText(this);
        input.setHint("New habit…");
        input.setHintTextColor(0x66FFFFFF);
        input.setTextColor(0xFFECEDF0);
        input.setSingleLine(true);
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(0x14FFFFFF);
        pill.setCornerRadius(dp(24));
        input.setBackground(pill);
        input.setPadding(dp(16), dp(13), dp(16), dp(13));
        addRow.addView(input, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView add = new TextView(this);
        add.setText("＋");
        add.setGravity(Gravity.CENTER);
        add.setTextColor(Color.WHITE);
        add.setTextSize(22f);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(ACCENT);
        add.setBackground(circle);
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(dp(48), dp(48));
        addLp.leftMargin = dp(10);
        add.setOnClickListener(v -> {
            String t = input.getText().toString().trim();
            if (!t.isEmpty()) {
                habits.add(new HabitStore.Habit(t));
                input.setText("");
                save();
                rebuild();
            }
        });
        addRow.addView(add, addLp);
        root.addView(addRow, arlp);

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(14), 0, dp(24));
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        rebuild();
    }

    private void rebuild() {
        list.removeAllViews();
        for (int i = 0; i < habits.size(); i++) {
            final int index = i;
            final HabitStore.Habit h = habits.get(i);
            boolean doneToday = h.doneDates != null && h.doneDates.contains(todayKey);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setColor(0xFF212429);
            cardBg.setCornerRadius(dp(16));
            cardBg.setStroke(1, 0x14FFFFFF);
            row.setBackground(cardBg);
            row.setPadding(dp(16), dp(14), dp(10), dp(14));
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.bottomMargin = dp(10);

            TextView name = new TextView(this);
            name.setText(h.name);
            name.setTextColor(0xFFECEDF0);
            name.setTextSize(16f);
            row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView todayToggle = new TextView(this);
            todayToggle.setText(doneToday ? "✓ Today" : "Mark today");
            todayToggle.setTextColor(doneToday ? Color.WHITE : ACCENT);
            todayToggle.setTextSize(13f);
            todayToggle.setPadding(dp(14), dp(8), dp(14), dp(8));
            GradientDrawable toggleBg = new GradientDrawable();
            toggleBg.setCornerRadius(dp(18));
            toggleBg.setColor(doneToday ? ACCENT : 0x1A6C7BFF);
            todayToggle.setBackground(toggleBg);
            todayToggle.setOnClickListener(v -> {
                if (h.doneDates == null) h.doneDates = new java.util.LinkedHashSet<>();
                if (h.doneDates.contains(todayKey)) h.doneDates.remove(todayKey);
                else h.doneDates.add(todayKey);
                save();
                rebuild();
            });
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tlp.rightMargin = dp(8);
            row.addView(todayToggle, tlp);

            TextView del = new TextView(this);
            del.setText("✕");
            del.setTextColor(0x80FFFFFF);
            del.setTextSize(16f);
            del.setPadding(dp(8), dp(6), dp(4), dp(6));
            del.setOnClickListener(v -> {
                habits.remove(index);
                save();
                rebuild();
            });
            row.addView(del);

            list.addView(row, rlp);
        }

        if (habits.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No habits yet. Add one above.");
            empty.setTextColor(0xFF8A8F99);
            empty.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            elp.topMargin = dp(40);
            list.addView(empty, elp);
        }
    }

    private void save() {
        HabitStore.setHabits(this, appWidgetId, habits);
        if (appWidgetId >= 0) EngineRenderer.render(getApplicationContext(), new int[]{appWidgetId});
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
