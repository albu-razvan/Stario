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
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
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
import com.stario.launcher.utils.animation.SharedAppTransition;

@SuppressLint("CustomSplashScreen")
public class SplashScreen extends ThemedActivity {
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

        Transition transition = new SharedAppTransition(true);
        transition.setDuration(Animation.MEDIUM.getDuration());

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

        String packageName = getIntent().getStringExtra(APPLICATION_PACKAGE);

        if (packageName != null) {
            LauncherApplication application = LauncherApplicationManager
                    .getInstance().get(packageName);

            if (application != null) {
                icon.setIcon(application.icon);

                transition.addListener(new TransitionListenerAdapter() {
                    @Override
                    public void onTransitionEnd(Transition transition) {
                        getRoot().post(() -> {
                            ActivityOptions activityOptions =
                                    ActivityOptions.makeScaleUpAnimation(container, 1, 1,
                                            container.getMeasuredWidth() - 1, container.getMeasuredHeight() - 1);

                            if (Utils.isMinimumSDK(Build.VERSION_CODES.TIRAMISU)) {
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

    public static void launch(String packageName, AdaptiveIconView view) {
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