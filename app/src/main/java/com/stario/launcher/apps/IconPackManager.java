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

package com.stario.launcher.apps;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.Xml;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stario.launcher.preferences.Entry;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.utils.ImageUtils;
import com.stario.launcher.utils.UiUtils;
import com.stario.launcher.utils.Utils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class IconPackManager {
    public static final String ICON_PACK_ENTRY = "com.stario.ICON_PACK";
    public static final String CORNER_RADIUS_ENTRY = "com.stario.CORNER_RADIUS";
    public static final String PATH_ALGORITHM_ENTRY = "com.stario.PATH_ALGORITHM";

    /*Applications that changed their launch components along the years.
      Feel free to update this whenever you find other apps that did so.*/
    private static final HashMap<String, String> changedComponents = new HashMap<>() {{
        put("com.google.android.googlequicksearchbox/com.google.android.googlequicksearchbox.SearchActivity",
                "com.google.android.googlequicksearchbox/com.google.android.googlequicksearchbox.GoogleAppImplicitMainInfoGatewayInternal");
        put("com.google.android.apps.safetyhub/com.google.android.apps.safetyhub.LauncherActivity",
                "com.google.android.apps.safetyhub/com.google.android.apps.safetyhub.home.HomePageAppInfoEntry");
    }};
    private static IconPackManager instance = null;
    private final PackageManager packageManager;
    private final SharedPreferences preferences;
    private final ArrayList<IconPack> iconPacks;
    private OnChangeListener listener;
    private IconPack activeIconPack;

    private IconPackManager(ThemedActivity activity, OnChangeListener listener) {
        this.iconPacks = new ArrayList<>();
        this.listener = listener;
        this.preferences = activity.getSharedPreferences(Entry.ICONS);
        this.packageManager = activity.getPackageManager();

        this.activeIconPack = null;
    }

    public static IconPackManager from(@NonNull ThemedActivity activity) {
        if (instance == null) {
            instance = new IconPackManager(activity, null);
        }

        return instance;
    }

    static IconPackManager from(@NonNull ThemedActivity activity,
                                OnChangeListener listener) {
        if (instance == null) {
            instance = new IconPackManager(activity, listener);
        } else {
            instance.listener = listener;
        }

        return instance;
    }

    public void setActiveIconPack(IconPack pack) {
        preferences.edit()
                .putString(ICON_PACK_ENTRY,
                        pack != null ? pack.application.info.packageName : null)
                .apply();

        activeIconPack = pack;

        if (listener != null) {
            listener.onChange();
        }
    }

    public int getCount() {
        return iconPacks.size();
    }

    public IconPack getPack(int index) {
        return iconPacks.get(index);
    }

    public IconPack getPack(String packageName) {
        for (int index = 0; index < iconPacks.size(); index++) {
            if (iconPacks.get(index)
                    .application.info
                    .packageName.equals(packageName)) {
                return getPack(index);
            }
        }

        return null;
    }

    synchronized void updateIcon(LauncherApplication application, OnUpdate listener) {
        if (activeIconPack != null) {
            CompletableFuture<Drawable> future = activeIconPack.getDrawable(application.getInfo().packageName);

            future.thenAccept(icon -> {
                if (icon == null) {
                    icon = ImageUtils.getIcon(application.getInfo(), packageManager);
                }

                final Drawable drawable = icon;
                UiUtils.runOnUIThread(() -> {
                    if (drawable != null && !drawable.equals(application.getIcon())) {
                        application.icon = drawable;

                        listener.onUpdate(application);
                    }
                });
            });
        } else {
            Drawable icon = ImageUtils.getIcon(application.getInfo(), packageManager);

            if (icon != null && !icon.equals(application.getIcon())) {
                application.icon = icon;

                listener.onUpdate(application);
            }
        }
    }

    synchronized void remove(LauncherApplication application) {
        if (application != null) {
            for (int index = 0; index < iconPacks.size(); index++) {
                if (application.equals(iconPacks.get(index).application)) {
                    iconPacks.remove(index);

                    return;
                }
            }
        }
    }

    synchronized void add(LauncherApplication application) {
        if (checkPackValidity(application)) {
            IconPack iconPack = new IconPack(application);
            iconPacks.add(iconPack);

            if (application.info.packageName
                    .equals(preferences.getString(ICON_PACK_ENTRY, null))) {
                activeIconPack = iconPack;

                if (listener != null) {
                    listener.onChange();
                }
            }
        }
    }

    synchronized void refresh() {
        for (IconPack pack : iconPacks) {
            pack.packageDrawables.clear();
        }
    }

    public boolean checkPackValidity(LauncherApplication application) {
        return new Intent("org.adw.launcher.THEMES")
                .setPackage(application.info.packageName)
                .resolveActivity(packageManager) != null ||
                new Intent("com.gau.go.launcherex.theme")
                        .setPackage(application.info.packageName)
                        .resolveActivity(packageManager) != null;
    }

    public interface OnUpdate {
        void onUpdate(LauncherApplication application);
    }

    @SuppressLint("DiscouragedApi")
    public class IconPack {
        private static final String TAG = "IconPackManager";
        private final LauncherApplication application;
        private final HashMap<String, String> packageDrawables;
        private Resources resources;
        private boolean loaded;

        private IconPack(LauncherApplication application) {
            this.application = application;
            this.packageDrawables = new HashMap<>();
            this.loaded = false;
        }

        void load() {
            try {
                XmlPullParser parser = null;

                resources = packageManager.getResourcesForApplication(application.info.packageName);
                int appFilterId = resources.getIdentifier("appfilter", "xml",
                        application.info.packageName);

                if (appFilterId > 0) {
                    parser = resources.getXml(appFilterId);
                } else {
                    try {
                        InputStream appFilterStream = resources.getAssets()
                                .open("appfilter.xml");

                        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                        factory.setNamespaceAware(true);

                        parser = factory.newPullParser();
                        parser.setInput(appFilterStream, Xml.Encoding.UTF_8.toString());
                    } catch (IOException e1) {
                        Log.d(TAG, "No appfilter.xml file");
                    }
                }

                if (parser != null) {
                    int eventType = parser.getEventType();

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            if (parser.getName().equals("item")) {
                                String componentName = null;
                                String drawableName = null;

                                for (int i = 0; i < parser.getAttributeCount(); i++) {
                                    if (parser.getAttributeName(i).equals("component")) {
                                        componentName = parser.getAttributeValue(i);

                                        if (componentName.indexOf('{') != -1 &&
                                                componentName.indexOf('{') + 1 < componentName.lastIndexOf('}')) {
                                            componentName = componentName.substring(componentName.indexOf('{') + 1, componentName.lastIndexOf('}'));
                                        }
                                    } else if (parser.getAttributeName(i).equals("drawable")) {
                                        drawableName = parser.getAttributeValue(i);
                                    }
                                }

                                if (!packageDrawables.containsKey(componentName)) {
                                    packageDrawables.put(componentName, drawableName);

                                    if (changedComponents.containsKey(componentName)) {
                                        packageDrawables.put(changedComponents.get(componentName), drawableName);
                                    }
                                }
                            }
                        }
                        eventType = parser.next();
                    }
                }

                loaded = true;
            } catch (PackageManager.NameNotFoundException exception) {
                Log.d(TAG, "Cannot load icon pack");
            } catch (XmlPullParserException exception) {
                Log.d(TAG, "Cannot parse icon pack appfilter.xml");
            } catch (IOException exception) {
                Log.e(TAG, "", exception);
            }
        }

        private Drawable loadDrawable(String drawableName) {
            int id = resources.getIdentifier(drawableName,
                    "drawable", application.info.packageName);

            if (id > 0) {
                return resources.getDrawable(id);
            } else {
                return null;
            }
        }

        public CompletableFuture<Drawable> getDrawable(String packageName) {
            CompletableFuture<Drawable> future = new CompletableFuture<>();

            Utils.submitTask(() -> {
                if (!loaded) {
                    load();
                }

                Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);

                if (launchIntent != null) {
                    ComponentName component = launchIntent.getComponent();

                    if (component != null) {
                        String drawableName = packageDrawables.get(component.getPackageName() + '/' + component.getClassName());

                        if (drawableName != null) {
                            future.complete(loadDrawable(drawableName));
                        } else {
                            // try to get a resource with the component filename
                            String componentName = component.getClassName();

                            int start = componentName.indexOf("{") + 1;
                            int end = componentName.indexOf("}", start);
                            if (end > start) {
                                drawableName = componentName.substring(start, end)
                                        .toLowerCase(Locale.getDefault()).replace(".", "_")
                                        .replace("/", "_");
                                if (resources.getIdentifier(drawableName, "drawable", packageName) > 0) {
                                    future.complete(loadDrawable(drawableName));
                                }
                            }
                        }
                    }
                }

                future.complete(null);
            });

            return future;
        }

        public String getLabel() {
            return application.getLabel();
        }

        public Drawable getIcon() {
            return application.getIcon();
        }

        public CompletableFuture<Integer> getComponentCount() {
            CompletableFuture<Integer> future = new CompletableFuture<>();

            UiUtils.runOnUIThread(() -> {
                if (loaded) {
                    future.complete(packageDrawables.size());
                } else {
                    Utils.submitTask(() -> {
                        load();

                        future.complete(packageDrawables.size());
                    });
                }
            });

            return future;
        }

        @Override
        public boolean equals(@Nullable Object object) {
            return object instanceof IconPack &&
                    application.equals(((IconPack) object).application);
        }

        @Override
        public int hashCode() {
            return application.hashCode();
        }
    }

    interface OnChangeListener {
        void onChange();
    }
}
