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
import android.view.MotionEvent;
import android.view.View;
import android.widget.EdgeEffect;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.stario.launcher.ui.Measurements;
import com.stario.launcher.utils.objects.ObjectDelegate;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

public class OverScrollEffect<V extends View & OverScroll> extends EdgeEffect {
    public static final int PULL_EDGE_TOP = 0b01;
    public static final int PULL_EDGE_BOTTOM = 0b10;

    private static final float VELOCITY_MULTIPLIER = 0.8f;
    private static final float SPRING_FACTOR_MULTIPLIER = 1000f;
    private static final float SPRING_STIFFNESS = 500f;
    private static final float SCALE_MULTIPLIER = 0.05f;
    private static final float SCALE_ALPHA = 0.3f;
    private static final byte PIVOT_UNSPECIFIED = 0b00;
    private static final byte PIVOT_TOP = 0b01;
    private static final byte PIVOT_BOTTOM = 0b10;

    @IntDef(flag = true, value = {
            PULL_EDGE_TOP,
            PULL_EDGE_BOTTOM,
    })

    @Retention(RetentionPolicy.SOURCE)
    public @interface Edge {
    }

    private final ArrayList<OnOverScrollListener> overScrollListeners;
    private final ObjectDelegate<Float> factor;
    private final SpringAnimation animation;
    private final Rect bounds;
    private final V view;

    private boolean isCanvasCaptured;
    private OverScrollState oldState;
    private OverScrollState state;
    private float initialTouchY;
    @Edge
    private int edges;
    private int pivot;

    public OverScrollEffect(@NonNull V view, int edges) {
        super(view.getContext());

        this.view = view;
        this.initialTouchY = -1;
        this.overScrollListeners = new ArrayList<>();
        this.factor = new ObjectDelegate<>(0f, (value) -> {
            notifyOverScrolled(value);
            view.invalidate();
        });
        this.bounds = new Rect();
        this.oldState = null;
        this.state = OverScrollState.IDLE;
        this.pivot = PIVOT_UNSPECIFIED;
        this.edges = edges;
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
        animation.setSpring(spring);
        animation.addEndListener((animation, canceled, value, velocity) -> {
            if (!canceled) {
                view.postOnAnimation(this::finish);
            }
        });

        view.addOverScrollContract(canvas -> {
            if (state == OverScrollState.IDLE) {
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

    public void setPullEdges(@Edge int edges) {
        this.edges = edges;
        factor.setValue(0f);
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        bounds.set(0, 0, width, height);

        pivot = PIVOT_UNSPECIFIED;
        factor.setValue(0f);
    }

    @Override
    public boolean isFinished() {
        return state == OverScrollState.IDLE;
    }

    @Override
    public void finish() {
        if (animation != null && animation.isRunning()) {
            animation.cancel();
        }

        state = OverScrollState.IDLE;
        notifyStateChange(state);

        super.finish();
    }

    @Override
    public float getDistance() {
        return factor.getValue();
    }

    @Override
    public float onPullDistance(float deltaDistance, float displacement) {
        if (deltaDistance >= 0 &&
                ((pivot == PIVOT_TOP && (edges & PULL_EDGE_TOP) != PULL_EDGE_TOP) ||
                        (pivot == PIVOT_BOTTOM && (edges & PULL_EDGE_BOTTOM) != PULL_EDGE_BOTTOM))) {
            return 0;
        }

        float delta = Math.max(0f, deltaDistance + factor.getValue()) - factor.getValue();
        onPull(delta);

        return delta;
    }

    @Override
    public void onPull(float deltaDistance) {
        state = OverScrollState.OVER_SCROLLING;
        notifyStateChange(state);

        if (animation.isRunning()) {
            animation.cancel();
        }

        factor.setValue(factor.getValue() + deltaDistance * (1f - factor.getValue()));
    }

    @Override
    public void onRelease() {
        if (!animation.isRunning()) {
            if (factor.getValue() == 0) {
                finish();
            } else {
                state = OverScrollState.SETTLING;
                notifyStateChange(state);
                animation.setStartVelocity(0)
                        .setStartValue(factor.getValue() * SPRING_FACTOR_MULTIPLIER)
                        .start();
            }
        }
    }

    @Override
    public void onAbsorb(int velocity) {
        state = OverScrollState.SETTLING;
        notifyStateChange(state);

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

    private void notifyStateChange(OverScrollState state) {
        if (oldState == state || pivot == PIVOT_UNSPECIFIED) {
            return;
        }

        if (pivot == PIVOT_TOP && (edges & PULL_EDGE_TOP) == PULL_EDGE_TOP) {
            for (OnOverScrollListener listener : overScrollListeners) {
                listener.onOverScrollStateChanged(PULL_EDGE_TOP, state);
            }
        } else if (pivot == PIVOT_BOTTOM && (edges & PULL_EDGE_BOTTOM) == PULL_EDGE_BOTTOM) {
            for (OnOverScrollListener listener : overScrollListeners) {
                listener.onOverScrollStateChanged(PULL_EDGE_BOTTOM, state);
            }
        }

        oldState = state;
    }

    private void notifyOverScrolled(float factor) {
        if (pivot == PIVOT_UNSPECIFIED) {
            return;
        }

        int edge = (pivot == PIVOT_TOP) ? PULL_EDGE_TOP : PULL_EDGE_BOTTOM;

        for (OnOverScrollListener listener : overScrollListeners) {
            listener.onOverScrolled(edge, factor);
        }
    }

    public void addOnOverScrollListener(OnOverScrollListener listener) {
        if (listener != null) {
            overScrollListeners.add(listener);
        }
    }

    public void removeOnOverScrollListener(OnOverScrollListener listener) {
        if (listener != null) {
            overScrollListeners.remove(listener);
        }
    }

    void onTouchEvent(MotionEvent event) {
        if (pivot != PIVOT_UNSPECIFIED) {
            return;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialTouchY = event.getY();
                break;

            case MotionEvent.ACTION_MOVE:
                if (initialTouchY == -1) {
                    initialTouchY = event.getY();

                    break;
                }

                float dy = event.getY() - initialTouchY;
                if (dy != 0 && !view.canScrollVertically(dy > 0 ? 1 : -1)) {
                    if (dy > 0) {
                        pivot = PIVOT_TOP;
                    } else {
                        pivot = PIVOT_BOTTOM;
                    }
                }

                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                initialTouchY = -1;
                break;
        }
    }

    public enum OverScrollState {
        IDLE,
        OVER_SCROLLING,
        SETTLING
    }

    public interface OnOverScrollListener {
        default void onOverScrollStateChanged(@OverScrollEffect.Edge int edge,
                                              @NonNull OverScrollState state) {
        }

        default void onOverScrolled(@OverScrollEffect.Edge int edge, float factor) {
        }
    }
}