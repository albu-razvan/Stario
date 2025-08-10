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

package com.stario.launcher.ui.common.glance;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;

import com.stario.launcher.R;
import com.stario.launcher.ui.Measurements;

import carbon.widget.ConstraintLayout;

@SuppressLint("AppCompatCustomView")
public class GlanceConstraintLayout extends ConstraintLayout {
    public final float RADIUS_AUTO = -1;
    private Paint strokePaint;
    private float strokeWidth;
    private float minRadius;
    private float maxRadius;
    private float fraction;
    private Path clipPath;

    public GlanceConstraintLayout(Context context) {
        super(context);

        init(null, 0);
    }

    public GlanceConstraintLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(attrs, 0);
    }

    public GlanceConstraintLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(attrs, defStyleAttr);
    }

    private void init(AttributeSet attrs, int defStyleAttr) {
        setLayerType(LAYER_TYPE_HARDWARE, null);

        minRadius = Measurements.dpToPx(30);
        maxRadius = RADIUS_AUTO;
        fraction = 1;

        clipPath = new Path();

        strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);

        strokeWidth = 0;
        int strokeColor = Color.TRANSPARENT;

        if (attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.GlanceConstraintLayout,
                    defStyleAttr,
                    0);
            try {
                strokeWidth = a.getDimensionPixelSize(R.styleable.GlanceConstraintLayout_strokeWidth, 0);
                strokeColor = a.getColor(R.styleable.GlanceConstraintLayout_strokeColor, Color.TRANSPARENT);
            } finally {
                a.recycle();
            }
        }

        strokePaint.setStrokeWidth(strokeWidth * 2);
        strokePaint.setColor(strokeColor);
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
    public void draw(@NonNull Canvas canvas) {
        buildClipPath();

        int save = canvas.save();
        canvas.clipPath(clipPath);

        super.draw(canvas);

        if (strokeWidth > 0) {
            canvas.drawPath(clipPath, strokePaint);
        }

        canvas.restoreToCount(save);
    }

    public void setRadiusPercentage(@FloatRange(from = 0f, to = 1f) float radius) {
        this.fraction = radius;
        invalidate();
    }

    private void buildClipPath() {
        clipPath.reset();

        float maxRadius = this.maxRadius == RADIUS_AUTO ? Math.min(getMeasuredHeight(),
                getMeasuredWidth()) * getScaleY() * 0.5f : this.maxRadius;
        float radius = fraction * maxRadius + (1 - fraction) * minRadius;

        radius = Math.min(radius, getWidth() / 2f);
        radius = Math.min(radius, getHeight() / 2f * getScaleY());

        float radiusY = radius / getScaleY();

        clipPath.addRoundRect(
                new RectF(0, 0, getWidth(), getHeight()),
                new float[]{
                        radius, radiusY,
                        radius, radiusY,
                        radius, radiusY,
                        radius, radiusY
                },
                Path.Direction.CW);
    }
}