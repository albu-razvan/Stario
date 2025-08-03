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

import android.app.Activity;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Window;

import com.stario.launcher.hidden.WallpaperManagerHidden;

import dev.rikka.tools.refine.Refine;

public class WallpaperAnimator {
    private static final String TAG = "WallpaperAnimation";
    private static final Handler handler = new Handler();
    private static final float ANIMATION_FRAME_STEP = 0.005f;
    private static final int TARGET_FRAME_COUNT = 30;
    private static final int ANIMATION_FRAME_DELAY = 8;

    private static WallpaperManagerHidden wallpaperManager;
    private static boolean hasLoggedMissingMethod = false;
    private static float lastRecordedZoomValue = 0;
    private static Runnable zoomAnimator;

    public static void updateZoom(Activity activity, float zoom) {
        if (hasLoggedMissingMethod || zoom == lastRecordedZoomValue) {
            return;
        }

        if (zoomAnimator != null) {
            handler.removeCallbacks(zoomAnimator);
        }

        float direction = Math.signum(zoom - lastRecordedZoomValue);
        IBinder token = getWindowToken(activity);
        zoomAnimator = new Runnable() {
            @Override
            public void run() {
                if (token != null) {
                    lastRecordedZoomValue += direction *
                            Math.max(ANIMATION_FRAME_STEP,
                                    Math.abs(zoom - lastRecordedZoomValue) / TARGET_FRAME_COUNT);
                    if ((direction > 0 && lastRecordedZoomValue > zoom)
                            || (direction < 0 && lastRecordedZoomValue < zoom)) {
                        lastRecordedZoomValue = zoom;
                    }

                    try {
                        getWallpaperManager(activity)
                                .setWallpaperZoomOut(token, lastRecordedZoomValue);
                    } catch (NoSuchMethodError exception) {
                        if (!hasLoggedMissingMethod) {
                            Log.e(TAG, "WallpaperManager::setWallpaperZoomOut does not exist. This error message will not be shown again.");
                            hasLoggedMissingMethod = true;
                        }

                        return;
                    }

                    if (lastRecordedZoomValue != zoom) {
                        handler.postDelayed(this, ANIMATION_FRAME_DELAY);
                    }
                }
            }
        };

        zoomAnimator.run();
    }

    private static WallpaperManagerHidden getWallpaperManager(Activity activity) {
        if (wallpaperManager != null) {
            return wallpaperManager;
        }

        wallpaperManager = Refine.unsafeCast(WallpaperManagerHidden.getInstance(activity));
        return wallpaperManager;
    }

    private static IBinder getWindowToken(Activity activity) {
        Window window = activity.getWindow();

        if (window != null) {
            return window.getDecorView().getWindowToken();
        }

        return null;
    }
}
