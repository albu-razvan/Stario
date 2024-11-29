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

package com.stario.launcher.glance.extensions;

import static com.stario.launcher.ui.dialogs.FullscreenDialog.BLUR_STEP;
import static com.stario.launcher.ui.dialogs.FullscreenDialog.STEP_COUNT;

import android.annotation.SuppressLint;
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
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.stario.launcher.activities.Launcher;
import com.stario.launcher.glance.Glance;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.dialogs.FullscreenDialog;
import com.stario.launcher.ui.glance.GlanceConstraintLayout;
import com.stario.launcher.utils.Utils;
import com.stario.launcher.utils.animation.Animation;

public abstract class GlanceDialogExtension extends AppCompatDialogFragment
        implements GlanceExtension {
    private static final float X1 = 0.2f;
    private static final float Y1 = 1f;
    private static final float X2 = 0.7f;
    private static final float Y2 = 1f;
    protected ThemedActivity activity;
    private GlanceViewExtension preview;
    private FullscreenDialog dialog;
    private Glance glance;
    private GlanceConstraintLayout container;
    private int gravity;

    protected GlanceDialogExtension() {
    }

    @Nullable
    @Override
    public FullscreenDialog getDialog() {
        return dialog;
    }

    public void attach(Glance glance, int gravity) {
        this.glance = glance;
        this.gravity = gravity;

        show(glance.getActivity().getSupportFragmentManager(), getTag());
    }

    @Override
    public void onAttach(@NonNull Context context) {
        if (!(context instanceof ThemedActivity)) {
            throw new RuntimeException("Parent activity is not of type ThemedActivity.");
        }

        activity = (ThemedActivity) context;

        super.onAttach(context);
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
        setRetainInstance(true);

        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            dismissAllowingStateLoss();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        dialog = new FullscreenDialog(activity, getTheme(), true);

        dialog.setOnBackPressed(() -> {
            hide();

            return false;
        });

        return dialog;
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
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

        preview = glance.attachViewExtension(getPreviewType(), v -> show());

        return root;
    }

    protected void sendDataToPreview(Bundle data) {
        if (preview != null) {
            preview.updateData(data);
        }
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
                !dialog.isShowing()) {

            container.post(new Runnable() {
                @Override
                public void run() {
                    float scale = glance.getHeight() /
                            container.getMeasuredHeight();

                    if (scale >= 0 && !Float.isInfinite(scale)) {
                        container.setScaleY(scale);
                        updateScalingInternal(scale);

                        container.setVisibility(View.VISIBLE);

                        container.animate()
                                .scaleY(1)
                                .setInterpolator(new PathInterpolator(X1, Y1, X2, Y2))
                                .setDuration(Animation.MEDIUM.getDuration())
                                .setUpdateListener(animation -> {
                                    float fraction = animation.getAnimatedFraction();

                                    updateScalingInternal(fraction);
                                    container.setRadiusPercentage(1f - fraction);
                                })
                                .withEndAction(() -> {
                                    updateScalingInternal(1);
                                    container.setRadiusPercentage(0);
                                });

                        container.post(() -> glance.hide());
                    } else {
                        container.setVisibility(View.INVISIBLE);

                        container.post(this);
                    }
                }
            });

            dialog.showDialog();
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        }
    }

    protected void hide(boolean animate) {
        if (dialog != null && dialog.isShowing()) {
            if (animate) {
                hide();
            } else {
                glance.show(Animation.NONE);

                glance.post(() -> {
                    float scale = glance.getHeight() /
                            container.getMeasuredHeight();

                    if (scale >= 0 && !Float.isInfinite(scale)) {
                        container.setScaleY(scale);
                        updateScalingInternal(0);
                    }

                    dialog.hide();
                });
            }

            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    protected void hide() {
        if (dialog != null && dialog.isShowing() &&
                container.getScaleY() == 1) {
            container.animate()
                    .scaleY(glance.getHeight() /
                            container.getMeasuredHeight())
                    .setInterpolator(new PathInterpolator(X1, Y1, X2, Y2))
                    .setDuration(Animation.MEDIUM.getDuration())
                    .setUpdateListener(animation -> {
                        float fraction = 1f - animation.getAnimatedFraction();

                        updateScalingInternal(fraction);
                        container.setRadiusPercentage(1f - fraction);
                    }).withEndAction(() -> {
                        glance.show(Animation.SHORT);

                        glance.post(() -> dialog.hide());
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

        container.invalidate();
    }

    abstract public String getTAG();

    abstract protected GlanceConstraintLayout inflateExpanded(LayoutInflater inflater,
                                                              ConstraintLayout container);

    abstract protected GlanceViewExtensionType getPreviewType();

    abstract protected boolean isEnabled();

    abstract protected void updateScaling(@FloatRange(from = 0f, to = 1f) float fraction,
                                          float scale);
}