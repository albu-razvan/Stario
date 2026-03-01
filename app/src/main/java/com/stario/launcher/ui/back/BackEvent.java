/*
 * Copyright (C) 2026 Răzvan Albu
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

package com.stario.launcher.ui.back;

public class BackEvent {
    public final BackEventType type;
    public final float progress;
    public final Class<?> origin;


    public BackEvent(BackEventType type, float progress, Class<?> origin) {
        this.type = type;
        this.origin = origin;
        this.progress = progress;
    }

    public BackEvent(BackEventType type, Class<?> origin) {
        this(type, 0f, origin);
    }
}