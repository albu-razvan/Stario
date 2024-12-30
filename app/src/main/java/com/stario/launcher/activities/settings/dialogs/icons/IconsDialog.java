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

package com.stario.launcher.activities.settings.dialogs.icons;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import com.google.android.material.slider.Slider;
import com.stario.launcher.R;
import com.stario.launcher.apps.IconPackManager;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.dialogs.ActionDialog;
import com.stario.launcher.ui.icons.PathCornerTreatmentAlgorithm;
import com.stario.launcher.ui.recyclers.DividerItemDecorator;

public class IconsDialog extends ActionDialog {
    /**
     * Broadcast Action: Icon corner radius changed by the user.
     * <ul>
     * <li> {@link #EXTRA_CORNER_RADIUS} containing the new radius.
     * </ul>
     */
    public static final String INTENT_CHANGE_CORNER_RADIUS = "com.stario.IconsDialog.CHANGE_CORNER_RADIUS";
    public static final String EXTRA_CORNER_RADIUS = "com.stario.IconsDialog.CORNER_RADIUS";
    /**
     * Broadcast Action: Icon clip path algorithm changed by the user.
     * <ul>
     * <li> {@link #EXTRA_PATH_ALGORITHM} containing the new algorithm.
     * </ul>
     */
    public static final String INTENT_CHANGE_PATH_ALGORITHM = "com.stario.IconsDialog.CHANGE_PATH_ALGORITHM";
    public static final String EXTRA_PATH_ALGORITHM = "com.stario.IconsDialog.PATH_ALGORITHM";
    private final LocalBroadcastManager localBroadcastManager;
    private final SharedPreferences preferences;
    private IconsRecyclerAdapter adapter;

    public IconsDialog(@NonNull ThemedActivity activity) {
        super(activity);

        this.localBroadcastManager = LocalBroadcastManager.getInstance(activity);
        this.preferences = activity.getSharedPreferences(Entry.ICONS);
    }

    @SuppressLint("ClickableViewAccessibility")
    @NonNull
    @Override
    protected View inflateContent(LayoutInflater inflater) {
        View root = inflater.inflate(R.layout.pop_up_icons, null);

        Slider slider = root.findViewById(R.id.slider);
        MaterialButtonToggleGroup algorithm = root.findViewById(R.id.algorithm);

        RecyclerView recycler = root.findViewById(R.id.recycler);
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
                if (event.getAction() == MotionEvent.ACTION_CANCEL ||
                        event.getAction() == MotionEvent.ACTION_UP) {
                    behavior.setDraggable(true);
                } else {
                    behavior.setDraggable(scrolledToTop);
                }

                return false;
            }
        });

        adapter = new IconsRecyclerAdapter(activity, v -> dismiss());

        slider.setValue(preferences.getFloat(IconPackManager.CORNER_RADIUS_ENTRY, 1f));
        slider.addOnChangeListener((slider1, value, fromUser) -> {
            Intent intent = new Intent(INTENT_CHANGE_CORNER_RADIUS);
            intent.putExtra(EXTRA_CORNER_RADIUS, value);

            preferences.edit()
                    .putFloat(IconPackManager.CORNER_RADIUS_ENTRY, value)
                    .apply();

            localBroadcastManager.sendBroadcastSync(intent);
        });

        PathCornerTreatmentAlgorithm currentPathCornerTreatmentAlgorithm = PathCornerTreatmentAlgorithm.fromIdentifier(
                preferences.getInt(IconPackManager.PATH_ALGORITHM_ENTRY, 0)
        );

        if (currentPathCornerTreatmentAlgorithm == PathCornerTreatmentAlgorithm.SQUIRCLE) {
            algorithm.check(R.id.squircle);
        } else {
            algorithm.check(R.id.regular);
        }

        algorithm.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            Intent intent = new Intent(INTENT_CHANGE_PATH_ALGORITHM);

            if (checkedId == R.id.squircle && isChecked) {
                intent.putExtra(EXTRA_PATH_ALGORITHM, PathCornerTreatmentAlgorithm.SQUIRCLE);

                preferences.edit()
                        .putInt(IconPackManager.PATH_ALGORITHM_ENTRY,
                                PathCornerTreatmentAlgorithm.SQUIRCLE.ordinal())
                        .apply();
            } else {
                intent.putExtra(EXTRA_PATH_ALGORITHM, PathCornerTreatmentAlgorithm.REGULAR);

                preferences.edit()
                        .putInt(IconPackManager.PATH_ALGORITHM_ENTRY,
                                PathCornerTreatmentAlgorithm.REGULAR.ordinal())
                        .apply();
            }

            localBroadcastManager.sendBroadcastSync(intent);
        });

        recycler.addItemDecoration(new DividerItemDecorator(getContext(),
                MaterialDividerItemDecoration.VERTICAL));
        recycler.setLayoutManager(new LinearLayoutManager(activity,
                LinearLayoutManager.VERTICAL, false));
        recycler.setAdapter(adapter);

        return root;
    }

    @Override
    public void show() {
        super.show();

        if (adapter != null) {
            adapter.notifyDataSetChanged();
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
