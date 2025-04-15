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

package com.stario.launcher.ui.common;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.utils.animation.Animation;

import java.util.ArrayList;
import java.util.List;

public class PulsingTextView extends AppCompatTextView {
    private static final int FADE_LENGTH = 50;
    private ValueAnimator animator;
    private Paint gradientPaint;
    private int[] evenColors;
    private int[] oddColors;
    private float offset;

    public PulsingTextView(Context context) {
        super(context);

        init();
    }

    public PulsingTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public PulsingTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {
        offset = 0;

        gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gradientPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));

        animator = ValueAnimator.ofFloat(0f, 0f)
                .setDuration(Animation.SUSTAINED.getDuration());
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        animator.cancel();

        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        post(this::calculateGradients);
    }

    @Override
    public void invalidate() {
        if (animator != null) {
            offset = animator.getAnimatedFraction() * 2f;
        }

        super.invalidate();
    }

    private void calculateGradients() {
        List<Integer> colors = new ArrayList<>();
        int fadeLength = Measurements.dpToPx(FADE_LENGTH);
        int width = getWidth();

        while (width > -fadeLength) {
            colors.add(Color.BLACK);
            colors.add(Color.argb(0.5f, 0, 0, 0));

            width -= fadeLength;
        }

        evenColors = new int[colors.size()];
        oddColors = new int[colors.size()];

        for (int index = 0; index < colors.size() - 1; index++) {
            int color = colors.get(index);

            evenColors[index] = color;
            oddColors[index + 1] = color;
        }

        int lastColor = colors.get(colors.size() - 1);
        evenColors[colors.size() - 1] = lastColor;
        oddColors[0] = lastColor;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getVisibility() != VISIBLE) {
            super.onDraw(canvas);

            return;
        }

        if (evenColors != null) {
            initGradient();

            int count = canvas.saveLayer(0.0f, 0.0f, (float) getWidth(), (float) getHeight(), null);
            super.onDraw(canvas);

            canvas.drawRect(0, 0, getWidth(), getHeight(), gradientPaint);

            canvas.restoreToCount(count);
        }

        invalidate();
    }

    private void initGradient() {
        LinearGradient gradient = new LinearGradient(0, 0, getWidth(), getHeight() * 0.3f,
                ((int) offset) % 2 == 0 ? evenColors : oddColors,
                generatePositions(), Shader.TileMode.CLAMP);
        gradientPaint.setShader(gradient);
    }

    private float[] generatePositions() {
        float fadeLength = Measurements.dpToPx(FADE_LENGTH);
        float[] positions = new float[evenColors.length];

        float interval = (1f + fadeLength / getWidth()) / positions.length;

        for (int index = 0; index < positions.length; index++) {
            positions[index] = interval * ((index - 1) + offset % 1);
        }

        return positions;
    }
}
