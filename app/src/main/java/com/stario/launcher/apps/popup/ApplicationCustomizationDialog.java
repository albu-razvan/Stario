package com.stario.launcher.apps.popup;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.stario.launcher.R;
import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.dialogs.ActionDialog;
import com.stario.launcher.ui.measurements.Measurements;

public class ApplicationCustomizationDialog extends ActionDialog {
    private final LauncherApplication application;

    public ApplicationCustomizationDialog(@NonNull ThemedActivity activity, LauncherApplication application) {
        super(activity);

        this.application = application;
    }

    @SuppressLint("ClickableViewAccessibility")
    @NonNull
    @Override
    protected View inflateContent(LayoutInflater inflater) {
        View root = inflater.inflate(R.layout.customize_pop_up, null);

        ViewGroup content = root.findViewById(R.id.content);
        RecyclerView icons = root.findViewById(R.id.icons);
        icons.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
        icons.setAdapter(new IconsRecyclerAdapter(activity, application, v -> dismiss()));

        Measurements.addNavListener(value -> content.setPadding(0, 0, 0, value));

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
