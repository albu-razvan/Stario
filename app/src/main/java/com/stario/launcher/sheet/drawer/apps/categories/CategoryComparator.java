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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.stario.launcher.exceptions.NoExistingInstanceException;
import com.stario.launcher.preferences.Entry;

import java.util.ArrayList;
import java.util.Comparator;

public class CategoryComparator implements Comparator<Category> {
    private static CategoryComparator instance;
    private final SharedPreferences categoryMap;

    private CategoryComparator(Activity activity) {
        this.categoryMap = activity.getSharedPreferences(Entry.CATEGORY_MAP.toString(), Context.MODE_PRIVATE);
    }

    public static CategoryComparator getInstance() throws NoExistingInstanceException {
        if (instance == null) {
            throw new NoExistingInstanceException(CategoryComparator.class);
        }

        return instance;
    }

    public static void from(Activity activity) {
        if (instance == null) {
            instance = new CategoryComparator(activity);
        }
    }

    @Override
    public int compare(Category category, Category otherCategory) {
        return Integer.compare(categoryMap.getInt(String.valueOf(category.id), category.id),
                categoryMap.getInt(String.valueOf(otherCategory.id), otherCategory.id));
    }

    void updatePermutation(ArrayList<Category> categories) {
        if (categoryMap != null) {
            SharedPreferences.Editor editor = categoryMap.edit();
            editor.clear();

            for (int index = 0; index < categories.size(); index++) {
                Category category = categories.get(index);

                editor.putInt(String.valueOf(category.id), index);
            }

            editor.apply();
        }
    }
}
