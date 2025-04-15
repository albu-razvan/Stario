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

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.Pair;
import android.util.Xml;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stario.launcher.BuildConfig;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.utils.ImageUtils;
import com.stario.launcher.ui.utils.UiUtils;
import com.stario.launcher.utils.Utils;

import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class IconPackManager {
    public static final String ICON_PACK_ENTRY = "com.stario.ICON_PACK";
    public static final String CORNER_RADIUS_ENTRY = "com.stario.CORNER_RADIUS";
    public static final String PATH_ALGORITHM_ENTRY = "com.stario.PATH_ALGORITHM";

    private static final String JSON_ICON_PACK = "pack";
    private static final String JSON_ICON_DRAWABLE_NAME = "drawable";

    /*Applications that changed their launch components along the years.
      Feel free to update this whenever you find other apps that did so.*/
    private static final HashMap<String, String> changedComponents = new HashMap<>() {{
        put("com.google.android.googlequicksearchbox/com.google.android.googlequicksearchbox.SearchActivity",
                "com.google.android.googlequicksearchbox/com.google.android.googlequicksearchbox.GoogleAppImplicitMainInfoGatewayInternal");
        put("com.google.android.apps.safetyhub/com.google.android.apps.safetyhub.LauncherActivity",
                "com.google.android.apps.safetyhub/com.google.android.apps.safetyhub.home.HomePageAppInfoEntry");
    }};
    private static IconPackManager instance = null;

    private final LauncherApplicationManager launcherApplicationManager;
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
        this.launcherApplicationManager = LauncherApplicationManager.getInstance();

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

    public void setIconFor(LauncherApplication application, IconPack pack) {
        setIconFor(application.info.packageName, pack);
    }

    public void setIconFor(LauncherApplication application, IconPack pack, String drawableName) {
        setIconFor(application.info.packageName, pack, drawableName);
    }

    public void setIconFor(String packageName, IconPack pack) {
        setIconFor(packageName, pack, null);
    }

    public void setIconFor(String packageName, IconPack pack, String drawableName) {
        if (pack != null) {
            String json = '{' + JSON_ICON_PACK + ":\"" + pack.application.info.packageName + "\"";

            if (drawableName != null) {
                json = json + ',' + JSON_ICON_DRAWABLE_NAME + ":\"" + drawableName + "\"";
            }

            preferences.edit()
                    .putString(packageName, json + "}")
                    .apply();
        } else {
            preferences.edit()
                    .putString(packageName, BuildConfig.APPLICATION_ID)
                    .apply();
        }

        LauncherApplication application = launcherApplicationManager.get(packageName);

        if (application != null) {
            updateIcon(application, launcherApplicationManager::notifyUpdate);
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

    synchronized void updateIcon(@NonNull LauncherApplication application, OnUpdate listener) {
        IconPack pack = activeIconPack;
        String drawableName = null;

        if (preferences.contains(application.info.packageName)) {
            String packagePreference = preferences.getString(application.info.packageName, null);

            if (packagePreference != null) {
                if (packagePreference.equals(BuildConfig.APPLICATION_ID)) {
                    pack = null;
                } else {
                    try {
                        JSONObject json = new JSONObject(packagePreference);

                        IconPack target = getPack((String) json.get(JSON_ICON_PACK));
                        if (target != null) {
                            pack = target;

                            if (json.has(JSON_ICON_DRAWABLE_NAME)) {
                                drawableName = (String) json.get(JSON_ICON_DRAWABLE_NAME);
                            }
                        }
                    } catch (Exception exception) {
                        Log.e("IconPackManager", "loadDrawable: Malformed JSON icon store for package " +
                                application.info.packageName);
                    }
                }
            }
        }

        if (pack != null) {
            CompletableFuture<Drawable> future = pack.loadDrawable(application.getInfo().packageName, drawableName);

            future.thenAccept(icon -> {
                if (icon == null) {
                    icon = ImageUtils.getIcon(application.getInfo(), packageManager);
                }

                final Drawable drawable = icon;
                UiUtils.runOnUIThread(() -> {
                    if (drawable != null && !drawable.equals(application.getIcon())) {
                        application.icon = drawable;

                        if (listener != null) {
                            listener.onUpdate(application);
                        }
                    }
                });
            });
        } else {
            Drawable icon = ImageUtils.getIcon(application.getInfo(), packageManager);

            if (icon != null && !icon.equals(application.getIcon())) {
                application.icon = icon;

                if (listener != null) {
                    listener.onUpdate(application);
                }
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
            pack.exactComponentDrawable.clear();
            pack.loaded = false;
        }
    }

    public boolean checkPackValidity(LauncherApplication application) {
        return checkPackValidity(application.info.packageName);
    }

    public boolean checkPackValidity(String packageName) {
        //ADW Launcher
        return new Intent("org.adw.launcher.THEMES")
                .setPackage(packageName)
                .resolveActivity(packageManager) != null ||
                //Lawnchair Launcher 14
                new Intent("app.lawnchair.icons.THEMED_ICON")
                        .setPackage(packageName)
                        .resolveActivity(packageManager) != null ||
                //Lawnchair Launcher Legacy
                new Intent("ch.deletescape.lawnchair.ICONPACK")
                        .setPackage(packageName)
                        .resolveActivity(packageManager) != null ||
                //Nova Launcher
                new Intent("com.novalauncher.THEME")
                        .setPackage(packageName)
                        .resolveActivity(packageManager) != null ||
                //GO Launcher
                new Intent("com.gau.go.launcherex.theme")
                        .setPackage(packageName)
                        .resolveActivity(packageManager) != null ||
                //Smart Launcher
                new Intent("ginlemon.smartlauncher.THEMES")
                        .setPackage(packageName)
                        .resolveActivity(packageManager) != null ||
                //TSF Shell
                new Intent("com.tsf.shell.themes")
                        .setPackage(packageName)
                        .resolveActivity(packageManager) != null ||
                //OnePlus Launcher
                new Intent("net.oneplus.launcher.icons.ACTION_PICK_ICON")
                        .setPackage(packageName)
                        .resolveActivity(packageManager) != null ||
                //Moto Launcher
                new Intent("com.motorola.launcher3.ACTION_ICON_PACK")
                        .setPackage(packageName)
                        .resolveActivity(packageManager) != null;
    }

    public List<Pair<IconPack, Pair<String, Drawable>>> getIcons(LauncherApplication application) {
        return getIcons(application.info.packageName);
    }

    public List<Pair<IconPack, Pair<String, Drawable>>> getIcons(String packageName) {
        List<Pair<IconPack, Pair<String, Drawable>>> result = new ArrayList<>();

        result.add(new Pair<>(null, new Pair<>(null, ImageUtils.getIcon(packageName, packageManager))));

        for (IconPack pack : iconPacks) {
            List<String> drawableNames = pack.getDrawableNameList(packageName);

            if (drawableNames != null && drawableNames.size() > 0) {
                for (String drawableName : drawableNames) {
                    Drawable drawable = pack.getDrawable(drawableName);

                    if (drawable != null) {
                        result.add(new Pair<>(pack, new Pair<>(drawableName, drawable)));
                    }
                }
            }
        }

        return result;
    }

    public interface OnUpdate {
        void onUpdate(LauncherApplication application);
    }

    @SuppressLint("DiscouragedApi")
    public class IconPack {
        private static final String TAG = "IconPackManager";
        private final LauncherApplication application;
        private final HashMap<String, List<String>> exactComponentDrawable;
        private final HashMap<String, List<String>> packageNameDrawables;
        private Resources resources;
        private boolean loaded;

        private IconPack(LauncherApplication application) {
            this.application = application;
            this.exactComponentDrawable = new HashMap<>();
            this.packageNameDrawables = new HashMap<>();
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
                            if (parser.getName().equals("item") || parser.getName().equals("calendar")) {
                                String componentName = null;
                                String drawableName = null;
                                String prefix = null;

                                for (int i = 0; i < parser.getAttributeCount(); i++) {
                                    if (parser.getAttributeName(i).equals("component")) {
                                        componentName = parser.getAttributeValue(i);

                                        if (componentName.indexOf('{') != -1 &&
                                                componentName.indexOf('{') + 1 < componentName.lastIndexOf('}')) {
                                            componentName = componentName.substring(componentName.indexOf('{') + 1, componentName.lastIndexOf('}'));
                                        }
                                    } else if (parser.getAttributeName(i).equals("drawable")) {
                                        drawableName = parser.getAttributeValue(i);
                                    } else if (parser.getAttributeName(i).equals("prefix")) {
                                        prefix = parser.getAttributeValue(i);
                                    }
                                }

                                if (componentName != null && componentName.contains("/")) {
                                    if (drawableName != null) {
                                        saveDrawable(componentName, drawableName);
                                    }

                                    if (prefix != null) {
                                        for (int day = 1; day <= 31; day++) {
                                            saveDrawable(componentName, prefix + day);
                                        }
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

        private void saveDrawable(String componentName, String drawableName) {
            List<String> drawables = exactComponentDrawable.get(componentName);
            if (drawables == null) {
                drawables = new ArrayList<>();

                drawables.add(drawableName);

                exactComponentDrawable.put(componentName, drawables);
            } else {
                if (!drawables.contains(drawableName)) {
                    drawables.add(drawableName);
                }
            }

            if (changedComponents.containsKey(componentName)) {
                String changedComponent = changedComponents.get(componentName);

                drawables = exactComponentDrawable.get(changedComponent);
                if (drawables == null) {
                    drawables = new ArrayList<>();
                    drawables.add(drawableName);

                    exactComponentDrawable.put(changedComponent, drawables);
                } else {
                    if (!drawables.contains(drawableName)) {
                        drawables.add(drawableName);
                    }
                }
            }

            String packageName = componentName.substring(0, componentName.indexOf('/'));
            drawables = packageNameDrawables.get(packageName);
            if (drawables == null) {
                drawables = new ArrayList<>();

                drawables.add(drawableName);

                packageNameDrawables.put(packageName, drawables);
            } else {
                if (!drawables.contains(drawableName)) {
                    drawables.add(drawableName);
                }
            }
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        private Drawable getDrawable(String drawableName) {
            int id = resources.getIdentifier(drawableName,
                    "drawable", application.info.packageName);

            if (id > 0) {
                return resources.getDrawable(id);
            } else {
                return null;
            }
        }

        public CompletableFuture<Drawable> loadDrawable(String packageName, String drawable) {
            CompletableFuture<Drawable> future = new CompletableFuture<>();

            Utils.submitTask(() -> {
                String drawableName = drawable;

                if (!loaded) {
                    load();
                }

                Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);

                if (launchIntent != null) {
                    if (drawableName == null) {
                        ComponentName component = launchIntent.getComponent();

                        if (component != null) {
                            List<String> drawableNames = exactComponentDrawable.get(component.getPackageName() + '/' + component.getClassName());

                            if (drawableNames != null && drawableNames.size() > 0) {
                                drawableName = drawableNames.get(0);
                            }
                        }
                    }

                    if (drawableName != null) {
                        future.complete(getDrawable(drawableName));
                    }
                }

                future.complete(null);
            });

            return future;
        }

        private List<String> getDrawableNameList(String packageName) {
            if (!loaded) {
                load();
            }

            Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);

            if (launchIntent != null) {
                ComponentName component = launchIntent.getComponent();

                if (component != null) {
                    return packageNameDrawables.get(component.getPackageName());
                }
            }

            return null;
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
                    future.complete(exactComponentDrawable.size());
                } else {
                    Utils.submitTask(() -> {
                        load();

                        future.complete(exactComponentDrawable.size());
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
