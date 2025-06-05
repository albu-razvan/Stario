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

package com.stario.launcher.themes;

import androidx.annotation.NonNull;

import com.stario.launcher.R;

public enum Theme {
    THEME_DYNAMIC("com.stario.THEME_DYNAMIC", R.string.dynamic,
            R.style.Theme_Light_Dynamic, R.style.Theme_Dark_Dynamic),
    THEME_RED("com.stario.THEME_RED", R.string.red,
            R.style.Theme_Light_Red, R.style.Theme_Dark_Red),
    THEME_ORANGE("com.stario.THEME_ORANGE", R.string.orange,
            R.style.Theme_Light_Orange, R.style.Theme_Dark_Orange),
    THEME_YELLOW("com.stario.THEME_YELLOW", R.string.yellow,
            R.style.Theme_Light_Yellow, R.style.Theme_Dark_Yellow),
    THEME_LIME("com.stario.THEME_LIME", R.string.lime,
            R.style.Theme_Light_Lime, R.style.Theme_Dark_Lime),
    THEME_GREEN("com.stario.THEME_GREEN", R.string.green,
            R.style.Theme_Light_Green, R.style.Theme_Dark_Green),
    THEME_TURQUOISE("com.stario.THEME_TURQUOISE", R.string.turquoise,
            R.style.Theme_Light_Turquoise, R.style.Theme_Dark_Turquoise),
    THEME_CYAN("com.stario.THEME_CYAN", R.string.cyan,
            R.style.Theme_Light_Cyan, R.style.Theme_Dark_Cyan),
    THEME_BLUE("com.stario.THEME_BLUE", R.string.blue,
            R.style.Theme_Light_Blue, R.style.Theme_Dark_Blue),
    THEME_PURPLE("com.stario.THEME_PURPLE", R.string.purple,
            R.style.Theme_Light_Purple, R.style.Theme_Dark_Purple),
    THEME_PINK("com.stario.THEME_PINK", R.string.pink,
            R.style.Theme_Light_Pink, R.style.Theme_Dark_Pink);

    private final int displayNameResource;
    private final String themeIdentifier;
    private final int lightRes;
    private final int darkRes;

    Theme(String themeIdentifier, int displayNameResource, int lightRes, int darkRes) {
        this.displayNameResource = displayNameResource;
        this.themeIdentifier = themeIdentifier;
        this.lightRes = lightRes;
        this.darkRes = darkRes;
    }

    // Defaults to THEME_BLUE
    public static Theme from(String theme) {
        if (theme.equals(THEME_DYNAMIC.themeIdentifier)) {
            return THEME_DYNAMIC;
        } else if (theme.equals(THEME_RED.themeIdentifier)) {
            return THEME_RED;
        } else if (theme.equals(THEME_ORANGE.themeIdentifier)) {
            return THEME_ORANGE;
        } else if (theme.equals(THEME_YELLOW.themeIdentifier)) {
            return THEME_YELLOW;
        } else if (theme.equals(THEME_LIME.themeIdentifier)) {
            return THEME_LIME;
        } else if (theme.equals(THEME_GREEN.themeIdentifier)) {
            return THEME_GREEN;
        } else if (theme.equals(THEME_TURQUOISE.themeIdentifier)) {
            return THEME_TURQUOISE;
        } else if (theme.equals(THEME_CYAN.themeIdentifier)) {
            return THEME_CYAN;
        } else if (theme.equals(THEME_PURPLE.themeIdentifier)) {
            return THEME_PURPLE;
        } else if (theme.equals(THEME_PINK.themeIdentifier)) {
            return THEME_PINK;
        } else {
            return THEME_BLUE;
        }
    }

    public int getLightResourceID() {
        return lightRes;
    }

    public int getDarkResourceID() {
        return darkRes;
    }

    public int getDisplayName() {
        return displayNameResource;
    }

    @NonNull
    @Override
    public String toString() {
        return themeIdentifier;
    }
}
