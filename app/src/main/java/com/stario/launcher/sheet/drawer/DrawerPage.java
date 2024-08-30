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

package com.stario.launcher.sheet.drawer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.sheet.SheetDialogFragment;
import com.stario.launcher.sheet.drawer.dialog.ApplicationsDialog;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.measurements.Measurements;
import com.stario.launcher.ui.recyclers.ScrollToTop;
import com.stario.launcher.utils.UiUtils;

public abstract class DrawerPage extends Fragment implements ScrollToTop {
    private BroadcastReceiver sheetReceiver;
    private LocalBroadcastManager localBroadcastManager;
    protected ThemedActivity activity;
    protected RecyclerView drawer;
    protected TextView title;
    protected EditText search;

    @Override
    public void onAttach(@NonNull Context context) {
        if (!(context instanceof ThemedActivity)) {
            throw new RuntimeException("Parent activity is not of type ThemedActivity.");
        }

        activity = (ThemedActivity) context;
        localBroadcastManager = LocalBroadcastManager.getInstance(activity);

        super.onAttach(context);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(getLayoutResID(), container, false);

        drawer = root.findViewById(R.id.drawer);
        title = root.findViewById(R.id.title);
        search = container.getRootView().findViewById(R.id.search);

        drawer.setOverScrollMode(View.OVER_SCROLL_NEVER);

        drawer.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                updateTitleTransforms(recyclerView);
            }
        });

        sheetReceiver = new BroadcastReceiver() {
            private float lastOffset = 0.5f - SheetDialogFragment.PUBLISH_STEP;
            private long scheduledUpdateTime = 0;

            @Override
            public void onReceive(Context context, Intent intent) {
                String className = intent.getStringExtra(SheetDialogFragment.CLASS);
                RecyclerView.Adapter<?> adapter = drawer.getAdapter();

                if (adapter instanceof BumpRecyclerViewAdapter &&
                        ApplicationsDialog.class.getName().equals(className)) {
                    BumpRecyclerViewAdapter bumpAdapter = (BumpRecyclerViewAdapter) adapter;
                    float offset = intent.getFloatExtra(SheetDialogFragment.OFFSET, 0f);

                    if (offset > lastOffset) {
                        int bumpSize;

                        if (offset == 1f) {
                            UiUtils.runOnUIThreadDelayed(bumpAdapter::removeLimit,
                                    Math.max(0, scheduledUpdateTime - System.currentTimeMillis()));
                        } else {
                            bumpSize = Math.round((offset - lastOffset) / SheetDialogFragment.PUBLISH_STEP);

                            // Fake a loading delay not to freeze the UI when inflating all views
                            int loop = bumpSize;
                            while (loop > 0) {
                                UiUtils.runOnUIThreadDelayed(bumpAdapter::bump,
                                        Math.max(0, scheduledUpdateTime - System.currentTimeMillis()) +
                                                loop * BumpRecyclerViewAdapter.DELAY);

                                loop--;
                            }

                            scheduledUpdateTime = Math.max(System.currentTimeMillis(), scheduledUpdateTime) +
                                    bumpSize * BumpRecyclerViewAdapter.DELAY;
                            lastOffset = offset;
                        }
                    }
                }
            }
        };

        localBroadcastManager.registerReceiver(sheetReceiver,
                new IntentFilter(SheetDialogFragment.SHEET_EVENT));

        title.setMinHeight(Measurements.dpToPx(Measurements.HEADER_SIZE_DP) +
                Measurements.spToPx(8));

        Measurements.addSysUIListener(value -> {
            drawer.setPadding(drawer.getPaddingLeft(),
                    value + (Measurements.isLandscape() ? Measurements.getDefaultPadding() :
                            Measurements.dpToPx(Measurements.HEADER_SIZE_DP) + Measurements.getDefaultPadding()),
                    drawer.getPaddingRight(),
                    drawer.getPaddingBottom());

            ((ViewGroup.MarginLayoutParams) title.getLayoutParams()).topMargin = value;
        });

        View searchContainer = (View) search.getParent();

        drawer.setPadding(drawer.getPaddingLeft(),
                drawer.getPaddingTop(), drawer.getPaddingRight(),
                searchContainer.getPaddingBottom() + (search.getBottom() - search.getTop()));

        search.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                drawer.setPadding(drawer.getPaddingLeft(),
                        drawer.getPaddingTop(), drawer.getPaddingRight(),
                        searchContainer.getPaddingBottom() + (bottom - top)));

        updateTitleVisibility();

        return root;
    }

    private void updateTitleTransforms(RecyclerView recyclerView) {
        title.setTranslationY(-recyclerView.computeVerticalScrollOffset());

        int headerSize = Measurements.dpToPx(Measurements.HEADER_SIZE_DP);
        title.setAlpha(1f - recyclerView.computeVerticalScrollOffset() / (headerSize / 2f));
    }

    private void updateTitleVisibility() {
        title.post(() -> {
            if (Measurements.isLandscape()) {
                title.setVisibility(View.GONE);
            } else {
                title.setVisibility(View.VISIBLE);

                updateTitleTransforms(drawer);
            }
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        updateTitleVisibility();
    }

    @Override
    public void onResume() {
        updateTitleVisibility();

        super.onResume();
    }

    @Override
    public void onDestroy() {
        localBroadcastManager.unregisterReceiver(sheetReceiver);

        super.onDestroy();
    }

    @Override
    public void scrollToTop() {
        if (drawer != null) {
            drawer.scrollToPosition(0);
        }
    }

    protected abstract int getLayoutResID();

    protected abstract int getPosition();
}
