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

package com.stario.launcher.ui.utils;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.IntRange;

public class LayoutSizeObserver {
    public static final int WIDTH = 0b000001;
    public static final int HEIGHT = 0b000010;
    public static final int LEFT = 0b000100;
    public static final int TOP = 0b001000;
    public static final int RIGHT = 0b010000;
    public static final int BOTTOM = 0b100000;


    public static void attach(View view, @IntRange(from = 1, to = 0b111111) int watchFlags,
                              OnChange listener) {
        attach(view, watchFlags, listener, true);
    }

    public static void attach(View view, @IntRange(from = 1, to = 0b111111) int watchFlags,
                              OnChange listener, boolean invalidateOnAttach) {
        View.OnLayoutChangeListener viewChangeListener = new View.OnLayoutChangeListener() {
            private Rect rect;

            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                Rect rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());

                if (this.rect == null) {
                    int flags = WIDTH | HEIGHT | LEFT | TOP | RIGHT | BOTTOM;
                    this.rect = rect;

                    listener.onChange(view, flags & watchFlags);
                    listener.onChange(view, flags & watchFlags, rect);
                }

                int flags = 0;

                if (this.rect.width() != rect.width()) {
                    flags |= WIDTH;
                }

                if (this.rect.height() != rect.height()) {
                    flags |= HEIGHT;
                }

                if (this.rect.left != rect.left) {
                    flags |= LEFT;
                }

                if (this.rect.top != rect.top) {
                    flags |= TOP;
                }

                if (this.rect.right != rect.right) {
                    flags |= RIGHT;
                }

                if (this.rect.bottom != rect.bottom) {
                    flags |= BOTTOM;
                }

                this.rect = rect;
                if ((flags & watchFlags) != 0) {
                    listener.onChange(view, flags & watchFlags);
                    listener.onChange(view, flags & watchFlags, rect);
                }
            }
        };

        if (invalidateOnAttach) {
            viewChangeListener.onLayoutChange(view, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        view.addOnLayoutChangeListener(viewChangeListener);
    }

    public interface OnChange {
        default void onChange(View view, int watchFlags) {
        }

        default void onChange(View view, int watchFlags, Rect rect) {
        }
    }
}
