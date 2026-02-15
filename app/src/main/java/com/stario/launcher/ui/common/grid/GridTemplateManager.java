/*
 * Copyright (C) 2026 Răzvan Albu
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

package com.stario.launcher.ui.common.grid;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;

import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.stario.launcher.Stario;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.utils.Utils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GridTemplateManager {
    private static final String TAG = "GridTemplateManager";

    private final Map<String, GridTemplate> templateCache;
    private final SharedPreferences prefs;

    public GridTemplateManager(@NonNull Stario context,
                               @NonNull String identifier, @RawRes int templateId) {
        this.templateCache = new HashMap<>();
        this.prefs = context.getSharedPreferences(Entry.GRID_TEMPLATE_MANAGER
                .toSubPreference(identifier), Context.MODE_PRIVATE);

        if (templateId == 0) {
            Log.w(TAG, "GridTemplateManager: a template file has not been provided.");
        } else {
            try {
                InputStream inputStream = context.getResources().openRawResource(templateId);

                List<GridTemplate> list = Utils.getGsonInstance()
                        .fromJson(new InputStreamReader(inputStream),
                                new TypeToken<List<GridTemplate>>() {
                                }.getType());

                if (list != null) {
                    for (GridTemplate template : list) {
                        template.processItems();
                        templateCache.put(template.getDimensionsKey(), template);
                    }
                }
            } catch (Exception exception) {
                Log.e(TAG, "loadTemplates: " + exception);
            }
        }
    }

    public Map<String, DynamicGridLayout.ItemLayoutData> getLayoutForSize(int cols, int rows) {
        Map<String, DynamicGridLayout.ItemLayoutData> layout = new HashMap<>();
        String key = cols + "x" + rows;

        GridTemplate template = templateCache.get(key);
        if (template != null) {
            layout.putAll(template.getItemMap());
        }

        String savedJson = prefs.getString("state_" + key, null);
        if (savedJson != null) {
            Type type = new TypeToken<Map<String,
                    DynamicGridLayout.ItemLayoutData>>() {
            }.getType();

            Map<String, DynamicGridLayout.ItemLayoutData> savedMap =
                    Utils.getGsonInstance().fromJson(savedJson, type);
            layout.putAll(savedMap);
        }

        return layout;
    }

    public void saveUserLayout(int cols, int rows, Map<String, DynamicGridLayout.ItemLayoutData> map) {
        prefs.edit().putString("state_" + cols + "x" + rows,
                Utils.getGsonInstance().toJson(map)).apply();
    }

    public static class GridTemplate {
        private transient Map<String, DynamicGridLayout.ItemLayoutData> itemMap;

        @SerializedName("items")
        public List<DynamicGridLayout.ItemLayoutData> itemList;

        @SerializedName("cols")
        public int cols;

        @SerializedName("rows")
        public int rows;

        public GridTemplate() {
            this.itemMap = new HashMap<>();
            this.itemList = new ArrayList<>();
        }

        public void processItems() {
            itemMap = new HashMap<>();
            if (itemList != null) {
                for (DynamicGridLayout.ItemLayoutData item : itemList) {
                    itemMap.put(item.id, item);
                }
            }
        }

        public Map<String, DynamicGridLayout.ItemLayoutData> getItemMap() {
            if (itemMap == null || (itemMap.isEmpty() && !itemList.isEmpty())) {
                processItems();
            }

            return itemMap;
        }

        public String getDimensionsKey() {
            return cols + "x" + rows;
        }
    }
}