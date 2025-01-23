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

package com.stario.launcher.sheet.behavior.right;

import android.view.View;

import androidx.annotation.NonNull;

import com.stario.launcher.R;
import com.stario.launcher.sheet.SheetCoordinator;
import com.stario.launcher.sheet.SheetDialog;
import com.stario.launcher.sheet.SheetType;
import com.stario.launcher.sheet.behavior.SheetBehavior;
import com.stario.launcher.themes.ThemedActivity;

public class RightSheetDialog extends SheetDialog {
    private SheetCoordinator container;

    public RightSheetDialog(ThemedActivity activity, int themeResId) {
        super(activity, themeResId);
    }

    @Override
    protected SheetCoordinator getContainer() {
        if (container == null) {
            container = (SheetCoordinator) View.inflate(getContext(), R.layout.right_sheet_dialog, null);

            sheet = container.findViewById(R.id.design_right_sheet);
            behavior = SheetBehavior.from(sheet);
        }

        return container;
    }

    @NonNull
    @Override
    public SheetType getType() {
        return SheetType.RIGHT_SHEET;
    }
}
