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

package com.stario.launcher.sheet.drawer.search.recyclers.adapters;

import android.annotation.SuppressLint;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.sheet.drawer.search.Searchable;
import com.stario.launcher.sheet.drawer.search.recyclers.OnVisibilityChangeListener;

public abstract class AbstractSearchListAdapter<V extends RecyclerView.ViewHolder> extends
        RecyclerView.Adapter<V> implements Searchable {
    private OnVisibilityChangeListener listener;
    private RecyclerView recyclerView;

    public void invalidateRecyclerVisibility() {
        if (recyclerView != null) {
            if (getItemCount() == 0 && recyclerView.getVisibility() != View.GONE) {
                if (listener != null) {
                    listener.onPreChange(recyclerView, View.GONE);
                }

                recyclerView.setVisibility(View.GONE);

                recyclerView.post(() -> {
                    if (listener != null) {
                        listener.onChange(recyclerView, View.GONE);
                    }
                });
            } else if (getItemCount() > 0 && recyclerView.getVisibility() != View.VISIBLE) {
                if (listener != null) {
                    listener.onPreChange(recyclerView, View.VISIBLE);
                }

                recyclerView.setVisibility(View.VISIBLE);

                recyclerView.post(() -> {
                    if (listener != null) {
                        listener.onChange(recyclerView, View.VISIBLE);
                    }
                });
            }
        }
    }

    public void setOnVisibilityChangeListener(OnVisibilityChangeListener listener) {
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void notifyInternal() {
        Runnable runnable = () -> {
            notifyDataSetChanged();
            invalidateRecyclerVisibility();
        };

        if (recyclerView != null && recyclerView.isAnimating()) {
            recyclerView.post(runnable);
        } else {
            runnable.run();
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        this.recyclerView = recyclerView;

        invalidateRecyclerVisibility();

        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        invalidateRecyclerVisibility();

        this.recyclerView = null;
        super.onDetachedFromRecyclerView(recyclerView);
    }
}
