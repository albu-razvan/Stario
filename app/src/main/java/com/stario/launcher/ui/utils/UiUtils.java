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

package com.stario.launcher.ui.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.transition.Fade;
import android.transition.TransitionSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.google.android.material.transition.platform.MaterialSharedAxis;
import com.stario.launcher.R;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.utils.animation.Animation;
import com.stario.launcher.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class UiUtils {
    private static final Handler UIHandler = new Handler(Looper.getMainLooper());

    public static void enforceLightSystemUI(Window window) {
        View decor = window.getDecorView();

        int flags = decor.getSystemUiVisibility();
        flags &= ~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        decor.setSystemUiVisibility(flags);
    }

    public static void setWindowTransitions(Window window) {
        window.setAllowEnterTransitionOverlap(true);
        window.setAllowReturnTransitionOverlap(true);

        //window transitions
        TransitionSet enterTransition = new TransitionSet();
        enterTransition.addTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, false));
        enterTransition.setInterpolator(new FastOutSlowInInterpolator());
        enterTransition.setDuration(Animation.EXTENDED.getDuration());

        enterTransition.excludeTarget(R.id.navigation_bar_contrast, true);
        enterTransition.excludeTarget(R.id.status_bar_contrast, true);

        window.setEnterTransition(enterTransition);
        window.setReenterTransition(enterTransition);
        window.setReturnTransition(enterTransition);

        TransitionSet exitTransition = new TransitionSet();
        exitTransition.addTransition(new Fade());
        exitTransition.setInterpolator(new AccelerateInterpolator());
        exitTransition.setDuration(Animation.SHORT.getDuration());

        exitTransition.excludeTarget(R.id.navigation_bar_contrast, true);
        exitTransition.excludeTarget(R.id.status_bar_contrast, true);

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

    // https://stackoverflow.com/a/54198408
    public static void expandStatusBar(Context context) {
        if (context == null) {
            return;
        }

        try {
            @SuppressLint("WrongConstant") Object service =
                    context.getSystemService("statusbar");
            Class<?> statusBarManager = Class.forName("android.app.StatusBarManager");

            //noinspection JavaReflectionMemberAccess
            statusBarManager.getMethod("expandNotificationsPanel").invoke(service);
        } catch (Exception exception) {
            Log.e("UiUtils", "expandStatusBar: Could not expand the status bar.", exception);
        }
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

    public static void removeOnUIThreadCallback(Runnable runnable) {
        UIHandler.removeCallbacks(runnable);
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

    public interface Condition {
        boolean evaluate();
    }

    public static class Notch {
        public static final int DEFAULT = 0;
        public static final int CENTER = 1;
        public static final int INVERSE = 2;

        @IntDef({
                DEFAULT,
                CENTER,
                INVERSE
        })

        @Retention(RetentionPolicy.SOURCE)
        public @interface NotchTreatment {
        }

        public static void applyNotchMargin(@NonNull View view) {
            applyNotchMargin(view, DEFAULT);
        }

        public static void applyNotchMargin(@NonNull View view, @NotchTreatment int treatment) {
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

                        if (treatment == CENTER) {
                            int margin = Math.max(startingMarginLeft + cutoutInsets.left + navigationInsets.left,
                                    startingMarginRight + cutoutInsets.right + navigationInsets.right);

                            marginParams.leftMargin = margin;
                            marginParams.rightMargin = margin;
                        } else if (treatment == INVERSE) {
                            marginParams.leftMargin = startingMarginLeft + cutoutInsets.right + navigationInsets.right;
                            marginParams.rightMargin = startingMarginRight + cutoutInsets.left + navigationInsets.left;
                        } else { // DEFAULT
                            marginParams.leftMargin = startingMarginLeft + cutoutInsets.left + navigationInsets.left;
                            marginParams.rightMargin = startingMarginRight + cutoutInsets.right + navigationInsets.right;
                        }

                        view.requestLayout();
                    }

                    v.onApplyWindowInsets(insets);

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
    }
}
