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

package com.stario.launcher.apps;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.transition.Transition;
import android.transition.TransitionListenerAdapter;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.stario.launcher.R;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.icons.AdaptiveIconView;
import com.stario.launcher.utils.Utils;
import com.stario.launcher.utils.animation.Animation;
import com.stario.launcher.utils.animation.SharedAppTransition;

@SuppressLint("CustomSplashScreen")
public class SplashScreen extends ThemedActivity {
    public static final String APPLICATION_PACKAGE = "com.stario.SplashScreen.APPLICATION_PACKAGE";
    public static final String SHARED_ICON_TRANSITION = "com.stario.SplashScreen.SHARED_ICON_TRANSITION";
    public static final String SHARED_CONTAINER_TRANSITION = "com.stario.SplashScreen.SHARED_CONTAINER_TRANSITION";
    private static Pair<ViewGroup, View[]> viewPair;
    private boolean hasPaused;

    public SplashScreen() {
        this.hasPaused = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Window window = getWindow();

        Transition transition = new SharedAppTransition(true);
        transition.setDuration(Animation.LONG.getDuration());

        transition.addListener(new TransitionListenerAdapter() {
            @Override
            public void onTransitionEnd(Transition transition) {
                super.onTransitionEnd(transition);

                if (viewPair != null) {
                    for (View child : viewPair.second) {
                        viewPair.first.removeView(child);
                    }

                    viewPair = null;
                }
            }

            @Override
            public void onTransitionCancel(Transition transition) {
                super.onTransitionCancel(transition);

                if (viewPair != null) {
                    for (View child : viewPair.second) {
                        viewPair.first.removeView(child);
                    }

                    viewPair = null;
                }
            }
        });

        window.setSharedElementEnterTransition(transition);
        window.setSharedElementExitTransition(null);
        window.setSharedElementReenterTransition(null);
        window.setSharedElementReturnTransition(null);

        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        AdaptiveIconView icon = findViewById(R.id.icon);
        ConstraintLayout container = findViewById(R.id.container);

        icon.setTransitionName(SHARED_ICON_TRANSITION);
        container.setTransitionName(SHARED_CONTAINER_TRANSITION);

        String packageName = getIntent().getStringExtra(APPLICATION_PACKAGE);

        if (packageName != null) {
            LauncherApplication application = LauncherApplicationManager
                    .getInstance().get(packageName);

            if (application != null) {
                icon.setIcon(application.icon);

                transition.addListener(new TransitionListenerAdapter() {
                    @Override
                    public void onTransitionEnd(Transition transition) {
                        super.onTransitionEnd(transition);

                        getRoot().post(() -> {
                            ActivityOptions activityOptions =
                                    ActivityOptions.makeThumbnailScaleUpAnimation(container,
                                            Utils.getSnapshot(getWindow().getDecorView()), 0, 0);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                activityOptions.setSplashScreenStyle(android.window.SplashScreen.SPLASH_SCREEN_STYLE_SOLID_COLOR);
                            }

                            PackageManager packageManager = getPackageManager();
                            Intent intent = packageManager
                                    .getLaunchIntentForPackage(application.info.packageName);

                            if (intent != null) {
                                startActivity(intent, activityOptions.toBundle());
                            }
                        });
                    }
                });
            } else {
                finish();
            }
        }
    }

    public static void scheduleViewForRemoval(ViewGroup parent, View[] children) {
        viewPair = new Pair<>(parent, children);
    }

    @Override
    protected boolean isOpaque() {
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(hasPaused) {
            finish();
            overridePendingTransition(0, 0);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        hasPaused = true;
    }

    @Override
    protected void onStop() {
        super.onStop();

        finish();
        overridePendingTransition(0, 0);
    }
}