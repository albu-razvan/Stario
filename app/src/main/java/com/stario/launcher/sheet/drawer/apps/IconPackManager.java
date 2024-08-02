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

package com.stario.launcher.sheet.drawer.apps;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.util.Log;
import android.util.Xml;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.utils.ImageUtils;
import com.stario.launcher.utils.Utils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

//TODO icon pack support
public class IconPackManager {
    /*Applications that changed their launch components along the years.
      Feel free to update this whenever you find other apps that did so.*/
    private static final HashMap<String, String> changedComponents = new HashMap<>() {{
        put("com.google.android.googlequicksearchbox/com.google.android.googlequicksearchbox.SearchActivity",
                "com.google.android.googlequicksearchbox/com.google.android.googlequicksearchbox.GoogleAppImplicitMainInfoGatewayInternal");
        put("com.google.android.apps.safetyhub/com.google.android.apps.safetyhub.LauncherActivity",
                "com.google.android.apps.safetyhub/com.google.android.apps.safetyhub.home.HomePageAppInfoEntry");
    }};
    private static IconPackManager instance = null;
    private final ArrayList<IconPack> iconPacks;
    private final PackageManager packageManager;

    //TODO refresh icons on theme change
    private IconPackManager(ThemedActivity activity) {
        this.iconPacks = new ArrayList<>();
        this.packageManager = activity.getPackageManager();
    }

    static IconPackManager from(@NonNull ThemedActivity activity) {
        if (instance == null) {
            instance = new IconPackManager(activity);
        }

        return instance;
    }

    //TODO
    void updateIcon(LauncherApplication application, OnUpdate listener) {
        Drawable icon;

        icon = ImageUtils.getIcon(application.getInfo(), packageManager);
        //icon = iconPack.getDrawable(application.getInfo().packageName);

        if (icon != null && !icon.equals(application.getIcon())) {
            application.icon = icon;

            listener.onUpdate(application);
        }
    }

    //TODO
    void remove(LauncherApplication application) {
        //iconPacks.remove();
    }

    boolean add(LauncherApplication application) {
        if (checkPackValidity(application)) {
            iconPacks.add(new IconPack(application));

            return true;
        }

        return false;
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
    class IconPack {
        private static final String TAG = "IconPackManager";
        private final LauncherApplication application;
        private final HashMap<String, String> packageDrawables;
        private Resources resources;
        private boolean loaded;

        IconPack(LauncherApplication application) {
            this.application = application;
            this.packageDrawables = new HashMap<>();
            this.loaded = false;
        }

        public void load() {
            Utils.submitTask(() -> {
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
            });
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

        public AdaptiveIconDrawable getDrawable(String packageName) {
            if (!loaded) {
                load();
            }

            Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);

            if (launchIntent != null) {
                ComponentName component = launchIntent.getComponent();

                if (component != null) {
                    Log.i(TAG, "getDrawable: " + component);

                    String drawable = packageDrawables.get(component.getPackageName() + '/' + component.getClassName());

                    if (drawable != null) {
                        Drawable drawableIcon = loadDrawable(drawable);

                        if (drawableIcon instanceof AdaptiveIconDrawable) {
                            return (AdaptiveIconDrawable) drawableIcon;
                        }

                        Drawable foreground = new ScaleDrawable(drawableIcon, Gravity.CENTER, 1f, 1f);
                        foreground.setLevel((int) (6400 * ImageUtils.calculateMultiplier(drawableIcon)));
                        foreground.setChangingConfigurations(0);
                        //Drawable background = new ColorDrawable(BitmapManipulations.CUSTOM_ICON);
                        Drawable background = new ColorDrawable(Color.WHITE);
                        background.setChangingConfigurations(0);

                        try {
                            return new AdaptiveIconDrawable(background, foreground);
                        } catch (Exception e) {
                            return null;
                        }
                    } else {
                        // try to get a resource with the component filename
                        /*int start = componentName.indexOf("{") + 1;
                        int end = componentName.indexOf("}", start);
                        if (end > start) {
                            drawable = componentName.substring(start, end).toLowerCase(Locale.getDefault()).replace(".", "_").replace("/", "_");
                            if (resources.getIdentifier(drawable, "drawable", packageName) > 0) {
                                Drawable drawableIcon = loadDrawable(drawable);

                                if (drawableIcon instanceof AdaptiveIconDrawable)
                                    return (AdaptiveIconDrawable) drawableIcon;

                                Drawable foreground = new ScaleDrawable(drawableIcon, Gravity.CENTER, 1f, 1f);
                                foreground.setLevel((int) (6400 * ImageUtils.calculateMultiplier(drawableIcon)));
                                foreground.setChangingConfigurations(0);
                                //Drawable background = new ColorDrawable(BitmapManipulations.CUSTOM_ICON);
                                Drawable background = new ColorDrawable(Color.WHITE);
                                background.setChangingConfigurations(0);

                                try {
                                    return new AdaptiveIconDrawable(background, foreground);
                                } catch (Exception e) {
                                    return null;
                                }
                            }
                        }*/
                    }
                }
            }

            return null;
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
}
