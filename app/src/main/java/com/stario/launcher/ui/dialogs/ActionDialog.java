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

package com.stario.launcher.ui.dialogs;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.stario.launcher.R;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.keyboard.KeyboardHeightProvider;
import com.stario.launcher.ui.utils.UiUtils;
import com.stario.launcher.ui.utils.animation.KeyboardAnimationHelper;
import com.stario.launcher.utils.Utils;

import java.util.Objects;

public abstract class ActionDialog extends BottomSheetDialog {
    protected final ThemedActivity activity;

    private KeyboardHeightProvider heightProvider;
    private boolean canCollapse;
    private View root;

    public ActionDialog(@NonNull ThemedActivity activity) {
        super(activity);

        this.activity = activity;
        this.canCollapse = false;

        activity.addOnConfigurationChangedListener(
                configuration -> {
                    ActionDialog.super.dismiss();

                    if (heightProvider != null) {
                        heightProvider.dismiss();
                    }
                });

        Lifecycle lifecycle = activity.getLifecycle();
        lifecycle.addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                lifecycle.removeObserver(this);
            }

            @Override
            public void onPause(@NonNull LifecycleOwner owner) {
                ActionDialog.super.dismiss();

                if (heightProvider != null) {
                    heightProvider.dismiss();
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getBehavior().setSkipCollapsed(true);
        getBehavior().setPeekHeight(100_000_000);

        LayoutInflater inflater = activity.getLayoutInflater();

        root = inflater.inflate(R.layout.pop_up_root, null, false);
        ViewGroup content = root.findViewById(R.id.content);
        content.addView(inflateContent(activity.getLayoutInflater()));

        heightProvider = new KeyboardHeightProvider(activity);

        if (Utils.isMinimumSDK(Build.VERSION_CODES.R)) {
            KeyboardAnimationHelper.configureKeyboardAnimator(Objects.requireNonNull(getWindow()).getDecorView(),
                    heightProvider, (translation) -> content.setPadding(content.getPaddingLeft(), content.getPaddingTop(),
                            content.getPaddingRight(), (int) (Measurements.getNavHeight() - translation)));

            heightProvider.addKeyboardHeightListener(height -> getBehavior().setDraggable(height == 0));
        } else {
            heightProvider.addKeyboardHeightListener(height -> {
                content.setPadding(content.getPaddingLeft(), content.getPaddingTop(),
                        content.getPaddingRight(), Measurements.getNavHeight() + height);

                getBehavior().setDraggable(height == 0);
            });
        }

        Measurements.addNavListener(value ->
                content.setPadding(content.getPaddingLeft(),
                        content.getPaddingTop(), content.getPaddingRight(), value));

        root.setOnClickListener(view -> dismiss());

        setContentView(root);

        ViewGroup.MarginLayoutParams params = ((ViewGroup.MarginLayoutParams) root.getLayoutParams());
        params.leftMargin = Measurements.dpToPx(10);
        params.rightMargin = Measurements.dpToPx(10);
        ((View) root.getParent()).setBackgroundColor(Color.TRANSPARENT);

        UiUtils.Notch.applyNotchMargin(root, UiUtils.Notch.INVERSE);

        getBehavior().addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED ||
                        newState == BottomSheetBehavior.STATE_HALF_EXPANDED ||
                        newState == BottomSheetBehavior.STATE_DRAGGING) {
                    canCollapse = true;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (slideOffset <= -1 && canCollapse) {
                    ActionDialog.super.dismiss();

                    if (heightProvider != null) {
                        heightProvider.dismiss();
                    }

                    canCollapse = false;
                }

                slideOffset = Math.min(slideOffset, 0);

                Window window = getWindow();

                if (window != null) {
                    root.setScaleX(1 + slideOffset * 0.08f);
                    root.setScaleY(1 + slideOffset * 0.08f);

                    window.setDimAmount((1 + slideOffset) * 0.5f);

                    if (blurBehind() && Utils.isMinimumSDK(Build.VERSION_CODES.S)) {
                        WindowManager.LayoutParams attributes = window.getAttributes();

                        attributes.setBlurBehindRadius(
                                (int) ((1 + slideOffset) * PersistentFullscreenDialog.STEP_COUNT / 2 *
                                        PersistentFullscreenDialog.BLUR_STEP));

                        window.setAttributes(attributes);
                    }
                }
            }
        });
    }

    @Override
    public void onAttachedToWindow() {
        Window window = getWindow();

        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false);

            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            if (blurBehind()) {
                window.setDimAmount(0);

                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            }

            window.setWindowAnimations(R.style.ActionDialogAnimations);

            window.getDecorView().setVisibility(View.INVISIBLE);
            getBehavior().setState(BottomSheetBehavior.STATE_HIDDEN);
        }

        root.post(() -> {
            ViewParent frame = root.getParent();

            if (frame != null) {
                fitToBottomInset((View) frame, false);

                ViewParent coordinator = frame.getParent();

                if (coordinator != null) {
                    fitToBottomInset((View) coordinator, false);

                    ViewParent container = coordinator.getParent();

                    if (container != null) {
                        fitToBottomInset((View) container, false);
                    }
                }
            }

            root.post(() -> {
                if (window != null) {
                    window.getDecorView().setVisibility(View.VISIBLE);

                    getBehavior().setState(getDesiredInitialState());
                }
            });
        });
    }

    // hack to remove STATE_EXPANDED fit to system window jitter on layout pass
    @Override
    public void onDetachedFromWindow() {
        ViewParent frame = root.getParent();

        if (frame != null) {
            fitToBottomInset((View) frame, true);

            ViewParent coordinator = frame.getParent();

            if (coordinator != null) {
                fitToBottomInset((View) coordinator, true);

                ViewParent container = coordinator.getParent();

                if (container != null) {
                    fitToBottomInset((View) container, true);
                }
            }
        }

        super.onDetachedFromWindow();
    }

    private void fitToBottomInset(View view, boolean fit) {
        view.setFitsSystemWindows(fit);
        view.setPadding(view.getPaddingLeft(),
                view.getPaddingTop(), view.getPaddingRight(), 0);
    }

    @Override
    public void show() {
        super.show();

        if (heightProvider != null) {
            heightProvider.start();
        }
    }

    @Override
    public void hide() {
        dismiss();
    }

    @Override
    public void dismiss() {
        BottomSheetBehavior<?> behavior = getBehavior();

        if (heightProvider.getKeyboardHeight() > 0) {
            UiUtils.hideKeyboard(root);
        } else if (canCollapse || behavior.getState() == BottomSheetBehavior.STATE_EXPANDED
                || behavior.getState() == BottomSheetBehavior.STATE_HALF_EXPANDED) {
            canCollapse = true;
            getBehavior().setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    }

    @Override
    protected void onStop() {
        dismiss();

        super.onStop();
    }

    protected abstract @NonNull View inflateContent(LayoutInflater inflater);

    protected abstract int getDesiredInitialState();

    protected abstract boolean blurBehind();
}
