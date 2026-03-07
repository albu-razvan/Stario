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
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.WindowInsets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.activities.launcher.Launcher;
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
    private boolean isControllerEnabled;
    private Rect systemGestureInsets;
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
        this.systemGestureInsets = new Rect();
        this.isControllerEnabled = true;
        this.sheetType = null;
    }

    public void setControllerEnabled(boolean enabled) {
        this.isControllerEnabled = enabled;
    }

    private boolean isTouchInSystemInsets(float x, float y) {
        MarginLayoutParams params = (MarginLayoutParams) getLayoutParams();

        return x < systemGestureInsets.left + params.leftMargin ||
                x - params.leftMargin > (getWidth() - systemGestureInsets.right) ||
                y < systemGestureInsets.top + params.topMargin ||
                y - params.topMargin > (getHeight() - systemGestureInsets.bottom);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isControllerEnabled) {
            return super.onInterceptTouchEvent(ev);
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (isTouchInSystemInsets(ev.getRawX(), ev.getRawY())) {
                    return false;
                }

                targetPointers.clear();
                targetPointers.add(0);

                X = ev.getX(getPointer(ev));
                Y = ev.getY(getPointer(ev));

                deltaX = deltaY = 0;
                dispatchedMoveEvent = false;

                postCheckForLongClick();

                return false;

            case MotionEvent.ACTION_MOVE:
                if (targetPointers.isEmpty()) {
                    return false;
                }

                deltaX = X - ev.getX(getPointer(ev));
                deltaY = Y - ev.getY(getPointer(ev));

                boolean movedEnough = Math.abs(deltaX) >= moveSlop || Math.abs(deltaY) >= moveSlop;
                if (movedEnough) {
                    removeCheck();
                }

                return movedEnough;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                removeCheck();

                sheetType = null;
                targetPointers.clear();

                return false;

            default:
                return false;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isControllerEnabled) {
            return super.onTouchEvent(ev);
        }

        int action = ev.getAction();

        if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_CANCEL) {
            dispatchSheetMotionEvent(MotionEvent.obtain(ev));

            removeCheck();

            sheetType = null;
            targetPointers.clear();
        } else {
            if (action == MotionEvent.ACTION_DOWN) {
                if (isTouchInSystemInsets(ev.getRawX(), ev.getRawY())) {
                    return false;
                }

                targetPointers.add(0, 0);

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

    @Override
    public WindowInsets dispatchApplyWindowInsets(WindowInsets insets) {
        WindowInsetsCompat compat = WindowInsetsCompat.toWindowInsetsCompat(insets);
        Insets gestureInsets = compat.getInsets(WindowInsetsCompat.Type.systemGestures());

        systemGestureInsets.set(
                gestureInsets.left,
                gestureInsets.top,
                gestureInsets.right,
                gestureInsets.bottom
        );

        return super.dispatchApplyWindowInsets(insets);
    }

    private int getPointer(MotionEvent event) {
        if (targetPointers.isEmpty()) {
            return 0;
        }

        int pointerId = targetPointers.get(0);
        int pointerIndex = event.findPointerIndex(pointerId);

        return Math.max(0, Math.min(event.getPointerCount() - 1, pointerIndex));
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

    /**
     * Use this if your view is or can be a direct or indirect child of SheetFocusController
     *
     * @param clickListener click listener to invoke on a valid gesture
     * @return touch listener
     */
    public static View.OnTouchListener createClickTouchListener(
            @Nullable View.OnClickListener clickListener
    ) {
        return createClickTouchListener(clickListener, null, null);
    }

    /**
     * Use this if your view is or can be a direct or indirect child of SheetFocusController
     *
     * @param clickListener          click listener to invoke on a valid gesture
     * @param longClickListener      long click listener to invoke on a valid gesture
     * @param longClickEventListener event listener for long click. <code>longClickListener</code> has to be provided.
     * @return touch listener
     */
    public static View.OnTouchListener createClickTouchListener(
            @Nullable View.OnClickListener clickListener,
            @Nullable View.OnLongClickListener longClickListener,
            @Nullable OnLongClickEventListener longClickEventListener
    ) {
        return createClickTouchListener(clickListener, longClickListener,
                longClickEventListener, null, null, null);
    }

    /**
     * Use this if your view is or can be a direct or indirect child of SheetFocusController
     *
     * @param clickListener          click listener to invoke on a valid gesture
     * @param longClickListener      long click listener to invoke on a valid gesture
     * @param longClickEventListener event listener for long click. <code>longClickListener</code> has to be provided.
     * @param viewHolder             target view holder
     * @param itemTouchHelper        drag RecyclerView item touch helper
     * @param dragStartListener      event listener for when a drag starts
     * @return touch listener
     */
    public static View.OnTouchListener createClickTouchListener(
            @Nullable View.OnClickListener clickListener,
            @Nullable View.OnLongClickListener longClickListener,
            @Nullable OnLongClickEventListener longClickEventListener,
            @Nullable RecyclerView.ViewHolder viewHolder,
            @Nullable ItemTouchHelper itemTouchHelper,
            @Nullable OnDragStartListener dragStartListener
    ) {
        return new View.OnTouchListener() {
            private boolean longPressPerformed = false;
            private Runnable longPressRunnable = null;
            private boolean isClickCandidate = false;
            private boolean isFinishedCalled = false;
            private boolean dragStarted = false;
            private boolean dragReady = false;
            private Integer touchSlop = null;
            private float startX = 0;
            private float startY = 0;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (touchSlop == null) {
                    touchSlop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
                }

                SheetsFocusController parentController = findParentController(view);

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();

                        longPressPerformed = false;
                        isClickCandidate = true;
                        isFinishedCalled = false;
                        dragStarted = false;
                        dragReady = false;

                        if (longClickListener != null && parentController != null) {
                            parentController.cancelLongPress();
                        }

                        longPressRunnable = () -> {
                            dragReady = true;

                            if (longClickListener != null) {
                                longPressPerformed = longClickListener.onLongClick(view);

                                triggerFinished();
                            }
                        };

                        int duration = ViewConfiguration.getLongPressTimeout();
                        view.postDelayed(longPressRunnable, duration);
                        if (longClickListener != null && longClickEventListener != null) {
                            longClickEventListener.onDown(duration);
                        }

                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = Math.abs(event.getX() - startX);
                        float dy = Math.abs(event.getY() - startY);

                        if (dx > touchSlop || dy > touchSlop) {
                            isClickCandidate = false;

                            if (dragReady && !dragStarted && itemTouchHelper != null && viewHolder != null) {
                                triggerFinished();
                                dragStarted = true;

                                if (view.getParent() != null) {
                                    view.getParent().requestDisallowInterceptTouchEvent(false);
                                }

                                itemTouchHelper.startDrag(viewHolder);

                                if (dragStartListener != null) {
                                    dragStartListener.onDragStart();
                                }

                                return true;
                            }

                            if (!dragReady && longPressRunnable != null) {
                                view.removeCallbacks(longPressRunnable);
                            }
                        }
                        return dragStarted;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (longPressRunnable != null) {
                            view.removeCallbacks(longPressRunnable);
                        }

                        boolean parentLongPressed = parentController != null
                                && parentController.hasPerformedLongPress;

                        if (event.getActionMasked() == MotionEvent.ACTION_UP && isClickCandidate
                                && !longPressPerformed && !parentLongPressed && clickListener != null) {
                            clickListener.onClick(view);
                        }

                        triggerFinished();

                        isClickCandidate = false;
                        longPressPerformed = false;
                        dragReady = false;
                        dragStarted = false;

                        return true;

                    default:
                        return false;
                }
            }

            private SheetsFocusController findParentController(View view) {
                ViewParent parent = view.getParent();

                while (parent instanceof View) {
                    if (parent instanceof SheetsFocusController) {
                        return (SheetsFocusController) parent;
                    }

                    parent = parent.getParent();
                }

                return null;
            }

            private void triggerFinished() {
                if (!isFinishedCalled && longClickListener != null
                        && longClickEventListener != null) {
                    longClickEventListener.onFinished();
                    isFinishedCalled = true;
                }
            }
        };
    }

    public interface OnDragStartListener {
        void onDragStart();
    }

    public interface OnLongClickEventListener {
        void onDown(long duration);

        void onFinished();
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