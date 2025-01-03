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

package com.stario.launcher.activities.settings.dialogs.license;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;

public class LicensesRecyclerAdapter extends RecyclerView.Adapter<LicensesRecyclerAdapter.ViewHolder> {
    private final String[][] licenses = {{"Android", "The Android Open Source Project", "Apache 2.0"},
            {"Android Fading Edge Layout", "Yang Bo", "Apache 2.0"},
            {"Android Jetpack", "The Android Open Source Project", "Apache 2.0"},
            {"Android Support Library", "The Android Open Source Project", "Apache 2.0"},
            {"Carbon", "Zileoni", "Apache 2.0"},
            {"Date Parser", "sisyphsu", "MIT"},
            {"Glide", "Meta", "MIT"},
            {"Glide Transformations", "Daichi Furiya", "Apache 2.0"},
            {"Hidden Api Refine Plugin", "RikkaW", "MIT"},
            {"Jsoup", "Jonathan Hedley", "MIT"},
            {"Material Components for Android", "The Android Open Source Project", "Apache 2.0"},
            {"Material Design", "The Android Open Source Project", "Apache 2.0"},
            {"OkHttp", "Square", "Apache 2.0"},
            {"RecyclerView Fast Scroller", "Quiph", "Apache 2.0"},
            {"RSS Parser", "Marco Gomiero", "Apache 2.0"},
            {"Smart Tab Layout", "ogaclejapan", "Apache 2.0"},
            {"Squiggly Slider", "Saket Narayan", "Apache 2.0"},
            {"Sunrise Sunset Calculator", "Mike Reedell", "Apache 2.0"},
            {"Weather Data", "MET Norway", "NLOD 2.0 and CC 4.0"},
            {"Woodstox", "FasterXML", "Apache 2.0"},
    };

    private final LayoutInflater inflater;

    public LicensesRecyclerAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView user;
        private final TextView license;

        public ViewHolder(View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.name);
            user = itemView.findViewById(R.id.user);
            license = itemView.findViewById(R.id.license);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(LicensesRecyclerAdapter.ViewHolder viewHolder, int position) {
        viewHolder.name.setText(licenses[position][0]);
        viewHolder.user.setText(licenses[position][1]);
        viewHolder.license.setText(licenses[position][2] + " " +
                viewHolder.itemView.getResources().getString(R.string.license));
    }

    @Override
    public int getItemCount() {
        return licenses.length;
    }

    @NonNull
    @Override
    public LicensesRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup container, int viewType) {
        return new ViewHolder(inflater.inflate(R.layout.licenses_item, container, false));
    }
}