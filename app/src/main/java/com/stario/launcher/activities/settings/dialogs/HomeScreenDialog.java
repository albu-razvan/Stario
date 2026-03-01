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

package com.stario.launcher.activities.settings.dialogs;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.stario.launcher.R;
import com.stario.launcher.Stario;
import com.stario.launcher.activities.launcher.widgets.ClockWidget;
import com.stario.launcher.activities.launcher.widgets.glance.extensions.media.Media;
import com.stario.launcher.activities.launcher.widgets.glance.extensions.weather.Weather;
import com.stario.launcher.activities.launcher.widgets.pins.PinnedCategory;
import com.stario.launcher.activities.settings.dialogs.location.LocationDialog;
import com.stario.launcher.activities.settings.dialogs.pin.PinnedCategoryDialog;
import com.stario.launcher.apps.CategoryManager;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.common.StylizedClockView;
import com.stario.launcher.ui.common.lock.LockDetector;
import com.stario.launcher.ui.dialogs.ActionDialog;
import com.stario.launcher.utils.Utils;

import java.util.UUID;

public class HomeScreenDialog extends ActionDialog {
    private NestedScrollView scroller;
    private ViewGroup root;

    // Data
    private SharedPreferences pinsPrefs;
    private SharedPreferences clockPrefs;
    private SharedPreferences weatherPrefs;
    private SharedPreferences settingsPrefs;
    private Resources resources;

    // Dynamic views
    private MaterialSwitch pinnedCategorySwitch;
    private MaterialSwitch lockAnimSwitch;
    private TextView pinnedCategoryName;
    private MaterialSwitch mediaSwitch;
    private MaterialSwitch lockSwitch;
    private View lockAnimContainer;

    public HomeScreenDialog(@NonNull ThemedActivity activity) {
        super(activity);
    }

    @SuppressLint("ClickableViewAccessibility")
    @NonNull
    @Override
    protected View inflateContent(LayoutInflater inflater) {
        root = (ViewGroup) inflater.inflate(R.layout.pop_up_home, null);

        resources = activity.getResources();
        Stario stario = activity.getApplicationContext();

        pinsPrefs = stario.getSharedPreferences(Entry.PINNED_CATEGORY);
        weatherPrefs = stario.getSharedPreferences(Entry.WEATHER);
        clockPrefs = stario.getSharedPreferences(Entry.CLOCK);
        settingsPrefs = stario.getSettings();

        pinnedCategoryName = root.findViewById(R.id.pinned_category_name);
        pinnedCategorySwitch = root.findViewById(R.id.pinned_category);
        lockAnimContainer = root.findViewById(R.id.lock_animation_container);
        lockAnimSwitch = root.findViewById(R.id.lock_animation);
        mediaSwitch = root.findViewById(R.id.media);
        scroller = root.findViewById(R.id.scroller);
        lockSwitch = root.findViewById(R.id.lock);

        initGeneralSection();
        initClockSection();
        initWeatherSection();
        initGestureSection();

        return root;
    }

    private void initGeneralSection() {
        // Media Player Switch
        setupSwitch(mediaSwitch, root.findViewById(R.id.media_container),
                settingsPrefs.getBoolean(Media.PREFERENCE_ENTRY, false),
                (button, checked) -> {
                    settingsPrefs.edit()
                            .putBoolean(Media.PREFERENCE_ENTRY, checked)
                            .apply();

                    if (checked && !Utils.isNotificationServiceEnabled(activity)) {
                        showNotificationPermissionDialog();
                    }
                });

        // Pinned Category
        View pinnedCategoryContainer = root.findViewById(R.id.pinned_category_container);

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
                    dialog = new PinnedCategoryDialog(activity, pinsPrefs,
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
    }

    private void initClockSection() {
        setupSwitch(root.findViewById(R.id.clock), root.findViewById(R.id.clock_container),
                settingsPrefs.getBoolean(ClockWidget.CLOCK_WIDGET_KEY, true),
                (button, checked) ->
                        settingsPrefs.edit()
                                .putBoolean(ClockWidget.CLOCK_WIDGET_KEY, checked)
                                .apply());

        Slider slider = root.findViewById(R.id.background_slider);

        slider.setValueFrom(0);
        slider.setValueTo(255);
        slider.setStepSize(1);

        slider.setValue(clockPrefs.getInt(StylizedClockView.BACKGROUND_ALPHA_KEY, 0));

        slider.addOnChangeListener((s, value, fromUser) -> clockPrefs.edit()
                .putInt(StylizedClockView.BACKGROUND_ALPHA_KEY, (int) value)
                .apply());

        setupSwitch(root.findViewById(R.id.imperial_clock), root.findViewById(R.id.imperial_clock_container),
                clockPrefs.getBoolean(StylizedClockView.IMPERIAL_KEY, Utils.isSystemUsingImperial(activity)),
                (button, checked) ->
                        clockPrefs.edit()
                                .putBoolean(StylizedClockView.IMPERIAL_KEY, checked)
                                .apply());
    }

    private void initWeatherSection() {
        // Weather
        setupSwitch(root.findViewById(R.id.weather), root.findViewById(R.id.weather_container),
                weatherPrefs.getBoolean(Weather.FORECAST_KEY, true),
                (button, checked) -> {
                    weatherPrefs.edit()
                            .putBoolean(Weather.FORECAST_KEY, checked)
                            .apply();

                    //noinspection deprecation
                    LocalBroadcastManager.getInstance(activity)
                            .sendBroadcastSync(new Intent(Weather.ACTION_REQUEST_UPDATE));
                });

        // Location
        TextView locationName = root.findViewById(R.id.location_name);
        locationName.setText(getLocationString());

        root.findViewById(R.id.location).setOnClickListener(new View.OnClickListener() {
            private LocationDialog dialog;
            private boolean showing = false;

            @Override
            public void onClick(View view) {
                if (dialog == null) {
                    dialog = new LocationDialog(activity);

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

        setupSwitch(root.findViewById(R.id.imperial_weather), root.findViewById(R.id.imperial_weather_container),
                weatherPrefs.getBoolean(Weather.IMPERIAL_KEY, Utils.isSystemUsingImperial(activity)),
                (button, checked) ->
                        weatherPrefs.edit()
                                .putBoolean(Weather.IMPERIAL_KEY, checked)
                                .apply());
    }

    private void initGestureSection() {
        // Double Tap Lock Switch
        setupSwitch(lockSwitch, root.findViewById(R.id.lock_container),
                settingsPrefs.getBoolean(LockDetector.PREFERENCE_ENTRY, false),
                (button, checked) -> {
                    settingsPrefs.edit()
                            .putBoolean(LockDetector.PREFERENCE_ENTRY, checked)
                            .apply();

                    if (checked && !Utils.isAccessibilityServiceEnabled(activity)) {
                        showAccessibilityPermissionDialog();
                    }

                    updateLockAnimationState(checked);
                });

        // Legacy Lock Animation
        setupSwitch(lockAnimSwitch, root.findViewById(R.id.lock_animation_container),
                settingsPrefs.getBoolean(LockDetector.LEGACY_ANIMATION, false),
                (button, checked) ->
                        settingsPrefs.edit()
                                .putBoolean(LockDetector.LEGACY_ANIMATION, checked)
                                .apply());
        updateLockAnimationState(settingsPrefs.getBoolean(LockDetector.PREFERENCE_ENTRY, false));
    }

    @Override
    public void show() {
        super.show();

        checkNotificationPermission();
        checkAccessibilityPermission();

        scroller.scrollTo(0, 0);
    }

    private void checkNotificationPermission() {
        if (!Utils.isNotificationServiceEnabled(activity)) {
            mediaSwitch.setChecked(false);
        }
    }

    private void checkAccessibilityPermission() {
        if (!Utils.isAccessibilityServiceEnabled(activity)) {
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

    private void updateLockAnimationState(boolean enabled) {
        lockAnimContainer.setAlpha(enabled ? 1f : 0.6f);

        if (enabled) {
            lockAnimContainer.setOnClickListener((view) -> lockAnimSwitch.performClick());
        } else {
            lockAnimContainer.setOnClickListener(null);
        }
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

    private String getLocationString() {
        String location = weatherPrefs.getString(Weather.LOCATION_NAME,
                resources.getString(R.string.location_ip_based));

        if (activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && weatherPrefs.getBoolean(Weather.PRECISE_LOCATION, false)) {
            location = resources.getString(R.string.precise_location);
        }

        return location;
    }

    // Permission Dialogs
    private void showNotificationPermissionDialog() {
        NotificationConfigurator dialog = new NotificationConfigurator(activity);

        dialog.setOnDismissListener(d -> checkNotificationPermission());
        dialog.show();
    }

    private void showAccessibilityPermissionDialog() {
        AccessibilityConfigurator dialog = new AccessibilityConfigurator(activity);

        dialog.setOnDismissListener(d -> checkAccessibilityPermission());
        dialog.show();
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

    @Override
    protected boolean blurBehind() {
        return true;
    }

    @Override
    protected int getDesiredInitialState() {
        return BottomSheetBehavior.STATE_HALF_EXPANDED;
    }
}
