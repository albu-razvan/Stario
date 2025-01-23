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
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.sheet.drawer.DrawerAdapter;
import com.stario.launcher.sheet.drawer.DrawerPage;
import com.stario.launcher.ui.Measurements;

public class FolderList extends DrawerPage {
    @NonNull
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        GridLayoutManager manager = new GridLayoutManager(activity,
                Measurements.getFolderColumnCount()) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        };

        Measurements.addFolderColumnCountChangeListener(manager::setSpanCount);

        drawer.setLayoutManager(manager);
        drawer.setItemAnimator(null);

        FolderListAdapter adapter = new FolderListAdapter(activity, this);

        drawer.setAdapter(adapter);

        ItemTouchHelper.Callback callback = new ItemTouchHelper.Callback() {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                Parcelable state = manager.onSaveInstanceState();
                boolean result = adapter.move(viewHolder, target);
                manager.onRestoreInstanceState(state);

                if(result) {
                    Vibrations.getInstance().vibrate();
                }

                return false;
            }

            @Override
            public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG
                        && viewHolder != null) {
                    adapter.focus(viewHolder);

                    drawer.setItemAnimator(new DefaultItemAnimator());

                    Vibrations.getInstance().vibrate();
                }
            }

            @Override
            public int interpolateOutOfBoundsScroll(@NonNull RecyclerView recyclerView, int viewSize, int viewSizeOutOfBounds, int totalSize, long msSinceStartScroll) {
                return Math.min(super.interpolateOutOfBoundsScroll(recyclerView, viewSize, viewSizeOutOfBounds, totalSize, msSinceStartScroll), 100);
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return false;
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                adapter.reset(viewHolder);

                RecyclerView.ItemAnimator itemAnimator = drawer.getItemAnimator();

                if (itemAnimator != null) {
                    itemAnimator.isRunning(() -> drawer.setItemAnimator(null));
                }
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                return makeFlag(ItemTouchHelper.ACTION_STATE_DRAG, ItemTouchHelper.DOWN | ItemTouchHelper.UP | ItemTouchHelper.START | ItemTouchHelper.END);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(drawer);

        postpone();

        return rootView;
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.drawer_folder_list;
    }

    @Override
    protected int getPosition() {
        return DrawerAdapter.CATEGORIES_POSITION;
    }

    public void postpone() {
        if (drawer != null) {
            postponeEnterTransition();

            drawer.post(this::startPostponedEnterTransition);
        }
    }
}
