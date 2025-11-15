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

package com.stario.launcher.sheet.briefing.configurator;

import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.stario.launcher.R;
import com.stario.launcher.sheet.briefing.dialog.page.feed.BriefingFeedList;
import com.stario.launcher.sheet.briefing.dialog.page.feed.Feed;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.dialogs.ActionDialog;

import carbon.view.SimpleTextWatcher;

public class FeedConfigurator extends ActionDialog {
    private static final String TAG = "FeedConfigurator";
    private final BriefingFeedList list;
    private final Feed feed;
    private EditText name;

    public FeedConfigurator(@NonNull ThemedActivity activity, @NonNull Feed feed) {
        super(activity);

        this.list = BriefingFeedList.from(activity);
        this.feed = feed;
    }

    @Override
    protected @NonNull View inflateContent(LayoutInflater inflater) {
        ViewGroup contentView = (ViewGroup) inflater.inflate(R.layout.feed_configurator, null);

        name = contentView.findViewById(R.id.name);
        View warning = contentView.findViewById(R.id.warning);

        name.setText(feed.getTitle());
        name.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(@NonNull Editable editable) {
                if (editable.length() == 0) {
                    warning.setVisibility(View.VISIBLE);
                } else {
                    warning.setVisibility(View.GONE);
                }
            }
        });

        return contentView;
    }

    @Override
    public void dismiss() {
        super.dismiss();

        if (name != null) {
            Editable editable = name.getText();
            String currentTitle = feed.getTitle();

            if (editable.length() > 0 &&
                    (currentTitle == null || !currentTitle.equals(editable.toString()))) {
                list.updateName(feed, editable.toString());
            }
        }
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
