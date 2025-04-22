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

package com.stario.launcher.apps.interfaces;

import com.stario.launcher.apps.LauncherApplication;

public interface LauncherApplicationListener {
        default void onInserted(LauncherApplication application) {
        }

        default void onShowed(LauncherApplication application) {
        }

        /**
         * This will always be called before and in the same UI frame as {@link #onRemoved(LauncherApplication)}
         */
        default void onPrepareRemoval() {
        }

        /**
         * This will always be called after and in the same UI frame as {@link #onPrepareRemoval()}
         */
        default void onRemoved(LauncherApplication application) {
        }

        /**
         * This will always be called before and in the same UI frame as {@link #onHidden(LauncherApplication)}
         */
        default void onPrepareHiding() {
        }

        /**
         * This will always be called after and in the same UI frame as {@link #onPrepareHiding()}
         */
        default void onHidden(LauncherApplication application) {
        }

        default void onUpdated(LauncherApplication application) {
        }
    }