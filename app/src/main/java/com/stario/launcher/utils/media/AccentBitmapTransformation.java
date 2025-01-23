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

package com.stario.launcher.utils.media;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class AccentBitmapTransformation extends BitmapTransformation {
    private static final String ID = "com.bumptech.glide.transformations.DarkenBitmapTransformation";
    private static final byte[] ID_BYTES = ID.getBytes(StandardCharsets.UTF_8);

    @Override
    public Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap bitmap,
                            int outWidth, int outHeight) {
        @ColorInt int color = getAccentColor(bitmap);

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float radius = Math.max(width, height) / 1.5f;

        Paint paint = new Paint();
        Canvas canvas = new Canvas(bitmap);
        RadialGradient gradient = new RadialGradient(
                width / 2f, height / 2f, radius,
                new int[]{
                        Color.argb(0.1f, Color.red(color) / 255f,
                                Color.green(color) / 255f, Color.blue(color) / 255f),
                        Color.argb(0.3f, Color.red(color) / 255f,
                                Color.green(color) / 255f, Color.blue(color) / 255f),
                        Color.argb(0.7f, Color.red(color) / 255f,
                                Color.green(color) / 255f, Color.blue(color) / 255f),
                        Color.argb(0.9f, Color.red(color) / 255f,
                                Color.green(color) / 255f, Color.blue(color) / 255f),
                },
                new float[]{0f, 0.33f, 0.66f, 1f},
                Shader.TileMode.CLAMP);

        paint.setAntiAlias(true);
        paint.setShader(gradient);

        canvas.drawRect(new Rect(0, 0, width, height), paint);

        return bitmap;
    }

    private int getAccentColor(Bitmap bitmap) {
        Palette palette = Palette.from(bitmap).generate();

        @ColorInt int color = checkLuma(palette.getDominantColor(Color.BLACK), Color.BLACK);

        if (color == Color.BLACK) {
            color = palette.getDarkVibrantColor(color);
        }

        if (color == Color.BLACK) {
            color = checkLuma(palette.getVibrantColor(color), color);
        }

        if (color == Color.BLACK) {
            color = palette.getDarkMutedColor(color);
        }

        if (color == Color.BLACK) {
            color = checkLuma(palette.getMutedColor(color), color);
        }

        return color;
    }

    private int checkLuma(@ColorInt int target, @ColorInt int color) {
        var luma = 0.2126 * Color.red(target) +
                0.7152 * Color.green(target) +
                0.0722 * Color.blue(target); // per ITU-R BT.709

        return luma < 70 ? target : color;
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof AccentBitmapTransformation;
    }

    @Override
    public int hashCode() {
        return ID.hashCode();
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) {
        messageDigest.update(ID_BYTES);
    }
}