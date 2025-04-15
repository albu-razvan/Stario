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

package com.stario.launcher.ui.utils.animation;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.os.IBinder;
import android.view.Window;

import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.stario.launcher.hidden.WallpaperManagerHidden;

import dev.rikka.tools.refine.Refine;

public class WallpaperAnimator {
    private static WallpaperManagerHidden wallpaperManager;
    private static float lastRecordedZoomValue = 0;
    private static ValueAnimator animator;

    /**
     * @param activity context activity
     * @param from starting value; <code>null</code> for last recorded value
     * @param to end value; <code>null</code> for last recorded value
     */
    public static void animateZoom(Activity activity, Float from, Float to) {
        if (animator != null) {
            animator.cancel();
        }

        animator = ValueAnimator.ofFloat(from != null ? from : lastRecordedZoomValue,
                to != null ? to : lastRecordedZoomValue);
        animator.setInterpolator(new FastOutSlowInInterpolator());
        animator.setDuration(Animation.LONG.getDuration());
        animator.addUpdateListener(animation -> {
            IBinder token = getWindowToken(activity);

            if (token != null) {
                lastRecordedZoomValue = (float) animation.getAnimatedValue();

                getWallpaperManager(activity)
                        .setWallpaperZoomOut(token, lastRecordedZoomValue);
            }
        });

        animator.start();
    }

    public static void updateZoom(Activity activity, float zoom) {
        if (animator != null && animator.isRunning()) {
            return;
        }

        IBinder token = getWindowToken(activity);

        if (token != null) {
            lastRecordedZoomValue = zoom;
            getWallpaperManager(activity)
                    .setWallpaperZoomOut(getWindowToken(activity), zoom);
        }
    }

    private static IBinder getWindowToken(Activity activity) {
        Window window = activity.getWindow();

        if (window != null) {
            return window.getDecorView().getWindowToken();
        }

        return null;
    }

    private static WallpaperManagerHidden getWallpaperManager(Activity activity) {
        if (wallpaperManager != null) {
            return wallpaperManager;
        }

        wallpaperManager = Refine.unsafeCast(WallpaperManagerHidden.getInstance(activity));
        return wallpaperManager;
    }
}
