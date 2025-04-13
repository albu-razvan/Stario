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

package com.stario.launcher.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.transition.TransitionSet;
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
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.google.android.material.transition.platform.MaterialSharedAxis;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.utils.animation.Animation;

public class UiUtils {
    private static final Handler UIHandler = new Handler(Looper.getMainLooper());

    // TODO add wallpaper zoom transition
    public static void setWindowTransitions(Window window) {
        window.setAllowEnterTransitionOverlap(true);
        window.setAllowReturnTransitionOverlap(true);

        //window transitions
        TransitionSet enterTransition = new TransitionSet();
        enterTransition.addTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, false));
        enterTransition.setInterpolator(new FastOutSlowInInterpolator());
        enterTransition.setDuration(Animation.LONG.getDuration());

        window.setEnterTransition(enterTransition);
        window.setReenterTransition(enterTransition);
        window.setReturnTransition(enterTransition);

        TransitionSet exitTransition = new TransitionSet();
        exitTransition.addTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, false));
        exitTransition.setInterpolator(new AccelerateInterpolator());
        exitTransition.setDuration(Animation.LONG.getDuration());

        window.setExitTransition(exitTransition);
    }

    public static void makeSysUITransparent(Window window) {
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setNavigationBarDividerColor(Color.TRANSPARENT);

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS |
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        if (Utils.isMinimumSDK(Build.VERSION_CODES.Q)) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }

        WindowCompat.setDecorFitsSystemWindows(window, false);
    }

    public static boolean areTransitionsOn(Context context) {
        return Settings.Global.getFloat(context.getContentResolver(),
                Settings.Global.TRANSITION_ANIMATION_SCALE, 0) > 0;
    }

    public static boolean areWindowAnimationsOn(Context context) {
        return Settings.Global.getFloat(context.getContentResolver(),
                Settings.Global.WINDOW_ANIMATION_SCALE, 0) > 0;
    }

    public static boolean isKeyboardVisible(View view) {
        if (view != null) {
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(view);

            if (insets != null) {
                return insets.isVisible(WindowInsetsCompat.Type.ime());
            }
        }

        return false;
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

    public static void applyNotchMargin(@NonNull View view) {
        applyNotchMargin(view, false);
    }

    public static void applyNotchMargin(@NonNull View view, boolean center) {
        View.OnApplyWindowInsetsListener listener = new View.OnApplyWindowInsetsListener() {
            Integer startingMarginLeft = null;
            Integer startingMarginRight = null;

            @NonNull
            @Override
            public WindowInsets onApplyWindowInsets(@NonNull View v, @NonNull WindowInsets insets) {
                WindowInsetsCompat compatInset = WindowInsetsCompat.toWindowInsetsCompat(insets);
                Insets cutoutInsets = compatInset.getInsets(WindowInsetsCompat.Type.displayCutout());
                Insets navigationInsets = compatInset.getInsets(WindowInsetsCompat.Type.navigationBars());

                ViewGroup.LayoutParams params = view.getLayoutParams();

                if (params != null) {
                    ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) params;

                    if (startingMarginLeft == null ||
                            startingMarginRight == null) {
                        startingMarginLeft = marginParams.leftMargin;
                        startingMarginRight = marginParams.rightMargin;
                    }

                    if (center) {
                        int margin = Math.max(startingMarginLeft + cutoutInsets.left + navigationInsets.left,
                                startingMarginRight + cutoutInsets.right + navigationInsets.right);

                        marginParams.leftMargin = margin;
                        marginParams.rightMargin = margin;
                    } else {
                        marginParams.leftMargin = startingMarginLeft + cutoutInsets.left + navigationInsets.left;
                        marginParams.rightMargin = startingMarginRight + cutoutInsets.right + navigationInsets.right;
                    }

                    view.requestLayout();
                }

                return insets;
            }
        };

        view.setOnApplyWindowInsetsListener(listener);

        if (view.isAttachedToWindow()) {
            if (view.getRootWindowInsets() != null) {
                listener.onApplyWindowInsets(view, view.getRootWindowInsets());
            }
        } else {
            view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(@NonNull View view) {
                    if (view.getRootWindowInsets() != null) {
                        listener.onApplyWindowInsets(view, view.getRootWindowInsets());
                    }

                    view.removeOnAttachStateChangeListener(this);
                }

                @Override
                public void onViewDetachedFromWindow(@NonNull View view) {
                    view.removeOnAttachStateChangeListener(this);
                }
            });
        }
    }

    public interface Condition {
        boolean evaluate();
    }
}
