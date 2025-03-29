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

import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.icons.AdaptiveIconView;
import com.stario.launcher.utils.UiUtils;
import com.stario.launcher.utils.Utils;

import java.util.UUID;

public class LauncherApplication {
    public static final String LEGACY_LAUNCH_ANIMATION = "com.stario.LauncherApplication.LEGACY_LAUNCH_ANIMATION";
    public static final LauncherApplication FALLBACK_APP = null;

    public final boolean systemPackage;

    @NonNull
    ApplicationInfo info;
    @NonNull
    String label;
    UUID category;
    UserHandle handle;
    Drawable icon;
    int notificationCount;

    public LauncherApplication(@NonNull ApplicationInfo info, @NonNull UserHandle handle, @NonNull String label) {
        this.info = info;
        this.label = label;
        this.category = UUID.randomUUID();
        this.icon = null;
        this.handle = handle;
        this.notificationCount = 0;
        this.systemPackage = (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    public void launch(ThemedActivity activity, AdaptiveIconView view) {
        Vibrations.getInstance().vibrate();

        if (!UiUtils.areWindowAnimationsOn(activity) ||
                activity.getSettings().getBoolean(LEGACY_LAUNCH_ANIMATION, false)) {
            LauncherActivityInfo info = Utils.getMainActivity(activity, getInfo().packageName, handle);

            if (info != null) {
                activity.getSystemService(LauncherApps.class).startMainActivity(info.getComponentName(),
                        handle, null, null);
            }
        } else {
            SplashScreen.launch(info.packageName, view, handle);
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