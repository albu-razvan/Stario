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

package com.stario.launcher.ui.keyboard;

import android.app.Activity;
import android.graphics.Insets;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.stario.launcher.ui.Measurements;
import com.stario.launcher.utils.Utils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class KeyboardHeightProvider extends PopupWindow {
    private final ViewTreeObserver.OnGlobalLayoutListener listener;
    private final List<KeyboardHeightListener> observers;
    private final Activity activity;
    private final View parentView;
    private final View popupView;

    private int oldHeight;

    public KeyboardHeightProvider(Activity activity) {
        super(activity);
        this.activity = activity;
        this.oldHeight = Integer.MIN_VALUE;

        this.observers = new CopyOnWriteArrayList<>();
        this.popupView = new LinearLayout(activity);
        popupView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        popupView.setBackground(new ColorDrawable(0));

        this.listener = () -> notifyKeyboardHeightChanged(getKeyboardHeight());

        setContentView(popupView);

        setSoftInputMode(LayoutParams.SOFT_INPUT_ADJUST_RESIZE | LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);

        parentView = activity.findViewById(android.R.id.content);

        setWidth(0);
        setHeight(LayoutParams.MATCH_PARENT);
    }

    public void start() {
        if (!isShowing() && parentView.getWindowToken() != null) {
            setBackgroundDrawable(new ColorDrawable(0));
            showAtLocation(parentView, Gravity.NO_GRAVITY, 0, 0);

            parentView.getViewTreeObserver()
                    .addOnGlobalLayoutListener(listener);

            notifyKeyboardHeightChanged(getKeyboardHeight());
        }
    }

    public void close() {
        popupView.getViewTreeObserver()
                .removeOnGlobalLayoutListener(listener);

        dismiss();
    }

    public void addKeyboardHeightListener(KeyboardHeightListener observer) {
        if (observer != null) {
            observers.add(observer);
        }
    }

    public void removeKeyboardHeightListener(KeyboardHeightListener observer) {
        if (observer != null) {
            observers.remove(observer);
        }
    }

    private void notifyKeyboardHeightChanged(int height) {
        if(oldHeight == height) {
            return;
        }

        oldHeight = height;

        for (KeyboardHeightListener observer : observers) {
            observer.onKeyboardHeightChanged(height);
        }
    }

    public int getKeyboardHeight() {
        if (!isShowing()) {
            return 0;
        }

        if (Utils.isMinimumSDK(Build.VERSION_CODES.R)) {
            WindowInsets windowInsets = parentView.getRootWindowInsets();

            if(windowInsets != null) {
                Insets insets = windowInsets.getInsets(WindowInsets.Type.ime());

                return Math.max(0, insets.bottom - Measurements.getNavHeight());
            }

            return 0;
        } else {
            Point screenSize = new Point();
            Display display = activity.getWindowManager().getDefaultDisplay();
            display.getSize(screenSize);

            if (Utils.isMinimumSDK(Build.VERSION_CODES.Q)) {
                DisplayCutout displayCutout = display.getCutout();

                if (displayCutout != null) {
                    screenSize.y += displayCutout.getSafeInsetTop();
                }
            }

            Rect rect = new Rect();
            popupView.getWindowVisibleDisplayFrame(rect);

            return screenSize.y - rect.bottom;
        }
    }

    public interface KeyboardHeightListener {
        void onKeyboardHeightChanged(int height);
    }
}