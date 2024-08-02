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

package com.stario.launcher.ui.glance;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;

import com.stario.launcher.ui.measurements.Measurements;

import carbon.widget.ConstraintLayout;

@SuppressLint("AppCompatCustomView")
public class GlanceConstraintLayout extends ConstraintLayout {
    public final float RADIUS_AUTO = -1;
    private float minRadius;
    private float maxRadius;
    private float fraction;
    private Path path;

    public GlanceConstraintLayout(Context context) {
        super(context);

        init();
    }

    public GlanceConstraintLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public GlanceConstraintLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_HARDWARE, null);

        minRadius = Measurements.dpToPx(30);
        maxRadius = RADIUS_AUTO;
        fraction = 1;

        path = new Path();
    }

    public void setMaxRadius(@FloatRange(from = 0f) float radius) {
        maxRadius = radius;

        invalidate();
    }

    @Override
    public void setScaleX(float scaleX) {
    }

    @Override
    public void setScaleY(float scaleY) {
        super.setScaleY(scaleY);

        invalidate();
    }

    @Override
    public void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(clip(canvas));
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(clip(canvas));
    }

    public void setRadiusPercentage(@FloatRange(from = 0f, to = 1f) float radius) {
        this.fraction = radius;

        invalidate();
    }

    public Canvas clip(Canvas canvas) {
        path.reset();
        path.moveTo(0, 0);

        float maxRadius = this.maxRadius == RADIUS_AUTO ? Math.min(getMeasuredHeight(),
                getMeasuredWidth()) * getScaleY() * 0.5f : this.maxRadius;
        float radius = fraction * maxRadius + (1 - fraction) * minRadius;

        path.addArc(0,
                0,
                2 * radius,
                2 * radius / getScaleY(),
                180,
                90);

        path.lineTo(getWidth() - radius, 0);

        path.addArc(getWidth() - 2 * radius,
                0,
                getWidth(),
                2 * radius / getScaleY(),
                270,
                90);

        path.lineTo(getWidth(), getHeight() - radius / getScaleY());

        path.addArc(getWidth() - 2 * radius,
                getHeight() - 2 * radius / getScaleY(),
                getWidth(),
                getHeight(),
                0,
                90);

        path.lineTo(radius, getHeight());
        path.lineTo(getWidth() - radius, 0);

        path.addArc(0,
                getHeight() - 2 * radius / getScaleY(),
                2 * radius,
                getHeight(),
                90,
                90);

        path.lineTo(0, radius / getScaleY());
        path.lineTo(getWidth() - radius, 0);

        canvas.clipPath(path);

        return canvas;
    }
}