package com.stario.launcher.ui.common.text;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.appcompat.widget.AppCompatTextView;

import com.stario.launcher.R;

public class WaveUnderlineTextView extends AppCompatTextView {

    // Wave movement
    private static final int WAVE_SPEED_PIXELS_PER_SECOND = 30;
    private static final int WAVE_STROKE_LENGTH_PIXELS = 6;
    private static final int WAVE_AMPLITUDE_PIXELS = 8;
    private static final int WAVE_LENGTH_PIXELS = 80;
    private static final int BOTTOM_OFFSET = 10;

    // Pulsation parameters
    private static final float PULSATE_SPEED_MIN_FACTOR = 0.5f;
    private static final float PULSATE_SPEED_MAX_FACTOR = 1.5f;
    private static final long PULSATE_DURATION_MS = 2000;
    private static final int PULSATE_ALPHA_MAX = 180;
    private static final int PULSATE_ALPHA_MIN = 80;

    private ValueAnimator pulsationAnimator;
    private float currentSpeedMultiplier;
    private ValueAnimator waveAnimator;
    private long lastWaveUpdate = 0;
    private float animationOffset;
    private int waveColor;
    private Paint paint;
    private Path path;

    public WaveUnderlineTextView(Context context) {
        super(context);

        init(null);
    }

    public WaveUnderlineTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(attrs);
    }

    public WaveUnderlineTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(attrs);
    }

    private void init(AttributeSet attrs) {
        this.path = new Path();
        this.animationOffset = 0f;
        this.currentSpeedMultiplier = PULSATE_SPEED_MIN_FACTOR;
        this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(WAVE_STROKE_LENGTH_PIXELS);
        paint.setPathEffect(null);

        waveColor = Color.WHITE;
        if (attrs != null) {
            //noinspection resource
            TypedArray a = getContext().obtainStyledAttributes(attrs,
                    R.styleable.WaveUnderlineTextView);

            try {
                waveColor = a.getColor(R.styleable.WaveUnderlineTextView_waveColor, Color.WHITE);
            } finally {
                a.recycle();
            }
        }

        paint.setColor(Color.argb(PULSATE_ALPHA_MIN,
                Color.red(waveColor), Color.green(waveColor), Color.blue(waveColor)));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint textPaint = getPaint();
        String text = getText().toString();

        if (text.isEmpty()) {
            return;
        }

        float textWidth = textPaint.measureText(text);
        float xStart;
        float viewWidth = getWidth();
        int gravity = getGravity();

        if ((gravity & Gravity.LEFT) == Gravity.LEFT ||
                (gravity & Gravity.START) == Gravity.START) {
            xStart = getPaddingLeft();
        } else if ((gravity & Gravity.RIGHT) == Gravity.RIGHT ||
                (gravity & Gravity.END) == Gravity.END) {
            xStart = viewWidth - getPaddingRight() - textWidth;
        } else if ((gravity & Gravity.CENTER_HORIZONTAL) == Gravity.CENTER_HORIZONTAL) {
            xStart = (viewWidth - textWidth) / 2f;
        } else {
            xStart = getPaddingLeft();
        }

        float xEnd = xStart + textWidth;
        float y = getHeight() - getPaddingBottom() + BOTTOM_OFFSET;

        path.reset();

        if (xStart >= xEnd) { // still, draw something
            float currentWaveX = xStart - animationOffset;
            float angle = (currentWaveX / WAVE_LENGTH_PIXELS) * (float) (2 * Math.PI);
            float yOffset = (float) Math.sin(angle) * WAVE_AMPLITUDE_PIXELS;

            path.moveTo(xStart, y + yOffset);
            path.lineTo(xStart + 1, y + yOffset);
        } else { // draw every sine point
            for (float currentXDraw = xStart; currentXDraw <= xEnd; currentXDraw += 1) {
                float currentWaveX = currentXDraw - animationOffset;
                float angle = (currentWaveX / WAVE_LENGTH_PIXELS) * (float) (2 * Math.PI);
                float yOffset = (float) Math.sin(angle) * WAVE_AMPLITUDE_PIXELS;

                if (currentXDraw == xStart) {
                    path.moveTo(currentXDraw, y + yOffset);
                } else {
                    path.lineTo(currentXDraw, y + yOffset);
                }
            }
        }
        canvas.drawPath(path, paint);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        lastWaveUpdate = 0;

        // Wave Animator
        waveAnimator = ValueAnimator.ofInt(0, 1);
        waveAnimator.setDuration(1000L);
        waveAnimator.setRepeatCount(ValueAnimator.INFINITE);
        waveAnimator.setInterpolator(new LinearInterpolator());

        waveAnimator.addUpdateListener(animation -> {
            long currentTimeNanos = System.nanoTime();
            if (lastWaveUpdate == 0) {
                lastWaveUpdate = currentTimeNanos;
                invalidate();
                return;
            }

            float deltaTimeSeconds = (currentTimeNanos - lastWaveUpdate) / 1_000_000_000.0f;
            lastWaveUpdate = currentTimeNanos;

            // cap deltaTime
            if (deltaTimeSeconds > 0.1f) {
                deltaTimeSeconds = 0.1f;
            }

            float actualSpeed = WAVE_SPEED_PIXELS_PER_SECOND * currentSpeedMultiplier;
            animationOffset += actualSpeed * deltaTimeSeconds;

            if (WAVE_LENGTH_PIXELS > 0) {
                animationOffset %= WAVE_LENGTH_PIXELS;
            }
            invalidate();
        });
        waveAnimator.start();

        // Pulsation Animator
        pulsationAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulsationAnimator.setDuration(PULSATE_DURATION_MS);
        pulsationAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulsationAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulsationAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        pulsationAnimator.addUpdateListener(animation -> {
            float interpolatedFraction = (float) animation.getAnimatedValue();

            paint.setColor(Color.argb((int) (PULSATE_ALPHA_MIN + interpolatedFraction * (PULSATE_ALPHA_MAX - PULSATE_ALPHA_MIN)),
                    Color.red(waveColor), Color.green(waveColor), Color.blue(waveColor)));

            //noinspection PointlessArithmeticExpression
            currentSpeedMultiplier = PULSATE_SPEED_MIN_FACTOR +
                    interpolatedFraction * (PULSATE_SPEED_MAX_FACTOR - PULSATE_SPEED_MIN_FACTOR);
        });
        pulsationAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (waveAnimator != null) {
            waveAnimator.cancel();
            waveAnimator = null;
        }

        if (pulsationAnimator != null) {
            pulsationAnimator.cancel();
            pulsationAnimator = null;
        }

        lastWaveUpdate = 0;
    }

    public void setWaveColor(int newColor) {
        waveColor = newColor;
        int currentAlphaValue;

        if (pulsationAnimator != null && pulsationAnimator.isRunning()) {
            float interpolatedFraction = (float) pulsationAnimator.getAnimatedValue();

            currentAlphaValue = (int) (PULSATE_ALPHA_MIN +
                    interpolatedFraction * (PULSATE_ALPHA_MAX - PULSATE_ALPHA_MIN));
        } else {
            currentAlphaValue = PULSATE_ALPHA_MIN;
        }

        paint.setColor(Color.argb(currentAlphaValue,
                Color.red(waveColor), Color.green(waveColor), Color.blue(waveColor)));

        invalidate();
    }
}