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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.measurements.Measurements;
import com.stario.launcher.utils.UiUtils;
import com.stario.launcher.utils.Utils;

public abstract class ActionDialog extends BottomSheetDialog {
    protected final ThemedActivity activity;

    public ActionDialog(@NonNull ThemedActivity activity) {
        super(activity);

        this.activity = activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BottomSheetBehavior<?> behavior = getBehavior();

        behavior.setSkipCollapsed(true);

        View contentView = inflateContent(activity.getLayoutInflater());
        UiUtils.applyNotchMargin(contentView);

        setContentView(contentView);

        ViewGroup.MarginLayoutParams params = ((ViewGroup.MarginLayoutParams) contentView.getLayoutParams());
        params.leftMargin = Measurements.dpToPx(10);
        params.rightMargin = Measurements.dpToPx(10);
        ((View) contentView.getParent()).setBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    public void show() {
        super.show();

        Vibrations.getInstance().vibrate();

        getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        Window window = getWindow();

        if (window != null) {
            if (blurBehind()) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND |
                        WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                window.setDimAmount(0.5f);

                if (Utils.isMinimumSDK(31)) {
                    WindowManager.LayoutParams attributes = window.getAttributes();

                    attributes.setBlurBehindRadius(
                            (int) (FullscreenDialog.STEP_COUNT / 2 *
                                    FullscreenDialog.BLUR_STEP));

                    window.setAttributes(attributes);
                }
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND |
                        WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            }
        }

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (!hasFocus) {
            dismiss();
        }

        super.onWindowFocusChanged(hasFocus);
    }

    protected abstract @NonNull View inflateContent(LayoutInflater inflater);

    protected abstract boolean blurBehind();
}
