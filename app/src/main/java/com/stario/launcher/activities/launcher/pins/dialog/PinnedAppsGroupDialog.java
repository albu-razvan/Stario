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

package com.stario.launcher.activities.launcher.pins.dialog;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.RelativeLayout;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.apps.Category;
import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.dialogs.PersistentFullscreenDialog;
import com.stario.launcher.ui.utils.HomeWatcher;
import com.stario.launcher.ui.utils.UiUtils;
import com.stario.launcher.ui.utils.animation.Animation;

public class PinnedAppsGroupDialog extends PersistentFullscreenDialog {
    private static final float CENTER_PIVOT_WEIGHT = 0.2f;
    private static final float SCALE_FACTOR = 0.75f;
    private static final float ITEM_SIZE_DP = 90;

    private final View.OnLayoutChangeListener sourceLayoutChangeListener;
    private final Category.CategoryItemListener categoryChangeListener;
    private final PinnedAppsGroupDialogRecyclerAdapter adapter;
    private final TransitionListener listener;
    private final GridLayoutManager manager;
    private final ThemedActivity activity;
    private final HomeWatcher homeWatcher;

    private RelativeLayout recyclerContainer;
    private RelativeLayout container;
    private boolean allowDismissal;
    private RecyclerView recycler;
    private Category category;
    private View source;
    private int skip;

    public PinnedAppsGroupDialog(@NonNull ThemedActivity activity, TransitionListener listener) {
        super(activity, activity.getThemeResourceId(), true);

        this.activity = activity;
        this.listener = listener;

        this.homeWatcher = new HomeWatcher(activity);

        this.manager = new GridLayoutManager(activity, 1);
        this.adapter = new PinnedAppsGroupDialogRecyclerAdapter(activity);

        this.categoryChangeListener = new Category.CategoryItemListener() {
            private void update() {
                if (recycler != null) {
                    recycler.post(() -> {
                        if (isShowing()) {
                            adapter.updateDataSnapshot(category, skip);
                            manager.setSpanCount(invalidateRecycler());
                        }
                    });
                }
            }

            @Override
            public void onInserted(LauncherApplication application) {
                update();
            }

            @Override
            public void onRemoved(LauncherApplication application) {
                update();
            }

            @Override
            public void onUpdated(LauncherApplication application) {
                update();
            }

            @Override
            public void onSwapped(int index1, int index2) {
                update();
            }
        };
        this.sourceLayoutChangeListener = (view, i, i1, i2,
                                           i3, i4, i5, i6, i7) -> {
            updateRecyclerPositionInContainer(view);
        };

        this.allowDismissal = false;
        this.category = null;
        this.skip = 0;

        Lifecycle lifecycle = activity.getLifecycle();
        lifecycle.addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onStop(@NonNull LifecycleOwner owner) {
                dismiss(false);
            }

            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                lifecycle.removeObserver(this);
            }
        });

        homeWatcher.setOnHomePressedListener(this::dismiss);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.pinned_apps_dialog);

        container = findViewById(R.id.container);
        if (container != null) {
            UiUtils.Notch.applyNotchMargin(container, UiUtils.Notch.Treatment.CENTER);
            container.setOnClickListener((v) -> dismiss());
            Measurements.addNavListener(value -> {
                ViewGroup.MarginLayoutParams params =
                        (ViewGroup.MarginLayoutParams) container.getLayoutParams();

                params.bottomMargin = value;
                container.requestLayout();
            });
            Measurements.addStatusBarListener(value -> {
                ViewGroup.MarginLayoutParams params =
                        (ViewGroup.MarginLayoutParams) container.getLayoutParams();

                params.topMargin = value;
                container.requestLayout();
            });

            recyclerContainer = container.findViewById(R.id.recycler_container);
            recycler = container.findViewById(R.id.recycler);

            recyclerContainer.setClipToOutline(true);

            // this will always update the approximation when we show the dialog,
            // no need for another call
            adapter.setRecyclerHeightApproximationListener(
                    height -> invalidateRecycler());

            recycler.setLayoutManager(manager);
            recycler.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            recycler.getViewTreeObserver()
                                    .removeOnGlobalLayoutListener(this);
                            recycler.setAdapter(adapter);
                        }
                    });
        } else {
            dismiss();
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        homeWatcher.startWatch();
    }

    @Override
    public void onDetachedFromWindow() {
        homeWatcher.stopWatch();

        super.onDetachedFromWindow();
    }

    public void setCategory(Category category) {
        if (this.category != null) {
            this.category.removeCategoryItemListener(categoryChangeListener);
        }

        this.category = category;

        if (this.category != null) {
            this.category.addCategoryItemListener(categoryChangeListener);
        }

        if (isShowing()) {
            adapter.updateDataSnapshot(category, skip);
            manager.setSpanCount(invalidateRecycler());
        }
    }

    /**
     * Invalidates the recycler layout and calculates the span count.
     *
     * @return layout manager span count
     */
    private int invalidateRecycler() {
        int size = getItemCount();

        if (size < 3) {
            size = Math.max(1, size);
        } else if (size < 5) {
            size = 2;
        } else {
            size = 3;
        }

        if (recycler != null) {
            int recyclerWidth = size * Measurements.dpToPx(ITEM_SIZE_DP) +
                    recycler.getPaddingLeft() + recycler.getPaddingRight();
            int recyclerHeight = Math.min(
                    Measurements.dpToPx(Measurements.HEADER_SIZE_DP),
                    adapter.approximateRecyclerHeight()
            ) + recycler.getPaddingBottom() + recycler.getPaddingTop();

            ViewGroup.LayoutParams params = recycler.getLayoutParams();
            params.width = recyclerWidth;
            params.height = recyclerHeight;

            updateRecyclerPositionInContainer(source);
        }

        return size;
    }

    private int getItemCount() {
        return category != null ? Math.max(0, category.getSize() - skip) : 0;
    }

    private void updateRecyclerPositionInContainer(View view) {
        if (recyclerContainer == null || recycler == null
                || container == null || view == null) {
            return;
        }

        int[] sourceLoc = new int[2];
        int[] containerLoc = new int[2];
        view.getLocationOnScreen(sourceLoc);
        container.getLocationOnScreen(containerLoc);

        int relativeSourceX = sourceLoc[0] - containerLoc[0];
        int relativeSourceY = sourceLoc[1] - containerLoc[1];

        int sourceCenterX = relativeSourceX + view.getWidth() / 2;
        int sourceCenterY = relativeSourceY + view.getHeight() / 2;

        int containerWidth = container.getWidth() > 0 ? container.getWidth() : Measurements.getWidth();
        int containerHeight = container.getHeight() > 0 ? container.getHeight() : Measurements.getHeight();

        int recyclerWidth = recycler.getLayoutParams().width;
        int recyclerHeight = recycler.getLayoutParams().height;

        int targetX = (int) (sourceCenterX * (1 - CENTER_PIVOT_WEIGHT)
                + containerWidth / 2f * CENTER_PIVOT_WEIGHT);
        int targetY = (int) (sourceCenterY * (1 - CENTER_PIVOT_WEIGHT)
                + containerHeight / 2f * CENTER_PIVOT_WEIGHT);

        int finalLeft = targetX - recyclerWidth / 2;
        int finalTop = targetY - recyclerHeight / 2;

        int padding = Measurements.getDefaultPadding();
        finalLeft = Math.max(padding, Math.min(finalLeft, containerWidth - recyclerWidth - padding));
        finalTop = Math.max(padding, Math.min(finalTop, containerHeight - recyclerHeight - padding));

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) recyclerContainer.getLayoutParams();
        if (params.leftMargin != finalLeft || params.topMargin != finalTop) {
            params.leftMargin = finalLeft;
            params.topMargin = finalTop;

            recyclerContainer.setPivotX(
                    Math.max(0, Math.min(sourceCenterX - finalLeft, recyclerWidth))
            );
            recyclerContainer.setPivotY(
                    Math.max(0, Math.min(sourceCenterY - finalTop, recyclerHeight))
            );
            recyclerContainer.setLayoutParams(params);
        }
    }

    public void show(@IntRange(from = 0) int skip, View source) {
        if (isShowing() || category == null || category.getSize() <= skip) {
            return;
        }

        this.skip = Math.max(0, skip);
        adapter.updateDataSnapshot(category, skip);
        manager.setSpanCount(invalidateRecycler());

        this.source = source;
        if (source != null) {
            source.addOnLayoutChangeListener(sourceLayoutChangeListener);
            sourceLayoutChangeListener.onLayoutChange(source,
                    0, 0, 0, 0, 0, 0, 0, 0);
        }

        allowDismissal = true;
        super.showDialog();

        Window window = getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }

        UiUtils.post(() -> recyclerContainer.animate()
                .scaleX(1)
                .scaleY(1)
                .alpha(1)
                .setDuration(Animation.MEDIUM.getDuration())
                .setUpdateListener(valueAnimator ->
                        setDimmingFactor(valueAnimator.getAnimatedFraction()))
                .setInterpolator(new DecelerateInterpolator(2.5f)));
    }

    @Override
    public void dismiss() {
        dismiss(true);
    }

    public void dismiss(boolean animate) {
        if (!allowDismissal) {
            return;
        }

        Window window = getWindow();
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

            allowDismissal = false;
            if (animate) {
                recyclerContainer.animate()
                        .scaleX(SCALE_FACTOR)
                        .scaleY(SCALE_FACTOR)
                        .alpha(0)
                        .setDuration(Animation.MEDIUM.getDuration())
                        .setInterpolator(new AccelerateInterpolator(2))
                        .setUpdateListener(valueAnimator ->
                                setDimmingFactor(1f - valueAnimator.getAnimatedFraction()))
                        .withEndAction(() -> {
                            if (!allowDismissal) {
                                window.getDecorView().post(() -> {
                                    if (!activity.isDestroyed()) {
                                        super.dismiss();
                                    }
                                });

                                if (source != null) {
                                    source.removeOnLayoutChangeListener(sourceLayoutChangeListener);
                                }
                            }
                        });
            } else {
                recyclerContainer.setScaleX(SCALE_FACTOR);
                recyclerContainer.setScaleY(SCALE_FACTOR);
                recyclerContainer.setAlpha(0);

                setDimmingFactor(0);

                if (!activity.isDestroyed()) {
                    super.dismiss();
                }

                if (source != null) {
                    source.removeOnLayoutChangeListener(sourceLayoutChangeListener);
                }
            }
        }
    }

    @Override
    public void setDimmingFactor(float factor) {
        super.setDimmingFactor(factor);

        if (listener != null) {
            listener.onProgressFraction(factor);
        }
    }

    public interface TransitionListener {
        void onProgressFraction(float factor);
    }
}