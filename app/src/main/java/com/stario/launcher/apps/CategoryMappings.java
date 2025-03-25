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

import com.stario.launcher.exceptions.NoExistingInstanceException;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.themes.ThemedActivity;

import java.util.List;

public class CategoryMappings {
    private static CategoryMappings instance;
    private final SharedPreferencesProvider provider;

    private CategoryMappings(SharedPreferencesProvider provider) {
        this.provider = provider;
    }

    /**
     * This method should be called once, typically in a {@link ThemedActivity}, to
     * load the mappings.
     *
     * @param activity The themed activity for which mappings will be loaded.
     */
    public static void from(ThemedActivity activity) {
        if (instance == null) {
            instance = new CategoryMappings((name) ->
                    activity.getSharedPreferences(Entry.CATEGORY_MAP.toSubPreference(name), Context.MODE_PRIVATE));
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
        private final SharedPreferences categoryMap;
        private final Category category;

        private ApplicationComparator(SharedPreferencesProvider provider, Category category) {
            this.category = category;
            this.categoryMap = provider.provide(category.identifier.toString());
        }

        @Override
        public int compare(LauncherApplication application, LauncherApplication otherApplication) {
            int applicationIndex = categoryMap.getInt(
                    application.getInfo().packageName, -1
            );

            if (applicationIndex >= 0) {
                int otherApplicationIndex = categoryMap.getInt(
                        otherApplication.getInfo().packageName, -1
                );

                if (otherApplicationIndex >= 0) {
                    return Integer.compare(applicationIndex, otherApplicationIndex);
                }
            }

            return application.getLabel().compareTo(otherApplication.getLabel());
        }

        @Override
        void updatePermutation() {
            List<LauncherApplication> applications = category.getAll();

            SharedPreferences.Editor editor = categoryMap.edit();
            editor.clear();

            for (int index = 0; index < applications.size(); index++) {
                LauncherApplication application = applications.get(index);

                editor.putInt(application.getInfo().packageName,
                        category.indexOf(application));
            }

            editor.apply();
        }
    }

    private static class MapComparator extends Comparator<Category> {
        private final SharedPreferences categoryMap;

        private MapComparator(SharedPreferencesProvider provider) {
            this.categoryMap = provider.provide("CATEGORIES");
        }

        @Override
        public int compare(Category category, Category otherCategory) {
            int categoryIndex = categoryMap.getInt(
                    category.identifier.toString(), -1
            );

            if (categoryIndex >= 0) {
                int otherCategoryIndex = categoryMap.getInt(
                        otherCategory.identifier.toString(), -1
                );

                if (otherCategoryIndex >= 0) {
                    return Integer.compare(categoryIndex, otherCategoryIndex);
                }
            }

            return category.identifier.compareTo(otherCategory.identifier);
        }

        @Override
        void updatePermutation() {
            List<Category> categories = CategoryManager.getInstance().getAll();

            SharedPreferences.Editor editor = categoryMap.edit();
            editor.clear();

            for (int index = 0; index < categories.size(); index++) {
                Category category = categories.get(index);

                editor.putInt(category.identifier.toString(), index);
            }

            editor.apply();
        }
    }
}
