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

package com.stario.launcher.sheet.drawer.search.adapters;

import android.animation.LayoutTransition;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;

import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.sheet.drawer.apps.LauncherApplication;
import com.stario.launcher.sheet.drawer.apps.LauncherApplicationManager;
import com.stario.launcher.sheet.drawer.search.JaroWinklerDistance;
import com.stario.launcher.sheet.drawer.search.Searchable;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.measurements.Measurements;
import com.stario.launcher.sheet.drawer.RecyclerApplicationAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AppAdapter extends RecyclerApplicationAdapter
        implements Searchable {
    private final ArrayList<LauncherApplication> applications;
    private final RecyclerView.LayoutManager layoutManager;
    private final Filter filter;

    public AppAdapter(ThemedActivity activity, ViewGroup content,
                      RecyclerView.LayoutManager layoutManager) {
        super(activity);

        this.layoutManager = layoutManager;
        this.applications = new ArrayList<>();

        this.filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<LauncherApplication> filteredList = new ArrayList<>();
                FilterResults results = null;

                int starting = 0;
                int containing = 0;
                int close = 0;

                if (constraint != null && constraint.length() > 0) {
                    String filterPattern = constraint.toString().toLowerCase().trim();

                    LauncherApplicationManager manager = LauncherApplicationManager.getInstance();

                    for (int index = 0; index < manager.getSize(); index++) {
                        LauncherApplication application = manager.get(index, true);

                        if (application != null) {
                            String lowercaseLabel = application.getLabel().toLowerCase();

                            if (lowercaseLabel.startsWith(filterPattern)) {
                                filteredList.add(starting++, application);

                                if (filteredList.size() >=
                                        Measurements.getListColumns()) {
                                    break;
                                }
                            } else if (lowercaseLabel.contains(filterPattern)) {
                                filteredList.add(starting + containing++, application);

                                if (filteredList.size() >=
                                        Measurements.getListColumns()) {
                                    break;
                                }
                            } else if (JaroWinklerDistance.getScore(lowercaseLabel, filterPattern) > 0.87d) {
                                filteredList.add(starting + containing + close++, application);

                                if (filteredList.size() >=
                                        Measurements.getListColumns()) {
                                    break;
                                }
                            }
                        }
                    }

                    results = new FilterResults();
                    results.values = filteredList;
                }

                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                applications.clear();

                if (results != null) {
                    applications.addAll((Collection<LauncherApplication>) results.values);
                }

                LayoutTransition transition = content.getLayoutTransition();

                if (transition != null && transition.isRunning()) {
                    transition.addTransitionListener(new LayoutTransition.TransitionListener() {
                        @Override
                        public void startTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
                        }

                        @Override
                        public void endTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
                            notifyDataSetChanged();

                            transition.removeTransitionListener(this);
                        }
                    });
                } else {
                    notifyDataSetChanged();
                }
            }
        };
    }

    @Override
    protected LauncherApplication getApplication(int index) {
        if (index >= getSize()) {
            return applications.get(0);
        }

        return applications.get(index);
    }

    @Override
    protected int getLabelLineCount() {
        return 1;
    }

    @Override
    protected int getSize() {
        return applications.size();
    }

    @Override
    public void update(String query) {
        filter.filter(query);
    }

    @Override
    public boolean submit() {
        View view = layoutManager.findViewByPosition(0);

        if (view != null) {
            view.callOnClick();

            return true;
        } else {
            return false;
        }
    }

    @Override
    public Filter getFilter() {
        return filter;
    }
}