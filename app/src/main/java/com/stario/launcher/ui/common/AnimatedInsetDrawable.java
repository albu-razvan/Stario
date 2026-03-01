/*
 * Copyright (C) 2026 Răzvan Albu
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
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AnimatedInsetDrawable extends Drawable {

    private final Drawable inner;

    private int left, top, right, bottom;

    public AnimatedInsetDrawable(Drawable inner) {
        this.inner = inner;
    }

    public void setInsets(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;

        updateInnerBounds(getBounds());
        invalidateSelf();
    }

    private void updateInnerBounds(Rect bounds) {
        inner.setBounds(
                bounds.left + left,
                bounds.top + top,
                bounds.right - right,
                bounds.bottom - bottom
        );
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        updateInnerBounds(bounds);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        inner.draw(canvas);
    }

    @Override
    public void setAlpha(int alpha) {
        inner.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        inner.setColorFilter(colorFilter);
    }

    /** @noinspection deprecation*/
    @Override
    public int getOpacity() {
        return inner.getOpacity();
    }
}