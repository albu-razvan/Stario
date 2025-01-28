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

package com.stario.launcher.ui.recyclers;

import androidx.annotation.NonNull;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ScrollListeningRecyclerView extends RecyclerView {
    private static final int MAX_VELOCITY = 250;
    private int lastCall;
    private SpringAnimation springAnimation;
    private ArrayList<ScrollStoppedListener> onScrollStoppedListeners;
    private ArrayList<OnScrollListener> onScrollListeners;

    public ScrollListeningRecyclerView(android.content.Context context) {
        super(context);

        init();
    }

    public ScrollListeningRecyclerView(android.content.Context context, android.util.AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public ScrollListeningRecyclerView(android.content.Context context, android.util.AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init();
    }

    private void init() {
        this.lastCall = 0;
        this.onScrollListeners = new ArrayList<>();
        this.onScrollStoppedListeners = new ArrayList<>();

        SpringForce spring = new SpringForce();
        spring.setFinalPosition(0f);
        spring.setStiffness(200f);
        spring.setDampingRatio(0.8f);
        springAnimation = new SpringAnimation(this, new FloatPropertyCompat<>("") {
            @Override
            public float getValue(ScrollListeningRecyclerView object) {
                return 0;
            }

            @Override
            public void setValue(ScrollListeningRecyclerView object, float value) {
                value = Math.abs(value / getMeasuredHeight()) + 1;

                if (!Float.isNaN(value)) {
                    setScaleY(value);
                }
            }
        }).setSpring(spring);

        addOnScrollStoppedListener((scroll) -> {
            if (canScrollVertically(1)) {
                setPivotY(0);
            } else {
                setPivotY(getMeasuredHeight());
            }

            springAnimation.setStartVelocity(
                            Math.max(-MAX_VELOCITY, Math.min(MAX_VELOCITY, scroll)) * 10)
                    .start();
        });
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        return false;
    }

    @Override
    public void addOnScrollListener(@NonNull OnScrollListener listener) {
        onScrollListeners.add(listener);

        super.addOnScrollListener(listener);
    }

    @Override
    public void removeOnScrollListener(@NonNull OnScrollListener listener) {
        onScrollListeners.remove(listener);

        super.removeOnScrollListener(listener);
    }

    @Override
    public void scrollToPosition(int position) {
        for (OnScrollListener listener : onScrollListeners) {
            listener.onScrolled(this, 0, 0);
        }

        super.scrollToPosition(position);
    }

    @Override
    public void onScrollStateChanged(int state) {
        springAnimation.skipToEnd();

        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            for (ScrollStoppedListener listener : onScrollStoppedListeners) {
                if (listener != null) {
                    listener.onScrollStopped(lastCall);
                }
            }
        }

        super.onScrollStateChanged(state);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow, int type) {
        if (canScrollVertically(dy)) {
            lastCall = dy;
        } else {
            lastCall = 0;
        }

        return super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type);
    }

    public void addOnScrollStoppedListener(ScrollStoppedListener listener) {
        this.onScrollStoppedListeners.add(listener);

        listener.onScrollStopped(getScrollY());
    }

    public interface ScrollStoppedListener {
        void onScrollStopped(int scroll);
    }
}