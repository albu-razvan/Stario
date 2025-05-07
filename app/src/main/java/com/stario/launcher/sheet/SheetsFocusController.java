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
    private SheetWrapper[] wrappers;
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
        wrappers = new SheetWrapper[SheetType.values().length];
        targetPointers = new ArrayList<>();
        sheetType = SheetType.UNDEFINED;
        moveSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        shouldDispatch = false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            shouldDispatch = getActiveSheetCount() == 0;

            targetPointers.add(0,
                    ev.getAction() >> MotionEvent.ACTION_POINTER_INDEX_SHIFT);

            X = ev.getX(getPointer(ev));
            Y = ev.getY(getPointer(ev));

            deltaX = 0;
            deltaY = 0;

            if (shouldDispatch) {
                sendMotionEvent(MotionEvent.obtain(ev));
            }

            super.onInterceptTouchEvent(ev);
            return false;
        } else {
            if (action == MotionEvent.ACTION_UP ||
                    action == MotionEvent.ACTION_CANCEL) {
                sheetType = SheetType.UNDEFINED;
                targetPointers.clear();

                sendMotionEvent(MotionEvent.obtain(ev));

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
            sendMotionEvent(MotionEvent.obtain(ev));

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
                    sendMotionEvent(MotionEvent.obtain(ev));
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
                    sendMotionEvent(sheetType, MotionEvent.obtain(ev));
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

    public void updateSheetSystemUI(boolean value) {
        for (SheetWrapper wrapper : wrappers) {
            if (wrapper != null && wrapper.dialogFragment != null) {
                wrapper.dialogFragment.updateSheetSystemUI(value);
            }
        }
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

                    for (SheetWrapper instance : wrappers) {
                        if (instance != null) {
                            SheetBehavior<?> behavior = instance.dialogFragment.getBehavior();

                            if (behavior != null) {
                                instance.dialogFragment.dialog.hide();
                            }
                        }
                    }
                }
            }
        }
    }

    public void wrapInDialog(Launcher launcher, SheetType type,
                             @NonNull SheetDialogFragment.OnSlideListener slideListener) {
        wrappers[type.ordinal()] = new SheetWrapper(launcher, type, slideListener);
    }

    private void sendMotionEvent(MotionEvent event) {
        Log.e("TAG", "sendMotionEvent: " + event);

        for (SheetWrapper wrapper : wrappers) {
            if (wrapper != null &&
                    wrapper.dialogFragment != null) {

                if (wrapper.dialogFragment.isAdded()) {
                    wrapper.dialogFragment.sendMotionEvent(event);
                }
            }
        }
    }

    private void sendMotionEvent(SheetType type, MotionEvent event) {
        for (int index = 0; index < wrappers.length; index++) {
            if (index == type.ordinal()) {
                continue;
            }

            if (wrappers[index] != null) {
                SheetBehavior<?> behavior = wrappers[index].dialogFragment.getBehavior();

                if (behavior != null &&
                        (behavior.getState() == SheetBehavior.STATE_SETTLING ||
                                behavior.getState() == SheetBehavior.STATE_EXPANDED)) {
                    return;
                }
            }
        }

        SheetWrapper wrapper = wrappers[type.ordinal()];

        if (wrapper != null) {
            if (wrapper.dialogFragment.isAdded()) {
                wrapper.dialogFragment.sendMotionEvent(event);
            } else if (wrapper.showRequest != null) {
                wrapper.showRequest.show();
            }
        }
    }

    public int getActiveSheetCount() {
        int count = 0;

        for (SheetWrapper instance : wrappers) {
            if (instance != null && instance.dialogFragment.dialog != null &&
                    instance.dialogFragment.dialog.isShowing()) {
                count++;
            }
        }

        return count;
    }

    public static class SheetWrapper {
        private static final String TAG = "SheetWrapper";

        private final SheetDialogFragment dialogFragment;

        private SheetWrapper.OnShowRequest showRequest;

        private SheetWrapper(Launcher launcher, SheetType type,
                             @NonNull SheetDialogFragment.OnSlideListener listener) {
            this.dialogFragment = SheetDialogFactory.forType(type,
                    launcher.getSharedPreferences(Entry.STARIO));

            dialogFragment.setCancelable(false);
            dialogFragment.setOnSlideListener(listener);

            showRequest = () -> {
                FragmentManager manager = launcher.getSupportFragmentManager();

                if (!manager.isDestroyed() && manager.findFragmentByTag(type.toString()) == null) {
                    dialogFragment.show(manager, type.toString());
                }

                showRequest = null;
            };
        }

        private interface OnShowRequest {
            void show();
        }
    }
}