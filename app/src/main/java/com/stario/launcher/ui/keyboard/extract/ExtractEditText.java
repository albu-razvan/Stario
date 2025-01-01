package com.stario.launcher.ui.keyboard.extract;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.measurements.Measurements;
import com.stario.launcher.utils.UiUtils;

public class ExtractEditText extends AppCompatEditText {
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
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
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
        UiUtils.hideKeyboard(this);

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
