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

package com.stario.launcher.utils;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.core.view.WindowInsetsCompat;

import com.stario.launcher.ui.keyboard.ImeAnimationController;
import com.stario.launcher.ui.keyboard.KeyboardHeightProvider;

import java.util.List;

public class KeyboardAnimationHelper {
    public static void configureKeyboardAnimator(@NonNull View root,
                                                 @NonNull KeyboardHeightProvider heightProvider,
                                                 @NonNull ContentTranslationListener listener) {
        configureKeyboardAnimator(root, heightProvider, null, listener);
    }

    public static void configureKeyboardAnimator(@NonNull View root,
                                                 @NonNull KeyboardHeightProvider heightProvider,
                                                 @Nullable ImeAnimationController controller,
                                                 @NonNull ContentTranslationListener listener) {
        ViewCompat.setWindowInsetsAnimationCallback(root, new WindowInsetsAnimationCompat
                .Callback(WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_STOP) {
            private WindowInsetsAnimationCompat imeAnimation;
            private boolean pendingTransition;
            private float startBottom;
            private float endBottom;
            private boolean running;

            {
                this.running = false;
                this.pendingTransition = true;
                this.startBottom = 0;
                this.endBottom = 0;

                heightProvider.addKeyboardHeightListener((height) -> {
                    if (!running &&
                            !pendingTransition) { // fix initial calculation flicker
                        // sometimes, detecting the keyboard height rakes longer than the actual animation
                        // dev tools -> animation scale 0
                        listener.translate(-height);
                    } else {
                        endBottom = height;
                    }
                });
            }

            @NonNull
            @Override
            public WindowInsetsAnimationCompat.BoundsCompat onStart(@NonNull WindowInsetsAnimationCompat animation, @NonNull WindowInsetsAnimationCompat.BoundsCompat bounds) {
                startBottom = UiUtils.isKeyboardVisible(root) ? 0 : heightProvider.getKeyboardHeight();
                endBottom = startBottom > 0 ? 0 : heightProvider.getKeyboardHeight();
                imeAnimation = null;
                pendingTransition = false;
                running = true;

                return bounds;
            }

            @NonNull
            @Override
            public WindowInsetsCompat onProgress(@NonNull WindowInsetsCompat insets, @NonNull List<WindowInsetsAnimationCompat> runningAnimations) {
                if (controller != null && controller.isAnimationInProgress()) {
                    float delta = endBottom - startBottom;
                    float fraction = controller.getExpandedFraction();
                    float translation = Math.abs(delta) * -fraction;

                    listener.translate(translation);

                    return insets;
                }

                if (imeAnimation == null) {
                    for (WindowInsetsAnimationCompat animation : runningAnimations) {
                        if ((animation.getTypeMask() & WindowInsetsCompat.Type.ime()) != 0) {
                            imeAnimation = animation;

                            break;
                        }
                    }
                }

                if (imeAnimation != null) {
                    float delta = endBottom - startBottom;
                    float fraction = (delta < 0 ? 1 : 0) - imeAnimation.getInterpolatedFraction();
                    float translation = delta * fraction;

                    listener.translate(translation);
                }

                return insets;
            }

            @Override
            public void onEnd(@NonNull WindowInsetsAnimationCompat animation) {
                running = false;
                imeAnimation = null;
            }
        });
    }

    public interface ContentTranslationListener {
        void translate(float translation);
    }
}
