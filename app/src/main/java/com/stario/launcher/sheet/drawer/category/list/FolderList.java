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
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.apps.CategoryManager;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.sheet.drawer.DrawerPage;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.recyclers.autogrid.AutoGridLayoutManager;
import com.stario.launcher.ui.utils.LayoutSizeObserver;

public class FolderList extends DrawerPage {
    @NonNull
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        postponeEnterTransition();

        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        AutoGridLayoutManager manager = new AutoGridLayoutManager(activity, 1);
        LayoutSizeObserver.attach(rootView, LayoutSizeObserver.WIDTH, new LayoutSizeObserver.OnChange() {
            @Override
            public void onChange(View view, int watchFlags, Rect rect) {
                int width = rect.width();
                int columns;

                if (width < Measurements.dpToPx(350)) {
                    columns = 1;
                } else if (width < Measurements.dpToPx(380)) {
                    columns = 2;
                } else {
                    columns = Math.max(1, width / Measurements.dpToPx(190));
                }

                manager.setSpanCount(columns);
            }
        });

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

                if (result) {
                    Vibrations.getInstance().vibrate();
                }

                return false;
            }

            @Override
            public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG
                        && viewHolder != null) {
                    viewHolder.itemView.forceLayout();
                    adapter.focus(viewHolder);

                    drawer.setItemAnimator(new DefaultItemAnimator());

                    Vibrations.getInstance().vibrate();

                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                }
            }

            @Override
            public int interpolateOutOfBoundsScroll(@NonNull RecyclerView recyclerView, int viewSize, int viewSizeOutOfBounds, int totalSize, long msSinceStartScroll) {
                return Math.min(super.interpolateOutOfBoundsScroll(recyclerView, viewSize, viewSizeOutOfBounds, totalSize, msSinceStartScroll), 100);
            }

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                return makeFlag(ItemTouchHelper.ACTION_STATE_DRAG, ItemTouchHelper.DOWN | ItemTouchHelper.UP | ItemTouchHelper.START | ItemTouchHelper.END);
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                adapter.reset(viewHolder);

                RecyclerView.ItemAnimator itemAnimator = drawer.getItemAnimator();

                if (itemAnimator != null) {
                    itemAnimator.isRunning(() -> drawer.setItemAnimator(null));
                }

                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return false;
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(drawer);

        drawer.post(this::startPostponedEnterTransition);

        CategoryManager.getInstance()
                .addOnReadyListener(applicationManager -> showLayout());

        return rootView;
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.drawer_folder_list;
    }
}
