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

package com.stario.launcher.ui.icons;

public enum PathCornerTreatmentAlgorithm {
    REGULAR,
    SQUIRCLE;

    public static final String PATH_ALGORITHM_ENTRY = "com.stario.PATH_ALGORITHM";
    public static final int DEFAULT_PATH_ALGORITHM_ENTRY = REGULAR.ordinal();

    public static PathCornerTreatmentAlgorithm fromIdentifier(int identifier) {
        if (identifier == 1) {
            return SQUIRCLE;
        } else {
            return REGULAR;
        }
    }
}
