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

package com.stario.launcher.sheet.drawer.category.folder;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.apps.categories.Category;
import com.stario.launcher.apps.categories.CategoryData;
import com.stario.launcher.sheet.drawer.RecyclerApplicationAdapter;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.utils.UiUtils;

class FolderAdapter extends RecyclerApplicationAdapter {
    private final Category.CategoryItemListener listener;
    private final Category category;

    public FolderAdapter(ThemedActivity activity, int categoryID) {
        super(activity);

        this.category = CategoryData.getInstance()
                .getByID(categoryID);

        listener = new Category.CategoryItemListener() {
            int preparedRemovalIndex = -1;

            @Override
            public void onInserted(LauncherApplication application) {
                int index = category.indexOf(application);

                if (index >= 0) {
                    notifyItemInserted(index);
                }
            }

            @Override
            public void onPrepareRemoval(LauncherApplication application) {
                preparedRemovalIndex = category.indexOf(application);
            }

            @Override
            public void onRemoved(LauncherApplication application) {
                if (preparedRemovalIndex >= 0) {
                    UiUtils.runOnUIThread(() -> notifyItemRemoved(preparedRemovalIndex));

                    preparedRemovalIndex = -1;
                } else {
                    notifyDataSetChanged();
                }
            }

            @Override
            public void onUpdated(LauncherApplication application) {
                int index = category.indexOf(application);

                if (index >= 0) {
                    notifyItemChanged(index);
                }
            }
        };
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        if (category != null && listener != null) {
            category.addCategoryItemListener(listener);
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);

        if (category != null && listener != null) {
            category.removeCategoryItemListener(listener);
        }
    }

    @Override
    protected LauncherApplication getApplication(int index) {
        return category.get(index);
    }

    @Override
    protected boolean allowApplicationStateEditing() {
        return true;
    }

    @Override
    protected int getSize() {
        return category.getSize();
    }
}