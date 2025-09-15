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

package com.stario.launcher.activities.settings.dialogs.theme;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.themes.Theme;
import com.stario.launcher.themes.ThemedActivity;

public class ThemeRecyclerAdapter extends RecyclerView.Adapter<ThemeRecyclerAdapter.ViewHolder> {
    private final View.OnClickListener clickListener;
    private final ThemedActivity activity;
    private final Theme[] themes;

    public ThemeRecyclerAdapter(ThemedActivity activity,
                                View.OnClickListener clickListener) {
        this.clickListener = clickListener;
        this.themes = Theme.values();
        this.activity = activity;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView theme;

        public ViewHolder(View itemView) {
            super(itemView);

            theme = itemView.findViewById(R.id.theme);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        Theme theme = themes[position];

        viewHolder.theme.setBackgroundColor(
                activity.getAttributeData(theme,
                        com.google.android.material.R.attr.colorPrimaryContainer)
        );
        viewHolder.theme.setTextColor(
                activity.getAttributeData(theme,
                        com.google.android.material.R.attr.colorOnPrimaryContainer)
        );
        viewHolder.theme.setText(theme.getDisplayName());

        viewHolder.itemView.setOnClickListener(v -> {
            activity.getApplicationContext()
                    .getSharedPreferences(Entry.THEME).edit()
                    .putString(ThemedActivity.THEME, theme.toString())
                    .apply();

            if (clickListener != null) {
                clickListener.onClick(v);
            }
        });
    }

    @Override
    public int getItemCount() {
        return themes.length;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup container, int viewType) {
        return new ViewHolder(LayoutInflater.from(activity)
                .inflate(R.layout.theme_item, container, false));
    }
}