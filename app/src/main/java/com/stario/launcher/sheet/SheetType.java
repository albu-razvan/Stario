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
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.stario.launcher.sheet.briefing.dialog.BriefingDialog;
import com.stario.launcher.sheet.drawer.dialog.ApplicationsDialog;
import com.stario.launcher.sheet.widgets.dialog.WidgetsDialog;

import java.util.ArrayList;
import java.util.List;

public enum SheetType {
    TOP_SHEET("com.stario.launcher.TOP_SHEET", View.SCROLL_AXIS_VERTICAL),
    BOTTOM_SHEET("com.stario.launcher.BOTTOM_SHEET", View.SCROLL_AXIS_VERTICAL),
    LEFT_SHEET("com.stario.launcher.LEFT_SHEET", View.SCROLL_AXIS_HORIZONTAL),
    RIGHT_SHEET("com.stario.launcher.RIGHT_SHEET", View.SCROLL_AXIS_HORIZONTAL),
    UNDEFINED("com.stario.launcher.UNDEFINED", View.SCROLL_AXIS_NONE);

    private static final String TAG = "SheetType";

    private final String stringType;
    private final int axes;

    SheetType(String stringType, int axes) {
        this.stringType = stringType;
        this.axes = axes;
    }

    public int getAxes() {
        return axes;
    }

    public static SheetType deserialize(String serial) {
        if (serial == null) {
            return null;
        }

        if (serial.equals(TOP_SHEET.stringType)) {
            return TOP_SHEET;
        } else if (serial.equals(RIGHT_SHEET.stringType)) {
            return RIGHT_SHEET;
        } else if (serial.equals(BOTTOM_SHEET.stringType)) {
            return BOTTOM_SHEET;
        } else if (serial.equals(LEFT_SHEET.stringType)) {
            return LEFT_SHEET;
        } else if (serial.equals(UNDEFINED.stringType)) {
            return UNDEFINED;
        }

        return null;
    }

    public static List<Pair<SheetType, Class<? extends SheetDialogFragment>>> getActiveSheets(
            @NonNull SharedPreferences preferences) {
        List<Pair<SheetType, Class<? extends SheetDialogFragment>>> list = new ArrayList<>();
        preferences.getAll().forEach((string, object) -> {
            try {
                Class<?> clazz = Class.forName(string);

                if (SheetDialogFragment.class.isAssignableFrom(clazz)) {
                    if (object instanceof String) {
                        SheetType type = deserialize((String) object);

                        if (type != null) {
                            // noinspection unchecked
                            list.add(new Pair<>(type, (Class<? extends SheetDialogFragment>) clazz));
                        } else {
                            Log.e(TAG, "getSheets: " + object + " does not map to a valid " + SheetType.class.getName() + " serial String.");
                            preferences.edit().remove(string).apply();
                        }
                    } else {
                        Log.e(TAG, "getSheets: " + string + " can only map to a " + SheetType.class.getName() + " serial String.");
                        preferences.edit().remove(string).apply();
                    }
                } else {
                    Log.e(TAG, "getSheets: " + string + " does not extend " + SheetDialogFragment.class.getName());
                    preferences.edit().remove(string).apply();
                }
            } catch (ClassNotFoundException exception) {
                Log.e(TAG, "getSheets: Could not get class " + string);
                preferences.edit().remove(string).apply();
            }
        });

        return list;
    }

    public static SheetType getSheetTypeForSheetDialogFragment(
            @NonNull Class<? extends SheetDialogFragment> clazz, @NonNull SharedPreferences preferences) {
        SheetType type = null;
        String typeString = preferences.getString(clazz.getName(), null);

        if (typeString != null) {
            type = SheetType.deserialize(typeString);
        }

        if (type != null) {
            return type;
        }

        return getDefaultSheetTypeForSheetDialogFragment(clazz, preferences);
    }

    private static SheetType getDefaultSheetTypeForSheetDialogFragment(
            Class<? extends SheetDialogFragment> clazz, SharedPreferences preferences) {
        SheetType type = null;

        if (clazz == ApplicationsDialog.class) {
            preferences.edit()
                    .putString(ApplicationsDialog.class.getName(),
                            SheetType.BOTTOM_SHEET.toString())
                    .apply();

            type = SheetType.BOTTOM_SHEET;
        } else if (clazz == BriefingDialog.class) {
            preferences.edit()
                    .putString(BriefingDialog.class.getName(),
                            SheetType.LEFT_SHEET.toString())
                    .apply();

            type = SheetType.LEFT_SHEET;
        } else if (clazz == WidgetsDialog.class) {
            preferences.edit()
                    .putString(WidgetsDialog.class.getName(),
                            SheetType.RIGHT_SHEET.toString())
                    .apply();

            type = SheetType.RIGHT_SHEET;
        }

        return type;
    }

    @NonNull
    @Override
    public String toString() {
        return stringType;
    }
}
