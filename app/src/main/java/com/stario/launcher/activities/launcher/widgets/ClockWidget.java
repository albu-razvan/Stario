/*
 * Copyright (C) 2026 Răzvan Albu
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

package com.stario.launcher.activities.launcher.widgets;

import android.content.SharedPreferences;
import android.view.View;

import com.stario.launcher.R;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.common.grid.DraggableGridItem;
import com.stario.launcher.ui.common.grid.DynamicGridLayout;
import com.stario.launcher.ui.utils.UiUtils;

public class ClockWidget {
    public static final String CLOCK_WIDGET_KEY = "com.stario.HOMESCREEN_CLOCK_WIDGET_VISIBLE";

    private static final String CLOCK_TAG = "StylizedClockGlance";

    private final SharedPreferences preferences;
    private final ThemedActivity activity;

    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private DynamicGridLayout.ItemLayoutData layoutData;
    private DraggableGridItem gridItem;
    private boolean isAttached;

    public ClockWidget(ThemedActivity activity) {
        this.isAttached = false;
        this.activity = activity;

        this.preferences = activity.getApplicationContext().getSettings();
    }

    public void attach(DynamicGridLayout container) {
        if (gridItem != null) {
            return;
        }

        gridItem = new DraggableGridItem(activity);
        gridItem.itemId = CLOCK_TAG;

        layoutData = new DynamicGridLayout.ItemLayoutData(CLOCK_TAG,
                0, 0, 2, 2);
        layoutData.minColSpan = 2;
        layoutData.minRowSpan = 1;

        View root = activity.getLayoutInflater()
                .inflate(R.layout.home_clock, gridItem, false);
        gridItem.addView(root);

        listener = (sharedPreferences, key) -> {
            if (CLOCK_WIDGET_KEY.equals(key)) {
                updateContainerState(container,
                        sharedPreferences.getBoolean(CLOCK_WIDGET_KEY, true));
            }
        };
        preferences.registerOnSharedPreferenceChangeListener(listener);

        updateContainerState(container, preferences.getBoolean(CLOCK_WIDGET_KEY, true));
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
        if (listener != null) {
            preferences.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }
}