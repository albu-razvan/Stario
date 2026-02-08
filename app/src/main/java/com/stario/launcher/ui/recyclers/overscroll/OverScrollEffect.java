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
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
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

    private static final byte PIVOT_UNSPECIFIED = 0b00;
    static final byte PIVOT_TOP = 0b10;
    static final byte PIVOT_BOTTOM = 0b01;

    private static final float VELOCITY_MULTIPLIER = 3f;
    private static final float VELOCITY_MULTIPLIER_FLING = 1.2f;
    private static final float SPRING_FACTOR_MULTIPLIER = 1000f;
    private static final float SPRING_STIFFNESS = 300f;
    private static final float SPRING_DAMPING_RATIO = 1f;
    private static final float SCALE_MULTIPLIER = 0.05f;
    private static final float TRANSLATE_MULTIPLIER = 0.1f;
    private static final float MAX_TRANSLATION_DP = 100f;

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
    private final float maxFlingVelocity;
    private final float touchSlop;
    private final Rect bounds;
    private final V view;

    private boolean receivedMoveEvent;
    private boolean isCanvasCaptured;
    private OverScrollState oldState;
    private OverScrollState state;
    private float initialTouchY;
    private int pivot;

    @Edge
    private int edges;

    public OverScrollEffect(@NonNull V view, int edges) {
        this(view, edges, PIVOT_UNSPECIFIED);
    }

    public OverScrollEffect(@NonNull V view, int edges, byte pivot) {
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
        this.pivot = pivot;
        this.edges = edges;
        this.receivedMoveEvent = false;
        this.isCanvasCaptured = false;

        ViewConfiguration configuration = ViewConfiguration.get(view.getContext());
        this.touchSlop = configuration.getScaledTouchSlop();
        this.maxFlingVelocity = configuration.getScaledMaximumFlingVelocity();

        SpringForce spring = new SpringForce();
        spring.setFinalPosition(0f);
        spring.setStiffness(SPRING_STIFFNESS);
        spring.setDampingRatio(SPRING_DAMPING_RATIO);

        // Animation behaves better with bigger numbers, so artificially increase the
        // factor by an arbitrary value when animating
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

            if (!view.tryCaptureOverScroll(this)) {
                isCanvasCaptured = false;

                return false;
            }

            if (this.pivot == PIVOT_UNSPECIFIED) {
                if (!view.canScrollVertically(1)) {
                    if (view.canScrollVertically(-1)) {
                        this.pivot = PIVOT_BOTTOM;
                    }
                } else if (!view.canScrollVertically(-1)) {
                    this.pivot = PIVOT_TOP;
                }
            }

            int maxTranslation = Math.min((int) (canvas.getHeight() * TRANSLATE_MULTIPLIER),
                    Measurements.dpToPx(MAX_TRANSLATION_DP));

            float factorValue = Math.min(1, factor.getValue());
            float easedFactor = 1 - (float) Math.pow(1 - factorValue, 3f);
            float translationY = easedFactor * maxTranslation;
            float scaleY = 1 + (float) Math.pow(factorValue, 0.8) * SCALE_MULTIPLIER;

            if (pivot == PIVOT_BOTTOM) {
                canvas.translate(0, -translationY);
                canvas.scale(1, scaleY, bounds.centerX(), bounds.height());
            } else if (pivot == PIVOT_TOP) {
                canvas.translate(0, translationY);
                canvas.scale(1, scaleY, bounds.centerX(), 0);
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

        view.releaseOverScroll(this);

        state = OverScrollState.IDLE;
        notifyStateChange(state);

        super.finish();
    }

    @Override
    public float getDistance() {
        if (!receivedMoveEvent || !isPullAllowed()) {
            return 0f;
        }

        return factor.getValue();
    }

    /** @noinspection BooleanMethodIsAlwaysInverted*/
    private boolean isPullAllowed() {
        if (pivot == PIVOT_TOP) {
            return (edges & PULL_EDGE_TOP) == PULL_EDGE_TOP;
        } else if (pivot == PIVOT_BOTTOM) {
            return (edges & PULL_EDGE_BOTTOM) == PULL_EDGE_BOTTOM;
        }

        return false;
    }

    @Override
    public float onPullDistance(float deltaDistance, float displacement) {
        if (!receivedMoveEvent || !isPullAllowed()) {
            return 0;
        }

        if (deltaDistance >= 0 &&
                (((pivot == PIVOT_TOP && (edges & PULL_EDGE_TOP) != PULL_EDGE_TOP))
                        || (pivot == PIVOT_BOTTOM && (edges & PULL_EDGE_BOTTOM) != PULL_EDGE_BOTTOM))) {
            return deltaDistance;
        }

        float newFactor = deltaDistance + factor.getValue();
        float delta = Math.max(0f, newFactor) - factor.getValue();
        onPull(delta);

        if (newFactor <= 0) {
            finish();
        }

        return delta;
    }

    @Override
    public void onPull(float deltaDistance) {
        if (!receivedMoveEvent || !isPullAllowed()) {
            return;
        }

        if (state == OverScrollState.IDLE
                && !view.tryCaptureOverScroll(this)) {
            return;
        }

        state = OverScrollState.OVER_SCROLLING;
        notifyStateChange(state);

        if (animation.isRunning()) {
            animation.cancel();
        }

        factor.setValue(factor.getValue() + deltaDistance);
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
        if (state == OverScrollState.IDLE
                && !view.tryCaptureOverScroll(this)) {
            return;
        }

        state = OverScrollState.SETTLING;
        notifyStateChange(state);

        if (animation.isRunning()) {
            animation.cancel();
        }

        float fraction = velocity / maxFlingVelocity;
        animation.setStartVelocity(
                        factor.getValue() > 0
                                ? velocity * VELOCITY_MULTIPLIER
                                : velocity * VELOCITY_MULTIPLIER_FLING * (1 - fraction))
                .setStartValue(factor.getValue() > 0
                        ? factor.getValue() * SPRING_FACTOR_MULTIPLIER
                        : (float) Math.pow(fraction, 1.7f) * SPRING_FACTOR_MULTIPLIER / 2)
                .start();
    }

    @Override
    public boolean draw(Canvas canvas) {
        return isCanvasCaptured && !isFinished();
    }

    private void notifyStateChange(OverScrollState state) {
        if (oldState == state || !isPullAllowed()) {
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
            listener.onOverScrolled(edge, factor * factor);
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
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialTouchY = event.getRawY();
                break;

            case MotionEvent.ACTION_MOVE:
                receivedMoveEvent = true;

                if (pivot == PIVOT_TOP || pivot == PIVOT_BOTTOM) {
                    break;
                }

                if (initialTouchY == -1) {
                    initialTouchY = event.getRawY();

                    break;
                }

                float dy = event.getRawY() - initialTouchY;
                if (Math.abs(dy) >= touchSlop &&
                        !view.canScrollVertically(dy > 0 ? 1 : -1)) {
                    initialTouchY = -1;

                    if (dy > 0) {
                        pivot = PIVOT_TOP;
                    } else {
                        pivot = PIVOT_BOTTOM;
                    }
                }

                break;

            case MotionEvent.ACTION_UP:
                receivedMoveEvent = false;
                initialTouchY = -1;

                break;

            case MotionEvent.ACTION_CANCEL:
                view.releaseOverScroll(this);
                receivedMoveEvent = false;
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