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

package com.stario.launcher.ui.scrollers;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.PreEventNestedScrollView;

import com.stario.launcher.utils.Utils;

public class BottomNestedScrollView extends PreEventNestedScrollView {
    private boolean nestedScrolling;

    public BottomNestedScrollView(@NonNull Context context) {
        super(context);

        init();
    }

    public BottomNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public BottomNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {
        setRotation(180);

        this.nestedScrolling = false;
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, bottom, right, top);
    }

    @Override
    public int getPaddingBottom() {
        return super.getPaddingTop();
    }

    @Override
    public int getPaddingTop() {
        return super.getPaddingBottom();
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        child.setRotation(child.getRotation() + 180);

        super.addView(child, index, params);
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        super.onNestedScroll(target, dxConsumed, -dyConsumed, dxUnconsumed, -dyUnconsumed);
    }

    @Override
    public boolean onNestedFling(@NonNull View target, float velocityX, float velocityY, boolean consumed) {
        if (!Utils.isMinimumSDK(Build.VERSION_CODES.Q)) {
            velocityY = -velocityY;
        }

        return super.onNestedFling(target, velocityX, velocityY, consumed);
    }

    @Override
    public void fling(int velocityY) {
        if (!Utils.isMinimumSDK(Build.VERSION_CODES.Q)) {
            velocityY = -velocityY;
        }

        super.fling(velocityY);
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed) {
        nestedScrolling = true;

        super.onNestedPreScroll(target, dx, dy, consumed);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed, @Nullable int[] offsetInWindow, int type) {
        boolean result;

        if (!nestedScrolling) {
            result = super.dispatchNestedPreScroll(dx, -dy, consumed, offsetInWindow, type);

            if (consumed != null) {
                consumed[1] = -consumed[1];
            }

            if (offsetInWindow != null) {
                offsetInWindow[1] = -offsetInWindow[1];
            }
        } else {
            result = super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type);
        }

        nestedScrolling = false;

        return result;
    }

    @Override
    public boolean canScrollVertically(int direction) {
        return super.canScrollVertically(-direction);
    }
}
