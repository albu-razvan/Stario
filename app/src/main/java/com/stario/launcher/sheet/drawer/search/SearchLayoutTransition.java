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

package com.stario.launcher.sheet.drawer.search;

import android.animation.LayoutTransition;
import android.view.ViewGroup;

import com.stario.launcher.hidden.LayoutTransitionHidden;

import dev.rikka.tools.refine.Refine;

public class SearchLayoutTransition extends LayoutTransitionHidden {
    private final LayoutTransition transition;
    private boolean animate;

    public SearchLayoutTransition() {
        super();

        this.animate = false;
        this.transition = Refine.unsafeCast(this);

        transition.enableTransitionType(LayoutTransition.CHANGING);
        transition.disableTransitionType(LayoutTransition.APPEARING);
        transition.disableTransitionType(LayoutTransition.DISAPPEARING);
        transition.disableTransitionType(LayoutTransition.CHANGE_APPEARING);
        transition.disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
    }

    @Override
    public void layoutChange(ViewGroup parent) {
        if (animate) {
            super.layoutChange(parent);
        }
    }

    public void setAnimate(boolean animate) {
        this.animate = animate;
    }

    public LayoutTransition getUnrefinedTransition() {
        return transition;
    }
}

