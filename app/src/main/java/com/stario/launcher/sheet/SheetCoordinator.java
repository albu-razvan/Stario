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

package com.stario.launcher.sheet;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

public class SheetCoordinator extends CoordinatorLayout {
    public static final int OWN = 0;
    public static final int ALL = 1;
    private int interceptStatus;

    public SheetCoordinator(@NonNull Context context) {
        super(context);

        interceptStatus = ALL;
    }

    public SheetCoordinator(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        interceptStatus = ALL;
    }

    public SheetCoordinator(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        interceptStatus = ALL;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean isPointInChildBounds(@NonNull View child, int x, int y) {
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (interceptStatus == ALL) {
            requestDisallowInterceptTouchEvent(true);

            return true;
        } else {
            return super.onInterceptTouchEvent(ev);
        }
    }

    public void intercept(int value) {
        interceptStatus = value;
    }
}