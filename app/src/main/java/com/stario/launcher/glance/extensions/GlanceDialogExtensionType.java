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

import com.stario.launcher.glance.extensions.media.Media;
import com.stario.launcher.glance.extensions.weather.Weather;

public enum GlanceDialogExtensionType {
    MEDIA_PLAYER,
    WEATHER;

    public static GlanceDialogExtension forType(GlanceDialogExtensionType type) {
        GlanceDialogExtension item = null;

        if (type == MEDIA_PLAYER) {
            item = new Media();
        } else if (type == WEATHER) {
            item = new Weather();
        }

        return item;
    }
}
