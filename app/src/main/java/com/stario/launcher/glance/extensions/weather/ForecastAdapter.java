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

package com.stario.launcher.glance.extensions.weather;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.utils.Utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ViewHolder> {
    private static final int FORECAST_SIZE = 12;
    private final ArrayList<Weather.Data> data;
    private final int indexToStart;
    private final SharedPreferences preferences;

    public ForecastAdapter(ThemedActivity activity, ArrayList<Weather.Data> data, int indexToStart) {
        this.preferences = activity.getSharedPreferences(Entry.STARIO);
        this.data = data;
        this.indexToStart = indexToStart;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public static final DateFormat DATE_FORMAT = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
        private final TextView time;
        private final ImageView icon;
        private final TextView temperature;

        @SuppressLint("ClickableViewAccessibility")
        public ViewHolder(View itemView) {
            super(itemView);

            time = itemView.findViewById(R.id.time);
            icon = itemView.findViewById(R.id.icon);
            temperature = itemView.findViewById(R.id.temperature);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int index) {
        Weather.Data data = this.data.get(index + indexToStart);

        viewHolder.time.setText(ViewHolder.DATE_FORMAT.format(data.date));
        viewHolder.icon.setImageResource(Weather.getIcon(data.iconCode));

        if (preferences.getBoolean(Weather.IMPERIAL_KEY, false)) {
            viewHolder.temperature.setText((int) Math.round(Utils.toFahrenheit(data.temperature)) + "°");
        } else {
            viewHolder.temperature.setText((int) Math.round(data.temperature) + "°");
        }
    }

    @Override
    public int getItemCount() {
        return indexToStart < 0 ? 0 : Math.min(data.size() - indexToStart, FORECAST_SIZE);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup container, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(container.getContext());

        return new ViewHolder(inflater.inflate(R.layout.forecast_item, container, false));
    }
}