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

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.stario.launcher.sheet.behavior.SheetBehavior;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.dialogs.PersistentFullscreenDialog;

public abstract class SheetDialog extends PersistentFullscreenDialog {
    private boolean dispatchedDownEvent;
    protected ConstraintLayout sheet;
    protected SheetBehavior<ConstraintLayout> behavior;

    public SheetDialog(ThemedActivity activity, int theme) {
        super(activity, theme, true);

        this.dispatchedDownEvent = false;
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

        behavior.addSheetCallback(new SheetBehavior.SheetCallback() {
            @Override
            public void onStateChanged(@NonNull View sheet, int newState) {
                if (newState == SheetBehavior.STATE_COLLAPSED) {
                    hide();

                    container.intercept(SheetCoordinator.ALL);
                } else {
                    showDialog();

                    if (newState == SheetBehavior.STATE_EXPANDED) {
                        container.intercept(SheetCoordinator.OWN);
                    }
                }
            }
        });

        return container;
    }

    void captureMotionEvent(MotionEvent event) {
        CoordinatorLayout coordinator = getContainer();

        if (coordinator != null &&
                behavior != null) {
            MotionEvent motionEvent = MotionEvent.obtain(event);

            if (event.getPointerCount() > 1) {
                motionEvent.setAction(MotionEvent.ACTION_UP);
            } else if (!dispatchedDownEvent &&
                    motionEvent.getAction() != MotionEvent.ACTION_UP &&
                    motionEvent.getAction() != MotionEvent.ACTION_CANCEL) {
                if (!isShowing()) {
                    showDialog();

                    return;
                }

                motionEvent.setAction(MotionEvent.ACTION_DOWN);

                dispatchedDownEvent = behavior.isDragHelperInstantiated();
            }

            coordinator.dispatchTouchEvent(motionEvent);

            if (motionEvent.getAction() == MotionEvent.ACTION_UP ||
                    motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
                dispatchedDownEvent = false;
            }
        }
    }

    public SheetBehavior<ConstraintLayout> getBehavior() {
        return behavior;
    }

    protected abstract SheetCoordinator getContainer();

    public abstract @NonNull SheetType getType();
}
