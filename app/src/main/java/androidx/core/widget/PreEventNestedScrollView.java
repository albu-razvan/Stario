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

package androidx.core.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PreEventNestedScrollView extends NestedScrollView {
    public static final float UP = 1;
    public static final float DOWN = -1;
    private boolean ignore;
    private PreEvent listener;

    public PreEventNestedScrollView(@NonNull Context context) {
        super(context);
    }

    public PreEventNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PreEventNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onStopNestedScroll(@NonNull View target, int type) {
        ignore = false;

        super.onStopNestedScroll(target, type);
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        if (!ignore) {
            super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
        }
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed, @Nullable int[] offsetInWindow, int type) {
        if (listener == null || !listener.onPreScroll(dy)) {
            ignore = false;

            return super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type);
        } else {
            ignore = true;

            return false;
        }
    }

    @Override
    boolean overScrollByCompat(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
        if (ignore) {
            deltaY = 0;
        }

        return super.overScrollByCompat(deltaX, deltaY, scrollX, scrollY, scrollRangeX, scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent);
    }

    @Override
    public void fling(int velocityY) {
        if (listener == null || !listener.onPreFling(velocityY)) {
            super.fling(velocityY);
        }
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        return false;
    }

    public void setOnPreScrollListener(PreEvent listener) {
        this.listener = listener;
    }

    public interface PreEvent {
        /**
         * @return true if event should be consumed by this method
         */
        default boolean onPreScroll(int delta) {
            return false;
        }

        /**
         * @return true if event should be consumed by this method
         */
        default boolean onPreFling(int velocity) {
            return false;
        }
    }
}
