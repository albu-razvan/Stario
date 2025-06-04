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

package com.stario.launcher.ui.recyclers.managers;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


/**
 * A custom GridLayoutManager that provides more accurate vertical scroll offset calculation.
 * <p>
 * <b>NOTE:</b> The layout manager assumes consistent item sizes for proper scroll offset calculations.
 *
 * @see GridLayoutManager
 */
public class AccurateScrollComputeLinearLayoutManager extends LinearLayoutManager {

    public AccurateScrollComputeLinearLayoutManager(Context context) {
        super(context, RecyclerView.VERTICAL, false);
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
    public int computeVerticalScrollOffset(RecyclerView.State state) {
        if (getChildCount() == 0) {
            return 0;
        }

        int position = findFirstVisibleItemPosition();

        if (position != RecyclerView.NO_POSITION) {
            View view = findViewByPosition(position);

            if (view != null) {
                return -(view.getTop()
                        - ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).topMargin
                        - getPaddingTop() - view.getHeight() * position);
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

            if (view != null) {
                return view.getHeight() * getItemCount();
            }
        }

        return 0;
    }

    @Override
    public int computeVerticalScrollExtent(RecyclerView.State state) {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    @Override
    public boolean canScrollHorizontally() {
        return false;
    }
}
