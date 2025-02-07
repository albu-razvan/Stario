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

package com.stario.launcher.ui.keyboard.extract;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.keyboard.InlineAutocompleteEditText;
import com.stario.launcher.utils.UiUtils;

public class ExtractEditText extends InlineAutocompleteEditText {
    private ExtractDialog extractDialog;
    private FragmentManager manager;

    public ExtractEditText(Context context) {
        super(context);

        init(context);
    }

    public ExtractEditText(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public ExtractEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    private void init(Context context) {
        if (!(context instanceof ThemedActivity)) {
            throw new RuntimeException("ExtractEditText can only be used from a FragmentActivity context.");
        }

        this.manager = ((FragmentActivity) context).getSupportFragmentManager();

        setShowSoftInputOnFocus(false);

        setOnClickListener(view -> {
            if (Measurements.isLandscape()) {
                setCursorVisible(false);

                openExtractDialog();
            } else {
                setCursorVisible(true);

                UiUtils.showKeyboard(this);
            }
        });

        extractDialog = new ExtractDialog(this);
    }

    @Override
    public void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (focused) {
            if (Measurements.isLandscape()) {
                setCursorVisible(false);

                openExtractDialog();
            } else {
                setCursorVisible(true);

                post(() -> UiUtils.showKeyboard(this));
            }
        }

        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    @Override
    public boolean extractText(ExtractedTextRequest request, ExtractedText outText) {
        if (Measurements.isLandscape()) {
            UiUtils.hideKeyboard(this);
        }

        return false;
    }

    private void openExtractDialog() {
        if (!extractDialog.isAdded()) {
            extractDialog.show(manager, null);
        } else {
            Dialog dialog = extractDialog.getDialog();

            if (dialog != null) {
                dialog.show();
            }
        }
    }
}
