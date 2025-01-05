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

package com.stario.launcher.sheet.drawer.list;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;

import com.stario.launcher.R;
import com.stario.launcher.sheet.drawer.DrawerAdapter;
import com.stario.launcher.sheet.drawer.DrawerPage;
import com.stario.launcher.sheet.drawer.dialog.ApplicationsDialog;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.recyclers.FastScroller;

public class List extends DrawerPage {
    private LocalBroadcastManager broadcastManager;
    private BroadcastReceiver visibilityReceiver;
    private BroadcastReceiver positionReceiver;
    private FastScroller fastScroller;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        broadcastManager = LocalBroadcastManager.getInstance(context);
    }

    @Override
    public void onDetach() {
        if (visibilityReceiver != null) {
            broadcastManager.unregisterReceiver(visibilityReceiver);
        }

        if (positionReceiver != null) {
            broadcastManager.unregisterReceiver(positionReceiver);
        }

        super.onDetach();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        broadcastManager.registerReceiver(visibilityReceiver,
                new IntentFilter(ApplicationsDialog.getTopic()));
        broadcastManager.registerReceiver(positionReceiver,
                new IntentFilter(ApplicationsDialog.getTopic(getView())));
    }

    @NonNull
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        fastScroller = rootView.findViewById(R.id.fast_scroller);

        fastScroller.setOverScrollMode(View.OVER_SCROLL_NEVER);

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

        ListAdapter adapter = new ListAdapter(activity, drawer);

        drawer.setAdapter(adapter);

        Measurements.addSysUIListener(value -> {
            fastScroller.setTopOffset(drawer.getPaddingTop());
        });

        View searchContainer = (View) search.getParent();

        search.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (Measurements.isLandscape() && Measurements.getWidth() >
                    // FastScroller popup size * 2 + search width
                    Measurements.spToPx(200) + searchContainer.getMeasuredWidth()) {
                fastScroller.setBottomOffset(searchContainer.getPaddingBottom() + (bottom - top));
            } else {
                fastScroller.setBottomOffset(searchContainer.getPaddingBottom() + (bottom - top) +
                        Measurements.spToPx(32) + Measurements.dpToPx(20));
            }
        });

        positionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                fastScroller.animateVisibility(
                        intent.getFloatExtra(ApplicationsDialog.INTENT_EXTRA_PAGE_POSITION, 0f) == 0);
            }
        };

        visibilityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                fastScroller.animateVisibility(
                        intent.getBooleanExtra(ApplicationsDialog.INTENT_EXTRA_PAGER_VISIBILITY, true));
            }
        };

        return rootView;
    }

    @Override
    protected int getPosition() {
        return DrawerAdapter.LIST_POSITION;
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.drawer_list;
    }
}
