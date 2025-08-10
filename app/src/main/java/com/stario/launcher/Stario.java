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

package com.stario.launcher;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.stario.launcher.apps.ProfileManager;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.ui.Measurements;

import org.chickenhook.restrictionbypass.Unseal;

public class Stario extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        try {
            Unseal.unseal();
        } catch (Exception exception) {
            Log.e("Stario", "Could not unseal the process.", exception);
        }

        Vibrations.from(this);
        ProfileManager.from(this);

        ProcessLifecycleOwner.get()
                .getLifecycle()
                .addObserver(new DefaultLifecycleObserver() {
                    @Override
                    public void onStart(@NonNull LifecycleOwner owner) {
                        if (!Measurements.wereTaken()) {
                            throw new RuntimeException("Measurements were not taken.");
                        }
                    }
                });
    }

    //warn the usage of malformed preference stores
    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        if (!Entry.isValid(name)) {
            Log.w("Stario", "getSharedPreferences: " + name +
                    " should be part of " + Entry.class.getCanonicalName());
        }

        return super.getSharedPreferences(name, mode);
    }

    public SharedPreferences getSharedPreferences(Entry entry) {
        return super.getSharedPreferences(entry.toString(), MODE_PRIVATE);
    }

    public SharedPreferences getSharedPreferences(Entry entry, String subPreference) {
        return super.getSharedPreferences(entry.toSubPreference(subPreference), MODE_PRIVATE);
    }

    public SharedPreferences getSettings() {
        return getSharedPreferences(Entry.STARIO);
    }
}
