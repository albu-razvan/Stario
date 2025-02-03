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

package com.stario.launcher.apps.categories.popup;

import android.text.Editable;
import android.text.TextWatcher;
import android.transition.ChangeBounds;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.stario.launcher.R;
import com.stario.launcher.apps.categories.CategoryManager;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.dialogs.ActionDialog;
import com.stario.launcher.ui.keyboard.extract.ExtractEditText;

import java.util.UUID;

public class RenameCategoryDialog extends ActionDialog {
    private final UUID categoryIdentifier;
    private final CategoryManager categoryManager;

    private ExtractEditText editText;

    public RenameCategoryDialog(@NonNull ThemedActivity activity, UUID categoryIdentifier) {
        super(activity);

        this.categoryIdentifier = categoryIdentifier;
        this.categoryManager = CategoryManager.getInstance();

        setOnDismissListener(dialog -> {
            Editable name = editText.getText();

            if (name != null) {
                categoryManager.updateCategory(categoryIdentifier, name.toString());
            }
        });
    }

    @NonNull
    @Override
    protected View inflateContent(LayoutInflater inflater) {
        View root = inflater.inflate(R.layout.pop_up_category, null);

        String initialName = categoryManager.getCategoryName(categoryIdentifier);

        View warning = root.findViewById(R.id.warning);

        editText = root.findViewById(R.id.category);
        editText.setText(initialName);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                Editable text = editText.getText();

                if (text != null) {
                    if (text.toString().equalsIgnoreCase(initialName) ||
                            categoryManager.getIdentifier(text.toString(), true) == null) {
                        if (warning.getVisibility() != View.GONE) {
                            TransitionManager.beginDelayedTransition((ViewGroup) root.getRootView(), new ChangeBounds());

                            warning.setVisibility(View.GONE);
                        }
                    } else if (warning.getVisibility() != View.VISIBLE) {
                        TransitionManager.beginDelayedTransition((ViewGroup) root.getRootView(), new ChangeBounds());

                        warning.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        root.findViewById(R.id.reset).setOnClickListener(v -> {
            editText.setText(initialName);
            editText.setSelection(initialName.length());
        });

        return root;
    }

    @Override
    protected int getDesiredInitialState() {
        return BottomSheetBehavior.STATE_EXPANDED;
    }

    @Override
    protected boolean blurBehind() {
        return true;
    }
}
