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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.apps.categories.Category;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.icons.AdaptiveIconView;
import com.stario.launcher.ui.recyclers.async.AsyncRecyclerAdapter;

import java.util.function.Supplier;

public class FolderListItemAdapter extends AsyncRecyclerAdapter<FolderListItemAdapter.ViewHolder> {
    public static final int SOFT_LIMIT = 3;
    public static final int HARD_LIMIT = 5;
    private final ThemedActivity activity;

    private Category.CategoryItemListener listener;
    private RecyclerView recyclerView;
    private Category category;

    public FolderListItemAdapter(ThemedActivity activity) {
        super(activity);

        this.activity = activity;

        setHasStableIds(true);
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
                int preparedRemovalIndex = -1;
                boolean skipInsertion = false;

                @Override
                public void onPrepareInsertion(LauncherApplication application) {
                    if (category.getSize() >= HARD_LIMIT) {
                        skipInsertion = true;
                    }
                }

                @Override
                public void onInserted(LauncherApplication application) {
                    if (recyclerView != null) {
                        recyclerView.post(() -> {
                            if (!skipInsertion) {
                                int index = category.indexOf(application);

                                if (index >= 0 && index < HARD_LIMIT) {
                                    notifyItemInserted(index);
                                }
                            }

                            skipInsertion = false;
                        });
                    }
                }

                @Override
                public void onPrepareRemoval(LauncherApplication application) {
                    preparedRemovalIndex = category.indexOf(application);
                }

                @Override
                public void onRemoved(LauncherApplication application) {
                    if (recyclerView != null) {
                        recyclerView.post(() -> {
                            if (preparedRemovalIndex >= 0 && preparedRemovalIndex < HARD_LIMIT) {
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
                    if (recyclerView != null) {
                        recyclerView.post(() -> notifyDataSetChanged());
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

                if (position >= SOFT_LIMIT && category.getSize() >= HARD_LIMIT) {
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
    public long getItemId(int position) {
        LauncherApplication application = category.get(position);

        if (application != null) {
            return application.hashCode();
        } else {
            return RecyclerView.NO_ID;
        }
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
    protected Supplier<ViewHolder> getHolderSupplier() {
        return ViewHolder::new;
    }

    @Override
    protected int getSize() {
        return Math.min(category.getSize(), HARD_LIMIT);
    }
}
