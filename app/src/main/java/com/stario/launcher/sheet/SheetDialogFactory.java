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

import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.stario.launcher.sheet.behavior.bottom.BottomSheetDialog;
import com.stario.launcher.sheet.behavior.left.LeftSheetDialog;
import com.stario.launcher.sheet.behavior.right.RightSheetDialog;
import com.stario.launcher.sheet.behavior.top.TopSheetDialog;
import com.stario.launcher.sheet.briefing.dialog.BriefingDialog;
import com.stario.launcher.sheet.drawer.dialog.ApplicationsDialog;
import com.stario.launcher.sheet.notes.dialog.NotesDialog;
import com.stario.launcher.sheet.widgets.dialog.WidgetsDialog;
import com.stario.launcher.themes.ThemedActivity;

import java.lang.reflect.Constructor;

public class SheetDialogFactory {
    private static final SheetDialog[] dialogs = new SheetDialog[SheetType.values().length];
    private static final String PREFIX = "sheet:";

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

    @SuppressWarnings("unchecked")
    @NonNull
    public static SheetDialogFragment forType(SheetType type, SharedPreferences preferences)
            throws IllegalArgumentException {

        if (type != SheetType.TOP_SHEET && type != SheetType.RIGHT_SHEET
                && type != SheetType.BOTTOM_SHEET && type != SheetType.LEFT_SHEET) {
            throw new IllegalArgumentException("Type must be SheetType.TOP_SHEET, SheetType.RIGHT_SHEET, " +
                    "SheetType.BOTTOM_SHEET or SheetType.LEFT_SHEET");
        }

        try {
            String className = preferences.getString(PREFIX + type, null);

            if (className != null) {
                Class<?> savedClass = getClassFromString(className);

                if (savedClass != null && extendsSheetDialogFragment(savedClass)) {
                    Constructor<SheetDialogFragment> constructor =
                            (Constructor<SheetDialogFragment>) savedClass.getConstructor(SheetType.class);

                    return constructor.newInstance(type);
                }
            } else {
                return getDefaultDialogForType(type, preferences);
            }
        } catch (Exception exception) {
            Log.e("SheetDialogFactory", "forType: Unknown exception when creating dialog. Defaulting..." +
                    '\n' + exception.getMessage() + '\n' + exception.getStackTrace()[0]);

            return getDefaultDialogForType(type, preferences);
        }

        return getDefaultDialogForType(type, preferences);
    }

    private static Class<?> getClassFromString(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static boolean extendsSheetDialogFragment(@NonNull Class<?> testedClass) {
        boolean extendsClass = false;

        while (!extendsClass && testedClass != null) {
            testedClass = testedClass.getSuperclass();

            if (testedClass != null && testedClass.equals(SheetDialogFragment.class))
                extendsClass = true;
        }

        return extendsClass;
    }

    @NonNull
    private static SheetDialogFragment getDefaultDialogForType(SheetType type, SharedPreferences preferences) {
        SheetDialogFragment dialogFragment;

        if (type == SheetType.TOP_SHEET) {
            preferences.edit()
                    .putString(PREFIX + type, WidgetsDialog.class.getName())
                    .apply();

            dialogFragment = new WidgetsDialog(SheetType.TOP_SHEET);
        } else if (type == SheetType.RIGHT_SHEET) {
            preferences.edit()
                    .putString(PREFIX + type, NotesDialog.class.getName())
                    .apply();

            dialogFragment = new NotesDialog(SheetType.RIGHT_SHEET);
        } else if (type == SheetType.BOTTOM_SHEET) {
            preferences.edit()
                    .putString(PREFIX + type, ApplicationsDialog.class.getName())
                    .apply();

            dialogFragment = new ApplicationsDialog(SheetType.BOTTOM_SHEET);
        } else { // will always be SheetType.LEFT_SHEET
            preferences.edit()
                    .putString(PREFIX + type, BriefingDialog.class.getName())
                    .apply();

            dialogFragment = new BriefingDialog(SheetType.LEFT_SHEET);
        }

        return dialogFragment;
    }
}
