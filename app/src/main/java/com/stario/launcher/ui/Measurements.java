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

package com.stario.launcher.ui;

import android.app.Activity;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.stario.launcher.utils.Utils;
import com.stario.launcher.utils.objects.ObservableObject;

public class Measurements {
    public static final int HEADER_SIZE_DP = 250;
    private static final ObservableObject<Integer> NAV_HEIGHT = new ObservableObject<>(0);
    private static final ObservableObject<Integer> SYS_UI_HEIGHT = new ObservableObject<>(0);
    private static final ObservableObject<Integer> LIST_COLUMNS = new ObservableObject<>(0);
    private static final ObservableObject<Integer> FOLDER_COLUMNS = new ObservableObject<>(0);
    private static final ObservableObject<Integer> WIDGET_COLUMNS = new ObservableObject<>(0);
    private static boolean measured = false;
    private static OnMeasureRoot listener;
    private static int defaultPadding;
    private static int width;
    private static int height;
    private static float dp;
    private static int dpi;
    private static float sp;

    public static void measure(@NonNull View root, OnMeasureRoot onMeasureListener) {
        listener = onMeasureListener;

        measure(root);
    }

    public static void remeasure(View root) {
        if (root != null && measured) {
            measure(root);
        } else {
            throw new RuntimeException("remeasure() should not be called without a prior measure() call.");
        }
    }

    private static void measure(View root) {
        Activity activity = (Activity) root.getContext();
        DisplayMetrics displayMetrics = activity.getResources()
                .getDisplayMetrics();

        dp = displayMetrics.density;
        dpi = displayMetrics.densityDpi;
        sp = displayMetrics.scaledDensity;

        width = displayMetrics.widthPixels;
        height = displayMetrics.heightPixels;

        defaultPadding = dpToPx(20);

        LIST_COLUMNS.updateObject(Math.min(6, width / dpToPx(90)));
        FOLDER_COLUMNS.updateObject(width / dpToPx(180));

        //ensure that the number of columns is a multiple of 2
        WIDGET_COLUMNS.updateObject((width / dpToPx(150) / 2) * 2);

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Utils.isMinimumSDK(Build.VERSION_CODES.R)) {
                SYS_UI_HEIGHT.updateObject(insets
                        .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()).top);

                boolean isKeyboardVisible = insets.isVisible(WindowInsets.Type.ime());

                if (isKeyboardVisible && ((activity.getWindow().getAttributes().softInputMode &
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
                        != WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)) {

                    NAV_HEIGHT.updateObject(insets
                            .getInsets(WindowInsets.Type.ime()).bottom);
                } else {
                    NAV_HEIGHT.updateObject(insets
                            .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()).bottom);
                }
            } else {
                SYS_UI_HEIGHT.updateObject(insets.getSystemWindowInsetTop());
                NAV_HEIGHT.updateObject(insets.getSystemWindowInsetBottom());
            }

            return listener.onMeasure(insets);
        });

        root.requestApplyInsets();

        measured = true;
    }

    public static int getIconSize() {
        return dpToPx(60);
    }

    public static int dpToPx(float value) {
        return (int) (dp * value);
    }

    public static float getDensity() {
        return dp;
    }

    public static int spToPx(float value) {
        return (int) (sp * value);
    }

    public static float getScaledDensity() {
        return sp;
    }

    public static int getDotsPerInch() {
        return dpi;
    }

    public static int getWidth() {
        return width;
    }

    public static int getHeight() {
        return height;
    }

    public static int getDefaultPadding() {
        return defaultPadding;
    }

    public static int getNavHeight() {
        return NAV_HEIGHT.getObject();
    }

    public static int getSysUIHeight() {
        return SYS_UI_HEIGHT.getObject();
    }

    public static int getListColumnCount() {
        return LIST_COLUMNS.getObject();
    }

    public static int getFolderColumnCount() {
        return FOLDER_COLUMNS.getObject();
    }

    public static int getWidgetColumnCount() {
        return WIDGET_COLUMNS.getObject();
    }

    public static void addListColumnCountChangeListener(ObservableObject.OnSet<Integer> listener) {
        if (listener != null) {
            LIST_COLUMNS.addListener(listener);

            listener.onSet(LIST_COLUMNS.getObject());
        }
    }

    public static void addFolderColumnCountChangeListener(ObservableObject.OnSet<Integer> listener) {
        if (listener != null) {
            FOLDER_COLUMNS.addListener(listener);

            listener.onSet(FOLDER_COLUMNS.getObject());
        }
    }

    public static void addWidgetColumnCountChangeListener(ObservableObject.OnSet<Integer> listener) {
        if (listener != null) {
            WIDGET_COLUMNS.addListener(listener);

            listener.onSet(WIDGET_COLUMNS.getObject());
        }
    }

    public static void addSysUIListener(ObservableObject.OnSet<Integer> listener) {
        if (listener != null) {
            SYS_UI_HEIGHT.addListener(listener);

            listener.onSet(SYS_UI_HEIGHT.getObject());
        }
    }

    public static void addNavListener(ObservableObject.OnSet<Integer> listener) {
        if (listener != null) {
            NAV_HEIGHT.addListener(listener);

            listener.onSet(NAV_HEIGHT.getObject());
        }
    }

    public static boolean wereTaken() {
        return measured;
    }

    public static boolean isLandscape() {
        return width > height;
    }

    public interface OnMeasureRoot {
        WindowInsets onMeasure(WindowInsets insets);
    }
}
