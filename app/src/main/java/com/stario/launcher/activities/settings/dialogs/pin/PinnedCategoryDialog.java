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

package com.stario.launcher.activities.settings.dialogs.pin;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.stario.launcher.R;
import com.stario.launcher.activities.launcher.pins.PinnedCategory;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.dialogs.ActionDialog;
import com.stario.launcher.ui.recyclers.DividerItemDecorator;

public class PinnedCategoryDialog extends ActionDialog {
    private final OnCheckedChangeListener checkedChangeListener;
    private final SharedPreferences preferences;

    public PinnedCategoryDialog(@NonNull ThemedActivity activity, SharedPreferences preferences,
                                OnCheckedChangeListener checkedChangeListener) {
        super(activity);

        this.preferences = preferences;
        this.checkedChangeListener = checkedChangeListener;
    }

    @NonNull
    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected View inflateContent(LayoutInflater inflater) {
        View root = inflater.inflate(R.layout.pop_up_pinned_category, null);
        RecyclerView recycler = root.findViewById(R.id.recycler);

        MaterialSwitch materialSwitch = root.findViewById(R.id.pinned_category);
        materialSwitch.setChecked(preferences.getBoolean(PinnedCategory.PINNED_CATEGORY_VISIBLE, false));
        materialSwitch.jumpDrawablesToCurrentState();

        materialSwitch.setOnCheckedChangeListener((compound, isChecked) -> {
            if (checkedChangeListener != null) {
                boolean override = checkedChangeListener.onChecked(isChecked);

                if (override != isChecked) {
                    materialSwitch.setChecked(override);
                }
            }
        });

        root.setOnClickListener(view -> materialSwitch.performClick());

        recycler.setLayoutManager(new LinearLayoutManager(activity,
                LinearLayoutManager.VERTICAL, false));
        recycler.addItemDecoration(new DividerItemDecorator(activity,
                MaterialDividerItemDecoration.VERTICAL));
        recycler.setAdapter(new PinnedCategoryRecyclerAdapter(activity, (v) -> {
            if(materialSwitch.isChecked()) {
                dismiss();
            } else {
                materialSwitch.setChecked(true);
            }
        }));

        return root;
    }

    @Override
    protected boolean blurBehind() {
        return true;
    }

    @Override
    protected int getDesiredInitialState() {
        return BottomSheetBehavior.STATE_EXPANDED;
    }

    public interface OnCheckedChangeListener {

        /**
         * @return checked state override value
         */
        boolean onChecked(boolean isChecked);
    }
}
