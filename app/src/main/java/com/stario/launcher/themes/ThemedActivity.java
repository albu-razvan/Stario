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

package com.stario.launcher.themes;

import static com.stario.launcher.themes.Theme.THEME_BLUE;
import static com.stario.launcher.themes.Theme.THEME_DYNAMIC;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Build;
import android.os.Bundle;
import android.transition.Transition;
import android.transition.TransitionListenerAdapter;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;

import androidx.activity.BackEventCompat;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.compose.animation.core.CubicBezierEasing;
import androidx.compose.animation.core.Easing;

import com.stario.launcher.preferences.Entry;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.utils.UiUtils;
import com.stario.launcher.ui.utils.animation.Animation;
import com.stario.launcher.utils.Utils;

import java.util.HashMap;

abstract public class ThemedActivity extends AppCompatActivity {
    public static final String THEME = "com.stario.THEME";
    public static final String FORCE_DARK = "com.stario.FORCE_DARK";

    private final HashMap<Integer, OnActivityResult> activityResultListeners;

    private PaintDrawable roundedCornerBackground;
    private SharedPreferences themePreferences;
    private ValueAnimator backgroundAnimator;
    private ColorDrawable windowBackground;
    private boolean allowTouches;
    private int backgroundColor;
    private Theme theme;

    public ThemedActivity() {
        this.allowTouches = true;
        this.activityResultListeners = new HashMap<>();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        themePreferences = getSharedPreferences(Entry.THEME);

        //default theme if it doesn't exist
        if (!themePreferences.contains(THEME)) {
            if (Utils.isMinimumSDK(Build.VERSION_CODES.R)) {
                themePreferences.edit().putString(THEME, THEME_DYNAMIC.toString()).apply();
            } else {
                themePreferences.edit().putString(THEME, THEME_BLUE.toString()).apply();
            }
        }

        //night mode flags
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkModeOn = nightModeFlags == Configuration.UI_MODE_NIGHT_YES ||
                themePreferences.getBoolean(FORCE_DARK, false);

        theme = Theme.from(themePreferences.getString(THEME, THEME_BLUE.toString()));
        setTheme(isDarkModeOn ? theme.getDarkResourceID() : theme.getLightResourceID());

        backgroundColor = getAttributeData(com.google.android.material.R.attr.colorSurface);

        roundedCornerBackground = new PaintDrawable(backgroundColor);
        roundedCornerBackground.setCornerRadius(Measurements.dpToPx(30));

        Window window = getWindow();

        if (window != null) {
            UiUtils.setWindowTransitions(window);
            UiUtils.makeSysUITransparent(window);

            if (isOpaque()) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
                windowBackground = new ColorDrawable(Color.TRANSPARENT);
            } else {
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);

                windowBackground = new ColorDrawable(backgroundColor);
                windowBackground.setAlpha(0);
            }

            window.setBackgroundDrawable(windowBackground);
            window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);

            if (Utils.isMinimumSDK(Build.VERSION_CODES.R)) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
            }
        }

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    private final Easing cubicBezier;

                    private int startingWindowBackgroundAlpha;
                    private boolean initialized;

                    {
                        this.startingWindowBackgroundAlpha = isOpaque() ? 255 : 0;
                        this.cubicBezier = new CubicBezierEasing(0.43f, 0.1f, -0.2f, 1);
                        this.initialized = false;
                    }

                    private boolean handleInit() {
                        if (initialized) {
                            return true;
                        }

                        if (backgroundAnimator != null && backgroundAnimator.isRunning()) {
                            backgroundAnimator.pause();
                        }

                        View root = getRoot();
                        if (root != null) {
                            root.animate().cancel();

                            startingWindowBackgroundAlpha = isActivityTransitionRunning() ?
                                    (isOpaque() ? 255 : 0) : windowBackground.getAlpha();

                            this.initialized = true;
                            return true;

                        }

                        return false;
                    }

                    @Override
                    public void handleOnBackStarted(@NonNull BackEventCompat backEvent) {
                        if (isAffectedByBackGesture() && !isActivityTransitionRunning()) {
                            handleInit();
                        }
                    }

                    @Override
                    public void handleOnBackProgressed(@NonNull BackEventCompat backEvent) {
                        if (isAffectedByBackGesture() &&
                                !isActivityTransitionRunning() && handleInit()) {
                            View root = getRoot();

                            float progress = cubicBezier.transform(backEvent.getProgress());
                            if (root != null) {
                                windowBackground.setAlpha((int) (Math.max((1f - progress * 2), 0) * 255));

                                root.setPivotY(backEvent.getTouchY() / 1.3f);

                                root.setScaleX(1f - progress * 0.15f);
                                root.setTranslationY((root.getHeight() / 10f) * progress);
                                root.setScaleY(1f - progress * 0.15f);

                                roundedCornerBackground.setCornerRadius(Measurements.dpToPx(10) +
                                        progress * Measurements.dpToPx(20));
                            }
                        }
                    }

                    @Override
                    public void handleOnBackCancelled() {
                        if (!initialized) {
                            return;
                        }

                        if (isAffectedByBackGesture() && !isActivityTransitionRunning()) {
                            View root = getRoot();

                            if (root != null) {
                                int alpha = windowBackground.getAlpha();

                                root.animate()
                                        .scaleX(1)
                                        .scaleY(1)
                                        .setDuration(Animation.MEDIUM.getDuration())
                                        .setUpdateListener(animation -> {
                                            float fraction = animation.getAnimatedFraction();

                                            windowBackground.setAlpha(alpha +
                                                    (int) ((startingWindowBackgroundAlpha - alpha) * fraction));
                                        })
                                        .withEndAction(() ->
                                                windowBackground.setAlpha(startingWindowBackgroundAlpha));
                            }
                        }

                        initialized = false;
                    }

                    @Override
                    public void handleOnBackPressed() {
                        if (initialized) {
                            if (backgroundAnimator != null &&
                                    backgroundAnimator.isRunning()) {
                                backgroundAnimator.pause();
                            }

                            backgroundAnimator = ValueAnimator.ofInt(windowBackground.getAlpha(), 0);
                            backgroundAnimator.setDuration(Animation.BRIEF.getDuration());
                            backgroundAnimator.addUpdateListener(animation ->
                                    windowBackground.setAlpha((int) backgroundAnimator.getAnimatedValue()));
                            backgroundAnimator.start();
                        }

                        if (isAffectedByBackGesture()) {
                            finishAfterTransition();
                        }

                        initialized = false;
                    }
                });

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        Window window = getWindow();

        if (window != null && isOpaque()) {
            getRoot().setBackground(roundedCornerBackground);

            if (isActivityTransitionRunning()) {
                if (backgroundAnimator != null &&
                        backgroundAnimator.isRunning()) {
                    backgroundAnimator.pause();
                }

                backgroundAnimator = ValueAnimator.ofFloat(30, 0);
                backgroundAnimator.setInterpolator(new AccelerateInterpolator(2));
                backgroundAnimator.setDuration((int) (Animation.EXTENDED.getDuration() *
                        Measurements.getTransitionAnimationScale() /
                        Measurements.getAnimatorDurationScale()));
                backgroundAnimator.addUpdateListener(animation -> {
                    roundedCornerBackground.setCornerRadius(
                            Measurements.dpToPx((float) animation.getAnimatedValue()));
                });
                backgroundAnimator.start();

                Transition transition = window.getEnterTransition();
                transition.addListener(new TransitionListenerAdapter() {


                    @Override
                    public void onTransitionEnd(Transition transition) {
                        assignActualBackgroundColor(window);
                        transition.removeListener(this);
                    }

                    @Override
                    public void onTransitionCancel(Transition transition) {
                        assignActualBackgroundColor(window);
                        transition.removeListener(this);
                    }
                });
            } else {
                assignActualBackgroundColor(window);
            }
        }
    }

    private void assignActualBackgroundColor(@NonNull Window window) {
        roundedCornerBackground.setCornerRadius(Measurements.dpToPx(0));
        windowBackground = new ColorDrawable(backgroundColor);

        // If the window background, when set, is completely opaque (alpha 255),
        // The window will treat every alpha value for the wallpaper as black
        windowBackground.setAlpha(0);
        window.setBackgroundDrawable(windowBackground);
        windowBackground.setAlpha(255);
    }

    @Override
    protected void onResume() {
        Window window = getWindow();

        if (window != null) {
            UiUtils.makeSysUITransparent(window);
        }

        super.onResume();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration configuration) {
        if (Measurements.wereTaken()) {
            Measurements.remeasure(getRoot());
        }

        super.onConfigurationChanged(configuration);
    }

    protected ViewGroup getRoot() {
        return (ViewGroup) ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
    }

    //disallow the usage of malformed preference store
    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        if (Entry.isValid(name)) {
            return super.getSharedPreferences(name, mode);
        } else {
            throw new IllegalArgumentException("Name must be declared in " +
                    Entry.class.getCanonicalName());
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return allowTouches && super.dispatchTouchEvent(ev);
    }

    public void setTouchEnabled(boolean enabled) {
        this.allowTouches = enabled;
    }

    public boolean isTouchEnabled() {
        return allowTouches;
    }

    public SharedPreferences getSharedPreferences(Entry entry) {
        return super.getSharedPreferences(entry.toString(), MODE_PRIVATE);
    }

    public SharedPreferences getSharedPreferences(Entry entry, String subPreference) {
        return super.getSharedPreferences(entry.toSubPreference(subPreference), MODE_PRIVATE);
    }

    public SharedPreferences getSettings() {
        return getSharedPreferences(Entry.STARIO);
    }

    public Theme getThemeType() {
        return theme;
    }

    public int getAttributeData(@AttrRes int attr) {
        return getAttributeData(theme, attr);
    }

    public int getAttributeData(@AttrRes int attr, boolean forceDark) {
        return getAttributeData(theme, attr, forceDark);
    }

    public int getAttributeData(@NonNull Theme theme, @AttrRes int attr) {
        return getAttributeData(theme, attr, false);
    }

    public int getAttributeData(@NonNull Theme theme, @AttrRes int attr, boolean forceDark) {
        TypedValue typedValue = new TypedValue();

        Resources.Theme wrappedTheme = getThemeFor(theme, forceDark);
        wrappedTheme.resolveAttribute(attr, typedValue, true);

        return typedValue.data;
    }

    public Resources.Theme getTheme(boolean forceDark) {
        return getThemeFor(theme, forceDark);
    }

    private Resources.Theme getThemeFor(@NonNull Theme theme, boolean forceDark) {
        ContextThemeWrapper wrapper;

        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkModeOn = nightModeFlags == Configuration.UI_MODE_NIGHT_YES ||
                themePreferences.getBoolean(FORCE_DARK, false);
        if (isDarkModeOn) {
            wrapper = new ContextThemeWrapper(getApplicationContext(), theme.getDarkResourceID());
        } else {
            wrapper = new ContextThemeWrapper(getApplicationContext(), theme.getLightResourceID());
        }

        return wrapper.getTheme();
    }

    protected abstract boolean isOpaque();

    protected abstract boolean isAffectedByBackGesture();

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        for (OnActivityResult resultListener : activityResultListeners.values()) {
            if (resultListener != null) {
                resultListener.onResult(resultCode, data);
            }
        }
    }

    public boolean addOnActivityResultListener(int configurationCode, OnActivityResult listener) {
        if (listener != null) {
            return activityResultListeners.putIfAbsent(configurationCode, listener) == null;
        }

        return false;
    }

    public void removeOnActivityResultListener(int configurationCode) {
        activityResultListeners.remove(configurationCode);
    }

    public interface OnActivityResult {
        void onResult(int resultCode, Intent intent);
    }
}
