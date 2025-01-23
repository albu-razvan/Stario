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

package com.stario.launcher.apps.popup;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.apps.IconPackManager;
import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.icons.AdaptiveIconView;

import java.util.List;

public class IconsRecyclerAdapter extends RecyclerView.Adapter<IconsRecyclerAdapter.ViewHolder> {
    private final List<Pair<IconPackManager.IconPack, Pair<String, Drawable>>> icons;
    private final LauncherApplication application;
    private final View.OnClickListener listener;
    private final IconPackManager manager;
    private final ThemedActivity activity;

    public IconsRecyclerAdapter(ThemedActivity activity,
                                LauncherApplication application, View.OnClickListener listener) {
        this.activity = activity;
        this.application = application;
        this.listener = listener;
        this.manager = IconPackManager.from(activity);
        this.icons = manager.getIcons(application);
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {
        private final AdaptiveIconView icon;

        public ViewHolder(View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.icon);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull IconsRecyclerAdapter.ViewHolder viewHolder, int position) {
        Pair<IconPackManager.IconPack, Pair<String, Drawable>> item = icons.get(position);

        viewHolder.icon.setIcon(item.second.second);
        viewHolder.itemView.setOnClickListener(v -> {
            manager.setIconFor(application, item.first, item.second.first);

            if (listener != null) {
                listener.onClick(v);
            }
        });
    }

    @Override
    public int getItemCount() {
        return icons.size();
    }

    @NonNull
    @Override
    public IconsRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup container, int viewType) {
        return new IconsRecyclerAdapter.ViewHolder(LayoutInflater.from(activity)
                .inflate(R.layout.pop_up_icon_item, container, false));
    }
}
