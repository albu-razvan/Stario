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

package com.stario.launcher.ui.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialog;

import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.utils.UiUtils;
import com.stario.launcher.utils.Utils;

public class FullscreenDialog extends AppCompatDialog {
    public final static int STEP_COUNT = 50;
    public final static float BLUR_STEP = 1.3f;
    private boolean dispatchTouchEvents;
    private final boolean blur;
    protected OnBackPressed listener;

    public FullscreenDialog(ThemedActivity activity, int theme, boolean blur) {
        super(activity, getThemeResId(activity, theme));

        this.blur = blur;

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        supportRequestWindowFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();

        if (window != null) {
            window.setWindowAnimations(0);

            UiUtils.setWindowTransitions(window);

            if (blur && Utils.isMinimumSDK(31)) {
                window.setDimAmount(0.001f); // some devices do not blur if the dim value is equal to 0

                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            }

            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);

            UiUtils.makeSysUITransparent(window);
        }
    }

    private static int getThemeResId(@NonNull Context context, int themeId) {
        // reuse the bottomSheetDialog theme
        if (themeId == 0) {
            TypedValue outValue = new TypedValue();

            if (context.getTheme()
                    .resolveAttribute(com.google.android.material.R.attr.bottomSheetDialogTheme, outValue, true)) {
                themeId = outValue.resourceId;
            } else {
                themeId = com.google.android.material.R.style.Theme_Design_Light_BottomSheetDialog;
            }
        }

        return themeId;
    }

    @Override
    public void onBackPressed() {
        if (listener == null || listener.onPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            dispatchTouchEvents = true;
        }

        boolean result = dispatchTouchEvents && super.dispatchTouchEvent(event);

        if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_CANCEL) {
            dispatchTouchEvents = true;
        }

        return result;
    }

    @Override
    public void show() {
        // Override default show behaviour
    }

    public void showDialog() {
        super.show();
    }

    public void setOnBackPressed(FullscreenDialog.OnBackPressed listener) {
        this.listener = listener;
    }

    public void requestIgnoreCurrentTouchEvent(boolean enabled) {
        dispatchTouchEvents = enabled;
    }

    public interface OnBackPressed {
        boolean onPressed();
    }
}
