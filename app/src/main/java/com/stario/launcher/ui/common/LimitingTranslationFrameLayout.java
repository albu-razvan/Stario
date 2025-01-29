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

package com.stario.launcher.ui.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

public class LimitingTranslationFrameLayout extends FrameLayout {
    float startX = 0;
    float startY = 0;
    float endX = 0;
    float endY = 0;

    public LimitingTranslationFrameLayout(Context context) {
        super(context);
    }

    public LimitingTranslationFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LimitingTranslationFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        View parent = ((View) getParent());

        this.startX = parent.getPaddingLeft();
        this.startY = parent.getPaddingTop();
        this.endX = parent.getWidth() - parent.getPaddingRight();
        this.endY = parent.getHeight() - parent.getPaddingBottom();
    }

    @Override
    public void setTranslationX(float translationX) {
        if (translationX + getLeft() < startX) {
            translationX = startX - getLeft();
        } else if (translationX + getRight() > endX) {
            translationX = endX - getRight();
        }

        super.setTranslationX(translationX);
    }

    @Override
    public void setTranslationY(float translationY) {
        int scroll = getParentScroll();
        int range = getParentScrollRange();

        if (translationY + getTop() + scroll < startY) {
            translationY = startY - getTop() - scroll;
        } else if (translationY + getBottom() - (range - scroll) > endY) {
            translationY = endY - getBottom() + (range - scroll);
        }

        super.setTranslationY(translationY);
    }

    private int getParentScroll() {
        ViewParent parent = getParent();

        if (parent instanceof RecyclerView) {
            return ((RecyclerView) parent).computeVerticalScrollOffset();
        } else if ((parent instanceof ScrollView) ||
                (parent instanceof NestedScrollView)) {
            return ((View) parent).getScrollY();
        }

        return 0;
    }

    private int getParentScrollRange() {
        ViewParent parent = getParent();

        if (parent instanceof RecyclerView) {
            return ((RecyclerView) parent).computeVerticalScrollRange();
        } else if (parent instanceof ScrollView ||
                parent instanceof NestedScrollView) {
            ViewGroup scrollView = (ViewGroup) parent;

            if (scrollView.getChildCount() > 0) {
                View child = scrollView.getChildAt(0);

                return Math.max(0,
                        child.getHeight() - (scrollView.getHeight() -
                                scrollView.getPaddingBottom() - scrollView.getPaddingTop()));
            }
        }

        return 0;
    }
}