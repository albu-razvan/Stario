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
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.stario.launcher.BuildConfig;
import com.stario.launcher.apps.categories.CategoryManager;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.utils.ThreadSafeArrayList;
import com.stario.launcher.utils.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LauncherApplicationManager {
    private static final String TAG = "Applications";
    private static LauncherApplicationManager instance = null;
    private final BroadcastReceiver receiver;
    private final List<LauncherApplication> applicationList;
    private final List<LauncherApplication> applicationListHidden;
    private final Map<String, LauncherApplication> applicationMap;
    private final SharedPreferences hiddenApplications;
    private final SharedPreferences applicationLabels;
    private final List<ApplicationListener> listeners;
    private final PackageManager packageManager;
    private final CategoryManager categoryManager;
    private final IconPackManager iconPacks;
    private boolean registered;

    private LauncherApplicationManager(ThemedActivity activity) {
        // CategoryData and IconPackManager needs LauncherApplicationManager
        // to be instantiated. Assign the instance in the constructor before
        // everything else to guarantee that the instance will be supplied.
        instance = this;

        this.applicationList = new ThreadSafeArrayList<>();
        this.applicationListHidden = new ThreadSafeArrayList<>();
        this.applicationMap = new HashMap<>();
        this.listeners = new ThreadSafeArrayList<>();
        this.applicationLabels = activity.getSharedPreferences(Entry.APPLICATION_LABELS);
        this.hiddenApplications = activity.getSharedPreferences(Entry.HIDDEN_APPS);
        this.packageManager = activity.getPackageManager();

        this.registered = false;

        this.iconPacks = IconPackManager.from(activity, this::updateIcons);

        this.receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Uri data = intent.getData();

                if (data == null) {
                    return;
                }

                LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);

                String packageName = data.getSchemeSpecificPart();
                String action = intent.getAction();

                boolean containsPackage = applicationMap.containsKey(packageName);

                if (action != null && !BuildConfig.APPLICATION_ID.equals(packageName)) {
                    Utils.submitTask(() -> {
                        if ((action.equals(Intent.ACTION_PACKAGE_REMOVED) &&
                                !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) ||
                                action.equals(Intent.ACTION_PACKAGE_FULLY_REMOVED)) {

                            removeApplication(packageName);
                        } else {
                            ApplicationInfo applicationInfo;

                            try {
                                applicationInfo = packageManager.getApplicationInfo(packageName, 0);

                                if (launcherApps.getActivityList(packageName,
                                        UserHandle.getUserHandleForUid(Process.myUid())).isEmpty()) {
                                    // Doesn't have any activity that specifies
                                    // Intent.ACTION_MAIN and Intent.CATEGORY_LAUNCHER

                                    if (containsPackage) {
                                        removeApplication(packageName);
                                    }

                                    return;
                                }
                            } catch (PackageManager.NameNotFoundException exception) {
                                Log.e(TAG, "Package " + packageName + " does not exist.");

                                return;
                            }

                            if ((action.equals(Intent.ACTION_PACKAGE_CHANGED) ||
                                    action.equals(Intent.ACTION_PACKAGE_VERIFIED) ||
                                    action.equals(Intent.ACTION_PACKAGE_REPLACED)) &&
                                    containsPackage) {

                                LauncherApplication application = get(packageName);

                                if (application != null) {
                                    if (!applicationInfo.enabled) {
                                        removeApplication(packageName);
                                    } else {
                                        application.info = applicationInfo;
                                        updateApplication(application);
                                    }
                                }
                            } else if ((action.equals(Intent.ACTION_PACKAGE_ADDED) &&
                                    intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) ||
                                    (!containsPackage && action.equals(Intent.ACTION_PACKAGE_CHANGED))) {

                                if (applicationInfo.enabled) {
                                    addApplication(createApplication(applicationInfo));
                                }
                            }
                        }
                    });
                }
            }
        };

        categoryManager = CategoryManager.from(activity);

        Utils.submitTask(() -> {
            //TODO multiple user support
            LauncherApps launcherApps = (LauncherApps) activity.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            List<LauncherActivityInfo> activityInfoList =
                    launcherApps.getActivityList(null, UserHandle.getUserHandleForUid(Process.myUid()));

            for (int index = 0; index < activityInfoList.size(); index++) {
                ApplicationInfo applicationInfo = activityInfoList.get(index).getApplicationInfo();

                if (iconPacks.checkPackValidity(applicationInfo.packageName)) {
                    LauncherActivityInfo activityInfo = activityInfoList.remove(index);
                    activityInfoList.add(0, activityInfo);
                }
            }

            for (int index = 0; index < activityInfoList.size(); index++) {
                ApplicationInfo applicationInfo = activityInfoList.get(index).getApplicationInfo();

                if (applicationInfo != null) {
                    if (!BuildConfig.APPLICATION_ID.equals(applicationInfo.packageName)) {
                        if (applicationMap.containsKey(applicationInfo.packageName)) {
                            LauncherApplication application = get(applicationInfo.packageName);

                            if (application != null) {
                                iconPacks.updateIcon(application, this::notifyUpdate);
                            }
                        } else {
                            addApplication(createApplication(applicationInfo));
                        }
                    }
                }
            }
        });
    }

    public static LauncherApplicationManager from(@NonNull ThemedActivity activity) {
        if (instance == null) {
            instance = new LauncherApplicationManager(activity);
        } else {
            instance.iconPacks.refresh();
            instance.updateIcons();
        }

        if (!instance.registered) {
            if (Utils.isMinimumSDK(Build.VERSION_CODES.TIRAMISU)) {
                activity.registerReceiver(instance.receiver, getIntentFilter(), Context.RECEIVER_EXPORTED);
            } else {
                //noinspection UnspecifiedRegisterReceiverFlag
                activity.registerReceiver(instance.receiver, getIntentFilter());
            }

            Lifecycle lifecycle = activity.getLifecycle();
            lifecycle.addObserver(new DefaultLifecycleObserver() {
                @Override
                public void onDestroy(@NonNull LifecycleOwner owner) {
                    try {
                        activity.unregisterReceiver(instance.receiver);
                        instance.registered = false;

                        instance.listeners.clear();
                    } catch (Exception exception) {
                        Log.e(TAG, "Receiver not registered");
                    }

                    lifecycle.removeObserver(this);
                }
            });
        }

        return instance;
    }

    public static LauncherApplicationManager getInstance() {
        if (instance == null) {
            throw new RuntimeException("Applications not initialized.");
        }

        return instance;
    }

    private static IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_VERIFIED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        intentFilter.addDataScheme("package");

        return intentFilter;
    }

    public void updateApplication(LauncherApplication application) {
        UUID category = categoryManager.getCategoryIdentifier(application.info);

        if (!application.category.equals(category)) {
            categoryManager.removeApplication(application);
            application.category = category;
            categoryManager.addApplication(application);
        }

        iconPacks.updateIcon(application, LauncherApplicationManager.this::notifyUpdate);

        for (ApplicationListener listener : listeners) {
            if (listener != null) {
                listener.onUpdated(application);
            }
        }
    }

    void updateIcons() {
        for (LauncherApplication application : applicationListHidden) {
            iconPacks.updateIcon(application, this::notifyUpdate);
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

            if (!hiddenApplications.contains(application.info.packageName)) {
                hideApplication(application);
                showApplication(application);
            }
        }
    }

    void notifyUpdate(LauncherApplication application) {
        for (ApplicationListener listener : listeners) {
            if (listener != null) {
                listener.onUpdated(application);
            }
        }
    }

    private LauncherApplication createApplication(ApplicationInfo applicationInfo) {
        LauncherApplication application;
        String label = applicationLabels.getString(applicationInfo.packageName, null);

        //noinspection ReplaceNullCheck
        if (label != null) {
            application = new LauncherApplication(applicationInfo, label);
        } else {
            application = new LauncherApplication(applicationInfo,
                    applicationInfo.loadLabel(packageManager).toString());
        }

        application.category = categoryManager.getCategoryIdentifier(applicationInfo);

        return application;
    }

    /**
     * Method to get the {@link LauncherApplication} object at index
     *
     * @param index The index of the application
     * @return {@link LauncherApplication} at index
     */
    @Nullable
    public LauncherApplication get(int index) {
        if (index >= 0 && index < applicationList.size()) {
            return applicationList.get(index);
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
            if (index >= 0 && index < applicationListHidden.size()) {
                return applicationListHidden.get(index);
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
        return applicationListHidden.size();
    }

    private synchronized void addApplication(LauncherApplication application) {
        boolean hidden = hiddenApplications.contains(application.info.packageName);

        applicationMap.put(application.info.packageName, application);

        addApplicationToList(application, applicationList);
        if (!hidden) {
            addApplicationToList(application, applicationListHidden);

            for (ApplicationListener listener : listeners) {
                if (listener != null) {
                    listener.onInserted(application);
                }
            }
        }

        iconPacks.add(application);
        iconPacks.updateIcon(application, this::notifyUpdate);

        categoryManager.addApplication(application);
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
            } else {
                return;
            }
        }

        list.add(left, applicationToAdd);
    }

    private synchronized void removeApplication(String packageName) {
        LauncherApplication application = applicationMap.getOrDefault(packageName, LauncherApplication.FALLBACK_APP);

        if (application != LauncherApplication.FALLBACK_APP) {
            for (ApplicationListener listener : listeners) {
                if (listener != null) {
                    listener.onPrepareRemoval();
                }
            }

            applicationMap.remove(packageName);
            applicationList.remove(application);
            applicationListHidden.remove(application);

            iconPacks.remove(application);

            categoryManager.removeApplication(application);

            for (ApplicationListener listener : listeners) {
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

        if (!applicationListHidden.contains(application)) {
            addApplicationToList(application, applicationListHidden);
            categoryManager.addApplication(application);

            for (ApplicationListener listener : listeners) {
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

        for (ApplicationListener listener : listeners) {
            if (listener != null) {
                listener.onPrepareHiding();
            }
        }

        applicationListHidden.remove(application);
        categoryManager.removeApplication(application);

        for (ApplicationListener listener : listeners) {
            if (listener != null) {
                listener.onHidden(application);
            }
        }
    }

    public void addApplicationListener(ApplicationListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * @return Index of application in the hidden list
     **/
    public int indexOf(LauncherApplication application) {
        return applicationListHidden.indexOf(application);
    }

    public interface ApplicationListener {
        default void onInserted(LauncherApplication application) {
        }

        default void onShowed(LauncherApplication application) {
        }

        /**
         * This will always be called before and in the same UI frame as {@link #onRemoved(LauncherApplication)}
         */
        default void onPrepareRemoval() {
        }

        /**
         * This will always be called after and in the same UI frame as {@link #onPrepareRemoval()}
         */
        default void onRemoved(LauncherApplication application) {
        }

        /**
         * This will always be called before and in the same UI frame as {@link #onHidden(LauncherApplication)}
         */
        default void onPrepareHiding() {
        }

        /**
         * This will always be called after and in the same UI frame as {@link #onPrepareHiding()}
         */
        default void onHidden(LauncherApplication application) {
        }

        default void onUpdated(LauncherApplication application) {
        }
    }
}