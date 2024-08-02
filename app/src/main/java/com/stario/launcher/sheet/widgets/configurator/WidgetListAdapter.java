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

package com.stario.launcher.sheet.widgets.configurator;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.transition.ChangeBounds;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.sheet.drawer.apps.LauncherApplication;
import com.stario.launcher.sheet.drawer.apps.LauncherApplicationManager;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.icons.AdaptiveIconView;
import com.stario.launcher.ui.measurements.Measurements;
import com.stario.launcher.utils.Utils;
import com.stario.launcher.utils.animation.Animation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WidgetListAdapter extends RecyclerView.Adapter<WidgetListAdapter.ViewHolder> {
    private final WidgetConfigurator.Request requestListener;
    private final ThemedActivity activity;
    private final RecyclerView recycler;
    private final WidgetEntries entries;

    public WidgetListAdapter(ThemedActivity activity, RecyclerView recycler,
                             WidgetConfigurator.Request requestListener) {
        this.activity = activity;
        this.recycler = recycler;
        this.requestListener = requestListener;
        this.entries = new WidgetEntries();
    }

    public void update() {
        PackageManager packageManager = this.activity.getPackageManager();

        List<AppWidgetProviderInfo> widgets = AppWidgetManager
                .getInstance(activity).getInstalledProviders();

        entries.clear();

        for (AppWidgetProviderInfo info : widgets) {
            String packageName = info.provider.getPackageName();
            WidgetGroupEntry entry = entries.contains(packageName);

            if (entry == null) {
                LauncherApplication application = LauncherApplicationManager.getInstance()
                        .get(info.provider.getPackageName());

                if (application != null) {
                    entry = new WidgetGroupEntry(packageName, application.getLabel(), application.getIcon());
                } else {
                    String label;
                    Drawable icon;

                    if (Utils.isMinimumSDK(31)) {
                        ActivityInfo activityInfo = info.getActivityInfo();

                        label = activityInfo.loadLabel(packageManager).toString();
                        icon = activityInfo.loadIcon(packageManager);
                    } else {
                        try {
                            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);

                            label = applicationInfo.loadLabel(packageManager).toString();
                            icon = applicationInfo.loadIcon(packageManager);
                        } catch (PackageManager.NameNotFoundException exception) {
                            label = info.loadLabel(packageManager);
                            icon = info.loadIcon(activity, Measurements.getDotsPerInch());
                        }
                    }

                    entry = new WidgetGroupEntry(packageName, label, icon);
                }

                entries.add(entry);
            }

            entry.addWidget(info);
        }

        Collections.sort(entries);

        // stupidly inefficient
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final AdaptiveIconView icon;
        private final TextView label;
        private final TextView count;
        private final RecyclerView widgets;

        public ViewHolder(View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.preview);
            label = itemView.findViewById(R.id.label);
            count = itemView.findViewById(R.id.count);
            widgets = itemView.findViewById(R.id.prebuilt);

            widgets.setLayoutManager(new LinearLayoutManager(activity));
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int index) {
        WidgetGroupEntry data = entries.get(index);

        if (data != null) {
            holder.icon.setIcon(data.icon);
            holder.label.setText(data.label);

            int size = data.widgets.size();
            holder.count.setText(" - " + size + " " +
                    (size == 1 ?
                            activity.getResources().getString(R.string.widget_one) :
                            activity.getResources().getString(R.string.widget_many)));

            holder.itemView.setOnClickListener(v -> {
                if (holder.widgets.getAdapter() == null) {
                    holder.widgets.setAdapter(new WidgetItemAdapter(activity, data, requestListener));
                }

                if (holder.widgets.getVisibility() == View.GONE) {
                    holder.widgets.setVisibility(View.VISIBLE);
                } else {
                    holder.widgets.setVisibility(View.GONE);
                }

                TransitionManager.beginDelayedTransition(recycler,
                        new ChangeBounds()
                                .setInterpolator(new AccelerateDecelerateInterpolator())
                                .setDuration(Animation.MEDIUM.getDuration()));
            });
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup container, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(container.getContext());

        return new ViewHolder(inflater.inflate(R.layout.widget_picker_group, container, false));
    }

    private static class WidgetEntries extends ArrayList<WidgetGroupEntry> {
        public WidgetGroupEntry contains(String packageName) {
            for (WidgetGroupEntry entry : this) {
                if (entry.packageName.equals(packageName)) {
                    return entry;
                }
            }

            return null;
        }
    }

    static class WidgetGroupEntry implements Comparable<WidgetGroupEntry> {
        final String packageName;
        final Drawable icon;
        final String label;
        final List<AppWidgetProviderInfo> widgets;

        WidgetGroupEntry(String packageName, String label, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
            this.widgets = new ArrayList<>();
        }

        private void addWidget(AppWidgetProviderInfo info) {
            widgets.add(info);
        }

        @Override
        public int compareTo(WidgetGroupEntry entry) {
            return label == null ? 0 : label.compareTo(entry.label);
        }
    }
}