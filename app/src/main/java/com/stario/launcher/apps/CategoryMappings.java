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

import androidx.annotation.NonNull;

import com.stario.launcher.Stario;
import com.stario.launcher.exceptions.NoExistingInstanceException;
import com.stario.launcher.preferences.Entry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryMappings {
    private static CategoryMappings instance;
    private final SharedPreferencesProvider provider;

    private CategoryMappings(SharedPreferencesProvider provider) {
        this.provider = provider;
    }

    public static void from(Stario stario) {
        if (instance == null) {
            instance = new CategoryMappings((name) ->
                    stario.getSharedPreferences(
                            Entry.CATEGORY_MAP.toSubPreference(name), Context.MODE_PRIVATE)
            );
        }
    }

    /**
     * @return A comparator for sorting {@link Category} objects.
     * @throws NoExistingInstanceException If the singleton instance has not been initialized.
     */
    public static Comparator<Category> getCategoryComparator() throws NoExistingInstanceException {
        if (instance == null) {
            throw new NoExistingInstanceException(CategoryMappings.class);
        }

        return new MapComparator(instance.provider);
    }

    /**
     * @param category The category for which to retrieve the application comparator.
     * @return A comparator for sorting {@link LauncherApplication} objects.
     * @throws NoExistingInstanceException If the singleton instance has not been initialized.
     */
    public static Comparator<LauncherApplication> getCategoryApplicationComparator(Category category) {
        if (instance == null) {
            throw new NoExistingInstanceException(CategoryMappings.class);
        }

        return new ApplicationComparator(instance.provider, category);
    }

    private interface SharedPreferencesProvider {
        SharedPreferences provide(@NonNull String name);
    }

    public static abstract class Comparator<T> implements java.util.Comparator<T> {
        abstract void updatePermutation();
    }

    private static class ApplicationComparator extends Comparator<LauncherApplication> {
        private final Map<String, Integer> indexCache;
        private final SharedPreferences categoryMap;
        private final Category category;

        private ApplicationComparator(
                SharedPreferencesProvider provider,
                Category category
        ) {
            this.category = category;
            this.indexCache = new HashMap<>();
            this.categoryMap = provider.provide(category.identifier.toString());

            for (Map.Entry<String, ?> entry : categoryMap.getAll().entrySet()) {
                Object value = entry.getValue();

                if (value instanceof Integer) {
                    indexCache.put(entry.getKey(), (Integer) value);
                }
            }
        }

        @Override
        public int compare(LauncherApplication a, LauncherApplication b) {
            Integer aIndex = indexCache.get(a.getInfo().packageName);
            Integer bIndex = indexCache.get(b.getInfo().packageName);

            if (aIndex != null && bIndex != null) {
                return Integer.compare(aIndex, bIndex);
            }

            return a.getLabel().compareTo(b.getLabel());
        }

        @Override
        void updatePermutation() {
            List<LauncherApplication> applications = category.getAll();

            SharedPreferences.Editor editor = categoryMap.edit();
            editor.clear();
            indexCache.clear();

            for (int index = 0; index < applications.size(); index++) {
                LauncherApplication application =
                        applications.get(index);

                String packageName =
                        application.getInfo().packageName;

                editor.putInt(packageName, index);
                indexCache.put(packageName, index);
            }

            editor.apply();
        }
    }

    private static class MapComparator
            extends Comparator<Category> {

        private final Map<String, Integer> indexCache;
        private final SharedPreferences categoryMap;

        private MapComparator(SharedPreferencesProvider provider) {
            this.indexCache = new HashMap<>();
            this.categoryMap = provider.provide("CATEGORIES");

            for (Map.Entry<String, ?> entry : categoryMap.getAll().entrySet()) {
                Object value = entry.getValue();

                if (value instanceof Integer) {
                    indexCache.put(entry.getKey(), (Integer) value);
                }
            }
        }

        @Override
        public int compare(Category a, Category b) {
            Integer aIndex = indexCache.get(a.identifier.toString());
            Integer bIndex = indexCache.get(b.identifier.toString());

            if (aIndex != null && bIndex != null) {
                return Integer.compare(aIndex, bIndex);
            }

            return a.identifier.compareTo(b.identifier);
        }

        @Override
        void updatePermutation() {
            List<Category> categories = CategoryManager.getInstance().getAll();

            SharedPreferences.Editor editor = categoryMap.edit();
            editor.clear();
            indexCache.clear();

            for (int index = 0; index < categories.size(); index++) {
                Category category = categories.get(index);
                String key = category.identifier.toString();

                editor.putInt(key, index);
                indexCache.put(key, index);
            }

            editor.apply();
        }
    }
}
