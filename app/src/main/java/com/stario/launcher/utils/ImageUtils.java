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

package com.stario.launcher.utils;

import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;

import com.stario.launcher.apps.ProfileManager;
import com.stario.launcher.apps.ProfileApplicationManager;
import com.stario.launcher.ui.Measurements;

import java.util.List;

public class ImageUtils {
    public static Drawable getIcon(LauncherApps service, String packageName) {
        Drawable drawable = null;

        List<ProfileApplicationManager> profiles = ProfileManager.getInstance().getProfiles();

        for (ProfileApplicationManager profile : profiles) {
            LauncherActivityInfo main = Utils.getMainActivity(service, packageName, profile.handle);

            if(main != null) {
                drawable = main.getIcon(Measurements.getDotsPerInch());

                if (drawable instanceof AdaptiveIconDrawable) {
                    return drawable;
                }
            }
        }

        if(drawable != null) {
            return new AdaptiveIconDrawable(new ColorDrawable(Color.WHITE),
                    new InsetDrawable(drawable, AdaptiveIconDrawable.getExtraInsetFraction()));
        }

        return null;
    }
}
