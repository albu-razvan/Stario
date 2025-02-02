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

package com.stario.launcher.ui.keyboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;

import com.stario.launcher.utils.UiUtils;

public class InlineAutocompleteEditText extends AppCompatEditText {
    private AutocompleteProvider provider;
    private boolean autocompleted;

    public InlineAutocompleteEditText(@NonNull Context context) {
        super(context);

        init();
    }

    public InlineAutocompleteEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public InlineAutocompleteEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {
        autocompleted = false;
    }

    public void setAutocompleteProvider(AutocompleteProvider provider) {
        this.provider = provider;
    }

    public AutocompleteProvider getAutocompleteProvider() {
        return provider;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            Editable text = getText();

            if (text != null && getSelectionEnd() == text.length()) {
                setSelection(text.length());
            }

            UiUtils.hideKeyboard(this);
        }

        return super.onKeyUp(keyCode, event);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        if (provider != null && text != null) {
            int textLength = text.length();

            if (textLength > 0 && !autocompleted &&
                    getSelectionEnd() == textLength &&
                    ((lengthBefore > 0 && lengthAfter > 0) || // selection was replaced
                            lengthBefore < lengthAfter)) { // check for insertion, not deletion
                String autocompletion = provider.autocomplete(text.toString());

                if (autocompletion != null) {
                    autocompleted = true;

                    super.setText(text + autocompletion);

                    setSelection(textLength, textLength + autocompletion.length());
                }
            } else if (autocompleted) {
                autocompleted = false;
            }
        } else if (autocompleted) {
            autocompleted = false;
        }
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        autocompleted = true; // skip autocompletion if externally set

        super.setText(text, type);
    }

    public interface AutocompleteProvider {
        /**
         * Returns an autocompleted suggestion for the given input string.
         * <p>
         * If a valid autocomplete suggestion is found based on the input, the suggestion
         * returns the remaining suggestion characters as a string. If no suggestion is available,
         * this method returns {@code null}.
         * </p>
         *
         * @param input the input string to be autocompleted
         * @return the autocompleted suggestion, or {@code null} if no suggestion is available
         */
        String autocomplete(@NonNull String input);
    }
}
