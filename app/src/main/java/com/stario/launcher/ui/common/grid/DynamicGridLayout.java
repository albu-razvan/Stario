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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import com.stario.launcher.R;
import com.stario.launcher.Stario;
import com.stario.launcher.ui.Measurements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Used Gemini 3 for the nudge and item move logic
 */
public class DynamicGridLayout extends ViewGroup {
    public static final String TAG = "DynamicGridLayout";

    private static final int MIN_CELL_SIZE_DP = 110;
    private static final int MIN_COLS_PORTRAIT = 4;
    private static final int MIN_COLS_LANDSCAPE = 5;
    private static final int MIN_ROWS_PORTRAIT = 5;
    private static final int MIN_ROWS_LANDSCAPE = 3;
    private static final long REORDER_DELAY_MS = 250;
    private static final int NUDGE_OFFSET_PX = 40;

    private final GridTemplateManager templateManager;
    private final Map<View, Point> preAnimVisualPos;

    private boolean isRearrangeable;
    private int cellWidth;
    private int cellHeight;
    private int colCount;
    private int rowCount;

    private DraggableGridItem activeItem;
    private float initialTouchX;
    private float initialTouchY;
    private boolean isResizing;
    private float lastTouchX;
    private float lastTouchY;

    private LayoutParams originalParams;
    private Runnable reorderRunnable;
    private View currentHoverTarget;
    private final Handler handler;
    private int pendingTargetCol;
    private int pendingTargetRow;
    private int lastMeasuredCols;
    private int lastMeasuredRows;
    private int pendingPushDirX;
    private int pendingPushDirY;

    public DynamicGridLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        String key;
        int templateResourceId;
        try (TypedArray arr = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.DynamicGridLayout,
                0, 0
        )) {
            key = arr.getString(R.styleable.DynamicGridLayout_grid_preference_key);
            templateResourceId = arr.getResourceId(
                    R.styleable.DynamicGridLayout_grid_layout_template,
                    0
            );

            if (key == null || key.isEmpty()) {
                throw new RuntimeException("'grid_preference_key' attribute is required");
            }
        }

        this.colCount = MIN_COLS_PORTRAIT;
        this.rowCount = MIN_ROWS_PORTRAIT;
        this.isRearrangeable = false;
        this.templateManager = new GridTemplateManager(
                (Stario) context.getApplicationContext(), key, templateResourceId);

        this.activeItem = null;
        this.isResizing = false;

        this.preAnimVisualPos = new HashMap<>();
        this.currentHoverTarget = null;
        this.handler = new Handler();

        this.pendingPushDirX = 0;
        this.pendingPushDirY = 0;
        this.lastMeasuredCols = -1;
        this.lastMeasuredRows = -1;

        setClipChildren(false);
        setClipToPadding(false);
    }

    public void setRearrangeable(boolean rearrangeable) {
        this.isRearrangeable = rearrangeable;

        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);

            if (child instanceof DraggableGridItem) {
                ((DraggableGridItem) child).setResizingActive(rearrangeable);
            }
        }
    }

    public void addItem(DraggableGridItem view, ItemLayoutData defaultTemplateData) {
        ItemLayoutData savedState = null;

        Map<String, ItemLayoutData> currentConfig = templateManager.getLayoutForSize(colCount, rowCount);

        if (currentConfig != null) {
            savedState = currentConfig.get(view.itemId);
        }

        ItemLayoutData target = savedState != null ? savedState : defaultTemplateData;

        view.minColSpan = defaultTemplateData.minColSpan;
        view.minRowSpan = defaultTemplateData.minRowSpan;
        view.maxColSpan = defaultTemplateData.maxColSpan > 0 ? defaultTemplateData.maxColSpan : -1;
        view.maxRowSpan = defaultTemplateData.maxRowSpan > 0 ? defaultTemplateData.maxRowSpan : -1;
        view.setResizingActive(isRearrangeable);

        Rect targetRect = new Rect(target.col, target.row,
                target.col + target.colSpan, target.row + target.rowSpan);

        LayoutParams layoutParams = new LayoutParams(target.col, target.row, target.colSpan, target.rowSpan);
        view.setLayoutParams(layoutParams);

        List<View> collisions = getCollisions(targetRect, view);
        if (!collisions.isEmpty()) {
            boolean shiftSuccess = shiftItemsToFreeSpace(collisions, view);

            if (!shiftSuccess) {
                Rect freeSpot = findClosestFreeSpot(target.colSpan,
                        target.rowSpan, target.col, target.row, view);

                if (freeSpot != null) {
                    layoutParams.col = freeSpot.left;
                    layoutParams.row = freeSpot.top;
                } else {
                    layoutParams.col = 0;
                    layoutParams.row = getNextBottomRow();
                }
            }
        }

        addView(view);
        saveLayoutState();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int totalWidth = MeasureSpec.getSize(widthMeasureSpec);
        int totalHeight = MeasureSpec.getSize(heightMeasureSpec);

        int horizontalPadding = getPaddingLeft() + getPaddingRight();
        int verticalPadding = getPaddingTop() + getPaddingBottom();

        int availableWidth = totalWidth - horizontalPadding;
        int availableHeight = totalHeight - verticalPadding;

        float density = getResources().getDisplayMetrics().density;
        int minCellSizePx = (int) (MIN_CELL_SIZE_DP * density);

        colCount = Math.max(Measurements.isLandscape()
                ? MIN_COLS_LANDSCAPE : MIN_COLS_PORTRAIT, availableWidth / minCellSizePx);

        while ((availableWidth / colCount) > (minCellSizePx * 2)) colCount++;
        cellWidth = availableWidth / colCount;

        rowCount = Math.max(Measurements.isLandscape()
                ? MIN_ROWS_LANDSCAPE : MIN_ROWS_PORTRAIT, availableHeight / minCellSizePx);

        while ((availableHeight / rowCount) > (minCellSizePx * 2)) rowCount++;
        cellHeight = availableHeight / rowCount;

        if (colCount != lastMeasuredCols || rowCount != lastMeasuredRows) {
            lastMeasuredCols = colCount;
            lastMeasuredRows = rowCount;

            reloadLayoutForCurrentSize();
        }

        int measuredWidth = colCount * cellWidth + horizontalPadding;
        int measuredHeight = rowCount * cellHeight + verticalPadding;

        setMeasuredDimension(measuredWidth, measuredHeight);

        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);
            LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();

            if (layoutParams.col + layoutParams.colSpan > colCount) {
                layoutParams.col = Math.max(0, colCount - layoutParams.colSpan);
            }

            if (layoutParams.row + layoutParams.rowSpan > rowCount) {
                layoutParams.row = Math.max(0, rowCount - layoutParams.rowSpan);
            }

            int childWidth = layoutParams.colSpan * cellWidth;
            int childHeight = layoutParams.rowSpan * cellHeight;

            child.measure(MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY));
        }
    }

    private void reloadLayoutForCurrentSize() {
        Map<String, ItemLayoutData> newConfig = templateManager.getLayoutForSize(colCount, rowCount);

        boolean changed = false;
        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);

            if (child instanceof DraggableGridItem) {
                ItemLayoutData data = newConfig.get(((DraggableGridItem) child).itemId);

                if (data != null) {
                    LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();

                    if (layoutParams.col != data.col || layoutParams.row != data.row ||
                            layoutParams.colSpan != data.colSpan || layoutParams.rowSpan != data.rowSpan) {
                        layoutParams.col = data.col;
                        layoutParams.row = data.row;
                        layoutParams.colSpan = data.colSpan;
                        layoutParams.rowSpan = data.rowSpan;

                        changed = true;
                    }
                }
            }
        }

        if (changed) {
            requestLayout();
        }
    }

    @Override
    @SuppressLint("DrawAllocation")
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);
            LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();

            int left = getPaddingLeft() + (layoutParams.col * cellWidth);
            int top = getPaddingTop() + (layoutParams.row * cellHeight);
            int right = left + child.getMeasuredWidth();
            int bottom = top + child.getMeasuredHeight();

            if (child == activeItem && !isResizing && child.getTag(R.id.is_dragging_tag) != null) {
                child.layout(child.getLeft(), child.getTop(),
                        child.getLeft() + child.getMeasuredWidth(),
                        child.getTop() + child.getMeasuredHeight());
            } else {
                Point oldPos = preAnimVisualPos.get(child);
                child.layout(left, top, right, bottom);

                if (oldPos != null && child != activeItem) {
                    int startTransX = oldPos.x - left;
                    int startTransY = oldPos.y - top;

                    double distance = Math.sqrt(Math.pow(startTransX, 2) + Math.pow(startTransY, 2));

                    if (distance > 2) { // Small threshold to avoid micro-animations
                        child.setTranslationX(startTransX);
                        child.setTranslationY(startTransY);

                        child.animate()
                                .translationX(0)
                                .translationY(0)
                                .setDuration((long) Math.min(350, Math.max(150, distance * 0.8)))
                                .setInterpolator(new DecelerateInterpolator(1.2f))
                                .start();
                    } else {
                        child.setTranslationX(0);
                        child.setTranslationY(0);
                    }
                } else if (child != activeItem) {
                    child.setTranslationX(0);
                    child.setTranslationY(0);
                }
            }
        }

        preAnimVisualPos.clear();
    }

    int getColumnCount() {
        return colCount;
    }

    int getRowCount() {
        return rowCount;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return isRearrangeable || super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isRearrangeable) {
            return super.onTouchEvent(event);
        }

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                activeItem = getChildAtPos(x, y);

                if (activeItem != null) {
                    captureLayoutState();

                    lastTouchX = x;
                    lastTouchY = y;
                    initialTouchX = x;
                    initialTouchY = y;

                    isResizing = activeItem.isResizeHandleTouched(
                            x - activeItem.getLeft(), y - activeItem.getTop());

                    LayoutParams layoutParams = (LayoutParams) activeItem.getLayoutParams();
                    originalParams = new LayoutParams(layoutParams.col,
                            layoutParams.row, layoutParams.colSpan, layoutParams.rowSpan);

                    activeItem.bringToFront();

                    if (!isResizing) {
                        activeItem.setTag(R.id.is_dragging_tag, true);
                    }

                    for (int index = 0; index < getChildCount(); index++) {
                        View child = getChildAt(index);

                        if (child instanceof DraggableGridItem) {
                            DraggableGridItem item = (DraggableGridItem) child;

                            if (item == activeItem) {
                                item.animateToState(DraggableGridItem.STATE_ACTIVE);
                            } else {
                                item.animateToState(DraggableGridItem.STATE_INACTIVE);
                            }
                        }
                    }

                    return true;
                }

                break;

            case MotionEvent.ACTION_MOVE:
                if (activeItem != null) {
                    float dx = x - lastTouchX;
                    float dy = y - lastTouchY;

                    if (isResizing) {
                        handleResize(x, y);
                    } else {
                        handleDrag(dx, dy);
                    }

                    lastTouchX = x;
                    lastTouchY = y;
                }

                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (activeItem != null) {
                    if (isResizing) {
                        finishResize();
                    } else {
                        finishDrag();
                    }

                    for (int index = 0; index < getChildCount(); index++) {
                        View child = getChildAt(index);

                        if (child instanceof DraggableGridItem) {
                            ((DraggableGridItem) child).animateToState(DraggableGridItem.STATE_IDLE);
                        }
                    }
                }

                resetHoverState();
                break;
        }

        return true;
    }

    private void handleResize(float currX, float currY) {
        float totalDx = currX - initialTouchX;
        float totalDy = currY - initialTouchY;

        float baseW = originalParams.colSpan * cellWidth;
        float baseH = originalParams.rowSpan * cellHeight;

        float rawVisualW = baseW + totalDx;
        float rawVisualH = baseH + totalDy;

        float maxAllowedW = getWidth() - getPaddingLeft() - getPaddingRight();
        if (activeItem.maxColSpan > 0) {
            maxAllowedW = Math.min(maxAllowedW, activeItem.maxColSpan * cellWidth);
        }

        float maxAllowedH = getHeight() - getPaddingTop() - getPaddingBottom();
        if (activeItem.maxRowSpan > 0) {
            maxAllowedH = Math.min(maxAllowedH, activeItem.maxRowSpan * cellHeight);
        }

        float minAllowedW = activeItem.minColSpan * cellWidth;
        float minAllowedH = activeItem.minRowSpan * cellHeight;

        float clampedVisualW = Math.max(minAllowedW, Math.min(rawVisualW, maxAllowedW));
        float clampedVisualH = Math.max(minAllowedH, Math.min(rawVisualH, maxAllowedH));

        activeItem.setVisualResizeBounds(clampedVisualW, clampedVisualH);

        int spanX = calculateGridSpan(clampedVisualW, cellWidth);
        int spanY = calculateGridSpan(clampedVisualH, cellHeight);

        spanX = Math.max(activeItem.minColSpan,
                activeItem.maxColSpan > 0 ? Math.min(spanX, activeItem.maxColSpan) : spanX);
        spanY = Math.max(activeItem.minRowSpan,
                activeItem.maxRowSpan > 0 ? Math.min(spanY, activeItem.maxRowSpan) : spanY);

        if (originalParams.col + spanX > colCount) {
            spanX = colCount - originalParams.col;
        }

        Rect potentialRect = new Rect(originalParams.col, originalParams.row,
                originalParams.col + spanX, originalParams.row + spanY);

        List<View> collisions = getCollisions(potentialRect, activeItem);
        if (collisions.isEmpty()) {
            LayoutParams layoutParams = (LayoutParams) activeItem.getLayoutParams();

            if (layoutParams.colSpan != spanX || layoutParams.rowSpan != spanY) {
                layoutParams.colSpan = spanX;
                layoutParams.rowSpan = spanY;

                activeItem.setLayoutParams(layoutParams);
            }
        }
    }

    private void finishResize() {
        DraggableGridItem itemToResize = activeItem;

        activeItem = null;
        isResizing = false;

        if (itemToResize == null) {
            return;
        }

        LayoutParams layoutParams = (LayoutParams) itemToResize.getLayoutParams();
        float targetW = layoutParams.colSpan * cellWidth;
        float targetH = layoutParams.rowSpan * cellHeight;

        itemToResize.animateVisualResize(targetW, targetH, () -> {
            itemToResize.setResizingActive(isRearrangeable);

            saveLayoutState();
            requestLayout();
        });
    }

    private void handleDrag(float dx, float dy) {
        // Bounds Constraint
        int parentLeft = getPaddingLeft();
        int parentTop = getPaddingTop();
        int parentRight = getWidth() - getPaddingRight();
        int parentBottom = getHeight() - getPaddingBottom();

        int itemW = activeItem.getWidth();
        int itemH = activeItem.getHeight();

        // Calculate drag based on current view position
        int potentialLeft = activeItem.getLeft() + (int) dx;
        int potentialTop = activeItem.getTop() + (int) dy;

        // Clamp
        int clampedLeft = Math.max(parentLeft, Math.min(potentialLeft, parentRight - itemW));
        int clampedTop = Math.max(parentTop, Math.min(potentialTop, parentBottom - itemH));

        activeItem.offsetLeftAndRight(clampedLeft - activeItem.getLeft());
        activeItem.offsetTopAndBottom(clampedTop - activeItem.getTop());

        // Snap based on insertion point (Left + Half Cell)
        float relativeLeft = activeItem.getLeft() - parentLeft;
        float relativeTop = activeItem.getTop() - parentTop;

        int rawCol = (int) ((relativeLeft + (cellWidth / 2f)) / cellWidth);
        int rawRow = (int) ((relativeTop + (cellHeight / 2f)) / cellHeight);

        LayoutParams layoutParams = (LayoutParams) activeItem.getLayoutParams();
        int targetCol = Math.max(0, Math.min(rawCol, colCount - layoutParams.colSpan));
        int targetRow = Math.max(0, Math.min(rawRow, rowCount - layoutParams.rowSpan));

        // Collision Logic
        Rect targetRect = new Rect(targetCol, targetRow,
                targetCol + layoutParams.colSpan,
                targetRow + layoutParams.rowSpan);
        List<View> collisions = getCollisions(targetRect, activeItem);

        if (collisions.isEmpty()) {
            if (layoutParams.col != targetCol || layoutParams.row != targetRow) {
                captureLayoutState();

                layoutParams.col = targetCol;
                layoutParams.row = targetRow;

                requestLayout();
                saveLayoutState();
            }
            resetHoverState();
        } else {
            View primaryCollision = collisions.get(0);

            // Smart Nudge Direction Calculation
            int parentCenterX = activeItem.getLeft() + activeItem.getWidth() / 2;
            int parentCenterY = activeItem.getTop() + activeItem.getHeight() / 2;
            int targetCenterX = primaryCollision.getLeft() + primaryCollision.getWidth() / 2;
            int targetCenterY = primaryCollision.getTop() + primaryCollision.getHeight() / 2;

            LayoutParams targetLayoutParams = (LayoutParams) primaryCollision.getLayoutParams();

            // Determine possible axes
            boolean canGoRight = (targetLayoutParams.col + targetLayoutParams.colSpan < colCount);
            boolean canGoLeft = (targetLayoutParams.col > 0);
            boolean canGoDown = (targetLayoutParams.row + targetLayoutParams.rowSpan < rowCount);
            boolean canGoUp = (targetLayoutParams.row > 0);

            // "Full Width Drag" Constraint -> Other view can only go Up/Down
            boolean forcedVertical = (layoutParams.colSpan == colCount);

            int nudgeX = 0;
            int nudgeY = 0;
            int pushDirX = 0;
            int pushDirY = 0;

            // Determine Preferred Axis
            boolean preferHorizontal = !forcedVertical
                    && (Math.abs(targetCenterX - parentCenterX) > Math.abs(targetCenterY - parentCenterY));

            if (preferHorizontal) {
                // Try Horizontal first
                if (targetCenterX > parentCenterX && canGoRight) {
                    nudgeX = NUDGE_OFFSET_PX;
                    pushDirX = 1;
                } else if (targetCenterX <= parentCenterX && canGoLeft) {
                    nudgeX = -NUDGE_OFFSET_PX;
                    pushDirX = -1;
                } else if (canGoDown) { // Fallback to Vertical
                    nudgeY = NUDGE_OFFSET_PX;
                    pushDirY = 1;
                } else if (canGoUp) {
                    nudgeY = -NUDGE_OFFSET_PX;
                    pushDirY = -1;
                }
            } else {
                // Try Vertical first
                if (targetCenterY > parentCenterY && canGoDown) {
                    nudgeY = NUDGE_OFFSET_PX;
                    pushDirY = 1;
                } else if (targetCenterY <= parentCenterY && canGoUp) {
                    nudgeY = -NUDGE_OFFSET_PX;
                    pushDirY = -1;
                } else if (!forcedVertical && canGoRight) { // Fallback to Horizontal
                    nudgeX = NUDGE_OFFSET_PX;
                    pushDirX = 1;
                } else if (!forcedVertical && canGoLeft) {
                    nudgeX = -NUDGE_OFFSET_PX;
                    pushDirX = -1;
                }
            }

            // Only animate if we found a valid direction
            if (nudgeX != 0 || nudgeY != 0) {
                // Update State if Target or Direction changed
                if (primaryCollision != currentHoverTarget
                        || pushDirX != pendingPushDirX
                        || pushDirY != pendingPushDirY) {

                    if (primaryCollision != currentHoverTarget) {
                        resetHoverState();

                        currentHoverTarget = primaryCollision;
                    }

                    pendingPushDirX = pushDirX;
                    pendingPushDirY = pushDirY;

                    // Show hint animation
                    animateNudge(currentHoverTarget, nudgeX, nudgeY);

                    // Cancel pending shift for old target/dir
                    if (reorderRunnable != null) {
                        handler.removeCallbacks(reorderRunnable);
                    }

                    // Store the intent
                    pendingTargetCol = targetCol;
                    pendingTargetRow = targetRow;

                    // Schedule real shift
                    reorderRunnable = () -> {
                        if (currentHoverTarget != null) {
                            captureLayoutState();

                            boolean shifted;

                            // Try Primary Direction
                            shifted = performDirectionalShift(currentHoverTarget, pendingPushDirX, pendingPushDirY);

                            // Try other axis if primary failed
                            if (!shifted) {
                                int fallbackDirX = (pendingPushDirX == 0) ? 1 : 0;
                                int fallbackDirY = (pendingPushDirY == 0) ? 1 : 0;

                                shifted = performDirectionalShift(currentHoverTarget, fallbackDirX, fallbackDirY);
                                if (!shifted) {
                                    shifted = performDirectionalShift(currentHoverTarget, -fallbackDirX, -fallbackDirY);
                                }
                            }

                            // "Evict" :) to any free spot on the screen
                            if (!shifted) {
                                LayoutParams targetParams = (LayoutParams) currentHoverTarget.getLayoutParams();
                                Rect freeSpot = findClosestFreeSpot(targetParams.colSpan, targetParams.rowSpan,
                                        targetParams.col, targetParams.row, activeItem);

                                if (freeSpot != null) {
                                    targetParams.col = freeSpot.left;
                                    targetParams.row = freeSpot.top;
                                    shifted = true;
                                }
                            }

                            // Claim the space
                            if (shifted) {
                                LayoutParams activeParams = (LayoutParams) activeItem.getLayoutParams();

                                activeParams.col = pendingTargetCol;
                                activeParams.row = pendingTargetRow;

                                requestLayout();
                                saveLayoutState();
                            }

                            resetHoverState();
                        }
                    };

                    handler.postDelayed(reorderRunnable, REORDER_DELAY_MS);
                }
            } else {
                resetHoverState();
            }
        }
    }

    private void finishDrag() {
        if (activeItem == null) {
            return;
        }

        activeItem.setTag(R.id.is_dragging_tag, null);
        captureLayoutState();

        activeItem = null;

        // This triggers layout. If layout params were not updated (due to failed shift/overlap),
        // item snaps back to its last valid position.
        requestLayout();
        saveLayoutState();
    }

    private void animateNudge(View target, int dx, int dy) {
        target.animate()
                .translationX(dx)
                .translationY(dy)
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void resetHoverState() {
        if (reorderRunnable != null) {
            handler.removeCallbacks(reorderRunnable);
            reorderRunnable = null;
        }

        if (currentHoverTarget != null) {
            currentHoverTarget.animate()
                    .translationX(0)
                    .translationY(0)
                    .setDuration(200)
                    .start();

            currentHoverTarget = null;
        }

        pendingPushDirX = 0;
        pendingPushDirY = 0;
    }

    private boolean performDirectionalShift(View target, int dirX, int dirY) {
        if (dirX == 0 && dirY == 0) {
            return false;
        }

        LayoutParams targetParams = (LayoutParams) target.getLayoutParams();
        LayoutParams activeParams = (LayoutParams) activeItem.getLayoutParams();

        int newCol = targetParams.col;
        int newRow = targetParams.row;

        // Calculate displacement based on spans to ensure they don't overlap anymore
        if (dirX > 0) {
            newCol = pendingTargetCol + activeParams.colSpan;
        } else if (dirX < 0) {
            newCol = pendingTargetCol - targetParams.colSpan;
        }

        if (dirY > 0) {
            newRow = pendingTargetRow + activeParams.rowSpan;
        } else if (dirY < 0) {
            newRow = pendingTargetRow - targetParams.rowSpan;
        }

        // Boundary Check
        if (newCol < 0 || newCol + targetParams.colSpan > colCount ||
                newRow < 0 || newRow + targetParams.rowSpan > rowCount) {
            return false;
        }

        // Collision Check at the new destination
        Rect destinationRect = new Rect(newCol, newRow,
                newCol + targetParams.colSpan, newRow + targetParams.rowSpan);

        List<View> collisions = getCollisions(destinationRect, activeItem);
        collisions.remove(target);

        if (collisions.isEmpty()) {
            targetParams.col = newCol;
            targetParams.row = newRow;
            return true;
        }

        return false;
    }

    private int calculateGridSpan(float visualSize, int cellSize) {
        return Math.max(1, Math.round(visualSize / cellSize));
    }

    private void captureLayoutState() {
        preAnimVisualPos.clear();

        for (int index = 0; index < getChildCount(); index++) {
            View view = getChildAt(index);
            preAnimVisualPos.put(view, new Point((int) view.getX(), (int) view.getY()));
        }
    }

    private DraggableGridItem getChildAtPos(float x, float y) {
        for (int index = getChildCount() - 1; index >= 0; index--) {
            View child = getChildAt(index);

            if (x >= child.getLeft() && x <= child.getRight() &&
                    y >= child.getTop() && y <= child.getBottom()) {
                return (DraggableGridItem) child;
            }
        }

        return null;
    }

    private List<View> getCollisions(Rect targetRect, View ignoreView) {
        List<View> collisions = new ArrayList<>();

        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);

            if (child != ignoreView) {
                LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
                Rect existing = new Rect(layoutParams.col, layoutParams.row,
                        layoutParams.col + layoutParams.colSpan,
                        layoutParams.row + layoutParams.rowSpan);

                if (Rect.intersects(targetRect, existing)) {
                    collisions.add(child);
                }
            }
        }

        return collisions;
    }

    private boolean shiftItemsToFreeSpace(List<View> itemsToMove, View ignoreView) {
        Map<View, Point> rollback = new HashMap<>();

        for (View view : itemsToMove) {
            LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
            rollback.put(view, new Point(layoutParams.col, layoutParams.row));
        }

        for (View view : itemsToMove) {
            LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
            Rect free = findClosestFreeSpot(layoutParams.colSpan, layoutParams.rowSpan,
                    layoutParams.col, layoutParams.row, ignoreView);

            if (free != null) {
                layoutParams.col = free.left;
                layoutParams.row = free.top;
            } else {
                for (Map.Entry<View, Point> entry : rollback.entrySet()) {
                    LayoutParams entryLayoutParams = (LayoutParams) entry.getKey().getLayoutParams();

                    entryLayoutParams.col = entry.getValue().x;
                    entryLayoutParams.row = entry.getValue().y;
                }

                return false;
            }
        }
        return true;
    }

    private Rect findClosestFreeSpot(int width, int height, int preferredCol,
                                     int preferredRow, View ignoreView) {
        int maxRow = Math.max(rowCount, getNextBottomRow() + height + 2);
        double bestDistance = Double.MAX_VALUE;
        Rect bestRect = null;

        for (int row = 0; row < maxRow; row++) {
            for (int column = 0; column <= colCount - width; column++) {
                double distance = Math.pow(column - preferredCol, 2)
                        + Math.pow(row - preferredRow, 2);

                if (distance < bestDistance) {
                    Rect candidate = new Rect(column, row,
                            column + width, row + height);
                    boolean collides = false;

                    if (ignoreView != null) {
                        LayoutParams ignoreViewLayoutParams = (LayoutParams) ignoreView.getLayoutParams();
                        Rect ignoreRect = new Rect(ignoreViewLayoutParams.col, ignoreViewLayoutParams.row,
                                ignoreViewLayoutParams.col + ignoreViewLayoutParams.colSpan,
                                ignoreViewLayoutParams.row + ignoreViewLayoutParams.rowSpan);

                        if (Rect.intersects(candidate, ignoreRect)) {
                            collides = true;
                        }
                    }

                    if (!collides) {
                        List<View> others = getCollisions(candidate, ignoreView);

                        if (!others.isEmpty()) {
                            collides = true;
                        }
                    }

                    if (!collides) {
                        bestDistance = distance;
                        bestRect = candidate;
                    }
                }
            }
        }

        return bestRect;
    }

    private int getNextBottomRow() {
        int max = 0;

        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);

            if (child != activeItem) {
                LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
                max = Math.max(max, layoutParams.row + layoutParams.rowSpan);
            }
        }

        return max;
    }

    private void saveLayoutState() {
        Map<String, ItemLayoutData> currentMap = new HashMap<>();
        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);

            if (child instanceof DraggableGridItem) {
                DraggableGridItem childItem = (DraggableGridItem) child;

                LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
                ItemLayoutData data = new ItemLayoutData(childItem.itemId,
                        layoutParams.col, layoutParams.row, layoutParams.colSpan, layoutParams.rowSpan);
                data.minColSpan = childItem.minColSpan;
                data.minRowSpan = childItem.minRowSpan;
                data.maxColSpan = childItem.maxColSpan;
                data.maxRowSpan = childItem.maxRowSpan;

                currentMap.put(childItem.itemId, data);
            }
        }

        templateManager.saveUserLayout(colCount, rowCount, currentMap);
    }

    private static class LayoutParams extends ViewGroup.LayoutParams {
        private int col;
        private int row;
        private int colSpan;
        private int rowSpan;

        public LayoutParams(int col, int row, int colSpan, int rowSpan) {
            super(MATCH_PARENT, MATCH_PARENT);

            this.col = col;
            this.row = row;
            this.colSpan = colSpan;
            this.rowSpan = rowSpan;
        }
    }

    public static class ItemLayoutData {
        public String id;
        public int col;
        public int row;
        public int colSpan;
        public int rowSpan;
        public int minColSpan;
        public int minRowSpan;
        public int maxColSpan;
        public int maxRowSpan;

        public ItemLayoutData(String id, int col, int row, int colSpan, int rowSpan) {
            this.id = id;
            this.col = col;
            this.row = row;
            this.colSpan = colSpan;
            this.rowSpan = rowSpan;
            this.minColSpan = 1;
            this.minRowSpan = 1;
            this.maxColSpan = -1;
            this.maxRowSpan = -1;
        }
    }
}