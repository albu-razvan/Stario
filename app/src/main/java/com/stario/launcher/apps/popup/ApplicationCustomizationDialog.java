package com.stario.launcher.apps.popup;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.stario.launcher.R;
import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.apps.LauncherApplicationManager;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.dialogs.ActionDialog;

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

        RecyclerView icons = root.findViewById(R.id.icons);
        icons.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
        icons.setAdapter(new IconsRecyclerAdapter(activity, application, v -> dismiss()));

        EditText label = root.findViewById(R.id.label);
        label.setText(application.getLabel());

        label.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                LauncherApplicationManager.getInstance()
                        .updateLabel(application, editable.toString());
            }
        });

        root.findViewById(R.id.reset).setOnClickListener(view -> {
            LauncherApplicationManager.getInstance()
                    .updateLabel(application, null);

            String applicationLabel = application.getLabel();

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
