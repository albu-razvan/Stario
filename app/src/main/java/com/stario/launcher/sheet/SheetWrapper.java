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

package com.stario.launcher.sheet;

import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.stario.launcher.activities.Launcher;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.sheet.behavior.SheetBehavior;

public class SheetWrapper {
    private static final String TAG = "SheetWrapper";
    private final static SheetWrapper[] instances = new SheetWrapper[SheetType.values().length];
    private SheetDialogFragment.OnSlideListener slideListener;
    private SheetDialogFragment dialog;
    private OnShowRequest showRequest;

    private SheetWrapper(Launcher launcher, SheetType type,
                         @NonNull SheetDialogFragment.OnSlideListener listener) {
        this.slideListener = listener;

        try {
            dialog = SheetDialogFactory.forType(type, launcher.getSharedPreferences(Entry.APPLICATION));
            dialog.setCancelable(false);

            showRequest = () -> {
                FragmentManager manager = launcher.getSupportFragmentManager();

                if (!manager.isDestroyed() && manager.findFragmentByTag(type.toString()) == null) {
                    dialog.show(manager, type.toString());
                }

                showRequest = null;
            };
        } catch (IllegalArgumentException exception) {
            Log.e(TAG, "Cannot inflate dialog.\n", exception);
        }
    }

    public static void wrapInDialog(Launcher launcher, SheetType type,
                                    @NonNull SheetDialogFragment.OnSlideListener slideListener) {
        if (instances[type.ordinal()] != null) {
            SheetWrapper wrapper = instances[type.ordinal()];
            wrapper.slideListener = slideListener;

            wrapper.showRequest = () -> {
                FragmentManager manager = launcher.getSupportFragmentManager();

                if (!manager.isDestroyed() && manager.findFragmentByTag(type.toString()) == null) {
                    wrapper.dialog.show(manager, type.toString());
                }

                wrapper.showRequest = null;
            };
        } else {
            instances[type.ordinal()] = new SheetWrapper(launcher, type, slideListener);
        }
    }

    static boolean update(SheetDialogFragment dialog, SheetType type) {
        if (dialog != null) {
            SheetWrapper wrapper = instances[type.ordinal()];

            if (wrapper != null) {
                if (dialog.getType() == type) {
                    if (!dialog.isResumed()) {
                        wrapper.dialog = dialog;

                        return true;
                    } else {
                        Log.e(TAG, "updateDialog: Dialog must not be resumed when updating");
                    }
                } else {
                    Log.e(TAG, "updateDialog: Dialog must have the same SheetType " +
                            "as the old one. Previous: " + type + "Current: " + dialog.getType());
                }
            } else {
                Log.e(TAG, "updateDialog: No instance to update of type " + dialog.getType());
            }
        } else {
            Log.e(TAG, "updateDialog: Dialog must not be null");
        }

        return false;
    }

    static void dispatchSlide(SheetType type, float slideOffset) {
        SheetWrapper wrapper = instances[type.ordinal()];

        if (wrapper != null && wrapper.slideListener != null) {
            wrapper.slideListener.onSlide(slideOffset);
        }
    }

    static void sendMotionEvent(SheetType type, MotionEvent event) {
        SheetWrapper wrapper = instances[type.ordinal()];

        if (wrapper != null) {
            if (wrapper.dialog.isAdded()) {
                wrapper.dialog.sendMotionEvent(event);
            } else if (wrapper.showRequest != null) {
                wrapper.showRequest.show();
            } else {
                instances[type.ordinal()] = null;
            }
        }
    }

    static boolean active() {
        for (SheetWrapper wrapper : instances) {
            if (wrapper != null) {
                SheetBehavior<?> behavior = wrapper.dialog.getBehavior();

                if (behavior != null && behavior.getState() != SheetBehavior.STATE_COLLAPSED) {
                    return true;
                }
            }
        }

        return false;
    }

    public static void requestIgnoreCurrentTouchEvent(boolean enabled) {
        for (SheetWrapper instance : instances) {
            if (instance != null) {
                instance.dialog.requestIgnoreCurrentTouchEvent(enabled);
            }
        }
    }

    private interface OnShowRequest {
        void show();
    }
}
