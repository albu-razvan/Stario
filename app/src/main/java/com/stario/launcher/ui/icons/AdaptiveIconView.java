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

package com.stario.launcher.ui.icons;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.stario.launcher.R;
import com.stario.launcher.activities.settings.dialogs.icons.IconsDialog;
import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.apps.ProfileManager;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.utils.Utils;
import com.stario.launcher.utils.objects.ObjectDelegate;

import java.io.Serializable;

public class AdaptiveIconView extends View {
    public static final String CORNER_RADIUS_ENTRY = "com.stario.CORNER_RADIUS";
    public static final float DEFAULT_CORNER_RADIUS = 1f;
    public static final float MAX_SCALE = 1.12f;

    private ObjectDelegate<PathCornerTreatmentAlgorithm> pathAlgorithm;
    private LocalBroadcastManager localBroadcastManager;
    private ColorMatrixColorFilter grayscaleFilter;
    private BroadcastReceiver squircleReceiver;
    private BroadcastReceiver radiusReceiver;
    private BroadcastReceiver pausedReceiver;
    private ObjectDelegate<Drawable> icon;
    private SharedPreferences preferences;
    private ObjectDelegate<Float> radius;
    private boolean applyAlternateBadge;
    private Drawable alternateBadge;
    private boolean sizeRestricted;
    private boolean looseClipping;
    private Boolean grayscale;
    private boolean paused;
    private Path path;

    public AdaptiveIconView(Context context) {
        super(context);

        init(context, null);
    }

    public AdaptiveIconView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init(context, attrs);
    }

    public AdaptiveIconView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        this.preferences = context.getSharedPreferences(Entry.ICONS.toString(), Context.MODE_PRIVATE);
        this.localBroadcastManager = LocalBroadcastManager.getInstance(context);
        this.radiusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                radius.setValue(intent.getFloatExtra(IconsDialog.EXTRA_CORNER_RADIUS, 1f));
            }
        };
        this.squircleReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Serializable serializable = intent.getSerializableExtra(IconsDialog.EXTRA_PATH_ALGORITHM);

                if (serializable instanceof PathCornerTreatmentAlgorithm) {
                    pathAlgorithm.setValue((PathCornerTreatmentAlgorithm) serializable);
                } else {
                    pathAlgorithm.setValue(PathCornerTreatmentAlgorithm.REGULAR);
                }
            }
        };

        if (attrs != null) {
            TypedArray attributes = context.getApplicationContext()
                    .obtainStyledAttributes(attrs, R.styleable.AdaptiveIconView);

            sizeRestricted = attributes.getBoolean(R.styleable.AdaptiveIconView_sizeRestricted, true);
            looseClipping = attributes.getBoolean(R.styleable.AdaptiveIconView_looseClipping, true);

            attributes.recycle();
        } else {
            sizeRestricted = true;
            looseClipping = true;
        }

        this.path = new Path();
        this.icon = new ObjectDelegate<>((o) -> invalidate());
        this.pathAlgorithm = new ObjectDelegate<>(
                PathCornerTreatmentAlgorithm.fromIdentifier(
                        preferences.getInt(PathCornerTreatmentAlgorithm.PATH_ALGORITHM_ENTRY,
                                PathCornerTreatmentAlgorithm.DEFAULT_PATH_ALGORITHM_ENTRY)
                ), (o) -> requestLayout()
        );
        this.radius = new ObjectDelegate<>(
                preferences.getFloat(CORNER_RADIUS_ENTRY,
                        DEFAULT_CORNER_RADIUS), (o) -> requestLayout());
        this.alternateBadge = ResourcesCompat.getDrawable(context.getResources(),
                R.drawable.ic_alternate_badge, context.getTheme());

        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        this.grayscaleFilter = new ColorMatrixColorFilter(matrix);
        this.pausedReceiver = null;
        this.paused = false;

        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        localBroadcastManager.registerReceiver(radiusReceiver,
                new IntentFilter(IconsDialog.INTENT_CHANGE_CORNER_RADIUS));
        localBroadcastManager.registerReceiver(squircleReceiver,
                new IntentFilter(IconsDialog.INTENT_CHANGE_PATH_ALGORITHM));

        PathCornerTreatmentAlgorithm currentPathCornerTreatmentAlgorithm = PathCornerTreatmentAlgorithm
                .fromIdentifier(preferences.getInt(PathCornerTreatmentAlgorithm.PATH_ALGORITHM_ENTRY,
                        PathCornerTreatmentAlgorithm.DEFAULT_PATH_ALGORITHM_ENTRY));
        if (!pathAlgorithm.getValue().equals(currentPathCornerTreatmentAlgorithm)) {
            this.pathAlgorithm.setValue(currentPathCornerTreatmentAlgorithm);
        }

        Float currentRadius = preferences.getFloat(CORNER_RADIUS_ENTRY, DEFAULT_CORNER_RADIUS);
        if (!radius.getValue().equals(currentRadius)) {
            this.radius.setValue(currentRadius);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        localBroadcastManager.unregisterReceiver(radiusReceiver);
        localBroadcastManager.unregisterReceiver(squircleReceiver);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxIconSize = Measurements.getIconSize();
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);

        if (measuredHeight > 0 && measuredWidth > 0) {
            if (measuredWidth != measuredHeight ||
                    (sizeRestricted && measuredHeight > maxIconSize)) {
                int size = Math.min(measuredWidth, measuredHeight);

                if (sizeRestricted) {
                    size = Math.min(maxIconSize, size);
                }

                widthMeasureSpec = heightMeasureSpec =
                        MeasureSpec.makeMeasureSpec(size, MeasureSpec.getMode(widthMeasureSpec));
                measuredWidth = measuredHeight = size;
            }

            Drawable icon = this.icon.getValue();

            if (icon != null) {
                icon.setBounds(0, 0, measuredWidth, measuredHeight);
            }

            updateClipPath(measuredWidth, measuredHeight);

            alternateBadge.setBounds((int) (measuredWidth * 0.6),
                    (int) (measuredHeight * 0.6), measuredWidth, measuredHeight);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void updateClipPath(int width, int height) {
        if (pathAlgorithm.getValue() == PathCornerTreatmentAlgorithm.SQUIRCLE) {
            createClipPathSquircle(width, height);
        } else {
            createClipPathRegular(width, height);
        }
    }

    //Thanks to Olga Nikolskaya https://medium.com/@nikolskayaolia/an-easy-way-to-implement-smooth-shapes-such-as-superellipse-and-squircle-into-a-user-interface-a5ba4e1139ed
    //And the https://copyicon.com/generator/svg-squircle implementation in JavaScript
    //Modified for the context of this project
    private void createClipPathSquircle(int width, int height) {
        float halfWidth = width / 2f;
        float halfHeight = height / 2f;
        float arc = Math.min(halfWidth, halfHeight) * (0.45f - (1f - radius.getValue()) * 0.45f);

        path.reset();
        path.moveTo(0, halfHeight);

        path.cubicTo(0, arc, arc, 0, halfWidth, 0);
        path.cubicTo(width - arc, 0, width, arc, width, halfHeight);
        path.cubicTo(width, height - arc, width - arc, height, halfWidth, height);
        path.cubicTo(arc, height, 0, height - arc, 0, halfHeight);

        path.close();
    }

    private void createClipPathRegular(int width, int height) {
        float cornerRadius = radius.getValue() * width / 2f;

        path.reset();
        path.addRoundRect(0, 0, width, height,
                cornerRadius, cornerRadius, Path.Direction.CW);
        path.close();
    }

    public Drawable getIcon() {
        return icon.getValue();
    }

    public void setApplication(LauncherApplication application) {
        if (application != null) {
            setIcon(application.getIcon());

            paused = !Utils.isProfileAvailable(getContext(), application.getProfile());
            applyAlternateBadge = !Utils.isMainProfile(application.getProfile());

            pausedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    paused = !intent.getBooleanExtra(ProfileManager.PROFILE_AVAILABLE_EXTRA, true);

                    post(() -> invalidate());
                }
            };
            localBroadcastManager.registerReceiver(pausedReceiver,
                    new IntentFilter(ProfileManager.getProfileAvailabilityIntentAction(application.getProfile())));
        } else {
            setIcon(null);
        }
    }

    public void setIcon(Drawable icon) {
        if (pausedReceiver != null) {
            localBroadcastManager.unregisterReceiver(pausedReceiver);
            pausedReceiver = null;
        }

        if (icon != null && icon.getConstantState() != null) {
            Drawable constantStateIcon = icon.getConstantState().newDrawable();

            constantStateIcon.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());

            this.icon.setValue(constantStateIcon);
        } else {
            this.icon.setValue(null);
        }

        paused = false;
        applyAlternateBadge = false;
    }

    @Override
    public void setClipBounds(Rect clipBounds) {
        if (icon.getValue() != null) {
            if (clipBounds != null) {
                icon.getValue().setBounds(clipBounds);

                updateClipPath(clipBounds.width(), clipBounds.height());
            } else {
                icon.getValue().setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());

                updateClipPath(getMeasuredWidth(), getMeasuredHeight());
            }
        }

        super.setClipBounds(clipBounds);

        invalidate();
    }

    public void setLooseClipping(boolean value) {
        this.looseClipping = value;
    }

    public void setSizeRestricted(boolean value) {
        this.sizeRestricted = value;
    }

    /**
     * If set, will override default application paused grayscale state.
     */
    public void setGrayscale(boolean value) {
        this.grayscale = value;

        post(this::invalidate);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (!looseClipping ||
                (icon != null && icon.getValue() instanceof AdaptiveIconDrawable)) {
            int save = canvas.save();

            canvas.clipPath(path);

            super.draw(canvas);
            canvas.restoreToCount(save);
        } else {
            super.draw(canvas);
        }

        if (applyAlternateBadge) {
            alternateBadge.draw(canvas);
        }
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        if (icon.getValue() != null) {
            Drawable icon = this.icon.getValue();

            if (icon instanceof AdaptiveIconDrawable) {
                AdaptiveIconDrawable adaptiveIconDrawable = (AdaptiveIconDrawable) icon;

                Drawable background = adaptiveIconDrawable.getBackground();
                Drawable foreground = adaptiveIconDrawable.getForeground();

                if (background != null) {
                    if (paused || (grayscale != null && grayscale)) {
                        background.setColorFilter(grayscaleFilter);
                    }

                    background.draw(canvas);
                    background.setColorFilter(null);
                }

                if (foreground != null) {
                    if (paused || (grayscale != null && grayscale)) {
                        foreground.setColorFilter(grayscaleFilter);
                    }

                    foreground.draw(canvas);
                    foreground.setColorFilter(null);
                }
            } else {
                if (paused || (grayscale != null && grayscale)) {
                    icon.setColorFilter(grayscaleFilter);
                }

                icon.draw(canvas);
                icon.setColorFilter(null);
            }
        }
    }
}
