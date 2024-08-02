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

package com.stario.launcher.sheet.drawer.category.list;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.sheet.drawer.BumpRecyclerViewAdapter;
import com.stario.launcher.sheet.drawer.apps.LauncherApplication;
import com.stario.launcher.sheet.drawer.apps.categories.Category;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.icons.AdaptiveIconView;
import com.stario.launcher.ui.recyclers.async.AsyncRecyclerAdapter;
import com.stario.launcher.utils.UiUtils;

import java.util.function.Supplier;

public class FolderListItemAdapter extends AsyncRecyclerAdapter<FolderListItemAdapter.ViewHolder>
        implements BumpRecyclerViewAdapter {
    public static final int SOFT_LIMIT = 3;
    public static final int HARD_LIMIT = 5;
    private final ThemedActivity activity;
    private Category category;
    private Category.CategoryItemListener listener;
    private boolean limit;
    private int size;

    public FolderListItemAdapter(ThemedActivity activity) {
        super(activity);

        this.activity = activity;
        this.size = 0;
        this.limit = true;

        setHasStableIds(true);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setCategory(Category category, boolean animate) {
        if (this.category != category) {
            if (listener != null) {
                this.category.removeCategoryItemListener(listener);
            }

            if (animate) {
                size = 0;
                limit = true;

                if (this.category != null && this.category.getSize() > 0) {
                    notifyItemRangeRemoved(0, this.category.getSize() - 1);
                }

                this.category = category;

                // Fake a loading delay not to freeze the UI when inflating all views
                int loop = Math.min(category.getSize(), HARD_LIMIT);
                while (loop > 0) {
                    UiUtils.runOnUIThreadDelayed(() -> bump(1),
                            loop * BumpRecyclerViewAdapter.DELAY * 4);

                    loop--;
                }
            } else {
                limit = false;

                this.category = category;

                notifyDataSetChanged();
            }

            listener = new Category.CategoryItemListener() {
                int preparedRemovalIndex = -1;

                @Override
                public void onInserted(LauncherApplication application) {
                    int index = category.indexOf(application);

                    if (index >= 0) {
                        UiUtils.runOnUIThread(() -> notifyItemInserted(index));
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
                        UiUtils.runOnUIThread(() -> notifyDataSetChanged());
                    }
                }

                @Override
                public void onUpdated(LauncherApplication application) {
                    int index = category.indexOf(application);

                    if (index >= 0) {
                        UiUtils.runOnUIThread(() -> notifyItemChanged(index));
                    }
                }
            };

            category.addCategoryItemListener(listener);
        }
    }

    protected class ViewHolder extends AsyncViewHolder {
        private AdaptiveIconView icon;

        @Override
        protected void onInflated() {
            itemView.setHapticFeedbackEnabled(false);

            icon = itemView.findViewById(R.id.icon);
        }
    }

    @Override
    protected int getLayout() {
        return R.layout.folder_item;
    }

    @Override
    protected void onBind(@NonNull ViewHolder holder, int position) {
        if (position < category.getSize()) {
            LauncherApplication application = category.get(position);

            if (application != LauncherApplication.FALLBACK_APP) {
                Drawable appIcon = application.getIcon();

                if (position >= SOFT_LIMIT && category.getSize() > HARD_LIMIT) {
                    holder.itemView.setOnClickListener((view) -> {
                        View folder = (View) holder.itemView.getParent();

                        while (!(folder.getParent() instanceof RecyclerView)) {
                            folder = (View) folder.getParent();
                        }

                        folder.callOnClick();
                    });
                } else {
                    holder.itemView.setOnClickListener(view ->
                            application.launch(activity, holder.icon));
                }

                if (appIcon != null) {
                    holder.icon.setIcon(appIcon);
                }
            }
        } else {
            holder.icon.setIcon(null);

            holder.itemView.setOnClickListener(null);
        }

        holder.itemView.requestLayout();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        if (category != null && listener != null) {
            category.removeCategoryItemListener(listener);
        }
    }

    @Override
    protected Supplier<ViewHolder> getHolderSupplier() {
        return ViewHolder::new;
    }

    @Override
    public long getItemId(int position) {
        return category.get(position)
                .getInfo().packageName.hashCode();
    }

    @Override
    public int getItemCount() {
        return limit ? size : Math.min(category.getSize(), HARD_LIMIT);
    }

    public boolean isCapped() {
        return category.getSize() > HARD_LIMIT;
    }

    @Override
    public void bump(@IntRange(from = 1, to = 1) int bumpSize) {
        if (++size >= Math.min(category.getSize(), HARD_LIMIT)) {
            limit = false;
        }

        notifyItemInserted(size - 1);
    }
}
