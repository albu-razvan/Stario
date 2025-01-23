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

package com.stario.launcher.utils.animation;

import android.transition.Fade;
import android.transition.TransitionSet;
import android.view.View;

import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.google.android.material.transition.platform.MaterialElevationScale;

import java.util.List;

public class FragmentTransition extends TransitionSet {
    public FragmentTransition(boolean growing) {
        this(growing, null);
    }

    public <V extends View> FragmentTransition(boolean growing, List<V> exclusions) {
        setOrdering(ORDERING_TOGETHER);

        addTransition(new Fade());
        addTransition(new MaterialElevationScale(growing));

        if (exclusions != null) {
            for (V view : exclusions) {
                excludeTarget(view, true);
            }
        }

        setInterpolator(new FastOutSlowInInterpolator());
        setDuration(Animation.LONG.getDuration());
    }
}