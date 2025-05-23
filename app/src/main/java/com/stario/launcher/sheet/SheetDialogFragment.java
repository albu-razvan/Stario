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

package com.stario.launcher.sheet;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.sheet.behavior.SheetBehavior;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.utils.HomeWatcher;
import com.stario.launcher.ui.utils.UiUtils;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class SheetDialogFragment extends DialogFragment {
    private final ArrayList<OnDestroyListener> destroyListeners;
    private final ArrayList<OnShowListener> onShowListeners;
    private SheetDialog.OnSlideListener slideListener;
    private ThemedActivity activity;
    private HomeWatcher homeWatcher;
    private int receivedDragEvents;
    private SheetDialog dialog;
    private SheetType type;

    public SheetDialogFragment() {
        this.slideListener = null;
        this.receivedDragEvents = 0;
        this.onShowListeners = new ArrayList<>();
        this.destroyListeners = new ArrayList<>();
    }

    protected SheetDialogFragment(@NonNull SheetType type) {
        this();

        this.type = type;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        if (!(context instanceof ThemedActivity)) {
            throw new RuntimeException("Parent activity is not of type ThemedActivity.");
        }

        activity = (ThemedActivity) context;

        homeWatcher = new HomeWatcher(context);
        homeWatcher.setOnHomePressedListener(() -> {
            SheetBehavior<?> behavior = getBehavior();

            if (behavior != null) {
                behavior.setState(SheetBehavior.STATE_COLLAPSED);
            }
        });
        homeWatcher.startWatch();

        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        homeWatcher.stopWatch();

        super.onDetach();
    }

    @Override
    public void onDestroy() {
        for (OnDestroyListener listener : destroyListeners) {
            listener.onDestroy(type);
        }

        super.onDestroy();
    }

    @Nullable
    public SheetDialog getSheetDialog() {
        return dialog;
    }

    @Override
    public void onStop() {
        SheetBehavior<?> behavior = getBehavior();

        if (behavior != null) {
            behavior.setState(SheetBehavior.STATE_COLLAPSED, false);
        }

        dialog.hide();

        super.onStop();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            dismissAllowingStateLoss();
        } else if (type == null) {
            throw new RuntimeException("SheetDialogFragment type cannot be null.");
        }

        super.onCreate(null);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        dialog = SheetDialogFactory.forType(type, activity, getTheme());

        if (slideListener != null) {
            dialog.setOnSlideListener(slideListener);

            slideListener = null;
        }

        dialog.setOnShowListener(dialogInterface -> {
            AtomicInteger state = new AtomicInteger(SheetBehavior.STATE_COLLAPSED);

            dialog.behavior.addSheetCallback(new SheetBehavior.SheetCallback() {
                @Override
                public void onStateChanged(@NonNull View sheet, int newState) {
                    state.set(newState);

                    if (newState == SheetBehavior.STATE_DRAGGING &&
                            receivedDragEvents < Integer.MAX_VALUE) {
                        receivedDragEvents++;
                    }
                }
            });

            if (receivedDragEvents == 0 && state.get() == SheetBehavior.STATE_COLLAPSED) {
                dialog.behavior.setState(SheetBehavior.STATE_EXPANDED);
            }

            for (OnShowListener listener : onShowListeners) {
                listener.onShow();
            }

            onShowListeners.clear();
        });

        UiUtils.Notch.applyNotchMargin(dialog.getContainer(), UiUtils.Notch.CENTER);

        return dialog;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        ViewGroup container = dialog.getContainer();

        container.requestLayout();
        snapScrollingViews(container);
    }

    private void snapScrollingViews(View view) {
        if (view instanceof RecyclerView) {
            RecyclerView recycler = (RecyclerView) view;

            if (recycler.computeVerticalScrollOffset() == 0) {
                recycler.post(() -> recycler.scrollBy(0, -recycler.computeVerticalScrollOffset()));
            }

            if (recycler.computeHorizontalScrollOffset() == 0) {
                recycler.post(() -> recycler.scrollBy(-recycler.computeHorizontalScrollOffset(), 0));
            }
        } else if (view instanceof ScrollView) {
            ScrollView scroller = (ScrollView) view;

            if (scroller.getScrollY() == 0) {
                scroller.post(() -> scroller.scrollBy(0, -scroller.getScrollY()));
            }

            if (scroller.getScrollX() == 0) {
                scroller.post(() -> scroller.scrollBy(-scroller.getScrollX(), 0));
            }
        } else if (view instanceof NestedScrollView) {
            NestedScrollView scroller = (NestedScrollView) view;

            if (scroller.getScrollY() == 0) {
                scroller.post(() -> scroller.scrollBy(0, -scroller.getScrollY()));
            }

            if (scroller.getScrollX() == 0) {
                scroller.post(() -> scroller.scrollBy(-scroller.getScrollX(), 0));
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;

            for (int index = 0; index < group.getChildCount(); index++) {
                View target = group.getChildAt(index);

                snapScrollingViews(target);
            }
        }
    }

    public SheetType getType() {
        return type;
    }

    @Nullable
    public SheetBehavior<?> getBehavior() {
        return dialog != null ? dialog.behavior : null;
    }

    public boolean onMotionEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (receivedDragEvents < Integer.MAX_VALUE) {
                receivedDragEvents++;
            }
        }

        return dialog.onMotionEvent(event);
    }

    public void show() {
        if (dialog != null) {
            dialog.showDialog();
        }
    }

    /**
     * Add a one time show listener
     */
    protected void addOnShowListener(OnShowListener listener) {
        if (listener != null) {
            this.onShowListeners.add(listener);
        }
    }

    public void addOnDestroyListener(OnDestroyListener listener) {
        if (listener != null) {
            this.destroyListeners.add(listener);
        }
    }

    protected void setOnBackPressed(SheetDialog.OnBackPressed listener) {
        dialog.setOnBackPressed(listener);
    }

    public void setOnSlideListener(SheetDialog.OnSlideListener listener) {
        if (dialog != null) {
            dialog.setOnSlideListener(listener);
        } else {
            slideListener = listener;
        }
    }

    public void updateSheetSystemUI(boolean lightMode) {
        if (dialog == null) {
            return;
        }

        Window window = dialog.getWindow();

        if (window != null) {
            View decor = window.getDecorView();

            if (lightMode) {
                decor.setSystemUiVisibility(decor.getSystemUiVisibility()
                        & ~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR));
            } else {
                decor.setSystemUiVisibility(decor.getSystemUiVisibility()
                        | (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR));
            }
        }
    }

    public interface OnDestroyListener {
        void onDestroy(SheetType type);
    }

    public interface OnShowListener {
        void onShow();
    }
}
