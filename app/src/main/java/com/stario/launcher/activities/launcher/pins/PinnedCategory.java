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

package com.stario.launcher.activities.launcher.pins;

import android.content.SharedPreferences;
import android.graphics.Rect;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.core.math.MathUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.activities.launcher.pins.dialog.PinnedAppsGroupDialog;
import com.stario.launcher.apps.CategoryManager;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.common.grid.DraggableGridItem;
import com.stario.launcher.ui.common.grid.DynamicGridLayout;
import com.stario.launcher.ui.icons.AdaptiveIconView;
import com.stario.launcher.ui.recyclers.autogrid.AutoGridLayoutManager;
import com.stario.launcher.ui.utils.LayoutSizeObserver;
import com.stario.launcher.ui.utils.UiUtils;

public class PinnedCategory {
    public static final String PINNED_CATEGORY_VISIBLE = "com.stario.IS_PINNED_CATEGORY_VISIBLE";
    public static final String PINNED_CATEGORY = "com.stario.PINNED_CATEGORY";
    private static final String CATEGORY_TAG = "CategoryGlance";

    private final CategoryManager categoryManager;
    private final SharedPreferences preferences;
    private final ThemedActivity activity;

    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private RecyclerView recycler;

    private DynamicGridLayout.ItemLayoutData layoutData;
    private DraggableGridItem gridItem;
    private boolean isAttached;

    public PinnedCategory(ThemedActivity activity) {
        this.isAttached = false;
        this.activity = activity;
        this.categoryManager = CategoryManager.getInstance();
        this.preferences = activity.getApplicationContext()
                .getSharedPreferences(Entry.PINNED_CATEGORY);
    }

    public void attach(DynamicGridLayout container,
                       PinnedAppsAdapter.OnPopUpShowListener popUpShowListener,
                       PinnedAppsGroupDialog.TransitionListener transitionListener) {
        if (gridItem != null) {
            return;
        }

        gridItem = new DraggableGridItem(activity);
        gridItem.itemId = CATEGORY_TAG;

        layoutData = new DynamicGridLayout.ItemLayoutData(CATEGORY_TAG,
                0, 0, 4, 1);
        layoutData.minColSpan = 1;
        layoutData.maxRowSpan = 1;

        RelativeLayout root = (RelativeLayout) activity.getLayoutInflater()
                .inflate(R.layout.pinned_apps, gridItem, false);
        recycler = root.findViewById(R.id.recycler);
        gridItem.addView(root);

        listener = (sharedPreferences, key) -> {
            if (PINNED_CATEGORY_VISIBLE.equals(key)) {
                updateContainerState(container,
                        sharedPreferences.getBoolean(PINNED_CATEGORY_VISIBLE, false));
            }
        };
        preferences.registerOnSharedPreferenceChangeListener(listener);

        AutoGridLayoutManager manager = new AutoGridLayoutManager(activity, 1);
        PinnedAppsAdapter adapter = new PinnedAppsAdapter(activity,
                preferences, popUpShowListener, transitionListener);

        LayoutSizeObserver.attach(root, LayoutSizeObserver.WIDTH, new LayoutSizeObserver.OnChange() {
            @Override
            public void onChange(View view, int watchFlags, Rect rect) {
                int columns = MathUtils.clamp(rect.width() /
                                (AdaptiveIconView.getMaxIconSize()
                                        + Measurements.getDefaultPadding()),
                        1, 6);

                manager.setSpanCount(columns);
                adapter.setMaxItemCount(columns);
            }
        });

        recycler.setItemAnimator(null);
        recycler.setLayoutManager(manager);

        categoryManager.addOnReadyListener(applicationManager -> {
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }

            recycler.setAdapter(adapter);
        });

        updateContainerState(container, preferences.getBoolean(PINNED_CATEGORY_VISIBLE, false));
    }

    private void updateContainerState(DynamicGridLayout container, boolean shouldBeVisible) {
        UiUtils.post(() -> {
            if (shouldBeVisible && !isAttached) {
                container.addItem(gridItem, layoutData);
                isAttached = true;
            } else if (!shouldBeVisible && isAttached) {
                container.removeItem(gridItem);
                isAttached = false;
            }
        });
    }

    public void detach() {
        if (recycler != null) {
            recycler.setAdapter(null);
        }

        if (listener != null) {
            preferences.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }
}
