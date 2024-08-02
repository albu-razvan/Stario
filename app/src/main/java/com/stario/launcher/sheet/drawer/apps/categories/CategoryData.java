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

package com.stario.launcher.sheet.drawer.apps.categories;

import android.content.SharedPreferences;
import android.content.res.Resources;

import com.stario.launcher.R;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.sheet.drawer.apps.LauncherApplication;
import com.stario.launcher.sheet.drawer.apps.LauncherApplicationManager;
import com.stario.launcher.themes.ThemedActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.function.BiConsumer;

public class CategoryData {
    private static final int NO_CATEGORY = -1;
    private static CategoryData instance = null;
    private final HashMap<Integer, Object> categoryResources;
    private final ArrayList<Category> categories;
    private CategoryListener categoryListener;

    private CategoryData(ThemedActivity activity) {
        this.categories = new ArrayList<>();
        this.categoryResources = new HashMap<>() {{
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

        SharedPreferences customCategories = activity.getSharedPreferences(Entry.CATEGORY_NAMES);

        customCategories.getAll()
                .forEach((BiConsumer<String, Object>) (key, value) -> {
                    try {
                        int categoryID = Integer.parseInt(key);

                        addCategoryResource(categoryID, value);
                    } catch (NumberFormatException exception) {
                        customCategories.edit()
                                .remove(key)
                                .apply();
                    }
                });

        CategoryComparator.from(activity);

        LauncherApplicationManager.getInstance()
                .addApplicationListener(new LauncherApplicationManager.ApplicationListener() {
                    @Override
                    public void onUpdated(LauncherApplication application) {
                        for (Category category : categories) {
                            for (Category.CategoryItemListener listener : category.listeners) {
                                if (category.applications.contains(application)) {
                                    listener.onUpdated(application);
                                }
                            }
                        }
                    }
                });
    }

    public static CategoryData from(ThemedActivity activity) {
        if (instance == null) {
            instance = new CategoryData(activity);
        }

        return instance;
    }

    public static CategoryData getInstance() {
        if (instance == null) {
            throw new RuntimeException("Applications not initialized.");
        }

        return instance;
    }

    public Category get(int index) {
        return categories.get(index);
    }

    public Category getByID(int categoryID) {
        for (Category category : categories) {
            if (category.id == categoryID) {
                return category;
            }
        }

        return null;
    }

    public void addCategoryResource(int categoryID, Object resource) {
        if (resource instanceof String || resource instanceof Integer) {
            categoryResources.putIfAbsent(categoryID, resource);
        }
    }

    public String getCategoryName(int categoryID, Resources resources) {
        Object value = categoryResources.get(categoryID);

        if (value instanceof Integer) {
            return resources.getString((Integer) value);
        } else if (value instanceof String) {
            return (String) value;
        } else {
            return null;
        }
    }

    public int size() {
        return categories.size();
    }

    private int containsCategory(int categoryID) {
        for (int index = 0; index < size(); index++) {
            if (get(index).id == categoryID) {
                return index;
            }
        }

        return NO_CATEGORY;
    }

    private int addCategory(int categoryID) {
        Category category = new Category(categoryID);
        CategoryComparator comparator = CategoryComparator.getInstance();

        int left = 0;
        int right = size() - 1;

        while (left <= right) {
            int middle = (left + right) / 2;

            Category categoryAtMiddle = get(middle);
            int compareValue = comparator.compare(categoryAtMiddle, category);

            if (compareValue < 0) {
                left = middle + 1;
            } else if (compareValue > 0) {
                right = middle - 1;
            } else {
                return middle;
            }
        }

        categories.add(left, category);

        if (categoryListener != null) {
            categoryListener.onCreated(category);
        }

        return left;
    }

    public void addApplication(LauncherApplication application) {
        int index = containsCategory(application.getCategory());

        if (index == NO_CATEGORY) {
            index = addCategory(application.getCategory());
        }

        get(index).addApplication(application);
    }

    public void removeApplication(LauncherApplication application) {
        int index = containsCategory(application.getCategory());

        if (index != NO_CATEGORY) {
            Category category = get(index);

            category.removeApplication(application.getInfo().packageName);

            if (category.getSize() == 0) {
                if (categoryListener != null) {
                    categoryListener.onPrepareRemoval(category);
                }

                categories.remove(index);

                if (categoryListener != null) {
                    categoryListener.onRemoved(category);
                }
            }
        }
    }

    public void swap(int index1, int index2) {
        Collections.swap(categories, index1, index2);

        CategoryComparator.getInstance()
                .updatePermutation(categories);
    }

    public int indexOf(Category category) {
        return categories.indexOf(category);
    }

    public void setOnCategoryUpdateListener(CategoryListener updateListener) {
        if (updateListener != null) {
            categoryListener = updateListener;
        }
    }

    /**
     * These callbacks do not guarantee that will be run on the UI thread
     */
    public interface CategoryListener {
        default void onCreated(Category category) {
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