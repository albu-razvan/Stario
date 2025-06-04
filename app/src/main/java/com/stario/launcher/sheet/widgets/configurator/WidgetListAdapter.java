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

package com.stario.launcher.sheet.widgets.configurator;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
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
import com.stario.launcher.apps.ProfileApplicationManager;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.apps.ProfileManager;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.icons.AdaptiveIconView;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.utils.Utils;
import com.stario.launcher.ui.utils.animation.Animation;

import java.util.ArrayList;
import java.util.List;

public class WidgetListAdapter extends RecyclerView.Adapter<WidgetListAdapter.ViewHolder> {
    private final WidgetConfigurator.Request requestListener;
    private final ThemedActivity activity;
    private final RecyclerView recycler;
    private final WidgetEntries entries;
    private ViewHolder targetHolder;

    public WidgetListAdapter(ThemedActivity activity, RecyclerView recycler,
                             WidgetConfigurator.Request requestListener) {
        this.activity = activity;
        this.recycler = recycler;
        this.requestListener = requestListener;
        this.entries = new WidgetEntries();
        this.targetHolder = null;

        setHasStableIds(true);
    }

    public void update() {
        reset();

        PackageManager packageManager = this.activity.getPackageManager();

        List<AppWidgetProviderInfo> widgets = AppWidgetManager
                .getInstance(activity).getInstalledProviders();

        entries.clear();

        ProfileApplicationManager mainProfile =
                ProfileManager.getInstance().getProfile(null);

        if(mainProfile != null) {
            for (AppWidgetProviderInfo info : widgets) {
                String packageName = info.provider.getPackageName();
                WidgetGroupEntry entry = entries.contains(packageName);

                if (entry == null) {
                    LauncherApplication application = mainProfile.get(info.provider.getPackageName());

                    if (application != null) {
                        entry = new WidgetGroupEntry(packageName, application.getLabel(), application.getIcon());
                    } else {
                        String label;
                        Drawable icon;

                        if (Utils.isMinimumSDK(Build.VERSION_CODES.S)) {
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

                    int index = 0;
                    while (index < entries.size() &&
                            entries.get(index).compareTo(entry) < 0) {
                        index++;
                    }

                    entries.add(index, entry);
                }

                entry.addWidget(info);
            }
        }

        // stupidly inefficient
        notifyDataSetChanged();
    }

    private void reset() {
        if (targetHolder != null) {
            if (targetHolder.adapter != null) {
                targetHolder.adapter.reset();
            }

            targetHolder.widgets.setVisibility(View.GONE);

            targetHolder = null;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final AdaptiveIconView icon;
        private final TextView label;
        private final TextView count;
        private final RecyclerView widgets;
        private WidgetItemAdapter adapter;

        public ViewHolder(View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.preview);
            label = itemView.findViewById(R.id.label);
            count = itemView.findViewById(R.id.count);
            widgets = itemView.findViewById(R.id.prebuilt);

            itemView.setHapticFeedbackEnabled(false);

            widgets.setLayoutManager(new LinearLayoutManager(activity) {
                @Override
                public boolean canScrollVertically() {
                    return false;
                }
            });
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
                Vibrations.getInstance().vibrate();

                holder.adapter = new WidgetItemAdapter(activity, data, requestListener);
                holder.widgets.setAdapter(holder.adapter);

                if (holder.widgets.getVisibility() != View.VISIBLE) {
                    holder.widgets.setVisibility(View.VISIBLE);

                    reset();

                    targetHolder = holder;
                } else {
                    reset();
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
    public long getItemId(int position) {
        return entries.get(position)
                .packageName.hashCode();
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

    public static class WidgetGroupEntry implements Comparable<WidgetGroupEntry> {
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