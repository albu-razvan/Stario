/*
    Copyright (C) 2024 Răzvan Albu

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.
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
import android.transition.Transition;
import android.transition.TransitionListenerAdapter;
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
import com.stario.launcher.activities.settings.dialogs.engine.EngineDialog;
import com.stario.launcher.activities.settings.dialogs.hide.HideApplicationsDialog;
import com.stario.launcher.activities.settings.dialogs.icons.IconsDialog;
import com.stario.launcher.activities.settings.dialogs.license.LicensesDialog;
import com.stario.launcher.glance.extensions.media.Media;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.sheet.drawer.apps.LauncherApplication;
import com.stario.launcher.sheet.drawer.search.SearchEngine;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.lock.LockDetector;
import com.stario.launcher.ui.measurements.Measurements;
import com.stario.launcher.utils.Utils;

public class Settings extends ThemedActivity {
    private MaterialSwitch mediaSwitch;
    private MaterialSwitch lockSwitch;

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        SharedPreferences settings = getSettings();
        SharedPreferences hiddenApps = getSharedPreferences(Entry.HIDDEN_APPS);

        boolean mediaAllowed = settings.getBoolean(Media.PREFERENCE_ENTRY, false);
        boolean lock = settings.getBoolean(LockDetector.PREFERENCE_ENTRY, false);
        boolean legacyLaunchAnim = settings.getBoolean(LauncherApplication.LEGACY_LAUNCH_ANIMATION, false);
        boolean legacyLockAnim = settings.getBoolean(LockDetector.LEGACY_ANIMATION, true);
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
        MaterialSwitch switchLaunchAnim = findViewById(R.id.lock_animations);
        MaterialSwitch switchLockAnim = findViewById(R.id.lock_animation);
        MaterialSwitch switchVibrations = findViewById(R.id.vibrations);

        TextView searchEngineName = findViewById(R.id.engine_name);
        View close = findViewById(R.id.close);
        TextView hideCount = findViewById(R.id.hidden_count);

        hideCount.setText(getResources().getString(R.string.hidden_apps) +
                ": " + hiddenApps.getAll().size());
        searchEngineName.setText(SearchEngine.engineFor(this).toString());

        close.setMinimumHeight(Measurements.dpToPx(Measurements.HEADER_SIZE_DP));
        close.setOnClickListener((view) -> onBackPressed());

        mediaSwitch.setChecked(Utils.isNotificationServiceEnabled(this) && mediaAllowed);
        lockSwitch.setChecked(Utils.isAccessibilityServiceEnabled(this) && lock);
        switchLockAnim.setChecked(legacyLockAnim);
        switchLaunchAnim.setChecked(legacyLaunchAnim);
        switchVibrations.setChecked(vibrations);

        lockSwitch.jumpDrawablesToCurrentState();
        mediaSwitch.jumpDrawablesToCurrentState();
        switchLockAnim.jumpDrawablesToCurrentState();
        switchLaunchAnim.jumpDrawablesToCurrentState();
        switchVibrations.jumpDrawablesToCurrentState();

        switchLockAnim.setVisibility(lockSwitch.isChecked() ? View.VISIBLE : View.GONE);

        mediaSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            private NotificationConfigurator dialog;

            @Override
            public void onCheckedChanged(CompoundButton compound, boolean isChecked) {
                if (isChecked && !Utils.isNotificationServiceEnabled(Settings.this)) {
                    if (dialog == null) {
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
                    if (dialog == null) {
                        dialog = new AccessibilityConfigurator(Settings.this);

                        dialog.setOnDismissListener(dialog -> checkAccessibilityPermission());
                    }

                    dialog.show();
                }

                settings.edit()
                        .putBoolean(LockDetector.PREFERENCE_ENTRY, isChecked)
                        .apply();

                switchLockAnim.setVisibility(lockSwitch.isChecked() ? View.VISIBLE : View.GONE);
            }
        });

        switchLockAnim.setOnCheckedChangeListener((compound, isChecked) -> {
            settings.edit()
                    .putBoolean(LockDetector.LEGACY_ANIMATION, isChecked)
                    .apply();
        });

        switchLaunchAnim.setOnCheckedChangeListener((compound, isChecked) -> {
            settings.edit()
                    .putBoolean(LauncherApplication.LEGACY_LAUNCH_ANIMATION, isChecked)
                    .apply();
        });

        switchVibrations.setOnCheckedChangeListener((compound, isChecked) -> {
            settings.edit()
                    .putBoolean(Vibrations.PREFERENCE_ENTRY, isChecked)
                    .apply();
        });

        findViewById(R.id.search_engine).setOnClickListener(new View.OnClickListener() {
            private EngineDialog dialog;

            @Override
            public void onClick(View view) {
                if (dialog == null) {
                    dialog = new EngineDialog(Settings.this);

                    dialog.setOnDismissListener(dialog ->
                            searchEngineName.setText(SearchEngine.engineFor(Settings.this).toString()));
                }

                dialog.show();
            }
        });

        findViewById(R.id.hidden_apps).setOnClickListener(new View.OnClickListener() {
            private HideApplicationsDialog dialog;

            @Override
            public void onClick(View view) {
                if (dialog == null) {
                    dialog = new HideApplicationsDialog(Settings.this);

                    dialog.setOnDismissListener(dialog ->
                            hideCount.setText(getResources().getString(R.string.hidden_apps) +
                                    ": " + hiddenApps.getAll().size()));
                }

                dialog.show();
            }
        });

        findViewById(R.id.icons).setOnClickListener(new View.OnClickListener() {
            private IconsDialog dialog;

            @Override
            public void onClick(View view) {
                if (dialog == null) {
                    dialog = new IconsDialog(Settings.this);
                }

                dialog.show();
            }
        });

        findViewById(R.id.licenses).setOnClickListener(new View.OnClickListener() {
            private LicensesDialog dialog;

            @Override
            public void onClick(View view) {
                if (dialog == null) {
                    dialog = new LicensesDialog(Settings.this);
                }

                dialog.show();
            }
        });

        findViewById(R.id.restart).setOnClickListener(new View.OnClickListener() {
            private void triggerRebirth() {
                PackageManager packageManager = getPackageManager();
                Intent intent = packageManager.getLaunchIntentForPackage(getPackageName());

                if (intent != null) {
                    ComponentName componentName = intent.getComponent();
                    Intent mainIntent = Intent.makeRestartActivityTask(componentName);
                    mainIntent.setPackage(getPackageName());

                    startActivity(mainIntent);
                    System.exit(0);
                }
            }

            @Override
            public void onClick(View view) {
                getOnBackPressedDispatcher().onBackPressed();

                Transition transition = getWindow().getReturnTransition();

                if (transition != null) {
                    transition.addListener(new TransitionListenerAdapter() {
                        @Override
                        public void onTransitionEnd(Transition transition) {
                            triggerRebirth();
                        }
                    });
                } else {
                    triggerRebirth();
                }
            }
        });

        findViewById(R.id.def_launcher).setOnClickListener((view) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });

        ((TextView) findViewById(R.id.version)).setText(BuildConfig.VERSION_NAME + " • Răzvan Albu");

        findViewById(R.id.donate).setOnClickListener((view) -> {
            view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce_small));
            startActivity(new Intent(Intent.ACTION_DEFAULT, Uri.parse("https://www.buymeacoffee.com/razvanalbu")));
        });

        findViewById(R.id.twitter).setOnClickListener((view) -> {
            view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce_small));
            startActivity(new Intent(Intent.ACTION_DEFAULT, Uri.parse("https://twitter.com/razvan_albu_")));
        });

        findViewById(R.id.discord).setOnClickListener((view) -> {
            view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce_small));
            startActivity(new Intent(Intent.ACTION_DEFAULT, Uri.parse("https://discord.gg/WuVapMt9gY")));
        });
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
    protected void onResume() {
        super.onResume();

        checkNotificationPermission();
        checkAccessibilityPermission();
    }

    @Override
    protected boolean isOpaque() {
        return true;
    }
}
