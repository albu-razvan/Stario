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

package com.stario.launcher.sheet;

import androidx.annotation.NonNull;

import com.stario.launcher.sheet.behavior.bottom.BottomSheetDialog;
import com.stario.launcher.sheet.behavior.left.LeftSheetDialog;
import com.stario.launcher.sheet.behavior.right.RightSheetDialog;
import com.stario.launcher.sheet.behavior.top.TopSheetDialog;
import com.stario.launcher.themes.ThemedActivity;

public class SheetDialogFactory {
    private static final SheetDialog[] dialogs = new SheetDialog[SheetType.values().length];

    public static SheetDialog forType(@NonNull SheetType type, ThemedActivity activity, int theme) {
        if (dialogs[type.ordinal()] == null ||
                !activity.equals(dialogs[type.ordinal()].getOwnerActivity())) {
            switch (type) {
                case TOP_SHEET: {
                    dialogs[type.ordinal()] = new TopSheetDialog(activity, theme);

                    break;
                }
                case RIGHT_SHEET: {
                    dialogs[type.ordinal()] = new RightSheetDialog(activity, theme);

                    break;
                }
                case BOTTOM_SHEET: {
                    dialogs[type.ordinal()] = new BottomSheetDialog(activity, theme);

                    break;
                }
                case LEFT_SHEET: {
                    dialogs[type.ordinal()] = new LeftSheetDialog(activity, theme);

                    break;
                }
            }
        }

        return dialogs[type.ordinal()];
    }
}
