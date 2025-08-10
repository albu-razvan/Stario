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

package com.stario.launcher.activities.launcher.pins.dialog;

import android.annotation.SuppressLint;

import com.stario.launcher.apps.Category;
import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.sheet.drawer.RecyclerApplicationAdapter;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.recyclers.async.InflationType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PinnedAppsGroupDialogRecyclerAdapter extends RecyclerApplicationAdapter {
    private List<LauncherApplication> applications;

    public PinnedAppsGroupDialogRecyclerAdapter(ThemedActivity activity) {
        super(activity, InflationType.SYNCED);

        this.applications = Collections.emptyList();
    }

    @Override
    protected LauncherApplication getApplication(int index) {
        if (index >= 0 && index < applications.size()) {
            return applications.get(index);
        }

        return LauncherApplication.FALLBACK_APP;
    }

    @Override
    protected boolean allowApplicationStateEditing() {
        return true;
    }

    @Override
    public int getTotalItemCount() {
        return applications.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateDataSnapshot(Category category, int skip) {
        if (category == null) {
            this.applications = Collections.emptyList();
        } else {
            List<LauncherApplication> snapshot = new ArrayList<>();

            for (int index = skip; index < category.getSize(); index++) {
                snapshot.add(category.get(index));
            }

            this.applications = snapshot;
        }

        notifyDataSetChanged();
    }
}
