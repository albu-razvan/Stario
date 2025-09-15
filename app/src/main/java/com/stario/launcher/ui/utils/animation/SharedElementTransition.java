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

import android.transition.ChangeBounds;
import android.transition.ChangeTransform;
import android.transition.TransitionSet;
import android.view.animation.PathInterpolator;

import com.stario.launcher.ui.icons.AdaptiveIconView;

public abstract class SharedElementTransition extends TransitionSet {
    public SharedElementTransition() {
        setOrdering(ORDERING_TOGETHER);

        ChangeBounds iconChangeBounds = new ChangeBounds();
        iconChangeBounds.setResizeClip(true);
        iconChangeBounds.addTarget(AdaptiveIconView.class);

        addTransition(iconChangeBounds);
        addTransition(new ChangeTransform());
    }

    public static class SharedAppFolderTransition extends SharedElementTransition {
        public SharedAppFolderTransition() {
            setPathMotion(new SharedElementMotion());
            setInterpolator(new PathInterpolator(0.3f, 0.9f, 0.3f, 0.95f));
        }
    }
}