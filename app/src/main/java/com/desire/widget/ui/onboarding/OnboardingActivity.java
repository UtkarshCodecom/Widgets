package com.desire.widget.ui.onboarding;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.desire.widget.R;
import com.desire.widget.service.MediaListenerHelper;
import com.desire.widget.util.PreferenceManager;
import com.desire.widget.util.ThemeManager;

/**
 * First-run walkthrough: explains the browse → customize → add-to-home flow and offers to enable
 * now-playing access. Shown once (guarded by {@link PreferenceManager#isFirstLaunch()}).
 */
public class OnboardingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeManager.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        findViewById(R.id.ob_media).setOnClickListener(v -> MediaListenerHelper.openSettings(this));
        findViewById(R.id.ob_start).setOnClickListener(v -> {
            PreferenceManager.getInstance(this).setFirstLaunch(false);
            finish();
        });
    }
}
