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

package com.stario.launcher.sheet.drawer.search.recyclers.adapters;

import android.annotation.SuppressLint;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.apps.ProfileManager;
import com.stario.launcher.apps.ProfileApplicationManager;
import com.stario.launcher.sheet.drawer.RecyclerApplicationAdapter;
import com.stario.launcher.sheet.drawer.search.JaroWinklerDistance;
import com.stario.launcher.sheet.drawer.search.SearchFragment;
import com.stario.launcher.sheet.drawer.search.Searchable;
import com.stario.launcher.sheet.drawer.search.recyclers.OnVisibilityChangeListener;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.recyclers.async.InflationType;

import java.util.ArrayList;
import java.util.List;

public class AppAdapter extends RecyclerApplicationAdapter
        implements Searchable {
    private List<LauncherApplication> applications;
    private OnVisibilityChangeListener listener;
    private RecyclerView recyclerView;

    public AppAdapter(ThemedActivity activity) {
        super(activity, null, InflationType.SYNCED);

        this.applications = new ArrayList<>();
    }

    @Override
    protected LauncherApplication getApplication(int index) {
        return applications.get(index);
    }

    @Override
    protected boolean allowApplicationStateEditing() {
        return false;
    }

    @Override
    protected int getLabelLineCount() {
        return 1;
    }

    @Override
    protected int getSize() {
        return applications.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void update(String query) {
        List<LauncherApplication> filteredList = new ArrayList<>();

        int starting = 0;
        int containing = 0;
        int close = 0;

        if (query != null && !query.isEmpty()) {
            String filterPattern = query.toLowerCase();

            List<ProfileApplicationManager> profileManagers =
                    ProfileManager.getInstance().getProfiles();

            for(ProfileApplicationManager manager : profileManagers) {
                for (int index = 0; index < manager.getSize(); index++) {
                    LauncherApplication application = manager.get(index, true);

                    if (application != null) {
                        String lowercaseLabel = application.getLabel().toLowerCase();

                        if (lowercaseLabel.startsWith(filterPattern)) {
                            filteredList.add(starting++, application);
                        } else if (lowercaseLabel.contains(filterPattern)) {
                            filteredList.add(starting + containing++, application);
                        } else if (JaroWinklerDistance.getScore(lowercaseLabel, filterPattern) > 0.87d) {
                            filteredList.add(starting + containing + close++, application);
                        }
                    }
                }
            }

            if (filteredList.size() >
                    SearchFragment.MAX_APP_QUERY_ITEMS) {
                filteredList = filteredList.subList(0, SearchFragment.MAX_APP_QUERY_ITEMS);
            }
        }

        applications = filteredList;

        Runnable runnable = () -> {
            notifyDataSetChanged();
            updateRecyclerVisibility();
        };

        if (recyclerView != null && recyclerView.isAnimating()) {
            recyclerView.post(runnable);
        } else {
            runnable.run();
        }
    }

    @Override
    public boolean submit() {
        if (recyclerView != null &&
                recyclerView.getLayoutManager() != null &&
                recyclerView.getVisibility() == View.VISIBLE) {
            View view = recyclerView.getLayoutManager()
                    .findViewByPosition(0);

            if (view != null) {
                view.callOnClick();

                return true;
            } else {
                return false;
            }
        }

        return false;
    }

    public void setOnVisibilityChangeListener(OnVisibilityChangeListener listener) {
        this.listener = listener;
    }

    private void updateRecyclerVisibility() {
        if (recyclerView != null) {
            if (getItemCount() == 0 && recyclerView.getVisibility() != View.GONE) {
                if (listener != null) {
                    listener.onPreChange(recyclerView, View.GONE);
                }

                recyclerView.setVisibility(View.GONE);

                recyclerView.post(() -> {
                    if (listener != null) {
                        listener.onChange(recyclerView, View.GONE);
                    }
                });
            } else if (getItemCount() > 0 && recyclerView.getVisibility() != View.VISIBLE) {
                if (listener != null) {
                    listener.onPreChange(recyclerView, View.VISIBLE);
                }

                recyclerView.setVisibility(View.VISIBLE);

                recyclerView.post(() -> {
                    if (listener != null) {
                        listener.onChange(recyclerView, View.VISIBLE);
                    }
                });
            }
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        this.recyclerView = recyclerView;

        updateRecyclerVisibility();

        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        updateRecyclerVisibility();

        this.recyclerView = null;
        super.onDetachedFromRecyclerView(recyclerView);
    }
}