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

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.recyclerview.widget.RecyclerView;

public class ClickableRecyclerView extends RecyclerView {

    private final int moveSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

    private boolean valid = false;
    private int x = 0;
    private int y = 0;

    public ClickableRecyclerView(Context context) {
        super(context);
    }

    public ClickableRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ClickableRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean result = super.onTouchEvent(e);

        if (e != null) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) e.getRawX();
                    y = (int) e.getRawY();
                    valid = true;

                    break;

                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(e.getRawX() - x) > moveSlop ||
                            Math.abs(e.getRawY() - y) > moveSlop) {
                        valid = false;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    if (valid && Math.abs(e.getRawX() - x) < moveSlop &&
                            Math.abs(e.getRawY() - y) < moveSlop) {
                        performClick();
                    }
                    break;
            }
        }

        return result;
    }
}