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

package com.stario.launcher.glance.extensions.weather;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.stario.launcher.R;
import com.stario.launcher.glance.extensions.GlanceViewExtension;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.utils.Utils;

public final class WeatherPreview implements GlanceViewExtension {
    private static final String CELSIUS = "°C";
    private static final String FAHRENHEIT = "°F";
    private View root;
    private ImageView icon;
    private TextView temperature;
    private boolean enabled;
    private SharedPreferences preferences;

    public WeatherPreview() {
        this.enabled = false;
    }

    @Override
    public View inflate(ThemedActivity activity, LinearLayout container) {
        root = activity.getLayoutInflater()
                .inflate(R.layout.weather_preview, container, false);

        preferences = activity.getSharedPreferences(Entry.WEATHER);

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
    @Override
    public void updateData(Bundle data) {
        String iconCode = data.getString(Weather.ICON_CODE_KEY, null);
        double temperatureValue = data.getDouble(Weather.TEMPERATURE_KEY, Double.NaN);

        if (iconCode != null && !Double.isNaN(temperatureValue)) {
            icon.setImageResource(Weather.getIcon(iconCode));

            if (preferences.getBoolean(Weather.IMPERIAL_KEY, false)) {
                temperature.setText((int) Math.round(Utils.toFahrenheit(temperatureValue)) + FAHRENHEIT);
            } else {
                temperature.setText((int) Math.round(temperatureValue) + CELSIUS);
            }
        }

        enabled = true;

        update();
    }

    @Override
    public void update() {
        if (enabled) {
            root.setVisibility(View.VISIBLE);
        } else {
            root.setVisibility(View.GONE);
        }
    }
}
