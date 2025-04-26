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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.stario.launcher.apps.interfaces.LauncherProfileListener;
import com.stario.launcher.themes.ThemedActivity;
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

    private boolean registered;

    private ProfileManager(ThemedActivity activity) {
        // CategoryData and IconPackManager needs LauncherApplicationManager
        // to be instantiated. Assign the instance in the constructor before
        // everything else to guarantee that the instance will be supplied.
        instance = this;

        this.iconPacks = IconPackManager.from(activity, this::update);
        this.listeners = new ThreadSafeArrayList<>();
        this.profilesList = new ArrayList<>();
        this.profilesMap = new HashMap<>();
        this.registered = false;

        LauncherApps launcherApps = (LauncherApps) activity.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        UserManager userManager = (UserManager) activity.getSystemService(Context.USER_SERVICE);

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
                    new ProfileApplicationManager(activity, handle, index == 0);

            profilesMap.put(handle, manager);
            profilesList.add(manager);
        }

        // there should always be a profile, if not, something TERRIBLE happened
        owner = !profilesList.isEmpty() ? profilesList.get(0).handle : Process.myUserHandle();
    }

    public static ProfileManager from(@NonNull ThemedActivity activity) {
        if (instance == null) {
            instance = new ProfileManager(activity);
        } else {
            instance.iconPacks.refresh();
            instance.update();
        }

        instance.refreshReceiver(activity);
        for (ProfileApplicationManager manager : instance.profilesList) {
            manager.refreshReceiver(activity);
        }

        return instance;
    }

    public static UserHandle getOwner() {
        if (instance == null || owner == null) {
            throw new IllegalStateException("ProfileManager not initialized");
        }

        return owner;
    }

    private void refreshReceiver(ThemedActivity activity) {
        if (!registered) {
            BroadcastReceiver receiver = getReceiver();

            if (Utils.isMinimumSDK(Build.VERSION_CODES.TIRAMISU)) {
                activity.registerReceiver(receiver, getIntentFilter(), Context.RECEIVER_EXPORTED);
            } else {
                //noinspection UnspecifiedRegisterReceiverFlag
                activity.registerReceiver(receiver, getIntentFilter());
            }

            Lifecycle lifecycle = activity.getLifecycle();
            lifecycle.addObserver(new DefaultLifecycleObserver() {
                @Override
                public void onDestroy(@NonNull LifecycleOwner owner) {
                    try {
                        activity.unregisterReceiver(receiver);
                        registered = false;
                    } catch (Exception exception) {
                        Log.e(TAG, "Receiver not registered");
                    }

                    lifecycle.removeObserver(this);
                }
            });

            registered = true;
        }
    }

    private BroadcastReceiver getReceiver() {
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
                    if (action.equals(Intent.ACTION_MANAGED_PROFILE_ADDED)) {
                        if (profilesMap.containsKey(handle)) {
                            return;
                        }

                        LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);

                        for (UserHandle profileHandle : launcherApps.getProfiles()) {
                            if (handle.equals(profileHandle)) {
                                ProfileApplicationManager manager = new ProfileApplicationManager((ThemedActivity) context,
                                        profileHandle, Utils.isMainProfile(profileHandle));

                                profilesMap.put(profileHandle, manager);
                                profilesList.add(manager);

                                for (LauncherProfileListener listener : listeners) {
                                    if (listener != null) {
                                        listener.onInserted(profileHandle);
                                    }
                                }
                            }
                        }
                    } else if (action.equals(Intent.ACTION_MANAGED_PROFILE_REMOVED)) {
                        ProfileApplicationManager manager = profilesMap.remove(handle);

                        if (manager == null) {
                            return;
                        }

                        profilesList.remove(manager);
                        for (LauncherProfileListener listener : listeners) {
                            if (listener != null) {
                                listener.onRemoved(handle);
                            }
                        }
                    } else if (action.equals(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)) {
                        intent = new Intent(getProfileAvailabilityIntentAction(handle));
                        intent.putExtra(PROFILE_AVAILABLE_EXTRA, true);

                        LocalBroadcastManager.getInstance(context)
                                .sendBroadcastSync(intent);
                    } else if (action.equals(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)) {
                        intent = new Intent(getProfileAvailabilityIntentAction(handle));
                        intent.putExtra(PROFILE_AVAILABLE_EXTRA, false);

                        LocalBroadcastManager.getInstance(context)
                                .sendBroadcastSync(intent);
                    }
                }
            }
        };
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