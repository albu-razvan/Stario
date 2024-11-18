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

package com.stario.launcher.sheet.drawer.category.folder;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;

import com.stario.launcher.R;
import com.stario.launcher.apps.categories.CategoryData;
import com.stario.launcher.sheet.drawer.DrawerAdapter;
import com.stario.launcher.sheet.drawer.DrawerPage;
import com.stario.launcher.ui.measurements.Measurements;
import com.stario.launcher.ui.recyclers.async.InflationType;

public class Folder extends DrawerPage {
    private OnCreateListener listener;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        GridLayoutManager manager = new GridLayoutManager(activity,
                Measurements.getListColumnCount());

        Measurements.addListColumnCountChangeListener(manager::setSpanCount);

        drawer.setLayoutManager(manager);
        drawer.setItemAnimator(null);

        if (listener != null) {
            listener.onCreate();
        }

        return rootView;
    }

    public void updateCategoryID(int categoryID) {
        postponeEnterTransition();

        setOnCreateListener(() -> {
            FolderAdapter adapter = new FolderAdapter(activity, categoryID);
            adapter.setInflationType(InflationType.SYNCED);

            drawer.setAdapter(adapter);

            drawer.post(() -> {
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
