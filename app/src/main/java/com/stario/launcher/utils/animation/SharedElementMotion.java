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

package com.stario.launcher.utils.animation;

import android.graphics.Path;
import android.transition.PathMotion;

import androidx.annotation.NonNull;

public final class SharedElementMotion extends PathMotion {

    @NonNull
    @Override
    public Path getPath(float startX, float startY, float endX, float endY) {
        Path path = new Path();
        path.moveTo(startX, startY);

        path.cubicTo(startX + endX * ((float) Math.random() * 0.5f),
                startY + endY * ((float) Math.random() * 0.5f),
                endX + startX * ((float) Math.random() * 0.5f),
                endY + startY * ((float) Math.random() * 0.5f),
                endX, endY);

        return path;
    }
}