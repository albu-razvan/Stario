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

package com.stario.launcher.activities.settings.dialogs.pin;

import com.stario.launcher.apps.Category;
import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.sheet.drawer.RecyclerApplicationAdapter;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.icons.AdaptiveIconView;
import com.stario.launcher.ui.recyclers.async.InflationType;

import java.util.function.Supplier;

public class PinnedCategoryApplicationRecyclerAdapter extends RecyclerApplicationAdapter {
    private final Category category;

    public PinnedCategoryApplicationRecyclerAdapter(ThemedActivity activity, Category category) {
        super(activity, false, InflationType.SYNCED);

        this.category = category;
    }

    public class PinnedCategoryApplicationViewHolder extends ApplicationViewHolder {
        public PinnedCategoryApplicationViewHolder(int viewType) {
            super(viewType);
        }

        @Override
        protected void onInflated() {
            itemView.getLayoutParams().width = AdaptiveIconView.getMaxIconSize()
                    + Measurements.getDefaultPadding();

            super.onInflated();
        }
    }

    @Override
    protected Supplier<ApplicationViewHolder> getHolderSupplier(int viewType) {
        return () -> new PinnedCategoryApplicationViewHolder(viewType);
    }

    @Override
    protected LauncherApplication getApplication(int index) {
        return category != null ?
                category.get(index) : LauncherApplication.FALLBACK_APP;
    }

    @Override
    public int getTotalItemCount() {
        return category != null ? category.getSize() : 0;
    }

    @Override
    protected boolean allowApplicationStateEditing() {
        return false;
    }
}