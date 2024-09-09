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

public class PreScrollListeningNestedScrollView extends NestedScrollView {
    public static final float UP = 1;
    public static final float DOWN = -1;
    private boolean ignore;
    private PreScroll listener;

    public PreScrollListeningNestedScrollView(@NonNull Context context) {
        super(context);
    }

    public PreScrollListeningNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PreScrollListeningNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
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
        if (listener == null || listener.onPreScroll(Math.signum(dy))) {
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
        if (listener == null || listener.onPreScroll(Math.signum(velocityY))) {
            super.fling(velocityY);
        }
    }

    @Override
    public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY) {
        if (listener == null || listener.onPreScroll(Math.signum(velocityY))) {
            return super.onNestedPreFling(target, velocityX, velocityY);
        }

        return true;
    }

    public void setOnPreScrollListener(PreScroll listener) {
        this.listener = listener;
    }

    public interface PreScroll {
        /**
         * @return Whether the ScrollView should scroll or not
         */
        boolean onPreScroll(float direction);
    }
}
