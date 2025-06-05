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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.stario.launcher.R;

// modification of https://github.com/bosphere/Android-FadingEdgeLayout to support corner fade rounding
public class FadingEdgeLayout extends FrameLayout {
    private static final int DEFAULT_GRADIENT_SIZE_DP = 80;

    public static final int FADE_EDGE_TOP = 1;
    public static final int FADE_EDGE_BOTTOM = 2;
    public static final int FADE_EDGE_LEFT = 4;
    public static final int FADE_EDGE_RIGHT = 8;

    private static final int DIRTY_FLAG_TOP = 1;
    private static final int DIRTY_FLAG_BOTTOM = 2;
    private static final int DIRTY_FLAG_LEFT = 4;
    private static final int DIRTY_FLAG_RIGHT = 8;

    private static final int[] FADE_COLORS = new int[]{Color.TRANSPARENT, Color.BLACK};
    private static final int[] FADE_COLORS_REVERSE = new int[]{Color.BLACK, Color.TRANSPARENT};

    private boolean fadeTop, fadeBottom, fadeLeft, fadeRight;
    private int gradientSizeTop, gradientSizeBottom, gradientSizeLeft, gradientSizeRight;
    private Paint gradientPaintTop, gradientPaintBottom, gradientPaintLeft, gradientPaintRight;
    private Paint cornerPaintTopLeft, cornerPaintTopRight, cornerPaintBottomLeft, cornerPaintBottomRight;
    private Rect gradientRectTop, gradientRectBottom, gradientRectLeft, gradientRectRight;
    private int gradientDirtyFlags;

    public FadingEdgeLayout(Context context) {
        super(context);

        init(null);
    }

    public FadingEdgeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(attrs);
    }

    public FadingEdgeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(attrs);
    }

    private void init(AttributeSet attrs) {
        int defaultSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_GRADIENT_SIZE_DP,
                getResources().getDisplayMetrics());

        if (attrs != null) {
            TypedArray arr = getContext().obtainStyledAttributes(attrs, R.styleable.FadingEdgeLayout, 0, 0);
            int flags = arr.getInt(R.styleable.FadingEdgeLayout_fel_edge, 0);

            fadeTop = (flags & FADE_EDGE_TOP) == FADE_EDGE_TOP;
            fadeBottom = (flags & FADE_EDGE_BOTTOM) == FADE_EDGE_BOTTOM;
            fadeLeft = (flags & FADE_EDGE_LEFT) == FADE_EDGE_LEFT;
            fadeRight = (flags & FADE_EDGE_RIGHT) == FADE_EDGE_RIGHT;

            gradientSizeTop = arr.getDimensionPixelSize(R.styleable.FadingEdgeLayout_fel_size_top, defaultSize);
            gradientSizeBottom = arr.getDimensionPixelSize(R.styleable.FadingEdgeLayout_fel_size_bottom, defaultSize);
            gradientSizeLeft = arr.getDimensionPixelSize(R.styleable.FadingEdgeLayout_fel_size_left, defaultSize);
            gradientSizeRight = arr.getDimensionPixelSize(R.styleable.FadingEdgeLayout_fel_size_right, defaultSize);

            if (fadeTop && gradientSizeTop > 0) gradientDirtyFlags |= DIRTY_FLAG_TOP;
            if (fadeLeft && gradientSizeLeft > 0) gradientDirtyFlags |= DIRTY_FLAG_LEFT;
            if (fadeBottom && gradientSizeBottom > 0) gradientDirtyFlags |= DIRTY_FLAG_BOTTOM;
            if (fadeRight && gradientSizeRight > 0) gradientDirtyFlags |= DIRTY_FLAG_RIGHT;

            arr.recycle();
        } else {
            gradientSizeTop = gradientSizeBottom = gradientSizeLeft = gradientSizeRight = defaultSize;
        }

        PorterDuffXfermode mode = new PorterDuffXfermode(PorterDuff.Mode.DST_IN);

        gradientPaintTop = createFadePaint(mode);
        gradientPaintBottom = createFadePaint(mode);
        gradientPaintLeft = createFadePaint(mode);
        gradientPaintRight = createFadePaint(mode);

        cornerPaintTopLeft = createFadePaint(mode);
        cornerPaintTopRight = createFadePaint(mode);
        cornerPaintBottomLeft = createFadePaint(mode);
        cornerPaintBottomRight = createFadePaint(mode);

        gradientRectTop = new Rect();
        gradientRectLeft = new Rect();
        gradientRectBottom = new Rect();
        gradientRectRight = new Rect();
    }

    private Paint createFadePaint(PorterDuffXfermode mode) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setXfermode(mode);
        return paint;
    }

    public void setFadeSizes(int top, int left, int bottom, int right) {
        if (gradientSizeTop != top) {
            gradientSizeTop = top;
            gradientDirtyFlags |= DIRTY_FLAG_TOP;
        }
        if (gradientSizeLeft != left) {
            gradientSizeLeft = left;
            gradientDirtyFlags |= DIRTY_FLAG_LEFT;
        }
        if (gradientSizeBottom != bottom) {
            gradientSizeBottom = bottom;
            gradientDirtyFlags |= DIRTY_FLAG_BOTTOM;
        }
        if (gradientSizeRight != right) {
            gradientSizeRight = right;
            gradientDirtyFlags |= DIRTY_FLAG_RIGHT;
        }
        if (gradientDirtyFlags != 0) invalidate();
    }

    public void setFadeEdges(boolean fadeTop, boolean fadeLeft, boolean fadeBottom, boolean fadeRight) {
        if (this.fadeTop != fadeTop) {
            this.fadeTop = fadeTop;
            gradientDirtyFlags |= DIRTY_FLAG_TOP;
        }
        if (this.fadeLeft != fadeLeft) {
            this.fadeLeft = fadeLeft;
            gradientDirtyFlags |= DIRTY_FLAG_LEFT;
        }
        if (this.fadeBottom != fadeBottom) {
            this.fadeBottom = fadeBottom;
            gradientDirtyFlags |= DIRTY_FLAG_BOTTOM;
        }
        if (this.fadeRight != fadeRight) {
            this.fadeRight = fadeRight;
            gradientDirtyFlags |= DIRTY_FLAG_RIGHT;
        }
        if (gradientDirtyFlags != 0) invalidate();
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        if (getPaddingLeft() != left) gradientDirtyFlags |= DIRTY_FLAG_LEFT;
        if (getPaddingTop() != top) gradientDirtyFlags |= DIRTY_FLAG_TOP;
        if (getPaddingRight() != right) gradientDirtyFlags |= DIRTY_FLAG_RIGHT;
        if (getPaddingBottom() != bottom) gradientDirtyFlags |= DIRTY_FLAG_BOTTOM;
        super.setPadding(left, top, right, bottom);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw) {
            gradientDirtyFlags |= DIRTY_FLAG_LEFT | DIRTY_FLAG_RIGHT;
        }
        if (h != oldh) {
            gradientDirtyFlags |= DIRTY_FLAG_TOP | DIRTY_FLAG_BOTTOM;
        }
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        int width = getWidth(), height = getHeight();
        boolean fadeAnyEdge = fadeTop || fadeBottom || fadeLeft || fadeRight;
        if (getVisibility() == GONE || width == 0 || height == 0 || !fadeAnyEdge) {
            super.dispatchDraw(canvas);
            return;
        }

        if ((gradientDirtyFlags & DIRTY_FLAG_TOP) != 0) {
            gradientDirtyFlags &= ~DIRTY_FLAG_TOP;
            initTopGradient();
        }
        if ((gradientDirtyFlags & DIRTY_FLAG_LEFT) != 0) {
            gradientDirtyFlags &= ~DIRTY_FLAG_LEFT;
            initLeftGradient();
        }
        if ((gradientDirtyFlags & DIRTY_FLAG_BOTTOM) != 0) {
            gradientDirtyFlags &= ~DIRTY_FLAG_BOTTOM;
            initBottomGradient();
        }
        if ((gradientDirtyFlags & DIRTY_FLAG_RIGHT) != 0) {
            gradientDirtyFlags &= ~DIRTY_FLAG_RIGHT;
            initRightGradient();
        }

        int count = canvas.saveLayer(0.0f, 0.0f, width, height, null, Canvas.ALL_SAVE_FLAG);
        super.dispatchDraw(canvas);

        if (fadeTop && gradientSizeTop > 0) canvas.drawRect(gradientRectTop, gradientPaintTop);
        if (fadeBottom && gradientSizeBottom > 0)
            canvas.drawRect(gradientRectBottom, gradientPaintBottom);
        if (fadeLeft && gradientSizeLeft > 0) canvas.drawRect(gradientRectLeft, gradientPaintLeft);
        if (fadeRight && gradientSizeRight > 0)
            canvas.drawRect(gradientRectRight, gradientPaintRight);

        // Smooth corner blending
        if (fadeTop && fadeLeft) {
            drawCornerBlend(canvas, getPaddingLeft(), getPaddingTop(), Math.min(gradientSizeTop, gradientSizeLeft), Corner.TOP_LEFT, cornerPaintTopLeft);
        }
        if (fadeTop && fadeRight) {
            drawCornerBlend(canvas, getWidth() - getPaddingRight(), getPaddingTop(), Math.min(gradientSizeTop, gradientSizeRight), Corner.TOP_RIGHT, cornerPaintTopRight);
        }
        if (fadeBottom && fadeLeft) {
            drawCornerBlend(canvas, getPaddingLeft(), getHeight() - getPaddingBottom(), Math.min(gradientSizeBottom, gradientSizeLeft), Corner.BOTTOM_LEFT, cornerPaintBottomLeft);
        }
        if (fadeBottom && fadeRight) {
            drawCornerBlend(canvas, getWidth() - getPaddingRight(), getHeight() - getPaddingBottom(), Math.min(gradientSizeBottom, gradientSizeRight), Corner.BOTTOM_RIGHT, cornerPaintBottomRight);
        }

        canvas.restoreToCount(count);
    }

    private void drawCornerBlend(Canvas canvas, int cx, int cy, int radius, Corner corner, Paint paint) {
        Shader shader = new RadialGradient(
                cx, cy, radius,
                FADE_COLORS_REVERSE, // from black to transparent
                null,
                Shader.TileMode.CLAMP
        );
        paint.setShader(shader);

        Path path = new Path();
        switch (corner) {
            case TOP_LEFT:
                path.moveTo(cx, cy);
                path.lineTo(cx, cy + radius);
                path.arcTo(new RectF(cx, cy, cx + 2 * radius, cy + 2 * radius), 180, 90);
                path.lineTo(cx + radius, cy);
                path.close();
                break;
            case TOP_RIGHT:
                path.moveTo(cx, cy);
                path.lineTo(cx - radius, cy);
                path.arcTo(new RectF(cx - 2 * radius, cy, cx, cy + 2 * radius), 270, 90);
                path.lineTo(cx, cy + radius);
                path.close();
                break;
            case BOTTOM_LEFT:
                path.moveTo(cx, cy);
                path.lineTo(cx + radius, cy);
                path.arcTo(new RectF(cx, cy - 2 * radius, cx + 2 * radius, cy), 90, 90);
                path.lineTo(cx, cy - radius);
                path.close();
                break;
            case BOTTOM_RIGHT:
                path.moveTo(cx, cy);
                path.lineTo(cx - radius, cy);
                path.arcTo(new RectF(cx - 2 * radius, cy - 2 * radius, cx, cy), 0, 90);
                path.lineTo(cx, cy - radius);
                path.close();
                break;
        }

        canvas.save();
        canvas.clipPath(path);
        canvas.drawCircle(cx, cy, radius, paint);
        canvas.restore();
    }


    private void initTopGradient() {
        int actualHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        int size = Math.min(gradientSizeTop, actualHeight);
        int l = getPaddingLeft();
        int t = getPaddingTop();
        int r = getWidth() - getPaddingRight();
        int b = t + size;
        gradientRectTop.set(l, t, r, b);
        LinearGradient gradient = new LinearGradient(l, t, l, b, FADE_COLORS, null, Shader.TileMode.CLAMP);
        gradientPaintTop.setShader(gradient);
    }

    private void initLeftGradient() {
        int actualWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int size = Math.min(gradientSizeLeft, actualWidth);
        int l = getPaddingLeft();
        int t = getPaddingTop();
        int r = l + size;
        int b = getHeight() - getPaddingBottom();
        gradientRectLeft.set(l, t, r, b);
        LinearGradient gradient = new LinearGradient(l, t, r, t, FADE_COLORS, null, Shader.TileMode.CLAMP);
        gradientPaintLeft.setShader(gradient);
    }

    private void initBottomGradient() {
        int actualHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        int size = Math.min(gradientSizeBottom, actualHeight);
        int l = getPaddingLeft();
        int t = getPaddingTop() + actualHeight - size;
        int r = getWidth() - getPaddingRight();
        int b = t + size;
        gradientRectBottom.set(l, t, r, b);
        LinearGradient gradient = new LinearGradient(l, t, l, b, FADE_COLORS_REVERSE, null, Shader.TileMode.CLAMP);
        gradientPaintBottom.setShader(gradient);
    }

    private void initRightGradient() {
        int actualWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int size = Math.min(gradientSizeRight, actualWidth);
        int l = getPaddingLeft() + actualWidth - size;
        int t = getPaddingTop();
        int r = l + size;
        int b = getHeight() - getPaddingBottom();
        gradientRectRight.set(l, t, r, b);
        LinearGradient gradient = new LinearGradient(l, t, r, t, FADE_COLORS_REVERSE, null, Shader.TileMode.CLAMP);
        gradientPaintRight.setShader(gradient);
    }

    private enum Corner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }
}