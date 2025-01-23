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

package com.stario.launcher.ui.popup;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;

import java.util.ArrayList;

import carbon.widget.ImageView;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    private final Activity activity;
    private final PopupWindow popupWindow;
    private final ArrayList<PopupMenu.Item> items;

    public RecyclerAdapter(PopupWindow popupWindow, Activity activity) {
        this.popupWindow = popupWindow;
        this.activity = activity;
        this.items = new ArrayList<>();
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView icon;
        private final TextView label;

        @SuppressLint("ClickableViewAccessibility")
        public ViewHolder(ViewGroup itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.icon);
            label = itemView.findViewById(R.id.label);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int index) {
        PopupMenu.Item holder = items.get(index);

        viewHolder.icon.setBackground(holder.icon);
        viewHolder.label.setText(holder.label);
        viewHolder.itemView.setOnClickListener(v -> {
            holder.listener.onClick(v);

            popupWindow.dismiss();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup container, int viewType) {
        return new ViewHolder((ViewGroup) LayoutInflater.from(activity)
                .inflate(R.layout.popup_item, container, false));
    }

    public void add(PopupMenu.Item item) {
        items.add(item);
    }
}
