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

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.ArrayList;
import java.util.List;

public class SheetsFocusController extends ConstraintLayout {
    private List<Integer> targetPointers;
    private SheetType sheetType;
    private float interceptSlop;
    private float X, Y;
    private float deltaX, deltaY;

    public SheetsFocusController(@NonNull Context context) {
        super(context);

        init(context);
    }

    public SheetsFocusController(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public SheetsFocusController(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    private void init(Context context) {
        targetPointers = new ArrayList<>();
        sheetType = SheetType.UNDEFINED;
        interceptSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (SheetWrapper.active()) {
            return false;
        } else if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            targetPointers.add(0,
                    ev.getAction() >> MotionEvent.ACTION_POINTER_INDEX_SHIFT);

            X = ev.getX(getPointer(ev));
            Y = ev.getY(getPointer(ev));

            deltaX = 0;
            deltaY = 0;

            super.onInterceptTouchEvent(ev);
            return false;
        } else {
            if (ev.getAction() == MotionEvent.ACTION_UP) {
                sheetType = SheetType.UNDEFINED;
                targetPointers.clear();

                super.onInterceptTouchEvent(ev);
                return false;
            } else {
                deltaY += Y - ev.getY(getPointer(ev));
                deltaX += X - ev.getX(getPointer(ev));

                super.onInterceptTouchEvent(ev);
                return Math.abs(deltaY) > interceptSlop ||
                        Math.abs(deltaX) > interceptSlop;
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP ||
                ev.getAction() == MotionEvent.ACTION_CANCEL) {
            SheetWrapper.sendMotionEvent(sheetType, ev);

            sheetType = SheetType.UNDEFINED;
            targetPointers.clear();

            return super.onTouchEvent(ev);
        } else {
            if (sheetType == SheetType.UNDEFINED) {
                boolean vertical = Math.abs(deltaY) > Math.abs(deltaX);

                if (vertical) {
                    sheetType = Math.signum(deltaY) >= 0 ? SheetType.BOTTOM_SHEET : SheetType.TOP_SHEET;
                } else {
                    sheetType = Math.signum(deltaX) >= 0 ? SheetType.RIGHT_SHEET : SheetType.LEFT_SHEET;
                }
            }

            SheetWrapper.sendMotionEvent(sheetType, ev);
        }

        return super.onTouchEvent(ev);
    }

    private int getPointer(MotionEvent event) {
        return Math.max(0, Math.min(event.getPointerCount() - 1,
                event.findPointerIndex(targetPointers.get(0))));
    }
}