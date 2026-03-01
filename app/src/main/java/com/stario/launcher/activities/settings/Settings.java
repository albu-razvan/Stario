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

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.stario.launcher.BuildConfig;
import com.stario.launcher.R;
import com.stario.launcher.Stario;
import com.stario.launcher.activities.pages.PageManager;
import com.stario.launcher.activities.settings.dialogs.HomeScreenDialog;
import com.stario.launcher.activities.settings.dialogs.hide.HideApplicationsDialog;
import com.stario.launcher.activities.settings.dialogs.icons.IconsDialog;
import com.stario.launcher.activities.settings.dialogs.license.LicensesDialog;
import com.stario.launcher.activities.settings.dialogs.search.engine.SearchEngineDialog;
import com.stario.launcher.activities.settings.dialogs.search.results.SearchResultsDialog;
import com.stario.launcher.activities.settings.dialogs.theme.ThemeDialog;
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
import com.stario.launcher.ui.dialogs.DialogBackgroundDimmingController;
import com.stario.launcher.ui.utils.LayoutSizeObserver;
import com.stario.launcher.ui.utils.UiUtils;

public class Settings extends ThemedActivity {

    // Data
    private SharedPreferences settingsPrefs;
    private SharedPreferences searchPrefs;
    private SharedPreferences iconsPrefs;
    private SharedPreferences themePrefs;
    private PowerManager powerManager;
    private boolean isBatterySaverOn;
    private Resources resources;

    // Views
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private NestedScrollView scroller;
    private AppBarLayout appBar;
    private View titleLandscape;
    private ViewGroup content;

    // Dynamic views
    private MaterialSwitch lowSpecSwitch;
    private View searchEngineContainer;
    private TextView searchEngineName;
    private View lowSpecContainer;
    private TextView iconPackName;
    private TextView hideCount;

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

        init();

        initViews();
        setupWindowInsets();
        setupLifecycleObservers();

        initGeneralSection();
        initDisplaySection();
        initSearchSection();
        initMiscSection();
        initFooterLinks();

        handleOrientation();
        getRoot().post(this::startPostponedEnterTransition);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (batterySaverReceiver != null) {
            unregisterReceiver(batterySaverReceiver);
        }
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

    private void init() {
        resources = getResources();
        Stario stario = getApplicationContext();

        settingsPrefs = stario.getSettings();
        iconsPrefs = stario.getSharedPreferences(Entry.ICONS);
        searchPrefs = stario.getSharedPreferences(Entry.SEARCH);
        themePrefs = stario.getSharedPreferences(Entry.THEME);

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
        iconPackName = findViewById(R.id.pack_name);
        hideCount = findViewById(R.id.hidden_count);
        lowSpecSwitch = findViewById(R.id.low_spec);
        searchEngineName = findViewById(R.id.engine_name);
        titleLandscape = findViewById(R.id.title_landscape);
        searchEngineContainer = findViewById(R.id.search_engine);
        lowSpecContainer = findViewById(R.id.low_spec_container);
        collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);

        appBar = findViewById(R.id.app_bar);
        TextView titlePortrait = findViewById(R.id.title_portrait);
        LayoutSizeObserver.attach(titlePortrait, LayoutSizeObserver.HEIGHT,
                new LayoutSizeObserver.OnChange() {
                    @Override
                    public void onChange(View view, int watchFlags) {
                        titlePortrait.setPivotY(titlePortrait.getHeight() / 2f);
                    }
                });
        appBar.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            // The text should scale from 30sp to 20sp
            float factor = (float) -verticalOffset / appBarLayout.getTotalScrollRange();
            factor = 1 - 0.333f * factor;

            if (!Float.isNaN(factor)) {
                titlePortrait.setScaleX(factor);
                titlePortrait.setScaleY(factor);
            }
        });
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
        // Home Screen
        findViewById(R.id.home).setOnClickListener(new View.OnClickListener() {
            private HomeScreenDialog dialog;
            private boolean showing = false;

            @Override
            public void onClick(View view) {
                if (dialog == null) {
                    dialog = new HomeScreenDialog(Settings.this);
                }

                dialog.setOnDismissListener((d) -> showing = false);

                if (!showing) {
                    dialog.show();
                    showing = true;
                }
            }
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
        View searchResultsContainer = findViewById(R.id.search_results_container);
        MaterialSwitch resultsSwitch = findViewById(R.id.search_results);

        searchResultsContainer.setOnClickListener(new View.OnClickListener() {
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

        setupSwitch(resultsSwitch, searchPrefs.getBoolean(WebAdapter.SEARCH_RESULTS, false),
                (button, checked) -> {
                    String key = searchPrefs.getString(WebAdapter.KAGI_API_KEY, null);
                    if (checked && (key == null || key.isEmpty())) {
                        searchResultsContainer.performClick();
                        button.setChecked(false);
                    } else {
                        searchPrefs.edit()
                                .putBoolean(WebAdapter.SEARCH_RESULTS, checked)
                                .apply();
                        updateEngineName();
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

    private void initMiscSection() {
        updateLowSpecState();
        lowSpecSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            if (!isBatterySaverOn) {
                settingsPrefs.edit()
                        .putBoolean(DialogBackgroundDimmingController.LOW_SPEC_KEY, isChecked)
                        .apply();
            }
        });
        lowSpecSwitch.jumpDrawablesToCurrentState();

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

                    dialog.setOnDismissListener(dialog -> showing = false);
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

    @SuppressLint("SetTextI18n")
    private void updateHiddenAppsCount() {
        int count = 0;

        for (ProfileApplicationManager manager : ProfileManager.getInstance().getProfiles()) {
            count += manager.getActualSize() - manager.getSize();
        }

        hideCount.setText(resources.getString(R.string.hidden_apps) + ": " + count);
    }

    // Utils
    private void handleOrientation() {
        scroller.stopNestedScroll();
        scroller.setNestedScrollingEnabled(false);

        AppBarLayout.LayoutParams params =
                (AppBarLayout.LayoutParams) collapsingToolbarLayout.getLayoutParams();

        if (Measurements.isLandscape()) {
            params.height = 0;
            params.setScrollFlags(
                    AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
            );

            collapsingToolbarLayout.setTitleEnabled(false);
            titleLandscape.setVisibility(View.VISIBLE);
        } else {
            params.height = Measurements.dpToPx(Measurements.HEADER_SIZE_DP);
            params.setScrollFlags(
                    AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL |
                            AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED |
                            AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
            );

            collapsingToolbarLayout.setTitleEnabled(true);
            titleLandscape.setVisibility(View.GONE);
        }

        collapsingToolbarLayout.setLayoutParams(params);

        scroller.setNestedScrollingEnabled(true);
        scroller.setScrollY(0);
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
