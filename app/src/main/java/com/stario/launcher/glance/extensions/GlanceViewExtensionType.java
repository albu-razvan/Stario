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

package com.stario.launcher.glance.extensions;

import com.stario.launcher.glance.extensions.calendar.Calendar;
import com.stario.launcher.glance.extensions.media.MediaPreview;
import com.stario.launcher.glance.extensions.weather.WeatherPreview;

public enum GlanceViewExtensionType {
    MEDIA_PLAYER_PREVIEW,
    WEATHER_PREVIEW,
    CALENDAR;

    public static GlanceViewExtension forType(GlanceViewExtensionType type) {
        GlanceViewExtension item = null;

        if (type == MEDIA_PLAYER_PREVIEW) {
            item = new MediaPreview();
        } else if (type == WEATHER_PREVIEW) {
            item = new WeatherPreview();
        } else if (type == CALENDAR) {
            item = new Calendar();
        }

        return item;
    }
}
