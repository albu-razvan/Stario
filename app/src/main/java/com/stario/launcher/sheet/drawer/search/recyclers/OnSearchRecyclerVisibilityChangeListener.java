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

package com.stario.launcher.sheet.drawer.search.recyclers;

import android.view.View;

import com.stario.launcher.sheet.drawer.search.SearchLayoutTransition;

public class OnSearchRecyclerVisibilityChangeListener implements OnVisibilityChangeListener {
    private final SearchLayoutTransition transition;

    public OnSearchRecyclerVisibilityChangeListener(SearchLayoutTransition transition) {
        this.transition = transition;
    }

    @Override
    public void onPreChange(View view, int visibility) {
        if (view.getVisibility() != visibility) {
            transition.setAnimate(false);
            transition.cancel();
        }
    }

    @Override
    public void onChange(View view, int visibility) {
        transition.setAnimate(true);
    }
}
