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

package com.stario.launcher.sheet.behavior.right;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.math.MathUtils;
import androidx.core.view.ViewCompat;
import androidx.customview.widget.ViewDragHelper;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.stario.launcher.sheet.behavior.SheetBehavior;
import com.stario.launcher.sheet.behavior.SheetDragHelper;
import com.stario.launcher.ui.Measurements;

public class RightSheetBehavior<V extends View> extends SheetBehavior<V> {
    private Boolean rememberInterceptResult = null;
    private boolean flung;
    private int initialY;

    public RightSheetBehavior(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void calculateCollapsedOffset() {
        collapsedOffset = expandedOffset + Measurements.dpToPx(SheetBehavior.COLLAPSED_DELTA_DP);
    }

    @Override
    protected void calculateExpandedOffset() {
        if (viewRef != null && viewRef.get() != null) {
            expandedOffset = parentWidth - viewRef.get().getMeasuredWidth();
        }
    }

    @Override
    protected void dispatchOnSlide(View child) {
        dispatchOnSlide(child.getLeft());
    }

    @Override
    protected int getPositionInParent(View child) {
        return child.getLeft();
    }

    @Override
    protected void offset(View child, int offset) {
        ViewCompat.offsetLeftAndRight(child, offset);
    }

    @Override
    protected void stopNestedScrollLogic(View child) {
        if (flung) {
            return;
        }

        if (child.getLeft() == expandedOffset) {
            setStateInternal(STATE_EXPANDED);
            return;
        }

        int left;
        int targetState;
        int currentLeft = child.getLeft();

        if (lastNestedScroll > 0) {
            left = expandedOffset;
            targetState = STATE_EXPANDED;
        } else if (lastNestedScroll == 0) {
            if (currentLeft < collapsedOffset * 0.5 +
                    expandedOffset * 0.5) {
                left = expandedOffset;
                targetState = STATE_EXPANDED;
            } else {
                left = collapsedOffset;
                targetState = STATE_COLLAPSED;
            }
        } else {
            left = collapsedOffset;
            targetState = STATE_COLLAPSED;
        }

        settleChildTo(child, targetState, left, child.getTop());
    }

    @Override
    protected boolean nestedPreFlingLogic(V child, float xvel, float yvel) {
        int currentLeft = child.getLeft();

        if (state == STATE_DRAGGING) {
            if (xvel > 0 && currentLeft < expandedOffset) {
                settleChildTo(child, STATE_EXPANDED, child.getLeft(), expandedOffset, (int) xvel, 0);

                flung = true;
            } else if (xvel < 0 && currentLeft > collapsedOffset) {
                settleChildTo(child, STATE_COLLAPSED, child.getLeft(), collapsedOffset, (int) xvel, 0);

                flung = true;
            }
        }

        return flung;
    }

    protected void nestedPreScrollLogic(View child, View target, int dx, int dy, int[] consumed) {
        int currentLeft = child.getLeft();
        int newLeft = currentLeft - dx;

        if (dx > 0) { // Left
            if (newLeft < expandedOffset) {
                consumed[1] = currentLeft - expandedOffset;
                ViewCompat.offsetLeftAndRight(child, -consumed[1]);
                setStateInternal(STATE_EXPANDED);
            } else {
                if (!draggable) {
                    // Prevent dragging
                    return;
                }

                consumed[1] = dx;
                ViewCompat.offsetLeftAndRight(child, -dx);
                setStateInternal(STATE_DRAGGING);
            }
        } else if (dx < 0) { // Right
            if (!target.canScrollHorizontally(-1)) {
                if (newLeft <= collapsedOffset) {
                    if (!draggable) {
                        // Prevent dragging
                        return;
                    }

                    consumed[1] = dx;
                    ViewCompat.offsetLeftAndRight(child, -dx);
                    setStateInternal(STATE_DRAGGING);
                } else {
                    consumed[1] = currentLeft - collapsedOffset;
                    ViewCompat.offsetLeftAndRight(child, -consumed[1]);
                    setStateInternal(STATE_COLLAPSED);
                }
            }
        }

        lastNestedScroll = dx;
    }

    @Override
    protected boolean startNestedScrollLogic(int axes) {
        flung = true;

        return (axes & ViewCompat.SCROLL_AXIS_HORIZONTAL) != 0;
    }

    @Override
    protected void touchEventLogic(View child, MotionEvent event) {
        if (dragHelper != null && Math.abs(initial - event.getX()) > dragHelper.getTouchSlop()) {
            dragHelper.captureChildView(child, event.getPointerId(event.getActionIndex()));
        }
    }

    @Override
    protected boolean interceptTouchEventLogic(CoordinatorLayout parent, View child, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                activePointerId = MotionEvent.INVALID_POINTER_ID;

                // Reset the ignore flag
                if (ignoreEvents) {
                    ignoreEvents = false;
                    return false;
                }

                break;
            }

            case MotionEvent.ACTION_DOWN: {
                initial = (int) event.getX();
                initialY = (int) event.getY();
                rememberInterceptResult = null;

                // Only intercept nested scrolling events here if the view not being moved by the
                // ViewDragHelper.
                if (state != STATE_SETTLING) {
                    View scroll = nestedScrollingChildRef != null ? nestedScrollingChildRef.get() : null;

                    if (scroll != null && parent.isPointInChildBounds(scroll, initial, initialY)) {
                        activePointerId = event.getPointerId(event.getActionIndex());
                    }
                }

                ignoreEvents = activePointerId == MotionEvent.INVALID_POINTER_ID
                        && !parent.isPointInChildBounds(child, initial, initialY);

                break;
            }
        }

        float absX = Math.abs(initial - event.getX());
        float absY = Math.abs(initialY - event.getY());

        if (dragHelper != null) {
            if (rememberInterceptResult == null &&
                    (absX > dragHelper.getTouchSlop() || absY > dragHelper.getTouchSlop())) {
                rememberInterceptResult = absX > absY;
            }

            return rememberInterceptResult != null && rememberInterceptResult &&
                    initial - event.getX() < 0 && absX > dragHelper.getTouchSlop();
        }

        return false;
    }

    @Override
    protected SheetDragHelper.Callback instantiateDragCallback() {
        return new SheetDragHelper.Callback() {

            @Override
            public boolean tryCaptureView(@NonNull View child, int pointerId) {
                if (capture) {
                    return true;
                }

                if (state == STATE_DRAGGING) {
                    return false;
                }

                if (state == STATE_EXPANDED && activePointerId == pointerId) {
                    ViewPager pager = pagerRef != null ? pagerRef.get() : null;

                    if (pager != null) {
                        PagerAdapter adapter = pager.getAdapter();

                        if (adapter != null && pager.getCurrentItem() > 0) {
                            return false;
                        }
                    }

                    View scroll = nestedScrollingChildRef != null ? nestedScrollingChildRef.get() : null;

                    if (scroll != null && scroll.canScrollHorizontally(-1)) {
                        return false;
                    }
                }

                return viewRef != null && viewRef.get() == child;
            }

            @Override
            public void onViewPositionChanged(
                    @NonNull View changedView,
                    int left,
                    int top,
                    int dx,
                    int dy
            ) {
                dispatchOnSlide(left);
            }

            @Override
            public void onViewDragStateChanged(int state) {
                if (state == ViewDragHelper.STATE_DRAGGING && draggable) {
                    setStateInternal(STATE_DRAGGING);
                }
            }

            @Override
            public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
                int left;
                @SheetBehavior.State int targetState;

                if (xvel == 0.f ||
                        Measurements.dpToPx(ViewConfiguration.getMinimumFlingVelocity() +
                                (ViewConfiguration.getMaximumFlingVelocity() -
                                        ViewConfiguration.getMinimumFlingVelocity()) / 10f) > Math.abs(xvel)) {
                    int currentLeft = releasedChild.getLeft();

                    if (currentLeft < (expandedOffset + collapsedOffset) / 2) {
                        left = expandedOffset;
                        targetState = STATE_EXPANDED;
                    } else {
                        left = collapsedOffset;
                        targetState = STATE_COLLAPSED;
                    }
                } else if (xvel < 0) { // Moving left
                    left = expandedOffset;
                    targetState = STATE_EXPANDED;
                } else { // Moving right
                    left = collapsedOffset;
                    targetState = STATE_COLLAPSED;
                }

                settleChildTo(releasedChild, targetState, left, releasedChild.getTop());
            }

            @Override
            public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
                return MathUtils.clamp(
                        left, expandedOffset, collapsedOffset
                );
            }

            @Override
            public int getViewHorizontalDragRange(@NonNull View child) {
                return collapsedOffset;
            }
        };
    }

    protected void settleToState(@NonNull View child, int state, boolean animate) {
        int left;

        if (state == STATE_COLLAPSED) {
            left = collapsedOffset;
        } else if (state == STATE_EXPANDED) {
            left = expandedOffset;
        } else {
            throw new IllegalArgumentException("Illegal state argument: " + state);
        }

        if (animate) {
            settleChildTo(child, state, left, child.getTop());
        } else {
            moveChildTo(child, state, left, child.getTop());
        }
    }
}
