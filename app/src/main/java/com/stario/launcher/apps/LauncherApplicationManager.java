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

import android.content.Context;
import android.content.pm.LauncherApps;
import android.os.Process;
import android.os.UserHandle;

import androidx.annotation.NonNull;

import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LauncherApplicationManager {
    private static final String TAG = "LauncherApplicationManager";
    private static LauncherApplicationManager instance = null;

    private final Map<UserHandle, ProfileApplicationManager> profilesMap;
    private final List<ProfileApplicationManager> profilesList;
    private final IconPackManager iconPacks;

    private LauncherApplicationManager(ThemedActivity activity) {
        // CategoryData and IconPackManager needs LauncherApplicationManager
        // to be instantiated. Assign the instance in the constructor before
        // everything else to guarantee that the instance will be supplied.
        instance = this;

        this.iconPacks = IconPackManager.from(activity, this::updateIcons);
        this.profilesList = new ArrayList<>();
        this.profilesMap = new HashMap<>();

        LauncherApps launcherApps = (LauncherApps) activity.getSystemService(Context.LAUNCHER_APPS_SERVICE);

        for (UserHandle profileHandle : launcherApps.getProfiles()) {
            ProfileApplicationManager manager = new ProfileApplicationManager(activity,
                    profileHandle, Utils.isMainProfile(profileHandle));

            profilesMap.put(profileHandle, manager);

            if(Utils.isMainProfile(profileHandle)) {
                profilesList.add(0, manager);
            } else {
                profilesList.add(manager);
            }
        }
    }

    public static LauncherApplicationManager from(@NonNull ThemedActivity activity) {
        if (instance == null) {
            instance = new LauncherApplicationManager(activity);
        } else {
            instance.iconPacks.refresh();
            instance.updateIcons();
        }

        for(ProfileApplicationManager manager : instance.profilesList) {
            manager.refreshReceiver(activity);
        }

        return instance;
    }

    public static LauncherApplicationManager getInstance() {
        if (instance == null) {
            throw new RuntimeException("Applications not initialized.");
        }

        return instance;
    }

    public ProfileApplicationManager getProfile(UserHandle handle) {
        if (handle == null) {
            return instance.profilesMap.getOrDefault(Utils.getMainUser(), null);
        }

        return instance.profilesMap.getOrDefault(handle, null);
    }

    public ProfileApplicationManager getProfile(int index) {
        if(index < 0 || index >= profilesList.size()) {
            return null;
        }

        return profilesList.get(index);
    }

    public List<ProfileApplicationManager> getProfiles() {
        return instance.profilesList;
    }

    public LauncherApplication getApplication(String packageName) {
        for(Map.Entry<UserHandle, ProfileApplicationManager> entry:instance.profilesMap.entrySet()) {
            LauncherApplication application = entry.getValue().get(packageName);

            if(application != null) {
                return application;
            }
        }

        return null;
    }

    void updateIcons() {
        for (ProfileApplicationManager profileApplicationManager : profilesMap.values()) {
            profileApplicationManager.updateIcons();
        }
    }

    public void updateLabel(LauncherApplication application, String label) {
        for (ProfileApplicationManager profileApplicationManager : profilesMap.values()) {
            profileApplicationManager.updateLabel(application, label);
        }
    }

    public void notifyUpdate(LauncherApplication application) {
        for (ProfileApplicationManager profileApplicationManager : profilesMap.values()) {
            profileApplicationManager.notifyUpdate(application);
        }
    }

    public int size() {
        return profilesMap.size();
    }
}