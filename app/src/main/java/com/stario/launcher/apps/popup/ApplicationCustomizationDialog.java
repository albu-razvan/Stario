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

package com.stario.launcher.apps.popup;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.transition.ChangeBounds;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.stario.launcher.R;
import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.apps.LauncherApplicationManager;
import com.stario.launcher.apps.CategoryManager;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.dialogs.ActionDialog;
import com.stario.launcher.ui.keyboard.InlineAutocompleteEditText;

public class ApplicationCustomizationDialog extends ActionDialog {
    private final LauncherApplication application;
    private final CategoryManager categoryManager;

    private InlineAutocompleteEditText category;
    private EditText label;

    public ApplicationCustomizationDialog(@NonNull ThemedActivity activity, LauncherApplication application) {
        super(activity);

        this.application = application;
        this.categoryManager = CategoryManager.getInstance();

        setOnDismissListener(dialog -> {
            Editable newLabel = label.getText();

            if (newLabel != null) {
                LauncherApplicationManager.getInstance()
                        .updateLabel(application, newLabel.toString());
            }

            Editable newCategoryName = category.getText();

            if (newCategoryName != null &&
                    !newCategoryName.toString()
                            .equals(categoryManager.getCategoryName(application.getCategory()))) {
                categoryManager.updateCategory(application,
                        categoryManager.addCustomCategory(newCategoryName.toString()));
                LauncherApplicationManager.getInstance().notifyUpdate(application);
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    @NonNull
    @Override
    protected View inflateContent(LayoutInflater inflater) {
        View root = inflater.inflate(R.layout.pop_up_customize, null);

        RecyclerView icons = root.findViewById(R.id.icons);
        icons.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
        icons.setAdapter(new IconsRecyclerAdapter(activity, application, v -> dismiss()));

        View labelWarning = root.findViewById(R.id.label_warning);

        label = root.findViewById(R.id.label);
        label.setText(application.getLabel());
        label.setFocusable(true);
        label.setFocusableInTouchMode(true);
        label.setShowSoftInputOnFocus(true);
        label.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        label.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() > 0) {
                    if (labelWarning.getVisibility() != View.GONE) {
                        TransitionManager.beginDelayedTransition((ViewGroup) root.getRootView(), new ChangeBounds());

                        labelWarning.setVisibility(View.GONE);
                    }
                } else if (labelWarning.getVisibility() != View.VISIBLE) {
                    TransitionManager.beginDelayedTransition((ViewGroup) root.getRootView(), new ChangeBounds());

                    labelWarning.setVisibility(View.VISIBLE);
                }
            }
        });

        View categoryWarning = root.findViewById(R.id.category_warning);

        category = root.findViewById(R.id.category);
        category.setText(categoryManager.getCategoryName(application.getCategory()));
        category.setFocusable(true);
        category.setFocusableInTouchMode(true);
        category.setShowSoftInputOnFocus(true);
        category.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        category.setAutocompleteProvider(input -> {
            String suggestion = categoryManager.getSuggestion(input);

            if (suggestion != null) {
                return suggestion.substring(suggestion.toLowerCase()
                        .indexOf(input.toLowerCase()) + input.length());
            }

            return null;
        });
        category.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                Editable text = category.getText();

                if (text != null) {
                    if (categoryManager.getIdentifier(text.toString()) != null) {
                        if (categoryWarning.getVisibility() != View.GONE) {
                            TransitionManager.beginDelayedTransition((ViewGroup) root.getRootView(), new ChangeBounds());

                            categoryWarning.setVisibility(View.GONE);
                        }
                    } else if (categoryWarning.getVisibility() != View.VISIBLE) {
                        TransitionManager.beginDelayedTransition((ViewGroup) root.getRootView(), new ChangeBounds());

                        categoryWarning.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        root.findViewById(R.id.reset).setOnClickListener(view -> {
            String applicationLabel = application.getInfo()
                    .loadLabel(activity.getPackageManager()).toString();

            label.setText(applicationLabel);
            label.setSelection(applicationLabel.length());
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
