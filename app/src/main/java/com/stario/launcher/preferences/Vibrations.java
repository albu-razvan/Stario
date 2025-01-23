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

package com.stario.launcher.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.VibrationEffect;
import android.os.Vibrator;

import com.stario.launcher.exceptions.NoExistingInstanceException;
import com.stario.launcher.themes.ThemedActivity;

public class Vibrations {
    public static final String PREFERENCE_ENTRY = "com.stario.VIBRATIONS";
    private static Vibrations instance;
    private final SharedPreferences settings;
    private final Vibrator vibrator;

    private Vibrations(ThemedActivity activity) {
        settings = activity.getSettings();

        vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public static void from(ThemedActivity activity) {
        if (instance == null) {
            instance = new Vibrations(activity);
        }
    }

    public static Vibrations getInstance() throws NoExistingInstanceException {
        if (instance == null) {
            throw new NoExistingInstanceException(Vibrations.class);
        }

        return instance;
    }

    public void vibrate() {
        if (settings.getBoolean(PREFERENCE_ENTRY, true)) {
            vibrator.vibrate(
                    VibrationEffect.createOneShot(5, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }
}
