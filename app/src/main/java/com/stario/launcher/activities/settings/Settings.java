/*
 * Copyright (C) 2025 Răzvan Albu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */

package com.stario.launcher.activities.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.stario.launcher.BuildConfig;
import com.stario.launcher.R;
import com.stario.launcher.activities.settings.dialogs.AccessibilityConfigurator;
import com.stario.launcher.activities.settings.dialogs.NotificationConfigurator;
import com.stario.launcher.activities.settings.dialogs.hide.HideApplicationsDialog;
import com.stario.launcher.activities.settings.dialogs.icons.IconsDialog;
import com.stario.launcher.activities.settings.dialogs.license.LicensesDialog;
import com.stario.launcher.activities.settings.dialogs.search.engine.SearchEngineDialog;
import com.stario.launcher.activities.settings.dialogs.search.results.SearchResultsDialog;
import com.stario.launcher.apps.IconPackManager;
import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.apps.ProfileManager;
import com.stario.launcher.glance.extensions.media.Media;
import com.stario.launcher.glance.extensions.weather.Weather;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.sheet.drawer.search.SearchEngine;
import com.stario.launcher.sheet.drawer.search.recyclers.adapters.WebAdapter;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.common.CollapsibleTitleBar;
import com.stario.launcher.ui.common.lock.LockDetector;
import com.stario.launcher.ui.utils.HomeWatcher;
import com.stario.launcher.utils.Utils;

public class Settings extends ThemedActivity {
    private CollapsibleTitleBar titleBar;
    private View lockAnimSwitchContainer;
    private MaterialSwitch lockAnimSwitch;
    private MaterialSwitch mediaSwitch;
    private MaterialSwitch lockSwitch;
    private TextView searchEngineName;
    private SharedPreferences search;
    private HomeWatcher homeWatcher;
    private boolean shouldRebirth;
    private TextView iconPackName;
    private TextView hideCount;
    private View searchEngine;

    public Settings() {
        super();

        this.shouldRebirth = false;
    }

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        postponeEnterTransition();

        homeWatcher = new HomeWatcher(this);
        homeWatcher.setOnHomePressedListener(this::finishAfterTransition);

        SharedPreferences settings = getSettings();
        search = getSharedPreferences(Entry.SEARCH);

        boolean mediaAllowed = settings.getBoolean(Media.PREFERENCE_ENTRY, false);
        boolean lock = settings.getBoolean(LockDetector.PREFERENCE_ENTRY, false);
        boolean legacyLockAnim = settings.getBoolean(LockDetector.LEGACY_ANIMATION, false);
        boolean imperialUnits = settings.getBoolean(Weather.IMPERIAL_KEY, false);
        boolean searchResults = search.getBoolean(WebAdapter.SEARCH_RESULTS, false);
        boolean vibrations = settings.getBoolean(Vibrations.PREFERENCE_ENTRY, true);

        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        onBackPressed();
                    }
                });

        mediaSwitch = findViewById(R.id.media);
        lockSwitch = findViewById(R.id.lock);
        lockAnimSwitch = findViewById(R.id.lock_animation);
        MaterialSwitch imperialSwitch = findViewById(R.id.imperial);
        MaterialSwitch searchResultsSwitch = findViewById(R.id.search_results);
        MaterialSwitch switchVibrations = findViewById(R.id.vibrations);

        lockAnimSwitchContainer = findViewById(R.id.lock_animation_container);
        View fader = findViewById(R.id.fader);

        searchEngine = findViewById(R.id.search_engine);
        searchEngineName = findViewById(R.id.engine_name);
        iconPackName = findViewById(R.id.pack_name);
        hideCount = findViewById(R.id.hidden_count);

        updateIconPackName();
        updateHiddenAppsCount();
        updateEngineName();

        titleBar = findViewById(R.id.title_bar);
        titleBar.setOnOffsetChangeListener(offset ->
                fader.setTranslationY(titleBar.getMeasuredHeight() - titleBar.getCollapsedHeight() + offset));

        mediaSwitch.setChecked(Utils.isNotificationServiceEnabled(this) && mediaAllowed);
        lockSwitch.setChecked(Utils.isAccessibilityServiceEnabled(this) && lock);
        imperialSwitch.setChecked(imperialUnits);
        searchResultsSwitch.setChecked(searchResults);
        lockAnimSwitch.setChecked(legacyLockAnim);
        switchVibrations.setChecked(vibrations);

        lockSwitch.jumpDrawablesToCurrentState();
        mediaSwitch.jumpDrawablesToCurrentState();
        imperialSwitch.jumpDrawablesToCurrentState();
        searchResultsSwitch.jumpDrawablesToCurrentState();
        lockAnimSwitch.jumpDrawablesToCurrentState();
        switchVibrations.jumpDrawablesToCurrentState();

        mediaSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            private NotificationConfigurator dialog;

            @Override
            public void onCheckedChanged(CompoundButton compound, boolean isChecked) {
                if (isChecked && !Utils.isNotificationServiceEnabled(Settings.this)) {
                    if (dialog == null || !Settings.this.equals(dialog.getContext())) {
                        dialog = new NotificationConfigurator(Settings.this);

                        dialog.setOnDismissListener(dialog -> checkNotificationPermission());
                    }

                    dialog.show();
                }

                settings.edit()
                        .putBoolean(Media.PREFERENCE_ENTRY, isChecked)
                        .apply();
            }
        });

        lockSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            private AccessibilityConfigurator dialog;

            @Override
            public void onCheckedChanged(CompoundButton compound, boolean isChecked) {
                if (isChecked && !Utils.isAccessibilityServiceEnabled(Settings.this)) {
                    if (dialog == null || !Settings.this.equals(dialog.getContext())) {
                        dialog = new AccessibilityConfigurator(Settings.this);

                        dialog.setOnDismissListener(dialog -> checkAccessibilityPermission());
                    }

                    dialog.show();
                }

                settings.edit()
                        .putBoolean(LockDetector.PREFERENCE_ENTRY, isChecked)
                        .apply();

                updateLockAnimationState(lockSwitch.isChecked());
            }
        });

        imperialSwitch.setOnCheckedChangeListener((compound, isChecked) -> {
            settings.edit()
                    .putBoolean(Weather.IMPERIAL_KEY, isChecked)
                    .apply();
        });

        searchResultsSwitch.setOnCheckedChangeListener((compound, isChecked) -> {
            search.edit()
                    .putBoolean(WebAdapter.SEARCH_RESULTS, isChecked)
                    .apply();

            updateEngineName();
        });

        lockAnimSwitch.setOnCheckedChangeListener((compound, isChecked) -> {
            settings.edit()
                    .putBoolean(LockDetector.LEGACY_ANIMATION, isChecked)
                    .apply();
        });

        switchVibrations.setOnCheckedChangeListener((compound, isChecked) -> {
            settings.edit()
                    .putBoolean(Vibrations.PREFERENCE_ENTRY, isChecked)
                    .apply();
        });

        searchEngine.setOnClickListener(new View.OnClickListener() {
            private SearchEngineDialog dialog;

            @Override
            public void onClick(View view) {
                if (dialog == null || !Settings.this.equals(dialog.getContext())) {
                    dialog = new SearchEngineDialog(Settings.this);

                    dialog.setOnDismissListener(dialog ->
                            updateEngineName());
                }

                dialog.show();
            }
        });

        findViewById(R.id.search_results_container).setOnClickListener(new View.OnClickListener() {
            private SearchResultsDialog dialog;

            @Override
            public void onClick(View view) {
                if (dialog == null || !Settings.this.equals(dialog.getContext())) {
                    dialog = new SearchResultsDialog(Settings.this);

                    dialog.setStatusListener(searchResultsSwitch::setChecked);
                }

                dialog.show();
            }
        });

        findViewById(R.id.hidden_apps).setOnClickListener(new View.OnClickListener() {
            private HideApplicationsDialog dialog;

            @Override
            public void onClick(View view) {
                if (dialog == null || !Settings.this.equals(dialog.getContext())) {
                    dialog = new HideApplicationsDialog(Settings.this);

                    dialog.setOnDismissListener(dialog ->
                            updateHiddenAppsCount());
                }

                dialog.show();
            }
        });

        findViewById(R.id.icons).setOnClickListener(new View.OnClickListener() {
            private IconsDialog dialog;

            @Override
            public void onClick(View view) {
                if (dialog == null || !Settings.this.equals(dialog.getContext())) {
                    dialog = new IconsDialog(Settings.this);

                    dialog.setOnDismissListener(dialog ->
                            updateIconPackName());
                }

                dialog.show();
            }
        });

        findViewById(R.id.licenses).setOnClickListener(new View.OnClickListener() {
            private LicensesDialog dialog;

            @Override
            public void onClick(View view) {
                if (dialog == null || !Settings.this.equals(dialog.getContext())) {
                    dialog = new LicensesDialog(Settings.this);
                }

                dialog.show();
            }
        });

        findViewById(R.id.restart).setOnClickListener(view -> {
            shouldRebirth = true;
            finishAfterTransition();
        });

        findViewById(R.id.def_launcher).setOnClickListener((view) -> {
            if (Utils.isMinimumSDK(Build.VERSION_CODES.Q)) {
                RoleManager roleManager = getApplicationContext().getSystemService(RoleManager.class);

                if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    if (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                        Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME);

                        activityResultLauncher.launch(intent, ActivityOptionsCompat.makeBasic());
                    } else {
                        Intent intent = new Intent(android.provider.Settings.ACTION_HOME_SETTINGS);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }
            } else {
                Intent intent = new Intent(android.provider.Settings.ACTION_HOME_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        findViewById(R.id.about).setOnClickListener((view) -> {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            intent.setData(Uri.parse("package:" + getPackageName()));

            startActivity(intent);
        });

        ((TextView) findViewById(R.id.version)).setText(BuildConfig.VERSION_NAME + " • Răzvan Albu");

        findViewById(R.id.github).setOnClickListener((view) -> {
            view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce_small));
            startActivity(new Intent(Intent.ACTION_DEFAULT, Uri.parse("https://github.com/albu-razvan/Stario")));
        });

        findViewById(R.id.website).setOnClickListener((view) -> {
            view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce_small));
            startActivity(new Intent(Intent.ACTION_DEFAULT, Uri.parse("https://www.razvanalbu.com")));
        });

        findViewById(R.id.discord).setOnClickListener((view) -> {
            view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce_small));
            startActivity(new Intent(Intent.ACTION_DEFAULT, Uri.parse("https://discord.gg/WuVapMt9gY")));
        });

        // delegates
        findViewById(R.id.media_container).setOnClickListener((view) -> mediaSwitch.performClick());
        findViewById(R.id.lock_container).setOnClickListener((view) -> lockSwitch.performClick());
        findViewById(R.id.imperial_container).setOnClickListener((view) -> imperialSwitch.performClick());
        findViewById(R.id.vibrations_container).setOnClickListener((view) -> switchVibrations.performClick());
        updateLockAnimationState(lockSwitch.isChecked());

        getRoot().post(this::startPostponedEnterTransition);
    }

    private void updateLockAnimationState(boolean enabled) {
        lockAnimSwitchContainer.setAlpha(enabled ? 1f : 0.5f);

        if(enabled) {
            lockAnimSwitchContainer.setOnClickListener((view) -> lockAnimSwitch.performClick());
        } else {
            lockAnimSwitchContainer.setOnClickListener(null);
        }
    }

    private void updateEngineName() {
        searchEngineName.setText(SearchEngine.getEngine(this).toString());

        searchEngine.setEnabled(!search.getBoolean(WebAdapter.SEARCH_RESULTS, false));
        searchEngine.setAlpha(searchEngine.isEnabled() ? 1f : 0.6f);
    }

    private void updateIconPackName() {
        SharedPreferences iconPreferences = getSharedPreferences(Entry.ICONS);
        String packPackageName = iconPreferences.getString(IconPackManager.ICON_PACK_ENTRY, null);

        if (packPackageName != null) {
            LauncherApplication iconPackApplication = ProfileManager
                    .getInstance().getApplication(packPackageName);

            if (iconPackApplication != LauncherApplication.FALLBACK_APP) {
                iconPackName.setText(iconPackApplication.getLabel());

                return;
            }
        }

        iconPackName.setText(R.string.default_text);
    }

    @SuppressLint("SetTextI18n")
    private void updateHiddenAppsCount() {
        hideCount.setText(getResources().getString(R.string.hidden_apps) +
                ": " + getSharedPreferences(Entry.HIDDEN_APPS).getAll().size());
    }

    private void checkNotificationPermission() {
        if (!Utils.isNotificationServiceEnabled(this)) {
            mediaSwitch.setChecked(false);
        }
    }

    private void checkAccessibilityPermission() {
        if (!Utils.isAccessibilityServiceEnabled(this)) {
            lockSwitch.setChecked(false);
        }
    }

    @Override
    protected void onStart() {
        homeWatcher.startWatch();

        super.onStart();
    }

    @Override
    protected void onStop() {
        homeWatcher.stopWatch();

        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkNotificationPermission();
        checkAccessibilityPermission();

        if (Measurements.isLandscape()) {
            titleBar.getLayoutParams().height = Measurements.dpToPx(Measurements.HEADER_SIZE_DP / 3f);
            titleBar.requestLayout();
        } else {
            titleBar.getLayoutParams().height = Measurements.dpToPx(Measurements.HEADER_SIZE_DP);
            titleBar.requestLayout();
        }
    }

    @Override
    protected boolean isOpaque() {
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (shouldRebirth) {
            PackageManager packageManager = getPackageManager();
            Intent intent = packageManager.getLaunchIntentForPackage(BuildConfig.APPLICATION_ID);

            if (intent != null) {
                ComponentName componentName = intent.getComponent();
                Intent mainIntent = Intent.makeRestartActivityTask(componentName);
                mainIntent.setPackage(BuildConfig.APPLICATION_ID);

                startActivity(mainIntent);

                System.exit(0);
            }
        }
    }

    @Override
    protected boolean isAffectedByBackGesture() {
        return true;
    }
}
