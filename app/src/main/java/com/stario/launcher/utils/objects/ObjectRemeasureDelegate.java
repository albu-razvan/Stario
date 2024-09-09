/*
    Copyright (C) 2024 Răzvan Albu

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.stario.launcher.utils.objects;

import android.view.View;

import androidx.annotation.NonNull;

public class ObjectRemeasureDelegate<T> {
    private final View view;
    private T object;

    public ObjectRemeasureDelegate(@NonNull View view) {
        this(view, null);
    }

    public ObjectRemeasureDelegate(@NonNull View view, T object) {
        this.view = view;

        this.object = object;
    }

    public T getValue() {
        return object;
    }

    public void setValue(T object) {
        this.object = object;

        view.requestLayout();
    }
}