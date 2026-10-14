package com.desire.widget.ui.notes;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.desire.widget.engine.data.UserDataStore;
import com.desire.widget.engine.runtime.EngineRenderer;
import com.desire.widget.util.ThemeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Functional editor for the To-Do widget bound to a single {@code appWidgetId}: add items, tick
 * them off, delete them. Every change is saved to {@link UserDataStore} and re-renders that one
 * placed widget immediately — other To-Do widgets are never touched.
 */
public class TodoActivity extends AppCompatActivity {
    public static final String EXTRA_APPWIDGET_ID = "appwidget_id";
    private static final int ACCENT = 0xFF6C7BFF;
    private static final int CARD = 0xFF212429;
    private static final int TEXT = 0xFFECEDF0;
    private static final int MUTED = 0xFF8A8F99;

    private List<UserDataStore.Todo> todos;
    private LinearLayout list;
    private TextView emptyState;
    private TextView subtitleView;
    private int appWidgetId = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeManager.apply(this);
        super.onCreate(savedInstanceState);
        appWidgetId = getIntent().getIntExtra(EXTRA_APPWIDGET_ID, -1);
        todos = appWidgetId >= 0 ? UserDataStore.getTodos(this, appWidgetId) : new ArrayList<>();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(16));
        root.setFitsSystemWindows(true);

        TextView title = new TextView(this);
        title.setText("To-Do");
        title.setTextColor(TEXT);
        title.setTextSize(28f);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title);

        subtitleView = new TextView(this);
        subtitleView.setText(todos.size() + " tasks");
        subtitleView.setTextColor(MUTED);
        subtitleView.setTextSize(13f);
        LinearLayout.LayoutParams stlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stlp.topMargin = dp(2);
        root.addView(subtitleView, stlp);

        // Add row — pill input + circular add button
        LinearLayout addRow = new LinearLayout(this);
        addRow.setOrientation(LinearLayout.HORIZONTAL);
        addRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams arlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        arlp.topMargin = dp(18);
        final EditText input = new EditText(this);
        input.setHint("Add a task…");
        input.setHintTextColor(0x66FFFFFF);
        input.setTextColor(TEXT);
        input.setSingleLine(true);
        input.setBackground(pill(0x14FFFFFF));
        input.setPadding(dp(16), dp(13), dp(16), dp(13));
        addRow.addView(input, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView add = new TextView(this);
        add.setText("＋");
        add.setGravity(Gravity.CENTER);
        add.setTextColor(Color.WHITE);
        add.setTextSize(22f);
        add.setBackground(circle(ACCENT));
        add.setForeground(rippleForeground());
        LinearLayout.LayoutParams addBtnLp = new LinearLayout.LayoutParams(dp(48), dp(48));
        addBtnLp.leftMargin = dp(10);
        Runnable addTask = () -> {
            String t = input.getText().toString().trim();
            if (!t.isEmpty()) {
                todos.add(0, new UserDataStore.Todo(t, false));
                input.setText("");
                save();
                rebuild();
            }
        };
        add.setOnClickListener(v -> addTask.run());
        addRow.addView(add, addBtnLp);
        root.addView(addRow, arlp);

        emptyState = new TextView(this);
        emptyState.setText("No tasks yet.\nAdd one above to get started.");
        emptyState.setTextColor(MUTED);
        emptyState.setTextSize(14f);
        emptyState.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams eslp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        eslp.topMargin = dp(48);
        root.addView(emptyState, eslp);

        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
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
        emptyState.setVisibility(todos.isEmpty() ? View.VISIBLE : View.GONE);
        subtitleView.setText(todos.size() + (todos.size() == 1 ? " task" : " tasks"));

        for (int i = 0; i < todos.size(); i++) {
            final int index = i;
            final UserDataStore.Todo t = todos.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(roundedCard(CARD, 16));
            row.setPadding(dp(14), dp(12), dp(10), dp(12));
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.bottomMargin = dp(10);
            row.setAlpha(0f);
            row.setScaleY(0.85f);

            CheckBox cb = new CheckBox(this);
            cb.setChecked(t.done);
            cb.setButtonTintList(android.content.res.ColorStateList.valueOf(ACCENT));
            row.addView(cb);

            TextView text = new TextView(this);
            text.setText(t.text);
            text.setTextColor(t.done ? MUTED : TEXT);
            text.setTextSize(16f);
            if (t.done) text.setPaintFlags(text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            else text.setPaintFlags(text.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tlp.leftMargin = dp(4);
            row.addView(text, tlp);

            cb.setOnCheckedChangeListener((btn, checked) -> {
                t.done = checked;
                text.setTextColor(checked ? MUTED : TEXT);
                if (checked) text.setPaintFlags(text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                else text.setPaintFlags(text.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                // Small pulse for tactile feedback on toggle.
                row.animate().scaleX(0.98f).scaleY(0.98f).setDuration(80).withEndAction(() ->
                        row.animate().scaleX(1f).scaleY(1f).setDuration(120).start()).start();
                save();
            });

            TextView del = new TextView(this);
            del.setText("✕");
            del.setTextColor(0x80FFFFFF);
            del.setTextSize(16f);
            del.setPadding(dp(10), dp(6), dp(4), dp(6));
            del.setForeground(rippleForeground());
            del.setOnClickListener(v -> {
                row.animate().alpha(0f).translationX(dp(40)).setDuration(160).withEndAction(() -> {
                    todos.remove(index);
                    save();
                    rebuild();
                }).start();
            });
            row.addView(del);

            list.addView(row, rlp);

            // Staggered entrance animation.
            AnimatorSet set = new AnimatorSet();
            set.playTogether(
                    ObjectAnimator.ofFloat(row, "alpha", 0f, 1f),
                    ObjectAnimator.ofFloat(row, "scaleY", 0.85f, 1f));
            set.setStartDelay(Math.min(index * 30L, 200L));
            set.setDuration(220);
            set.setInterpolator(new OvershootInterpolator(1.2f));
            set.start();
        }
    }

    private void save() {
        UserDataStore.setTodos(this, appWidgetId, todos);
        if (appWidgetId >= 0) {
            EngineRenderer.render(getApplicationContext(), new int[]{appWidgetId});
        }
    }

    private GradientDrawable pill(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(24));
        return d;
    }

    private GradientDrawable circle(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    private GradientDrawable roundedCard(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(1, 0x14FFFFFF);
        return d;
    }

    private android.graphics.drawable.Drawable rippleForeground() {
        return androidx.core.content.ContextCompat.getDrawable(this,
                android.R.drawable.list_selector_background);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
