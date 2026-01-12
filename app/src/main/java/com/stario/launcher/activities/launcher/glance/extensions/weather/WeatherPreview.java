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

package com.stario.launcher.activities.launcher.glance.extensions.weather;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.stario.launcher.R;
import com.stario.launcher.activities.launcher.glance.GlanceViewExtension;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.utils.Utils;

final class WeatherPreview implements GlanceViewExtension {
    private static final String CELSIUS = "°C";
    private static final String FAHRENHEIT = "°F";

    private SharedPreferences preferences;
    private boolean hasTemperature;
    private TextView temperature;
    private boolean hasIcon;
    private ImageView icon;
    private View root;

    public WeatherPreview() {
        this.hasTemperature = false;
        this.hasIcon = false;
    }

    @Override
    public View inflate(ThemedActivity activity, LinearLayout container) {
        root = activity.getLayoutInflater()
                .inflate(R.layout.weather_preview, container, false);

        preferences = activity.getApplicationContext().getSettings();

        icon = root.findViewById(R.id.icon);
        temperature = root.findViewById(R.id.temperature);

        root.setBackground(ResourcesCompat.getDrawable(activity.getResources(),
                R.drawable.weather_background, activity.getTheme(true)));
        temperature.setTextColor(activity.getAttributeData(
                com.google.android.material.R.attr.colorOnPrimaryContainer, true)
        );

        return root;
    }

    @SuppressLint("SetTextI18n")
    void update(Weather.Data data) {
        if (data == null) {
            hasIcon = false;
            hasTemperature = false;

            update();

            return;
        }

        if (!Double.isNaN(data.temperature)) {
            if (preferences.getBoolean(Weather.IMPERIAL_KEY, false)) {
                this.temperature.setText((int) Math.round(
                        Utils.toFahrenheit(data.temperature)) + FAHRENHEIT);
            } else {
                this.temperature.setText((int) Math.round(data.temperature) + CELSIUS);
            }

            hasTemperature = true;
        } else {
            hasTemperature = false;
        }

        if (data.iconCode != null) {
            icon.setImageResource(Weather.getIcon(data.iconCode));
            hasIcon = true;
        } else {
            hasIcon = false;
        }

        update();
    }

    @Override
    public void update() {
        if (hasIcon && hasTemperature) {
            root.setVisibility(View.VISIBLE);
        } else {
            root.setVisibility(View.GONE);
        }
    }
}
