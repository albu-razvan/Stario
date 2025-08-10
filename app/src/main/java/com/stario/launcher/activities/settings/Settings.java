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

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.stario.launcher.BuildConfig;
import com.stario.launcher.R;
import com.stario.launcher.Stario;
import com.stario.launcher.activities.launcher.glance.extensions.media.Media;
import com.stario.launcher.activities.launcher.glance.extensions.weather.Weather;
import com.stario.launcher.activities.launcher.pins.PinnedCategory;
import com.stario.launcher.activities.pages.PageManager;
import com.stario.launcher.activities.settings.dialogs.AccessibilityConfigurator;
import com.stario.launcher.activities.settings.dialogs.NotificationConfigurator;
import com.stario.launcher.activities.settings.dialogs.hide.HideApplicationsDialog;
import com.stario.launcher.activities.settings.dialogs.icons.IconsDialog;
import com.stario.launcher.activities.settings.dialogs.license.LicensesDialog;
import com.stario.launcher.activities.settings.dialogs.location.LocationDialog;
import com.stario.launcher.activities.settings.dialogs.pin.PinnedCategoryDialog;
import com.stario.launcher.activities.settings.dialogs.search.engine.SearchEngineDialog;
import com.stario.launcher.activities.settings.dialogs.search.results.SearchResultsDialog;
import com.stario.launcher.activities.settings.dialogs.theme.ThemeDialog;
import com.stario.launcher.apps.CategoryManager;
import com.stario.launcher.apps.IconPackManager;
import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.apps.ProfileApplicationManager;
import com.stario.launcher.apps.ProfileManager;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.sheet.drawer.search.SearchEngine;
import com.stario.launcher.sheet.drawer.search.SearchFragment;
import com.stario.launcher.sheet.drawer.search.recyclers.adapters.WebAdapter;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.common.CollapsibleTitleBar;
import com.stario.launcher.ui.common.lock.LockDetector;
import com.stario.launcher.ui.utils.UiUtils;
import com.stario.launcher.utils.Utils;

import java.util.UUID;

public class Settings extends ThemedActivity {
    private MaterialSwitch lockAnimSwitch;
    private CollapsibleTitleBar titleBar;
    private View titleMeasurePlaceholder;
    private View lockAnimSwitchContainer;
    private TextView pinnedCategoryName;
    private MaterialSwitch mediaSwitch;
    private SharedPreferences settings;
    private NestedScrollView scroller;
    private MaterialSwitch lockSwitch;
    private TextView searchEngineName;
    private SharedPreferences search;
    private SharedPreferences icons;
    private SharedPreferences pins;
    private TextView iconPackName;
    private Resources resources;
    private View titleLandscape;
    private TextView hideCount;
    private View searchEngine;
    private ViewGroup content;
    private View fader;

    public Settings() {
        super();
    }

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        postponeEnterTransition();

        resources = getResources();

        Stario stario = getApplicationContext();
        settings = stario.getSettings();
        icons = stario.getSharedPreferences(Entry.ICONS);
        search = stario.getSharedPreferences(Entry.SEARCH);
        pins = stario.getSharedPreferences(Entry.PINNED_CATEGORY);
        SharedPreferences theme = stario.getSharedPreferences(Entry.THEME);
        SharedPreferences weather = stario.getSharedPreferences(Entry.WEATHER);

        boolean mediaAllowed = settings.getBoolean(Media.PREFERENCE_ENTRY, false);
        boolean lock = settings.getBoolean(LockDetector.PREFERENCE_ENTRY, false);
        boolean legacyLockAnim = settings.getBoolean(LockDetector.LEGACY_ANIMATION, false);
        boolean imperialUnits = settings.getBoolean(Weather.IMPERIAL_KEY, false);
        boolean vibrations = settings.getBoolean(Vibrations.PREFERENCE_ENTRY, true);
        boolean searchResults = search.getBoolean(WebAdapter.SEARCH_RESULTS, false);
        boolean searchHiddenApps = search.getBoolean(SearchFragment.SEARCH_HIDDEN_APPS, false);
        boolean pinnedCategoryVisible = pins.getBoolean(PinnedCategory.PINNED_CATEGORY_VISIBLE, false);

        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        finishAfterTransition();
                    }
                });

        content = findViewById(R.id.content);
        scroller = findViewById(R.id.scroller);

        mediaSwitch = findViewById(R.id.media);
        lockSwitch = findViewById(R.id.lock);
        lockAnimSwitch = findViewById(R.id.lock_animation);
        MaterialSwitch imperialSwitch = findViewById(R.id.imperial);
        MaterialSwitch searchResultsSwitch = findViewById(R.id.search_results);
        MaterialSwitch pinnedCategorySwitch = findViewById(R.id.pinned_category);
        MaterialSwitch switchVibrations = findViewById(R.id.vibrations);
        MaterialSwitch switchSearchHiddenApps = findViewById(R.id.search_hidden_apps);

        View pinnedCategoryContainer = findViewById(R.id.pinned_category_container);
        lockAnimSwitchContainer = findViewById(R.id.lock_animation_container);
        titleMeasurePlaceholder = findViewById(R.id.title_placeholder);
        titleLandscape = findViewById(R.id.title_landscape);
        View container = findViewById(R.id.container);
        fader = findViewById(R.id.fader);

        Measurements.addStatusBarListener(value ->
                container.setPadding(container.getPaddingLeft(), value,
                        container.getPaddingRight(), Measurements.getNavHeight()));

        Measurements.addNavListener(value ->
                container.setPadding(container.getPaddingLeft(), Measurements.getSysUIHeight(),
                        container.getPaddingRight(), value));


        searchEngine = findViewById(R.id.search_engine);
        searchEngineName = findViewById(R.id.engine_name);
        pinnedCategoryName = findViewById(R.id.pinned_category_name);
        iconPackName = findViewById(R.id.pack_name);
        hideCount = findViewById(R.id.hidden_count);

        View location = findViewById(R.id.location);
        TextView themeName = findViewById(R.id.theme_name);
        TextView locationName = findViewById(R.id.location_name);

        updateIconPackName();
        updateHiddenAppsCount();
        updateEngineName();

        titleBar = findViewById(R.id.title_bar);
        titleBar.setOnOffsetChangeListener(offset ->
                fader.setTranslationY(titleBar.getMeasuredHeight() - titleBar.getCollapsedHeight() + offset));

        mediaSwitch.setChecked(Utils.isNotificationServiceEnabled(this) && mediaAllowed);
        lockSwitch.setChecked(Utils.isAccessibilityServiceEnabled(this) && lock);
        imperialSwitch.setChecked(imperialUnits);
        pinnedCategorySwitch.setChecked(pinnedCategoryVisible);
        searchResultsSwitch.setChecked(searchResults);
        switchSearchHiddenApps.setChecked(searchHiddenApps);
        lockAnimSwitch.setChecked(legacyLockAnim);
        switchVibrations.setChecked(vibrations);

        lockSwitch.jumpDrawablesToCurrentState();
        mediaSwitch.jumpDrawablesToCurrentState();
        imperialSwitch.jumpDrawablesToCurrentState();
        pinnedCategorySwitch.jumpDrawablesToCurrentState();
        searchResultsSwitch.jumpDrawablesToCurrentState();
        lockAnimSwitch.jumpDrawablesToCurrentState();
        switchVibrations.jumpDrawablesToCurrentState();
        switchSearchHiddenApps.jumpDrawablesToCurrentState();

        mediaSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            private NotificationConfigurator dialog;
            private boolean showing = false;

            @Override
            public void onCheckedChanged(CompoundButton compound, boolean isChecked) {
                if (isChecked && !Utils.isNotificationServiceEnabled(Settings.this)) {
                    if (dialog == null) {
                        dialog = new NotificationConfigurator(Settings.this);

                        dialog.setOnDismissListener(dialog -> {
                            checkNotificationPermission();
                            showing = false;
                        });
                    }

                    if (!showing) {
                        dialog.show();
                        showing = true;
                    }
                }

                settings.edit()
                        .putBoolean(Media.PREFERENCE_ENTRY, isChecked)
                        .apply();
            }
        });

        lockSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            private AccessibilityConfigurator dialog;
            private boolean showing = false;

            @Override
            public void onCheckedChanged(CompoundButton compound, boolean isChecked) {
                if (isChecked && !Utils.isAccessibilityServiceEnabled(Settings.this)) {
                    if (dialog == null) {
                        dialog = new AccessibilityConfigurator(Settings.this);

                        dialog.setOnDismissListener(dialog -> {
                            checkAccessibilityPermission();
                            showing = false;
                        });
                    }

                    if (!showing) {
                        dialog.show();
                        showing = true;
                    }
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

        pinnedCategorySwitch.setOnCheckedChangeListener((compound, isChecked) -> {
            if (isChecked && !isPinnedCategoryValid()) {
                pinnedCategorySwitch.setChecked(false);
                pinnedCategoryContainer.performClick();
            } else {
                pins.edit()
                        .putBoolean(PinnedCategory.PINNED_CATEGORY_VISIBLE, isChecked)
                        .apply();
            }
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

        themeName.setText(getThemeType().getDisplayName());
        if (theme.getBoolean(FORCE_DARK, false)) {
            themeName.append(" " + resources.getString(R.string.dark));
        }

        findViewById(R.id.theme).setOnClickListener(new View.OnClickListener() {
            private ThemeDialog dialog;
            private boolean showing = false;

            @Override
            public void onClick(View view) {
                if (dialog == null) {
                    dialog = new ThemeDialog(Settings.this);

                    dialog.setOnDismissListener((ThemeDialog.OnDismissListener) stateChanged -> {
                        showing = false;

                        if (stateChanged) {
                            restart();
                        }
                    });
                }

                if (!showing) {
                    dialog.show();
                    showing = true;
                }
            }
        });

        updatePinnedCategoryName();
        pinnedCategoryContainer.setOnClickListener(new View.OnClickListener() {
            private PinnedCategoryDialog dialog;
            private boolean showing = false;

            @Override
            public void onClick(View view) {
                if (dialog == null) {
                    dialog = new PinnedCategoryDialog(Settings.this, pins,
                            (isChecked) -> {
                                pinnedCategorySwitch.setChecked(isChecked);

                                return isPinnedCategoryValid() && isChecked;
                            });

                    dialog.setOnDismissListener(dialog -> {
                        updatePinnedCategoryName();
                        showing = false;
                    });
                }

                if (!showing) {
                    dialog.show();
                    showing = true;
                }
            }
        });

        switchSearchHiddenApps.setOnCheckedChangeListener((compound, isChecked) -> {
            search.edit()
                    .putBoolean(SearchFragment.SEARCH_HIDDEN_APPS, isChecked)
                    .apply();
        });

        searchEngine.setOnClickListener(new View.OnClickListener() {
            private SearchEngineDialog dialog;
            private boolean showing = false;

            @Override
            public void onClick(View view) {
                if (dialog == null) {
                    dialog = new SearchEngineDialog(Settings.this);

                    dialog.setOnDismissListener(dialog -> {
                        updateEngineName();
                        showing = false;
                    });
                }

                if (!showing) {
                    dialog.show();
                    showing = true;
                }
            }
        });

        findViewById(R.id.search_results_container).setOnClickListener(new View.OnClickListener() {
            private SearchResultsDialog dialog;
            private boolean showing = false;

            @Override
            public void onClick(View view) {
                if (dialog == null) {
                    dialog = new SearchResultsDialog(Settings.this);

                    dialog.setStatusListener(searchResultsSwitch::setChecked);
                    dialog.setOnDismissListener(dialog -> showing = false);
                }

                if (!showing) {
                    dialog.show();
                    showing = true;
                }
            }
        });

        findViewById(R.id.hidden_apps).setOnClickListener(new View.OnClickListener() {
            private HideApplicationsDialog dialog;
            private boolean showing = false;

            @Override
            public void onClick(View view) {
                if (dialog == null) {
                    dialog = new HideApplicationsDialog();

                    dialog.setOnHideListener(() -> {
                        updateHiddenAppsCount();
                        showing = false;
                    });
                }

                if (!showing) {
                    if (!dialog.isAdded()) {
                        dialog.show(getSupportFragmentManager(), "HideApplications");
                    } else {
                        dialog.show();
                    }

                    showing = true;
                }
            }
        });

        findViewById(R.id.icons).setOnClickListener(new View.OnClickListener() {
            private IconsDialog dialog;
            private boolean showing = false;

            @Override
            public void onClick(View view) {
                if (dialog == null) {
                    dialog = new IconsDialog(Settings.this);

                    dialog.setOnDismissListener(dialog -> {
                        updateIconPackName();
                        showing = false;
                    });
                }

                if (!showing) {
                    dialog.show();
                    showing = true;
                }
            }
        });

        locationName.setText(weather.getString(Weather.LOCATION_NAME,
                resources.getString(R.string.location_ip_based)));
        location.setOnClickListener(new View.OnClickListener() {
            private LocationDialog dialog;
            private boolean showing = false;

            @Override
            public void onClick(View view) {
                if (dialog == null) {
                    dialog = new LocationDialog(Settings.this);

                    dialog.setOnDismissListener(dialog -> {
                        locationName.setText(weather.getString(Weather.LOCATION_NAME,
                                resources.getString(R.string.location_ip_based)));
                        showing = false;
                    });
                }

                if (!showing) {
                    dialog.show();
                    showing = true;
                }
            }
        });

        findViewById(R.id.licenses).setOnClickListener(new View.OnClickListener() {
            private LicensesDialog dialog;
            private boolean showing = false;

            @Override
            public void onClick(View view) {
                if (dialog == null) {
                    dialog = new LicensesDialog(Settings.this);

                    dialog.setOnDismissListener(dialog -> {
                        showing = false;
                    });
                }

                if (!showing) {
                    dialog.show();
                    showing = true;
                }
            }
        });

        findViewById(R.id.pages).setOnClickListener(view ->
                startActivity(new Intent(this, PageManager.class),
                        ActivityOptions.makeSceneTransitionAnimation(this).toBundle()));

        findViewById(R.id.restart).setOnClickListener(view -> restart());

        findViewById(R.id.def_launcher).setOnClickListener((view) -> {
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
        findViewById(R.id.search_hidden_apps_container).setOnClickListener((view) -> switchSearchHiddenApps.performClick());
        updateLockAnimationState(lockSwitch.isChecked());

        UiUtils.Notch.applyNotchMargin(findViewById(R.id.coordinator), UiUtils.Notch.Treatment.CENTER);
        getRoot().post(this::startPostponedEnterTransition);
        handleOrientation();
    }

    private void restart() {
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

    private void updateLockAnimationState(boolean enabled) {
        lockAnimSwitchContainer.setAlpha(enabled ? 1f : 0.5f);

        if (enabled) {
            lockAnimSwitchContainer.setOnClickListener((view) -> lockAnimSwitch.performClick());
        } else {
            lockAnimSwitchContainer.setOnClickListener(null);
        }
    }

    private void updateEngineName() {
        searchEngineName.setText(SearchEngine.getEngine(getApplicationContext()).toString());

        searchEngine.setEnabled(!search.getBoolean(WebAdapter.SEARCH_RESULTS, false));
        searchEngine.setAlpha(searchEngine.isEnabled() ? 1f : 0.6f);
    }

    private void updateIconPackName() {
        String packPackageName = icons.getString(IconPackManager.ICON_PACK_ENTRY, null);

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

    private void updatePinnedCategoryName() {
        if (pins.contains(PinnedCategory.PINNED_CATEGORY)) {
            try {
                pinnedCategoryName.setText(CategoryManager.getInstance().getCategoryName(
                        UUID.fromString(pins.getString(PinnedCategory.PINNED_CATEGORY, ""))));
                pinnedCategoryName.setVisibility(View.VISIBLE);
            } catch (IllegalArgumentException exception) {
                pinnedCategoryName.setVisibility(View.GONE);
            }
        } else {
            pinnedCategoryName.setVisibility(View.GONE);
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateHiddenAppsCount() {
        int count = 0;

        for (ProfileApplicationManager manager : ProfileManager.getInstance().getProfiles()) {
            count += manager.getActualSize() - manager.getSize();
        }

        hideCount.setText(resources.getString(R.string.hidden_apps) + ": " + count);
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

    private boolean isPinnedCategoryValid() {
        String identifier = pins.getString(PinnedCategory.PINNED_CATEGORY, null);

        if (identifier == null) {
            return false;
        }

        try {
            return CategoryManager.getInstance().get(UUID.fromString(identifier)) != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkNotificationPermission();
        checkAccessibilityPermission();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration configuration) {
        content.setLayoutTransition(null);

        super.onConfigurationChanged(configuration);
        handleOrientation();

        content.post(() -> content.setLayoutTransition(new LayoutTransition()));
    }

    private void handleOrientation() {
        if (Measurements.isLandscape()) {
            titleBar.getLayoutParams().height = Measurements.dpToPx(Measurements.HEADER_SIZE_DP / 3f);
            titleBar.requestLayout();

            titleMeasurePlaceholder.setVisibility(View.GONE);
            titleLandscape.setVisibility(View.VISIBLE);
            titleBar.setVisibility(View.GONE);

            fader.setTranslationY(0);
        } else {
            titleBar.getLayoutParams().height = Measurements.dpToPx(Measurements.HEADER_SIZE_DP);
            titleBar.requestLayout();

            titleMeasurePlaceholder.setVisibility(View.INVISIBLE);
            titleLandscape.setVisibility(View.GONE);

            scroller.post(() -> {
                if (scroller.canScrollVertically(-1)) {
                    titleBar.collapse();
                    fader.setTranslationY(0);
                }

                titleBar.setVisibility(View.VISIBLE);
            });
        }
    }

    @Override
    protected boolean isOpaque() {
        return true;
    }

    @Override
    protected boolean isAffectedByBackGesture() {
        return true;
    }
}
