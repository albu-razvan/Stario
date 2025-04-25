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

package com.stario.launcher.activities;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.transition.Transition;
import android.transition.TransitionListenerAdapter;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.content.res.ResourcesCompat;

import com.stario.launcher.R;
import com.stario.launcher.activities.settings.Settings;
import com.stario.launcher.apps.ProfileManager;
import com.stario.launcher.glance.Glance;
import com.stario.launcher.glance.extensions.GlanceDialogExtension;
import com.stario.launcher.glance.extensions.calendar.Calendar;
import com.stario.launcher.glance.extensions.media.Media;
import com.stario.launcher.glance.extensions.weather.Weather;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.sheet.SheetType;
import com.stario.launcher.sheet.SheetsFocusController;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.common.lock.ClosingAnimationView;
import com.stario.launcher.ui.common.lock.LockDetector;
import com.stario.launcher.ui.popup.PopupMenu;
import com.stario.launcher.ui.utils.UiUtils;
import com.stario.launcher.ui.utils.animation.WallpaperAnimator;
import com.stario.launcher.utils.Utils;

public class Launcher extends ThemedActivity {
    public final static int MAX_BACKGROUND_ALPHA = 230;
    private BroadcastReceiver screenOnReceiver;
    private SheetsFocusController coordinator;
    private ClosingAnimationView main;
    private LockDetector detector;
    private View decorView;
    private Glance glance;

    public Launcher() {
    }

    @SuppressLint({"ClickableViewAccessibility", "UseCompatLoadingForDrawables"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Vibrations.from(this);
        ProfileManager.from(this);

        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_SPLIT_TOUCH);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.launcher);

        main = findViewById(R.id.main);
        coordinator = findViewById(R.id.coordinator);
        detector = findViewById(R.id.detector);
        decorView = window.getDecorView();

        Measurements.measure(getRoot(), (insets) -> {
            coordinator.setPadding(0, Measurements.getSysUIHeight(), 0,
                    Measurements.getNavHeight() + Measurements.dpToPx(20));

            return insets;
        });

        UiUtils.applyNotchMargin(coordinator);
        coordinator.setOnLongClickListener((v) -> {
            Vibrations.getInstance().vibrate();

            displayLauncherOptions(this);
            return true;
        });

        screenOnReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setShowWhenLocked(false);
                main.reset();
            }
        };

        if (Utils.isMinimumSDK(Build.VERSION_CODES.TIRAMISU)) {
            registerReceiver(screenOnReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON), RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(screenOnReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
        }

        prepareWallpaperTransitions();

        attachSheets();
        attachGlance();
    }

    private void attachGlance() {
        glance = new Glance(this);
        glance.attach(detector);

        GlanceDialogExtension.TransitionListener listener = this::updateWallpaperZoom;

        Calendar calendar = new Calendar();
        glance.attachViewExtension(calendar);
        glance.attachDialogExtension(new Media(), Gravity.BOTTOM, listener);
        glance.attachDialogExtension(new Weather(), Gravity.BOTTOM, listener);
    }

    private void attachSheets() {
        SheetsFocusController.Wrapper.wrapInDialog(this, SheetType.BOTTOM_SHEET, (slideOffset) ->
                animateSheet(SheetType.BOTTOM_SHEET, slideOffset));
        SheetsFocusController.Wrapper.wrapInDialog(this, SheetType.TOP_SHEET, (slideOffset) ->
                animateSheet(SheetType.TOP_SHEET, slideOffset));
        SheetsFocusController.Wrapper.wrapInDialog(this, SheetType.LEFT_SHEET, (slideOffset) ->
                animateSheet(SheetType.LEFT_SHEET, slideOffset));
    }

    public void displayLauncherOptions(Launcher activity) {
        PopupMenu menu = new PopupMenu(activity);

        Resources resources = activity.getResources();

        menu.add(new PopupMenu.Item(resources.getString(R.string.settings),
                ResourcesCompat.getDrawable(resources, R.drawable.ic_settings, activity.getTheme()),
                view -> {
                    menu.dismiss();

                    view.post(() -> {
                        Intent intent = new Intent(activity, Settings.class);

                        activity.startActivity(intent,
                                ActivityOptions.makeSceneTransitionAnimation(activity).toBundle());
                    });
                }));

        menu.add(new PopupMenu.Item(resources.getString(R.string.wallpaper),
                ResourcesCompat.getDrawable(resources, R.drawable.ic_palette, activity.getTheme()),
                view -> {
                    menu.dismiss();

                    view.post(() -> {
                        Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .putExtra("com.android.wallpaper.LAUNCH_SOURCE", "app_launched_launcher");

                        activity.startActivity(intent,
                                ActivityOptions.makeScaleUpAnimation(view, 0, 0,
                                        view.getWidth(), view.getHeight()).toBundle());
                    });
                }));

        menu.showAtLocation(activity, coordinator, coordinator.getLastX(),
                coordinator.getLastY(), PopupMenu.PIVOT_CENTER_HORIZONTAL);
    }

    private void animateSheet(SheetType type, float slideOffset) {
        if (Float.isNaN(slideOffset)) {
            slideOffset = 0;
        }

        float targetAlpha = 1f - slideOffset * 2f;
        slideOffset = slideOffset * slideOffset;

        if (targetAlpha > 0) {
            main.setAlpha((float) Math.sqrt(targetAlpha));
            if (decorView.getWindowToken() != null) {
                if (type == SheetType.TOP_SHEET) {
                    decorView.setTranslationY(slideOffset / 2f * main.getMeasuredHeight());
                    decorView.setTranslationX(0);
                } else if (type == SheetType.LEFT_SHEET) {
                    decorView.setTranslationY(0);
                    decorView.setTranslationX(slideOffset / 2f * main.getMeasuredWidth());
                } else if (type == SheetType.BOTTOM_SHEET) {
                    decorView.setTranslationY(-slideOffset / 2f * main.getMeasuredHeight());
                    decorView.setTranslationX(0);
                } else if (type == SheetType.RIGHT_SHEET) {
                    decorView.setTranslationY(0);
                    decorView.setTranslationX(-slideOffset / 2f * main.getMeasuredWidth());
                } else {
                    decorView.setTranslationY(0);
                    decorView.setTranslationX(0);
                }
            }

            main.setVisibility(View.VISIBLE);
        } else {
            main.setVisibility(View.INVISIBLE);
        }

        updateWallpaperZoom(slideOffset);
    }

    private void updateWallpaperZoom(float zoom) {
        if (decorView.getWindowToken() != null) {
            if (Utils.isMinimumSDK(Build.VERSION_CODES.R)) {
                zoom = Math.max(Math.min(1, zoom), 0);

                WallpaperAnimator.updateZoom(this, zoom);
            }
        }
    }

    private void prepareWallpaperTransitions() {
        Window window = getWindow();

        if (window == null) {
            return;
        }

        Transition enterTransition = window.getEnterTransition();
        if (enterTransition != null) {
            enterTransition.addListener(new TransitionListenerAdapter() {
                @Override
                public void onTransitionStart(Transition transition) {
                    WallpaperAnimator.animateZoom(Launcher.this, null, 0f);
                }
            });
        }

        Transition exitTransition = window.getExitTransition();
        if (exitTransition != null) {
            exitTransition.addListener(new TransitionListenerAdapter() {
                @Override
                public void onTransitionStart(Transition transition) {
                    WallpaperAnimator.animateZoom(Launcher.this, null, 1f);
                }
            });
        }

    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(screenOnReceiver);
        } catch (Exception exception) {
            Log.e("Launcher", "onDestroy: Screen On receiver was not registered.");
        }

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        glance.update();

        super.onResume();

        WallpaperAnimator.animateZoom(this, null, 0f);
    }

    @Override
    protected void onStop() {
        prepareWallpaperTransitions();
        WallpaperAnimator.animateZoom(this, null, 1f);

        super.onStop();

        setShowWhenLocked(false);
        main.reset();
    }

    @Override
    public void requestIgnoreCurrentTouchEvent(boolean enabled) {
        super.requestIgnoreCurrentTouchEvent(enabled);

        SheetsFocusController.Wrapper.requestIgnoreCurrentTouchEvent(enabled);
    }

    @Override
    protected boolean isOpaque() {
        return false;
    }

    @Override
    protected boolean isAffectedByBackGesture() {
        return false;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
    }
}