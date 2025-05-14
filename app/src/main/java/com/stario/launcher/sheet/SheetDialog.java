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
import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.stario.launcher.activities.Launcher;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.sheet.behavior.SheetBehavior;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.dialogs.PersistentFullscreenDialog;
import com.stario.launcher.ui.utils.UiUtils;
import com.stario.launcher.utils.Utils;

public abstract class SheetDialog extends PersistentFullscreenDialog {
    private static final String TAG = "SheetDialog";

    private final Drawable background;

    private boolean dispatchedMotionEventToCoordinator;
    private boolean shouldDispatchMotionEventsToParent;
    private boolean dispatchMotionEventsToParent;
    private boolean receivedMoveEvent;

    protected SheetBehavior<ConstraintLayout> behavior;
    protected ConstraintLayout sheet;

    public SheetDialog(ThemedActivity activity, int theme) {
        super(activity, theme, true);

        this.dispatchedMotionEventToCoordinator = false;
        this.shouldDispatchMotionEventsToParent = false;
        this.dispatchMotionEventsToParent = false;
        this.receivedMoveEvent = false;

        this.background = new ColorDrawable(
                activity.getAttributeData(com.google.android.material.R.attr.colorSurface, false)
        );
    }

    @Override
    public void setContentView(@NonNull View view) {
        super.setContentView(wrapInSheet(view, null));
    }

    @Override
    public void setContentView(@NonNull View view, ViewGroup.LayoutParams params) {
        super.setContentView(wrapInSheet(view, params));
    }

    @Override
    public void cancel() {
        // ignore cancel event so that the sheet will never close
    }

    @Override
    public boolean showDialog() {
        return superShow();
    }

    @SuppressLint("ClickableViewAccessibility")
    private View wrapInSheet(@Nullable View view, @Nullable ViewGroup.LayoutParams params) {
        SheetCoordinator container = getContainer();

        if (view == null) {
            view = getLayoutInflater().inflate(0, container, false);
        }

        sheet.removeAllViews();

        if (params == null) {
            sheet.addView(view);
        } else {
            sheet.addView(view, params);
        }

        sheet.setOnTouchListener((v, event) -> true);

        Window window = getWindow();
        if (window != null) {
            UiUtils.enforceLightSystemUI(window);

            background.setAlpha(0);
            window.setBackgroundDrawable(background);
        }

        behavior.addSheetCallback(new SheetBehavior.SheetCallback() {
            private int lastBlurStep;
            boolean wasCollapsed;

            {
                this.lastBlurStep = -1;
                this.wasCollapsed = true;
            }

            @Override
            public void onStateChanged(@NonNull View sheet, int state) {
                if (state == SheetBehavior.STATE_COLLAPSED) {
                    hide();

                    wasCollapsed = true;
                    container.intercept(SheetCoordinator.ALL);
                } else if (state == SheetBehavior.STATE_EXPANDED ||
                        state == SheetBehavior.STATE_SETTLING) {
                    container.intercept(SheetCoordinator.OWN);

                    if (state == SheetBehavior.STATE_EXPANDED) {
                        shouldDispatchMotionEventsToParent = false;
                    }

                    Window window = getWindow();
                    if (window != null) {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    }
                }
            }

            @Override
            public void onSettleToState(@NonNull View sheet, int stateToSettle) {
                shouldDispatchMotionEventsToParent = stateToSettle == SheetBehavior.STATE_COLLAPSED;
            }

            @Override
            public void onSlide(@NonNull View sheet, float slideOffset) {
                if (slideOffset >= 0.5f) {
                    if (wasCollapsed) {
                        Vibrations.getInstance().vibrate();
                    }

                    wasCollapsed = false;
                }

                // in case motion event capture or state change hide()
                // happens to be called accidentally after showing the
                // sheet and preparing for sliding
                if (slideOffset != 0 && !isShowing()) {
                    if (showDialog()) {
                        behavior.invalidate();
                    }
                } else {
                    Window window = getWindow();
                    if (window != null) {
                        double offsetSemi = Utils.getGenericInterpolatedValue(slideOffset);

                        background.setAlpha((int) (Launcher.MAX_BACKGROUND_ALPHA * offsetSemi));
                        window.setBackgroundDrawable(background);

                        // only STEP_COUNT states for performance
                        int step = (int) (STEP_COUNT * offsetSemi);

                        if (Utils.isMinimumSDK(Build.VERSION_CODES.S) && lastBlurStep != step) {
                            window.setBackgroundBlurRadius((int) (step * BLUR_STEP));

                            this.lastBlurStep = step;
                        }
                    }

                    float alpha = slideOffset * 2 - 1f;

                    if (alpha > 0) {
                        sheet.setAlpha(alpha);
                        sheet.setVisibility(View.VISIBLE);
                    } else {
                        if (shouldDispatchMotionEventsToParent) {
                            dispatchMotionEventsToParent = true;
                        }

                        sheet.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });

        return container;
    }

    boolean onMotionEvent(MotionEvent event) {
        CoordinatorLayout coordinator = getContainer();

        try {
            if (coordinator != null && behavior != null) {
                if (event.getAction() == MotionEvent.ACTION_UP ||
                        event.getAction() == MotionEvent.ACTION_CANCEL) {
                    if (!receivedMoveEvent) {
                        hide();
                    }

                    boolean result = coordinator.dispatchTouchEvent(event);

                    dispatchedMotionEventToCoordinator = false;
                    dispatchMotionEventsToParent = false;
                    receivedMoveEvent = false;

                    return result;
                } else if (!dispatchedMotionEventToCoordinator) {
                    if (!isShowing()) {
                        showDialog();

                        return false;
                    }

                    event.setAction(MotionEvent.ACTION_DOWN);
                }

                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    receivedMoveEvent = true;
                }

                dispatchedMotionEventToCoordinator = behavior.isDragHelperInstantiated() &&
                        coordinator.dispatchTouchEvent(event);

                return dispatchedMotionEventToCoordinator;
            }
        } catch (IllegalArgumentException exception) {
            Log.e(TAG, "onMotionEvent: " + exception.getMessage());

            dispatchedMotionEventToCoordinator = false;
            dispatchMotionEventsToParent = false;
            receivedMoveEvent = false;
        }

        return false;
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent event) {
        if (dispatchMotionEventsToParent) {
            MotionEvent newEvent = MotionEvent.obtain(event);
            newEvent.setLocation(event.getRawX(), event.getRawY());

            Activity activity = getOwnerActivity();

            if (activity != null) {
                activity.dispatchTouchEvent(newEvent);
            }

            newEvent.recycle();

            return true;
        }

        try {
            return super.dispatchTouchEvent(event);
        } catch (RuntimeException exception) {
            Log.e(TAG, "dispatchTouchEvent: " + exception.getMessage());

            return false;
        }
    }

    public SheetBehavior<ConstraintLayout> getBehavior() {
        return behavior;
    }

    protected abstract SheetCoordinator getContainer();

    public abstract @NonNull SheetType getType();
}
