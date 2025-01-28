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

package com.stario.launcher.sheet.drawer.category.folder;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Parcelable;
import android.transition.Transition;
import android.util.Log;
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
import com.stario.launcher.apps.categories.CategoryData;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.sheet.drawer.DrawerAdapter;
import com.stario.launcher.sheet.drawer.DrawerPage;
import com.stario.launcher.sheet.drawer.RecyclerApplicationAdapter;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.recyclers.async.InflationType;

import java.lang.reflect.Method;

public class Folder extends DrawerPage {
    private ItemTouchHelper itemTouchHelper;
    private OnCreateListener listener;
    private FolderAdapter adapter;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        GridLayoutManager manager = new GridLayoutManager(activity,
                Measurements.getListColumnCount()) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        };

        Measurements.addListColumnCountChangeListener(manager::setSpanCount);

        drawer.setLayoutManager(manager);
        drawer.setItemAnimator(null);

        drawer.addOnScrollListener(new RecyclerView.OnScrollListener() {
            Method method = null;
            boolean valid = true;

            @SuppressLint("SoonBlockedPrivateApi")
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                Object tester = getEnterTransition();

                if (tester instanceof Transition) {
                    try {
                        if (!valid) {
                            return;
                        }

                        if (method == null) {
                            //noinspection JavaReflectionMemberAccess
                            method = Transition.class
                                    .getDeclaredMethod("forceToEnd", ViewGroup.class);
                            method.setAccessible(true);
                        }

                        method.invoke(tester, rootView);
                    } catch (SecurityException | NoSuchMethodException |
                             IllegalAccessError exception) {
                        valid = false;
                    } catch (Exception exception) {
                        Log.e("Folder", "onScrollStateChanged: ", exception);
                    }
                }
            }
        });

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
                    ((RecyclerApplicationAdapter.ViewHolder) viewHolder).focus();

                    drawer.setItemAnimator(new DefaultItemAnimator());

                    Vibrations.getInstance().vibrate();
                }
            }

            @Override
            public int interpolateOutOfBoundsScroll(@NonNull RecyclerView recyclerView, int viewSize, int viewSizeOutOfBounds, int totalSize, long msSinceStartScroll) {
                return Math.min(super.interpolateOutOfBoundsScroll(recyclerView, viewSize, viewSizeOutOfBounds, totalSize, msSinceStartScroll), 100);
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                ((RecyclerApplicationAdapter.ViewHolder) viewHolder).clearFocus();
                RecyclerView.ItemAnimator itemAnimator = drawer.getItemAnimator();

                if (itemAnimator != null) {
                    itemAnimator.isRunning(() -> drawer.setItemAnimator(null));
                }
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return false;
            }

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                return makeFlag(ItemTouchHelper.ACTION_STATE_DRAG, ItemTouchHelper.DOWN | ItemTouchHelper.UP | ItemTouchHelper.START | ItemTouchHelper.END);
            }
        };

        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(drawer);

        if (listener != null) {
            listener.onCreate();
        }

        return rootView;
    }

    public void updateCategoryID(int categoryID) {
        postponeEnterTransition();

        setOnCreateListener(() -> {
            adapter = new FolderAdapter(activity, categoryID, itemTouchHelper);
            adapter.setInflationType(InflationType.SYNCED);

            drawer.setAdapter(adapter);

            drawer.post(() -> {
                drawer.scrollToPosition(0);
                updateTitleTransforms(drawer);

                title.setText(CategoryData.getInstance()
                        .getCategoryName(categoryID, getResources()));

                startPostponedEnterTransition();
            });
        });
    }

    private void setOnCreateListener(@NonNull OnCreateListener listener) {
        this.listener = listener;
    }

    private interface OnCreateListener {
        void onCreate();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.drawer_folder;
    }

    @Override
    protected int getPosition() {
        return DrawerAdapter.CATEGORIES_POSITION;
    }
}
