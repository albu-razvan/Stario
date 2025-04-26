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

package com.stario.launcher.activities.settings.dialogs.hide;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.stario.launcher.R;
import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.apps.ProfileManager;
import com.stario.launcher.apps.CategoryManager;
import com.stario.launcher.apps.ProfileApplicationManager;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.icons.AdaptiveIconView;

public class HiddenRecyclerAdapter extends RecyclerView.Adapter<HiddenRecyclerAdapter.ViewHolder> {
    private final ProfileApplicationManager manager;
    private final SharedPreferences hiddenApps;
    private final Context activity;

    public HiddenRecyclerAdapter(ThemedActivity activity) {
        this.activity = activity;
        this.hiddenApps = activity.getSharedPreferences(Entry.HIDDEN_APPS);
        this.manager = ProfileManager.getInstance().getProfile(null);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialSwitch materialSwitch;
        private final AdaptiveIconView icon;
        private final TextView category;
        private final TextView label;

        @SuppressLint("ClickableViewAccessibility")
        public ViewHolder(View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.icon);
            category = itemView.findViewById(R.id.category);
            label = itemView.findViewById(R.id.label);
            materialSwitch = itemView.findViewById(R.id.hide_switch);

            materialSwitch.setClickable(false);
            itemView.setOnClickListener(view -> materialSwitch.performClick());
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        LauncherApplication application = manager.get(position, false);

        if (application != null) {
            viewHolder.icon.setApplication(application);
            viewHolder.label.setText(application.getLabel());
            viewHolder.category.setText(CategoryManager.getInstance()
                    .getCategoryName(application.getCategory()));

            String packageName = application.getInfo().packageName;

            viewHolder.materialSwitch.setChecked(hiddenApps.contains(packageName));
            viewHolder.materialSwitch.jumpDrawablesToCurrentState();
            viewHolder.materialSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    manager.hideApplication(application);
                } else {
                    manager.showApplication(application);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return manager.getActualSize();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup container, int viewType) {
        return new ViewHolder(LayoutInflater.from(activity)
                .inflate(R.layout.hidden_item, container, false));
    }
}