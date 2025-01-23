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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jp.wasabeef.glide.transformations.BlurTransformation;

public class BlurBitmapTransformation extends BlurTransformation {
    private static final String ID = "com.bumptech.glide.transformations.BlurBitmapTransformation";
    private static final byte[] ID_BYTES = ID.getBytes(StandardCharsets.UTF_8);

    public BlurBitmapTransformation(int radius) {
        super(radius);
    }

    @Override
    protected Bitmap transform(@NonNull Context context, @NonNull BitmapPool pool,
                               @NonNull Bitmap toTransform, int outWidth, int outHeight) {
        Bitmap blurred = super.transform(context, pool, toTransform, outWidth, outHeight);

        Canvas canvas = new Canvas(blurred);

        canvas.drawBitmap(createFadedBitmap(toTransform),
                new Rect(0, 0, toTransform.getWidth(), toTransform.getHeight()),
                new Rect(0, 0, blurred.getWidth(), blurred.getHeight()), null);

        return blurred;
    }

    private Bitmap createFadedBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        GradientDrawable radialGradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{
                        Color.argb(1f, 0f, 0f, 0f),
                        Color.argb(0.3f, 0f, 0f, 0f),
                        Color.argb(0.05f, 0f, 0f, 0f),
                        Color.argb(0f, 0f, 0f, 0f),
                });
        radialGradient.setBounds(0, 0, width, height);
        radialGradient.setGradientRadius(Math.max(width, height) / 1.5f);
        radialGradient.setGradientCenter(0.5f, 0.5f);
        radialGradient.setGradientType(GradientDrawable.RADIAL_GRADIENT);

        Canvas canvas = new Canvas(result);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        radialGradient.draw(canvas);
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return result;
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof BlurBitmapTransformation;
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