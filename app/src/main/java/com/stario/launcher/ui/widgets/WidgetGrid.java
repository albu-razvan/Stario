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

import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;

import com.stario.launcher.sheet.widgets.Widget;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.utils.animation.Animation;

import java.util.PriorityQueue;

public class WidgetGrid extends GridLayout {
    private static final String TAG = "com.stario.WidgetGrid";
    private WidgetMap map;

    public WidgetGrid(Context context) {
        super(context);

        init();
    }

    public WidgetGrid(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public WidgetGrid(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {
        this.map = new WidgetMap();

        setRotation(180);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(null, "alpha", 0, 1),
                ObjectAnimator.ofFloat(null, "scaleX", 0.8f, 1),
                ObjectAnimator.ofFloat(null, "scaleY", 0.8f, 1)
        );

        LayoutTransition layoutTransition = new LayoutTransition();
        layoutTransition.setAnimator(LayoutTransition.APPEARING, set);
        layoutTransition.setDuration(LayoutTransition.APPEARING, Animation.MEDIUM.getDuration());
        layoutTransition.setDuration(LayoutTransition.CHANGE_APPEARING, Animation.MEDIUM.getDuration());
        layoutTransition.setDuration(LayoutTransition.CHANGE_DISAPPEARING, Animation.MEDIUM.getDuration());
        layoutTransition.setDuration(LayoutTransition.DISAPPEARING, Animation.MEDIUM.getDuration());
        layoutTransition.setStartDelay(LayoutTransition.APPEARING, 0);
        layoutTransition.disableTransitionType(LayoutTransition.CHANGING);

        setLayoutTransition(layoutTransition);

        Measurements.addWidgetColumnCountChangeListener(object -> {
            View parent = (View) getParent();

            float originalAlpha = parent != null ? parent.getAlpha() : 1;
            if (parent != null) {
                parent.setAlpha(0);
            }

            LayoutTransition transition = getLayoutTransition();
            setLayoutTransition(null);

            post(this::reorder);

            if (parent != null && originalAlpha > 0) {
                post(() -> parent.animate().alpha(originalAlpha)
                        .setDuration(Animation.MEDIUM.getDuration()));
            }

            setLayoutTransition(transition);
        });
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (child instanceof WidgetContainer) {
            super.addView(child, index, params);
        } else {
            throw new RuntimeException("WidgetGrid can only have WidgetContainer children.");
        }
    }

    @Override
    public void removeView(View view) {
        super.removeView(view);

        post(this::reorder);
    }

    int getCellSize() {
        return getMeasuredWidth() / Measurements.getWidgetColumnCount();
    }

    public void attach(AppWidgetHostView host, Widget widget) {
        WidgetMap.Cell cell = map.getAvailableOrigin(widget.size);

        super.addView(new WidgetContainer(getContext(), host, widget, cell));
        map.add(cell, widget.size);
    }

    private void reorder() {
        map.clear();

        PriorityQueue<WidgetContainer> containers = new PriorityQueue<>();

        for (int index = 0; index < getChildCount(); index++) {
            containers.add((WidgetContainer) getChildAt(index));
        }

        while (containers.size() > 0) {
            WidgetContainer container = containers.poll();

            if (container != null) {
                WidgetMap.Cell cell = map.getAvailableOrigin(container.getSize());

                container.updateOrigin(cell);

                map.add(cell, container.getSize());
            }
        }
    }

    public int allocatePosition() {
        View view = getChildAt(getChildCount() - 1);

        if (view != null) {
            return ((WidgetContainer) view).getPosition() + 1;
        }

        return 0;
    }

    public int computeCellSize() {
        return getWidth() / Measurements.getWidgetColumnCount();
    }
}
