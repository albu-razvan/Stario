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
import android.content.SharedPreferences;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.apps.ProfileApplicationManager;
import com.stario.launcher.apps.ProfileManager;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.sheet.drawer.RecyclerApplicationAdapter;
import com.stario.launcher.sheet.drawer.search.JaroWinklerDistance;
import com.stario.launcher.sheet.drawer.search.SearchFragment;
import com.stario.launcher.sheet.drawer.search.Searchable;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.recyclers.async.InflationType;

import java.util.ArrayList;
import java.util.List;

public class AppAdapter extends RecyclerApplicationAdapter
        implements Searchable {
    private final SharedPreferences preferences;
    private List<LauncherApplication> applications;
    private RecyclerView recyclerView;
    private String currentQuery;

    public AppAdapter(ThemedActivity activity) {
        super(activity, null, InflationType.SYNCED);

        this.applications = new ArrayList<>();
        this.currentQuery = "";
        this.preferences = activity.getSharedPreferences(Entry.SEARCH);
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
    public int getTotalItemCount() {
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

            boolean showHiddenItems = preferences.getBoolean(SearchFragment.SEARCH_HIDDEN_APPS, false);

            for (ProfileApplicationManager manager : profileManagers) {
                for (int index = 0; index < (showHiddenItems ?
                        manager.getSize() : manager.getActualSize()); index++) {
                    LauncherApplication application = showHiddenItems ?
                            manager.get(index, true) : manager.get(index);

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
        currentQuery = query;

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

    private void updateRecyclerVisibility() {
        if (recyclerView != null) {
            if (getItemCount() == 0 && recyclerView.getVisibility() != View.GONE) {
                recyclerView.setVisibility(View.GONE);
            } else if (getItemCount() > 0 && recyclerView.getVisibility() != View.VISIBLE) {
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onBind(@NonNull ViewHolder viewHolder, int index) {
        super.onBind(viewHolder, index);

        String label = getApplication(index).getLabel();
        int substringStart = label.toLowerCase().indexOf(currentQuery);

        if (substringStart >= 0) {
            SpannableStringBuilder builder = new SpannableStringBuilder(label);
            builder.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                    substringStart, substringStart + currentQuery.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            viewHolder.setLabel(builder);
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