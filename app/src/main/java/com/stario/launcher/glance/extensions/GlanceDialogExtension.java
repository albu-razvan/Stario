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

package com.stario.launcher.glance.extensions;

import static com.stario.launcher.ui.dialogs.PersistentFullscreenDialog.BLUR_STEP;
import static com.stario.launcher.ui.dialogs.PersistentFullscreenDialog.STEP_COUNT;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.PathInterpolator;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;

import com.stario.launcher.activities.Launcher;
import com.stario.launcher.glance.Glance;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.common.glance.GlanceConstraintLayout;
import com.stario.launcher.ui.dialogs.PersistentFullscreenDialog;
import com.stario.launcher.ui.utils.HomeWatcher;
import com.stario.launcher.ui.utils.animation.Animation;
import com.stario.launcher.utils.Utils;

public abstract class GlanceDialogExtension extends DialogFragment
        implements GlanceExtension {
    private static final float X1 = 0.2f;
    private static final float Y1 = 1f;
    private static final float X2 = 0.4f;
    private static final float Y2 = 1f;

    protected ThemedActivity activity;

    private PersistentFullscreenDialog dialog;
    private GlanceConstraintLayout container;
    private TransitionListener listener;
    private HomeWatcher homeWatcher;
    private Glance glance;
    private int gravity;

    protected GlanceDialogExtension() {
        super();
    }

    @Nullable
    @Override
    public PersistentFullscreenDialog getDialog() {
        return dialog;
    }

    public void attach(Glance glance, int gravity, TransitionListener listener) {
        this.glance = glance;
        this.gravity = gravity;
        this.listener = listener;

        show(glance.getActivity().getSupportFragmentManager(), getTag());
        glance.attachViewExtension(getViewExtensionPreview(), v -> show());
    }

    @Override
    public void onAttach(@NonNull Context context) {
        if (!(context instanceof ThemedActivity)) {
            throw new RuntimeException("Parent activity is not of type ThemedActivity.");
        }

        activity = (ThemedActivity) context;

        homeWatcher = new HomeWatcher(activity);
        homeWatcher.setOnHomePressedListener(this::hide);
        homeWatcher.startWatch();

        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        homeWatcher.stopWatch();

        super.onDetach();
    }

    @Override
    public void dismiss() {
        hide();
    }

    @Override
    public void onStop() {
        hide(false);

        super.onStop();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            dismissAllowingStateLoss();
        }

        super.onCreate(null);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        dialog = new PersistentFullscreenDialog(activity, getTheme(), true);

        dialog.setOnBackPressed(() -> {
            hide();

            return false;
        });

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ConstraintLayout root = new ConstraintLayout(activity);
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        this.container = inflateExpanded(inflater, root);
        root.addView(this.container);

        root.setOnClickListener(v -> hide());

        return root;
    }

    public void updateLayout(int[] location, int width, int height) {
        ConstraintLayout.LayoutParams params =
                ((ConstraintLayout.LayoutParams) container.getLayoutParams());
        Window window = dialog.getWindow();

        container.setMaxRadius(height / 2f);

        if (window != null) {
            params.width = width;

            if (gravity == Gravity.BOTTOM) {
                params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
                params.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
                params.topToTop = ConstraintLayout.LayoutParams.UNSET;

                params.bottomMargin = dialog.getWindow()
                        .getDecorView().getMeasuredHeight() - location[1] - height;
                params.rightMargin = 0;
                params.leftMargin = location[0];
                params.topMargin = 0;

                container.setPivotY(container.getMeasuredHeight());
            } else if (gravity == Gravity.TOP) {
                params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET;
                params.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
                params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;

                params.bottomMargin = 0;
                params.rightMargin = 0;
                params.leftMargin = location[0];
                params.topMargin = location[1];

                container.setPivotY(0);
            }
        }

        container.setLayoutParams(params);
    }

    protected void show() {
        if (dialog != null && isEnabled() &&
                !dialog.isShowing() && dialog.showDialog()) {
            container.post(new Runnable() {
                @Override
                public void run() {
                    float scale = glance.getHeight() /
                            container.getMeasuredHeight();

                    if (scale >= 0 && !Float.isInfinite(scale)) {
                        container.setScaleY(scale);
                        container.setVisibility(View.VISIBLE);

                        container.post(() -> container.animate()
                                .scaleY(1)
                                .setInterpolator(new PathInterpolator(X1, Y1, X2, Y2))
                                .setDuration(Animation.LONG.getDuration())
                                .setUpdateListener(animation -> {
                                    float fraction = animation.getAnimatedFraction();

                                    updateScalingInternal(fraction);
                                    container.setRadiusPercentage(1f - fraction);
                                })
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationCancel(Animator animation) {
                                        updateScalingInternal(1);
                                        container.setRadiusPercentage(0);
                                    }

                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        updateScalingInternal(1);
                                        container.setRadiusPercentage(0);
                                    }
                                }));
                    } else {
                        container.setVisibility(View.INVISIBLE);
                        container.post(this);
                    }
                }
            });

            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        }
    }

    protected void hide(boolean animate) {
        if (dialog != null && dialog.isShowing()) {
            if (animate) {
                hide();
            } else {
                float scale = glance.getHeight() /
                        container.getMeasuredHeight();

                if (scale >= 0 && !Float.isInfinite(scale)) {
                    container.setScaleY(scale);

                    container.setVisibility(View.INVISIBLE);

                    updateScalingInternal(0);
                }

                container.post(() -> dialog.hide());
            }

            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    protected void hide() {
        if (dialog != null && dialog.isShowing() &&
                container.getScaleY() == 1) {
            float targetScale = glance.getHeight() /
                    container.getMeasuredHeight();

            container.animate()
                    .scaleY(targetScale)
                    .setInterpolator(new PathInterpolator(X1, Y1, X2, Y2))
                    .setDuration(Animation.MEDIUM.getDuration())
                    .setUpdateListener(animation -> {
                        float fraction = 1f - animation.getAnimatedFraction();

                        updateScalingInternal(fraction);
                        container.setRadiusPercentage(1f - fraction);
                    }).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationCancel(Animator animation) {
                            container.setScaleY(targetScale);

                            container.setVisibility(View.INVISIBLE);

                            container.post(() -> dialog.hide());
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            container.setVisibility(View.INVISIBLE);

                            container.post(() -> dialog.hide());
                        }
                    });

            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    private void updateScalingInternal(@FloatRange(from = 0f, to = 1f) float fraction) {
        float scale = 1f / container.getScaleY();

        if (!Float.isNaN(scale) && scale != Float.POSITIVE_INFINITY) {
            updateScaling(fraction, scale);
        }

        Window window = dialog.getWindow();

        if (window != null) {
            int step = (int) (STEP_COUNT * fraction);

            View decor = activity.getWindow().getDecorView();

            Drawable background = decor.getBackground();
            background.setAlpha((int) (fraction * Launcher.MAX_BACKGROUND_ALPHA));

            if (Utils.isMinimumSDK(Build.VERSION_CODES.S)) {
                decor.post(() -> {
                    window.setBackgroundBlurRadius((int) (step * BLUR_STEP));
                });
            }
        }

        if (listener != null) {
            listener.onProgressFraction(fraction);
        }

        container.invalidate();
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

    public interface TransitionListener {
        void onProgressFraction(float fraction);
    }

    abstract public String getTAG();

    abstract protected GlanceConstraintLayout inflateExpanded(LayoutInflater inflater,
                                                              ConstraintLayout container);

    abstract protected GlanceViewExtension getViewExtensionPreview();

    abstract protected boolean isEnabled();

    abstract protected void updateScaling(@FloatRange(from = 0f, to = 1f) float fraction, float scale);
}