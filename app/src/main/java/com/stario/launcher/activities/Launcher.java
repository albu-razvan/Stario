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

package com.stario.launcher.activities;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.stario.launcher.R;
import com.stario.launcher.apps.LauncherApplicationManager;
import com.stario.launcher.glance.Glance;
import com.stario.launcher.glance.extensions.GlanceDialogExtension;
import com.stario.launcher.glance.extensions.calendar.Calendar;
import com.stario.launcher.glance.extensions.media.Media;
import com.stario.launcher.glance.extensions.weather.Weather;
import com.stario.launcher.hidden.WallpaperManagerHidden;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.sheet.SheetType;
import com.stario.launcher.sheet.SheetWrapper;
import com.stario.launcher.sheet.SheetsFocusController;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.lock.ClosingAnimationView;
import com.stario.launcher.ui.measurements.Measurements;
import com.stario.launcher.utils.UiUtils;
import com.stario.launcher.utils.Utils;

import dev.rikka.tools.refine.Refine;

public class Launcher extends ThemedActivity {
    public final static int MAX_BACKGROUND_ALPHA = 230;
    private WallpaperManagerHidden wallpaperManager;
    private SheetsFocusController coordinator;
    private ClosingAnimationView main;
    private View decorView;
    private Glance glance;

    public Launcher() {
    }

    @SuppressLint({"ClickableViewAccessibility", "UseCompatLoadingForDrawables"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_SPLIT_TOUCH);

        Vibrations.from(this);
        LauncherApplicationManager.from(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.launcher);

        main = findViewById(R.id.main);
        coordinator = findViewById(R.id.coordinator);
        decorView = window.getDecorView();

        if (Utils.isMinimumSDK(Build.VERSION_CODES.R)) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        }

        Measurements.measure(getRoot(), (insets) -> {
            coordinator.setPadding(0, Measurements.getSysUIHeight(), 0,
                    Measurements.getNavHeight() + Measurements.dpToPx(20));

            return insets;
        });

        UiUtils.applyNotchMargin(coordinator);

        wallpaperManager = Refine.unsafeCast(
                WallpaperManagerHidden.getInstance(Launcher.this)
        );

        attachSheets();
        attachGlance();

        main.post(() -> updateWallpaperZoom(0));
    }

    private void attachGlance() {
        glance = new Glance(this);

        glance.attach(findViewById(R.id.detector));

        GlanceDialogExtension.TransitionListener listener = this::updateWallpaperZoom;
        glance.attachViewExtension(new Calendar());
        glance.attachDialogExtension(new Media(), Gravity.BOTTOM, listener);
        glance.attachDialogExtension(new Weather(), Gravity.BOTTOM, listener);
    }

    private void attachSheets() {
        // load sheets
        SheetWrapper.wrapInDialog(this, SheetType.BOTTOM_SHEET, (slideOffset) ->
                animateSheet(SheetType.BOTTOM_SHEET, slideOffset * slideOffset, wallpaperManager));
        SheetWrapper.wrapInDialog(this, SheetType.TOP_SHEET, (slideOffset) ->
                animateSheet(SheetType.TOP_SHEET, slideOffset * slideOffset, wallpaperManager));
        SheetWrapper.wrapInDialog(this, SheetType.LEFT_SHEET, (slideOffset) ->
                animateSheet(SheetType.LEFT_SHEET, slideOffset * slideOffset, wallpaperManager));
        /*SheetWrapper.wrapInDialog(this, SheetType.RIGHT_SHEET, (slideOffset) ->
                animateSheet(SheetType.RIGHT_SHEET, coordinator, slideOffset * slideOffset, wallpaperManager));*/
    }

    private void animateSheet(SheetType type, float slideOffset,
                              WallpaperManagerHidden wallpaperManager) {
        slideOffset = Math.min(1, Math.max(0, slideOffset));
        if (Float.isNaN(slideOffset)) {
            slideOffset = 0;
        }

        main.setAlpha(1f - slideOffset * 1.5f);
        main.setScaleX(1f - slideOffset * slideOffset / 5f);
        main.setScaleY(1f - slideOffset * slideOffset / 5f);

        if (decorView.getWindowToken() != null) {
            IBinder windowToken = decorView.getWindowToken();

            if (type == SheetType.TOP_SHEET) {
                wallpaperManager.setWallpaperOffsets(windowToken, 0.5f, 0.5f - slideOffset / 10);

                decorView.setTranslationY(slideOffset / 2f * main.getMeasuredHeight());
                decorView.setTranslationX(0);
            } else if (type == SheetType.LEFT_SHEET) {
                wallpaperManager.setWallpaperOffsets(windowToken, 0.5f - slideOffset / 10, 0.5f);

                decorView.setTranslationY(0);
                decorView.setTranslationX(slideOffset / 2f * main.getMeasuredWidth());
            } else if (type == SheetType.BOTTOM_SHEET) {
                wallpaperManager.setWallpaperOffsets(windowToken, 0.5f, 0.5f + slideOffset / 10);

                decorView.setTranslationY(-slideOffset / 2f * main.getMeasuredHeight());
                decorView.setTranslationX(0);
            } else if (type == SheetType.RIGHT_SHEET) {
                wallpaperManager.setWallpaperOffsets(windowToken, 0.5f + slideOffset / 10, 0.5f);

                decorView.setTranslationY(0);
                decorView.setTranslationX(-slideOffset / 2f * main.getMeasuredWidth());
            } else {
                wallpaperManager.setWallpaperOffsets(windowToken, 0.5f, 0.5f);

                decorView.setTranslationY(0);
                decorView.setTranslationX(0);
            }

            updateWallpaperZoom(slideOffset);
        }
    }

    private void updateWallpaperZoom(float zoom) {
        if (decorView.getWindowToken() != null) {
            IBinder windowToken = decorView.getWindowToken();

            if (Utils.isMinimumSDK(Build.VERSION_CODES.R)) {
                zoom = Math.max(Math.min(1, zoom), 0);

                wallpaperManager.setWallpaperZoomOut(windowToken, zoom);
            }
        }
    }

    @Override
    protected void onResume() {
        glance.update();

        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();

        main.reset();
    }

    @Override
    public void requestIgnoreCurrentTouchEvent(boolean enabled) {
        super.requestIgnoreCurrentTouchEvent(enabled);

        SheetWrapper.requestIgnoreCurrentTouchEvent(enabled);
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