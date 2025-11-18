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

package com.stario.launcher.ui.utils.animation;

import android.view.View;
import android.view.ViewGroup;

import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.transition.Fade;
import androidx.transition.Transition;
import androidx.transition.TransitionPropagation;
import androidx.transition.TransitionSet;
import androidx.transition.TransitionValues;

import com.google.android.material.transition.MaterialElevationScale;
import com.stario.launcher.R;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class FragmentTransition extends TransitionSet {
    private static class StaggerPropagation extends TransitionPropagation {
        private static final String PROP_STAGGER_INDEX = "com.stario.launcher:propagation:staggerIndex";
        private static final long STAGGER_DELAY_MS = 20;

        @Override
        public long getStartDelay(@NonNull ViewGroup sceneRoot, @NonNull Transition transition,
                                  @Nullable TransitionValues startValues, @Nullable TransitionValues endValues) {
            if (endValues == null || !endValues.values.containsKey(PROP_STAGGER_INDEX)) {
                return 0;
            }

            Object staggerIndex = endValues.values.get(PROP_STAGGER_INDEX);
            int index = (staggerIndex instanceof Integer) ? (Integer) staggerIndex : 0;

            return index * STAGGER_DELAY_MS;
        }

        @Override
        public void captureValues(@NonNull TransitionValues transitionValues) {
            Object tag = transitionValues.view.getTag(R.id.stagger_order_tag);
            transitionValues.values.put(PROP_STAGGER_INDEX, tag instanceof Integer ? tag : 0);
        }

        @Override
        public String @Nullable [] getPropagationProperties() {
            return new String[]{PROP_STAGGER_INDEX};
        }
    }

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
        setPropagation(new StaggerPropagation());
    }
}