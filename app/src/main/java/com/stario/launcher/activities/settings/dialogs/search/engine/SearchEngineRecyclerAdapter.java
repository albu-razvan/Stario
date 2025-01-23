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

package com.stario.launcher.activities.settings.dialogs.search.engine;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.sheet.drawer.search.SearchEngine;
import com.stario.launcher.themes.ThemedActivity;

public class SearchEngineRecyclerAdapter extends RecyclerView.Adapter<SearchEngineRecyclerAdapter.ViewHolder> {
    private final ThemedActivity activity;
    private final View.OnClickListener listener;

    public SearchEngineRecyclerAdapter(ThemedActivity activity,
                                       View.OnClickListener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView icon;
        private final TextView label;
        private final TextView url;

        public ViewHolder(View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.icon);
            label = itemView.findViewById(R.id.label);
            url = itemView.findViewById(R.id.url);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        SearchEngine engine = SearchEngine.values()[position];

        viewHolder.icon.setImageDrawable(engine.getDrawable(activity));
        viewHolder.label.setText(engine.toString());
        viewHolder.url.setText(engine.getUrl());

        viewHolder.itemView.setOnClickListener(v -> {
            SearchEngine.setEngine(activity, engine);

            if (listener != null) {
                listener.onClick(v);
            }
        });
    }

    @Override
    public int getItemCount() {
        return SearchEngine.values().length;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup container, int viewType) {
        return new ViewHolder(LayoutInflater.from(activity)
                .inflate(R.layout.engine_item, container, false));
    }
}