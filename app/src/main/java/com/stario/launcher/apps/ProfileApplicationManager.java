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
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.stario.launcher.BuildConfig;
import com.stario.launcher.apps.interfaces.LauncherApplicationListener;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.utils.ThreadSafeArrayList;
import com.stario.launcher.utils.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ProfileApplicationManager {
    private static final String TAG = "ProfileApplicationManager";

    private final List<LauncherApplication> visibleApplicationList;
    private final Map<String, LauncherApplication> applicationMap;
    private final List<LauncherApplicationListener> listeners;
    private final List<LauncherApplication> applicationList;
    private final SharedPreferences hiddenApplications;
    private final SharedPreferences applicationLabels;
    private final PackageManager packageManager;
    private final IconPackManager iconPacks;
    private final boolean mainUser;

    public final UserHandle handle;

    private boolean registered;

    ProfileApplicationManager(ThemedActivity activity, UserHandle handle, boolean mainUser) {
        this.visibleApplicationList = new ThreadSafeArrayList<>();
        this.applicationList = new ThreadSafeArrayList<>();
        this.applicationMap = new HashMap<>();
        this.listeners = new ThreadSafeArrayList<>();
        this.applicationLabels = activity.getSharedPreferences(Entry.APPLICATION_LABELS);
        this.hiddenApplications = activity.getSharedPreferences(Entry.HIDDEN_APPS,
                Integer.toString(handle.hashCode()));
        this.packageManager = activity.getPackageManager();
        this.mainUser = mainUser;
        this.registered = false;
        this.handle = handle;

        this.iconPacks = IconPackManager.from(activity);
        if (mainUser) {
            CategoryManager.from(activity, this);
        }

        Utils.submitTask(() -> loadApplications(activity));
    }

    void refreshReceiver(ThemedActivity activity) {
        if (!registered) {
            LauncherApps launcherApps = ((LauncherApps) activity.getSystemService(Context.LAUNCHER_APPS_SERVICE));
            LauncherApps.Callback callback = getReceiver(launcherApps);
            launcherApps.registerCallback(callback);

            Lifecycle lifecycle = activity.getLifecycle();
            lifecycle.addObserver(new DefaultLifecycleObserver() {
                @Override
                public void onDestroy(@NonNull LifecycleOwner owner) {
                    try {
                        launcherApps.unregisterCallback(callback);
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

    private LauncherApps.Callback getReceiver(LauncherApps launcherApps) {
        return new LauncherApps.Callback() {
            @Override
            public void onPackageRemoved(String packageName, UserHandle user) {
                if (!handle.equals(user) || BuildConfig.APPLICATION_ID.equals(packageName)) {
                    return;
                }

                removeApplication(packageName);
            }

            @Override
            public void onPackageAdded(String packageName, UserHandle user) {
                if (!handle.equals(user) || BuildConfig.APPLICATION_ID.equals(packageName)) {
                    return;
                }

                ApplicationInfo applicationInfo = getApplicationInfo(packageName);

                if (applicationInfo != null && applicationInfo.enabled) {
                    addApplication(createApplication(applicationInfo));
                }
            }

            @Override
            public void onPackageChanged(String packageName, UserHandle user) {
                if (!handle.equals(user) || BuildConfig.APPLICATION_ID.equals(packageName)) {
                    return;
                }

                ApplicationInfo applicationInfo = getApplicationInfo(packageName);

                if (applicationInfo != null) {
                    LauncherApplication application = get(packageName);

                    if (application != null) {
                        if (!applicationInfo.enabled) {
                            removeApplication(packageName);
                        } else {
                            application.info = applicationInfo;
                            updateApplication(application);
                        }
                    } else {
                        addApplication(createApplication(applicationInfo));
                    }
                } else {
                    removeApplication(packageName);
                }
            }

            @Override
            public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {
                for (String packageName : packageNames) {
                    if (replacing) {
                        onPackageChanged(packageName, user);
                    } else {
                        onPackageAdded(packageName, user);
                    }
                }
            }

            @Override
            public void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) {
                for (String packageName : packageNames) {
                    if (!replacing) {
                        onPackageRemoved(packageName, user);
                    }
                }
            }

            private ApplicationInfo getApplicationInfo(String packageName) {
                boolean containsPackage = applicationMap.containsKey(packageName);
                if (launcherApps.getActivityList(packageName, handle).isEmpty()) {
                    return null;
                }

                ApplicationInfo applicationInfo;
                try {
                    applicationInfo = launcherApps.getApplicationInfo(packageName, 0, handle);

                    if (launcherApps.getActivityList(packageName,
                            handle).isEmpty()) {
                        // Doesn't have any activity that specifies
                        // Intent.ACTION_MAIN and Intent.CATEGORY_LAUNCHER

                        if (containsPackage) {
                            removeApplication(packageName);
                        }

                        return null;
                    }
                } catch (PackageManager.NameNotFoundException exception) {
                    Log.e(TAG, "Package " + packageName + " does not exist.");

                    return null;
                }

                return applicationInfo;
            }
        };
    }

    private void loadApplications(ThemedActivity activity) {
        LauncherApps launcherApps = activity.getSystemService(LauncherApps.class);
        List<LauncherActivityInfo> activityInfoList =
                launcherApps.getActivityList(null, handle);

        for (int index = 0; index < activityInfoList.size(); index++) {
            ApplicationInfo applicationInfo = activityInfoList.get(index).getApplicationInfo();

            if (iconPacks.checkPackValidity(applicationInfo.packageName)) {
                addApplication(createApplication(applicationInfo));
            }
        }

        for (int index = 0; index < activityInfoList.size(); index++) {
            ApplicationInfo applicationInfo = activityInfoList.get(index).getApplicationInfo();

            if (applicationInfo != null) {
                if (!BuildConfig.APPLICATION_ID.equals(applicationInfo.packageName)) {
                    if (applicationMap.containsKey(applicationInfo.packageName)) {
                        LauncherApplication application = get(applicationInfo.packageName);

                        if (application != null) {
                            iconPacks.updateIcon(application.info.packageName);
                        }
                    } else {
                        addApplication(createApplication(applicationInfo));
                    }
                }
            }
        }
    }


    public void updateApplication(LauncherApplication application) {
        iconPacks.updateIcon(application.info.packageName);

        // might double update, which is fine if loading the icon takes a while
        notifyUpdate(application);
    }

    void update() {
        for (int index = 0; index < applicationList.size(); index++) {
            updateApplication(applicationList.get(index));
        }
    }

    public void updateLabel(LauncherApplication application, String label) {
        String oldLabel = application.label;

        if (label != null) {
            if (!label.isEmpty() && !application.label.equals(label)) {
                application.label = label;
            }
        } else {
            application.label = application.info.loadLabel(packageManager).toString();
        }

        if (!oldLabel.equals(application.label)) {
            applicationLabels.edit()
                    .putString(application.info.packageName, label)
                    .apply();

            if (isVisibleToUser(application.info.packageName)) {
                notifyUpdate(application);
            }
        }
    }

    void notifyUpdate(LauncherApplication application) {
        for (LauncherApplicationListener listener : listeners) {
            if (listener != null) {
                listener.onUpdated(application);
            }
        }
    }

    private LauncherApplication createApplication(ApplicationInfo applicationInfo) {
        LauncherApplication application = new LauncherApplication(applicationInfo,
                handle, getLabel(applicationInfo));

        if (mainUser) {
            application.category = CategoryManager.getInstance()
                    .getCategoryIdentifier(applicationInfo);
        }

        return application;
    }

    private String getLabel(ApplicationInfo applicationInfo) {
        String label = applicationLabels.getString(applicationInfo.packageName, null);

        if (label == null) {
            label = applicationInfo.loadLabel(packageManager).toString();
        }

        return label;
    }

    /**
     * Method to get the {@link LauncherApplication} object at index
     *
     * @param index The index of the application
     * @return {@link LauncherApplication} at index
     */
    @Nullable
    public LauncherApplication get(int index) {
        if (index >= 0 && index < visibleApplicationList.size()) {
            return visibleApplicationList.get(index);
        } else {
            return LauncherApplication.FALLBACK_APP;
        }
    }

    /**
     * Method to get the {@link LauncherApplication} object at index
     *
     * @param index  The index of the application
     * @param hidden If true, account for hidden items
     * @return {@link LauncherApplication} at index
     */
    @Nullable
    public LauncherApplication get(int index, boolean hidden) {
        if (hidden) {
            if (index >= 0 && index < applicationList.size()) {
                return applicationList.get(index);
            } else {
                return LauncherApplication.FALLBACK_APP;
            }
        } else {
            return get(index);
        }
    }

    /**
     * Method to get the {@link LauncherApplication} object by package name
     *
     * @param packageName The package name of the application
     * @return {@link LauncherApplication} at index
     */
    @Nullable
    public LauncherApplication get(@NonNull String packageName) {
        return applicationMap.getOrDefault(packageName, LauncherApplication.FALLBACK_APP);
    }

    public int getActualSize() {
        return applicationList.size();
    }

    public int getSize() {
        return visibleApplicationList.size();
    }

    private synchronized void addApplication(LauncherApplication application) {
        applicationMap.put(application.info.packageName, application);

        addApplicationToList(application, applicationList);
        if (isVisibleToUser(application.info.packageName)) {
            addApplicationToList(application, visibleApplicationList);

            for (LauncherApplicationListener listener : listeners) {
                if (listener != null) {
                    listener.onInserted(application);
                }
            }
        }

        iconPacks.add(application);
        iconPacks.updateIcon(application.info.packageName);

        if (mainUser) {
            CategoryManager.getInstance().addApplication(application);
        }
    }

    private synchronized void addApplicationToList(LauncherApplication applicationToAdd, List<LauncherApplication> list) {
        int left = 0;
        int right = list.size() - 1;

        while (left <= right) {
            int middle = (left + right) / 2;

            LauncherApplication application = list.get(middle);
            int compareValue = application.label
                    .compareToIgnoreCase(applicationToAdd.label);

            if (compareValue < 0) {
                left = middle + 1;
            } else if (compareValue > 0) {
                right = middle - 1;
            } else if (!application.info.packageName
                    .equals(applicationToAdd.info.packageName)) {
                list.add(middle, applicationToAdd);

                return;
            } else {
                return; // same package found
            }
        }

        list.add(left, applicationToAdd);
    }

    private synchronized void removeApplication(String packageName) {
        LauncherApplication application = applicationMap.getOrDefault(packageName, LauncherApplication.FALLBACK_APP);

        if (application != LauncherApplication.FALLBACK_APP) {
            for (LauncherApplicationListener listener : listeners) {
                if (listener != null) {
                    listener.onPrepareRemoval();
                }
            }

            applicationMap.remove(packageName);
            visibleApplicationList.remove(application);
            applicationList.remove(application);

            iconPacks.remove(application);
            if (mainUser) {
                CategoryManager.getInstance().removeApplication(application);
            }

            for (LauncherApplicationListener listener : listeners) {
                if (listener != null) {
                    listener.onRemoved(application);
                }
            }
        }
    }

    public synchronized void showApplication(LauncherApplication application) {
        hiddenApplications.edit()
                .remove(application.info.packageName)
                .apply();

        if (!visibleApplicationList.contains(application)) {
            addApplicationToList(application, visibleApplicationList);
            if (mainUser) {
                CategoryManager.getInstance().addApplication(application);
            }

            for (LauncherApplicationListener listener : listeners) {
                if (listener != null) {
                    listener.onShowed(application);
                }
            }
        }
    }

    public synchronized void hideApplication(LauncherApplication application) {
        hiddenApplications.edit()
                .putBoolean(application.info.packageName, true)
                .apply();

        for (LauncherApplicationListener listener : listeners) {
            if (listener != null) {
                listener.onPrepareHiding();
            }
        }

        visibleApplicationList.remove(application);
        if (mainUser) {
            CategoryManager.getInstance().removeApplication(application);
        }

        for (LauncherApplicationListener listener : listeners) {
            if (listener != null) {
                listener.onHidden(application);
            }
        }
    }

    public void addApplicationListener(LauncherApplicationListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeApplicationListener(LauncherApplicationListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public boolean isVisibleToUser(LauncherApplication application) {
        return isVisibleToUser(application.info.packageName);
    }

    public boolean isVisibleToUser(String packageName) {
        return !hiddenApplications.contains(packageName);
    }

    /**
     * @return Index of application in the hidden list
     **/
    public int indexOf(LauncherApplication application) {
        return applicationList.indexOf(application);
    }
}
