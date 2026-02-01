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

package com.stario.launcher.sheet.behavior;

import android.content.Context;
import android.graphics.Rect;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams;
import androidx.core.view.ViewCompat;
import androidx.customview.widget.ViewDragHelper;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * An interaction behavior plugin for a child view of {@link CoordinatorLayout} to make it work as a sheet.
 */
public abstract class SheetBehavior<V extends View> extends CoordinatorLayout.Behavior<V> {

    /**
     * Callback for monitoring events about sheets.
     */
    public interface SheetCallback {

        /**
         * Called when the sheet changes its state.
         *
         * @param sheet    The sheet view.
         * @param newState The new state. This will be one of {@link #STATE_DRAGGING}, {@link
         *                 #STATE_SETTLING}, {@link #STATE_EXPANDED} or {@link #STATE_COLLAPSED}.
         */
        default void onStateChanged(@NonNull View sheet, @State int newState) {
        }

        /**
         * Called when the sheet changes its state.
         *
         * @param sheet         The sheet view.
         * @param stateToSettle The state to settle to. This will be one of {@link #STATE_EXPANDED}
         *                      or {@link #STATE_COLLAPSED}.
         */
        default void onSettleToState(@NonNull View sheet, @State int stateToSettle) {
        }

        /**
         * Called when the sheet is being dragged.
         *
         * @param sheet       The sheet view.
         * @param slideOffset The new offset of this sheet within [-1,1] range. Offset increases
         *                    as this sheet is moving. From 0 to 1 the sheet is between collapsed and
         *                    expanded states and from -1 to 0 it is between hidden and collapsed states.
         */
        default void onSlide(@NonNull View sheet, float slideOffset) {
        }
    }

    /**
     * The sheet is dragging.
     */
    public static final int STATE_DRAGGING = 1;

    /**
     * The sheet is settling.
     */
    public static final int STATE_SETTLING = 2;

    /**
     * The sheet is expanded.
     */
    public static final int STATE_EXPANDED = 3;

    /**
     * The sheet is collapsed.
     */
    public static final int STATE_COLLAPSED = 4;

    @IntDef({
            STATE_EXPANDED,
            STATE_COLLAPSED,
            STATE_DRAGGING,
            STATE_SETTLING
    })

    @Retention(RetentionPolicy.SOURCE)
    public @interface State {
    }

    /**
     * Offset difference between EXPANDED and COLLAPSED state in dp
     */
    public static final int COLLAPSED_DELTA_DP = 350;

    private SettleRunnable settleRunnable;
    protected int expandedOffset;
    protected int collapsedOffset;
    @State
    protected int state = STATE_COLLAPSED;
    @Nullable
    protected SheetDragHelper dragHelper;
    protected boolean ignoreEvents;
    protected int lastNestedScroll;
    private boolean nestedScrolled;
    protected int parentWidth;
    protected int parentHeight;
    @Nullable
    protected WeakReference<V> viewRef;
    @Nullable
    protected WeakReference<ViewPager> pagerRef;
    @Nullable
    protected WeakReference<View> nestedScrollingChildRef;
    @NonNull
    protected final ArrayList<SheetCallback> callbacks = new ArrayList<>();
    protected boolean draggable;
    protected boolean capture;
    protected int activePointerId;
    protected int initial;

    public SheetBehavior() {
        init();
    }

    public SheetBehavior(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    private void init() {
        this.capture = false;
        this.draggable = true;
        this.settleRunnable = null;
    }

    @Override
    public void onRestoreInstanceState(@NonNull CoordinatorLayout parent, @NonNull V child, @NonNull Parcelable state) {
        SavedSheetState savedSheetState = (SavedSheetState) state;

        if (savedSheetState.getSuperState() != null) {
            super.onRestoreInstanceState(parent, child, savedSheetState.getSuperState());

            // Intermediate states are restored as collapsed state
            if (savedSheetState.state == STATE_DRAGGING || savedSheetState.state == STATE_SETTLING) {
                this.state = STATE_COLLAPSED;
            } else {
                this.state = savedSheetState.state;
            }
        } else {
            super.onRestoreInstanceState(parent, child, state);
        }
    }

    @NonNull
    @Override
    public Parcelable onSaveInstanceState(@NonNull CoordinatorLayout parent, @NonNull V child) {
        return new SavedSheetState(super.onSaveInstanceState(parent, child), state);
    }

    @Override
    public void onAttachedToLayoutParams(@NonNull LayoutParams layoutParams) {
        super.onAttachedToLayoutParams(layoutParams);

        // These may already be null, but just be safe, explicitly assign them. This lets us know the
        // first time we layout with this behavior by checking (viewRef == null).
        viewRef = null;
        dragHelper = null;
    }

    @Override
    public void onDetachedFromLayoutParams() {
        super.onDetachedFromLayoutParams();

        // Release references so we don't run unnecessary code paths while not attached to a view.
        viewRef = null;
        dragHelper = null;
    }

    @Override
    public boolean onLayoutChild(
            @NonNull CoordinatorLayout parent, @NonNull V child, int layoutDirection) {
        if (ViewCompat.getFitsSystemWindows(parent) && !ViewCompat.getFitsSystemWindows(child)) {
            child.setFitsSystemWindows(true);
        }

        if (viewRef == null) {
            // First layout with this behavior.
            viewRef = new WeakReference<>(child);

            if (ViewCompat.getImportantForAccessibility(child)
                    == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
                ViewCompat.setImportantForAccessibility(child, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
            }
        }

        // look for view pager switching
        if (pagerRef == null || pagerRef.get() == null) {
            ViewPager pager = findPager(child);

            if (pager != null) {
                pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageScrollStateChanged(int state) {
                        if (state == ViewPager.SCROLL_STATE_IDLE) {
                            View target = findNestedScrollingChild(pager);

                            if (target != null) {
                                nestedScrollingChildRef = new WeakReference<>(target);
                            }
                        }
                    }
                });

                pagerRef = new WeakReference<>(pager);
            }
        }

        View target = (nestedScrollingChildRef == null ||
                nestedScrollingChildRef.get() == null ||
                !nestedScrollingChildRef.get().isShown())
                ? findNestedScrollingChild(child)
                : null;

        if (target != null) {
            nestedScrollingChildRef = new WeakReference<>(target);
        }


        if (dragHelper == null) {
            dragHelper = SheetDragHelper.create(parent, instantiateDragCallback());
        }

        int saved = getPositionInParent(child);

        // First let the parent lay it out
        parent.onLayoutChild(child, layoutDirection);

        // Offset the sheet
        parentWidth = parent.getWidth();
        parentHeight = parent.getHeight();

        calculateExpandedOffset();
        calculateCollapsedOffset();

        if (state == STATE_EXPANDED) {
            offset(child, expandedOffset);
        } else if (state == STATE_COLLAPSED) {
            offset(child, collapsedOffset);
        } else if (state == STATE_DRAGGING || state == STATE_SETTLING) {
            offset(child, saved - getPositionInParent(child));
        }

        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(
            @NonNull CoordinatorLayout parent, @NonNull V child, @NonNull MotionEvent event) {
        if (!draggable) {
            ignoreEvents = true;
            return false;
        }

        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN) {
            reset();
        }

        boolean shouldIntercept = interceptTouchEventLogic(parent, child, event);

        if (dragHelper != null) {
            boolean viewDragHelperIntercept = dragHelper.shouldInterceptTouchEvent(event);

            if (!ignoreEvents && shouldIntercept && viewDragHelperIntercept) {
                return true;
            }
        }

        if (state != STATE_COLLAPSED && state != STATE_EXPANDED) {
            return true;
        }

        // We have to handle cases that the ViewDragHelper does not capture the sheet because
        // it is not the top most view of its parent. This is not necessary when the touch event is
        // happening over the scrolling content as nested scrolling logic handles that case.
        View scroll = nestedScrollingChildRef != null ? nestedScrollingChildRef.get() : null;

        return action == MotionEvent.ACTION_MOVE
                && scroll != null
                && !ignoreEvents
                && state != STATE_DRAGGING
                && !parent.isPointInChildBounds(scroll, (int) event.getX(), (int) event.getY())
                && dragHelper != null
                && shouldIntercept;
    }

    @Override
    public boolean onTouchEvent(
            @NonNull CoordinatorLayout parent, @NonNull V child, @NonNull MotionEvent event) {
        int action = event.getActionMasked();

        if (state == STATE_DRAGGING && action == MotionEvent.ACTION_DOWN) {
            return true;
        }

        if (dragHelper != null) {
            dragHelper.processTouchEvent(event);
        }

        if (action == MotionEvent.ACTION_DOWN) {
            reset();
        }

        // The ViewDragHelper tries to capture only the top-most View. We have to explicitly tell it
        // to capture the sheet in case it is not captured and the touch slop is passed.
        if (action == MotionEvent.ACTION_MOVE && !ignoreEvents) {
            touchEventLogic(child, event);
        }

        return !ignoreEvents;
    }

    @Override
    public boolean onStartNestedScroll(
            @NonNull CoordinatorLayout coordinatorLayout,
            @NonNull V child,
            @NonNull View directTargetChild,
            @NonNull View target,
            int axes,
            int type) {

        lastNestedScroll = 0;
        nestedScrolled = false;

        return startNestedScrollLogic(axes);
    }

    @Override
    public void onNestedPreScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child,
                                  @NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {

        if (type == ViewCompat.TYPE_NON_TOUCH) {
            // Ignore fling here. The ViewDragHelper handles it.
            return;
        }

        View scrollingChild = nestedScrollingChildRef != null ? nestedScrollingChildRef.get() : null;
        if (target != scrollingChild) {
            return;
        }

        nestedPreScrollLogic(child, target, dx, dy, consumed);

        dispatchOnSlide(child);
        nestedScrolled = true;
    }

    @Override
    public void onStopNestedScroll(@NonNull CoordinatorLayout coordinatorLayout,
                                   @NonNull V child, @NonNull View target, int type) {
        if (nestedScrollingChildRef == null
                || target != nestedScrollingChildRef.get()
                || !nestedScrolled) {
            return;
        }

        stopNestedScrollLogic(child);

        nestedScrolled = false;
    }

    @Override
    public boolean onNestedPreFling(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child,
                                    @NonNull View target, float velocityX, float velocityY) {
        return nestedScrollingChildRef != null &&
                target == nestedScrollingChildRef.get() &&
                nestedPreFlingLogic(child, velocityX, velocityY);
    }

    @Override
    public void onNestedScroll(
            @NonNull CoordinatorLayout coordinatorLayout,
            @NonNull V child,
            @NonNull View target,
            int dxConsumed,
            int dyConsumed,
            int dxUnconsumed,
            int dyUnconsumed,
            int type,
            @NonNull int[] consumed) {
        // Overridden to prevent the default consumption of the entire scroll distance.
    }

    /**
     * Adds a callback to be notified of sheet events.
     *
     * @param callback The callback to notify when sheet events occur.
     */
    public void addSheetCallback(@NonNull SheetCallback callback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }

    /**
     * Removes a previously added callback.
     *
     * @param callback The callback to remove.
     */
    public void removeSheetCallback(@NonNull SheetCallback callback) {
        callbacks.remove(callback);
    }

    /**
     * Sets the state of the sheet. The sheet will transition to that state with
     * animation.
     *
     * @param state {@link #STATE_COLLAPSED} or {@link #STATE_EXPANDED}.
     */
    public void setState(@State int state) {
        setState(state, true);
    }

    /**
     * Sets the state of the sheet. The sheet will transition to that state with
     * or without an animation.
     *
     * @param state {@link #STATE_COLLAPSED} or {@link #STATE_EXPANDED}.
     */
    public void setState(@State int state, boolean animate) {
        if (viewRef == null) {
            // The view is not laid out yet; modify mState and let onLayoutChild handle it later
            if (state == STATE_COLLAPSED
                    || state == STATE_EXPANDED) {
                this.state = state;
            }

            return;
        }

        settleToStatePendingLayout(state, animate);
    }

    private void settleToStatePendingLayout(@State int state, boolean animate) {
        if (viewRef != null) {
            final V child = viewRef.get();

            if (child == null) {
                return;
            }

            // Start the animation; wait until a pending layout if there is one.
            ViewParent parent = child.getParent();

            if (parent != null && parent.isLayoutRequested() && ViewCompat.isAttachedToWindow(child)) {
                child.post(() -> settleToState(child, state, animate));
            } else {
                settleToState(child, state, animate);
            }
        }
    }

    private void notifySettleToState(int state) {
        if (viewRef == null) {
            return;
        }

        View sheet = viewRef.get();
        if (sheet == null) {
            return;
        }

        for (int i = 0; i < callbacks.size(); i++) {
            callbacks.get(i).onSettleToState(sheet, state);
        }
    }

    /**
     * Gets the current state of the sheet.
     *
     * @return One of {@link #STATE_EXPANDED}, {@link #STATE_COLLAPSED},
     * {@link #STATE_DRAGGING} or {@link #STATE_SETTLING}.
     */
    @State
    public int getState() {
        return state;
    }

    protected void setStateInternal(@State int state) {
        if (this.state == state) {
            return;
        }

        if (viewRef == null) {
            this.state = state;
            return;
        }

        V sheet = viewRef.get();
        if (sheet == null) {
            this.state = state;
            return;
        }

        // dragging the sheet while settling will still fire the callback
        // even if the sheet is not fully settled
        if (state == STATE_SETTLING || state == STATE_DRAGGING
                || isFullySettled(sheet, state)) {
            this.state = state;

            for (int i = 0; i < callbacks.size(); i++) {
                callbacks.get(i).onStateChanged(sheet, state);
            }
        }
    }

    private void reset() {
        activePointerId = ViewDragHelper.INVALID_POINTER;
    }

    @Nullable
    private View findNestedScrollingChild(View view) {
        if (view == null) {
            return null;
        }

        if (ViewCompat.isNestedScrollingEnabled(view) &&
                !(view instanceof SwipeRefreshLayout) &&
                view.isAttachedToWindow() &&
                view.isShown()) {
            Rect testRect = new Rect(0, 0, 0, 0);

            if (view.getLocalVisibleRect(testRect)) {
                return view;
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;

            for (int index = 0, count = group.getChildCount(); index < count; index++) {
                View scrollingChild = findNestedScrollingChild(group.getChildAt(index));

                if (scrollingChild != null) {
                    return scrollingChild;
                }
            }
        }

        return null;
    }

    @Nullable
    private ViewPager findPager(View view) {
        if (view instanceof ViewPager) {
            return (ViewPager) view;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;

            for (int index = 0, count = group.getChildCount(); index < count; index++) {
                ViewPager scrollingChild = findPager(group.getChildAt(index));

                if (scrollingChild != null) {
                    return scrollingChild;
                }
            }
        }

        return null;
    }

    public boolean isDragHelperInstantiated() {
        return dragHelper != null;
    }

    protected void dispatchOnSlide(int value) {
        if (viewRef != null) {
            View sheet = viewRef.get();

            if (sheet != null && !callbacks.isEmpty()) {
                float slideOffset = Math.max(0, Math.min(1,
                        (value - collapsedOffset) * 1f / (expandedOffset - collapsedOffset)));

                for (SheetCallback callback : callbacks) {
                    callback.onSlide(sheet, slideOffset);
                }
            }
        }
    }

    /**
     * Move a specific child into place.
     *
     * @param child Child targeted by move
     * @param state Target state
     * @param left  Final left offset of child
     * @param top   Final top offset of child
     */
    protected void moveChildTo(View child, int state, int left, int top) {
        if (dragHelper != null) {
            dragHelper.abort();

            int dx = left - child.getLeft();
            int dy = top - child.getTop();

            if (dx != 0) {
                ViewCompat.offsetLeftAndRight(child, dx);
            } else if (dy != 0) {
                ViewCompat.offsetTopAndBottom(child, dy);
            }
        }

        if (viewRef != null) {
            View sheet = viewRef.get();

            if (sheet != null) {
                for (SheetCallback callback : callbacks) {
                    if (state == STATE_COLLAPSED) {
                        callback.onSlide(sheet, 0);
                    } else if (state == STATE_EXPANDED) {
                        callback.onSlide(sheet, 1);
                    }
                }
            }
        }

        setStateInternal(state);
    }

    protected void settleChildTo(View child, int state, int left, int top) {
        settleChildTo(child, state, left, top, null, null);
    }

    /**
     * Settle a specific child into place.
     *
     * @param child Child targeted by the settle animation
     * @param state Target state
     * @param left  Final left offset of child
     * @param top   Final top offset of child
     * @param xvel  Horizontal velocity to calculate animation duration, or null for base duration.
     * @param yvel  Vertical velocity to calculate animation duration, or null for base duration.
     */
    protected void settleChildTo(View child, int state, int left,
                                 int top, @Nullable Integer xvel, @Nullable Integer yvel) {
        if (dragHelper != null) {
            boolean startedSettling;

            if (xvel != null || yvel != null) {
                startedSettling = dragHelper.smoothSlideViewTo(child, left,
                        top, xvel != null ? xvel : 0, yvel != null ? yvel : 0);
            } else {
                startedSettling = dragHelper.smoothSlideViewTo(child, left, top);
            }

            if (startedSettling) {
                setStateInternal(STATE_SETTLING);
                if (settleRunnable == null) {
                    // If the singleton SettleRunnable instance has not been instantiated, create it.
                    settleRunnable = new SettleRunnable(child, state);
                }

                // If the SettleRunnable has not been posted, post it with the correct state.
                if (!settleRunnable.isPosted) {
                    settleRunnable.targetState = state;

                    ViewCompat.postOnAnimation(child, settleRunnable);
                    settleRunnable.isPosted = true;
                } else {
                    // Otherwise, if it has been posted, just update the target state.
                    settleRunnable.targetState = state;
                }

                notifySettleToState(state);
            } else {
                setStateInternal(state);
            }
        }
    }

    /**
     * Force the sheet to capture the event disregarding all other logic.
     */
    public void interceptTouches(boolean capture) {
        this.capture = capture;
    }

    public void setDraggable(boolean draggable) {
        this.draggable = draggable;
    }

    public boolean isDraggable(boolean draggable) {
        return draggable;
    }

    private class SettleRunnable implements Runnable {
        private final View view;
        private boolean isPosted;

        @State
        int targetState;

        SettleRunnable(View view, @State int targetState) {
            this.view = view;
            this.targetState = targetState;
        }

        @Override
        public void run() {
            if (dragHelper != null && dragHelper.continueSettling(true)) {
                ViewCompat.postOnAnimation(view, this);
            } else {
                setStateInternal(targetState);
            }

            this.isPosted = false;
        }
    }

    public void invalidate() {
        if (viewRef == null) {
            return;
        }

        V sheet = viewRef.get();
        if (sheet == null) {
            return;
        }

        dispatchOnSlide(sheet);
    }

    /**
     * A utility function to get the {@link SheetBehavior} associated with the {@code view}.
     *
     * @param view The {@link View} with {@link SheetBehavior}.
     * @return The {@link SheetBehavior} associated with the {@code view}.
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public static <V extends View> SheetBehavior<V> from(@NonNull V view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (!(params instanceof LayoutParams)) {
            throw new IllegalArgumentException("The view is not a child of CoordinatorLayout");
        }
        CoordinatorLayout.Behavior<?> behavior =
                ((LayoutParams) params).getBehavior();
        if (!(behavior instanceof SheetBehavior)) {
            throw new IllegalArgumentException("The view is not associated with SheetBehavior");
        }
        return (SheetBehavior<V>) behavior;
    }

    protected abstract void calculateCollapsedOffset();

    protected abstract void calculateExpandedOffset();

    protected abstract void dispatchOnSlide(V child);

    protected abstract int getPositionInParent(V child);

    protected abstract void offset(V child, int offset);

    /**
     * Check if a view is fully settled to a state.
     *
     * @param sheet The sheet instance.
     * @param state The state to check. This will be one of {@link #STATE_EXPANDED}
     *              or {@link #STATE_COLLAPSED}.
     */
    protected abstract boolean isFullySettled(@NonNull V sheet, int state);

    protected abstract void stopNestedScrollLogic(V child);

    protected abstract boolean nestedPreFlingLogic(V child, float xvel, float yvel);

    protected abstract void nestedPreScrollLogic(V child, View target, int dx, int dy, int[] consumed);

    protected abstract boolean startNestedScrollLogic(int axes);

    protected abstract void touchEventLogic(V child, MotionEvent event);

    protected abstract boolean interceptTouchEventLogic(CoordinatorLayout parent, V
            child, MotionEvent event);

    protected abstract SheetDragHelper.Callback instantiateDragCallback();

    protected abstract void settleToState(@NonNull View child, int state, boolean animate);
}