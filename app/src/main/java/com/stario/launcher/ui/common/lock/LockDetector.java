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

package com.stario.launcher.ui.common.lock;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stario.launcher.BuildConfig;
import com.stario.launcher.activities.launcher.Launcher;
import com.stario.launcher.services.AccessibilityService;

public class LockDetector extends LinearLayout {
    public static final String PREFERENCE_ENTRY = "com.stario.LockDetector.LOCK";
    public static final String LEGACY_ANIMATION = "com.stario.LockDetector.LEGACY_LOCK_ANIMATION";

    private DoubleTapDetector detector;
    private boolean shouldIntercept;

    public LockDetector(@NonNull Context context) {
        super(context);

        init(context);
    }

    public LockDetector(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public LockDetector(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    private void init(Context context) {
        if (!(context instanceof Launcher)) {
            throw new RuntimeException("LockDetector needs the Launcher context. (Is this view used in an activity other than Launcher.java?)");
        }

        detector = new DoubleTapDetector(((Launcher) context));
        shouldIntercept = false;
        setHapticFeedbackEnabled(false);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        shouldIntercept = false;

        if (detector != null && detector.onTouchEvent(event)) {
            shouldIntercept = true;
            requestDisallowInterceptTouchEvent(true);

            return true;
        }

        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return shouldIntercept || super.onTouchEvent(event);
    }

    class DoubleTapDetector {
        private static final long NOT_REGISTERED = -1;

        private final SharedPreferences preferences;
        private final Activity activity;

        private long lastEventTime;

        private DoubleTapDetector(Launcher activity) {
            this.activity = activity;
            this.preferences = activity.getApplicationContext().getSettings();
            this.lastEventTime = NOT_REGISTERED;
        }

        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_DOWN) {
                return false;
            }

            long currentTime = System.currentTimeMillis();

            if (lastEventTime > 0 &&
                    currentTime - lastEventTime < ViewConfiguration.getDoubleTapTimeout()) {
                if (isAccessibilitySettingsOn(activity) &&
                        preferences.getBoolean(PREFERENCE_ENTRY, false)) {

                    if (!preferences.getBoolean(LEGACY_ANIMATION, false)) {
                        getClosingAnimationView()
                                .closeTo(event.getRawX(), event.getRawY(),
                                        () -> sleep(activity));
                    } else {
                        sleep(activity);
                    }

                    lastEventTime = NOT_REGISTERED;
                    return true;
                }

                lastEventTime = NOT_REGISTERED;
                return false;
            }

            lastEventTime = currentTime;
            return false;
        }

        private ClosingAnimationView getClosingAnimationView() {
            View view = LockDetector.this;

            while (view != null) {
                if (view instanceof ClosingAnimationView) {
                    return (ClosingAnimationView) view;
                }

                ViewParent parent = view.getParent();

                if (parent instanceof View) {
                    view = (View) parent;
                } else {
                    view = null;
                }
            }

            throw new RuntimeException("This view must be a child of " +
                    ClosingAnimationView.class.getName());
        }

        private void sleep(Context context) {
            AccessibilityManager manager =
                    (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
            AccessibilityEvent event = AccessibilityEvent.obtain();

            event.setEventType(AccessibilityEvent.TYPE_ANNOUNCEMENT);
            event.setClassName(getClass().getName());
            event.setAction(AccessibilityService.LOCK);

            activity.setShowWhenLocked(true);
            manager.sendAccessibilityEvent(event);
        }

        private boolean isAccessibilitySettingsOn(Context context) {
            int accessibilityEnabled = 0;

            final String service = BuildConfig.APPLICATION_ID + "/" +
                    AccessibilityService.class.getCanonicalName();
            try {
                accessibilityEnabled = android.provider.Settings.Secure.getInt(context.getContentResolver(),
                        android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
            } catch (android.provider.Settings.SettingNotFoundException exception) {
                Log.e("", "Error finding setting, default accessibility to not found: " +
                        exception.getMessage());
            }

            TextUtils.SimpleStringSplitter stringColonSplitter =
                    new TextUtils.SimpleStringSplitter(':');

            if (accessibilityEnabled == 1) {
                String settingValue = android.provider.Settings.Secure.getString(context.getContentResolver(),
                        android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

                if (settingValue != null) {
                    stringColonSplitter.setString(settingValue);

                    while (stringColonSplitter.hasNext()) {
                        String accessibilityService = stringColonSplitter.next();
                        if (accessibilityService.equalsIgnoreCase(service)) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }
    }
}
