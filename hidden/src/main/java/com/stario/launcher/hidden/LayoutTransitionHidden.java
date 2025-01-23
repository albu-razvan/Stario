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

package com.stario.launcher.hidden;

import android.animation.LayoutTransition;
import android.view.ViewGroup;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(LayoutTransition.class)
public class LayoutTransitionHidden {
    public void layoutChange(ViewGroup parent) {
        throw new RuntimeException("Stub!");
    }

    public void cancel() {
        throw new RuntimeException("Stub!");
    }
}