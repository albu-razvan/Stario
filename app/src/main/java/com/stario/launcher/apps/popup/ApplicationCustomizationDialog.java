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

    private EditText label;

    public ApplicationCustomizationDialog(@NonNull ThemedActivity activity, LauncherApplication application) {
        super(activity);

        this.application = application;

        setOnDismissListener(dialog -> {
            if (label != null) {
                Editable newLabel = label.getText();

                if (newLabel != null) {
                    LauncherApplicationManager.getInstance()
                            .updateLabel(application, newLabel.toString());
                }
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    @NonNull
    @Override
    protected View inflateContent(LayoutInflater inflater) {
        View root = inflater.inflate(R.layout.customize_pop_up, null);

        RecyclerView icons = root.findViewById(R.id.icons);
        icons.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
        icons.setAdapter(new IconsRecyclerAdapter(activity, application, v -> dismiss()));

        View warning = root.findViewById(R.id.warning);

        label = root.findViewById(R.id.label);
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
                if (editable.length() > 0) {
                    warning.setVisibility(View.GONE);
                } else {
                    warning.setVisibility(View.VISIBLE);
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
