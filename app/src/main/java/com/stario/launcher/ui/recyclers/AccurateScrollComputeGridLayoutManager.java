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

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


/**
 * A custom GridLayoutManager that provides more accurate vertical scroll offset calculation.
 * <p>
 * <b>NOTE:</b> The layout manager assumes consistent item sizes for proper scroll offset calculations.
 *
 * @see GridLayoutManager
 */
public class AccurateScrollComputeGridLayoutManager extends GridLayoutManager {
    public AccurateScrollComputeGridLayoutManager(Context context, int spanCount) {
        super(context, spanCount, RecyclerView.VERTICAL, false);
    }

    public AccurateScrollComputeGridLayoutManager(Context context, int spanCount, boolean reverseLayout) {
        super(context, spanCount, RecyclerView.VERTICAL, reverseLayout);
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
        return false;
    }

    @Override
    public int computeVerticalScrollOffset(RecyclerView.State state) {
        if (getChildCount() == 0) {
            return 0;
        }

        int position = findFirstVisibleItemPosition();

        if (position != RecyclerView.NO_POSITION) {
            View view = findViewByPosition(position);

            if (view != null) {
                return (int) -(view.getTop() -
                        view.getHeight() * Math.ceil(position / (float) getSpanCount()) -
                        getPaddingTop());
            }
        }

        return 0;
    }

    @Override
    public int computeVerticalScrollRange(RecyclerView.State state) {
        if (getChildCount() == 0) {
            return 0;
        }

        int position = findFirstVisibleItemPosition();

        if (position != RecyclerView.NO_POSITION) {
            View view = findViewByPosition(position);

            int rows = (int) Math.ceil(getItemCount() / (float) getSpanCount());

            if (view != null) {
                return Math.max(0, view.getHeight() * rows -
                        (getHeight() - getPaddingTop() - getPaddingBottom()));
            }
        }

        return 0;
    }
}
