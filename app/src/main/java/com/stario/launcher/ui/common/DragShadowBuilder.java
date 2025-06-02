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

package com.stario.launcher.ui.common;

import android.graphics.Canvas;
import android.graphics.Point;
import android.view.View;

import androidx.annotation.NonNull;

import com.stario.launcher.utils.ImageUtils;

public class DragShadowBuilder extends View.DragShadowBuilder {
    private final Point touchPoint;
    private final View view;

    public DragShadowBuilder(View view, Point touchPoint) {
        super(view);

        this.view = view;
        this.touchPoint = touchPoint;
    }

    @Override
    public void onDrawShadow(@NonNull Canvas canvas) {
        super.onDrawShadow(canvas);

        canvas.drawBitmap(ImageUtils.toBitmap(view), 0, 0, null);
    }

    @Override
    public void onProvideShadowMetrics(Point shadowSize, Point touchPoint) {
        shadowSize.set(view.getWidth(), view.getHeight());
        touchPoint.set(this.touchPoint.x, this.touchPoint.y);
    }
}
