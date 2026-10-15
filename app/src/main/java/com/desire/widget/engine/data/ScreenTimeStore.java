package com.desire.widget.engine.data;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Process;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Today's screen time from {@link UsageStatsManager} events (needs the user to grant Usage access).
 * Foreground time is reconstructed from resume/pause events since midnight — the same approach
 * digital-wellbeing apps use — because daily aggregate buckets can include part of yesterday.
 * Launchers and System UI are excluded. Results are cached for a few minutes.
 */
public final class ScreenTimeStore {
    private static final long CACHE_MS = 3 * 60_000L;
    private static Result cached;
    private static long cachedAt;

    public static final class AppUsage {
        public final String packageName;
        public final String label;
        public final long millis;

        AppUsage(String packageName, String label, long millis) {
            this.packageName = packageName;
            this.label = label;
            this.millis = millis;
        }
    }

    public static final class Result {
        public final long totalMillis;
        public final List<AppUsage> top;

        Result(long totalMillis, List<AppUsage> top) {
            this.totalMillis = totalMillis;
            this.top = top;
        }
    }

    private ScreenTimeStore() {}

    public static boolean hasAccess(Context c) {
        try {
            AppOpsManager ops = (AppOpsManager) c.getSystemService(Context.APP_OPS_SERVICE);
            int mode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    ? ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.getPackageName())
                    : ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    /** Today's usage, or null without access. Cached for {@link #CACHE_MS}. */
    public static synchronized Result today(Context c) {
        if (!hasAccess(c)) return null;
        long now = System.currentTimeMillis();
        if (cached != null && now - cachedAt < CACHE_MS) return cached;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();

        UsageStatsManager usm = (UsageStatsManager) c.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return null;
        Set<String> excluded = excludedPackages(c);

        Map<String, Long> totals = new HashMap<>();
        Map<String, Long> resumedAt = new HashMap<>();
        UsageEvents events = usm.queryEvents(start, now);
        UsageEvents.Event e = new UsageEvents.Event();
        while (events != null && events.hasNextEvent()) {
            events.getNextEvent(e);
            String pkg = e.getPackageName();
            if (pkg == null || excluded.contains(pkg)) continue;
            int type = e.getEventType();
            if (type == UsageEvents.Event.MOVE_TO_FOREGROUND) {        // == ACTIVITY_RESUMED
                resumedAt.put(pkg, e.getTimeStamp());
            } else if (type == UsageEvents.Event.MOVE_TO_BACKGROUND) { // == ACTIVITY_PAUSED
                Long since = resumedAt.remove(pkg);
                if (since != null && e.getTimeStamp() > since) {
                    totals.put(pkg, totals.getOrDefault(pkg, 0L) + (e.getTimeStamp() - since));
                }
            }
        }
        // Anything still in the foreground counts up to now.
        for (Map.Entry<String, Long> open : resumedAt.entrySet()) {
            totals.put(open.getKey(), totals.getOrDefault(open.getKey(), 0L) + Math.max(0, now - open.getValue()));
        }

        long total = 0;
        List<AppUsage> list = new ArrayList<>();
        PackageManager pm = c.getPackageManager();
        for (Map.Entry<String, Long> t : totals.entrySet()) {
            if (t.getValue() < 60_000L) continue; // ignore sub-minute blips
            total += t.getValue();
            list.add(new AppUsage(t.getKey(), label(pm, t.getKey()), t.getValue()));
        }
        Collections.sort(list, (a, b) -> Long.compare(b.millis, a.millis));
        cached = new Result(total, list.size() > 5 ? new ArrayList<>(list.subList(0, 5)) : list);
        cachedAt = now;
        return cached;
    }

    /** Home-screen launchers and System UI aren't "screen time" the user cares about. */
    private static Set<String> excludedPackages(Context c) {
        Set<String> out = new HashSet<>();
        out.add("com.android.systemui");
        try {
            Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            for (ResolveInfo ri : c.getPackageManager().queryIntentActivities(home, 0)) {
                if (ri.activityInfo != null) out.add(ri.activityInfo.packageName);
            }
        } catch (Exception ignored) {}
        return out;
    }

    private static String label(PackageManager pm, String pkg) {
        try {
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception e) {
            String last = pkg.substring(pkg.lastIndexOf('.') + 1);
            return last.isEmpty() ? pkg : Character.toUpperCase(last.charAt(0)) + last.substring(1);
        }
    }

    public static String format(long millis) {
        long mins = millis / 60_000L;
        long h = mins / 60, m = mins % 60;
        if (h == 0) return m + "m";
        return m == 0 ? h + "h" : h + "h " + m + "m";
    }
}
