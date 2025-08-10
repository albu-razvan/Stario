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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LauncherApps;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.stario.launcher.Stario;
import com.stario.launcher.apps.interfaces.LauncherProfileListener;
import com.stario.launcher.utils.ThreadSafeArrayList;
import com.stario.launcher.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ProfileManager {
    public static final String PROFILE_AVAILABLE_EXTRA = "com.stario.launcher.PROFILE_AVAILABLE_EXTRA";

    private static final String PROFILE_AVAILABLE_INTENT = "com.stario.launcher.PROFILE_AVAILABLE_INTENT";
    private static final String TAG = "LauncherApplicationManager";
    private static ProfileManager instance = null;
    private static UserHandle owner;

    private final Map<UserHandle, ProfileApplicationManager> profilesMap;
    private final List<ProfileApplicationManager> profilesList;
    private final List<LauncherProfileListener> listeners;
    private final IconPackManager iconPacks;

    private ProfileManager(Stario stario) {
        // CategoryData and IconPackManager needs LauncherApplicationManager
        // to be instantiated. Assign the instance in the constructor before
        // everything else to guarantee that the instance will be supplied.
        instance = this;

        this.iconPacks = IconPackManager.from(stario, this::update);
        this.listeners = new ThreadSafeArrayList<>();
        this.profilesList = new ArrayList<>();
        this.profilesMap = new HashMap<>();

        LauncherApps launcherApps = (LauncherApps) stario.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        UserManager userManager = (UserManager) stario.getSystemService(Context.USER_SERVICE);

        // work profiles will always be created after the owner
        List<UserHandle> profiles = launcherApps.getProfiles();
        profiles.sort((handle1, handle2) -> {
            long diff = userManager.getUserCreationTime(handle1) -
                    userManager.getUserCreationTime(handle2);

            return diff < 0 ? -1 : (diff > 0 ? 1 : 0);
        });

        for (int index = 0; index < profiles.size(); index++) {
            UserHandle handle = profiles.get(index);

            ProfileApplicationManager manager =
                    new ProfileApplicationManager(stario, handle, index == 0);

            profilesMap.put(handle, manager);
            profilesList.add(manager);
        }

        // there should always be a profile, if not, something TERRIBLE happened
        owner = !profilesList.isEmpty() ? profilesList.get(0).handle : Process.myUserHandle();
    }

    public static ProfileManager from(@NonNull Stario stario) {
        return from(stario, true);
    }

    public static ProfileManager from(@NonNull Stario stario, boolean refreshIcons) {
        if (instance == null) {
            instance = new ProfileManager(stario);
            BroadcastReceiver receiver = getReceiver(instance);

            if (Utils.isMinimumSDK(Build.VERSION_CODES.TIRAMISU)) {
                stario.registerReceiver(receiver, getIntentFilter(), Context.RECEIVER_EXPORTED);
            } else {
                //noinspection UnspecifiedRegisterReceiverFlag
                stario.registerReceiver(receiver, getIntentFilter());
            }
        } else if (refreshIcons) {
            instance.iconPacks.refresh();
            instance.update();
        }

        return instance;
    }

    private static BroadcastReceiver getReceiver(ProfileManager instance) {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                UserHandle handle;
                if (Utils.isMinimumSDK(Build.VERSION_CODES.TIRAMISU)) {
                    handle = intent.getParcelableExtra(Intent.EXTRA_USER, UserHandle.class);
                } else {
                    handle = intent.getParcelableExtra(Intent.EXTRA_USER);
                }

                if (handle == null) {
                    return;
                }

                String action = intent.getAction();
                if (action != null) {
                    switch (action) {
                        case Intent.ACTION_MANAGED_PROFILE_ADDED:
                            if (instance.profilesMap.containsKey(handle)) {
                                return;
                            }

                            LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);

                            for (UserHandle profileHandle : launcherApps.getProfiles()) {
                                if (handle.equals(profileHandle)) {
                                    ProfileApplicationManager manager = new ProfileApplicationManager(
                                            (Stario) context.getApplicationContext(),
                                            profileHandle, Utils.isMainProfile(profileHandle)
                                    );

                                    instance.profilesMap.put(profileHandle, manager);
                                    instance.profilesList.add(manager);

                                    for (LauncherProfileListener listener : instance.listeners) {
                                        if (listener != null) {
                                            listener.onInserted(profileHandle);
                                        }
                                    }
                                }
                            }
                            break;
                        case Intent.ACTION_MANAGED_PROFILE_REMOVED:
                            ProfileApplicationManager manager = instance.profilesMap.remove(handle);

                            if (manager == null) {
                                return;
                            }

                            instance.profilesList.remove(manager);
                            for (LauncherProfileListener listener : instance.listeners) {
                                if (listener != null) {
                                    listener.onRemoved(handle);
                                }
                            }
                            break;
                        case Intent.ACTION_MANAGED_PROFILE_AVAILABLE:
                            intent = new Intent(getProfileAvailabilityIntentAction(handle));
                            intent.putExtra(PROFILE_AVAILABLE_EXTRA, true);

                            LocalBroadcastManager.getInstance(context)
                                    .sendBroadcastSync(intent);
                            break;
                        case Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE:
                            intent = new Intent(getProfileAvailabilityIntentAction(handle));
                            intent.putExtra(PROFILE_AVAILABLE_EXTRA, false);

                            LocalBroadcastManager.getInstance(context)
                                    .sendBroadcastSync(intent);
                            break;
                    }
                }
            }
        };
    }

    public static UserHandle getOwner() {
        if (instance == null || owner == null) {
            throw new IllegalStateException("ProfileManager not initialized");
        }

        return owner;
    }

    public static String getProfileAvailabilityIntentAction(UserHandle handle) {
        return PROFILE_AVAILABLE_INTENT + (handle != null ? (":" + handle) : "");
    }

    private static IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MANAGED_PROFILE_ADDED);
        intentFilter.addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED);
        intentFilter.addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE);
        intentFilter.addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE);
        intentFilter.addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED);

        return intentFilter;
    }


    public static ProfileManager getInstance() {
        if (instance == null) {
            throw new RuntimeException("Applications not initialized.");
        }

        return instance;
    }

    public ProfileApplicationManager getProfile(UserHandle handle) {
        if (handle == null) {
            return instance.profilesMap.getOrDefault(owner, null);
        }

        return instance.profilesMap.getOrDefault(handle, null);
    }

    public ProfileApplicationManager getProfile(int index) {
        if (index < 0 || index >= profilesList.size()) {
            return null;
        }

        return profilesList.get(index);
    }

    public List<ProfileApplicationManager> getProfiles() {
        return Collections.unmodifiableList(profilesList);
    }

    public LauncherApplication getApplication(String packageName) {
        for (ProfileApplicationManager manager : profilesList) {
            LauncherApplication application = manager.get(packageName);

            if (application != null) {
                return application;
            }
        }

        return null;
    }

    void update() {
        for (ProfileApplicationManager manager : profilesList) {
            manager.update();
        }
    }

    void updateIcon(String packageName, Drawable icon) {
        for (ProfileApplicationManager manager : profilesList) {
            LauncherApplication application = manager.get(packageName);

            if (application != null &&
                    icon != null && !icon.equals(application.getIcon())) {
                application.icon = icon;

                notifyUpdate(packageName);
            }
        }
    }

    public void updateLabel(String packageName, String label) {
        for (ProfileApplicationManager manager : profilesList) {
            LauncherApplication application = manager.get(packageName);

            if (application != null) {
                manager.updateLabel(application, label);
            }
        }
    }

    public void notifyUpdate(String packageName) {
        for (ProfileApplicationManager manager : profilesList) {
            LauncherApplication application = manager.get(packageName);

            if (application != null) {
                manager.notifyUpdate(application);
            }
        }
    }

    public int size() {
        return profilesList.size();
    }

    public void addLauncherProfileListener(LauncherProfileListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeLauncherProfileListener(LauncherProfileListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }
}