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

import static com.stario.launcher.ui.dialogs.PersistentFullscreenDialog.BLUR_STEP;
import static com.stario.launcher.ui.dialogs.PersistentFullscreenDialog.STEP_COUNT;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.activities.Launcher;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.sheet.behavior.SheetBehavior;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.utils.HomeWatcher;
import com.stario.launcher.ui.utils.UiUtils;
import com.stario.launcher.utils.Utils;

import java.util.ArrayList;

public abstract class SheetDialogFragment extends DialogFragment {
    private final ArrayList<DialogInterface.OnShowListener> showListeners;
    private final ArrayList<OnDestroyListener> destroyListeners;
    private OnSlideListener slideListener;
    private boolean receivedMoveEvent;
    private HomeWatcher homeWatcher;
    private boolean capturing;
    private SheetType type;

    protected ThemedActivity activity;
    protected SheetDialog dialog;

    public SheetDialogFragment() {
        showListeners = new ArrayList<>();
        destroyListeners = new ArrayList<>();
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

        Window window = dialog.getWindow();
        if (window != null) {
            UiUtils.enforceLightSystemUI(window);

            Drawable background = new ColorDrawable(
                    activity.getAttributeData(com.google.android.material.R.attr.colorSurface, false)
            );
            background.setAlpha(0);

            window.setBackgroundDrawable(background);
        }

        dialog.setOnShowListener(dialogInterface -> {
            dialog.behavior.setState(SheetBehavior.STATE_COLLAPSED);
            dialog.behavior.addSheetCallback(new SheetBehavior.SheetCallback() {
                private int lastBlurStep = -1;
                private float lastSlideOffset = -1;
                boolean wasCollapsed = true;

                @Override
                public void onStateChanged(@NonNull View view, int newState) {
                    if (newState == SheetBehavior.STATE_COLLAPSED) {
                        wasCollapsed = true;
                    }
                }

                @Override
                public void onSlide(@NonNull View sheet, float slideOffset) {
                    if (slideOffset == lastSlideOffset) {
                        return;
                    }

                    lastSlideOffset = slideOffset;

                    if (window != null) {
                        double offsetSemi = Utils.getGenericInterpolatedValue(slideOffset);

                        Drawable background = window.getDecorView().getBackground();
                        background.setAlpha((int) (Launcher.MAX_BACKGROUND_ALPHA * offsetSemi));

                        // only STEP_COUNT states for performance
                        int step = (int) (STEP_COUNT * offsetSemi);

                        if (Utils.isMinimumSDK(Build.VERSION_CODES.S) && lastBlurStep != step) {
                            window.setBackgroundBlurRadius((int) (step * BLUR_STEP));

                            this.lastBlurStep = step;
                        }
                    }

                    if (slideOffset >= 0.5f) {
                        if (wasCollapsed) {
                            Vibrations.getInstance().vibrate();
                        }

                        wasCollapsed = false;
                    }

                    float alpha = slideOffset * 2 - 1f;

                    if (alpha > 0) {
                        sheet.setAlpha(alpha);
                        sheet.setVisibility(View.VISIBLE);
                    } else {
                        sheet.setVisibility(View.INVISIBLE);
                    }

                    if (slideListener != null) {
                        slideListener.onSlide(slideOffset);
                    }
                }
            });

            for (DialogInterface.OnShowListener listener : showListeners) {
                listener.onShow(dialog);
            }

            if (!capturing && receivedMoveEvent) {
                dialog.behavior.setState(SheetBehavior.STATE_EXPANDED, true);
            }

            showListeners.clear();
        });

        UiUtils.applyNotchMargin(dialog.getContainer());

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

    public void sendMotionEvent(MotionEvent event) {
        dialog.captureMotionEvent(event);

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            receivedMoveEvent = false;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            receivedMoveEvent = true;
        }

        capturing = event.getAction() != MotionEvent.ACTION_UP &&
                event.getAction() != MotionEvent.ACTION_CANCEL;
    }

    public void show() {
        if (dialog != null) {
            dialog.showDialog();
        }
    }

    public void requestIgnoreCurrentTouchEvent(boolean enabled) {
        if (dialog != null) {
            dialog.requestIgnoreCurrentTouchEvent(enabled);
        }
    }

    protected void addOnShowListener(DialogInterface.OnShowListener listener) {
        if (listener != null) {
            this.showListeners.add(listener);
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

    public void setOnSlideListener(OnSlideListener listener) {
        this.slideListener = listener;
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

    public interface OnSlideListener {
        void onSlide(float slideOffset);
    }
}
