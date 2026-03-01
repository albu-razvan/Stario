/*
 * Copyright (C) 2026 Răzvan Albu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.stario.launcher.ui.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.stario.launcher.R;
import com.stario.launcher.Stario;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.utils.Utils;

import java.util.Calendar;
import java.util.Locale;

// Used Gemini 3 Pro for refining the layout logic
public class StylizedClockView extends View implements SensorEventListener {
    public static final String BACKGROUND_ALPHA_KEY = "com.stario.BACKGROUND_ALPHA";
    public static final String IMPERIAL_KEY = "com.stario.IMPERIAL";

    private static final float MIN_SCALING_RATIO_THRESHOLD = 0.3f;
    private static final float LYING_ON_TABLE_THRESHOLD = 8.5f;
    private static final float TILT_DEADZONE = 1.2f;
    private static final float TEST_HOUR_SIZE = 100f;
    private static final float BASE_GAP = 5f;

    private RectF pillBackgroundRect;
    private RectF amContainerRect;
    private RectF pmContainerRect;
    private RectF contentRect;

    private Paint backgroundPaint;
    private Paint outlinePaint;
    private Paint minutePaint;
    private Paint pillBgPaint;
    private Paint pillFgPaint;
    private Paint amPmPaint;
    private Paint hourPaint;

    private float containerRadius;
    private float minuteDrawX;
    private float minuteDrawY;
    private float hourDrawX;
    private float hourDrawY;
    private float amTextX;
    private float amTextY;
    private float pmTextX;
    private float pmTextY;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float gravityAngle;

    private SharedPreferences preferences;
    private Calendar calendar;
    private Stario stario;

    public StylizedClockView(Context context) {
        super(context);

        init(context, null);
    }

    public StylizedClockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init(context, attrs);
    }

    public StylizedClockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        this.stario = (Stario) context.getApplicationContext();
        this.preferences = stario.getSharedPreferences(Entry.CLOCK);
        this.calendar = Calendar.getInstance();

        contentRect = new RectF();
        amContainerRect = new RectF();
        pmContainerRect = new RectF();
        pillBackgroundRect = new RectF();

        int textColor = Color.rgb(239, 223, 219);
        int bgColor = Color.rgb(34, 26, 24);
        int outlineColor = Color.rgb(55, 46, 44);
        int pillBgColor = Color.rgb(93, 64, 56);
        int pillFgColor = Color.rgb(249, 183, 165);

        if (attrs != null) {
            try (TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.StylizedClockView)) {
                textColor = array.getColor(R.styleable.StylizedClockView_clockTextColor, textColor);
                bgColor = array.getColor(R.styleable.StylizedClockView_clockBackgroundColor, bgColor);
                outlineColor = array.getColor(R.styleable.StylizedClockView_clockOutlineColor, outlineColor);
                pillBgColor = array.getColor(R.styleable.StylizedClockView_clockPillBackgroundColor, pillBgColor);
                pillFgColor = array.getColor(R.styleable.StylizedClockView_clockPillForegroundColor, pillFgColor);
            }
        }

        Typeface clockfaceTypeface;
        Typeface dmSansTypeface;

        try {
            clockfaceTypeface = ResourcesCompat.getFont(context, R.font.stario_clockface_variable);
            dmSansTypeface = ResourcesCompat.getFont(context, R.font.dm_sans_black);
        } catch (Exception e) {
            clockfaceTypeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
            dmSansTypeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
        }

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(bgColor);

        outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setColor(outlineColor);

        hourPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hourPaint.setColor(textColor);
        hourPaint.setTypeface(clockfaceTypeface);

        minutePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        minutePaint.setColor(textColor);
        minutePaint.setTypeface(clockfaceTypeface);

        amPmPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        amPmPaint.setColor(textColor);
        amPmPaint.setTypeface(dmSansTypeface);
        amPmPaint.setTextAlign(Paint.Align.CENTER);

        pillBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pillBgPaint.setColor(pillBgColor);
        pillBgPaint.setStyle(Paint.Style.FILL);

        pillFgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pillFgPaint.setColor(pillFgColor);
        pillFgPaint.setStyle(Paint.Style.FILL);

        gravityAngle = 0f;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        sensorManager.unregisterListener(this);
    }

    /**
     * @noinspection SuspiciousNameCombination
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_GRAVITY) {
            return;
        }

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float flatAmount = Math.min(1f,
                Math.max(0f, Math.abs(z) - LYING_ON_TABLE_THRESHOLD));
        int rotation = ((WindowManager) stario.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay()
                .getRotation();

        float adjustedX = x;
        float adjustedY = y;
        switch (rotation) {
            // portrait
            case Surface.ROTATION_0:
                adjustedX = -x;
                adjustedY = y;

                break;
            // landscape
            case Surface.ROTATION_90:
                adjustedX = y;
                adjustedY = x;

                break;
            // reverse portrait
            case android.view.Surface.ROTATION_180:
                adjustedX = x;
                adjustedY = -y;

                break;
            // reverse landscape
            case android.view.Surface.ROTATION_270:
                adjustedX = -y;
                adjustedY = -x;

                break;
        }

        float rawAngle = ((float) Math.toDegrees(Math.atan2(-adjustedX, adjustedY)) + 360f) % 360f;
        if (z < 0) {
            rawAngle += 180f;
        }

        if ((float) Math.sqrt(x * x + y * y) < TILT_DEADZONE) {
            rawAngle = gravityAngle;
        }

        gravityAngle += 0.08f *
                ((getDegreeStrengthBias(rawAngle, flatAmount) - gravityAngle + 540f) % 360f - 180f);

        invalidate();
    }

    private float getDegreeStrengthBias(float angle, float factor) {
        float delta = ((0 - angle + 540f) % 360f) - 180f;
        return (angle + delta * factor + 360f) % 360f;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // ¯\_(ツ)_/¯
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        super.onSizeChanged(width, height, oldw, oldh);

        calculateLayout(width, height);
    }

    /**
     * @noinspection SuspiciousNameCombination
     */
    private void calculateLayout(int viewWidth, int viewHeight) {
        if (viewWidth <= 0 || viewHeight <= 0) {
            return;
        }

        boolean isWideMode = viewWidth >= viewHeight * 1.8f;

        hourPaint.setTextScaleX(1f);
        minutePaint.setTextScaleX(1f);
        amPmPaint.setTextScaleX(1f);

        hourPaint.setTextSize(TEST_HOUR_SIZE);
        hourPaint.setFontVariationSettings("'VTCL' 100");

        Rect hourBounds = new Rect();
        hourPaint.getTextBounds("00", 0, 2, hourBounds);
        float hourHeight = hourBounds.height();
        float hourWidth = hourBounds.width();

        float testAmPmSize = TEST_HOUR_SIZE * 0.11f;
        amPmPaint.setTextSize(testAmPmSize);
        Rect amBounds = new Rect();
        amPmPaint.getTextBounds("AM", 0, 2, amBounds);

        Rect minBounds = new Rect();

        float containerHeight;
        float stackedContainersHeight;
        float containerWidth;

        float testMinuteSize;
        float minWidth;

        float totalMaxWidth;
        float totalMaxHeight;

        if (isWideMode) {
            minutePaint.setFontVariationSettings("'VTCL' 0");
            testMinuteSize = TEST_HOUR_SIZE;
            minutePaint.setTextSize(testMinuteSize);
            minutePaint.getTextBounds("00", 0, 2, minBounds);
            minWidth = minBounds.width();

            float wideStackHeight = hourHeight * 0.5f;
            containerHeight = wideStackHeight / 2f;
            stackedContainersHeight = wideStackHeight;

            float desiredTextHeight = containerHeight * 0.55f;
            if (amBounds.height() > 0) {
                float scaleFactor = desiredTextHeight / amBounds.height();
                testAmPmSize *= scaleFactor;
            }

            amPmPaint.setTextSize(testAmPmSize);
            amPmPaint.getTextBounds("AM", 0, 2, amBounds);

            containerWidth = containerHeight * 2.3f;

            totalMaxWidth = hourWidth + minWidth + containerWidth + (BASE_GAP * 6);
            totalMaxHeight = Math.max(hourHeight, stackedContainersHeight) + (BASE_GAP * 4);
        } else {
            float amPmPaddingY = testAmPmSize * 0.2f;
            containerHeight = amBounds.height() + amPmPaddingY * 2;
            stackedContainersHeight = containerHeight * 2;

            float targetMinHeight = hourHeight - stackedContainersHeight - BASE_GAP * 1.5f;
            testMinuteSize = TEST_HOUR_SIZE * 0.5f;

            minutePaint.setFontVariationSettings("'VTCL' 0");
            minutePaint.setTextSize(testMinuteSize);
            minutePaint.getTextBounds("00", 0, 2, minBounds);

            float desiredBaseMinHeight = targetMinHeight / 1.15f;
            if (minBounds.height() > 0) {
                testMinuteSize *= (desiredBaseMinHeight / minBounds.height());
            }

            minutePaint.setTextSize(testMinuteSize);
            minutePaint.getTextBounds("00", 0, 2, minBounds);

            float vtcl = 0;
            if (minBounds.height() > 0) {
                float multiplier = targetMinHeight / minBounds.height();
                vtcl = Math.max(0, Math.min(100, (multiplier - 1.0f) / 0.3356f * 100f));
            }

            minutePaint.setFontVariationSettings("'VTCL' " + vtcl);
            minutePaint.getTextBounds("00", 0, 2, minBounds);

            minWidth = minutePaint.measureText("00");
            containerWidth = minWidth;

            totalMaxWidth = hourWidth + BASE_GAP + minWidth + (BASE_GAP * 4);
            totalMaxHeight = hourHeight + (BASE_GAP * 4);
        }

        float scaleX = viewWidth / totalMaxWidth;
        float scaleY = viewHeight / totalMaxHeight;
        float ratioXtoY = scaleX / scaleY;

        // There are instances where micro gaps occur (i.e. the actual clock face
        // takes just a bit less space than the fully available space on an axis).
        // When that happens, a small scale factor can be applied to align the content
        // to the bounds.
        int backgroundAlpha = preferences.getInt(BACKGROUND_ALPHA_KEY, 0);
        if (Math.abs(ratioXtoY - 1f) > MIN_SCALING_RATIO_THRESHOLD || backgroundAlpha > 0) {
            float scale = Math.min(viewWidth / totalMaxWidth, viewHeight / totalMaxHeight);

            hourPaint.setTextSize(TEST_HOUR_SIZE * scale);
            minutePaint.setTextSize(testMinuteSize * scale);
            amPmPaint.setTextSize(testAmPmSize * scale);

            float finalGap = BASE_GAP * scale;
            float finalPadding = BASE_GAP * 2f * scale;
            float finalContainerWidth = containerWidth * scale;
            float finalContainerHeight = containerHeight * scale;
            containerRadius = finalContainerHeight / 2f;

            hourPaint.getTextBounds("00", 0, 2, hourBounds);
            minutePaint.getTextBounds("00", 0, 2, minBounds);
            amPmPaint.getTextBounds("AM", 0, 2, amBounds);

            float finalContentWidth = totalMaxWidth * scale - (finalPadding * 2);
            float finalContentHeight = totalMaxHeight * scale - (finalPadding * 2);

            float startX = (viewWidth - finalContentWidth) / 2f;
            float startY = (viewHeight - finalContentHeight) / 2f;

            contentRect.set(
                    startX - finalPadding,
                    startY - finalPadding,
                    startX + finalContentWidth + finalPadding,
                    startY + finalContentHeight + finalPadding
            );

            hourDrawX = startX;
            hourDrawY = startY - hourBounds.top;

            float rightColX = hourDrawX + hourPaint.measureText("00") + finalGap;

            minuteDrawX = rightColX;
            minuteDrawY = startY - minBounds.top;

            if (isWideMode) {
                float amPmX = minuteDrawX + minutePaint.measureText("00") + finalGap;
                float stackTop = startY + (finalContentHeight - (finalContainerHeight * 2)) / 2f;

                amContainerRect.set(amPmX, stackTop, amPmX + finalContainerWidth,
                        stackTop + finalContainerHeight);
                pmContainerRect.set(amPmX, amContainerRect.bottom, amPmX + finalContainerWidth,
                        amContainerRect.bottom + finalContainerHeight);
            } else {
                float stackBottom = startY + hourBounds.height();
                float amPmTop = stackBottom - (finalContainerHeight * 2);

                amContainerRect.set(rightColX, amPmTop, rightColX + finalContainerWidth,
                        amPmTop + finalContainerHeight);
                pmContainerRect.set(rightColX, amContainerRect.bottom, rightColX
                        + finalContainerWidth, amContainerRect.bottom + finalContainerHeight);
            }

        } else {
            hourPaint.setTextSize(TEST_HOUR_SIZE * scaleY);
            hourPaint.setTextScaleX(ratioXtoY);

            minutePaint.setTextSize(testMinuteSize * scaleY);
            minutePaint.setTextScaleX(ratioXtoY);

            amPmPaint.setTextSize(testAmPmSize * scaleY);
            amPmPaint.setTextScaleX(ratioXtoY);

            float finalGapX = BASE_GAP * scaleX;
            float finalPaddingX = BASE_GAP * 2f * scaleX;
            float finalPaddingY = BASE_GAP * 2f * scaleY;

            float finalContainerWidth = containerWidth * scaleX;
            float finalContainerHeight = containerHeight * scaleY;
            containerRadius = Math.min(finalContainerWidth, finalContainerHeight) / 2f;

            hourPaint.getTextBounds("00", 0, 2, hourBounds);
            minutePaint.getTextBounds("00", 0, 2, minBounds);
            amPmPaint.getTextBounds("AM", 0, 2, amBounds);

            float scaledHourWidth = hourPaint.measureText("00");
            float scaledMinWidth = minutePaint.measureText("00");

            contentRect.set(0, 0, viewWidth, viewHeight);

            hourDrawX = finalPaddingX;
            hourDrawY = finalPaddingY - hourBounds.top;

            float rightColX = hourDrawX + scaledHourWidth + finalGapX;

            minuteDrawX = rightColX;
            minuteDrawY = finalPaddingY - minBounds.top;

            if (isWideMode) {
                float amPmX = minuteDrawX + scaledMinWidth + finalGapX;
                float stackTop = finalPaddingY + (totalMaxHeight * scaleY - (finalPaddingY * 2)
                        - (finalContainerHeight * 2)) / 2f;

                amContainerRect.set(amPmX, stackTop, amPmX + finalContainerWidth,
                        stackTop + finalContainerHeight);
                pmContainerRect.set(amPmX, amContainerRect.bottom, amPmX + finalContainerWidth,
                        amContainerRect.bottom + finalContainerHeight);
            } else {
                float stackBottom = finalPaddingY + hourBounds.height();
                float amPmTop = stackBottom - (finalContainerHeight * 2);

                amContainerRect.set(rightColX, amPmTop, rightColX + finalContainerWidth,
                        amPmTop + finalContainerHeight);
                pmContainerRect.set(rightColX, amContainerRect.bottom, rightColX
                        + finalContainerWidth, amContainerRect.bottom + finalContainerHeight);
            }
        }

        amTextX = amContainerRect.centerX();
        amTextY = amContainerRect.centerY() - (amBounds.top + amBounds.bottom) / 2f;

        pmTextX = pmContainerRect.centerX();
        pmTextY = pmContainerRect.centerY() - (amBounds.top + amBounds.bottom) / 2f;

        outlinePaint.setStrokeWidth(Measurements.dpToPx(1.5f));

        pillBackgroundRect.set(
                amContainerRect.left,
                amContainerRect.top,
                amContainerRect.right,
                pmContainerRect.bottom
        );
    }

    @Override
    @SuppressLint("MissingSuperCall")
    public void draw(@NonNull Canvas canvas) {
        if (hourPaint == null) {
            return;
        }

        // Background
        float cornerRadius = Math.min(contentRect.width(), contentRect.height()) * 0.1f;
        float halfStroke = outlinePaint.getStrokeWidth() / 2f;
        RectF bgRect = new RectF(
                contentRect.left + halfStroke,
                contentRect.top + halfStroke,
                contentRect.right - halfStroke,
                contentRect.bottom - halfStroke
        );

        int alpha = preferences.getInt(BACKGROUND_ALPHA_KEY, 0);
        backgroundPaint.setAlpha(alpha);
        outlinePaint.setAlpha(alpha);

        canvas.drawRoundRect(
                bgRect,
                cornerRadius,
                cornerRadius,
                backgroundPaint
        );

        canvas.drawRoundRect(
                bgRect,
                cornerRadius,
                cornerRadius,
                outlinePaint
        );

        // Time compute
        calendar.setTimeInMillis(System.currentTimeMillis());
        boolean is24Hour = !preferences.getBoolean(IMPERIAL_KEY, Utils.isSystemUsingImperial(stario));

        int hour = is24Hour ? calendar.get(Calendar.HOUR_OF_DAY) : calendar.get(Calendar.HOUR);
        if (!is24Hour && hour == 0) {
            hour = 12;
        }

        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int millisecond = calendar.get(Calendar.MILLISECOND);

        String hourStr = String.format(Locale.US, "%02d", hour);
        String minStr = String.format(Locale.US, "%02d", minute);

        // Time drawing
        canvas.drawText(hourStr, hourDrawX, hourDrawY, hourPaint);
        canvas.drawText(minStr, minuteDrawX, minuteDrawY, minutePaint);

        // AM/PM or 24H pill
        if (is24Hour) {
            float centerX = pillBackgroundRect.centerX();
            float centerY = pillBackgroundRect.centerY();
            float width = pillBackgroundRect.width();
            float height = pillBackgroundRect.height();
            float diagonal = (float) Math.sqrt(width * width + height * height);
            float radius = diagonal / 2f;

            canvas.drawRoundRect(pillBackgroundRect, containerRadius, containerRadius, pillBgPaint);

            canvas.save();

            Path clipPath = new Path();
            clipPath.addRoundRect(pillBackgroundRect, containerRadius, containerRadius, Path.Direction.CW);

            canvas.clipPath(clipPath);
            canvas.translate(centerX, centerY);
            canvas.rotate(gravityAngle);

            float progress = (second * 1000f + millisecond) / 60000f;
            float liquidLevel = radius - (diagonal * progress);

            RectF fillRect = new RectF(-radius, liquidLevel, radius, radius + 100f);
            canvas.drawRect(fillRect, pillFgPaint);

            canvas.restore();
        } else {
            boolean isAm = calendar.get(Calendar.AM_PM) == Calendar.AM;

            pillBgPaint.setAlpha(isAm ? 255 : 80);
            amPmPaint.setAlpha(isAm ? 255 : 80);
            canvas.drawRoundRect(amContainerRect, containerRadius, containerRadius, pillBgPaint);
            canvas.drawText("AM", amTextX, amTextY, amPmPaint);

            pillBgPaint.setAlpha(!isAm ? 255 : 80);
            amPmPaint.setAlpha(!isAm ? 255 : 80);
            canvas.drawRoundRect(pmContainerRect, containerRadius, containerRadius, pillBgPaint);
            canvas.drawText("PM", pmTextX, pmTextY, amPmPaint);

            pillBgPaint.setAlpha(Color.alpha(255));
        }

        postInvalidateOnAnimation();
    }
}