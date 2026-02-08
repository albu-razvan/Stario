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

import android.Manifest;
import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.role.RoleManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

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
import com.stario.launcher.ui.dialogs.DialogBackgroundDimmingController;
import com.stario.launcher.ui.utils.UiUtils;
import com.stario.launcher.utils.Utils;

import java.util.UUID;

public class Settings extends ThemedActivity {

    // Data
    private SharedPreferences settingsPrefs;
    private SharedPreferences searchPrefs;
    private SharedPreferences iconsPrefs;
    private SharedPreferences pinsPrefs;
    private SharedPreferences weatherPrefs;
    private SharedPreferences themePrefs;
    private Resources resources;
    private PowerManager powerManager;
    private boolean isBatterySaverOn;

    // Views
    private CollapsibleTitleBar titleBar;
    private View titleMeasurePlaceholder;
    private View titleLandscape;
    private NestedScrollView scroller;
    private ViewGroup content;
    private View fader;

    // Dynamic views
    private TextView searchEngineName;
    private View searchEngineContainer;
    private TextView pinnedCategoryName;
    private MaterialSwitch pinnedCategorySwitch;
    private TextView iconPackName;
    private TextView hideCount;
    private MaterialSwitch mediaSwitch;
    private MaterialSwitch lockSwitch;
    private MaterialSwitch lowSpecSwitch;
    private View lowSpecContainer;
    private MaterialSwitch lockAnimSwitch;
    private View lockAnimContainer;

    // Misc
    private ActivityResultLauncher<Intent> homeRoleLauncher;
    private BroadcastReceiver batterySaverReceiver;

    public Settings() {
        super();
    }

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        postponeEnterTransition();

        initCoreServices();
        initViews();
        setupWindowInsets();
        setupLifecycleObservers();

        initGeneralSection();
        initDisplaySection();
        initSearchSection();
        initWeatherSection();
        initAnimationSection();
        initMiscSection();
        initFooterLinks();

        getRoot().post(this::startPostponedEnterTransition);
        handleOrientation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (batterySaverReceiver != null) {
            unregisterReceiver(batterySaverReceiver);
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
        // Prevent layout transition glitches during rotation
        if (content != null) {
            content.setLayoutTransition(null);
        }

        super.onConfigurationChanged(configuration);

        handleOrientation();
        if (content != null) {
            content.post(() -> content.setLayoutTransition(new LayoutTransition()));
        }
    }

    private void initCoreServices() {
        resources = getResources();
        Stario stario = getApplicationContext();

        settingsPrefs = stario.getSettings();
        iconsPrefs = stario.getSharedPreferences(Entry.ICONS);
        searchPrefs = stario.getSharedPreferences(Entry.SEARCH);
        pinsPrefs = stario.getSharedPreferences(Entry.PINNED_CATEGORY);
        themePrefs = stario.getSharedPreferences(Entry.THEME);
        weatherPrefs = stario.getSharedPreferences(Entry.WEATHER);

        powerManager = (PowerManager) getSystemService(ThemedActivity.POWER_SERVICE);

        homeRoleLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        finishAfterTransition();
                    }
                });
    }

    private void initViews() {
        content = findViewById(R.id.content);
        scroller = findViewById(R.id.scroller);
        titleBar = findViewById(R.id.title_bar);
        fader = findViewById(R.id.fader);
        titleMeasurePlaceholder = findViewById(R.id.title_placeholder);
        titleLandscape = findViewById(R.id.title_landscape);

        searchEngineName = findViewById(R.id.engine_name);
        searchEngineContainer = findViewById(R.id.search_engine);
        pinnedCategoryName = findViewById(R.id.pinned_category_name);
        pinnedCategorySwitch = findViewById(R.id.pinned_category);
        iconPackName = findViewById(R.id.pack_name);
        hideCount = findViewById(R.id.hidden_count);
        mediaSwitch = findViewById(R.id.media);
        lockSwitch = findViewById(R.id.lock);
        lowSpecSwitch = findViewById(R.id.low_spec);
        lowSpecContainer = findViewById(R.id.low_spec_container);
        lockAnimSwitch = findViewById(R.id.lock_animation);
        lockAnimContainer = findViewById(R.id.lock_animation_container);

        titleBar.setOnOffsetChangeListener(offset ->
                fader.setTranslationY(titleBar.getMeasuredHeight() - titleBar.getCollapsedHeight() + offset));
    }

    private void setupWindowInsets() {
        View container = findViewById(R.id.container);
        Measurements.addStatusBarListener(value ->
                container.setPadding(container.getPaddingLeft(), value,
                        container.getPaddingRight(), Measurements.getNavHeight()));

        Measurements.addNavListener(value ->
                container.setPadding(container.getPaddingLeft(), Measurements.getSysUIHeight(),
                        container.getPaddingRight(), value));

        UiUtils.Notch.applyNotchMargin(findViewById(R.id.coordinator),
                UiUtils.Notch.Treatment.CENTER);
    }

    private void setupLifecycleObservers() {
        isBatterySaverOn = powerManager.isPowerSaveMode();
        batterySaverReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                isBatterySaverOn = powerManager.isPowerSaveMode();
                updateLowSpecState();
            }
        };

        registerReceiver(batterySaverReceiver,
                new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED));
    }

    // Settings
    private void initGeneralSection() {
        // Media Player Switch
        setupSwitch(mediaSwitch, findViewById(R.id.media_container),
                settingsPrefs.getBoolean(Media.PREFERENCE_ENTRY, false),
                (button, checked) -> {
                    settingsPrefs.edit()
                            .putBoolean(Media.PREFERENCE_ENTRY, checked)
                            .apply();

                    if (checked && !Utils.isNotificationServiceEnabled(Settings.this)) {
                        showNotificationPermissionDialog();
                    }
                });

        // Double Tap Lock Switch
        setupSwitch(lockSwitch, findViewById(R.id.lock_container),
                settingsPrefs.getBoolean(LockDetector.PREFERENCE_ENTRY, false),
                (button, checked) -> {
                    settingsPrefs.edit()
                            .putBoolean(LockDetector.PREFERENCE_ENTRY, checked)
                            .apply();

                    if (checked && !Utils.isAccessibilityServiceEnabled(Settings.this)) {
                        showAccessibilityPermissionDialog();
                    }

                    updateLockAnimationState(checked);
                });

        // Page Manager
        findViewById(R.id.pages).setOnClickListener(view ->
                startActivity(new Intent(this, PageManager.class),
                        ActivityOptions.makeSceneTransitionAnimation(this).toBundle()));

        // Theme
        TextView themeName = findViewById(R.id.theme_name);
        themeName.setText(getThemeType().getDisplayName());

        if (themePrefs.getBoolean(FORCE_DARK, false)) {
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
    }

    private void initDisplaySection() {
        // Pinned Category
        View pinnedCategoryContainer = findViewById(R.id.pinned_category_container);

        updatePinnedCategoryName();
        pinnedCategorySwitch.setChecked(
                pinsPrefs.getBoolean(PinnedCategory.PINNED_CATEGORY_VISIBLE, false));
        pinnedCategorySwitch.jumpDrawablesToCurrentState();

        pinnedCategorySwitch.setOnCheckedChangeListener((button, isChecked) -> {
            if (isChecked && !isPinnedCategoryValid()) {
                pinnedCategorySwitch.setChecked(false);
                pinnedCategoryContainer.performClick();
            } else {
                pinsPrefs.edit()
                        .putBoolean(PinnedCategory.PINNED_CATEGORY_VISIBLE, isChecked)
                        .apply();
            }
        });

        pinnedCategoryContainer.setOnClickListener(new View.OnClickListener() {
            private PinnedCategoryDialog dialog;
            private boolean showing = false;

            @Override
            public void onClick(View view) {
                if (dialog == null) {
                    dialog = new PinnedCategoryDialog(Settings.this, pinsPrefs,
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

        // Hidden Apps
        updateHiddenAppsCount();
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

        // Icons
        updateIconPackName();
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
    }

    private void initSearchSection() {
        updateEngineName();

        // Search Engine
        searchEngineContainer.setOnClickListener(new View.OnClickListener() {
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

        // Search Results
        MaterialSwitch resultsSwitch = findViewById(R.id.search_results);
        setupSwitch(resultsSwitch, searchPrefs.getBoolean(WebAdapter.SEARCH_RESULTS, false),
                (button, checked) -> {
                    searchPrefs.edit()
                            .putBoolean(WebAdapter.SEARCH_RESULTS, checked)
                            .apply();
                    updateEngineName();
                });

        findViewById(R.id.search_results_container).setOnClickListener(new View.OnClickListener() {
            private SearchResultsDialog dialog;
            private boolean showing = false;

            @Override
            public void onClick(View view) {
                if (dialog == null) {
                    dialog = new SearchResultsDialog(Settings.this);

                    dialog.setStatusListener(resultsSwitch::setChecked);
                    dialog.setOnDismissListener(dialog -> showing = false);
                }

                if (!showing) {
                    dialog.show();
                    showing = true;
                }
            }
        });

        // Search Hidden Apps
        setupSwitch(findViewById(R.id.search_hidden_apps), findViewById(R.id.search_hidden_apps_container),
                searchPrefs.getBoolean(SearchFragment.SEARCH_HIDDEN_APPS, false),
                (button, checked) ->
                        searchPrefs.edit()
                                .putBoolean(SearchFragment.SEARCH_HIDDEN_APPS, checked)
                                .apply());
    }

    private void initWeatherSection() {
        setupSwitch(findViewById(R.id.weather), findViewById(R.id.weather_container),
                weatherPrefs.getBoolean(Weather.FORECAST_KEY, true),
                (button, checked) -> {
                    weatherPrefs.edit()
                            .putBoolean(Weather.FORECAST_KEY, checked)
                            .apply();

                    //noinspection deprecation
                    LocalBroadcastManager.getInstance(this)
                            .sendBroadcastSync(new Intent(Weather.ACTION_REQUEST_UPDATE));
                });

        // Location
        TextView locationName = findViewById(R.id.location_name);
        locationName.setText(getLocationString());

        findViewById(R.id.location).setOnClickListener(new View.OnClickListener() {
            private LocationDialog dialog;
            private boolean showing = false;

            @Override
            public void onClick(View view) {
                if (dialog == null) {
                    dialog = new LocationDialog(Settings.this);

                    dialog.setOnLocationUpdateListener(() ->
                            locationName.setText(getLocationString()));
                    dialog.setOnDismissListener(dialog -> showing = false);
                }

                if (!showing) {
                    dialog.show();
                    showing = true;
                }
            }
        });
    }

    private void initAnimationSection() {
        // Low Spec
        updateLowSpecState();
        lowSpecSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            if (!isBatterySaverOn) {
                settingsPrefs.edit()
                        .putBoolean(DialogBackgroundDimmingController.LOW_SPEC_KEY, isChecked)
                        .apply();
            }
        });
        lowSpecSwitch.jumpDrawablesToCurrentState();

        // Legacy Lock Animation
        setupSwitch(lockAnimSwitch, findViewById(R.id.lock_animation_container),
                settingsPrefs.getBoolean(LockDetector.LEGACY_ANIMATION, false),
                (button, checked) ->
                        settingsPrefs.edit()
                                .putBoolean(LockDetector.LEGACY_ANIMATION, checked)
                                .apply());
        updateLockAnimationState(settingsPrefs.getBoolean(LockDetector.PREFERENCE_ENTRY, false));
    }

    private void initMiscSection() {
        setupSwitch(findViewById(R.id.imperial), findViewById(R.id.imperial_container),
                settingsPrefs.getBoolean(Weather.IMPERIAL_KEY, false),
                (button, checked) ->
                        settingsPrefs.edit()
                                .putBoolean(Weather.IMPERIAL_KEY, checked)
                                .apply());

        setupSwitch(findViewById(R.id.vibrations), findViewById(R.id.vibrations_container),
                settingsPrefs.getBoolean(Vibrations.PREFERENCE_ENTRY, true),
                (button, checked) ->
                        settingsPrefs.edit()
                                .putBoolean(Vibrations.PREFERENCE_ENTRY, checked)
                                .apply());

        findViewById(R.id.restart).setOnClickListener(view -> restart());
        findViewById(R.id.def_launcher).setOnClickListener(view -> requestDefaultLauncherRole());
    }

    private void initFooterLinks() {
        //noinspection SetTextI18n
        ((TextView) findViewById(R.id.version)).setText(BuildConfig.VERSION_NAME + " • Răzvan Albu");

        findViewById(R.id.about).setOnClickListener(view -> {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
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

        setupUrlButton(R.id.github, "https://github.com/albu-razvan/Stario");
        setupUrlButton(R.id.website, "https://www.razvanalbu.com");
        setupUrlButton(R.id.discord, "https://discord.gg/WuVapMt9gY");
    }

    // Helpers
    private void setupSwitch(MaterialSwitch switchView, boolean defaultValue,
                             CompoundButton.OnCheckedChangeListener listener) {
        setupSwitch(switchView, null, defaultValue, listener);
    }

    private void setupSwitch(MaterialSwitch switchView, @Nullable View container,
                             boolean defaultValue, CompoundButton.OnCheckedChangeListener listener) {

        switchView.setChecked(defaultValue);
        switchView.jumpDrawablesToCurrentState();
        switchView.setOnCheckedChangeListener(listener);

        if (container != null) {
            container.setOnClickListener(view -> switchView.performClick());
        }
    }

    private void setupUrlButton(int viewId, String url) {
        findViewById(viewId).setOnClickListener(view -> {
            view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce_small));
            startActivity(new Intent(Intent.ACTION_DEFAULT, Uri.parse(url)));
        });
    }

    private void requestDefaultLauncherRole() {
        RoleManager roleManager = getSystemService(RoleManager.class);

        if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
            if (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME);
                homeRoleLauncher.launch(intent, ActivityOptionsCompat.makeBasic());
            } else {
                Intent intent = new Intent(android.provider.Settings.ACTION_HOME_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }
    }

    private void updateLockAnimationState(boolean enabled) {
        lockAnimContainer.setAlpha(enabled ? 1f : 0.6f);

        if (enabled) {
            lockAnimContainer.setOnClickListener((view) -> lockAnimSwitch.performClick());
        } else {
            lockAnimContainer.setOnClickListener(null);
        }
    }

    private void updateLowSpecState() {
        lowSpecContainer.setAlpha(isBatterySaverOn ? 0.6f : 1f);

        if (isBatterySaverOn) {
            lowSpecContainer.setOnClickListener(null);
            lowSpecSwitch.setChecked(true);
        } else {
            lowSpecContainer.setOnClickListener((view) -> lowSpecSwitch.performClick());
            lowSpecSwitch.setChecked(settingsPrefs.getBoolean(DialogBackgroundDimmingController.LOW_SPEC_KEY, false));
        }
    }

    private void updateEngineName() {
        searchEngineName.setText(SearchEngine.getEngine(getApplicationContext()).toString());

        boolean resultsEnabled = searchPrefs.getBoolean(WebAdapter.SEARCH_RESULTS, false);
        searchEngineContainer.setEnabled(!resultsEnabled);
        searchEngineContainer.setAlpha(!resultsEnabled ? 1f : 0.6f);
    }

    private void updateIconPackName() {
        String packPackageName = iconsPrefs.getString(IconPackManager.ICON_PACK_ENTRY, null);

        if (packPackageName != null) {
            LauncherApplication iconPackApp =
                    ProfileManager.getInstance().getApplication(packPackageName);

            if (iconPackApp != LauncherApplication.FALLBACK_APP) {
                iconPackName.setText(iconPackApp.getLabel());

                return;
            }
        }

        iconPackName.setText(R.string.default_text);
    }

    private void updatePinnedCategoryName() {
        if (pinsPrefs.contains(PinnedCategory.PINNED_CATEGORY)) {
            try {
                pinnedCategoryName.setText(CategoryManager.getInstance().getCategoryName(
                        UUID.fromString(pinsPrefs.getString(PinnedCategory.PINNED_CATEGORY, ""))));
                pinnedCategoryName.setVisibility(View.VISIBLE);
            } catch (IllegalArgumentException | NullPointerException exception) {
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

    private String getLocationString() {
        String location = weatherPrefs.getString(Weather.LOCATION_NAME,
                resources.getString(R.string.location_ip_based));

        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && weatherPrefs.getBoolean(Weather.PRECISE_LOCATION, false)) {
            location = resources.getString(R.string.precise_location);
        }

        return location;
    }

    // Permission Dialogs
    private void showNotificationPermissionDialog() {
        NotificationConfigurator dialog = new NotificationConfigurator(Settings.this);

        dialog.setOnDismissListener(d -> checkNotificationPermission());
        dialog.show();
    }

    private void showAccessibilityPermissionDialog() {
        AccessibilityConfigurator dialog = new AccessibilityConfigurator(Settings.this);

        dialog.setOnDismissListener(d -> checkAccessibilityPermission());
        dialog.show();
    }

    // Utils
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
        String identifier = pinsPrefs.getString(PinnedCategory.PINNED_CATEGORY, null);

        if (identifier == null) {
            return false;
        }

        try {
            return CategoryManager.getInstance().get(UUID.fromString(identifier)) != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private void handleOrientation() {
        if (Measurements.isLandscape()) {
            titleBar.getLayoutParams().height =
                    Measurements.dpToPx(Measurements.HEADER_SIZE_DP / 3f);
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

    @Override
    protected boolean isOpaque() {
        return true;
    }

    @Override
    protected boolean isAffectedByBackGesture() {
        return true;
    }
}
