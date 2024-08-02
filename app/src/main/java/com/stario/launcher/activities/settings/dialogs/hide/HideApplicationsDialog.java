/*
    Copyright (C) 2024 Răzvan Albu

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.stario.launcher.activities.settings.dialogs.hide;

import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import com.stario.launcher.R;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.dialogs.ActionDialog;
import com.stario.launcher.ui.measurements.Measurements;
import com.stario.launcher.ui.recyclers.DividerItemDecorator;

import carbon.widget.NestedScrollView;

public class HideApplicationsDialog extends ActionDialog {
    public HideApplicationsDialog(@NonNull ThemedActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    protected View inflateContent(LayoutInflater inflater) {
        View root = inflater.inflate(R.layout.pop_up_hide, null);
        NestedScrollView scroller = root.findViewById(R.id.scroller);
        RecyclerView recycler = root.findViewById(R.id.recycler);

        scroller.setClipToOutline(true);
        Measurements.addNavListener(value -> scroller.setPadding(0, 0, 0, value));

        recycler.setLayoutManager(new LinearLayoutManager(activity,
                LinearLayoutManager.VERTICAL, false));
        recycler.addItemDecoration(new DividerItemDecorator(activity,
                MaterialDividerItemDecoration.VERTICAL));
        recycler.setAdapter(new HiddenRecyclerAdapter(activity));

        return root;
    }

    @Override
    public void show() {
        super.show();

        if (!Measurements.isLandscape()) {
            getBehavior().setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        }
    }

    @Override
    protected boolean blurBehind() {
        return true;
    }
}
