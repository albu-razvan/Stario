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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;

import com.stario.launcher.R;
import com.stario.launcher.sheet.drawer.DrawerAdapter;
import com.stario.launcher.sheet.drawer.DrawerPage;
import com.stario.launcher.ui.measurements.Measurements;
import com.stario.launcher.ui.recyclers.FastScroller;

public class List extends DrawerPage {
    private FastScroller fastScroller;

    @NonNull
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        fastScroller = rootView.findViewById(R.id.fast_scroller);

        fastScroller.setOverScrollMode(View.OVER_SCROLL_NEVER);

        GridLayoutManager manager = new GridLayoutManager(activity,
                Measurements.getListColumns());

        Measurements.addListColumnsListener(manager::setSpanCount);

        drawer.setLayoutManager(manager);
        drawer.setItemAnimator(null);

        ListAdapter adapter = new ListAdapter(activity, manager);

        drawer.setAdapter(adapter);

        Measurements.addSysUIListener(value -> {
            fastScroller.setTopOffset(drawer.getPaddingTop());
        });

        View searchContainer = (View) search.getParent();

        search.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                fastScroller.setBottomOffset(searchContainer.getPaddingBottom() + (bottom - top) +
                        Measurements.spToPx(32) + Measurements.dpToPx(20)));

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
