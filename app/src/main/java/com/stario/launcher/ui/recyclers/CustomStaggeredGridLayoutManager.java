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

package com.stario.launcher.ui.recyclers;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

public class CustomStaggeredGridLayoutManager extends StaggeredGridLayoutManager {
    private boolean canScroll;

    public CustomStaggeredGridLayoutManager(int spanCount) {
        super(spanCount, RecyclerView.VERTICAL);

        this.canScroll = true;
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
        return false;
    }

    @Override
    public void setOrientation(int orientation) {
        if (orientation != RecyclerView.VERTICAL) {
            throw new RuntimeException("This layout manager supports only vertical orientation.");
        }

        super.setOrientation(orientation);
    }

    @Override
    public boolean canScrollHorizontally() {
        return false;
    }

    @Override
    public boolean canScrollVertically() {
        return canScroll && super.canScrollVertically();
    }

    public void setScrollEnabled(boolean enabled) {
        this.canScroll = enabled;
    }
}
