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

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.view.WindowCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.stario.launcher.R;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.measurements.Measurements;
import com.stario.launcher.utils.UiUtils;
import com.stario.launcher.utils.Utils;

public abstract class ActionDialog extends BottomSheetDialog {
    protected final ThemedActivity activity;
    private View root;

    public ActionDialog(@NonNull ThemedActivity activity) {
        super(activity);

        this.activity = activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BottomSheetBehavior<?> behavior = getBehavior();

        behavior.setSkipCollapsed(true);

        LayoutInflater inflater = activity.getLayoutInflater();

        root = inflater.inflate(R.layout.pop_up_root, null, false);
        ((ViewGroup) root.findViewById(R.id.content)).addView(inflateContent(activity.getLayoutInflater()));
        UiUtils.applyNotchMargin(root);

        root.setOnClickListener(view -> dismiss());

        setContentView(root);

        ViewGroup.MarginLayoutParams params = ((ViewGroup.MarginLayoutParams) root.getLayoutParams());
        params.leftMargin = Measurements.dpToPx(10);
        params.rightMargin = Measurements.dpToPx(10);
        ((View) root.getParent()).setBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    public void onAttachedToWindow() {
        Window window = getWindow();

        root.post(() -> {
            ViewParent frame = root.getParent();

            if (frame != null) {
                fitToBottomInset((View) frame, false);

                ViewParent coordinator = frame.getParent();

                if (coordinator != null) {
                    fitToBottomInset((View) coordinator, false);

                    ViewParent container = coordinator.getParent();

                    if (container != null) {
                        fitToBottomInset((View) container, false);
                    }
                }
            }
        });

        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false);

            if (blurBehind()) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                window.setDimAmount(0.5f);

                if (Utils.isMinimumSDK(Build.VERSION_CODES.S)) {
                    WindowManager.LayoutParams attributes = window.getAttributes();

                    attributes.setBlurBehindRadius(
                            (int) (FullscreenDialog.STEP_COUNT / 2 *
                                    FullscreenDialog.BLUR_STEP));

                    window.setAttributes(attributes);
                }
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            }
        }
    }

    // hack to remove STATE_EXPANDED fit to system window jitter on layout pass
    @Override
    public void onDetachedFromWindow() {
        ViewParent frame = root.getParent();

        if (frame != null) {
            fitToBottomInset((View) frame, true);

            ViewParent coordinator = frame.getParent();

            if (coordinator != null) {
                fitToBottomInset((View) coordinator, true);

                ViewParent container = coordinator.getParent();

                if (container != null) {
                    fitToBottomInset((View) container, true);
                }
            }
        }

        super.onDetachedFromWindow();
    }

    private void fitToBottomInset(View view, boolean fit) {
        view.setFitsSystemWindows(fit);
        view.setPadding(view.getPaddingLeft(),
                view.getPaddingTop(), view.getPaddingRight(), 0);
    }

    @Override
    public void hide() {
        super.dismiss();
    }

    @Override
    public void show() {
        super.show();

        Vibrations.getInstance().vibrate();

        getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!hasFocus) {
            dismiss();
        }
    }

    @Override
    protected void onStop() {
        dismiss();

        super.onStop();
    }

    protected abstract @NonNull View inflateContent(LayoutInflater inflater);

    protected abstract boolean blurBehind();
}
