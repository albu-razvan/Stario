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

package com.stario.launcher.activities.launcher.pins;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.activities.launcher.pins.dialog.PinnedAppsGroupDialog;
import com.stario.launcher.apps.Category;
import com.stario.launcher.apps.CategoryManager;
import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.sheet.drawer.RecyclerApplicationAdapter;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.recyclers.async.InflationType;
import com.stario.launcher.ui.utils.UiUtils;

import java.util.UUID;
import java.util.function.Supplier;

class PinnedAppsAdapter extends RecyclerApplicationAdapter {
    private static final int GROUP_VIEW_TYPE = 2;

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;
    private final CategoryManager.CategoryListener categoryManagerChangeListener;
    private final PinnedAppsGroupDialog.TransitionListener transitionListener;
    private final Category.CategoryItemListener categoryChangeListener;
    private final OnPopUpShowListener popUpShowListener;
    private final SharedPreferences settings;
    private final ThemedActivity activity;

    private Category category;
    private int itemCount;

    public PinnedAppsAdapter(ThemedActivity activity,
                             SharedPreferences settings,
                             OnPopUpShowListener popUpShowListener,
                             PinnedAppsGroupDialog.TransitionListener transitionListener) {
        super(activity, false, InflationType.SYNCED);

        this.transitionListener = transitionListener;
        this.popUpShowListener = popUpShowListener;
        this.activity = activity;

        this.itemCount = 0;
        this.settings = settings;
        this.sharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (!PinnedCategory.PINNED_CATEGORY.equals(key)) {
                return;
            }

            load();
        };

        this.categoryManagerChangeListener = new CategoryManager.CategoryListener() {
            @Override
            public void onRemoved(Category category) {
                if (category.equals(PinnedAppsAdapter.this.category)) {
                    load();
                }
            }
        };

        this.categoryChangeListener = new Category.CategoryItemListener() {
            @Override
            @SuppressLint("NotifyDataSetChanged")
            public void onInserted(LauncherApplication application) {
                notifyDataSetChanged();
            }

            @Override
            @SuppressLint("NotifyDataSetChanged")
            public void onRemoved(LauncherApplication application) {
                notifyDataSetChanged();
            }

            @Override
            @SuppressLint("NotifyDataSetChanged")
            public void onUpdated(LauncherApplication application) {
                notifyDataSetChanged();
            }

            @Override
            @SuppressLint("NotifyDataSetChanged")
            public void onSwapped(int index1, int index2) {
                notifyDataSetChanged();
            }
        };
    }

    @SuppressLint("NotifyDataSetChanged")
    private void load() {
        if (category != null) {
            category.removeCategoryItemListener(categoryChangeListener);
        }

        try {
            category = CategoryManager.getInstance().get(
                    UUID.fromString(settings.getString(PinnedCategory.PINNED_CATEGORY, null))
            );

            if (category != null) {
                category.addCategoryItemListener(categoryChangeListener);
            } else {
                resetSharedPreferences();
            }
        } catch (IllegalArgumentException exception) {
            category = null;
            resetSharedPreferences();
        }

        UiUtils.runOnUIThread(this::notifyDataSetChanged);
    }

    private void resetSharedPreferences() {
        settings.edit()
                .remove(PinnedCategory.PINNED_CATEGORY)
                .remove(PinnedCategory.PINNED_CATEGORY_VISIBLE)
                .apply();
    }

    public class PinnedGroupViewHolder extends ApplicationViewHolder {
        private RecyclerView group;

        public PinnedGroupViewHolder(int viewType) {
            super(viewType);
        }

        @Override
        protected void onInflated() {
            super.onInflated();

            group = itemView.findViewById(R.id.group);
        }

        @Override
        public View.OnClickListener getOnClickListener() {
            return view -> {
                int index = getBindingAdapterPosition();
                if (index == RecyclerView.NO_POSITION) {
                    return;
                }

                PinnedAppsGroupDialog dialog = new PinnedAppsGroupDialog(activity, transitionListener);
                dialog.setCategory(category);

                dialog.show(index, group);
            };
        }

        @Override
        public View.OnLongClickListener getOnLongClickListener() {
            return null;
        }
    }

    public class PinnedApplicationViewHolder extends ApplicationViewHolder {
        public PinnedApplicationViewHolder(int viewType) {
            super(viewType);
        }

        @Override
        public View.OnLongClickListener getOnLongClickListener() {
            View.OnLongClickListener listener = super.getOnLongClickListener();

            return view -> {
                if (popUpShowListener != null) {
                    popUpShowListener.onShow();
                }

                if (listener != null) {
                    return listener.onLongClick(view);
                }

                return true;
            };
        }
    }

    @Override
    public void onBind(@NonNull ApplicationViewHolder viewHolder, int index) {
        super.onBind(viewHolder, index);

        if (viewHolder instanceof PinnedGroupViewHolder) {
            viewHolder.setIcon(null);

            PinnedGroupViewHolder pinedViewHolder = (PinnedGroupViewHolder) viewHolder;
            pinedViewHolder.group.setLayoutManager(new GridLayoutManager(activity, 2));
            pinedViewHolder.group.setAdapter(new PinnedAppsGroupAdapter(activity,
                    category, itemCount - 1));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == itemCount - 1 && category.getSize() - position - 1 > 0) {
            return GROUP_VIEW_TYPE;
        }

        return super.getItemViewType(position);
    }

    @Override
    protected int getLayout(int viewType) {
        if (viewType == GROUP_VIEW_TYPE) {
            return R.layout.pinned_application_group;
        }

        return super.getLayout(ONLY_ICON_LAYOUT);
    }

    @Override
    protected Supplier<ApplicationViewHolder> getHolderSupplier(int viewType) {
        if (viewType == GROUP_VIEW_TYPE) {
            return () -> new PinnedGroupViewHolder(viewType);
        }

        return () -> new PinnedApplicationViewHolder(viewType);
    }

    protected LauncherApplication getApplication(int index) {
        return category != null ?
                category.get(index) : LauncherApplication.FALLBACK_APP;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        settings.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        CategoryManager.getInstance().addOnCategoryUpdateListener(categoryManagerChangeListener);

        load();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);

        settings.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        CategoryManager.getInstance().removeOnCategoryUpdateListener(categoryManagerChangeListener);

        if (category != null) {
            category.removeCategoryItemListener(categoryChangeListener);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setMaxItemCount(int count) {
        this.itemCount = count;
        notifyDataSetChanged();
    }

    @Override
    public int getTotalItemCount() {
        return Math.min(category != null ? category.getSize() : 0, itemCount);
    }

    @Override
    protected boolean allowApplicationStateEditing() {
        return true;
    }

    public interface OnPopUpShowListener {
        void onShow();
    }
}
