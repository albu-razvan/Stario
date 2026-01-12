/*
 * Copyright (C) 2026 Răzvan Albu
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

package com.stario.launcher.ui.common.text;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;

import androidx.appcompat.widget.AppCompatTextView;

import com.stario.launcher.ui.Measurements;

import java.util.concurrent.locks.ReentrantLock;

public class DelayedMarqueeTextView extends AppCompatTextView {
    private static final float SCROLL_SPEED_PIXELS_PER_SECOND = 100f;
    private static final long MARQUEE_DELAY = 2000L;
    private static final int FADING_EDGE_LENGTH = 20;
    private static final String SPACING = "        ";

    private ValueAnimator scrollAnimator;
    private CharSequence originalText;
    private boolean isMarqueeNeeded;
    private ReentrantLock textLock;
    private int lastWidth;

    public DelayedMarqueeTextView(Context context) {
        this(context, null);
    }

    public DelayedMarqueeTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DelayedMarqueeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {
        this.textLock = new ReentrantLock();
        this.originalText = "";
        this.isMarqueeNeeded = false;
        this.lastWidth = -1;

        setSingleLine(true);
        setEllipsize(null);
        setHorizontallyScrolling(true);

        setHorizontalScrollBarEnabled(false);
        setHorizontalFadingEdgeEnabled(true);
        setFadingEdgeLength(FADING_EDGE_LENGTH);
    }

    @Override
    public void setSingleLine(boolean singleLine) {
        if (!singleLine) {
            return;
        }

        super.setSingleLine(true);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (textLock == null) {
            textLock = new ReentrantLock();
        }

        textLock.lock();

        try {
            originalText = text;
            lastWidth = -1;

            super.setText(text, type);
        } finally {
            textLock.unlock();
            setupMarqueeInternal();
        }
    }

    @Override
    public CharSequence getText() {
        if (originalText == null) {
            return "";
        }

        return originalText;
    }

    private void setupMarqueeInternal() {
        int width = getWidth();
        if (width <= 0 || width == lastWidth) {
            return;
        }

        lastWidth = width;
        stopMarqueeInternal();

        if (originalText == null || originalText.length() == 0) {
            return;
        }

        float originalTextWidth = getPaint().measureText(originalText.toString());
        float viewWidth = width - getPaddingLeft() - getPaddingRight();

        isMarqueeNeeded = originalTextWidth > viewWidth
                && Measurements.getAnimatorDurationScale() > 0;
        if (!isMarqueeNeeded) {
            restoreOriginalText();

            return;
        }

        if (textLock == null) {
            textLock = new ReentrantLock();
        }

        textLock.lock();
        try {
            super.setText(
                    TextUtils.concat(originalText, SPACING, originalText),
                    BufferType.NORMAL
            );
        } finally {
            textLock.unlock();
        }

        int scrollDistance = (int) getPaint().measureText(originalText.toString() + SPACING);
        long duration = (long) ((scrollDistance / SCROLL_SPEED_PIXELS_PER_SECOND) * 1000);

        scrollAnimator = ValueAnimator.ofInt(0, scrollDistance);
        scrollAnimator.setInterpolator(new LinearInterpolator());
        scrollAnimator.setInterpolator(new PathInterpolator(0.15f, 0.01f, 0.85f, 1f));
        scrollAnimator.setDuration((long) (duration / Measurements.getAnimatorDurationScale()));
        scrollAnimator.setStartDelay((long) (MARQUEE_DELAY / Measurements.getAnimatorDurationScale()));

        scrollAnimator.addUpdateListener(anim ->
                scrollTo((int) anim.getAnimatedValue(), 0));

        scrollAnimator.addListener(new AnimatorListenerAdapter() {
            boolean isCancelled = false;

            @Override
            public void onAnimationCancel(Animator animation) {
                isCancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                scrollTo(0, 0);

                if (!isCancelled) {
                    postDelayed(() -> {
                        if (!isCancelled && scrollAnimator != null) {
                            scrollAnimator.setIntValues(0, scrollDistance);
                            scrollAnimator.start();
                        }
                    }, (long) (MARQUEE_DELAY / Measurements.getAnimatorDurationScale()));
                } else {
                    isCancelled = false;
                }
            }
        });

        scrollAnimator.start();
    }

    private void restoreOriginalText() {
        if (textLock == null) {
            textLock = new ReentrantLock();
        }

        textLock.lock();

        try {
            if (!TextUtils.equals(super.getText(), originalText)) {
                super.setText(originalText, BufferType.NORMAL);
            }

            scrollTo(0, 0);
        } finally {
            textLock.unlock();
        }
    }

    public void stopMarqueeInternal() {
        if (scrollAnimator != null) {
            scrollAnimator.cancel();
            scrollAnimator = null;
        }

        lastWidth = -1;
        scrollTo(0, 0);
    }

    @Override
    protected float getLeftFadingEdgeStrength() {
        if (!isMarqueeNeeded && getLayout() != null) {
            return 0f;
        }

        if (getLayoutDirection() == LAYOUT_DIRECTION_LTR) {
            int distance = getScrollX();
            return distance > 0 && distance <= (int) getLayout().getLineRight(0) / 2 ?
                    Math.min((float) distance / FADING_EDGE_LENGTH, 1f) : 0f;
        } else {
            return 1f;
        }
    }

    @Override
    protected float getRightFadingEdgeStrength() {
        if (!isMarqueeNeeded && getLayout() != null) {
            return 0f;
        }

        if (getLayoutDirection() == LAYOUT_DIRECTION_LTR) {
            return 1f;
        } else {
            int distance = getScrollX();
            return distance > 0 && distance <= (int) getLayout().getLineRight(0) / 2 ?
                    Math.min((float) distance / FADING_EDGE_LENGTH, 1f) : 0f;
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        if (width != oldWidth) {
            setupMarqueeInternal();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        setupMarqueeInternal();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);

        if (hasWindowFocus) {
            setupMarqueeInternal();
        } else {
            stopMarqueeInternal();
        }
    }
}
