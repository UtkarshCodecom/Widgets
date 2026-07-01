package com.desire.widget.engine.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.desire.widget.engine.ComponentRenderer;
import com.desire.widget.engine.RenderContext;
import com.desire.widget.engine.model.ComponentSpec;
import com.desire.widget.engine.util.SpecColors;
import com.desire.widget.engine.util.SpecProps;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.EnumMap;
import java.util.Map;

/**
 * Encodes {@code data} into a QR matrix with ZXing and draws the modules as squares, centered and
 * scaled to the bounds. props: data, foreground, background, quietZone (modules), errorCorrection
 * (L|M|Q|H).
 */
public class QrCodeRenderer implements ComponentRenderer {
    @Override
    public String type() {
        return "qr_code";
    }

    @Override
    public void render(Canvas canvas, RectF b, ComponentSpec s, RenderContext ctx) {
        Map<String, Object> props = s.props;
        String data = SpecProps.str(props, "data", "");
        int fg = SpecColors.resolve(SpecProps.str(props, "foreground", "@textPrimary"), ctx.theme, 0xFF000000);
        int bg = SpecColors.resolve(SpecProps.str(props, "background", ""), ctx.theme, 0);
        int quiet = SpecProps.i(props, "quietZone", 1);

        float side = Math.min(b.width(), b.height());
        float left = b.centerX() - side / 2f;
        float top = b.centerY() - side / 2f;

        if (bg != 0) {
            Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setColor(bg);
            canvas.drawRect(left, top, left + side, top + side, bgPaint);
        }
        if (data.isEmpty()) return;

        BitMatrix matrix = encode(data, props, quiet);
        if (matrix == null) return;

        int dim = matrix.getWidth();
        float cell = side / dim;
        Paint module = new Paint();
        module.setColor(fg);
        module.setAntiAlias(false);
        for (int y = 0; y < dim; y++) {
            for (int x = 0; x < dim; x++) {
                if (matrix.get(x, y)) {
                    float px = left + x * cell;
                    float py = top + y * cell;
                    // +1 to avoid hairline gaps between modules from float rounding
                    canvas.drawRect(px, py, px + cell + 1f, py + cell + 1f, module);
                }
            }
        }
    }

    private BitMatrix encode(String data, Map<String, Object> props, int quiet) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, Math.max(0, quiet));
            hints.put(EncodeHintType.ERROR_CORRECTION, ecLevel(SpecProps.str(props, "errorCorrection", "M")));
            return new QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, 0, 0, hints);
        } catch (Exception e) {
            return null;
        }
    }

    private ErrorCorrectionLevel ecLevel(String s) {
        if (s == null) return ErrorCorrectionLevel.M;
        switch (s.toUpperCase()) {
            case "L": return ErrorCorrectionLevel.L;
            case "Q": return ErrorCorrectionLevel.Q;
            case "H": return ErrorCorrectionLevel.H;
            default: return ErrorCorrectionLevel.M;
        }
    }
}
