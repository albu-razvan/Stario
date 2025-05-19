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

package com.stario.launcher.sheet.drawer.category.folder;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.apps.Category;
import com.stario.launcher.apps.CategoryManager;
import com.stario.launcher.sheet.drawer.RecyclerApplicationAdapter;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.recyclers.async.InflationType;

import java.util.UUID;

class FolderAdapter extends RecyclerApplicationAdapter {
    private final Category.CategoryItemListener listener;
    private final Category category;

    private RecyclerView recyclerView;

    public FolderAdapter(ThemedActivity activity, UUID categoryID, ItemTouchHelper itemTouchHelper) {
        super(activity, itemTouchHelper, InflationType.SYNCED);

        this.category = CategoryManager.getInstance().get(categoryID);

        listener = new Category.CategoryItemListener() {
            int preparedRemovalIndex = -1;

            @Override
            public void onInserted(LauncherApplication application) {
                if (recyclerView != null && category != null) {
                    recyclerView.post(() -> {
                        int index = category.indexOf(application);

                        if (index >= 0) {
                            notifyItemInserted(index);
                        }
                    });
                }
            }

            @Override
            public void onPrepareRemoval(LauncherApplication application) {
                if (category != null) {
                    preparedRemovalIndex = category.indexOf(application);
                }
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onRemoved(LauncherApplication application) {
                if (recyclerView != null) {
                    recyclerView.post(() -> {
                        if (preparedRemovalIndex >= 0) {
                            notifyItemRemoved(preparedRemovalIndex);

                            preparedRemovalIndex = -1;
                        } else {
                            notifyDataSetChanged();
                        }
                    });
                }
            }

            @Override
            public void onUpdated(LauncherApplication application) {
                if (recyclerView != null && category != null) {
                    recyclerView.post(() -> {
                        int index = category.indexOf(application);

                        if (index >= 0) {
                            notifyItemChanged(index);
                        }
                    });
                }
            }
        };
    }

    public boolean move(RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder targetHolder) {
        int position = viewHolder.getAbsoluteAdapterPosition();
        int target = targetHolder.getAbsoluteAdapterPosition();

        if (position == target) {
            return false;
        }

        while (position - target != 0) {
            int newTarget = position - ((position - target) > 0 ? 1 : -1);

            category.swap(position, newTarget);
            notifyItemMoved(position, newTarget);

            position = newTarget;
        }

        return true;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        this.recyclerView = recyclerView;

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

        this.recyclerView = null;
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
    public int getTotalItemCount() {
        return category.getSize();
    }
}