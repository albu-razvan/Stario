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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.stario.launcher.R;
import com.stario.launcher.glance.Glance;
import com.stario.launcher.glance.extensions.GlanceDialogExtensionType;
import com.stario.launcher.glance.extensions.GlanceViewExtensionType;
import com.stario.launcher.hidden.WallpaperManagerHidden;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.sheet.SheetType;
import com.stario.launcher.sheet.SheetWrapper;
import com.stario.launcher.sheet.SheetsFocusController;
import com.stario.launcher.sheet.drawer.apps.LauncherApplicationManager;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.lock.ClosingAnimationView;
import com.stario.launcher.ui.measurements.Measurements;
import com.stario.launcher.utils.UiUtils;
import com.stario.launcher.utils.Utils;

import dev.rikka.tools.refine.Refine;

public class Launcher extends ThemedActivity {
    public final static int MAX_BACKGROUND_ALPHA = 230;
    private static final String TAG = "Launcher";
    private SheetsFocusController coordinator;
    private BroadcastReceiver reorderReceiver;
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

        Measurements.measure(getRoot(), (insets) -> {
            coordinator.setPadding(0, Measurements.getSysUIHeight(), 0,
                    Measurements.getNavHeight() + Measurements.dpToPx(20));

            return insets;
        });

        UiUtils.applyNotchMargin(coordinator);

        attachSheets(Refine.unsafeCast(
                WallpaperManagerHidden.getInstance(Launcher.this)
        ));

        attachGlance();
        registerReorderReceiver();
    }

    private void attachGlance() {
        glance = new Glance(this);

        glance.attach(findViewById(R.id.detector));

        glance.attachViewExtension(GlanceViewExtensionType.CALENDAR);
        glance.attachDialogExtension(GlanceDialogExtensionType.MEDIA_PLAYER, Gravity.BOTTOM);
        glance.attachDialogExtension(GlanceDialogExtensionType.WEATHER, Gravity.BOTTOM);
    }

    private void registerReorderReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);

        reorderReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Intent reorder = new Intent(Launcher.this, Launcher.class);
                reorder.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT |
                        Intent.FLAG_ACTIVITY_NO_ANIMATION);

                startActivity(reorder);
            }
        };

        registerReceiver(reorderReceiver, filter);
    }

    private void attachSheets(WallpaperManagerHidden wallpaperManager) {
        // load sheets
        SheetWrapper.wrapInDialog(this, SheetType.BOTTOM_SHEET, (slideOffset) ->
                animateSheet(SheetType.BOTTOM_SHEET, slideOffset * slideOffset, wallpaperManager));
        SheetWrapper.wrapInDialog(this, SheetType.TOP_SHEET, (slideOffset) ->
                animateSheet(SheetType.TOP_SHEET, slideOffset * slideOffset, wallpaperManager));
        SheetWrapper.wrapInDialog(this, SheetType.LEFT_SHEET, (slideOffset) ->
                animateSheet(SheetType.LEFT_SHEET, slideOffset * slideOffset, wallpaperManager));
        /*SheetWrapper.wrapInDialog(this, SheetType.RIGHT_SHEET, (slideOffset) ->
                animateSheet(SheetType.RIGHT_SHEET, coordinator, slideOffset * slideOffset, wallpaperManager));*/

        animateSheet(SheetType.UNDEFINED, 0, wallpaperManager);
    }

    private void animateSheet(SheetType type, float slideOffset,
                              WallpaperManagerHidden wallpaperManager) {
        slideOffset = Math.min(1, Math.max(0, slideOffset));
        if (Float.isNaN(slideOffset)) {
            slideOffset = 0;
        }

        Drawable background = decorView.getBackground();
        background.setAlpha((int) (MAX_BACKGROUND_ALPHA * slideOffset));

        coordinator.setAlpha(1f - slideOffset * 1.5f);
        coordinator.setScaleX(1f - slideOffset / 5f);
        coordinator.setScaleY(1f - slideOffset / 5f);

        if (decorView.getWindowToken() != null) {
            IBinder windowToken = decorView.getWindowToken();

            float xOffset, yOffset;

            if (type == SheetType.TOP_SHEET) {
                xOffset = 0.5f;
                yOffset = 0.5f - slideOffset / 10;
            } else if (type == SheetType.LEFT_SHEET) {
                xOffset = 0.5f - slideOffset / 10;
                yOffset = 0.5f;
            } else if (type == SheetType.BOTTOM_SHEET) {
                xOffset = 0.5f;
                yOffset = 0.5f + slideOffset / 10;
            } else if (type == SheetType.RIGHT_SHEET) {
                xOffset = 0.5f + slideOffset / 10;
                yOffset = 0.5f;
            } else {
                xOffset = 0.5f;
                yOffset = 0.5f;
            }

            wallpaperManager.setWallpaperOffsets(windowToken, xOffset, yOffset);

            if (Utils.isMinimumSDK(30)) {
                slideOffset = Math.min(1, slideOffset);

                wallpaperManager.setWallpaperZoomOut(windowToken, slideOffset);
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
    protected void onDestroy() {
        try {
            unregisterReceiver(reorderReceiver);
        } catch (Exception exception) {
            Log.e(TAG, "onDestroy: Lock receiver not registered.");
        }

        super.onDestroy();
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

    @SuppressWarnings("deprecation")
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
    }
}