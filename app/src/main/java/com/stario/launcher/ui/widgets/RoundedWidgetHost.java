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

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.view.ViewGroup;

import com.stario.launcher.ui.utils.UiUtils;

public class RoundedWidgetHost extends AppWidgetHostView {
    public static final int RADIUS_DP = 20;

    public RoundedWidgetHost(Context context) {
        super(context);

        setClipChildren(true);
        setClipToOutline(true);
    }

    public RoundedWidgetHost(Context context, ViewGroup.LayoutParams params) {
        super(context);

        setLayoutParams(params);
        setPadding(0, 0, 0, 0);
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
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        UiUtils.roundViewGroup(this, RADIUS_DP);
    }
}
