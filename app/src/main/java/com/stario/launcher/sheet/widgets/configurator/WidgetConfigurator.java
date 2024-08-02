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

package com.stario.launcher.sheet.widgets.configurator;

import android.appwidget.AppWidgetProviderInfo;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import com.stario.launcher.R;
import com.stario.launcher.sheet.widgets.WidgetSize;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.dialogs.ActionDialog;
import com.stario.launcher.ui.measurements.Measurements;
import com.stario.launcher.ui.recyclers.DividerItemDecorator;

public class WidgetConfigurator extends ActionDialog {
    private static final String TAG = "WidgetConfigurator";
    private final Request requestListener;
    private WidgetListAdapter adapter;

    public WidgetConfigurator(@NonNull ThemedActivity activity, @NonNull Request requestListener) {
        super(activity);

        this.requestListener = requestListener;
    }

    @NonNull
    @Override
    protected View inflateContent(LayoutInflater inflater) {
        View contentView = inflater.inflate(R.layout.widget_picker, null);

        NestedScrollView scroller = contentView.findViewById(R.id.scroller);
        RecyclerView recycler = contentView.findViewById(R.id.container_widgets);

        scroller.setClipToOutline(true);

        Measurements.addNavListener(value ->
                scroller.setPadding(0, 0, 0, value));

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);

        adapter = new WidgetListAdapter(activity, recycler, requestListener);

        recycler.setAdapter(adapter);
        recycler.setLayoutManager(layoutManager);
        recycler.addItemDecoration(new DividerItemDecorator(activity, MaterialDividerItemDecoration.VERTICAL));

        return contentView;
    }

    @Override
    protected boolean blurBehind() {
        return false;
    }

    @Override
    public void show() {
        super.show();

        if (!Measurements.isLandscape()) {
            getBehavior().setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        }

        adapter.update();
    }

    public interface Request {
        void requestAddition(AppWidgetProviderInfo info, WidgetSize size);
    }
}
