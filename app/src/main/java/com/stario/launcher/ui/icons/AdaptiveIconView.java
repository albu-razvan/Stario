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

package com.stario.launcher.ui.icons;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stario.launcher.R;
import com.stario.launcher.ui.measurements.Measurements;
import com.stario.launcher.utils.objects.ObjectInvalidateDelegate;

public class AdaptiveIconView extends View {
    public static final float MAX_SCALE = 1.15f;
    private ObjectInvalidateDelegate<Float> radius;
    private ObjectInvalidateDelegate<Drawable> icon;
    private Path path;
    private boolean sizeRestricted;

    public AdaptiveIconView(Context context) {
        super(context);

        init(context, null);
    }

    public AdaptiveIconView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init(context, attrs);
    }

    public AdaptiveIconView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        if (attrs != null) {
            TypedArray attributes = context.getApplicationContext().obtainStyledAttributes(attrs, R.styleable.AdaptiveIconView);

            sizeRestricted = attributes.getBoolean(R.styleable.AdaptiveIconView_sizeRestricted, true);

            attributes.recycle();
        } else {
            sizeRestricted = true;
        }

        this.path = new Path();
        this.icon = new ObjectInvalidateDelegate<>(this);
        this.radius = new ObjectInvalidateDelegate<>(this, 1f);

        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxIconSize = Measurements.getIconSize();
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);

        if (measuredHeight > 0 && measuredWidth > 0) {
            if (measuredWidth != measuredHeight ||
                    (sizeRestricted && measuredHeight > maxIconSize)) {
                int size = Math.min(measuredWidth, measuredHeight);

                if (sizeRestricted) {
                    size = Math.min(maxIconSize, size);
                }

                widthMeasureSpec = heightMeasureSpec =
                        MeasureSpec.makeMeasureSpec(size, MeasureSpec.getMode(widthMeasureSpec));
                measuredWidth = measuredHeight = size;
            }

            Drawable icon = this.icon.getValue();

            if (icon != null) {
                icon.setBounds(0, 0, measuredWidth, measuredHeight);
            }

            updateClipPath(measuredWidth, measuredHeight);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void updateClipPath(int width, int height) {
        float cornerRadius = radius.getValue() * width / 2f;

        path.reset();
        path.addRoundRect(0, 0, width, height,
                cornerRadius, cornerRadius, Path.Direction.CW);
        path.close();
    }

    public Drawable getIcon() {
        return icon.getValue();
    }

    public void setIcon(Drawable icon) {
        if (icon != null && icon.getConstantState() != null) {
            Drawable constantStateIcon = icon.getConstantState().newDrawable();

            constantStateIcon.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());

            this.icon.setValue(constantStateIcon);
        } else {
            this.icon.setValue(null);
        }
    }

    @Override
    public void setClipBounds(Rect clipBounds) {
        if (clipBounds != null && icon.getValue() != null) {
            icon.getValue().setBounds(clipBounds);

            updateClipPath(clipBounds.width(), clipBounds.height());
        } else {
            icon.getValue().setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());

            updateClipPath(getMeasuredWidth(), getMeasuredHeight());
        }

        super.setClipBounds(clipBounds);

        invalidate();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        int save = canvas.save();

        canvas.clipPath(path);

        super.draw(canvas);
        canvas.restoreToCount(save);
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (icon.getValue() != null) {
            Drawable icon = this.icon.getValue();

            if (icon instanceof AdaptiveIconDrawable) {
                AdaptiveIconDrawable adaptiveIconDrawable = (AdaptiveIconDrawable) icon;

                adaptiveIconDrawable.getBackground().draw(canvas);
                adaptiveIconDrawable.getForeground().draw(canvas);
            } else {
                icon.draw(canvas);
            }
        }
    }
}
