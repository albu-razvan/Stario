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

package com.stario.launcher.sheet;

import android.view.View;

import androidx.annotation.NonNull;

public enum SheetType {
    TOP_SHEET("com.stario.launcher.TOP_SHEET", View.SCROLL_AXIS_VERTICAL),
    BOTTOM_SHEET("com.stario.launcher.BOTTOM_SHEET", View.SCROLL_AXIS_VERTICAL),
    LEFT_SHEET("com.stario.launcher.LEFT_SHEET", View.SCROLL_AXIS_HORIZONTAL),
    RIGHT_SHEET("com.stario.launcher.RIGHT_SHEET", View.SCROLL_AXIS_HORIZONTAL);

    private final String stringType;
    private final int axes;

    SheetType(String stringType, int axes) {
        this.stringType = stringType;
        this.axes = axes;
    }

    public int getAxes() {
        return axes;
    }

    @NonNull
    @Override
    public String toString() {
        return stringType;
    }
}
