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

package com.stario.launcher.activities.settings.dialogs.location;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import com.stario.launcher.R;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.dialogs.ActionDialog;
import com.stario.launcher.ui.recyclers.DividerItemDecorator;
import com.stario.launcher.ui.utils.UiUtils;

public class LocationDialog extends ActionDialog {
    public LocationDialog(@NonNull ThemedActivity activity) {
        super(activity);
    }

    @SuppressLint("ClickableViewAccessibility")
    @NonNull
    @Override
    protected View inflateContent(LayoutInflater inflater) {
        View root = inflater.inflate(R.layout.pop_up_location, null);

        EditText query = root.findViewById(R.id.query);
        RecyclerView recycler = root.findViewById(R.id.recycler);

        recycler.addItemDecoration(new DividerItemDecorator(getContext(),
                MaterialDividerItemDecoration.VERTICAL));
        recycler.setLayoutManager(new LinearLayoutManager(activity,
                LinearLayoutManager.VERTICAL, false));
        recycler.setItemAnimator(null);
        recycler.setOnTouchListener(new View.OnTouchListener() {
            private final BottomSheetBehavior<?> behavior;
            private boolean scrolledToTop;

            {
                this.behavior = getBehavior();
                this.scrolledToTop = true;

                recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        scrolledToTop = recyclerView.computeVerticalScrollOffset() == 0;
                    }
                });
            }

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (UiUtils.isKeyboardVisible(view)) {
                    behavior.setDraggable(false);
                } else {
                    if (event.getAction() == MotionEvent.ACTION_CANCEL ||
                            event.getAction() == MotionEvent.ACTION_UP) {
                        behavior.setDraggable(true);
                    } else {
                        behavior.setDraggable(scrolledToTop);
                    }
                }

                return false;
            }
        });

        LocationRecyclerAdapter adapter = new LocationRecyclerAdapter(activity,
                v -> {
                    UiUtils.hideKeyboard(v);

                    BottomSheetBehavior<?> behavior = getBehavior();

                    behavior.setDraggable(true);
                    behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                });
        recycler.setAdapter(adapter);

        query.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                adapter.update(editable != null ? editable.toString() : null);
            }
        });

        return root;
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
