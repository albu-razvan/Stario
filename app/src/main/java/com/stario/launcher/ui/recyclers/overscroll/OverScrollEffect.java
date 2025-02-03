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

package com.stario.launcher.ui.recyclers.overscroll;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.view.View;
import android.widget.EdgeEffect;

import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.stario.launcher.ui.Measurements;
import com.stario.launcher.utils.objects.ObjectDelegate;

public class OverScrollEffect<V extends View & OverScroll> extends EdgeEffect {
    private static final float VELOCITY_MULTIPLIER = 0.8f;
    private static final float SPRING_FACTOR_MULTIPLIER = 1000f;
    private static final float SPRING_STIFFNESS = 500f;
    private static final float SCALE_MULTIPLIER = 0.05f;
    private static final float SCALE_ALPHA = 0.3f;
    private static final byte PIVOT_TOP = 0b01;
    private static final byte PIVOT_BOTTOM = 0b10;
    private static final byte STATE_IDLE = 0b0;
    private static final byte STATE_ACTIVE = 0b1;

    private final ObjectDelegate<Float> factor;
    private final SpringAnimation animation;
    private final Rect bounds;

    private boolean isCanvasCaptured;
    private int pivot;
    private byte state;

    public OverScrollEffect(V view) {
        super(view.getContext());

        this.factor = new ObjectDelegate<>(0f, (o) -> view.invalidate());
        this.bounds = new Rect();
        this.state = STATE_IDLE;
        this.isCanvasCaptured = false;

        SpringForce spring = new SpringForce();
        spring.setFinalPosition(0f);
        spring.setStiffness(SPRING_STIFFNESS);
        spring.setDampingRatio(1f);

        animation = new SpringAnimation(new Object(), new FloatPropertyCompat<>("") {
            @Override
            public float getValue(Object object) {
                return factor.getValue() * SPRING_FACTOR_MULTIPLIER;
            }

            @Override
            public void setValue(Object object, float value) {
                OverScrollEffect.this.factor.setValue(value / SPRING_FACTOR_MULTIPLIER);
            }
        });
        animation.setMaxValue(SPRING_FACTOR_MULTIPLIER * 1.5f);
        animation.setSpring(spring);
        animation.addEndListener((animation, canceled, value, velocity) -> {
            if (!canceled) {
                view.postOnAnimation(() -> state = STATE_IDLE);
            }
        });

        view.addOverScrollContract(canvas -> {
            if (state == STATE_IDLE) {
                isCanvasCaptured = false;
                return false;
            }

            if (!view.canScrollVertically(1)) {
                if (view.canScrollVertically(-1)) {
                    pivot = PIVOT_BOTTOM;
                }
            } else if (!view.canScrollVertically(-1)) {
                pivot = PIVOT_TOP;
            }

            int maxTranslation = Math.min(
                    Measurements.dpToPx(Measurements.HEADER_SIZE_DP / 4f),
                    bounds.height() / 4
            );

            if (pivot == PIVOT_BOTTOM) {
                canvas.translate(0, factor.getValue() * -maxTranslation);
                canvas.scale(1, 1 + factor.getValue() * SCALE_MULTIPLIER,
                        bounds.centerX(), bounds.height());
            } else if (pivot == PIVOT_TOP) {
                canvas.translate(0, factor.getValue() * maxTranslation);
                canvas.scale(1, 1 + factor.getValue() * SCALE_MULTIPLIER,
                        bounds.centerX(), 0);
            } else {
                isCanvasCaptured = false;
                return false;
            }

            isCanvasCaptured = true;
            return true;
        });
    }

    @Override
    public void setSize(int width, int height) {
        bounds.set(0, 0, width, height);
    }

    @Override
    public boolean isFinished() {
        return state == STATE_IDLE;
    }

    @Override
    public void finish() {
        animation.cancel();
        state = STATE_IDLE;
    }

    @Override
    public float getDistance() {
        return factor.getValue();
    }

    @Override
    public float onPullDistance(float deltaDistance, float displacement) {
        float delta = Math.max(0f, deltaDistance + factor.getValue()) - factor.getValue();

        onPull(delta);

        return delta;
    }

    @Override
    public void onPull(float deltaDistance) {
        state = STATE_ACTIVE;

        if (animation.isRunning()) {
            animation.cancel();
        }

        factor.setValue(factor.getValue() + deltaDistance * (1f - factor.getValue()));
    }

    @Override
    public void onRelease() {
        if (!animation.isRunning()) {
            if (factor.getValue() == 0) {
                state = STATE_IDLE;
            } else {
                state = STATE_ACTIVE;
                animation.setStartVelocity(0)
                        .setStartValue(factor.getValue() * SPRING_FACTOR_MULTIPLIER)
                        .start();
            }
        }
    }

    @Override
    public void onAbsorb(int velocity) {
        state = STATE_ACTIVE;

        if (animation.isRunning()) {
            animation.cancel();
        }

        animation.setStartVelocity((1f - factor.getValue()) * velocity * VELOCITY_MULTIPLIER)
                .setStartValue(factor.getValue() * SPRING_FACTOR_MULTIPLIER)
                .start();
    }

    @Override
    public boolean draw(Canvas canvas) {
        if (isCanvasCaptured) {
            canvas.drawColor(
                    Color.argb(
                            1f - factor.getValue() * SCALE_ALPHA, 0f, 0f, 0f
                    ), PorterDuff.Mode.DST_IN
            );
        }

        return isCanvasCaptured && !isFinished();
    }
}