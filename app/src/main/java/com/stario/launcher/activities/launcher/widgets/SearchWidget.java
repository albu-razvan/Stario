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

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.stario.launcher.R;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.sheet.SheetsFocusController;
import com.stario.launcher.sheet.drawer.dialog.ApplicationsDialog;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.common.grid.DraggableGridItem;
import com.stario.launcher.ui.common.grid.DynamicGridLayout;
import com.stario.launcher.ui.utils.LayoutSizeObserver;
import com.stario.launcher.ui.utils.UiUtils;

public class SearchWidget {
    public static final String SEARCH_WIDGET_KEY = "com.stario.HOMESCREEN_SEARCH_WIDGET_VISIBLE";

    private static final String SEARCH_TAG = "SearchWidget";

    private final SharedPreferences preferences;
    private final ThemedActivity activity;

    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private DynamicGridLayout.ItemLayoutData layoutData;
    private DraggableGridItem gridItem;
    private boolean isAttached;

    public SearchWidget(ThemedActivity activity) {
        this.isAttached = false;
        this.activity = activity;

        this.preferences = activity.getApplicationContext().getSettings();
    }

    public void attach(DynamicGridLayout container) {
        if (gridItem != null) {
            return;
        }

        gridItem = new DraggableGridItem(activity);
        gridItem.itemId = SEARCH_TAG;

        layoutData = new DynamicGridLayout.ItemLayoutData(SEARCH_TAG,
                0, 0, 1, 1);
        layoutData.maxColSpan = 1;
        layoutData.maxRowSpan = 1;

        View root = activity.getLayoutInflater()
                .inflate(R.layout.home_search, gridItem, false);

        View background = root.findViewById(R.id.background);
        LayoutSizeObserver.attach(background, LayoutSizeObserver.WIDTH | LayoutSizeObserver.HEIGHT,
                new LayoutSizeObserver.OnChange() {
                    @Override
                    public void onChange(View view, int watchFlags) {
                        background.setPivotX(background.getWidth() / 2f);
                        background.setPivotY(background.getHeight() / 2f);
                    }
                });
        background.setPivotX(background.getWidth() / 2f);
        background.setPivotY(background.getHeight() / 2f);

        ObjectAnimator rotate = ObjectAnimator.ofFloat(background, View.ROTATION, 0f, 360f);
        rotate.setDuration(50000);
        rotate.setRepeatCount(ValueAnimator.INFINITE);
        rotate.setInterpolator(new LinearInterpolator());
        rotate.start();

        root.setOnTouchListener(SheetsFocusController.createClickTouchListener(
                view1 -> {
                    Vibrations.getInstance().vibrate();

                    //noinspection deprecation
                    LocalBroadcastManager.getInstance(activity)
                            .sendBroadcastSync(new Intent(ApplicationsDialog.INTENT_LAUNCH_SEARCH));
                }));

        gridItem.addView(root);

        listener = (sharedPreferences, key) -> {
            if (SEARCH_WIDGET_KEY.equals(key)) {
                updateContainerState(container,
                        sharedPreferences.getBoolean(SEARCH_WIDGET_KEY, true));
            }
        };
        preferences.registerOnSharedPreferenceChangeListener(listener);

        updateContainerState(container, preferences.getBoolean(SEARCH_WIDGET_KEY, true));
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