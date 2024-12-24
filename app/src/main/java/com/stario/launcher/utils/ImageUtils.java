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

package com.stario.launcher.utils;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;

import androidx.annotation.NonNull;

public class ImageUtils {
    public static Drawable getIcon(String packageName, PackageManager packageManager) {
        try {
            return getIcon(packageManager.getApplicationInfo(packageName, PackageManager.MATCH_ALL), packageManager);
        } catch (PackageManager.NameNotFoundException exception) {
            return null;
        }
    }

    public static Drawable getIcon(ApplicationInfo applicationInfo, @NonNull PackageManager packageManager) {
        Drawable drawable;

        try {
            Intent intent = packageManager.getLaunchIntentForPackage(applicationInfo.packageName);

            if (intent == null) {
                return null;
            }

            drawable = packageManager.getActivityIcon(intent);

            if (drawable instanceof AdaptiveIconDrawable) {
                return drawable;
            } else {
                drawable = packageManager.getApplicationIcon(applicationInfo);

                if (drawable instanceof AdaptiveIconDrawable) {
                    return drawable;
                }
            }
        } catch (PackageManager.NameNotFoundException exception) {
            return null;
        }

        return toAdaptive(drawable, Color.WHITE);
    }

    public static AdaptiveIconDrawable toAdaptive(Drawable drawable, int backgroundColor) {
        return new AdaptiveIconDrawable(new ColorDrawable(backgroundColor),
                new InsetDrawable(drawable, AdaptiveIconDrawable.getExtraInsetFraction()));
    }
}
