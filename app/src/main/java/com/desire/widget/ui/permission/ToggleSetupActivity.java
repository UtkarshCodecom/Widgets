package com.desire.widget.ui.permission;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.desire.widget.engine.data.ToggleState;
import com.desire.widget.service.ToggleReceiver;

/**
 * One-time setup for a quick toggle that needs a special permission (auto-rotate / brightness →
 * "Modify system settings", Do Not Disturb / Silent → "Do Not Disturb access", Bluetooth →
 * "Nearby devices"). Instead of dropping the user on a bare Settings page, it explains what's
 * needed in a small sheet, sends them to the exact switch, and — as soon as they come back with
 * it granted — flips the toggle they tapped. Every later tap on the widget is then instant.
 *
 * <p>Not noHistory: it must stay alive behind the Settings screen / permission dialog.
 */
public class ToggleSetupActivity extends Activity {
    private static final int REQ_NEARBY = 41;

    private String kind;
    private boolean sentToSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        kind = getIntent().getStringExtra(ToggleReceiver.EXTRA_KIND);
        if (kind == null) {
            finish();
            return;
        }
        if (ToggleReceiver.need(this, kind) == ToggleReceiver.Need.NONE) {
            completeAndFinish(); // granted in the meantime (e.g. a stale widget intent)
            return;
        }
        setContentView(buildSheet());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sentToSettings && ToggleReceiver.need(this, kind) == ToggleReceiver.Need.NONE) {
            completeAndFinish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_NEARBY) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            completeAndFinish();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT)) {
            // "Don't ask again" — the only way left is the app's settings page.
            sentToSettings = true;
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
            Toast.makeText(this, "Turn on “Nearby devices” under Permissions", Toast.LENGTH_LONG).show();
        }
    }

    private void allow() {
        switch (ToggleReceiver.need(this, kind)) {
            case WRITE_SETTINGS:
                sentToSettings = true;
                startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:" + getPackageName())));
                break;
            case DND_ACCESS:
                sentToSettings = true;
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
                Toast.makeText(this, "Find “Widgets” and allow access", Toast.LENGTH_LONG).show();
                break;
            case NEARBY_DEVICES:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQ_NEARBY);
                }
                break;
            default:
                completeAndFinish();
                break;
        }
    }

    /** Permission is in place: flip the toggle the user originally tapped, then get out of the way. */
    private void completeAndFinish() {
        if (ToggleReceiver.route(this, kind) == ToggleReceiver.Route.DIRECT) {
            String result = ToggleReceiver.perform(this, kind);
            if (result != null) Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
        } else if (ToggleState.BLUETOOTH.equals(kind)) {
            // Android 13+: apps can't switch Bluetooth themselves; the system's small
            // "Turn on Bluetooth?" dialog is the one-tap way.
            try {
                startActivity(ToggleReceiver.systemIntent(this, kind));
            } catch (Exception ignored) {}
        }
        // Re-render so the widget's tiles now point straight at the direct toggle.
        ToggleReceiver.renderToggleWidgets(getApplicationContext());
        finish();
    }

    // ------------------------------------------------------------------ UI

    private View buildSheet() {
        FrameLayout scrim = new FrameLayout(this);
        scrim.setBackgroundColor(0x99000000);
        scrim.setOnClickListener(v -> finish());

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(24), dp(26), dp(24), dp(20));
        card.setClickable(true); // taps on the card must not dismiss
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF1D1F24);
        bg.setCornerRadius(dp(28));
        card.setBackground(bg);

        int accent = 0xFF6C7BFF;
        ImageView icon = new ImageView(this);
        Drawable d = ContextCompat.getDrawable(this, ToggleState.icon(this, kind));
        if (d != null) {
            d = d.mutate();
            d.setTint(Color.WHITE);
            icon.setImageDrawable(d);
        }
        icon.setPadding(dp(14), dp(14), dp(14), dp(14));
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(accent);
        icon.setBackground(iconBg);
        card.addView(icon, new LinearLayout.LayoutParams(dp(60), dp(60)));

        TextView title = new TextView(this);
        title.setText(titleText());
        title.setTextColor(0xFFECEDF0);
        title.setTextSize(19f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = wrap();
        tlp.topMargin = dp(16);
        card.addView(title, tlp);

        TextView body = new TextView(this);
        body.setText(bodyText());
        body.setTextColor(0xFFA6ABB5);
        body.setTextSize(14.5f);
        body.setGravity(Gravity.CENTER);
        body.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams blp = wrap();
        blp.topMargin = dp(8);
        card.addView(body, blp);

        TextView allow = button("Allow", accent, Color.WHITE);
        allow.setOnClickListener(v -> allow());
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        alp.topMargin = dp(22);
        card.addView(allow, alp);

        TextView later = button("Not now", Color.TRANSPARENT, 0xFFA6ABB5);
        later.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        llp.topMargin = dp(4);
        card.addView(later, llp);

        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        clp.setMargins(dp(12), 0, dp(12), dp(28));
        scrim.addView(card, clp);
        return scrim;
    }

    private String titleText() {
        String name;
        switch (kind) {
            case ToggleState.ROTATE: name = "Auto-rotate"; break;
            case ToggleState.AUTOBRIGHT: name = "Auto-brightness"; break;
            case ToggleState.DND: name = "Do Not Disturb"; break;
            case ToggleState.SOUND: name = "Sound modes"; break;
            case ToggleState.BLUETOOTH: name = "Bluetooth"; break;
            default: name = ToggleState.label(kind); break;
        }
        return "Control " + name + " from your home screen";
    }

    private String bodyText() {
        switch (ToggleReceiver.need(this, kind)) {
            case WRITE_SETTINGS:
                return "Allow “Modify system settings” for Widgets once. After that, every tap on "
                        + "this tile switches it instantly — no Settings screen.";
            case DND_ACCESS:
                return "Allow “Do Not Disturb access” for Widgets once. After that, the tile "
                        + "switches Do Not Disturb and Silent mode instantly.";
            case NEARBY_DEVICES:
                return "Allow “Nearby devices” once so the tile can see Bluetooth and turn it on "
                        + "with a single tap.";
            default:
                return "";
        }
    }

    private TextView button(String text, int fill, int textColor) {
        TextView b = new TextView(this);
        b.setText(text);
        b.setTextColor(textColor);
        b.setTextSize(16f);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(25));
        b.setBackground(g);
        return b;
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
