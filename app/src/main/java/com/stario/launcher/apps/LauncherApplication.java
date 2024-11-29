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

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.transition.Transition;
import android.transition.TransitionListenerAdapter;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.transition.platform.MaterialElevationScale;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.icons.AdaptiveIconView;
import com.stario.launcher.utils.Utils;

import lombok.NonNull;

public class LauncherApplication {
    public static final String LEGACY_LAUNCH_ANIMATION = "com.stario.LauncherApplication.LEGACY_LAUNCH_ANIMATION";
    public static final LauncherApplication FALLBACK_APP = null;
    public final boolean systemPackage;
    @NonNull ApplicationInfo info;
    @NonNull String label;
    @NonNull Drawable icon;
    int category;
    int notificationCount;

    public LauncherApplication(@NonNull ApplicationInfo info, PackageManager packageManager) {
        this.info = info;
        this.label = info.loadLabel(packageManager).toString();
        this.category = ApplicationInfo.CATEGORY_UNDEFINED;
        this.icon = null;
        this.notificationCount = 0;
        this.systemPackage = (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    public void launch(ThemedActivity activity, AdaptiveIconView view) {
        Vibrations.getInstance().vibrate();

        if (activity.getSettings().getBoolean(LEGACY_LAUNCH_ANIMATION, false)) {
            PackageManager packageManager = activity.getPackageManager();

            ActivityOptions activityOptions = ActivityOptions.makeBasic();

            if (Utils.isMinimumSDK(Build.VERSION_CODES.TIRAMISU)) {
                activityOptions.setSplashScreenStyle(android.window.SplashScreen.SPLASH_SCREEN_STYLE_ICON);
            }

            Intent intent = packageManager.getLaunchIntentForPackage(info.packageName);

            if (intent != null) {
                activity.startActivity(intent, activityOptions.toBundle());
            }
        } else {
            Intent intent = new Intent(activity, SplashScreen.class);

            intent.putExtra(SplashScreen.APPLICATION_PACKAGE, info.packageName);

            ViewGroup parent = (ViewGroup) view.getParent();

            Transition transition = new MaterialElevationScale(false);

            // dialog window flicker hack
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

            transition.addListener(new TransitionListenerAdapter() {
                @Override
                public void onTransitionEnd(Transition transition) {
                    view.setIcon(icon);
                }
            });

            SplashScreen.scheduleViewForRemoval(parent, new View[]{container, image});
            // end hack

            activity.getWindow().setExitTransition(transition);

            Pair<View, String> iconPair = new Pair<>(view, SplashScreen.SHARED_ICON_TRANSITION);
            Pair<View, String> containerPair = new Pair<>(container, SplashScreen.SHARED_CONTAINER_TRANSITION);

            ActivityOptions options = ActivityOptions
                    .makeSceneTransitionAnimation(activity, iconPair, containerPair);

            image.post(() ->
                    container.post(() ->
                            activity.startActivity(intent, options.toBundle())));
        }
    }

    public ApplicationInfo getInfo() {
        return info;
    }

    public String getLabel() {
        return label;
    }

    public Drawable getIcon() {
        return icon;
    }

    public int getCategory() {
        return category;
    }

    public int getNotificationCount() {
        return notificationCount;
    }

    public void setNotificationCount(int count) {
        if (count >= 0) {
            notificationCount = count;
        }
    }

    @Override
    public int hashCode() {
        return info.packageName.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof LauncherApplication) {
            return ((LauncherApplication) obj).info.equals(info);
        } else {
            return false;
        }
    }
}