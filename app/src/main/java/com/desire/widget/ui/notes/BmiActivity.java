package com.desire.widget.ui.notes;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.desire.widget.engine.data.BmiStateStore;
import com.desire.widget.engine.runtime.EngineRenderer;
import com.desire.widget.util.ThemeManager;

/** Editor for the BMI widget: height (cm) + weight (kg), bound to a single {@code appWidgetId}. */
public class BmiActivity extends AppCompatActivity {
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
        title.setText("BMI");
        title.setTextColor(0xFFECEDF0);
        title.setTextSize(28f);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title);

        float curHeight = appWidgetId >= 0 ? BmiStateStore.getHeightCm(this, appWidgetId) : 0f;
        float curWeight = appWidgetId >= 0 ? BmiStateStore.getWeightKg(this, appWidgetId) : 0f;

        EditText heightInput = field(root, "Height (cm)", curHeight > 0 ? String.valueOf((int) curHeight) : "");
        EditText weightInput = field(root, "Weight (kg)", curWeight > 0 ? String.valueOf((int) curWeight) : "");

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
        sp.topMargin = dp(20);
        save.setOnClickListener(v -> {
            try {
                float h = Float.parseFloat(heightInput.getText().toString().trim());
                float w = Float.parseFloat(weightInput.getText().toString().trim());
                if (h <= 0 || w <= 0) throw new NumberFormatException();
                BmiStateStore.set(this, appWidgetId, h, w);
                if (appWidgetId >= 0) EngineRenderer.render(getApplicationContext(), new int[]{appWidgetId});
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
                finish();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Enter valid height and weight", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(save, sp);

        setContentView(root);
    }

    private EditText field(LinearLayout root, String hint, String initial) {
        TextView label = new TextView(this);
        label.setText(hint);
        label.setTextColor(0xFF8A8F99);
        label.setTextSize(13f);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.topMargin = dp(20);
        root.addView(label, llp);

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(initial);
        input.setTextColor(0xFFECEDF0);
        input.setTextSize(20f);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF212429);
        bg.setCornerRadius(dp(14));
        bg.setStroke(1, 0x14FFFFFF);
        input.setBackground(bg);
        input.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ilp.topMargin = dp(6);
        root.addView(input, ilp);
        return input;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
