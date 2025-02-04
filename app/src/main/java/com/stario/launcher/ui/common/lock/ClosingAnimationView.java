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

package com.stario.launcher.ui.common.lock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.util.AttributeSet;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.stario.launcher.utils.animation.Animation;

import carbon.widget.ConstraintLayout;

public class ClosingAnimationView extends ConstraintLayout {
    private Activity activity;
    private Paint paint;
    private boolean closed;
    private ValueAnimator animator;
    private float x = 0, y = 0;

    public ClosingAnimationView(Context context) {
        super(context);

        init(context);
    }

    public ClosingAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public ClosingAnimationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    public void init(Context context) {
        this.activity = (Activity) context;
        this.closed = false;
        this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setDither(true);
    }

    @Override
    public void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);

        if (closed) {
            float value = (float) animator.getAnimatedValue();

            if (value <= 0) {
                canvas.drawColor(Color.BLACK);
            } else {
                float abs = Math.max(getMeasuredWidth() - x, x);
                float ord = Math.max(getMeasuredHeight() - y, y);
                float radius = (float) Math.sqrt(abs * abs + ord * ord);

                RadialGradient gradient = new RadialGradient(x, y,
                        radius * value, Color.argb((1 - value), 0, 0, 0),
                        Color.BLACK, android.graphics.Shader.TileMode.CLAMP);
                paint.setShader(gradient);

                paint.setAlpha((int) Math.min(255, 500 * (1f - value)));

                canvas.drawCircle(x, y, radius, paint);
            }
        }
    }

    public boolean reset() {
        Window window = activity.getWindow();

        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }

        if (!closed) {
            return false;
        }

        closed = false;

        return true;
    }

    public interface OnAnimationEnd {
        void animationEnd();
    }

    public void closeTo(float x, float y, OnAnimationEnd listener) {
        this.x = x;
        this.y = y;

        Window window = activity.getWindow();

        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }

        animator = ValueAnimator.ofFloat(1f, 0f);
        animator.setDuration(Animation.EXTENDED.getDuration());
        animator.setInterpolator(new FastOutSlowInInterpolator());

        animator.addUpdateListener(animation -> {
            invalidate();
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                if (listener != null) {
                    listener.animationEnd();
                }
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);

                closed = true;
            }
        });

        animator.start();
    }
}