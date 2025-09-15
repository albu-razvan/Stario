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

package com.stario.launcher.sheet.drawer.category.list;

import android.annotation.SuppressLint;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.apps.Category;
import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.sheet.drawer.RecyclerApplicationAdapter;
import com.stario.launcher.themes.ThemedActivity;

import java.util.function.Supplier;

public class FolderListItemAdapter extends RecyclerApplicationAdapter {
    public static final int SOFT_LIMIT = 3;
    public static final int HARD_LIMIT = 5;

    private Category.CategoryItemListener listener;
    private RecyclerView recyclerView;
    private Category category;

    public FolderListItemAdapter(ThemedActivity activity) {
        super(activity);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setCategory(Category category) {
        if (this.category != category) {
            if (listener != null) {
                this.category.removeCategoryItemListener(listener);
            }

            this.category = category;

            notifyDataSetChanged();

            listener = new Category.CategoryItemListener() {
                @Override
                public void onInserted(LauncherApplication application) {
                    if (recyclerView != null) {
                        recyclerView.post(() -> notifyDataSetChanged());
                    }
                }

                @Override
                public void onRemoved(LauncherApplication application) {
                    if (recyclerView != null) {
                        recyclerView.post(() -> notifyDataSetChanged());
                    }
                }

                @Override
                public void onUpdated(LauncherApplication application) {
                    if (recyclerView != null) {
                        recyclerView.post(() -> notifyDataSetChanged());
                    }
                }

                @Override
                public void onSwapped(int index1, int index2) {
                    if (recyclerView != null) {
                        recyclerView.post(() -> notifyDataSetChanged());
                    }
                }
            };

            category.addCategoryItemListener(listener);
        }
    }

    protected class FolderItemViewHolder extends ApplicationViewHolder {
        @Override
        public View.OnClickListener getOnClickListener() {
            return view -> {
                int position = getAbsoluteAdapterPosition();
                if (position == RecyclerView.NO_POSITION) {
                    return;
                }

                if (position >= SOFT_LIMIT && category.getSize() >= HARD_LIMIT) {
                    View folder = (View) itemView.getParent();

                    while (!(folder.getParent() instanceof RecyclerView)) {
                        folder = (View) folder.getParent();
                    }

                    folder.callOnClick();
                } else {
                    View.OnClickListener superListener = super.getOnClickListener();

                    if (superListener != null) {
                        superListener.onClick(view);
                    }
                }
            };
        }

        @Override
        public View.OnLongClickListener getOnLongClickListener() {
            return null;
        }
    }

    @Override
    protected Supplier<ApplicationViewHolder> getHolderSupplier(int viewType) {
        return FolderItemViewHolder::new;
    }

    @Override
    protected int getLayout(int viewType) {
        return R.layout.folder_item;
    }

    @Override
    protected LauncherApplication getApplication(int index) {
        return category != null ? category.get(index) : LauncherApplication.FALLBACK_APP;
    }

    @Override
    protected boolean allowApplicationStateEditing() {
        return false;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        if (category != null && listener != null) {
            category.removeCategoryItemListener(listener);
        }

        this.recyclerView = null;
    }

    @Override
    public int getTotalItemCount() {
        return category != null ? Math.min(category.getSize(), HARD_LIMIT) : 0;
    }
}
