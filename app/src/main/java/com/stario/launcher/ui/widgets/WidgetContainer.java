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

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.widget.GridLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.stario.launcher.sheet.widgets.Widget;
import com.stario.launcher.sheet.widgets.WidgetSize;
import com.stario.launcher.ui.Measurements;

@SuppressLint("ViewConstructor")
class WidgetContainer extends RelativeLayout implements Comparable<WidgetContainer> {
    private final AppWidgetHostView host;
    private final Widget widget;
    private WidgetMap.Cell origin; // top-left

    WidgetContainer(Context context, AppWidgetHostView host, Widget widget, WidgetMap.Cell cell) {
        super(context);

        this.origin = cell;
        this.host = host;
        this.widget = widget;

        int padding = Measurements.dpToPx(10);
        setPadding(padding, padding, padding, padding);
        setRotation(180);

        addView(host);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!(getParent() instanceof WidgetGrid)) {
            throw new RuntimeException("WidgetContainer views can only be children of WidgetGrid");
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        GridLayout.LayoutParams params = (GridLayout.LayoutParams) getLayoutParams();

        int cellSize = ((WidgetGrid) getParent()).getCellSize();

        params.rowSpec = WidgetGrid.spec(origin.row, widget.size.height);
        params.columnSpec = WidgetGrid.spec(origin.column, widget.size.width);
        params.width = cellSize * widget.size.width;
        params.height = cellSize * widget.size.height;

        setLayoutParams(params);

        int hostWidth = params.width - getPaddingLeft() - getPaddingRight();
        int hostHeight = params.height - getPaddingTop() - getPaddingBottom();

        if (getMeasuredWidth() > 0 && getMeasuredHeight() > 0) {
            host.updateAppWidgetSize(null,
                    (int) (hostWidth / Measurements.getDensity()),
                    (int) (hostHeight / Measurements.getDensity()),
                    (int) (hostWidth / Measurements.getDensity()),
                    (int) (hostHeight / Measurements.getDensity()));
        }
    }

    WidgetSize getSize() {
        return widget.size;
    }

    int getPosition() {
        return widget.position;
    }

    void updateOrigin(WidgetMap.Cell origin) {
        if (origin != null && !origin.equals(this.origin)) {
            this.origin = origin;

            requestLayout();
        }
    }

    @Override
    public int compareTo(@NonNull WidgetContainer container) {
        return widget.compareTo(container.widget);
    }
}
