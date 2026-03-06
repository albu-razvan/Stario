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
import android.widget.Toast;

import com.stario.launcher.R;
import com.stario.launcher.Stario;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.utils.animation.Animation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

// Used Gemini 3 for the nudge and item move logic
public class DynamicGridLayout extends ViewGroup {
    public static final String TAG = "DynamicGridLayout";

    private static final int MIN_CELL_SIZE_DP = 110;
    private static final int MIN_COLS_PORTRAIT = 4;
    private static final int MIN_COLS_LANDSCAPE = 5;
    private static final int MIN_ROWS_PORTRAIT = 5;
    private static final int MIN_ROWS_LANDSCAPE = 3;
    private static final long REORDER_DELAY_MS = 300;
    private static final float HINT_SCALE = 0.85f;
    private static final float HINT_ALPHA = 0.85f;

    private final GridTemplateManager templateManager;
    private final Map<View, Point> preAnimVisualPos;
    private final List<View> hintedViews;

    private boolean isRearrangeable;
    private int cellWidth;
    private int cellHeight;
    private int colCount;
    private int rowCount;

    private String warningMessage;

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
    private int lastReorderCol;
    private int lastReorderRow;

    private int runningAnimations;

    public DynamicGridLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        String key;
        int templateResourceId;
        try (TypedArray array = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.DynamicGridLayout,
                0, 0
        )) {
            key = array.getString(R.styleable.DynamicGridLayout_grid_preference_key);
            templateResourceId = array.getResourceId(
                    R.styleable.DynamicGridLayout_grid_layout_template,
                    0
            );
            warningMessage = array.getString(R.styleable.DynamicGridLayout_grid_no_space_warning);

            if (key == null || key.isEmpty()) {
                throw new RuntimeException("'grid_preference_key' attribute is required");
            }
        }

        if (warningMessage == null) {
            warningMessage = "No room available.";
        }

        this.colCount = MIN_COLS_PORTRAIT;
        this.rowCount = MIN_ROWS_PORTRAIT;
        this.isRearrangeable = false;
        this.templateManager = new GridTemplateManager(
                (Stario) context.getApplicationContext(), key, templateResourceId);

        this.activeItem = null;
        this.isResizing = false;

        this.preAnimVisualPos = new HashMap<>();
        this.hintedViews = new ArrayList<>();
        this.currentHoverTarget = null;
        this.handler = new Handler();

        this.lastReorderCol = -1;
        this.lastReorderRow = -1;
        this.lastMeasuredCols = -1;
        this.lastMeasuredRows = -1;

        this.runningAnimations = 0;

        setClipChildren(false);
        setClipToPadding(false);
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);

        if (!(child instanceof DraggableGridItem)) {
            throw new IllegalStateException("DraggableGridLayout can host only DraggableGridItem children.");
        }
    }

    private boolean isAnimationRunning() {
        return runningAnimations > 0;
    }

    public void setRearrangeable(boolean rearrangeable) {
        this.isRearrangeable = rearrangeable;

        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);

            if (child != null) {
                ((DraggableGridItem) child).setResizingActive(rearrangeable);
            }
        }
    }

    /**
     * @noinspection ReplaceNullCheck
     */
    public void addItem(DraggableGridItem view, ItemLayoutData defaultTemplateData) {
        ItemLayoutData saved = templateManager.getLayoutForSize(colCount, rowCount).get(view.itemId);
        ItemLayoutData data;

        if (saved != null) {
            data = saved;
        } else if (defaultTemplateData != null) {
            data = defaultTemplateData;
        } else {
            data = new ItemLayoutData(view.itemId, 0, 0, 1, 1);
        }

        view.minColSpan = data.minColSpan;
        view.minRowSpan = data.minRowSpan;
        view.maxColSpan = data.maxColSpan > 0 ? data.maxColSpan : -1;
        view.maxRowSpan = data.maxRowSpan > 0 ? data.maxRowSpan : -1;
        view.setResizingActive(isRearrangeable);

        LayoutParams layoutParams = new LayoutParams(data.col, data.row, data.colSpan, data.rowSpan);
        view.setLayoutParams(layoutParams);

        GridState currentState = buildCurrentState();
        Rect targetRect = new Rect(layoutParams.col, layoutParams.row, layoutParams.col + layoutParams.colSpan, layoutParams.row + layoutParams.rowSpan);

        // Attempt preferred position first
        if (!currentState.isOccupied(targetRect, null)) {
            addView(view);
            saveLayoutState();

            return;
        }

        // Try to rearrange items
        GridState rearranged = attemptGlobalRearrange(currentState, view, layoutParams.colSpan, layoutParams.rowSpan, false);
        if (rearranged != null) {
            commitState(rearranged);
            addView(view);
            saveLayoutState();

            return;
        }

        // Try shrinking and rearranging items
        GridState shrunken = attemptGlobalRearrange(currentState, view, layoutParams.colSpan, layoutParams.rowSpan, true);
        if (shrunken != null) {
            commitState(shrunken);
            addView(view);
            saveLayoutState();

            return;
        }

        // Drop it on the first free spot
        Rect firstFree = findClosestFreeSpotInState(currentState, layoutParams.colSpan, layoutParams.rowSpan, 0, 0, null);
        if (firstFree != null) {
            layoutParams.col = firstFree.left;
            layoutParams.row = firstFree.top;
            addView(view);
            saveLayoutState();

            return;
        }

        // (#-_-)
        Toast.makeText(getContext(), warningMessage, Toast.LENGTH_SHORT).show();
    }

    private Rect findClosestFreeSpotInState(GridState state, int spanX, int spanY,
                                            int preferredCol, int preferredRow, View ignoreView) {
        double bestDistance = Double.MAX_VALUE;
        Rect bestRect = null;

        for (int row = 0; row <= state.rows - spanY; row++) {
            for (int col = 0; col <= state.cols - spanX; col++) {
                Rect candidate = new Rect(col, row, col + spanX, row + spanY);

                if (!state.isOccupied(candidate, ignoreView)) {
                    double distance = Math.sqrt(Math.pow(col - preferredCol, 2)
                            + Math.pow(row - preferredRow, 2));

                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestRect = candidate;
                    }
                }
            }
        }

        return bestRect;
    }

    private GridState attemptGlobalRearrange(GridState currentState, View newItem,
                                             int newSpanX, int newSpanY, boolean allowShrink) {

        DraggableGridItem newDrag = (DraggableGridItem) newItem;
        int minX = newDrag.minColSpan;
        int minY = newDrag.minRowSpan;

        // really, REALLY SLOW, but the cell count is small
        for (int spanX = newSpanX; spanX >= minX; spanX--) {
            for (int spanY = newSpanY; spanY >= minY; spanY--) {

                for (int row = 0; row <= rowCount - spanY; row++) {
                    for (int col = 0; col <= colCount - spanX; col++) {

                        GridState state = currentState.cloneState();
                        Rect target = new Rect(col, row, col + spanX, row + spanY);
                        state.placements.put(newItem, target);

                        GridState solved = resolveAllCollisions(state, newItem,
                                allowShrink, rowCount, colCount);

                        if (solved != null) {
                            return solved;
                        }
                    }
                }
            }
        }

        return null;
    }

    private GridState resolveAllCollisions(GridState state, View newItem, boolean allowShrink,
                                           int rowCount, int colCount) {
        Queue<GridState> queue = new LinkedList<>();
        queue.add(state);

        while (!queue.isEmpty()) {
            GridState current = queue.poll();
            if (current == null) {
                break;
            }

            boolean collisionFound = false;
            boolean stopProcessing = false;

            for (Map.Entry<View, Rect> a : current.placements.entrySet()) {
                if (stopProcessing) {
                    break;
                }

                for (Map.Entry<View, Rect> b : current.placements.entrySet()) {
                    if (a.getKey() != b.getKey()) {
                        Rect ra = a.getValue();
                        Rect rb = b.getValue();

                        if (Rect.intersects(ra, rb)) {
                            collisionFound = true;
                            View victim = b.getKey();

                            if (victim != newItem) {
                                if (allowShrink) {
                                    DraggableGridItem d = (DraggableGridItem) victim;

                                    // Try shrinking width
                                    if (rb.width() > d.minColSpan) {
                                        GridState copy = current.cloneState();
                                        Rect victimRect = copy.placements.get(victim);

                                        if (victimRect != null) {
                                            victimRect.right -= 1;

                                            if (reflowItem(copy, victim, rowCount, colCount)) {
                                                queue.add(copy);
                                            }
                                        }
                                    }

                                    // Try shrinking height
                                    if (rb.height() > d.minRowSpan) {
                                        GridState copy = current.cloneState();
                                        Rect victimRect = copy.placements.get(victim);

                                        if (victimRect != null) {
                                            victimRect.bottom -= 1;

                                            if (reflowItem(copy, victim, rowCount, colCount)) {
                                                queue.add(copy);
                                            }
                                        }
                                    }
                                }

                                GridState movedCopy = current.cloneState();
                                if (reflowItem(movedCopy, victim, rowCount, colCount)) {
                                    queue.add(movedCopy);
                                }
                            }

                            stopProcessing = true;
                        }
                    }
                }
            }

            if (!collisionFound) {
                return current;
            }
        }

        return null;
    }

    private boolean reflowItem(GridState state, View item, int rowCount, int colCount) {
        Rect rect = state.placements.remove(item);
        if (rect == null) {
            return false;
        }

        int spanX = rect.width();
        int spanY = rect.height();

        for (int row = 0; row <= rowCount - spanY; row++) {
            for (int col = 0; col <= colCount - spanX; col++) {

                Rect candidate = new Rect(col, row, col + spanX, row + spanY);
                boolean fits = true;

                for (Rect other : state.placements.values()) {
                    if (Rect.intersects(candidate, other)) {
                        fits = false;
                        break;
                    }
                }

                if (fits) {
                    state.placements.put(item, candidate);
                    return true;
                }
            }
        }

        state.placements.put(item, rect);
        return false;
    }

    public void removeItem(DraggableGridItem view) {
        if (view == null) {
            return;
        }

        removeView(view);
        preAnimVisualPos.remove(view);

        if (activeItem == view) {
            activeItem = null;
            resetHoverState();
        }

        if (currentHoverTarget == view) {
            currentHoverTarget = null;
        }

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
        if (newConfig == null) {
            return;
        }

        boolean changed = false;
        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);

            if (child != null) {
                ItemLayoutData data = newConfig.get(((DraggableGridItem) child).itemId);

                if (data != null) {
                    LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();

                    Rect targetRect = new Rect(
                            data.col,
                            data.row,
                            data.col + data.colSpan,
                            data.row + data.rowSpan
                    );

                    layoutParams.col = data.col;
                    layoutParams.row = data.row;
                    layoutParams.colSpan = data.colSpan;
                    layoutParams.rowSpan = data.rowSpan;

                    List<View> collisions = getCollisions(targetRect, child);

                    if (!collisions.isEmpty()) {
                        boolean shifted = shiftItemsToFreeSpace(collisions, child);

                        if (!shifted) {
                            Rect freeSpot = findClosestFreeSpot(
                                    layoutParams.colSpan,
                                    layoutParams.rowSpan,
                                    layoutParams.col,
                                    layoutParams.row,
                                    child
                            );

                            if (freeSpot != null) {
                                layoutParams.col = freeSpot.left;
                                layoutParams.row = freeSpot.top;
                            } else {
                                layoutParams.col = 0;
                                layoutParams.row = getNextBottomRow();
                            }
                        }
                    }

                    changed = true;
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
                                .setDuration((long) Math.min(Animation.LONG.getDuration(),
                                        Math.max(Animation.SHORT.getDuration(), distance * 0.8)))
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

    @SuppressLint("ClickableViewAccessibility")
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

                    lastReorderCol = originalParams.col;
                    lastReorderRow = originalParams.row;
                    pendingTargetCol = originalParams.col;
                    pendingTargetRow = originalParams.row;

                    activeItem.bringToFront();

                    if (!isResizing) {
                        activeItem.setTag(R.id.is_dragging_tag, true);
                    }

                    for (int index = 0; index < getChildCount(); index++) {
                        View child = getChildAt(index);

                        if (child != null) {
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
                    if (reorderRunnable != null) {
                        handler.removeCallbacks(reorderRunnable);

                        reorderRunnable.run();
                        reorderRunnable = null;
                    }

                    if (isResizing) {
                        finishResize();
                    } else {
                        finishDrag();
                    }

                    for (int index = 0; index < getChildCount(); index++) {
                        View child = getChildAt(index);

                        if (child != null) {
                            ((DraggableGridItem) child).animateToState(DraggableGridItem.STATE_IDLE);
                        }
                    }
                }

                clearCurrentHints();
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
        // Move the view visually
        int parentLeft = getPaddingLeft();
        int parentTop = getPaddingTop();
        int parentRight = getWidth() - getPaddingRight();
        int parentBottom = getHeight() - getPaddingBottom();

        int clampedX = Math.max(parentLeft,
                Math.min(activeItem.getLeft() + (int) dx, parentRight - activeItem.getWidth()));
        int clampedY = Math.max(parentTop,
                Math.min(activeItem.getTop() + (int) dy, parentBottom - activeItem.getHeight()));

        activeItem.offsetLeftAndRight(clampedX - activeItem.getLeft());
        activeItem.offsetTopAndBottom(clampedY - activeItem.getTop());

        // Compute which cell we are hovering over
        LayoutParams layoutParams = (LayoutParams) activeItem.getLayoutParams();
        float relativeLeft = activeItem.getLeft() - parentLeft;
        float relativeTop = activeItem.getTop() - parentTop;

        int targetCol = Math.max(0, Math.min(
                (int) ((relativeLeft + (cellWidth / 2f)) / cellWidth),
                colCount - layoutParams.colSpan
        ));
        int targetRow = Math.max(0, Math.min(
                (int) ((relativeTop + (cellHeight / 2f)) / cellHeight),
                rowCount - layoutParams.rowSpan
        ));

        // Check if we are hovering over a different cell
        // than the one we are currently waiting for
        if (targetCol != pendingTargetCol || targetRow != pendingTargetRow) {
            // If we move, cancel the previous timer
            // Because this block only runs if targetCol or targetRow changes,
            // tiny jitters inside the same cell will NOT trigger this and thus
            // NOT reset the timer.
            if (reorderRunnable != null) {
                handler.removeCallbacks(reorderRunnable);
                reorderRunnable = null;
            }

            clearCurrentHints();

            pendingTargetCol = targetCol;
            pendingTargetRow = targetRow;

            // Don't start a timer if we are just hovering over where the item already is
            if (targetCol == lastReorderCol && targetRow == lastReorderRow) {
                return;
            }

            GridState currentState = buildCurrentState();
            Rect targetRect = new Rect(targetCol, targetRow,
                    targetCol + layoutParams.colSpan, targetRow + layoutParams.rowSpan);
            boolean isSpotOccupied = currentState.isOccupied(targetRect, activeItem);

            if (isSpotOccupied) {
                applyHint(targetCol, targetRow, layoutParams.colSpan, layoutParams.rowSpan);
            }

            reorderRunnable = () -> {
                if (isAnimationRunning()) {
                    clearCurrentHints();
                    return;
                }

                GridState simulated = simulateMove(
                        currentState, activeItem, targetCol, targetRow,
                        layoutParams.colSpan, layoutParams.rowSpan
                );

                if (simulated != null) {
                    clearCurrentHints();

                    if (!isSpotOccupied) {
                        clearVisualNudges();
                    } else {
                        applySimulatedStateVisually(simulated);
                    }

                    commitState(simulated);
                    lastReorderCol = targetCol;
                    lastReorderRow = targetRow;
                }
            };

            handler.postDelayed(reorderRunnable, REORDER_DELAY_MS);
        }
    }

    private void applyHint(int targetCol, int targetRow, int colSpan, int rowSpan) {
        clearCurrentHints();

        Rect targetRect = new Rect(targetCol, targetRow, targetCol + colSpan, targetRow + rowSpan);

        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);

            if (child != activeItem) {
                LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
                Rect childRect = new Rect(layoutParams.col, layoutParams.row,
                        layoutParams.col + layoutParams.colSpan, layoutParams.row + layoutParams.rowSpan);

                if (Rect.intersects(targetRect, childRect)) {
                    hintedViews.add(child);
                    child.animate()
                            .scaleX(HINT_SCALE)
                            .scaleY(HINT_SCALE)
                            .alpha(HINT_ALPHA)
                            .setDuration(Animation.MEDIUM.getDuration())
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                }
            }
        }
    }

    private void clearCurrentHints() {
        for (View view : hintedViews) {
            view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1f)
                    .setDuration(Animation.EXTENDED.getDuration())
                    .start();
        }

        hintedViews.clear();
    }

    private void clearVisualNudges() {
        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);

            if (child != activeItem) {
                if (child.getTranslationX() != 0 || child.getTranslationY() != 0) {
                    child.animate()
                            .translationX(0)
                            .translationY(0)
                            .setDuration(Animation.SHORT.getDuration())
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                }
            }
        }
    }

    private void applySimulatedStateVisually(GridState state) {
        for (Map.Entry<View, Rect> entry : state.placements.entrySet()) {
            View view = entry.getKey();

            if (view != activeItem) {
                Rect targetRect = entry.getValue();
                int targetLeft = getPaddingLeft() + (targetRect.left * cellWidth);
                int targetTop = getPaddingTop() + (targetRect.top * cellHeight);

                int dx = targetLeft - view.getLeft();
                int dy = targetTop - view.getTop();

                runningAnimations++;
                view.animate()
                        .translationX(dx)
                        .translationY(dy)
                        .setDuration(Animation.MEDIUM.getDuration())
                        .setInterpolator(new DecelerateInterpolator())
                        .setListener(new AnimatorListenerAdapter() {

                            boolean handled = false;

                            private void finish() {
                                if (!handled) {
                                    handled = true;
                                    runningAnimations--;
                                }
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                finish();
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                                finish();
                            }
                        })
                        .start();
            }
        }
    }

    private void commitState(GridState state) {
        captureLayoutState();

        for (Map.Entry<View, Rect> entry : state.placements.entrySet()) {
            LayoutParams layoutParams = (LayoutParams) entry.getKey().getLayoutParams();

            layoutParams.col = entry.getValue().left;
            layoutParams.row = entry.getValue().top;
            layoutParams.colSpan = entry.getValue().width();
            layoutParams.rowSpan = entry.getValue().height();
        }

        requestLayout();
        saveLayoutState();
    }

    private void finishDrag() {
        if (activeItem == null) {
            return;
        }

        LayoutParams layoutParams = (LayoutParams) activeItem.getLayoutParams();

        int parentLeft = getPaddingLeft();
        int parentTop = getPaddingTop();

        float relativeLeft = activeItem.getLeft() - parentLeft;
        float relativeTop = activeItem.getTop() - parentTop;

        int rawCol = (int) ((relativeLeft + (cellWidth / 2f)) / cellWidth);
        int rawRow = (int) ((relativeTop + (cellHeight / 2f)) / cellHeight);

        int targetCol = Math.max(0, Math.min(rawCol, colCount - layoutParams.colSpan));
        int targetRow = Math.max(0, Math.min(rawRow, rowCount - layoutParams.rowSpan));

        GridState currentState = buildCurrentState();

        GridState simulated = simulateMove(
                currentState,
                activeItem,
                targetCol,
                targetRow,
                layoutParams.colSpan,
                layoutParams.rowSpan
        );

        if (simulated != null) {
            commitState(simulated);
        }

        activeItem.setTag(R.id.is_dragging_tag, null);
        captureLayoutState();

        activeItem = null;

        requestLayout();
        saveLayoutState();
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
                    .setDuration(Animation.MEDIUM.getDuration())
                    .start();

            currentHoverTarget = null;
        }
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

                    List<View> others = getCollisions(candidate, ignoreView);
                    if (!others.isEmpty()) {
                        collides = true;
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
        Map<String, ItemLayoutData> existing =
                templateManager.getLayoutForSize(colCount, rowCount);

        if (existing == null) {
            existing = new HashMap<>();
        }

        for (int index = 0; index < getChildCount(); index++) {
            DraggableGridItem childItem = (DraggableGridItem) getChildAt(index);
            LayoutParams layoutParams = (LayoutParams) childItem.getLayoutParams();

            ItemLayoutData data = new ItemLayoutData(
                    childItem.itemId,
                    layoutParams.col,
                    layoutParams.row,
                    layoutParams.colSpan,
                    layoutParams.rowSpan
            );

            data.minColSpan = childItem.minColSpan;
            data.minRowSpan = childItem.minRowSpan;
            data.maxColSpan = childItem.maxColSpan;
            data.maxRowSpan = childItem.maxRowSpan;

            existing.put(childItem.itemId, data);
        }

        templateManager.saveUserLayout(colCount, rowCount, existing);
    }

    private GridState simulateMove(GridState initialState, View activeItem, int targetCol, int targetRow, int spanX, int spanY) {
        GridState state = initialState.cloneState();
        Rect targetRect = new Rect(targetCol, targetRow, targetCol + spanX, targetRow + spanY);

        if (targetRect.right > state.cols || targetRect.bottom > state.rows) return null;

        state.placements.remove(activeItem); // Prevent self-collision logic
        List<View> collisions = state.getCollisions(targetRect, null);
        state.placements.put(activeItem, targetRect); // Claim space for dragged item

        if (collisions.isEmpty()) return state;

        // Resolve overlaps deterministically
        for (View victim : collisions) {
            if (!resolveCollision(state, victim, targetCol, targetRow)) {
                return null; // Dead end, move is invalid
            }
        }
        return state;
    }

    private boolean resolveCollision(GridState state, View victim, int activeCol, int activeRow) {
        Rect victimRect = state.placements.get(victim);
        if (victimRect == null) {
            return false;
        }

        int pushX = Integer.compare(activeCol - victimRect.left, 0);
        int pushY = Integer.compare(activeRow - victimRect.top, 0);

        int[][] directions;
        if (Math.abs(pushX) > Math.abs(pushY)) {
            directions = new int[][]{
                    {-1, 0}, // prefer left movement
                    {pushX, 0},
                    {0, pushY},
                    {0, -pushY},
                    {-pushX, 0}
            };
        } else {
            directions = new int[][]{
                    {0, 1}, // prefer down movement
                    {0, pushY},
                    {pushX, 0},
                    {-pushX, 0},
                    {0, -pushY}
            };
        }

        for (int[] dir : directions) {
            if (dir[0] != 0 || dir[1] != 0) {
                Rect testRect = new Rect(
                        victimRect.left + dir[0], victimRect.top + dir[1],
                        victimRect.right + dir[0], victimRect.bottom + dir[1]
                );

                if (!state.isOccupied(testRect, victim)) {
                    state.placements.put(victim, testRect);

                    return true;
                }
            }
        }

        // Fallback
        Rect closest = findClosestFreeSpotInState(state, victimRect.width(),
                victimRect.height(), victimRect.left, victimRect.top, victim);
        if (closest != null) {
            state.placements.put(victim, closest);

            return true;
        }

        return false;
    }

    private GridState buildCurrentState() {
        GridState state = new GridState(colCount, rowCount);

        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);
            LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();

            state.placements.put(child, new Rect(layoutParams.col, layoutParams.row,
                    layoutParams.col + layoutParams.colSpan, layoutParams.row + layoutParams.rowSpan));
        }

        return state;
    }

    private static class GridState {
        public final Map<View, Rect> placements = new HashMap<>();
        public final int cols;
        public final int rows;

        public GridState(int cols, int rows) {
            this.cols = cols;
            this.rows = rows;
        }

        public GridState cloneState() {
            GridState copy = new GridState(cols, rows);
            copy.placements.putAll(this.placements);

            return copy;
        }

        public List<View> getCollisions(Rect targetRect, View ignoreView) {
            List<View> collisions = new ArrayList<>();

            for (Map.Entry<View, Rect> entry : placements.entrySet()) {
                if (entry.getKey() != ignoreView
                        && Rect.intersects(targetRect, entry.getValue())) {
                    collisions.add(entry.getKey());
                }
            }

            return collisions;
        }

        public boolean isOccupied(Rect rect, View ignoreView) {
            if (rect.left < 0 || rect.top < 0
                    || rect.right > cols || rect.bottom > rows) {
                return true;
            }

            return !getCollisions(rect, ignoreView).isEmpty();
        }
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