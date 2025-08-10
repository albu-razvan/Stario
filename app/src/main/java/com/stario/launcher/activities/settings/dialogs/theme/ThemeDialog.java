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

package com.stario.launcher.activities.settings.dialogs.theme;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.stario.launcher.R;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.dialogs.ActionDialog;

public class ThemeDialog extends ActionDialog {
    private OnDismissListener listener;
    private boolean recreateActivity;

    public ThemeDialog(@NonNull ThemedActivity activity) {
        super(activity);

        this.listener = null;
        this.recreateActivity = false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @NonNull
    @Override
    protected View inflateContent(LayoutInflater inflater) {
        SharedPreferences themePreferences = activity.getApplicationContext()
                .getSharedPreferences(Entry.THEME);
        View root = inflater.inflate(R.layout.pop_up_theme, null);

        boolean isForceDarkOn = themePreferences.getBoolean(ThemedActivity.FORCE_DARK, false);

        MaterialSwitch materialSwitch = root.findViewById(R.id.force_dark);
        materialSwitch.setChecked(isForceDarkOn);
        materialSwitch.jumpDrawablesToCurrentState();
        materialSwitch.setOnCheckedChangeListener((compound, isChecked) -> {
            themePreferences.edit()
                    .putBoolean(ThemedActivity.FORCE_DARK, isChecked)
                    .apply();
        });

        root.findViewById(R.id.force_dark_container)
                .setOnClickListener(v -> materialSwitch.performClick());

        RecyclerView recycler = root.findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(activity,
                LinearLayoutManager.HORIZONTAL, false));
        recycler.setAdapter(new ThemeRecyclerAdapter(activity, v -> {
            recreateActivity = true;
            dismiss();
        }));

        super.setOnDismissListener(dialog -> {
            if (listener != null) {
                listener.onDismiss(recreateActivity ||
                        isForceDarkOn != materialSwitch.isChecked());
            }
        });

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

    @Override
    public void setOnDismissListener(@Nullable DialogInterface.OnDismissListener listener) {
        throw new RuntimeException("Operation not supported by " + getClass().getName());
    }

    public void setOnDismissListener(@Nullable OnDismissListener listener) {
        this.listener = listener;
    }

    public interface OnDismissListener {
        void onDismiss(boolean stateChanged);
    }
}
