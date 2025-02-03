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

package com.stario.launcher.apps.categories;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

import androidx.annotation.NonNull;

import com.stario.launcher.R;
import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.apps.LauncherApplicationManager;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.utils.ThreadSafeArrayList;
import com.stario.launcher.utils.UiUtils;
import com.stario.launcher.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CategoryManager {
    private static final HashMap<Integer, Integer> DEFAULT_CATEGORIES = new HashMap<>() {{
        putIfAbsent(-1, R.string.unsorted);
        putIfAbsent(0, R.string.games);
        putIfAbsent(1, R.string.audio);
        putIfAbsent(2, R.string.video);
        putIfAbsent(3, R.string.images);
        putIfAbsent(4, R.string.social);
        putIfAbsent(5, R.string.news);
        putIfAbsent(6, R.string.maps);
        putIfAbsent(7, R.string.productivity);
        putIfAbsent(8, R.string.accessibility);
        putIfAbsent(9, R.string.finance);
        putIfAbsent(10, R.string.health);
        putIfAbsent(11, R.string.personalization);
        putIfAbsent(12, R.string.sports);
    }};
    private static final int NO_CATEGORY = -1;
    private static CategoryManager instance = null;

    private final HashMap<String, String> resolvedCategoryResources;
    private final CategoryMappings.Comparator<Category> comparator;
    private final List<CategoryListener> categoryListeners;
    private final SharedPreferences customCategoryNames;
    private final SharedPreferences remappedCategories;
    private final SharedPreferences hiddenApplications;
    private final ArrayList<Category> categories;


    private CategoryManager(ThemedActivity activity) {
        this.categories = new ThreadSafeArrayList<>();
        this.hiddenApplications = activity.getSharedPreferences(Entry.HIDDEN_APPS);
        this.remappedCategories = activity.getSharedPreferences(Entry.CATEGORIES);
        this.customCategoryNames = activity.getSharedPreferences(Entry.CATEGORY_NAMES);
        this.resolvedCategoryResources = new HashMap<>();
        this.categoryListeners = new ArrayList<>();

        CategoryMappings.from(activity);
        comparator = CategoryMappings.getCategoryComparator();

        LauncherApplicationManager.getInstance()
                .addApplicationListener(new LauncherApplicationManager.ApplicationListener() {
                    @Override
                    public void onUpdated(LauncherApplication application) {
                        for (Category category : categories) {
                            if (category.applications.contains(application)) {
                                for (Category.CategoryItemListener listener : category.listeners) {
                                    listener.onUpdated(application);
                                }
                            }
                        }
                    }
                });
    }

    public static CategoryManager from(ThemedActivity activity) {
        if (instance == null) {
            instance = new CategoryManager(activity);
        }

        DEFAULT_CATEGORIES.forEach((id, resource) ->
                instance.resolvedCategoryResources.put(Utils.intToUUID(id).toString(),
                        activity.getResources().getString(resource))
        );

        return instance;
    }

    public static CategoryManager getInstance() {
        if (instance == null) {
            throw new RuntimeException("Applications not initialized.");
        }

        return instance;
    }

    public Category get(int index) {
        return categories.get(index);
    }

    public Category get(UUID identifier) {
        for (Category category : categories) {
            if (category.identifier.equals(identifier)) {
                return category;
            }
        }

        return null;
    }

    public UUID getIdentifier(String name) {
        return getIdentifier(name, false);
    }

    public UUID getIdentifier(String name, boolean includeDefaults) {
        if (name == null) {
            return null;
        }

        ArrayList<UUID> testedDefaults = new ArrayList<>();

        for (Category category : categories) {
            String categoryName = getCategoryName(category.identifier);

            if (categoryName != null && categoryName.equalsIgnoreCase(name)) {
                return category.identifier;
            }

            if(resolvedCategoryResources.containsKey(category.identifier.toString())) {
                testedDefaults.add(category.identifier);
            }
        }

        if (includeDefaults) {
            for (Map.Entry<String, String> tester : resolvedCategoryResources.entrySet()) {
                if (tester.getValue().equalsIgnoreCase(name) &&
                        !testedDefaults.contains(UUID.fromString(tester.getKey()))) {

                    return UUID.fromString(tester.getKey());
                }
            }
        }

        return null;
    }

    public String getCategoryName(UUID identifier) {
        String name = customCategoryNames.getString(identifier.toString(), null);

        if (name == null) {
            name = resolvedCategoryResources.get(identifier.toString());
        }

        return name;
    }

    private int indexOf(UUID identifier) {
        for (int index = 0; index < size(); index++) {
            if (get(index).identifier.equals(identifier)) {
                return index;
            }
        }

        return NO_CATEGORY;
    }

    public int indexOf(Category category) {
        return categories.indexOf(category);
    }

    public List<Category> getAll() {
        return Collections.unmodifiableList(categories);
    }

    public String getSuggestion(String input) {
        if (input == null) {
            return null;
        }

        for (String tester : resolvedCategoryResources.values()) {
            if (tester.toLowerCase().startsWith(input.toLowerCase())) {
                return tester;
            }
        }

        for (Category category : categories) {
            String name = getCategoryName(category.identifier);

            if (name != null && name.toLowerCase().startsWith(input.toLowerCase())) {
                return name;
            }
        }

        return null;
    }

    public @NonNull UUID getCategoryIdentifier(ApplicationInfo applicationInfo) {
        if (remappedCategories.contains(applicationInfo.packageName)) {
            String identifier = remappedCategories.getString(applicationInfo.packageName, null);

            try {
                return UUID.fromString(identifier);
            } catch (IllegalArgumentException exception) {
                remappedCategories.edit()
                        .remove(applicationInfo.packageName)
                        .apply();
            }
        }

        return Utils.intToUUID(applicationInfo.category);
    }

    public int size() {
        return categories.size();
    }

    private synchronized int addCategory(UUID identifier) {
        Category categoryToAdd = new Category(identifier);

        int left = 0;
        int right = size() - 1;

        while (left <= right) {
            int middle = (left + right) / 2;

            Category categoryAtMiddle = get(middle);
            int compareValue = comparator.compare(categoryAtMiddle, categoryToAdd);

            if (compareValue < 0) {
                left = middle + 1;
            } else if (compareValue > 0) {
                right = middle - 1;
            } else {
                return middle;
            }
        }

        categories.add(left, categoryToAdd);

        for (CategoryListener listener : categoryListeners) {
            listener.onCreated(categoryToAdd);
        }

        return left;
    }

    public UUID addCustomCategory(String name) {
        UUID identifier = getIdentifier(name, true);
        if (identifier == null) {
            identifier = UUID.randomUUID();

            customCategoryNames.edit()
                    .putString(identifier.toString(), name).apply();
        } else {
            if (indexOf(identifier) == NO_CATEGORY) {
                addCategory(identifier);
            }
        }

        return identifier;
    }

    public void updateCategory(LauncherApplication application, UUID uuid) {
        if (uuid == null || application.getCategory().equals(uuid)) {
            return;
        }

        remappedCategories.edit()
                .putString(application.getInfo().packageName, uuid.toString()).apply();
        LauncherApplicationManager.getInstance().updateApplication(application);
    }

    public void updateCategory(UUID categoryIdentifier, String text) {
        if (getIdentifier(text, true) != null) {
            return;
        }

        customCategoryNames.edit()
                .putString(categoryIdentifier.toString(), text)
                .apply();

        for (CategoryListener listener : categoryListeners) {
            listener.onChanged(get(categoryIdentifier));
        }
    }

    public synchronized void addApplication(LauncherApplication application) {
        if (!hiddenApplications.contains(application.getInfo().packageName)) {
            UiUtils.runOnUIThread(() -> {
                int index = indexOf(application.getCategory());

                if (index == NO_CATEGORY) {
                    index = addCategory(application.getCategory());
                }

                Category category = get(index);

                category.addApplication(application);
            });
        }
    }

    public synchronized void removeApplication(LauncherApplication application) {
        int index = indexOf(application.getCategory());

        if (index != NO_CATEGORY) {
            UiUtils.runOnUIThread(() -> {
                Category category = get(index);

                category.removeApplication(application.getInfo().packageName);

                if (category.getSize() == 0) {
                    for (CategoryListener listener : categoryListeners) {
                        listener.onPrepareRemoval(category);
                    }

                    category.clearListeners();
                    categories.remove(index);

                    for (CategoryListener listener : categoryListeners) {
                        listener.onRemoved(category);
                    }
                }
            });
        }
    }

    public void swap(int index1, int index2) {
        Collections.swap(categories, index1, index2);

        comparator.updatePermutation();
    }

    public void addOnCategoryUpdateListener(CategoryListener updateListener) {
        if (updateListener != null) {
            categoryListeners.add(updateListener);
        }
    }

    public void removeOnCategoryUpdateListener(CategoryListener updateListener) {
        if (updateListener != null) {
            categoryListeners.remove(updateListener);
        }
    }

    /**
     * These callbacks do not guarantee that will be run on the UI thread
     */
    public interface CategoryListener {
        default void onCreated(Category category) {
        }

        default void onChanged(Category category) {
        }

        /**
         * This will always be called before {@link #onRemoved(Category)}
         */
        default void onPrepareRemoval(Category category) {
        }

        /**
         * This will always be called after {@link #onPrepareRemoval(Category)}
         */
        default void onRemoved(Category category) {
        }
    }
}