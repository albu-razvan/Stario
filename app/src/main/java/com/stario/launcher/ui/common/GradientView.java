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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stario.launcher.themes.ThemedActivity;

public class GradientView extends WebView {
    private boolean loaded;

    public GradientView(@NonNull Context context) {
        super(context);

        init(context);
    }

    public GradientView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public GradientView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void init(Context context) {
        if (!(context instanceof ThemedActivity)) {
            throw new RuntimeException("GradientView can only be instantiated from a ThemedActivity context.");
        }

        loaded = false;
        ThemedActivity activity = (ThemedActivity) context;

        setClickable(false);
        setFocusable(false);
        setFocusableInTouchMode(false);

        setBackgroundColor(Color.TRANSPARENT);

        WebSettings settings = getSettings();
        settings.setOffscreenPreRaster(true);
        settings.setJavaScriptEnabled(true);
        settings.setDatabaseEnabled(false);
        settings.setAllowFileAccess(false);
        settings.setSupportZoom(false);

        // I refuse to rewrite the whole thing to work on an android canvas
        // Thank you Stripe and Kevin Hufnagl <3
        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                int surfaceContainer = activity.getAttributeData(com.google.android.material.R.attr.colorSurfaceContainer);
                int surfaceContainerHigh = activity.getAttributeData(com.google.android.material.R.attr.colorSurfaceContainerHigh);
                int primaryContainer = activity.getAttributeData(com.google.android.material.R.attr.colorPrimaryContainer);

                // @formatter:off
                view.loadUrl(
                        "javascript: (function () {\n" +
                                "    var parent = document.getElementsByTagName('head').item(0);" +
                                "    var style = document.createElement('style');" +
                                "    style.type = 'text/css';" +
                                "    style.innerHTML = '" +
                                "canvas {" +
                                "   --gradient-color-1: #" + Integer.toHexString(surfaceContainer & 0x00FFFFFF) + ";" +
                                "   --gradient-color-2: #" + Integer.toHexString(surfaceContainer & 0x00FFFFFF) + ";" +
                                "   --gradient-color-3: #" + Integer.toHexString(primaryContainer & 0x00FFFFFF) + ";" +
                                "   --gradient-color-4: #" + Integer.toHexString(surfaceContainerHigh & 0x00FFFFFF) + ";" +
                                "}" +
                                "    ';" +
                                "    parent.appendChild(style)" +
                                "})()"
                );
                // @formatter:on

                super.onPageFinished(view, url);
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        if (!loaded) {
            loadUrl("file:///android_res/raw/gradient.html");
            requestLayout();
            loaded = true;
        }

        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // in case the view just changes parent
        post(() -> {
            if (!isAttachedToWindow()) {
                clearHistory();
                removeAllViews();
                destroy();

                loaded = false;
            }
        });
    }
}
