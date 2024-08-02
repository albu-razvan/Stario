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

package com.stario.launcher.ui.tabs;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.view.ViewCompat;

import com.ogaclejapan.smarttablayout.SmartTabLayout;

public class TabLayout extends SmartTabLayout {
    private int centerTranslation;
    private int centerBias;

    public TabLayout(Context context) {
        super(context);
    }

    public TabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TabLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        View parent = (View) getParent();

        centerBias = (parent.getPaddingRight() - parent.getPaddingLeft()) / 2;
        centerTranslation = getPaddingLeft();

        ViewCompat.setPaddingRelative(this, 0, getPaddingTop(),
                centerTranslation + centerBias, getPaddingBottom());
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public void scrollTo(int x, int y) {
        float percentage = Math.min(1f, (float) x / centerTranslation);

        x = (int) (x + (centerBias - centerTranslation) * Math.pow(percentage, 0.7f));

        super.scrollTo(x, y);

        ((View) tabStrip).setTranslationX(2 * centerBias);
    }
}
