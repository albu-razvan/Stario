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

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;

import com.stario.launcher.activities.Launcher;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.sheet.behavior.SheetBehavior;

import java.util.ArrayList;
import java.util.List;

public class SheetsFocusController extends ConstraintLayout {
    private CheckForLongPress pendingCheckForLongPress;
    private View.OnLongClickListener longClickListener;
    private boolean hasPerformedLongPress;
    private List<Integer> targetPointers;
    private boolean shouldDispatch;
    private float deltaX, deltaY;
    private SheetType sheetType;
    private float moveSlop;
    private float X, Y;

    public SheetsFocusController(@NonNull Context context) {
        super(context);

        init(context);
    }

    public SheetsFocusController(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public SheetsFocusController(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    private void init(Context context) {
        targetPointers = new ArrayList<>();
        sheetType = SheetType.UNDEFINED;
        moveSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        shouldDispatch = false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            shouldDispatch = Wrapper.getActiveSheetCount() == 0;

            targetPointers.add(0,
                    ev.getAction() >> MotionEvent.ACTION_POINTER_INDEX_SHIFT);

            X = ev.getX(getPointer(ev));
            Y = ev.getY(getPointer(ev));

            deltaX = 0;
            deltaY = 0;

            if (shouldDispatch) {
                Wrapper.sendMotionEvent(MotionEvent.obtain(ev));
            }

            super.onInterceptTouchEvent(ev);
            return false;
        } else {
            if (action == MotionEvent.ACTION_UP ||
                    action == MotionEvent.ACTION_CANCEL) {
                sheetType = SheetType.UNDEFINED;
                targetPointers.clear();

                if (shouldDispatch) {
                    Wrapper.sendMotionEvent(MotionEvent.obtain(ev));
                }

                removeCheck();

                super.onInterceptTouchEvent(ev);
                return false;
            } else {
                if (hasPerformedLongPress) {
                    removeCheck();

                    super.onInterceptTouchEvent(ev);
                    return false;
                }

                deltaY = Y - ev.getY(getPointer(ev));
                deltaX = X - ev.getX(getPointer(ev));

                super.onInterceptTouchEvent(ev);
                return Math.abs(deltaY) >= moveSlop ||
                        Math.abs(deltaX) >= moveSlop;
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getAction();

        if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_CANCEL) {
            if (shouldDispatch) {
                Wrapper.sendMotionEvent(MotionEvent.obtain(ev));
            }
            removeCheck();

            sheetType = SheetType.UNDEFINED;
            targetPointers.clear();
        } else {
            if (action == MotionEvent.ACTION_DOWN) {
                targetPointers.add(0,
                        ev.getAction() >> MotionEvent.ACTION_POINTER_INDEX_SHIFT);

                X = ev.getX(getPointer(ev));
                Y = ev.getY(getPointer(ev));

                deltaX = 0;
                deltaY = 0;

                if (shouldDispatch) {
                    Wrapper.sendMotionEvent(MotionEvent.obtain(ev));
                }

                postCheckForLongClick();
            } else {
                if (hasPerformedLongPress) {
                    removeCheck();

                    return super.onTouchEvent(ev);
                }

                deltaY = Y - ev.getY(getPointer(ev));
                deltaX = X - ev.getX(getPointer(ev));

                if (ev.getAction() == MotionEvent.ACTION_MOVE &&
                        (Math.abs(deltaX) >= moveSlop ||
                                Math.abs(deltaY) >= moveSlop)) {
                    if (sheetType == SheetType.UNDEFINED) {
                        if (Math.abs(deltaY) > Math.abs(deltaX)) {
                            sheetType = Math.signum(deltaY) >= 0 ? SheetType.BOTTOM_SHEET : SheetType.TOP_SHEET;
                        } else {
                            sheetType = Math.signum(deltaX) >= 0 ? SheetType.RIGHT_SHEET : SheetType.LEFT_SHEET;
                        }
                    }

                    removeCheck();
                }

                if (shouldDispatch) {
                    Wrapper.sendMotionEvent(sheetType, MotionEvent.obtain(ev));
                }
            }
        }

        return true;
    }

    private int getPointer(MotionEvent event) {
        return Math.max(0, Math.min(event.getPointerCount() - 1,
                event.findPointerIndex(targetPointers.get(0))));
    }

    private void removeCheck() {
        if (pendingCheckForLongPress != null) {
            removeCallbacks(pendingCheckForLongPress);

            pendingCheckForLongPress = null;
        }
    }

    private void postCheckForLongClick() {
        if (pendingCheckForLongPress == null) {
            hasPerformedLongPress = false;

            pendingCheckForLongPress = new CheckForLongPress();
            postDelayed(pendingCheckForLongPress, ViewConfiguration.getLongPressTimeout());
        }
    }

    private void hideAllSheets() {
        Wrapper.hideAll();
    }

    @Override
    public void setOnLongClickListener(@Nullable OnLongClickListener listener) {
        longClickListener = listener;
    }

    @Override
    public boolean performLongClick() {
        return false; // override default long click logic
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        removeCheck();
    }

    public float getLastX() {
        return X;
    }

    public float getLastY() {
        return Y;
    }

    private class CheckForLongPress implements Runnable {
        private final int mOriginalWindowAttachCount;

        private CheckForLongPress() {
            mOriginalWindowAttachCount = getWindowAttachCount();
        }

        public void run() {
            if (getParent() != null // ignore window focus because of shown sheets that are ready to intercept input
                    && mOriginalWindowAttachCount == getWindowAttachCount()
                    && !hasPerformedLongPress) {
                if (longClickListener != null &&
                        longClickListener.onLongClick(SheetsFocusController.this)) {
                    hasPerformedLongPress = true;

                    if (pendingCheckForLongPress != null) {
                        removeCallbacks(pendingCheckForLongPress);
                    }

                    hideAllSheets();
                }
            }
        }
    }

    public static class Wrapper {
        private static final String TAG = "SheetWrapper";
        private final static Wrapper[] instances = new Wrapper[SheetType.values().length];
        private SheetDialogFragment dialog;
        private Wrapper.OnShowRequest showRequest;

        private Wrapper(Launcher launcher, SheetType type,
                        @NonNull SheetDialogFragment.OnSlideListener listener) {
            try {
                dialog = SheetDialogFactory.forType(type, launcher.getSharedPreferences(Entry.STARIO));
                dialog.setCancelable(false);

                dialog.setOnSlideListener(listener);

                updateShowListener(this, type, launcher);
            } catch (IllegalArgumentException exception) {
                Log.e(TAG, "Cannot inflate dialog.\n", exception);
            }
        }

        public static void wrapInDialog(Launcher launcher, SheetType type,
                                        @NonNull SheetDialogFragment.OnSlideListener slideListener) {
            Wrapper wrapper;

            if (instances[type.ordinal()] != null) {
                wrapper = instances[type.ordinal()];
                wrapper.dialog.setOnSlideListener(slideListener);

                updateShowListener(wrapper, type, launcher);
            } else {
                wrapper = new Wrapper(launcher, type, slideListener);
            }

            instances[type.ordinal()] = wrapper;
        }

        private static void updateShowListener(Wrapper wrapper, SheetType type, Launcher launcher) {
            wrapper.showRequest = () -> {
                FragmentManager manager = launcher.getSupportFragmentManager();

                if (!manager.isDestroyed() && manager.findFragmentByTag(type.toString()) == null) {
                    wrapper.dialog.show(manager, type.toString());
                }

                wrapper.showRequest = null;
            };
        }

        static boolean update(SheetDialogFragment dialog, SheetType type) {
            if (dialog != null) {
                Wrapper wrapper = instances[type.ordinal()];

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

        private static void sendMotionEvent(MotionEvent event) {
            for (Wrapper wrapper : instances) {
                if (wrapper != null &&
                        wrapper.dialog != null) {

                    if (wrapper.dialog.isAdded()) {
                        wrapper.dialog.sendMotionEvent(event);
                    }
                }
            }
        }

        private static void sendMotionEvent(SheetType type, MotionEvent event) {
            Wrapper wrapper = instances[type.ordinal()];

            if (wrapper != null) {
                if (wrapper.dialog.isAdded()) {
                    wrapper.dialog.sendMotionEvent(event);
                } else if (wrapper.showRequest != null) {
                    wrapper.showRequest.show();
                }
            }
        }

        private static void hideAll() {
            for (Wrapper instance : instances) {
                if (instance != null) {
                    SheetBehavior<?> behavior = instance.dialog.getBehavior();

                    if (behavior != null) {
                        instance.dialog.dialog.hide();
                    }
                }
            }
        }

        public static void requestIgnoreCurrentTouchEvent(boolean enabled) {
            for (Wrapper instance : instances) {
                if (instance != null) {
                    instance.dialog.requestIgnoreCurrentTouchEvent(enabled);
                }
            }
        }

        public static int getActiveSheetCount() {
            int count = 0;

            for (Wrapper instance : instances) {
                if (instance != null && instance.dialog.dialog != null &&
                        instance.dialog.dialog.isShowing()) {
                    count++;
                }
            }

            return count;
        }

        private interface OnShowRequest {
            void show();
        }
    }

}