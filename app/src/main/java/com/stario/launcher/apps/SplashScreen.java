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

package com.stario.launcher.apps;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.transition.Transition;
import android.transition.TransitionListenerAdapter;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.stario.launcher.R;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.icons.AdaptiveIconView;
import com.stario.launcher.utils.UiUtils;
import com.stario.launcher.utils.Utils;
import com.stario.launcher.utils.animation.Animation;
import com.stario.launcher.utils.animation.SharedElementTransition;

@SuppressLint("CustomSplashScreen")
public class SplashScreen extends ThemedActivity {
    public static final String USER_HANDLE = "com.stario.SplashScreen.USER_HANDLE";
    public static final String APPLICATION_PACKAGE = "com.stario.SplashScreen.APPLICATION_PACKAGE";
    public static final String SHARED_ICON_TRANSITION = "com.stario.SplashScreen.SHARED_ICON_TRANSITION";
    public static final String SHARED_CONTAINER_TRANSITION = "com.stario.SplashScreen.SHARED_CONTAINER_TRANSITION";

    private static OnClearTargets clearListener;

    private boolean hasPaused;

    public SplashScreen() {
        this.hasPaused = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (clearListener == null) {
            Log.e("SplashScreen", "This activity can only be launched by calling SplashScreen.launch(String, AdaptiveIconView)");

            super.onCreate(null);

            finish();
            return;
        }

        Window window = getWindow();

        Transition transition = new SharedElementTransition.SharedAppSplashScreenTransition();
        transition.setDuration(Animation.LONG.getDuration());

        window.setSharedElementEnterTransition(transition);
        window.setSharedElementExitTransition(null);
        window.setSharedElementReenterTransition(null);
        window.setSharedElementReturnTransition(null);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        AdaptiveIconView icon = findViewById(R.id.icon);
        ConstraintLayout container = findViewById(R.id.container);

        icon.setTransitionName(SHARED_ICON_TRANSITION);
        container.setTransitionName(SHARED_CONTAINER_TRANSITION);

        Intent startIntent = getIntent();
        String packageName = startIntent.getStringExtra(APPLICATION_PACKAGE);
        UserHandle handle;
        if (Utils.isMinimumSDK(Build.VERSION_CODES.TIRAMISU)) {
            handle = startIntent.getParcelableExtra(USER_HANDLE, UserHandle.class);
        } else {
            handle = startIntent.getParcelableExtra(USER_HANDLE);
        }

        if (packageName != null && handle != null) {
            LauncherApplication application = LauncherApplicationManager
                    .getInstance().getApplication(packageName);

            if (application != null) {
                icon.setIcon(application.icon);

                transition.addListener(new TransitionListenerAdapter() {
                    @Override
                    public void onTransitionEnd(Transition transition) {
                        UiUtils.runOnUIThread(() -> {
                            if (!isFinishing()) {
                                ComponentName componentName = Utils.getMainActivityComponent(SplashScreen.this, packageName, handle);

                                if (componentName != null) {
                                    ActivityOptions activityOptions = ActivityOptions.makeBasic();

                                    if (Utils.isMinimumSDK(Build.VERSION_CODES.TIRAMISU)) {
                                        activityOptions.setSplashScreenStyle(android.window.SplashScreen.SPLASH_SCREEN_STYLE_SOLID_COLOR);
                                    }

                                    getSystemService(LauncherApps.class).startMainActivity(componentName,
                                            handle, null, activityOptions.toBundle());
                                    overridePendingTransition(0, 0);
                                } else {
                                    finish();
                                }
                            }
                        });
                    }
                });
            } else {
                finish();
            }
        }
    }

    @Override
    protected boolean isOpaque() {
        return true;
    }

    @Override
    protected boolean isAffectedByBackGesture() {
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (hasPaused) {
            finish();
            overridePendingTransition(0, 0);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (clearListener != null) {
            clearListener.onClear();

            clearListener = null;
        }

        hasPaused = true;
    }

    @Override
    protected void onStop() {
        super.onStop();

        UiUtils.runOnUIThread(() -> {
            finish();
            overridePendingTransition(0, 0);
        });
    }

    public static void launch(String packageName, AdaptiveIconView view, UserHandle handle) {
        if (clearListener != null) {
            Log.w("SplashScreen", "Only one instance of SplashScreen can run at a time.");

            return;
        }

        if (!view.isAttachedToWindow() ||
                !(view.getContext() instanceof Activity)) {
            Log.w("SplashScreen", "Cannot create transition from a detached view.");

            return;
        }

        final Activity activity = (Activity) view.getContext();

        // dialog window flicker hack
        ViewGroup parent = (ViewGroup) view.getParent();

        ImageView image = new ImageView(activity);

        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = view.getHeight();
        params.width = view.getWidth();
        image.setLayoutParams(params);

        image.setImageBitmap(Utils.getSnapshot(view));
        parent.addView(image);

        ConstraintLayout container = new ConstraintLayout(activity);
        container.setLayoutParams(params);
        parent.addView(container);

        Drawable icon = view.getIcon();
        view.setIcon(null);

        Pair<View, String> iconPair = new Pair<>(view, SplashScreen.SHARED_ICON_TRANSITION);
        Pair<View, String> containerPair = new Pair<>(container, SplashScreen.SHARED_CONTAINER_TRANSITION);

        Intent intent = new Intent(activity, SplashScreen.class);
        intent.putExtra(APPLICATION_PACKAGE, packageName);
        intent.putExtra(USER_HANDLE, handle);

        UiUtils.runOnUIThread(() -> {
            clearListener = () -> {
                view.setIcon(icon);
                parent.removeView(container);
                parent.removeView(image);
            };

            activity.startActivity(intent,
                    ActivityOptions.makeSceneTransitionAnimation(activity, iconPair, containerPair)
                            .toBundle());
        });
    }

    private interface OnClearTargets {
        void onClear();
    }
}