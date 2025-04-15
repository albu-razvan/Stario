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

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.icons.AdaptiveIconView;
import com.stario.launcher.utils.Utils;

import java.util.UUID;

public class LauncherApplication {
    public static final LauncherApplication FALLBACK_APP = null;

    public final boolean systemPackage;

    @NonNull
    ApplicationInfo info;
    @NonNull
    String label;
    UUID category;
    Drawable icon;
    int notificationCount;

    public LauncherApplication(@NonNull ApplicationInfo info, @NonNull String label) {
        this.info = info;
        this.label = label;
        this.category = UUID.randomUUID();
        this.icon = null;
        this.notificationCount = 0;
        this.systemPackage = (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    public void launch(ThemedActivity activity, AdaptiveIconView view) {
        Vibrations.getInstance().vibrate();

        PackageManager packageManager = activity.getPackageManager();

        ActivityOptions activityOptions = ActivityOptions.makeBasic();

        if (Utils.isMinimumSDK(Build.VERSION_CODES.TIRAMISU)) {
            activityOptions.setSplashScreenStyle(android.window.SplashScreen.SPLASH_SCREEN_STYLE_ICON);
        }

        Intent intent = packageManager.getLaunchIntentForPackage(info.packageName);

        if (intent != null) {
            activity.startActivity(intent, activityOptions.toBundle());
        }
    }

    @NonNull
    public ApplicationInfo getInfo() {
        return info;
    }

    @NonNull
    public String getLabel() {
        return label;
    }

    public Drawable getIcon() {
        return icon;
    }

    public UUID getCategory() {
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