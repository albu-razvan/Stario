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

package com.stario.launcher.themes;

import static com.stario.launcher.themes.Theme.THEME_BLUE;
import static com.stario.launcher.themes.Theme.THEME_DYNAMIC;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.stario.launcher.preferences.Entry;
import com.stario.launcher.ui.measurements.Measurements;
import com.stario.launcher.utils.UiUtils;
import com.stario.launcher.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;

abstract public class ThemedActivity extends AppCompatActivity {
    public static final String THEME = "com.stario.THEME";
    public static final String FORCE_DARK = "com.stario.FORCE_DARK";
    private final ArrayList<OnDestroyListener> destroyListeners;
    private HashMap<Integer, OnActivityResult> activityResultListeners;
    private boolean dispatchTouchEvents;
    private Theme theme;

    public ThemedActivity() {
        this.dispatchTouchEvents = true;
        this.activityResultListeners = new HashMap<>();
        this.destroyListeners = new ArrayList<>();
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

        Window window = getWindow();

        if (window != null) {
            UiUtils.setWindowTransitions(window);
            UiUtils.makeSysUITransparent(window);

            Drawable background = new ColorDrawable(
                    getAttributeData(com.google.android.material.R.attr.colorSurface, false)
            );

            if (isOpaque()) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
            } else {
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
                background.setAlpha(0);
            }

            window.setBackgroundDrawable(background);
            window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
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

    @Override
    protected void onDestroy() {
        for (OnDestroyListener listener : destroyListeners) {
            if (listener != null) {
                listener.onDestroy();
            }
        }

        destroyListeners.clear();

        super.onDestroy();
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

    public void addDestroyListener(OnDestroyListener listener) {
        destroyListeners.add(listener);
    }

    protected abstract boolean isOpaque();

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

    public interface OnDestroyListener {
        void onDestroy();
    }

    public interface OnActivityResult {
        void onResult(int resultCode, Intent intent);
    }
}
