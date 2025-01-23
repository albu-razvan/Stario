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

package com.stario.launcher.ui.dialogs;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialog;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.utils.UiUtils;
import com.stario.launcher.utils.Utils;

public class PersistentFullscreenDialog extends AppCompatDialog {
    public final static float BLUR_STEP = 1.3f;
    public final static int STEP_COUNT = 50;

    private boolean dispatchTouchEvents;

    protected OnBackPressed listener;
    private final boolean blur;

    public PersistentFullscreenDialog(ThemedActivity activity, int theme, boolean blur) {
        super(activity, getThemeResId(activity, theme));

        this.blur = blur;

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        supportRequestWindowFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);

        Lifecycle lifecycle = activity.getLifecycle();
        lifecycle.addObserver(new DefaultLifecycleObserver() {
            boolean hasCalledOnPause = false;
            boolean hasCalledOnStart = false;

            @Override
            public void onStart(@NonNull LifecycleOwner owner) {
                hasCalledOnStart = true;
            }

            @Override
            public void onResume(@NonNull LifecycleOwner owner) {
                // If the dialog is hidden, parent activity is paused, but not stopped,
                // the decorView will be visible despite it having View.GONE on activity
                // resume. It is not just visual, it behaves as if the dialog is shown.
                // A workaround is to toggle between visibility states onResume, but that results
                // in a small flicker. This half-baked solution sends the dialog window way off-screen
                // and disables window touches when hidden, so that no one can see it when it happens.

                if (hasCalledOnPause && !hasCalledOnStart) {
                    Window window = getWindow();

                    if (window != null) {
                        View decor = window.getDecorView();

                        int visibility = decor.getVisibility();
                        decor.setVisibility(View.INVISIBLE);
                        decor.post(() -> decor.setVisibility(visibility));
                    }
                }

                hasCalledOnPause = false;
                hasCalledOnStart = false;
            }

            @Override
            public void onPause(@NonNull LifecycleOwner owner) {
                hasCalledOnPause = true;
            }

            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                lifecycle.removeObserver(this);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();

        if (window != null) {
            window.setWindowAnimations(0);

            if (blur && Utils.isMinimumSDK(Build.VERSION_CODES.S)) {
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

    public void setOnBackPressed(PersistentFullscreenDialog.OnBackPressed listener) {
        this.listener = listener;
    }

    public void requestIgnoreCurrentTouchEvent(boolean enabled) {
        dispatchTouchEvents = enabled;
    }

    public interface OnBackPressed {
        boolean onPressed();
    }
}
