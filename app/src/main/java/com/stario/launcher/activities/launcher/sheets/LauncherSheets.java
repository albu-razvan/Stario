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

package com.stario.launcher.activities.launcher.sheets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.stario.launcher.activities.launcher.Launcher;
import com.stario.launcher.sheet.SheetDialog;
import com.stario.launcher.sheet.SheetDialogFragment;
import com.stario.launcher.sheet.SheetsFocusController;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class LauncherSheets {
    public static final String INTENT_SHEET_CLASS_EXTRA = "com.stario.launcher.INTENT_SHEET_CLASS_EXTRA";
    public static final String ACTION_REMOVE_SHEET = "com.stario.launcher.ACTION_REMOVE_SHEET";
    public static final String ACTION_MOVE_SHEET = "com.stario.launcher.ACTION_MOVE_SHEET";
    public static final String ACTION_ADD_SHEET = "com.stario.launcher.ACTION_ADD_SHEET";

    public static void attach(Launcher launcher,
                              SheetDialog.OnSlideListener slideListener) {
        SheetsFocusController controller = launcher.getSheetsController();

        controller.setSlideListener(slideListener);
        controller.addSheetDialog(launcher, SheetDialogFragment.IMPLEMENTATIONS);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_ADD_SHEET);
        filter.addAction(ACTION_MOVE_SHEET);
        filter.addAction(ACTION_REMOVE_SHEET);

        LocalBroadcastManager.getInstance(launcher).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Class<? extends SheetDialogFragment>[] classes = getClasses(intent);
                if (classes == null) {
                    return;
                }

                String action = intent.getAction();
                if (action == null) {
                    return;
                }

                switch (action) {
                    case ACTION_ADD_SHEET: {
                        controller.addSheetDialog(launcher, classes);
                        break;
                    }
                    case ACTION_MOVE_SHEET: {
                        controller.moveSheetDialog(launcher, classes);
                        break;
                    }
                    case ACTION_REMOVE_SHEET: {
                        controller.removeSheetDialog(classes);
                        break;
                    }
                }
            }
        }, filter);
    }

    private static Class<? extends SheetDialogFragment>[] getClasses(Intent intent) {
        if (intent == null) {
            return null;
        }

        Serializable extra = intent.getSerializableExtra(INTENT_SHEET_CLASS_EXTRA);

        if (extra == null) {
            return null;
        }

        List<Class<? extends SheetDialogFragment>> classes = new ArrayList<>();

        if (extra instanceof Class<?> &&
                SheetDialogFragment.class.isAssignableFrom((Class<?>) extra)) {
            // noinspection unchecked
            classes.add((Class<? extends SheetDialogFragment>) extra);
        } else if (extra.getClass().isArray()) {
            // noinspection DataFlowIssue
            Object[] array = (Object[]) extra;

            for (Object element : array) {
                if (element instanceof Class<?> &&
                        SheetDialogFragment.class.isAssignableFrom((Class<?>) element)) {
                    // noinspection unchecked
                    classes.add((Class<? extends SheetDialogFragment>) element);
                }
            }
        }

        if (classes.isEmpty()) {
            return null;
        }

        // noinspection ToArrayCallWithZeroLengthArrayArgument, unchecked
        return classes.toArray(new Class[classes.size()]);
    }
}
