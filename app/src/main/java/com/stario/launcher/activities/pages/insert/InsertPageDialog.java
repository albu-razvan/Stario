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

package com.stario.launcher.activities.pages.insert;

import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import com.stario.launcher.R;
import com.stario.launcher.sheet.SheetDialogFragment;
import com.stario.launcher.sheet.SheetType;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.dialogs.ActionDialog;
import com.stario.launcher.ui.recyclers.DividerItemDecorator;

import java.util.List;

public class InsertPageDialog extends ActionDialog {
    private final InsertPageRecyclerAdapter adapter;

    public InsertPageDialog(@NonNull ThemedActivity activity, @NonNull OnItemSelected listener) {
        super(activity);

        this.adapter = new InsertPageRecyclerAdapter(activity, item -> {
            listener.onSelect(item);

            dismiss();
        });
    }

    @NonNull
    @Override
    protected View inflateContent(LayoutInflater inflater) {
        View root = inflater.inflate(R.layout.pop_up_insert_item, null);

        RecyclerView recycler = root.findViewById(R.id.recycler);

        recycler.setLayoutManager(new LinearLayoutManager(activity,
                LinearLayoutManager.VERTICAL, false));
        recycler.addItemDecoration(new DividerItemDecorator(activity, MaterialDividerItemDecoration.VERTICAL));

        recycler.setAdapter(adapter);

        return root;
    }

    public void setItems(List<Pair<SheetType, Class<? extends SheetDialogFragment>>> items) {
        adapter.setItems(items);
    }

    @Override
    protected boolean blurBehind() {
        return true;
    }

    @Override
    protected int getDesiredInitialState() {
        return BottomSheetBehavior.STATE_EXPANDED;
    }

    public interface OnItemSelected {
        void onSelect(Pair<SheetType, Class<? extends SheetDialogFragment>> item);
    }
}
