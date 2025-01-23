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

package com.stario.launcher.activities.settings.dialogs.icons;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.apps.IconPackManager;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.icons.AdaptiveIconView;
import com.stario.launcher.utils.UiUtils;

import java.util.concurrent.CompletableFuture;

public class IconsRecyclerAdapter extends RecyclerView.Adapter<IconsRecyclerAdapter.ViewHolder> {
    private final ThemedActivity activity;
    private final View.OnClickListener listener;
    private final IconPackManager manager;

    public IconsRecyclerAdapter(ThemedActivity activity,
                                View.OnClickListener listener) {
        this.activity = activity;
        this.listener = listener;
        this.manager = IconPackManager.from(activity);
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {
        private final AdaptiveIconView icon;
        private final TextView label;
        private final TextView count;

        public ViewHolder(View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.icon);
            label = itemView.findViewById(R.id.label);
            count = itemView.findViewById(R.id.count);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        if (position < getItemCount() - 1) {
            IconPackManager.IconPack pack = manager.getPack(position);

            viewHolder.label.setText(pack.getLabel());
            viewHolder.icon.setIcon(pack.getIcon());

            viewHolder.count.setVisibility(View.VISIBLE);
            viewHolder.count.setText(R.string.calculating);

            CompletableFuture<Integer> future = pack.getComponentCount();
            future.thenAccept(integer ->
                    UiUtils.runOnUIThread(() ->
                            viewHolder.count.setText(String.format("%,d", integer) + " " +
                                    activity.getResources().getString(R.string.components))));

            viewHolder.itemView.setOnClickListener(v -> {
                manager.setActiveIconPack(pack);

                if (listener != null) {
                    listener.onClick(v);
                }
            });
        } else {
            viewHolder.label.setText(R.string.default_text);
            viewHolder.icon.setIcon(AppCompatResources.getDrawable(activity, R.mipmap.ic_launcher));

            viewHolder.count.setVisibility(View.GONE);

            viewHolder.itemView.setOnClickListener(v -> {
                manager.setActiveIconPack(null);

                if (listener != null) {
                    listener.onClick(v);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return manager.getCount() + 1;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup container, int viewType) {
        return new ViewHolder(LayoutInflater.from(activity)
                .inflate(R.layout.icon_item, container, false));
    }
}