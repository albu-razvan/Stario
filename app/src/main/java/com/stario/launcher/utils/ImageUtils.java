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

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;

public class ImageUtils {
    public static Drawable getIcon(ApplicationInfo applicationInfo, PackageManager packageManager) {
        Drawable drawable;

        try {
            drawable = packageManager.getActivityIcon(
                    packageManager.getLaunchIntentForPackage(applicationInfo.packageName));

            if (drawable instanceof AdaptiveIconDrawable) {
                return drawable;
            } else {
                drawable = packageManager.getApplicationIcon(applicationInfo);

                if (drawable instanceof AdaptiveIconDrawable) {
                    return drawable;
                }
            }
        } catch (PackageManager.NameNotFoundException exception) {
            // package is not found, no need to try further

            return null;
        }

        return toAdaptive(drawable, Color.WHITE);
    }

    //TODO
    public static float calculateMultiplier(Drawable drawable) {
        if (!(drawable instanceof BitmapDrawable)) {
            return 1;
        }

        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        if (bitmap.getWidth() != bitmap.getHeight())
            return 1;

        int size = bitmap.getWidth();

        for (int i = 0; i < size; i += 2)
            for (int j = 0; j < size; j += 2)
                if (Color.alpha(bitmap.getPixel(i, j)) > 220 ||
                        Color.alpha(bitmap.getPixel(i, j)) > 220 ||
                        Color.alpha(bitmap.getPixel(j, i)) > 220 ||
                        Color.alpha(bitmap.getPixel(i, size - j - 1)) > 220 ||
                        Color.alpha(bitmap.getPixel(size - i - 1, j)) > 220)
                    return size / (size - Math.min(i, j) * 2f);

        return 1;
    }

    public static AdaptiveIconDrawable toAdaptive(Drawable drawable, int backgroundColor) {
        return new AdaptiveIconDrawable(new ColorDrawable(backgroundColor),
                new InsetDrawable(drawable, AdaptiveIconDrawable.getExtraInsetFraction()));
    }

    public static Bitmap toBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;

        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}
