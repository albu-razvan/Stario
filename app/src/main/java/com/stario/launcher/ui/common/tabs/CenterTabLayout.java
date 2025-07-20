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

package com.stario.launcher.ui.common.tabs;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.viewpager.widget.ViewPager;

import com.ogaclejapan.smarttablayout.SmartTabLayout;
import com.stario.launcher.R;
import com.stario.launcher.ui.utils.UiUtils;

public class CenterTabLayout extends SmartTabLayout {
    private OnLongClickTabListener listener;
    private LayoutInflater inflater;
    private ViewPager viewPager;

    public CenterTabLayout(Context context) {
        super(context);

        init(context);
    }

    public CenterTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public CenterTabLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init(context);
    }

    private void init(Context context) {
        this.inflater = UiUtils.unwrapContext(context).getLayoutInflater();

        setCustomTabView((viewGroup, position, adapter) -> {
            CharSequence text = adapter.getPageTitle(position);

            TextView textView = (TextView) inflater.inflate(R.layout.tab, viewGroup, false);
            textView.setText(text);

            textView.setOnClickListener((v) -> {
                if (viewPager != null) {
                    viewPager.setCurrentItem(position);
                }
            });
            textView.setOnLongClickListener((v) -> {
                if(listener != null) {
                    listener.onLongClick(v, position);
                }

                return true;
            });

            return textView;
        });
    }

    @Override
    public void setViewPager(ViewPager viewPager) {
        this.viewPager = viewPager;

        super.setViewPager(viewPager);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    public void setOnTabLongClickListener(OnLongClickTabListener listener) {
        this.listener = listener;
    }

    public interface OnLongClickTabListener {
        void onLongClick(View tab, int position);
    }
}
