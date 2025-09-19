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
import android.content.res.Configuration;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.core.math.MathUtils;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.activities.launcher.pins.dialog.PinnedAppsGroupDialog;
import com.stario.launcher.apps.CategoryManager;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.icons.AdaptiveIconView;
import com.stario.launcher.ui.recyclers.autogrid.AutoGridLayoutManager;
import com.stario.launcher.ui.utils.LayoutSizeObserver;
import com.stario.launcher.ui.utils.animation.Animation;
import com.stario.launcher.utils.objects.ObjectDelegate;

public class PinnedCategory {
    public static final String PINNED_CATEGORY_VISIBLE = "com.stario.IS_PINNED_CATEGORY_VISIBLE";
    public static final String PINNED_CATEGORY = "com.stario.PINNED_CATEGORY";

    private final CategoryManager categoryManager;
    private final SharedPreferences preferences;
    private final ThemedActivity activity;

    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private Consumer<Configuration> landscapeObserver;
    private RecyclerView recycler;

    public PinnedCategory(ThemedActivity activity) {
        this.activity = activity;
        this.categoryManager = CategoryManager.getInstance();
        this.preferences = activity.getApplicationContext()
                .getSharedPreferences(Entry.PINNED_CATEGORY);
    }

    public void attach(ViewGroup container,
                       PinnedAppsAdapter.OnPopUpShowListener popUpShowListener,
                       PinnedAppsGroupDialog.TransitionListener transitionListener) {
        RelativeLayout root = (RelativeLayout) activity.getLayoutInflater()
                .inflate(R.layout.pinned_apps, container, false);
        recycler = root.findViewById(R.id.recycler);

        ObjectDelegate<Boolean> visible = new ObjectDelegate<>(
                preferences.getBoolean(PINNED_CATEGORY_VISIBLE, false), value -> {
            root.post(() -> {
                if (value && categoryManager.isReady()) {
                    root.setVisibility(View.VISIBLE);
                } else {
                    root.setVisibility(View.GONE);
                }
            });
        });

        if (visible.getValue()) {
            root.setVisibility(View.VISIBLE);
        } else {
            root.setVisibility(View.GONE);
        }

        listener = (sharedPreferences, key) -> {
            if (PINNED_CATEGORY_VISIBLE.equals(key)) {
                visible.setValue(sharedPreferences
                        .getBoolean(PINNED_CATEGORY_VISIBLE, false));
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

        landscapeObserver = new Consumer<>() {
            {
                updateLayout();
            }

            private void updateLayout() {
                manager.setCenterItems(!Measurements.isLandscape());

                if (Measurements.isLandscape()) {
                    root.setLayoutParams(new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1.0f
                    ));
                } else {
                    root.setLayoutParams(new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                }

                root.forceLayout();
            }

            @Override
            public void accept(Configuration configuration) {
                updateLayout();
            }
        };
        activity.addOnConfigurationChangedListener(landscapeObserver);

        recycler.setItemAnimator(null);
        recycler.setLayoutManager(manager);

        categoryManager.addOnReadyListener(applicationManager -> {
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }

            root.post(() -> {
                recycler.setAdapter(adapter);
                recycler.animate().alpha(1)
                        .setDuration(Animation.LONG.getDuration());
            });
        });

        container.addView(root);
    }

    public void detach() {
        if (recycler != null) {
            recycler.setAdapter(null);
        }

        if (listener != null) {
            preferences.unregisterOnSharedPreferenceChangeListener(listener);
        }

        if (landscapeObserver != null) {
            activity.removeOnConfigurationChangedListener(landscapeObserver);
        }
    }
}
