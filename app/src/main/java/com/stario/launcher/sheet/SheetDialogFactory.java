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
    public static SheetDialog forType(@NonNull SheetType type, ThemedActivity activity, int theme) {
        switch (type) {
            case TOP_SHEET: {
                return new TopSheetDialog(activity, theme);
            }
            case RIGHT_SHEET: {
                return new RightSheetDialog(activity, theme);
            }
            case BOTTOM_SHEET: {
                return new BottomSheetDialog(activity, theme);
            }
            case LEFT_SHEET: {
                return new LeftSheetDialog(activity, theme);
            }
        }

        return null;
    }
}
