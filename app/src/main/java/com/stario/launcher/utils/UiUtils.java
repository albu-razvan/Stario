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

package com.stario.launcher.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.google.android.material.transition.platform.MaterialSharedAxis;
import com.stario.launcher.ui.measurements.Measurements;
import com.stario.launcher.utils.animation.Animation;

public class UiUtils {
    private static final Handler UIHandler = new Handler(Looper.getMainLooper());

    public static void setWindowTransitions(Window window) {
        //prepare window for transitions
        window.setAllowEnterTransitionOverlap(true);
        window.setAllowReturnTransitionOverlap(true);

        //window transitions
        MaterialSharedAxis enterTransform = new MaterialSharedAxis(MaterialSharedAxis.Z, false);
        enterTransform.setInterpolator(new FastOutSlowInInterpolator());
        enterTransform.setDuration(Animation.LONG.getDuration());

        window.setEnterTransition(enterTransform);
        window.setReenterTransition(enterTransform);
        window.setReturnTransition(enterTransform);

        MaterialSharedAxis exitTransform = new MaterialSharedAxis(MaterialSharedAxis.Z, true);
        exitTransform.setInterpolator(new AccelerateInterpolator());
        exitTransform.setDuration(Animation.LONG.getDuration());

        window.setExitTransition(exitTransform);
    }

    public static void makeSysUITransparent(Window window) {
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setNavigationBarDividerColor(Color.TRANSPARENT);

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS |
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        if (Utils.isMinimumSDK(29)) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }

        WindowCompat.setDecorFitsSystemWindows(window, false);
    }

    public static void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        view.clearFocus();
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void showKeyboard(View view) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        view.requestFocus();
        inputMethodManager.showSoftInput(view, 0);
    }

    public static void runOnUIThread(Runnable runnable) {
        UIHandler.post(runnable);
    }

    public static void runOnUIThreadDelayed(Runnable runnable, long delay) {
        UIHandler.postDelayed(runnable, delay);
    }

    public static void loopOnUIThread(Runnable runnable, long period, Condition condition) {
        UIHandler.post(new Runnable() {
            public void run() {
                if (condition.evaluate()) {
                    runnable.run();

                    UIHandler.postDelayed(this, period);
                }
            }
        });
    }

    public static void roundViewGroup(ViewGroup view, int radiusDp) {
        view.setClipChildren(true);
        view.setClipToOutline(true);

        view.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(new Rect(0, 0,
                                view.getMeasuredWidth(),
                                view.getMeasuredHeight()),
                        Measurements.dpToPx(radiusDp));
            }
        });
    }

    public static void applyNotchMargin(View view) {
        view.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            int startingMarginLeft = -1;
            int startingMarginRight = -1;

            @NonNull
            @Override
            public WindowInsets onApplyWindowInsets(@NonNull View v, @NonNull WindowInsets insets) {
                Insets compatInset = WindowInsetsCompat.toWindowInsetsCompat(insets)
                        .getInsets(WindowInsetsCompat.Type.displayCutout());

                ViewGroup.LayoutParams params = view.getLayoutParams();

                if (params != null) {
                    ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) params;

                    if (startingMarginLeft == -1 ||
                            startingMarginRight == -1) {
                        startingMarginLeft = marginParams.leftMargin;
                        startingMarginRight = marginParams.rightMargin;
                    }

                    marginParams.leftMargin = startingMarginLeft + compatInset.left;
                    marginParams.rightMargin = startingMarginRight + compatInset.right;

                    view.setLayoutParams(marginParams);
                }

                return insets;
            }
        });
    }

    public interface Condition {
        boolean evaluate();
    }
}
