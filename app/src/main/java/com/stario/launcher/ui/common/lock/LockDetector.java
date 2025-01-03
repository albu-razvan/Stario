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

package com.stario.launcher.ui.common.lock;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.stario.launcher.BuildConfig;
import com.stario.launcher.R;
import com.stario.launcher.activities.Launcher;
import com.stario.launcher.activities.settings.Settings;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.services.AccessibilityService;
import com.stario.launcher.ui.popup.PopupMenu;

public class LockDetector extends LinearLayout {
    public static final String PREFERENCE_ENTRY = "com.stario.LockDetector.LOCK";
    public static final String LEGACY_ANIMATION = "com.stario.LockDetector.LEGACY_LOCK_ANIMATION";
    private android.view.GestureDetector detector;
    private boolean doubleTapped;
    private float x;
    private float y;

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

        SharedPreferences preferences = ((Launcher) context).getSettings();

        detector = new GestureDetector(getContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(@NonNull MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(@NonNull MotionEvent e) {
                        AccessibilityManager accessibilityManager =
                                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);

                        doubleTapped = true;

                        if (isAccessibilitySettingsOn(context) &&
                                preferences.getBoolean(PREFERENCE_ENTRY, false)) {

                            if (!preferences.getBoolean(LEGACY_ANIMATION, true)) {
                                getClosingAnimationView()
                                        .closeTo(e.getRawX(), e.getRawY(),
                                                () -> sleep(accessibilityManager));
                            } else {
                                sleep(accessibilityManager);
                            }

                            requestDisallowInterceptTouchEvent(true);
                        }

                        return true;
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
                });

        setHapticFeedbackEnabled(false);

        setOnLongClickListener(view -> {
            if (!doubleTapped) {
                Vibrations.getInstance().vibrate();

                displayLauncherOptions((Launcher) context, view);

                requestDisallowInterceptTouchEvent(true);
            }

            return true;
        });
    }

    private void sleep(AccessibilityManager manager) {
        AccessibilityEvent event = AccessibilityEvent.obtain();

        event.setEventType(AccessibilityEvent.TYPE_ANNOUNCEMENT);
        event.setClassName(getClass().getName());
        event.setAction(AccessibilityService.LOCK);

        manager.sendAccessibilityEvent(event);
    }

    public void displayLauncherOptions(Launcher activity, View parent) {
        PopupMenu menu = new PopupMenu(activity);

        Resources resources = activity.getResources();

        menu.add(new PopupMenu.Item(resources.getString(R.string.settings),
                ResourcesCompat.getDrawable(resources, R.drawable.ic_settings, activity.getTheme()),
                view -> {
                    Intent intent = new Intent(activity, Settings.class);

                    activity.startActivity(intent,
                            ActivityOptions.makeSceneTransitionAnimation(activity).toBundle());
                }));

        menu.add(new PopupMenu.Item(resources.getString(R.string.wallpaper),
                ResourcesCompat.getDrawable(resources, R.drawable.ic_palette, activity.getTheme()),
                view -> {
                    Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra("com.android.wallpaper.LAUNCH_SOURCE", "app_launched_launcher");

                    activity.startActivity(intent,
                            ActivityOptions.makeScaleUpAnimation(view, 0, 0,
                                    view.getWidth(), view.getHeight()).toBundle());
                }));

        menu.showAtLocation(activity, parent, x, y, PopupMenu.PIVOT_CENTER_HORIZONTAL);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            doubleTapped = false;
        }

        if (detector != null) {
            detector.onTouchEvent(event);
        }

        x = event.getX();
        y = event.getY();

        return super.onTouchEvent(event);
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
