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

package com.stario.launcher.activities.settings.dialogs.hide.pager;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.stario.launcher.R;
import com.stario.launcher.apps.ProfileApplicationManager;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.recyclers.autogrid.AutoGridLayoutManager;
import com.stario.launcher.ui.recyclers.overscroll.OverScrollEffect;
import com.stario.launcher.ui.recyclers.overscroll.OverScrollRecyclerView;

public class HideApplicationsPage extends Fragment {
    private ProfileApplicationManager applicationManager;
    private OverScrollRecyclerView recyclerView;

    public HideApplicationsPage() {
        // default
    }

    public HideApplicationsPage(ProfileApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.pop_up_hide_page, container, false);

        recyclerView = root.findViewById(R.id.recycler);
        recyclerView.setOverscrollPullEdges(OverScrollEffect.PULL_EDGE_BOTTOM);

        AutoGridLayoutManager manager = new AutoGridLayoutManager(getActivity(),
                Measurements.getListColumnCount(Measurements.dpToPx(500)));

        recyclerView.setLayoutManager(manager);
        recyclerView.setItemAnimator(null);

        recyclerView.setAdapter(new HiddenRecyclerAdapter(
                (ThemedActivity) getActivity(), applicationManager) {
        });

        return root;
    }

    public OverScrollRecyclerView getRecycler() {
        return recyclerView;
    }
}
