package com.desire.widget.widget;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.CalendarContract;

import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Maps a widget to the app it should open when tapped on the home screen.
 *
 * <p>The action is inferred from the widget's name / category / preview style at install time and
 * stored as a short key (e.g. {@code "clock"}, {@code "ai:gemini"}). At render time the key is
 * resolved into a concrete launch {@link Intent}; if nothing matches or the target isn't installed
 * the caller falls back to opening this app.
 */
public final class WidgetActions {

    private WidgetActions() {}

    /** Computes the click-action key for a widget from its descriptive fields. */
    public static String inferActionKey(@Nullable String name, @Nullable String category, @Nullable String style) {
        String text = ((name == null ? "" : name) + " "
                + (category == null ? "" : category) + " "
                + (style == null ? "" : style)).toLowerCase(Locale.US);

        // AI assistants — order matters (most specific first).
        if (text.contains("chatgpt") || text.contains("openai") || text.contains("gpt")) return "ai:chatgpt";
        if (text.contains("gemini") || text.contains("bard")) return "ai:gemini";
        if (text.contains("claude")) return "ai:claude";
        if (text.contains("copilot")) return "ai:copilot";
        if (text.contains("perplexity")) return "ai:perplexity";
        if (text.contains("grok")) return "ai:grok";
        if (text.contains("deepseek")) return "ai:deepseek";
        if (text.contains(" ai") || text.startsWith("ai") || text.contains("assistant")) return "ai:chatgpt";

        if (text.contains("clock") || text.contains("alarm") || text.contains("world clock")
                || text.contains("timer") || text.contains("stopwatch") || text.contains("countdown")) return "clock";
        if (text.contains("calendar") || text.contains("date") || text.contains("agenda")
                || text.contains("schedule")) return "calendar";
        if (text.contains("weather") || text.contains("forecast")) return "weather";
        if (text.contains("music") || text.contains("player") || text.contains("song")
                || text.contains("spotify")) return "music";
        if (text.contains("calculator") || text.contains("calc")) return "calculator";
        if (text.contains("battery")) return "battery";
        if (text.contains("photo") || text.contains("gallery") || text.contains("album")) return "gallery";
        if (text.contains("step") || text.contains("activit") || text.contains("health")
                || text.contains("habit") || text.contains("fitness")) return "health";
        if (text.contains("note") || text.contains("remind")) return "notes";
        if (text.contains("crypto") || text.contains("bitcoin") || text.contains("finance")
                || text.contains("stock") || text.contains("ticker")) return "finance";
        if (text.contains("launcher") || text.contains("app grid")) return "launcher";

        return "";
    }

    /**
     * Resolves an action key into a launchable Intent, or {@code null} if it can't be resolved
     * on this device (caller should fall back to opening the app itself).
     */
    @Nullable
    public static Intent intentForAction(Context context, @Nullable String key) {
        if (key == null || key.isEmpty()) return null;

        Intent intent = buildIntent(key);
        if (intent == null) return null;
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            return intent;
        }

        // AI widgets fall back to opening the assistant on the web.
        if (key.startsWith("ai:")) {
            Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse(aiWebUrl(key)));
            web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (web.resolveActivity(context.getPackageManager()) != null) return web;
        }
        return null;
    }

    @Nullable
    private static Intent buildIntent(String key) {
        switch (key) {
            case "clock":
                return new Intent(AlarmClock.ACTION_SHOW_ALARMS);
            case "calendar": {
                Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time");
                ContentUris.appendId(builder, System.currentTimeMillis());
                return new Intent(Intent.ACTION_VIEW).setData(builder.build());
            }
            case "weather": {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=weather"));
                return i;
            }
            case "music":
                return new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC);
            case "calculator":
                return new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR);
            case "gallery":
                return new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_GALLERY);
            case "battery":
                return new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
            case "health":
                return new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/search?q=my+health+steps+today"));
            case "notes":
                return new Intent(Intent.ACTION_VIEW,
                        Uri.parse("content://com.google.android.keep"))
                        .setType("text/plain");
            case "finance":
                return new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://finance.yahoo.com/crypto/"));
            case "launcher":
                return null; // no-op: tapping an app launcher widget opens the individual apps
            default:
                if (key.startsWith("ai:")) {
                    return new Intent(Intent.ACTION_VIEW, Uri.parse(aiAppUri(key)));
                }
                if (key.startsWith("app:")) {
                    // Plain package launch handled by caller via getLaunchIntentForPackage.
                    return null;
                }
                if (key.startsWith("url:")) {
                    return new Intent(Intent.ACTION_VIEW, Uri.parse(key.substring(4)));
                }
                return null;
        }
    }

    private static String aiAppUri(String key) {
        // Deep links / sites that the respective apps register to handle.
        switch (key) {
            case "ai:chatgpt": return "https://chatgpt.com/";
            case "ai:gemini": return "https://gemini.google.com/app";
            case "ai:claude": return "https://claude.ai/new";
            case "ai:copilot": return "https://copilot.microsoft.com/";
            case "ai:perplexity": return "https://www.perplexity.ai/";
            case "ai:grok": return "https://grok.com/";
            case "ai:deepseek": return "https://chat.deepseek.com/";
            default: return "https://chatgpt.com/";
        }
    }

    private static String aiWebUrl(String key) {
        return aiAppUri(key);
    }
}
