package com.desire.widget.engine.action;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.desire.widget.engine.model.ActionSpec;
import com.desire.widget.service.MediaNotificationListener;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns a declarative {@link ActionSpec} into a {@link PendingIntent} ready to attach to a
 * RemoteViews click target. Package and class names travel in the spec (never hardcoded), so the
 * same widget definition is portable across devices and installs.
 *
 * <p>Returns {@code null} when the action is unrepresentable (e.g. launch_app for an app that is
 * not installed); callers fall back to opening the host app.
 */
public final class ActionCompiler {
    private static final String TAG = "ActionCompiler";

    private ActionCompiler() {}

    public static PendingIntent compile(Context context, int requestCode, ActionSpec action) {
        if (action == null || action.type == null) return null;
        Intent intent = buildIntent(context, action);
        if (intent == null) return null;

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        boolean broadcast = "broadcast".equals(action.type) || "toggle_setting".equals(action.type);
        if (broadcast) {
            return PendingIntent.getBroadcast(context, requestCode, intent, flags);
        }
        return PendingIntent.getActivity(context, requestCode, intent, flags);
    }

    /** Public: build the raw target Intent for an action (used by the tap trampoline). */
    public static Intent buildTargetIntent(Context context, ActionSpec a) {
        if (a == null || a.type == null) return null;
        return buildIntent(context, a);
    }

    private static Intent buildIntent(Context context, ActionSpec a) {
        switch (a.type) {
            case "launch_app": {
                if (a.packageName == null) return null;
                Intent launch = context.getPackageManager().getLaunchIntentForPackage(a.packageName);
                if (launch != null) launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                return launch; // null if not installed -> caller falls back
            }
            case "open_activity": {
                if (a.packageName == null || a.className == null) return null;
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(a.packageName, a.className));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                applyExtras(intent, a.extras);
                // The exact Activity class may not exist on this device (OEM clock apps differ) —
                // fall back to just launching the package so the tap still opens something.
                if (context.getPackageManager().resolveActivity(intent, 0) == null) {
                    Intent launch = context.getPackageManager().getLaunchIntentForPackage(a.packageName);
                    if (launch != null) launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    return launch;
                }
                return intent;
            }
            case "alarms": {
                // Prefer launching the clock app's MAIN activity — that reliably brings it to the
                // foreground. SHOW_ALARMS is the "nicer" intent but on some OEM builds (e.g. Vivo's
                // HandleApiCalls) it silently handles the call and finishes without showing the app.
                Intent byKw = findLaunchByKeywords(context, "deskclock", "clock", "alarm");
                if (byKw != null) { Log.i(TAG, "alarms -> keyword " + byKw.getPackage()); return byKw; }
                Intent pkg = firstLaunchable(context, CLOCK_PACKAGES);
                if (pkg != null) { Log.i(TAG, "alarms -> package " + pkg.getPackage()); return pkg; }
                Intent showAlarms = new Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS);
                showAlarms.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (resolves(context, showAlarms)) { Log.i(TAG, "alarms -> SHOW_ALARMS"); return showAlarms; }
                Log.w(TAG, "alarms -> NONE found");
                return null;
            }
            case "calendar": {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("content://com.android.calendar/time"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (resolves(context, intent)) return intent;
                Intent byKw = findLaunchByKeywords(context, "calendar", "agenda");
                if (byKw != null) return byKw;
                return firstLaunchable(context, CALENDAR_PACKAGES);
            }
            case "battery": {
                Intent intent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (context.getPackageManager().resolveActivity(intent, 0) == null) {
                    intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                return intent;
            }
            case "music": {
                // 1. If a media session is currently live, open the app that owns it — that is the
                // app "playing music" and is what the widget's transport controls talk to. Skip
                // video apps so a paused YouTube session doesn't hijack the music widget.
                String active = activeMediaPackage(context);
                if (active != null && !isVideoPackage(active)) {
                    Intent launch = context.getPackageManager().getLaunchIntentForPackage(active);
                    if (launch != null) {
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        Log.i(TAG, "music -> active session " + active);
                        return launch;
                    }
                }
                // 2. Find the installed music app by label/package, excluding video players.
                Intent byKw = findLaunch(context, VIDEO_KEYWORDS,
                        "music", "player", "audio", "spotify", "wynk", "gaana", "soundcloud");
                if (byKw != null) { Log.i(TAG, "music -> keyword " + byKw.getPackage()); return byKw; }
                // 3. Known music packages (incl. OEM), then the generic music-app selector.
                Intent pkg = firstLaunchable(context, MUSIC_PACKAGES);
                if (pkg != null) { Log.i(TAG, "music -> package " + pkg.getPackage()); return pkg; }
                Intent intent = Intent.makeMainSelectorActivity(
                        Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (resolves(context, intent)) { Log.i(TAG, "music -> APP_MUSIC selector"); return intent; }
                Log.w(TAG, "music -> NONE found");
                return null;
            }
            case "weather": {
                // Open the installed weather app if any; else a known OEM weather package; else the URL.
                Intent byKw = findLaunchByKeywords(context, "weather", "climate", "forecast");
                if (byKw != null) { Log.i(TAG, "weather -> keyword " + byKw.getPackage()); return byKw; }
                Intent pkg = firstLaunchable(context, WEATHER_PACKAGES);
                if (pkg != null) { Log.i(TAG, "weather -> package " + pkg.getPackage()); return pkg; }
                String url = (a.data != null && !a.data.isEmpty()) ? a.data : "https://weather.com";
                Log.i(TAG, "weather -> url " + url);
                Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                return web;
            }
            case "ai": {
                // Preferred AI app first, then any installed AI assistant, then the OS assistant.
                if (a.packageName != null) {
                    Intent launch = context.getPackageManager().getLaunchIntentForPackage(a.packageName);
                    if (launch != null) {
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        return launch;
                    }
                }
                Intent byKw = findLaunchByKeywords(context,
                        "chatgpt", "openai", "gemini", "bard", "copilot", "perplexity",
                        "claude", "grok", "deepseek", "assistant");
                if (byKw != null) return byKw;
                Intent assist = new Intent(Intent.ACTION_VOICE_COMMAND);
                assist.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (resolves(context, assist)) return assist;
                if (a.data != null && !a.data.isEmpty()) {
                    Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse(a.data));
                    web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    return web;
                }
                return null;
            }
            case "open_url": {
                if (a.data == null || a.data.isEmpty()) return null;
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(a.data));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                return intent;
            }
            case "toggle_setting": {
                Intent intent = new Intent(context, EngineActionReceiver.class);
                intent.setAction(EngineActionReceiver.ACTION_TOGGLE_SETTING);
                intent.putExtra(EngineActionReceiver.EXTRA_KEY, a.data);
                return intent;
            }
            case "broadcast": {
                if (a.data == null || a.data.isEmpty()) return null;
                Intent intent = new Intent(a.data);
                if (a.packageName != null) intent.setPackage(a.packageName);
                applyExtras(intent, a.extras);
                return intent;
            }
            default:
                return null;
        }
    }

    private static final String[] CLOCK_PACKAGES = {
            "com.google.android.deskclock", "com.android.deskclock",
            "com.sec.android.app.clockpackage", "com.oneplus.deskclock",
            "com.oplus.alarmclock", "com.coloros.alarmclock", "com.miui.clock",
            "com.hihonor.deskclock", "com.huawei.deskclock", "com.motorola.timeweatherwidget",
            "com.android.BBKClock", "com.vivo.deskclock", "com.iqoo.deskclock" // Vivo / iQOO
    };
    private static final String[] CALENDAR_PACKAGES = {
            "com.google.android.calendar", "com.android.calendar",
            "com.samsung.android.calendar", "com.miui.calendar",
            "com.bbk.calendar", "com.vivo.calendar" // Vivo / iQOO
    };
    private static final String[] MUSIC_PACKAGES = {
            "com.android.bbkmusic", "com.vivo.musicwidgetmix", // Vivo / iQOO first (user's default)
            "com.spotify.music", "com.google.android.apps.youtube.music",
            "com.google.android.music", "com.sec.android.app.music",
            "com.miui.player", "com.amazon.mp3"
    };
    private static final String[] WEATHER_PACKAGES = {
            "com.vivo.weather", "com.miui.weather2", "com.coloros.weather2",
            "com.oneplus.weather", "com.sec.android.daemonapp",
            "com.google.android.apps.weather", "com.google.android.apps.weatherradar"
    };
    /** Substrings that mark a launcher entry as a video (not music) app. */
    private static final String[] VIDEO_KEYWORDS = {"video", "movie", "netflix", "prime", "gallery"};

    private static boolean resolves(Context context, Intent intent) {
        try {
            return context.getPackageManager().resolveActivity(intent, 0) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Finds an installed, launchable app whose package or label contains one of the keywords, and
     * returns its launch intent. This is device-agnostic — it finds the user's actual clock / music
     * / weather / AI app regardless of the OEM package name. Needs the LAUNCHER {@code <queries>}.
     */
    private static Intent findLaunchByKeywords(Context context, String... keywords) {
        return findLaunch(context, null, keywords);
    }

    /** Like {@link #findLaunchByKeywords} but skips any launcher entry matching an exclude keyword. */
    private static Intent findLaunch(Context context, String[] exclude, String... keywords) {
        try {
            android.content.pm.PackageManager pm = context.getPackageManager();
            Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            for (android.content.pm.ResolveInfo ri : pm.queryIntentActivities(main, 0)) {
                if (ri.activityInfo == null) continue;
                String pkg = ri.activityInfo.packageName.toLowerCase(Locale.US);
                if (pkg.equals(context.getPackageName())) continue; // never our own app
                String label = "";
                try {
                    label = ri.loadLabel(pm).toString().toLowerCase(Locale.US);
                } catch (Exception ignored) {}
                if (matchesAny(pkg, label, exclude)) continue;
                if (matchesAny(pkg, label, keywords)) {
                    Intent launch = pm.getLaunchIntentForPackage(ri.activityInfo.packageName);
                    if (launch != null) {
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        return launch;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static boolean matchesAny(String pkg, String label, String[] keywords) {
        if (keywords == null) return false;
        for (String kw : keywords) {
            if (kw != null && (pkg.contains(kw) || label.contains(kw))) return true;
        }
        return false;
    }

    /** Package of the currently active media session, or null (needs notification access). */
    private static String activeMediaPackage(Context context) {
        try {
            MediaSessionManager msm =
                    (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (msm == null) return null;
            ComponentName listener = new ComponentName(context, MediaNotificationListener.class);
            List<MediaController> controllers = msm.getActiveSessions(listener);
            if (controllers != null && !controllers.isEmpty()) {
                return controllers.get(0).getPackageName();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static boolean isVideoPackage(String pkg) {
        if (pkg == null) return false;
        String p = pkg.toLowerCase(Locale.US);
        if (p.contains("music") || p.contains("audio")) return false; // e.g. YT Music
        for (String kw : VIDEO_KEYWORDS) {
            if (p.contains(kw)) return true;
        }
        return false;
    }

    private static Intent firstLaunchable(Context context, String[] packages) {
        for (String pkg : packages) {
            Intent launch = context.getPackageManager().getLaunchIntentForPackage(pkg);
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                return launch;
            }
        }
        return null;
    }

    private static void applyExtras(Intent intent, Map<String, String> extras) {
        if (extras == null) return;
        for (Map.Entry<String, String> e : extras.entrySet()) {
            intent.putExtra(e.getKey(), e.getValue());
        }
    }
}
