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

package com.stario.launcher.utils.objects;

import androidx.annotation.NonNull;

public class ObjectDelegate<T> {
    private final ObjectDelegateAction<T> action;
    private T object;

    public ObjectDelegate(@NonNull ObjectDelegateAction<T> action) {
        this(null, action);
    }

    public ObjectDelegate(T object, @NonNull ObjectDelegateAction<T> action) {
        this.object = object;
        this.action = action;
    }

    public T getValue() {
        return object;
    }

    public void setValue(T object) {
        this.object = object;

        action.onSet(object);
    }

    public interface ObjectDelegateAction<T> {
        void onSet(T object);
    }
}
