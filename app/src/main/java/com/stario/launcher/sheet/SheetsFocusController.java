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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;

import com.stario.launcher.activities.Launcher;
import com.stario.launcher.sheet.behavior.SheetBehavior;
import com.stario.launcher.ui.utils.UiUtils;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public class SheetsFocusController extends ConstraintLayout {
    private CheckForLongPress pendingCheckForLongPress;
    private View.OnLongClickListener longClickListener;
    private SheetDialog.OnSlideListener slideListener;
    private boolean hasPerformedLongPress;
    private List<Integer> targetPointers;
    private boolean dispatchedMoveEvent;
    private SheetWrapper[] wrappers;
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
        this.slideListener = null;
        this.moveSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.wrappers = new SheetWrapper[SheetType.values().length];
        this.targetPointers = new ArrayList<>();
        this.dispatchedMoveEvent = false;
        this.sheetType = null;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            targetPointers.add(0,
                    ev.getAction() >> MotionEvent.ACTION_POINTER_INDEX_SHIFT);

            X = ev.getX(getPointer(ev));
            Y = ev.getY(getPointer(ev));

            deltaX = 0;
            deltaY = 0;

            dispatchedMoveEvent = false;
            dispatchSheetMotionEvent(MotionEvent.obtain(ev));

            super.onInterceptTouchEvent(ev);
            return false;
        } else {
            if (action == MotionEvent.ACTION_UP ||
                    action == MotionEvent.ACTION_CANCEL) {
                dispatchSheetMotionEvent(MotionEvent.obtain(ev));

                removeCheck();

                sheetType = null;
                targetPointers.clear();

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
            dispatchSheetMotionEvent(MotionEvent.obtain(ev));

            removeCheck();

            sheetType = null;
            targetPointers.clear();
        } else {
            if (action == MotionEvent.ACTION_DOWN) {
                targetPointers.add(0,
                        ev.getAction() >> MotionEvent.ACTION_POINTER_INDEX_SHIFT);

                X = ev.getX(getPointer(ev));
                Y = ev.getY(getPointer(ev));

                deltaX = 0;
                deltaY = 0;

                dispatchedMoveEvent = false;
                dispatchSheetMotionEvent(MotionEvent.obtain(ev));
                postCheckForLongClick();
            } else {
                if (hasPerformedLongPress) {
                    hideAllSheets();
                    removeCheck();

                    return super.onTouchEvent(ev);
                }

                deltaY = Y - ev.getY(getPointer(ev));
                deltaX = X - ev.getX(getPointer(ev));

                if (ev.getAction() == MotionEvent.ACTION_MOVE &&
                        (Math.abs(deltaX) >= moveSlop ||
                                Math.abs(deltaY) >= moveSlop)) {
                    if (sheetType == null) {
                        if (Math.abs(deltaY) > Math.abs(deltaX)) {
                            sheetType = Math.signum(deltaY) >= 0 ? SheetType.BOTTOM_SHEET : SheetType.TOP_SHEET;
                        } else {
                            sheetType = Math.signum(deltaX) >= 0 ? SheetType.RIGHT_SHEET : SheetType.LEFT_SHEET;
                        }

                        hideAllSheets(sheetType);
                    }

                    removeCheck();
                }

                dispatchSheetMotionEvent(sheetType, MotionEvent.obtain(ev));
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

    public boolean hasSheetFocus() {
        return sheetType != null && dispatchedMoveEvent;
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
                                SheetDialog dialog = instance.dialogFragment.getSheetDialog();

                                if (dialog != null) {
                                    dialog.hide();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void setSlideListener(SheetDialog.OnSlideListener slideListener) {
        this.slideListener = slideListener;
    }

    @SafeVarargs
    public final void removeSheetDialog(
            @NonNull Class<? extends SheetDialogFragment>... dialogFragmentClass) {
        removeSheetDialog(List.of(dialogFragmentClass));
    }

    public final void removeSheetDialog(
            @NonNull List<Class<? extends SheetDialogFragment>> dialogFragmentClass) {
        for (Class<? extends SheetDialogFragment> clazz : dialogFragmentClass) {
            for (int index = 0; index < wrappers.length; index++) {
                SheetWrapper wrapper = wrappers[index];

                if (wrapper != null && wrapper.dialogFragment != null &&
                        wrapper.dialogFragment.getClass().equals(clazz)) {
                    if (wrapper.dialogFragment.isAdded()) {
                        wrapper.dialogFragment.dismissAllowingStateLoss();
                    }

                    wrappers[index] = null;

                    break;
                }
            }
        }
    }

    public final void moveSheetDialog(
            @NonNull Launcher launcher,
            @NonNull List<Class<? extends SheetDialogFragment>> dialogFragmentClass) {
        removeSheetDialog(dialogFragmentClass);
        addSheetDialog(launcher, dialogFragmentClass);
    }

    @SafeVarargs
    public final void moveSheetDialog(
            @NonNull Launcher launcher,
            @NonNull Class<? extends SheetDialogFragment>... dialogFragmentClass) {
        moveSheetDialog(launcher, List.of(dialogFragmentClass));
    }

    public final void addSheetDialog(
            @NonNull Launcher launcher,
            @NonNull List<Class<? extends SheetDialogFragment>> dialogFragmentClass) {
        for (Class<? extends SheetDialogFragment> clazz : dialogFragmentClass) {
            SheetType type = SheetType.getSheetTypeForSheetDialogFragment(launcher, clazz);

            if (type == null || type == SheetType.UNDEFINED || wrappers[type.ordinal()] != null) {
                return;
            }

            try {
                Constructor<? extends SheetDialogFragment> constructor =
                        clazz.getConstructor(SheetType.class);

                wrappers[type.ordinal()] =
                        new SheetWrapper(launcher, type, constructor.newInstance(type));
            } catch (Exception exception) {
                throw new RuntimeException(clazz.getName() +
                        "(" + SheetType.class.getName() + ")" +
                        "has to be visible to public scope.");
            }
        }
    }

    @SafeVarargs
    public final void addSheetDialog(
            @NonNull Launcher launcher,
            @NonNull Class<? extends SheetDialogFragment>... dialogFragmentClass) {
        addSheetDialog(launcher, List.of(dialogFragmentClass));
    }

    private void dispatchSheetMotionEvent(MotionEvent event) {
        for (SheetWrapper wrapper : wrappers) {
            if (wrapper != null &&
                    wrapper.dialogFragment != null) {

                if (wrapper.dialogFragment.isAdded() &&
                        wrapper.dialogFragment.onMotionEvent(event) &&
                        event.getAction() == MotionEvent.ACTION_MOVE) {
                    dispatchedMoveEvent = true;
                }
            }
        }
    }

    private void dispatchSheetMotionEvent(SheetType type, MotionEvent event) {
        if (type == null) {
            return;
        }

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
                if (wrapper.dialogFragment.onMotionEvent(event) &&
                        event.getAction() == MotionEvent.ACTION_MOVE) {
                    dispatchedMoveEvent = true;
                }
            } else {
                wrapper.show();
            }
        } else if (type == SheetType.TOP_SHEET) {
            UiUtils.expandStatusBar(getContext());
        }
    }

    public void hideAllSheets(@NonNull SheetType... keepVisible) {
        for (int index = 0; index < wrappers.length; index++) {
            boolean hide = true;

            for (SheetType type : keepVisible) {
                if (type.ordinal() == index) {
                    hide = false;

                    break;
                }
            }

            if (hide) {
                SheetWrapper instance = wrappers[index];

                if (instance != null && instance.dialogFragment.getSheetDialog() != null) {
                    instance.dialogFragment.hide(false);
                }
            }
        }
    }

    public class SheetWrapper {
        private static final String TAG = "SheetWrapper";

        private final SheetDialogFragment dialogFragment;
        private Runnable showRunnable;

        private SheetWrapper(Launcher launcher, SheetType type,
                             @NonNull SheetDialogFragment fragment) {
            dialogFragment = fragment;

            dialogFragment.setCancelable(false);
            dialogFragment.setOnSlideListener(slideOffset -> {
                if (slideListener != null) {
                    slideListener.onSlide(slideOffset);
                }
            });

            showRunnable = () -> {
                FragmentManager manager = launcher.getSupportFragmentManager();

                if (!manager.isDestroyed() && manager.findFragmentByTag(type.toString()) == null) {
                    dialogFragment.show(manager, type.toString());
                }

                showRunnable = null;
            };
        }

        public void show() {
            if (showRunnable != null) {
                showRunnable.run();
            }
        }
    }
}