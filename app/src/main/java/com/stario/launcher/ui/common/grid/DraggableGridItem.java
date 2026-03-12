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

package com.stario.launcher.ui.common.grid;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;

public class DraggableGridItem extends FrameLayout {
    public static final byte STATE_ACTIVE = 1 << 2;
    public static final byte STATE_INACTIVE = 1 << 1;
    public static final byte STATE_IDLE = 1;

    private static final int HANDLE_STROKE_WIDTH_DP = 6;
    private static final int HANDLE_SIZE_DP = 40;
    private static final int CORNER_RADIUS_DP = 20;
    private static final int STROKE_WIDTH_DP = 2;
    private static final long ALPHA_TRANSITION_DURATION = 200;
    private static final float IDLE_ALPHA = 0.4f;
    private static final float ACTIVE_ALPHA = 1.0f;
    private static final int PADDING_DP = 5;

    private final Paint borderPaint;
    private final Paint handlePaint;

    private boolean isVisualResizeEnabled;
    private ValueAnimator alphaAnimator;
    private boolean isResizingActive;
    private float visualHeight;
    private float visualWidth;
    private float borderAlpha;

    public String itemId;
    public int minColSpan;
    public int minWidth;
    public int maxColSpan;
    public int maxWidth;
    public int minRowSpan;
    public int minHeight;
    public int maxRowSpan;
    public int maxHeight;

    public DraggableGridItem(Context context) {
        super(context);

        this.isVisualResizeEnabled = false;
        this.isResizingActive = false;

        this.borderAlpha = 0;
        this.minColSpan = 1;
        this.minWidth = -1;
        this.maxColSpan = -1;
        this.maxWidth = -1;
        this.minRowSpan = 1;
        this.minHeight = -1;
        this.maxRowSpan = -1;
        this.maxHeight = -1;

        if (!(context instanceof ThemedActivity)) {
            throw new RuntimeException("Parent activity is not of type ThemedActivity.");
        }

        int color = ((ThemedActivity) context)
                .getAttributeData(com.google.android.material.R.attr.colorPrimaryFixed);

        borderPaint = new Paint();
        borderPaint.setColor(color);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(Measurements.dpToPx(STROKE_WIDTH_DP));

        handlePaint = new Paint();
        handlePaint.setColor(color);
        handlePaint.setStyle(Paint.Style.STROKE);
        handlePaint.setStrokeCap(Paint.Cap.ROUND);
        handlePaint.setStrokeWidth(Measurements.dpToPx(HANDLE_STROKE_WIDTH_DP));

        setWillNotDraw(false);

        int padding = Measurements.dpToPx(PADDING_DP);
        setPadding(padding, padding, padding, padding);
    }

    void setResizingActive(boolean active) {
        isResizingActive = active;
        animateBorderAlpha(active ? IDLE_ALPHA : 0f);
    }

    void animateToState(byte state) {
        if (!isResizingActive) {
            return;
        }

        if (state == STATE_IDLE) {
            animateBorderAlpha(IDLE_ALPHA);
        } else if (state == STATE_ACTIVE) {
            animateBorderAlpha(ACTIVE_ALPHA);
        } else if (state == STATE_INACTIVE) {
            animateBorderAlpha(0f);
        }
    }

    private void animateBorderAlpha(float targetAlpha) {
        if (alphaAnimator != null) {
            alphaAnimator.cancel();
        }

        alphaAnimator = ValueAnimator.ofFloat(borderAlpha, targetAlpha);
        alphaAnimator.setDuration(ALPHA_TRANSITION_DURATION);
        alphaAnimator.addUpdateListener(animation -> {
            borderAlpha = (float) animation.getAnimatedValue();
            invalidate();
        });

        alphaAnimator.start();
    }

    public void setVisualResizeBounds(float width, float height) {
        this.isVisualResizeEnabled = true;
        this.visualWidth = width;
        this.visualHeight = height;

        invalidate();
    }

    public void animateVisualResize(float targetW, float targetH, Runnable onAnimationEnd) {
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        float startW = isVisualResizeEnabled ? visualWidth : getWidth();
        float startH = isVisualResizeEnabled ? visualHeight : getHeight();

        isVisualResizeEnabled = true;

        anim.setDuration(200);
        anim.addUpdateListener(animation -> {
            float frac = animation.getAnimatedFraction();
            visualWidth = startW + (targetW - startW) * frac;
            visualHeight = startH + (targetH - startH) * frac;

            invalidate();
        });

        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isVisualResizeEnabled = false;

                if (onAnimationEnd != null) {
                    onAnimationEnd.run();
                }
            }
        });

        anim.start();
    }

    boolean isResizeHandleTouched(float x, float y) {
        if (!isResizingActive) {
            return false;
        }

        int handleSize = Measurements.dpToPx(HANDLE_SIZE_DP);

        float width = isVisualResizeEnabled ? visualWidth : getWidth();
        float height = isVisualResizeEnabled ? visualHeight : getHeight();

        boolean resizeW = canResizeWidth();
        boolean resizeH = canResizeHeight();

        if (resizeW && resizeH) {
            return x > width - handleSize && y > height - handleSize;
        }

        if (resizeW) {
            return x > width - handleSize
                    && y > height / 2f - handleSize / 2f
                    && y < height / 2f + handleSize / 2f;
        }

        if (resizeH) {
            return y > height - handleSize
                    && x > width / 2f - handleSize / 2f
                    && x < width / 2f + handleSize / 2f;
        }

        return false;
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);

        if (getChildCount() > 1) {
            throw new IllegalStateException("DraggableGridItem can host only one direct child.");
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!(getParent() instanceof DynamicGridLayout)) {
            throw new IllegalStateException(
                    "DraggableGridItem can only be added to a DynamicGridLayout."
            );
        }
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);

        if (borderAlpha > 0) {
            int cornerSize = Measurements.dpToPx(CORNER_RADIUS_DP);
            int strokeWidth = Measurements.dpToPx(HANDLE_STROKE_WIDTH_DP);

            float currentWidth = isVisualResizeEnabled ? visualWidth : getWidth();
            float currentHeight = isVisualResizeEnabled ? visualHeight : getHeight();

            int baseAlpha = 255;
            borderPaint.setAlpha((int) (baseAlpha * borderAlpha));
            handlePaint.setAlpha((int) (baseAlpha * Math.min(1, borderAlpha / IDLE_ALPHA)));

            RectF rect = new RectF(
                    strokeWidth / 2f,
                    strokeWidth / 2f,
                    currentWidth - strokeWidth / 2f,
                    currentHeight - strokeWidth / 2f
            );

            canvas.drawRoundRect(rect, cornerSize, cornerSize, borderPaint);

            boolean resizeW = canResizeWidth();
            boolean resizeH = canResizeHeight();

            if (resizeW || resizeH) {

                float left, top, right, bottom;

                if (resizeW && resizeH) {
                    left = currentWidth - 2 * cornerSize - strokeWidth / 2f;
                    top = currentHeight - 2 * cornerSize - strokeWidth / 2f;
                    right = currentWidth - strokeWidth / 2f;
                    bottom = currentHeight - strokeWidth / 2f;

                    RectF handleRect = new RectF(left, top, right, bottom);
                    canvas.drawArc(handleRect, 20, 50, false, handlePaint);
                } else if (resizeW) {
                    left = currentWidth - strokeWidth / 2f;
                    top = currentHeight / 2f - cornerSize / 2f;
                    right = currentWidth - strokeWidth / 2f;
                    bottom = currentHeight / 2f + cornerSize / 2f;

                    canvas.drawLine(left, top, right, bottom, handlePaint);
                } else {
                    left = currentWidth / 2f - cornerSize / 2f;
                    top = currentHeight - strokeWidth / 2f;
                    right = currentWidth / 2f + cornerSize / 2f;
                    bottom = currentHeight - strokeWidth / 2f;

                    canvas.drawLine(left, top, right, bottom, handlePaint);
                }
            }
        }
    }

    private boolean canResizeWidth() {
        DynamicGridLayout parent = (DynamicGridLayout) getParent();

        int minComputedWidth;
        if (minWidth > 0) {
            if (minColSpan > 0) {
                minComputedWidth = Math.max(minWidth, minColSpan * parent.getCellWidth());
            } else {
                minComputedWidth = minWidth;
            }
        } else if (minColSpan > 0) {
            minComputedWidth = minColSpan * parent.getCellWidth();
        } else {
            minComputedWidth = 0;
        }

        int maxComputedWidth;
        if (maxWidth > 0) {
            if (maxColSpan > 0) {
                maxComputedWidth = Math.min(maxWidth, maxColSpan * parent.getCellWidth());
            } else {
                maxComputedWidth = maxWidth;
            }
        } else if (maxColSpan > 0) {
            maxComputedWidth = maxColSpan * parent.getCellWidth();
        } else {
            maxComputedWidth = parent.getWidth();
        }

        if (maxComputedWidth - minComputedWidth < parent.getCellWidth()) {
            return false;
        }

        if (maxColSpan > 0) {
            return Math.min(maxColSpan, parent.getColumnCount()) > minColSpan;
        }

        return parent.getColumnCount() > minColSpan;
    }

    private boolean canResizeHeight() {
        DynamicGridLayout parent = (DynamicGridLayout) getParent();

        int minComputedHeight;
        if (minHeight > 0) {
            if (minRowSpan > 0) {
                minComputedHeight = Math.max(minHeight, minRowSpan * parent.getCellHeight());
            } else {
                minComputedHeight = minHeight;
            }
        } else if (minRowSpan > 0) {
            minComputedHeight = minRowSpan * parent.getCellHeight();
        } else {
            minComputedHeight = 0;
        }

        int maxComputedHeight;
        if (maxHeight > 0) {
            if (maxRowSpan > 0) {
                maxComputedHeight = Math.min(maxHeight, maxRowSpan * parent.getCellHeight());
            } else {
                maxComputedHeight = maxHeight;
            }
        } else if (maxRowSpan > 0) {
            maxComputedHeight = maxRowSpan * parent.getCellHeight();
        } else {
            maxComputedHeight = parent.getHeight();
        }

        if (maxComputedHeight - minComputedHeight < parent.getCellHeight()) {
            return false;
        }

        if (maxRowSpan > 0) {
            return Math.min(maxRowSpan, parent.getRowCount()) > minRowSpan;
        }

        return parent.getRowCount() > minRowSpan;
    }

    private boolean canResize() {
        return canResizeWidth() || canResizeHeight();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return isResizingActive;
    }
}