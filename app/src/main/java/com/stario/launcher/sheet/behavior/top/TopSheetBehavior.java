/*
    Copyright (C) 2015 The Android Open Source Project
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

package com.stario.launcher.sheet.behavior.top;

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

import com.stario.launcher.sheet.behavior.SheetBehavior;
import com.stario.launcher.sheet.behavior.SheetDragHelper;
import com.stario.launcher.ui.measurements.Measurements;

public class TopSheetBehavior<V extends View> extends SheetBehavior<V> {
    private Boolean rememberInterceptResult = null;
    private boolean touchingScrollingChild;
    private int initialX;

    public TopSheetBehavior(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void calculateCollapsedOffset() {
        collapsedOffset = expandedOffset - Measurements.dpToPx(SheetBehavior.COLLAPSED_DELTA_DP);
    }

    @Override
    protected void calculateHalfExpandedOffset() {
        halfExpandedOffset = (int) (collapsedOffset * (1 - halfExpandedRatio) +
                expandedOffset * halfExpandedRatio);
    }

    @Override
    protected void calculateExpandedOffset() {
        expandedOffset = 0;
    }

    @Override
    protected void dispatchOnSlide(V child) {
        dispatchOnSlide(child.getTop());
    }

    @Override
    protected int getPositionInParent(V child) {
        return child.getTop();
    }

    @Override
    protected void offset(V child, int offset) {
        ViewCompat.offsetTopAndBottom(child, offset);
    }

    @Override
    protected void stopNestedScrollLogic(V child, View target) {
        if (child.getTop() == expandedOffset) {
            setStateInternal(STATE_EXPANDED);
            return;
        }

        int bottom;
        int targetState;
        int currentBottom = child.getBottom();

        if (lastNestedScroll < 0) { // Down - towards expanding.
            bottom = expandedOffset;
            targetState = STATE_EXPANDED;
        } else if (lastNestedScroll == 0) {
            if (currentBottom > halfExpandedOffset) {
                bottom = expandedOffset;
                targetState = STATE_EXPANDED;
            } else {
                bottom = collapsedOffset;
                targetState = STATE_COLLAPSED;
            }
        } else { // Up - towards collapsing
            bottom = collapsedOffset;
            targetState = STATE_COLLAPSED;
        }

        startSettling(child, targetState, child.getLeft(), bottom, true);
    }

    @Override
    protected void nestedPreScrollLogic(V child, View target, int dx, int dy, int[] consumed) {
        int currentBottom = child.getBottom();
        int newBottom = currentBottom - dy;

        if (dy > 0) { // Upward - Collapsing the top sheet!
            if (!target.canScrollVertically(1)) {
                if (newBottom >= collapsedOffset) {
                    consumed[1] = dy;
                    ViewCompat.offsetTopAndBottom(child, -dy);
                    setStateInternal(STATE_DRAGGING);
                } else {
                    consumed[1] = currentBottom - collapsedOffset;
                    ViewCompat.offsetTopAndBottom(child, -consumed[1]);
                    setStateInternal(STATE_COLLAPSED);
                }
            }
        } else if (dy < 0) { // Downward
            if (newBottom >= expandedOffset + parentHeight) {
                consumed[1] = currentBottom - expandedOffset - parentHeight;
                ViewCompat.offsetTopAndBottom(child, -consumed[1]);
                setStateInternal(STATE_EXPANDED);
            } else {
                consumed[1] = dy;
                ViewCompat.offsetTopAndBottom(child, -dy);
                setStateInternal(STATE_DRAGGING);
            }
        }

        lastNestedScroll = dy;
    }

    @Override
    protected boolean startNestedScrollLogic(int axes) {
        return (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    protected void touchEventLogic(V child, MotionEvent event) {
        if (dragHelper != null && Math.abs(initial - event.getY()) > dragHelper.getTouchSlop()) {
            dragHelper.captureChildView(child, event.getPointerId(event.getActionIndex()));
        }
    }

    @Override
    protected boolean interceptTouchEventLogic(CoordinatorLayout parent, V child, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                touchingScrollingChild = false;
                activePointerId = MotionEvent.INVALID_POINTER_ID;

                // Reset the ignore flag
                if (ignoreEvents) {
                    ignoreEvents = false;
                    return false;
                }

                break;
            }
            case MotionEvent.ACTION_DOWN: {
                initialX = (int) event.getX();
                initial = (int) event.getY();
                rememberInterceptResult = null;

                // Only intercept nested scrolling events here if the view not being moved by the
                // ViewDragHelper.
                if (state != STATE_SETTLING) {
                    View scroll = nestedScrollingChildRef != null ? nestedScrollingChildRef.get() : null;

                    if (scroll != null && parent.isPointInChildBounds(scroll, initialX, initial)) {
                        activePointerId = event.getPointerId(event.getActionIndex());
                        touchingScrollingChild = true;
                    }
                }

                ignoreEvents = activePointerId == MotionEvent.INVALID_POINTER_ID
                        && !parent.isPointInChildBounds(child, initialX, initial);

                break;
            }
        }

        float absX = Math.abs(initialX - event.getX());
        float absY = Math.abs(initial - event.getY());

        if (dragHelper != null) {
            if (rememberInterceptResult == null &&
                    (absX > dragHelper.getTouchSlop() || absY > dragHelper.getTouchSlop())) {
                rememberInterceptResult = absX < absY;
            }

            return rememberInterceptResult != null && rememberInterceptResult &&
                    initial - event.getY() > 0 && absY > dragHelper.getTouchSlop();
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

                if (touchingScrollingChild) {
                    return false;
                }

                if (state == STATE_EXPANDED && activePointerId == pointerId) {
                    View scroll = nestedScrollingChildRef != null ? nestedScrollingChildRef.get() : null;
                    if (true || (scroll != null && scroll.canScrollVertically(-1))) {
                        // Let the content scroll up
                        return false;
                    }
                }

                return viewRef != null && viewRef.get() == child;
            }

            @Override
            public void onViewPositionChanged(
                    @NonNull View changedView, int left, int top, int dx, int dy) {
                dispatchOnSlide(top);
            }

            @Override
            public void onViewDragStateChanged(int state) {
                if (state == ViewDragHelper.STATE_DRAGGING) {
                    setStateInternal(STATE_DRAGGING);
                }
            }

            @Override
            public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
                int bottom;
                @SheetBehavior.State int targetState;

                if (yvel == 0.f ||
                        Measurements.dpToPx(ViewConfiguration.getMinimumFlingVelocity() +
                                (ViewConfiguration.getMaximumFlingVelocity() -
                                        ViewConfiguration.getMinimumFlingVelocity()) / 10f) > Math.abs(yvel)) {
                    int currentBottom = releasedChild.getBottom();

                    if (currentBottom > -collapsedOffset / 2) {
                        bottom = expandedOffset;
                        targetState = STATE_EXPANDED;
                    } else {
                        bottom = collapsedOffset;
                        targetState = STATE_COLLAPSED;
                    }
                } else if (yvel < 0) { // Moving Down
                    bottom = collapsedOffset;
                    targetState = STATE_COLLAPSED;
                } else { // Moving Up
                    bottom = expandedOffset;
                    targetState = STATE_EXPANDED;
                }

                startSettling(releasedChild, targetState, releasedChild.getLeft(), bottom - expandedOffset, true);
            }

            @Override
            public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
                return MathUtils.clamp(
                        top, collapsedOffset, expandedOffset);
            }

            @Override
            public int getViewVerticalDragRange(@NonNull View child) {
                return -collapsedOffset;
            }
        };
    }

    protected void settleToState(@NonNull View child, int state, boolean animate) {
        int top;

        if (state == STATE_COLLAPSED) {
            top = collapsedOffset;
        } else if (state == STATE_HALF_EXPANDED) {
            top = halfExpandedOffset;
        } else if (state == STATE_EXPANDED) {
            top = expandedOffset;
        } else {
            throw new IllegalArgumentException("Illegal state argument: " + state);
        }

        startSettling(child, state, child.getLeft(), top, animate);
    }
}
