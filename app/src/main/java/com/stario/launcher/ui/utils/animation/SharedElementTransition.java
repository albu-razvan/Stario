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

import android.util.Pair;
import android.view.View;
import android.view.animation.PathInterpolator;

import androidx.transition.ChangeBounds;
import androidx.transition.ChangeTransform;
import androidx.transition.Transition;
import androidx.transition.TransitionSet;

import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SharedElementTransition extends TransitionSet {
    public SharedElementTransition(List<View> targets) {
        setOrdering(ORDERING_TOGETHER);

        ChangeBounds iconChangeBounds = new ChangeBounds();
        iconChangeBounds.setResizeClip(true);

        addTransition(iconChangeBounds);

        // https://issuetracker.google.com/issues/339169168
        // It has not been fixed...
        ChangeTransform changeTransform = new ChangeTransform();
        // This has to be false to avoid the issue
        changeTransform.setReparentWithOverlay(false);
        changeTransform.addListener(new TransitionListener() {
            private final Map<Transition, Set<Pair<@NonNull View, @NonNull Integer>>> startingVisibility;

            {
                this.startingVisibility = new HashMap<>();
            }

            private void reset(Transition transition) {
                Set<Pair<View, Integer>> startingVisibilityForTransition = startingVisibility.remove(transition);

                if (startingVisibilityForTransition != null) {
                    for (Pair<View, Integer> pair : startingVisibilityForTransition) {
                        pair.first.setVisibility(pair.second);
                    }
                }
            }

            @Override
            public void onTransitionStart(@NonNull Transition transition) {
                for (View target : targets) {
                    Set<Pair<View, Integer>> startingVisibilityForTransition =
                            startingVisibility.computeIfAbsent(transition, k -> new HashSet<>());
                    startingVisibilityForTransition.add(new Pair<>(target, target.getVisibility()));

                    target.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onTransitionEnd(@NonNull Transition transition) {
                reset(transition);
            }

            @Override
            public void onTransitionCancel(@NonNull Transition transition) {
                reset(transition);
            }

            @Override
            public void onTransitionPause(@NonNull Transition transition) {
            }

            @Override
            public void onTransitionResume(@NonNull Transition transition) {
            }
        });

        addTransition(changeTransform);

        setPathMotion(new SharedElementMotion());
        setInterpolator(new PathInterpolator(0.3f,
                0.9f, 0.3f, 0.95f));

        setDuration(Animation.LONG.getDuration());
    }

    @Override
    public boolean isSeekingSupported() {
        // ChangeTransform is not seekable, suppress the logcat warning
        return true;
    }
}