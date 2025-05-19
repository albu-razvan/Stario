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

package com.stario.launcher.ui.recyclers.autogrid;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.stario.launcher.ui.recyclers.overscroll.OverScrollRecyclerView;

public class AutoGridRecyclerView extends OverScrollRecyclerView {
    private LayoutManager layoutManager;

    public AutoGridRecyclerView(Context context) {
        super(context);

        this.layoutManager = null;
    }

    public AutoGridRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.layoutManager = null;
    }

    public AutoGridRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        this.layoutManager = null;
    }

    @Override
    public void setLayoutManager(@Nullable LayoutManager layout) {
        super.setLayoutManager(layout);

        this.layoutManager = layout;
    }

    @Override
    public void setAdapter(@Nullable Adapter adapter) {
        super.setAdapter(adapter);

        if (layoutManager instanceof AutoGridLayoutManager) {
            ((AutoGridLayoutManager) layoutManager).setAdapter(getAdapter());
        }
    }
}
