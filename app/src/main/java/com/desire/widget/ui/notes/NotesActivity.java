package com.desire.widget.ui.notes;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.desire.widget.R;
import com.desire.widget.engine.data.UserDataStore;
import com.desire.widget.engine.runtime.EngineRenderer;
import com.desire.widget.util.ThemeManager;

/**
 * Editor for the Notes widget bound to one {@code appWidgetId}. Everything autosaves — the widget
 * on the home screen updates as you type (debounced) — so there's no Save button to forget.
 * The paper colour and text size chosen here are per widget too.
 */
public class NotesActivity extends AppCompatActivity {
    public static final String EXTRA_APPWIDGET_ID = "appwidget_id";
    private static final long SAVE_DELAY_MS = 600;
    private static final String[] SIZE_LABELS = {"S", "M", "L"};

    private int appWidgetId = -1;
    private EditText input;
    private TextView status;
    private LinearLayout papers, sizes;
    private String paper = "";
    private int sizeIndex = 1;
    private String savedText = "";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable saveRunnable = this::saveNow;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeManager.apply(this);
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_notes);
        appWidgetId = getIntent().getIntExtra(EXTRA_APPWIDGET_ID, -1);

        View root = findViewById(R.id.notes_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });

        input = findViewById(R.id.notes_input);
        status = findViewById(R.id.notes_status);
        papers = findViewById(R.id.notes_papers);
        sizes = findViewById(R.id.notes_sizes);
        findViewById(R.id.notes_back).setOnClickListener(v -> finish());
        View done = findViewById(R.id.notes_done);
        done.setSelected(true);
        done.setOnClickListener(v -> finish());

        if (appWidgetId >= 0) {
            savedText = UserDataStore.getNotes(this, appWidgetId);
            paper = UserDataStore.getNotePaper(this, appWidgetId);
            sizeIndex = UserDataStore.getNoteSize(this, appWidgetId);
        }
        input.setText(savedText);
        input.setSelection(input.getText().length());
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                status.setText("Saving…");
                handler.removeCallbacks(saveRunnable);
                handler.postDelayed(saveRunnable, SAVE_DELAY_MS);
            }
        });

        buildPapers();
        buildSizes();
        applyPaper();
        updateStatus();
        input.requestFocus();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(saveRunnable);
        saveNow(); // leaving by Home / recents must not lose the last words
        super.onPause();
    }

    private void saveNow() {
        if (appWidgetId < 0) return;
        String text = input.getText().toString();
        if (!text.equals(savedText)) {
            UserDataStore.setNotes(this, appWidgetId, text);
            savedText = text;
            EngineRenderer.render(getApplicationContext(), new int[]{appWidgetId});
        }
        updateStatus();
    }

    private void updateStatus() {
        if (appWidgetId < 0) {
            status.setText("Preview — add the widget to keep notes");
            return;
        }
        long at = UserDataStore.getNotesUpdatedAt(this, appWidgetId);
        status.setText(at == 0 ? "Saved automatically" : "Saved · shows on your widget");
    }

    // ------------------------------------------------------------------ paper & size

    private void buildPapers() {
        papers.removeAllViews();
        for (int i = 0; i < UserDataStore.NOTE_PAPERS.length; i++) {
            final String hex = UserDataStore.NOTE_PAPERS[i];
            View swatch = new View(this);
            swatch.setContentDescription(UserDataStore.NOTE_PAPER_NAMES[i]);
            swatch.setTag(hex);
            swatch.setOnClickListener(v -> {
                paper = hex;
                if (appWidgetId >= 0) {
                    UserDataStore.setNotePaper(this, appWidgetId, hex);
                    EngineRenderer.render(getApplicationContext(), new int[]{appWidgetId});
                }
                applyPaper();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(34), dp(34));
            lp.setMarginEnd(dp(10));
            papers.addView(swatch, lp);
        }
    }

    private void buildSizes() {
        for (int i = 0; i < SIZE_LABELS.length; i++) {
            final int index = i;
            TextView chip = new TextView(this);
            chip.setText(SIZE_LABELS[i]);
            chip.setGravity(Gravity.CENTER);
            chip.setTextSize(13f + i * 2f);
            chip.setBackgroundResource(R.drawable.bg_category_chip);
            chip.setTextColor(getColorStateList(R.color.chip_text));
            chip.setOnClickListener(v -> {
                sizeIndex = index;
                if (appWidgetId >= 0) {
                    UserDataStore.setNoteSize(this, appWidgetId, index);
                    EngineRenderer.render(getApplicationContext(), new int[]{appWidgetId});
                }
                applySizes();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(44), dp(32));
            lp.setMarginEnd(dp(8));
            sizes.addView(chip, lp);
        }
        applySizes();
    }

    private void applySizes() {
        // Child 0 is the "Text size" label.
        for (int i = 1; i < sizes.getChildCount(); i++) sizes.getChildAt(i).setSelected(i - 1 == sizeIndex);
        input.setTextSize(17f * UserDataStore.NOTE_SIZES[sizeIndex]);
    }

    /** Swatch rings + the editor card takes the paper colour, so it looks like the widget. */
    private void applyPaper() {
        int accent = 0xFF6C7BFF;
        for (int i = 0; i < papers.getChildCount(); i++) {
            View v = papers.getChildAt(i);
            String hex = (String) v.getTag();
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(hex.isEmpty() ? 0xFF212429 : Color.parseColor(hex));
            boolean selected = hex.equals(paper);
            d.setStroke(dp(selected ? 3 : 1), selected ? accent : 0x40FFFFFF);
            v.setBackground(d);
        }
        int card = paper.isEmpty() ? 0xFF212429 : Color.parseColor(paper);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(card);
        bg.setCornerRadius(dp(20));
        bg.setStroke(1, 0x14FFFFFF);
        input.setBackground(bg);
        boolean light = luminance(card) > 0.6;
        input.setTextColor(light ? 0xFF2B2722 : 0xFFECEDF0);
    }

    private static double luminance(int c) {
        return (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)) / 255.0;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
