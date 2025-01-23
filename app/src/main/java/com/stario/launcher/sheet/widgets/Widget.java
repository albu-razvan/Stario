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

package com.stario.launcher.sheet.widgets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stario.launcher.utils.Utils;

public class Widget implements Comparable<Widget> {
    public final int id;
    public int position; // up-down
    public WidgetSize size;

    public Widget(int id, int position, WidgetSize size) {
        this.id = id;
        this.position = position;
        this.size = size;
    }

    public static Widget deserialize(String data) {
        try {
            Widget holder = Utils.getGsonInstance()
                    .fromJson(data, Widget.class);

            if (holder.size == null || holder.id == -1 || holder.position == -1) {
                return null;
            }

            return holder;
        } catch (Exception exception) {
            return null;
        }
    }

    public String serialize() {
        return Utils.getGsonInstance().toJson(this);
    }

    @Override
    public boolean equals(@Nullable Object object) {
        return object instanceof Widget && ((Widget) object).id == id;
    }

    @Override
    public int compareTo(@NonNull Widget widget) {
        return position - widget.position;
    }
}