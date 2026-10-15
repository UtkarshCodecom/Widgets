package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.data.BmiStateStore;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;

import java.util.Locale;
import java.util.Map;

/**
 * Computes and shows Body Mass Index from the height/weight the user entered via the BMI editor
 * (per widget instance, {@link BmiStateStore}). Tap opens the editor (action type {@code edit_bmi}).
 * props: textColor, subColor.
 */
public class BmiRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "bmi";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> p = s.props;
        int textColor = SpecColors.resolve(SpecProps.str(p, "textColor", "@textPrimary"), ctx.theme, ctx.theme.textPrimary);
        int subColor = SpecColors.resolve(SpecProps.str(p, "subColor", "@textSecondary"), ctx.theme, ctx.theme.textSecondary);

        float heightCm = BmiStateStore.getHeightCm(ctx.android, ctx.appWidgetId);
        float weightKg = BmiStateStore.getWeightKg(ctx.android, ctx.appWidgetId);

        Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
        label.setColor(subColor);
        label.setTextAlign(Paint.Align.CENTER);
        label.setTextSize(b.height() * 0.13f);
        canvas.drawText("BMI", b.centerX(), b.top + b.height() * 0.2f, label);

        if (heightCm <= 0 || weightKg <= 0) {
            Paint hint = new Paint(Paint.ANTI_ALIAS_FLAG);
            hint.setColor(textColor);
            hint.setTextAlign(Paint.Align.CENTER);
            hint.setTextSize(b.height() * 0.15f);
            canvas.drawText("Tap to set up", b.centerX(), b.centerY(), hint);
            return;
        }

        float meters = heightCm / 100f;
        float bmi = weightKg / (meters * meters);
        String category;
        int categoryColor;
        if (bmi < 18.5f) { category = "Underweight"; categoryColor = 0xFF42A5F5; }
        else if (bmi < 25f) { category = "Normal"; categoryColor = 0xFF66BB6A; }
        else if (bmi < 30f) { category = "Overweight"; categoryColor = 0xFFFFA726; }
        else { category = "Obese"; categoryColor = 0xFFEF5350; }

        Paint value = new Paint(Paint.ANTI_ALIAS_FLAG);
        value.setColor(textColor);
        value.setTextAlign(Paint.Align.CENTER);
        value.setFakeBoldText(true);
        value.setTextSize(b.height() * 0.34f);
        Paint.FontMetrics fm = value.getFontMetrics();
        canvas.drawText(String.format(Locale.US, "%.1f", bmi), b.centerX(),
                b.centerY() - (fm.ascent + fm.descent) / 2f, value);

        Paint cat = new Paint(Paint.ANTI_ALIAS_FLAG);
        cat.setColor(categoryColor);
        cat.setTextAlign(Paint.Align.CENTER);
        cat.setFakeBoldText(true);
        cat.setTextSize(b.height() * 0.13f);
        canvas.drawText(category, b.centerX(), b.bottom - b.height() * 0.14f, cat);
    }
}
