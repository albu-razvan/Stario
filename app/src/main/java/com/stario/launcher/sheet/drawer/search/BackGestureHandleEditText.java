package com.stario.launcher.sheet.drawer.search;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;

import com.stario.launcher.utils.Utils;

public class BackGestureHandleEditText extends AppCompatEditText {
    private OnBackInvoke listener;

    public BackGestureHandleEditText(@NonNull Context context) {
        super(context);
    }

    public BackGestureHandleEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BackGestureHandleEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (!Utils.isMinimumSDK(Build.VERSION_CODES.TIRAMISU)) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK &&
                    event.getAction() == KeyEvent.ACTION_UP) {
                if (listener != null && listener.onBack()) {
                    return true;
                }
            }
        }

        return super.dispatchKeyEvent(event);
    }

    public void setOnBackInvoked(OnBackInvoke listener) {
        this.listener = listener;
    }

    public interface OnBackInvoke {
        /**
         * Return true if back event should be intercepted
         */
        boolean onBack();
    }
}
