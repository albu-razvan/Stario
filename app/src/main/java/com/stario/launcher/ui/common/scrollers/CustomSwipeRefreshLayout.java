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

package com.stario.launcher.ui.common.scrollers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class CustomSwipeRefreshLayout extends SwipeRefreshLayout {
    private OnEngageListener engageListener;
    private boolean isPullingOrSettling;

    public CustomSwipeRefreshLayout(Context context) {
        super(context);

        this.isPullingOrSettling = false;
    }

    public CustomSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.isPullingOrSettling = false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (isRefreshing() || isPullingOrSettling) {
            return true;
        }

        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_MOVE) {
            if (getScrollY() < 0 && !isPullingOrSettling) {
                isPullingOrSettling = true;

                if (engageListener != null) {
                    engageListener.onEngaged(true);
                }
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (isPullingOrSettling) {
                isPullingOrSettling = false;

                if (engageListener != null) {
                    engageListener.onEngaged(false);
                }
            }
        }

        if (isPullingOrSettling) {
            return true;
        }

        return super.onTouchEvent(event);
    }

    @Override
    public void setRefreshing(boolean refreshing) {
        super.setRefreshing(refreshing);

        if (isPullingOrSettling != refreshing) {
            isPullingOrSettling = refreshing;
            if (engageListener != null) {
                engageListener.onEngaged(isPullingOrSettling);
            }
        }
    }

    public void setOnEngageListener(OnEngageListener listener) {
        this.engageListener = listener;
    }

    public interface OnEngageListener {
        void onEngaged(boolean engaged);
    }
}