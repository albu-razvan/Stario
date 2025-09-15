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
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
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

    private boolean fadeTop;
    private boolean fadeBottom;
    private boolean fadeLeft;
    private boolean fadeRight;

    private int gradientSizeTop;
    private int gradientSizeBottom;
    private int gradientSizeLeft;
    private int gradientSizeRight;
    private boolean rounded;

    private Paint gradientPaintTop;
    private Paint gradientPaintBottom;
    private Paint gradientPaintLeft;
    private Paint gradientPaintRight;

    private Paint cornerPaint;

    private Rect gradientRectTop;
    private Rect gradientRectBottom;
    private Rect gradientRectLeft;
    private Rect gradientRectRight;

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
            TypedArray arr = getContext().obtainStyledAttributes(attrs,
                    R.styleable.FadingEdgeLayout, 0, 0);
            int flags = arr.getInt(R.styleable.FadingEdgeLayout_edge, 0);

            fadeTop = (flags & FADE_EDGE_TOP) == FADE_EDGE_TOP;
            fadeBottom = (flags & FADE_EDGE_BOTTOM) == FADE_EDGE_BOTTOM;
            fadeLeft = (flags & FADE_EDGE_LEFT) == FADE_EDGE_LEFT;
            fadeRight = (flags & FADE_EDGE_RIGHT) == FADE_EDGE_RIGHT;

            gradientSizeTop = arr.getDimensionPixelSize(R.styleable.FadingEdgeLayout_size_top, defaultSize);
            gradientSizeBottom = arr.getDimensionPixelSize(R.styleable.FadingEdgeLayout_size_bottom, defaultSize);
            gradientSizeLeft = arr.getDimensionPixelSize(R.styleable.FadingEdgeLayout_size_left, defaultSize);
            gradientSizeRight = arr.getDimensionPixelSize(R.styleable.FadingEdgeLayout_size_right, defaultSize);
            rounded = arr.getBoolean(R.styleable.FadingEdgeLayout_is_rounded, false);

            if (fadeTop && gradientSizeTop > 0) gradientDirtyFlags |= DIRTY_FLAG_TOP;
            if (fadeLeft && gradientSizeLeft > 0) gradientDirtyFlags |= DIRTY_FLAG_LEFT;
            if (fadeBottom && gradientSizeBottom > 0) gradientDirtyFlags |= DIRTY_FLAG_BOTTOM;
            if (fadeRight && gradientSizeRight > 0) gradientDirtyFlags |= DIRTY_FLAG_RIGHT;

            arr.recycle();
        } else {
            gradientSizeTop = gradientSizeBottom = gradientSizeLeft = gradientSizeRight = defaultSize;
            rounded = false;
        }

        gradientPaintTop = createFadePaint();
        gradientPaintBottom = createFadePaint();
        gradientPaintLeft = createFadePaint();
        gradientPaintRight = createFadePaint();

        cornerPaint = createFadePaint();

        gradientRectTop = new Rect();
        gradientRectLeft = new Rect();
        gradientRectBottom = new Rect();
        gradientRectRight = new Rect();
    }

    private Paint createFadePaint() {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        return paint;
    }

    public boolean isRounded() {
        return rounded;
    }

    public void setRounded(boolean rounded) {
        if (this.rounded != rounded) {
            this.rounded = rounded;

            gradientDirtyFlags = (DIRTY_FLAG_TOP | DIRTY_FLAG_LEFT | DIRTY_FLAG_BOTTOM | DIRTY_FLAG_TOP);
            invalidate();
        }
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
        if (gradientDirtyFlags != 0) {
            invalidate();
        }
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
        if (gradientDirtyFlags != 0) {
            invalidate();
        }
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        if (getPaddingLeft() != left) {
            gradientDirtyFlags |= DIRTY_FLAG_LEFT;
        }
        if (getPaddingTop() != top) {
            gradientDirtyFlags |= DIRTY_FLAG_TOP;
        }
        if (getPaddingRight() != right) {
            gradientDirtyFlags |= DIRTY_FLAG_RIGHT;
        }
        if (getPaddingBottom() != bottom) {
            gradientDirtyFlags |= DIRTY_FLAG_BOTTOM;
        }

        super.setPadding(left, top, right, bottom);
        invalidate();
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

        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (changed) {
            gradientDirtyFlags |= DIRTY_FLAG_TOP | DIRTY_FLAG_BOTTOM | DIRTY_FLAG_LEFT | DIRTY_FLAG_RIGHT;
            invalidate();
        }
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        if (getVisibility() == GONE || width == 0 || height == 0
                || !(fadeTop || fadeBottom || fadeLeft || fadeRight)) {
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

        int count = canvas.saveLayer(0.0f, 0.0f, width, height, null);
        super.dispatchDraw(canvas);

        int radius_tl = 0, radius_tr = 0, radius_bl = 0, radius_br = 0;
        if (rounded) {
            if (fadeTop && fadeLeft) {
                radius_tl = Math.min(gradientSizeTop, gradientSizeLeft);
            }
            if (fadeTop && fadeRight) {
                radius_tr = Math.min(gradientSizeTop, gradientSizeRight);
            }
            if (fadeBottom && fadeLeft) {
                radius_bl = Math.min(gradientSizeBottom, gradientSizeLeft);
            }
            if (fadeBottom && fadeRight) {
                radius_br = Math.min(gradientSizeBottom, gradientSizeRight);
            }
        }

        if (fadeTop && gradientSizeTop > 0) {
            Rect topRect = new Rect(gradientRectTop);
            topRect.left += radius_tl;
            topRect.right -= radius_tr;

            if (topRect.left < topRect.right) {
                canvas.drawRect(topRect, gradientPaintTop);
            }
        }

        if (fadeBottom && gradientSizeBottom > 0) {
            Rect bottomRect = new Rect(gradientRectBottom);
            bottomRect.left += radius_bl;
            bottomRect.right -= radius_br;

            if (bottomRect.left < bottomRect.right) {
                canvas.drawRect(bottomRect, gradientPaintBottom);
            }
        }

        if (fadeLeft && gradientSizeLeft > 0) {
            Rect leftRect = new Rect(gradientRectLeft);
            leftRect.top += radius_tl;
            leftRect.bottom -= radius_bl;

            if (leftRect.top < leftRect.bottom) {
                canvas.drawRect(leftRect, gradientPaintLeft);
            }
        }

        if (fadeRight && gradientSizeRight > 0) {
            Rect rightRect = new Rect(gradientRectRight);
            rightRect.top += radius_tr;
            rightRect.bottom -= radius_br;

            if (rightRect.top < rightRect.bottom) {
                canvas.drawRect(rightRect, gradientPaintRight);
            }
        }

        if (rounded) {
            if (radius_tl > 0) {
                float cx = getPaddingLeft() + radius_tl;
                float cy = getPaddingTop() + radius_tl;
                cornerPaint.setShader(new RadialGradient(cx, cy, radius_tl, FADE_COLORS_REVERSE,
                        null, Shader.TileMode.CLAMP));
                canvas.drawRect(getPaddingLeft(), getPaddingTop(), cx, cy, cornerPaint);
            }

            if (radius_tr > 0) {
                float cx = getWidth() - getPaddingRight() - radius_tr;
                float cy = getPaddingTop() + radius_tr;
                cornerPaint.setShader(new RadialGradient(cx, cy, radius_tr, FADE_COLORS_REVERSE,
                        null, Shader.TileMode.CLAMP));
                canvas.drawRect(cx, getPaddingTop(), getWidth() - getPaddingRight(), cy, cornerPaint);
            }

            if (radius_bl > 0) {
                float cx = getPaddingLeft() + radius_bl;
                float cy = getHeight() - getPaddingBottom() - radius_bl;
                cornerPaint.setShader(new RadialGradient(cx, cy, radius_bl, FADE_COLORS_REVERSE,
                        null, Shader.TileMode.CLAMP));
                canvas.drawRect(getPaddingLeft(), cy, cx, getHeight() - getPaddingBottom(), cornerPaint);
            }

            if (radius_br > 0) {
                float cx = getWidth() - getPaddingRight() - radius_br;
                float cy = getHeight() - getPaddingBottom() - radius_br;
                cornerPaint.setShader(new RadialGradient(cx, cy, radius_br, FADE_COLORS_REVERSE,
                        null, Shader.TileMode.CLAMP));
                canvas.drawRect(cx, cy, getWidth() - getPaddingRight(), getHeight() - getPaddingBottom(), cornerPaint);
            }
        }

        canvas.restoreToCount(count);
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
}