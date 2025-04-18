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

package androidx.core.widget;

import android.content.Context;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PreEventNestedScrollView extends NestedScrollView {
    private boolean ignore;
    private PreEvent listener;

    /**
     * The following are copied from OverScroller to determine how far a fling will go.
     */
    private static final float SCROLL_FRICTION = 0.015f;
    private static final float INFLEXION = 0.35f; // Tension lines cross at (INFLEXION, 1)
    private static final float DECELERATION_RATE = (float) (Math.log(0.78) / Math.log(0.9));

    private float mPhysicalCoeff;


    public PreEventNestedScrollView(@NonNull Context context) {
        super(context);

        init(context);
    }

    public PreEventNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public PreEventNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    private void init(Context context) {
        final float ppi = context.getResources().getDisplayMetrics().density * 160.0f;
        mPhysicalCoeff = SensorManager.GRAVITY_EARTH // g (m/s^2)
                * 39.37f // inch/meter
                * ppi
                * 0.84f; // look and feel tuning
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

    /**
     * Copied from OverScroller, this returns the distance that a fling with the given velocity
     * will go.
     *
     * @param velocity The velocity of the fling
     * @return The distance that will be traveled by a fling of the given velocity.
     */
    public int getSplineFlingDistance(int velocity) {
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        return (int) (SCROLL_FRICTION * mPhysicalCoeff
                * Math.exp(DECELERATION_RATE / decelMinusOne
                * Math.log(INFLEXION * Math.abs(velocity)
                / (SCROLL_FRICTION * mPhysicalCoeff))));
    }

    /**
     * Reverse of {@link PreEventNestedScrollView#getSplineFlingDistance(int)}
     *
     * @param distance The distance to be converted
     * @return The fling velocity needed to travel the provided distance.
     */
    public int getSplineFlingVelocity(int distance) {
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        return (int) (Math.abs(distance) > 0 ? (Math.signum(distance) // Preserve the sign
                * (SCROLL_FRICTION * mPhysicalCoeff / INFLEXION
                * Math.exp(decelMinusOne / DECELERATION_RATE
                * Math.log(Math.abs(distance) / (SCROLL_FRICTION * mPhysicalCoeff))))) : 0);
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
