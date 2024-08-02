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

package com.stario.launcher.ui.widgets;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.stario.launcher.utils.UiUtils;

public class RoundedWidgetHost extends AppWidgetHostView {
    public static final int RADIUS_DP = 20;

    public RoundedWidgetHost(Context context) {
        super(context);

        setClipChildren(true);
        setClipToOutline(true);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(0, 0, 0, 0);
    }

    @Override
    public void setPaddingRelative(int start, int top, int end, int bottom) {
        super.setPaddingRelative(0, 0, 0, 0);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (child instanceof ViewGroup) {
            child.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                UiUtils.roundViewGroup((ViewGroup) child, RADIUS_DP);
            });
        }

        super.addView(child, index, params);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        UiUtils.roundViewGroup(this, RADIUS_DP);
    }
}
