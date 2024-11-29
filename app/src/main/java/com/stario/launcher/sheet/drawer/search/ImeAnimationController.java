/*
    Copyright (C) 2024 Răzvan Albu

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.stario.launcher.sheet.drawer.search;

import android.os.Build;
import android.os.CancellationSignal;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsAnimationControlListenerCompat;
import androidx.core.view.WindowInsetsAnimationControllerCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.stario.launcher.utils.Utils;

public class ImeAnimationController {
    private static final float SCROLL_THRESHOLD = 0.15f;
    private WindowInsetsAnimationControllerCompat insetsAnimationController;
    private CancellationSignal cancellationSignal;
    private SpringAnimation springAnimation;
    private StateListener statusListener;
    private boolean isImeShownAtStart;

    public ImeAnimationController() {
        if(!Utils.isMinimumSDK(Build.VERSION_CODES.R)) {
            throw new RuntimeException("Keyboard animation controller requires at least API 30.");
        }
    }

    public void startControlRequest(View view, StateListener listener) {
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(view);

        if (insets != null) {
            isImeShownAtStart = insets.isVisible(WindowInsetsCompat.Type.ime());
            statusListener = listener;

            cancellationSignal = new CancellationSignal();

            //noinspection deprecation
            WindowInsetsControllerCompat windowInsetsController = ViewCompat.getWindowInsetsController(view);

            if (windowInsetsController != null) {
                windowInsetsController.controlWindowInsetsAnimation(WindowInsetsCompat.Type.ime(), -1,
                        new LinearInterpolator(), cancellationSignal,
                        new WindowInsetsAnimationControlListenerCompat() {
                            @Override
                            public void onReady(@NonNull WindowInsetsAnimationControllerCompat controller, int types) {
                                insetsAnimationController = controller;
                                cancellationSignal = null;

                                if (listener != null) {
                                    listener.onReady();
                                }
                            }

                            @Override
                            public void onFinished(@NonNull WindowInsetsAnimationControllerCompat controller) {
                                if (listener != null) {
                                    listener.onFinish();
                                }

                                reset();
                            }

                            @Override
                            public void onCancelled(WindowInsetsAnimationControllerCompat controller) {
                                if (listener != null) {
                                    listener.onFinish();
                                }

                                reset();
                            }
                        }
                );
            }
        }
    }

    public int insetBy(int dy) {
        if (insetsAnimationController != null) {
            return insetTo(insetsAnimationController.getCurrentInsets().bottom - dy);
        }

        return 0;
    }

    public int insetTo(int inset) {
        if (insetsAnimationController != null) {
            int hiddenBottom = insetsAnimationController.getHiddenStateInsets().bottom;
            int shownBottom = insetsAnimationController.getShownStateInsets().bottom;

            int startBottom = isImeShownAtStart ? shownBottom : hiddenBottom;
            int endBottom = isImeShownAtStart ? hiddenBottom : shownBottom;

            int bottom = Math.max(hiddenBottom, Math.min(shownBottom, inset));

            int consumed = insetsAnimationController.getCurrentInsets().bottom - bottom;

            insetsAnimationController.setInsetsAndAlpha(
                    Insets.of(0, 0, 0, bottom),
                    1f,
                    (float) (bottom - startBottom) / (endBottom - startBottom)
            );

            return consumed;
        }

        return 0;
    }

    public boolean isAnimationInProgress() {
        return insetsAnimationController != null;
    }

    public boolean isSettleAnimationInProgress() {
        return springAnimation != null;
    }

    public boolean isRequestPending() {
        return cancellationSignal != null;
    }

    public boolean isCurrentPositionFullyHidden() {
        return isAnimationInProgress() &&
                insetsAnimationController.getCurrentInsets().bottom ==
                        insetsAnimationController.getHiddenStateInsets().bottom;
    }

    public boolean isCurrentPositionFullyShown() {
        return isAnimationInProgress() &&
                insetsAnimationController.getCurrentInsets().bottom ==
                        insetsAnimationController.getShownStateInsets().bottom;
    }

    public void cancel() {
        if (cancellationSignal != null) {
            cancellationSignal.cancel();
        }
    }

    private void finish(boolean shown) {
        statusListener.onPreFinish();
        insetsAnimationController.finish(shown);
    }

    public void finish() {
        finish(null);
    }

    public void finish(Integer velocity) {
        if (springAnimation != null) {
            springAnimation.cancel();
        }

        if (insetsAnimationController != null) {
            if (velocity != null) {
                if (velocity > 0 && isCurrentPositionFullyShown()) {
                    statusListener.onPreFinish();
                    insetsAnimationController.finish(true);
                } else if (velocity < 0 && isCurrentPositionFullyHidden()) {
                    statusListener.onPreFinish();
                    insetsAnimationController.finish(false);
                } else {
                    setVisibilityWithAnimation(velocity > 0, velocity);
                }
            } else if (isCurrentPositionFullyShown()) {
                finish(true);
            } else if (isCurrentPositionFullyHidden()) {
                finish(false);
            } else {
                if (insetsAnimationController.getCurrentFraction() >= SCROLL_THRESHOLD) {
                    setVisibilityWithAnimation(!isImeShownAtStart, null);
                } else {
                    setVisibilityWithAnimation(isImeShownAtStart, null);
                }
            }
        } else {
            cancel();
        }
    }

    private void reset() {
        cancel();

        springAnimation = null;
        insetsAnimationController = null;
        cancellationSignal = null;
        isImeShownAtStart = false;
        statusListener = null;
    }

    private void setVisibilityWithAnimation(boolean visible, Integer velocity) {
        springAnimation = new SpringAnimation(new FloatValueHolder((float) insetsAnimationController.getCurrentInsets().bottom));

        springAnimation.addUpdateListener((animation, value, vel) -> insetTo(Math.round(value)));

        springAnimation.addEndListener((animation, canceled, value, vel) -> {
            springAnimation = null;

            if (!canceled) {
                if (insetsAnimationController != null) {
                    if (isCurrentPositionFullyShown()) {
                        finish(true);
                    } else if (isCurrentPositionFullyHidden()) {
                        finish(false);
                    } else {
                        if (insetsAnimationController.getCurrentFraction() >= SCROLL_THRESHOLD) {
                            finish(isImeShownAtStart);
                        } else {
                            finish(!isImeShownAtStart);
                        }
                    }
                }
            }
        });

        if (velocity != null) {
            springAnimation.setStartVelocity(velocity);
        }

        springAnimation.animateToFinalPosition(
                visible ? (float) insetsAnimationController.getShownStateInsets().bottom :
                        (float) insetsAnimationController.getHiddenStateInsets().bottom);

        springAnimation.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY);
    }

    public interface StateListener {
        default void onReady() {
        }

        default void onPreFinish() {
        }

        default void onFinish() {
        }
    }
}
