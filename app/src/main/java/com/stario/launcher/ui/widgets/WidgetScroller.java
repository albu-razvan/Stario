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

package com.stario.launcher.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stario.launcher.ui.common.scrollers.BottomNestedScrollView;

public class WidgetScroller extends BottomNestedScrollView {
    public WidgetScroller(@NonNull Context context) {
        super(context);
    }

    public WidgetScroller(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public WidgetScroller(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        if (!target.canScrollVertically(1)) {
            super.onNestedPreScroll(target, dx, dy, consumed, type);
        }
    }
}
