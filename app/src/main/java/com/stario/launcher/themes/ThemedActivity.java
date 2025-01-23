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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.BackEventCompat;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.stario.launcher.preferences.Entry;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.utils.UiUtils;
import com.stario.launcher.utils.Utils;
import com.stario.launcher.utils.animation.Animation;

import java.util.HashMap;

abstract public class ThemedActivity extends AppCompatActivity {
    public static final String THEME = "com.stario.THEME";
    public static final String FORCE_DARK = "com.stario.FORCE_DARK";
    private final HashMap<Integer, OnActivityResult> activityResultListeners;
    private boolean dispatchTouchEvents;
    private Theme theme;

    public ThemedActivity() {
        this.dispatchTouchEvents = true;
        this.activityResultListeners = new HashMap<>();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SharedPreferences themePreferences = getSharedPreferences(Entry.THEME);

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

        int backgroundColor = getAttributeData(com.google.android.material.R.attr.colorSurface);
        Drawable background = new ColorDrawable(backgroundColor);

        Window window = getWindow();

        if (window != null) {
            UiUtils.setWindowTransitions(window);
            UiUtils.makeSysUITransparent(window);

            if (isOpaque()) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
            } else {
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
                background.setAlpha(0);
            }

            window.setBackgroundDrawable(background);
            window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);

            if (Utils.isMinimumSDK(Build.VERSION_CODES.R)) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
            }
        }

        if (Utils.isMinimumSDK(Build.VERSION_CODES.TIRAMISU)) {
            getOnBackPressedDispatcher().addCallback(this,
                    new OnBackPressedCallback(true) {
                        private int startingWindowBackgroundAlpha = isOpaque() ? 255 : 0;
                        private Drawable startingRootBackground;
                        private PaintDrawable progressRootBackground;

                        @Override
                        public void handleOnBackStarted(@NonNull BackEventCompat backEvent) {
                            if (isAffectedByBackGesture()) {
                                View root = getRoot();

                                if (root != null) {
                                    startingWindowBackgroundAlpha = isActivityTransitionRunning() ?
                                            (isOpaque() ? 255 : 0) : background.getAlpha();

                                    startingRootBackground = root.getBackground();
                                    progressRootBackground = new PaintDrawable(backgroundColor);
                                    progressRootBackground.setCornerRadius(Measurements.dpToPx(10));

                                    root.setBackground(progressRootBackground);

                                    if (backEvent.getSwipeEdge() == BackEventCompat.EDGE_RIGHT) {
                                        root.setPivotX(root.getMeasuredWidth() * 0.25f);
                                    } else if (backEvent.getSwipeEdge() == BackEventCompat.EDGE_LEFT) {
                                        root.setPivotX(root.getMeasuredWidth() * 0.75f);
                                    }
                                }
                            }
                        }

                        @Override
                        public void handleOnBackProgressed(@NonNull BackEventCompat backEvent) {
                            if (isAffectedByBackGesture()) {
                                View root = getRoot();

                                float progress = backEvent.getProgress();

                                if (root != null) {
                                    background.setAlpha((int) ((1f - progress * 0.5f) * 255));

                                    root.setPivotY(backEvent.getTouchY() / 1.3f);

                                    root.setScaleX(1f - progress * 0.15f);
                                    root.setScaleY(1f - progress * 0.15f);

                                    progressRootBackground.setCornerRadius(Measurements.dpToPx(10) +
                                            progress * Measurements.dpToPx(20));
                                }
                            }
                        }

                        @Override
                        public void handleOnBackCancelled() {
                            if (isAffectedByBackGesture()) {
                                View root = getRoot();

                                if (root != null) {
                                    int alpha = background.getAlpha();

                                    root.animate()
                                            .scaleX(1)
                                            .scaleY(1)
                                            .setDuration(Animation.MEDIUM.getDuration())
                                            .setUpdateListener(animation -> {
                                                float fraction = animation.getAnimatedFraction();

                                                background.setAlpha(alpha +
                                                        (int) ((startingWindowBackgroundAlpha - alpha) * fraction));
                                            })
                                            .withEndAction(() -> {
                                                root.setBackground(startingRootBackground);
                                                background.setAlpha(startingWindowBackgroundAlpha);
                                            });
                                }
                            }
                        }

                        @Override
                        public void handleOnBackPressed() {
                            if (isAffectedByBackGesture()) {
                                finishAfterTransition();
                            }
                        }
                    });
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            dispatchTouchEvents = true;
        }

        boolean result = dispatchTouchEvents && super.dispatchTouchEvent(event);

        if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_CANCEL) {
            dispatchTouchEvents = true;
        }

        return result;
    }

    public void requestIgnoreCurrentTouchEvent(boolean enabled) {
        dispatchTouchEvents = enabled;
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

    public SharedPreferences getSharedPreferences(Entry entry) {
        return super.getSharedPreferences(entry.toString(), MODE_PRIVATE);
    }

    public SharedPreferences getSettings() {
        return getSharedPreferences(Entry.STARIO);
    }

    public Resources.Theme getTheme(boolean forceDark) {
        if (forceDark && theme != null) {
            ContextThemeWrapper wrapper = new ContextThemeWrapper(getApplicationContext(), theme.getDarkResourceID());

            return wrapper.getTheme();
        }

        return getTheme();
    }

    public int getAttributeData(@AttrRes int attr) {
        return getAttributeData(attr, false);
    }

    public int getAttributeData(@AttrRes int attr, boolean forceDark) {
        TypedValue typedValue = new TypedValue();

        Resources.Theme theme = getTheme(forceDark);
        theme.resolveAttribute(attr, typedValue, true);

        return typedValue.data;
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
