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

package com.stario.launcher.ui.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.RemoteViews;

import com.stario.launcher.utils.animation.Animation;

@SuppressLint("ViewConstructor")
public class WidgetHostView extends RoundedWidgetHost {
    static final float STARTING_SCALE = 0.9f;
    private static final float MOVE_THRESHOLD = 10;
    private boolean mHasPerformedLongPress;
    private CheckForLongPress mPendingCheckForLongPress;

    public WidgetHostView(Context context, WidgetContainer.LayoutParams params) {
        super(context);

        setLayoutParams(params);
    }

    @Override
    public void updateAppWidget(RemoteViews remoteViews) {
        super.updateAppWidget(remoteViews);

        enableChildrenNestedScrolling(this);
    }

    private void enableChildrenNestedScrolling(View view) {
        view.setNestedScrollingEnabled(true);
        view.setOverScrollMode(OVER_SCROLL_NEVER);

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;

            for (int index = 0; index < viewGroup.getChildCount(); index++) {
                View child = viewGroup.getChildAt(index);

                enableChildrenNestedScrolling(child);
            }
        }
    }

    private float X;
    private float Y;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            X = ev.getRawX();
            Y = ev.getRawY();

            postCheckForLongClick();
        } else if (ev.getAction() == MotionEvent.ACTION_UP ||
                ev.getAction() == MotionEvent.ACTION_CANCEL) {
            removeCheck();
        }

        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP ||
                ev.getAction() == MotionEvent.ACTION_CANCEL) {
            removeCheck();
        } else {
            if (mHasPerformedLongPress) {
                removeCheck();

                return super.dispatchTouchEvent(ev);
            } else if (ev.getAction() == MotionEvent.ACTION_MOVE &&
                    (Math.abs(X - ev.getRawX()) >= MOVE_THRESHOLD ||
                            Math.abs(Y - ev.getRawY()) >= MOVE_THRESHOLD)) {
                removeCheck();
            }
        }

        return super.dispatchTouchEvent(ev);
    }

    private void removeCheck() {
        if (mPendingCheckForLongPress != null) {
            removeCallbacks(mPendingCheckForLongPress);

            mPendingCheckForLongPress = null;

            if (!mHasPerformedLongPress) {
                animate().scaleY(1)
                        .scaleX(1)
                        .alpha(1)
                        .setDuration(Animation.SHORT.getDuration());
            }
        }
    }

    @Override
    public int getDescendantFocusability() {
        return ViewGroup.FOCUS_BLOCK_DESCENDANTS;
    }

    private void postCheckForLongClick() {
        mHasPerformedLongPress = false;

        mPendingCheckForLongPress = new CheckForLongPress();
        postDelayed(mPendingCheckForLongPress, ViewConfiguration.getLongPressTimeout());

        animate().scaleY(STARTING_SCALE)
                .scaleX(STARTING_SCALE)
                .alpha(0.7f)
                .setInterpolator(new PathInterpolator(0.5f, 0, 0.2f, 1))
                .setDuration(ViewConfiguration.getLongPressTimeout());
    }

    @Override
    public boolean performLongClick() {
        boolean value = !mHasPerformedLongPress && super.performLongClick();

        if (value) {
            mHasPerformedLongPress = true;

            if (mPendingCheckForLongPress != null) {
                removeCallbacks(mPendingCheckForLongPress);
            }
        }

        return value;
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        removeCheck();
    }

    private class CheckForLongPress implements Runnable {
        private final int mOriginalWindowAttachCount;

        private CheckForLongPress() {
            mOriginalWindowAttachCount = getWindowAttachCount();
        }

        public void run() {
            if ((getParent() != null) && hasWindowFocus()
                    && mOriginalWindowAttachCount == getWindowAttachCount()
                    && !mHasPerformedLongPress) {
                performLongClick();
            }
        }
    }
}