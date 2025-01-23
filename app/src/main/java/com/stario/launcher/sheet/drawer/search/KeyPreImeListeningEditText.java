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

package com.stario.launcher.sheet.drawer.search;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;

import java.util.HashMap;

public class KeyPreImeListeningEditText extends AppCompatEditText {
    private HashMap<Integer, OnKeyListener> listeners;

    public KeyPreImeListeningEditText(@NonNull Context context) {
        super(context);

        listeners = new HashMap<>();
    }

    public KeyPreImeListeningEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        listeners = new HashMap<>();
    }

    public KeyPreImeListeningEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        listeners = new HashMap<>();
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            OnKeyListener listener = listeners.getOrDefault(event.getKeyCode(), null);

            if (listener != null && listener.onKey()) {
                return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    public void addOnKeyUp(int keyCode, OnKeyListener listener) {
        if (listener != null) {
            listeners.put(keyCode, listener);
        }
    }

    public interface OnKeyListener {
        /**
         * Return true if back event should be intercepted
         */
        boolean onKey();
    }
}
