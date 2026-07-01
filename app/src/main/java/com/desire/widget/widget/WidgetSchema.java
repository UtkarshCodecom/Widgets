package com.desire.widget.widget;

import android.content.ComponentName;
import android.content.Context;

import java.util.Locale;

public final class WidgetSchema {
    public static final String SIZE_1X1 = "1x1";
    public static final String SIZE_2X2 = "2x2";
    public static final String SIZE_2X1 = "2x1";
    public static final String SIZE_4X1 = "4x1";
    public static final String SIZE_4X2 = "4x2";

    public static final String STYLE_ICON = "icon";
    public static final String STYLE_FOLDER = "folder";
    public static final String STYLE_AI_BAR = "ai_bar";
    public static final String STYLE_CLOCK_DIGITAL = "clock_digital";
    public static final String STYLE_CLOCK_ANALOG = "clock_analog";

    public static final String[] SIZE_OPTIONS = {
            SIZE_1X1,
            SIZE_2X2,
            SIZE_2X1,
            SIZE_4X1,
            SIZE_4X2
    };

    public static final String[] STYLE_OPTIONS = {
            STYLE_ICON,
            STYLE_FOLDER,
            STYLE_AI_BAR,
            STYLE_CLOCK_DIGITAL,
            STYLE_CLOCK_ANALOG
    };

    private WidgetSchema() {}

    public static String normalizeSize(String value) {
        if (value == null) return SIZE_2X2;
        String normalized = value.trim().toLowerCase(Locale.US).replace(" ", "");
        switch (normalized) {
            case SIZE_1X1:
            case SIZE_2X2:
            case SIZE_2X1:
            case SIZE_4X1:
            case SIZE_4X2:
                return normalized;
            case "1x2":
                return SIZE_2X1;
            case "1x4":
                return SIZE_4X1;
            case "2x4":
                return SIZE_4X2;
            default:
                return SIZE_2X2;
        }
    }

    public static String normalizeStyle(String value) {
        if (value == null) return STYLE_ICON;
        String normalized = value.trim().toLowerCase(Locale.US).replace(" ", "_");
        for (String option : STYLE_OPTIONS) {
            if (option.equals(normalized)) return normalized;
        }
        return STYLE_ICON;
    }

    public static boolean isWide(String size) {
        String normalized = normalizeSize(size);
        return SIZE_2X1.equals(normalized) || SIZE_4X1.equals(normalized) || SIZE_4X2.equals(normalized);
    }

    /**
     * Aspect ratio for preview cards.
     * Returns {widthUnits, heightUnits} — e.g. {4,1} means 4:1 landscape.
     */
    public static int[] previewRatio(String size) {
        String normalized = normalizeSize(size);
        if (SIZE_4X2.equals(normalized)) return new int[]{4, 2};
        if (SIZE_4X1.equals(normalized)) return new int[]{4, 1};
        if (SIZE_2X1.equals(normalized)) return new int[]{4, 1}; // spans full width like 4x1
        if (SIZE_1X1.equals(normalized)) return new int[]{1, 1};
        return new int[]{1, 1}; // 2x2 — square in its half-width column
    }

    public static int previewHeightDp(String size) {
        String normalized = normalizeSize(size);
        // 4x2 at full card width ≈ 360dp → height = 360/2 = 180
        if (SIZE_4X2.equals(normalized)) return 180;
        // 4x1 / 2x1 at full card width → height = 360/4 = 90, add padding = 100
        if (SIZE_4X1.equals(normalized) || SIZE_2X1.equals(normalized)) return 100;
        // 1x1 at half card width ≈ 160dp → height = 160
        if (SIZE_1X1.equals(normalized)) return 155;
        // 2x2 at half card width ≈ 160dp → height = 160 (square)
        return 155;
    }

    public static int renderWidthPx(String size) {
        String normalized = normalizeSize(size);
        if (SIZE_4X1.equals(normalized) || SIZE_4X2.equals(normalized)) return 1024;
        if (SIZE_2X1.equals(normalized)) return 720;
        return 512;
    }

    public static int renderHeightPx(String size) {
        String normalized = normalizeSize(size);
        if (SIZE_2X1.equals(normalized) || SIZE_4X1.equals(normalized)) return 256;
        return 512;
    }

    public static ComponentName providerComponent(Context context, String size) {
        String normalized = normalizeSize(size);
        if (SIZE_2X1.equals(normalized)) return new ComponentName(context, WidgetProvider2x1.class);
        if (SIZE_4X1.equals(normalized)) return new ComponentName(context, WidgetProvider4x1.class);
        if (SIZE_4X2.equals(normalized)) return new ComponentName(context, WidgetProvider4x2.class);
        return new ComponentName(context, WidgetProvider.class);
    }

    public static boolean isCompleteHtml(String html) {
        if (html == null) return false;
        String normalized = html.trim().toLowerCase(Locale.US);
        return normalized.contains("<!doctype")
                && normalized.contains("<html")
                && normalized.contains("<head")
                && normalized.contains("<body")
                && normalized.contains("</html>");
    }

    public static boolean hasVisibleSurface(String html) {
        if (html == null) return false;
        String normalized = html.toLowerCase(Locale.US);
        String compact = normalized.replace(" ", "");
        boolean hasSizing = compact.contains("100vw")
                || compact.contains("100vh")
                || compact.contains("width:100%")
                || compact.contains("height:100%");
        boolean hasVisiblePaint = normalized.contains("background")
                || normalized.contains("<img")
                || normalized.contains("<canvas")
                || normalized.contains("box-shadow")
                || normalized.contains("border:");
        return hasSizing && hasVisiblePaint;
    }

    public static String htmlValidationError(String html) {
        if (html == null || html.trim().isEmpty()) {
            return "Widget HTML is required";
        }
        if (!isCompleteHtml(html)) {
            return "Paste a complete HTML document with <!doctype>, <html>, <head>, <body>, and </html>";
        }
        if (!hasVisibleSurface(html)) {
            return "HTML must define a full-size visible surface, for example width/height 100% or 100vw/100vh plus a background";
        }
        return null;
    }

    public static String documentForWebView(String html) {
        String error = htmlValidationError(html);
        if (error != null) return errorHtml(error);
        return html;
    }

    public static String errorHtml(String message) {
        String safeMessage = escape(message == null ? "Widget render failed" : message);
        return "<!doctype html><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=1\">"
                + "<style>html,body{margin:0;width:100%;height:100%;overflow:hidden;background:#000;}body{display:flex;align-items:center;justify-content:center;color:#ffd700;font:700 18px sans-serif;text-align:center;padding:20px;box-sizing:border-box;}</style>"
                + "</head><body>" + safeMessage + "</body></html>";
    }

    public static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
