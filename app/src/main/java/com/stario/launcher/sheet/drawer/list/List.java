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

package com.stario.launcher.sheet.drawer.list;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.stario.launcher.R;
import com.stario.launcher.apps.ProfileApplicationManager;
import com.stario.launcher.apps.ProfileManager;
import com.stario.launcher.sheet.drawer.search.ListDrawerPage;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.recyclers.FastScroller;
import com.stario.launcher.ui.recyclers.async.AsyncRecyclerAdapter;
import com.stario.launcher.ui.recyclers.autogrid.AutoGridLayoutManager;
import com.stario.launcher.ui.utils.LayoutSizeObserver;
import com.stario.launcher.utils.Utils;
import com.stario.launcher.utils.objects.ObservableObject;

public class List extends ListDrawerPage {
    private static final String USER_HANDLE_KEY = "com.stario.UserHandle";

    private FastScroller fastScroller;
    private UserHandle handle;

    public List() {
        this.handle = null;
    }

    public List(@NonNull ProfileApplicationManager profile) {
        this.handle = profile.handle;
    }

    @NonNull
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        fastScroller = rootView.findViewById(R.id.fast_scroller);

        AutoGridLayoutManager manager = new AutoGridLayoutManager(activity, 1);
        LayoutSizeObserver.attach(fastScroller, LayoutSizeObserver.WIDTH, new LayoutSizeObserver.OnChange() {
            @Override
            public void onChange(View view, int watchFlags, Rect rect) {
                manager.setSpanCount(getColumnCount(rect.width()));
            }
        });

        drawer.setLayoutManager(manager);
        drawer.setItemAnimator(null);

        Measurements.addStatusBarListener(value -> fastScroller.setTopOffset(drawer.getPaddingTop()));

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View searchContainer = (View) search.getParent();
        search.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (Measurements.isLandscape() && view.getMeasuredWidth() >
                    // FastScroller popup size * 2 + search width
                    Measurements.spToPx(200) + searchContainer.getMeasuredWidth()) {
                fastScroller.setBottomOffset(searchContainer.getPaddingBottom());
            } else {
                fastScroller.setBottomOffset(searchContainer.getPaddingBottom() + (bottom - top) +
                        Measurements.spToPx(32) + Measurements.dpToPx(20));
            }
        });
    }

    @Override
    public void setSelected(boolean selected) {
        if (isSelected() == selected) {
            super.setSelected(selected);

            return;
        }

        super.setSelected(selected);
        fastScroller.animateVisibility(selected);
    }

    @Override
    public void onResume() {
        if (ProfileManager.getInstance().getProfiles().size() <= 1) {
            title.setText(R.string.apps);
        } else {
            title.setText(Utils.isMainProfile(handle) ? R.string.personal : R.string.managed);
        }

        super.onResume();
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(USER_HANDLE_KEY)) {
            if (Utils.isMinimumSDK(Build.VERSION_CODES.TIRAMISU)) {
                handle = savedInstanceState.getParcelable(USER_HANDLE_KEY, UserHandle.class);
            } else {
                handle = savedInstanceState.getParcelable(USER_HANDLE_KEY);
            }
        }

        ProfileApplicationManager applicationManager = ProfileManager.getInstance().getProfile(handle);
        AsyncRecyclerAdapter<?> adapter = new ListAdapter(activity, applicationManager);
        LayoutSizeObserver.attach(drawer, LayoutSizeObserver.WIDTH, new LayoutSizeObserver.OnChange() {
            final ObservableObject<Integer> columnCount;

            {
                columnCount = new ObservableObject<>(0,
                        object -> adapter.approximateRecyclerHeight());
            }

            @Override
            public void onChange(View view, int watchFlags, Rect rect) {
                columnCount.updateObject(getColumnCount(rect.width()));
            }
        });
        adapter.setRecyclerHeightApproximationListener(height -> {
            ViewGroup parent = (ViewGroup) drawer.getParent();
            ConstraintLayout.LayoutParams params =
                    (ConstraintLayout.LayoutParams) drawer.getLayoutParams();

            if (parent.getMeasuredHeight() < height) {
                if (params.height == ConstraintLayout.LayoutParams.WRAP_CONTENT) {
                    params.height = ConstraintLayout.LayoutParams.MATCH_PARENT;
                    drawer.requestLayout();
                }
            } else if (params.height == ConstraintLayout.LayoutParams.MATCH_PARENT) {
                params.height = ConstraintLayout.LayoutParams.WRAP_CONTENT;
                drawer.requestLayout();
            }

            drawer.requestLayout();
        });
        drawer.setAdapter(adapter);

        applicationManager.addOnReadyListener(manager -> showLayout());

        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelable(USER_HANDLE_KEY, handle);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.drawer_list;
    }

    public UserHandle getUserHandle() {
        return handle;
    }
}
