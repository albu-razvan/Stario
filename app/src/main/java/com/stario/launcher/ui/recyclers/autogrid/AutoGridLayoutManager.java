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

package com.stario.launcher.ui.recyclers.autogrid;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.ui.recyclers.managers.AccurateScrollComputeGridLayoutManager;
import com.stario.launcher.ui.recyclers.async.AsyncRecyclerAdapter;

public class AutoGridLayoutManager extends AccurateScrollComputeGridLayoutManager {
    private final RecyclerView.AdapterDataObserver observer;
    private final View.OnLayoutChangeListener layoutListener;

    private RecyclerView.Adapter<?> adapter;
    private RecyclerView recyclerView;
    private int actualSpanCount;

    public AutoGridLayoutManager(Context context, int spanCount) {
        super(context, spanCount);

        this.actualSpanCount = spanCount;
        this.recyclerView = null;
        this.adapter = null;
        this.layoutListener = new View.OnLayoutChangeListener() {
            int width = 0;

            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (view.getMeasuredWidth() != width) {
                    width = view.getMeasuredWidth();

                    centerItems();
                }
            }
        };
        this.observer = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                centerItems();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                centerItems();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                centerItems();
            }
        };
    }

    @Override
    public void setSpanCount(int spanCount) {
        this.actualSpanCount = spanCount;

        centerItems();
    }

    @Override
    public void onAttachedToWindow(RecyclerView view) {
        recyclerView = view;
        recyclerView.addOnLayoutChangeListener(layoutListener);

        super.onAttachedToWindow(view);
    }

    @Override
    public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
        super.onDetachedFromWindow(view, recycler);

        recyclerView.removeOnLayoutChangeListener(layoutListener);
        recyclerView = null;
        setAdapter(null);
    }

    void setAdapter(RecyclerView.Adapter<?> adapter) {
        if (this.adapter != null) {
            this.adapter.unregisterAdapterDataObserver(observer);
        }

        this.adapter = adapter;

        if (adapter != null) {
            adapter.registerAdapterDataObserver(observer);
            centerItems();
        }
    }

    private void centerItems() {
        if (recyclerView == null || adapter == null) {
            super.setSpanCount(actualSpanCount);
            return;
        }

        int itemCount;
        if (adapter instanceof AsyncRecyclerAdapter) {
            itemCount = ((AsyncRecyclerAdapter<?>) adapter).getTotalItemCount();
        } else {
            itemCount = adapter.getItemCount();
        }
        itemCount = getBalancedSpanCount(itemCount, actualSpanCount);

        if (itemCount < actualSpanCount) {
            super.setSpanCount(itemCount);

            ViewGroup.MarginLayoutParams marginLayoutParams =
                    ((ViewGroup.MarginLayoutParams) recyclerView.getLayoutParams());

            int margin = (int) ((recyclerView.getMeasuredWidth()
                    + marginLayoutParams.leftMargin + marginLayoutParams.rightMargin
                    - recyclerView.getPaddingLeft() - recyclerView.getPaddingRight())
                    * (1f - itemCount / (float) actualSpanCount) / 2);

            marginLayoutParams.leftMargin = Math.max(0, margin);
            marginLayoutParams.rightMargin = Math.max(0, margin);
            recyclerView.setLayoutParams(marginLayoutParams);
        } else {
            super.setSpanCount(actualSpanCount);

            ViewGroup.MarginLayoutParams marginLayoutParams =
                    ((ViewGroup.MarginLayoutParams) recyclerView.getLayoutParams());
            marginLayoutParams.leftMargin = 0;
            marginLayoutParams.rightMargin = 0;
            recyclerView.setLayoutParams(marginLayoutParams);
        }

        recyclerView.post(() -> {
            if(recyclerView != null) {
                recyclerView.requestLayout();
            }
        });
    }

    private int getBalancedSpanCount(int itemCount, int spanCount) {
        if (itemCount <= 0 || spanCount <= 0) {
            return 1;
        }

        return Math.min((int) Math.ceil((double) itemCount /
                ((int) Math.ceil((double) itemCount / spanCount))), spanCount);
    }
}
