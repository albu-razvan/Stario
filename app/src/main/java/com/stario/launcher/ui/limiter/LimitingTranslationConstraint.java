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

package com.stario.launcher.ui.limiter;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;

public class LimitingTranslationConstraint extends ConstraintLayout {
    float startX = 0;
    float endX = 0;

    public LimitingTranslationConstraint(Context context) {
        super(context);
    }

    public LimitingTranslationConstraint(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LimitingTranslationConstraint(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        View parent = ((View) getParent());

        this.startX = parent.getPaddingLeft();
        this.endX = parent.getWidth() - parent.getPaddingRight();
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
}