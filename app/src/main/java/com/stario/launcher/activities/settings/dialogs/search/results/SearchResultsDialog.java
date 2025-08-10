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

package com.stario.launcher.activities.settings.dialogs.search.results;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.stario.launcher.R;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.sheet.drawer.search.recyclers.adapters.WebAdapter;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.dialogs.ActionDialog;

public class SearchResultsDialog extends ActionDialog {
    private final SharedPreferences preferences;

    private MaterialSwitch resultsSwitch;
    private StatusListener listener;

    public SearchResultsDialog(@NonNull ThemedActivity activity) {
        super(activity);

        preferences = activity.getApplicationContext()
                .getSharedPreferences(Entry.SEARCH);
    }

    @NonNull
    @Override
    protected View inflateContent(LayoutInflater inflater) {
        View root = inflater.inflate(R.layout.pop_up_kagi, null);

        resultsSwitch = root.findViewById(R.id.search_results);
        resultsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onChanged(isChecked);
            }
        });

        root.findViewById(R.id.search_results_container)
                .setOnClickListener(v -> resultsSwitch.performClick());

        EditText editText = root.findViewById(R.id.edit_text);
        editText.setText(preferences.getString(WebAdapter.KAGI_API_KEY, ""));
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable content) {
                preferences.edit()
                        .putString(WebAdapter.KAGI_API_KEY, content.toString())
                        .apply();
            }
        });

        root.findViewById(R.id.paste).setOnClickListener((v) -> {
            ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);

            if (clipboard.hasPrimaryClip()) {
                ClipData clip = clipboard.getPrimaryClip();

                if (clip != null) {
                    if (clip.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                        String text = clip.getItemAt(0)
                                .coerceToText(activity).toString();

                        if (!text.isEmpty()) {
                            editText.setText(text);
                            editText.setSelection(text.length());
                        }
                    }
                }
            }
        });

        return root;
    }

    @Override
    public void show() {
        super.show();

        resultsSwitch.setChecked(preferences.getBoolean(WebAdapter.SEARCH_RESULTS, false));
        resultsSwitch.jumpDrawablesToCurrentState();
    }

    public void setStatusListener(StatusListener listener) {
        this.listener = listener;
    }

    @Override
    protected boolean blurBehind() {
        return true;
    }

    @Override
    protected int getDesiredInitialState() {
        return BottomSheetBehavior.STATE_EXPANDED;
    }

    public interface StatusListener {
        void onChanged(boolean enabled);
    }
}
