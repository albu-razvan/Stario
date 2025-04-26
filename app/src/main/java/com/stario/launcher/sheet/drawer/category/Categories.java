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

package com.stario.launcher.sheet.drawer.category;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.stario.launcher.R;
import com.stario.launcher.sheet.drawer.category.list.FolderList;

public class Categories extends Fragment {
    public static final String STACK_ID = "com.stario.CategoriesFragments";
    private static final String RESTORE_IDENTIFIER = "Categories.RESTORE";

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(RESTORE_IDENTIFIER, true);

        super.onSaveInstanceState(outState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.drawer_categories, container, false);

        boolean restored = false;

        if (savedInstanceState != null &&
                savedInstanceState.getBoolean(RESTORE_IDENTIFIER, false)) {
            FragmentManager manager = getParentFragmentManager();

            for (Fragment fragment : manager.getFragments()) {
                if (FolderList.class.equals(fragment.getClass())) {
                    if (fragment.isHidden()) {
                        manager.beginTransaction()
                                .show(fragment)
                                .commit();
                    }

                    restored = true;

                    break;
                }
            }
        }

        if (!restored) {
            view.post(() -> {
                try {
                    getParentFragmentManager().beginTransaction()
                            .add(R.id.categories, new FolderList())
                            .commit();
                } catch (Exception exception) {
                    Log.e("Categories", "FolderList failed to attach.");
                }
            });
        }

        return view;
    }
}
