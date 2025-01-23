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

package com.stario.launcher.activities.settings.dialogs;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.stario.launcher.BuildConfig;
import com.stario.launcher.R;
import com.stario.launcher.services.AccessibilityService;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.dialogs.ActionDialog;

public class AccessibilityConfigurator extends ActionDialog {
    public AccessibilityConfigurator(@NonNull ThemedActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    protected View inflateContent(LayoutInflater inflater) {
        View root = inflater.inflate(R.layout.pop_up_accesibility, null);

        root.findViewById(R.id.proceed)
                .setOnClickListener(v -> {
                    setOnDismissListener(null);
                    showAccessibilitySettingsActivity();
                });
        root.findViewById(R.id.cancel)
                .setOnClickListener(v -> dismiss());

        return root;
    }

    private void showAccessibilitySettingsActivity() {
        Bundle bundle = new Bundle();
        String showArgs = BuildConfig.APPLICATION_ID + "/" + AccessibilityService.class.getName();
        bundle.putString(":settings:fragment_args_key", showArgs);

        Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);

        intent.putExtra(":settings:fragment_args_key", showArgs);
        intent.putExtra(":settings:show_fragment_args", bundle);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        activity.startActivity(intent);
    }

    @Override
    protected boolean blurBehind() {
        return true;
    }

    @Override
    protected int getDesiredInitialState() {
        return BottomSheetBehavior.STATE_EXPANDED;
    }
}
