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

package com.stario.launcher.ui.dialogs;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SearchEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.utils.Utils;

public class DialogBackgroundDimmingController
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String LOW_SPEC_KEY = "DialogBackgroundBlurController.LOW_SPEC";

    private static final String TAG = "DialogBackgroundBlurController";
    private static final int MAX_BACKGROUND_ALPHA_LOW_SPEC = 240;
    private static final int MAX_BACKGROUND_ALPHA = 190;
    private static final int MAX_BLUR_SIZE = 100;

    private final BroadcastReceiver batterySaverReceiver;
    private final SharedPreferences settings;
    private final PowerManager powerManager;
    private final ThemedActivity activity;
    private final Drawable background;

    private DimmingController dimmingController;
    private boolean hasLimitedResources;
    private Window window;

    private DialogBackgroundDimmingController(ThemedActivity activity) {
        this.background = new ColorDrawable(
                activity.getAttributeData(com.google.android.material.R.attr.colorSurfaceContainer));
        this.background.setAlpha(0);
        this.activity = activity;

        this.powerManager = activity.getSystemService(PowerManager.class);
        this.settings = activity.getApplicationContext().getSettings();
        this.hasLimitedResources = false;

        this.batterySaverReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                invalidateLowSpec();
            }
        };
    }

    public static DimmingController attach(@NonNull ThemedActivity activity,
                                           @NonNull Dialog dialog, boolean blur) {
        return attach(activity, dialog, blur, 1f);
    }

    public static DimmingController attach(@NonNull ThemedActivity activity,
                                           @NonNull Dialog dialog, boolean blur, float multiplier) {
        DialogBackgroundDimmingController controller =
                new DialogBackgroundDimmingController(activity);
        if (controller.hook(dialog)) {
            controller.dimmingController = new DimmingController() {
                private int lastBlurStep = -1;
                private float lastFactor = 0;

                @Override
                public void setFactor(float factor) {
                    if (controller.window == null) {
                        return;
                    }

                    factor = factor * multiplier;

                    updateAlpha(factor);
                    this.lastFactor = factor;

                    if (!blur || controller.hasLimitedResources) {
                        return;
                    }

                    if (Utils.isMinimumSDK(Build.VERSION_CODES.S)) {
                        int blurRadius = (int) (MAX_BLUR_SIZE * factor);

                        if (blurRadius != lastBlurStep) {
                            controller.window.setBackgroundBlurRadius(blurRadius);
                            lastBlurStep = blurRadius;
                        }
                    }
                }

                @Override
                public void invalidate() {
                    if (controller.window == null) {
                        return;
                    }

                    updateAlpha(lastFactor);

                    if (!blur || controller.hasLimitedResources) {
                        if (Utils.isMinimumSDK(Build.VERSION_CODES.S)) {
                            controller.window.setBackgroundBlurRadius(0);
                        }

                        lastBlurStep = -1;
                    }
                }

                private void updateAlpha(float factor) {
                    controller.background.setAlpha((int) ((controller.hasLimitedResources ?
                            MAX_BACKGROUND_ALPHA_LOW_SPEC : MAX_BACKGROUND_ALPHA) * factor));
                }
            };

            return controller.dimmingController;
        }

        return null;
    }

    private boolean hook(Dialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) {
            Log.w(TAG, "hook() was called too early. Are you sure the window is available?");
            return false;
        }

        Window.Callback original = window.getCallback();
        if (original instanceof WindowDimmingCallbackWrapper) {
            Log.w(TAG, "hook() has already been called before on this window. Ignoring...");
            return false;
        } else {
            this.window = window;
            window.setCallback(new WindowDimmingCallbackWrapper(original));

            if (window.getDecorView().isAttachedToWindow()) {
                attach();
            }

            return true;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (LOW_SPEC_KEY.equals(key)) {
            invalidateLowSpec();
        }
    }

    private void invalidateLowSpec() {
        hasLimitedResources = settings.getBoolean(LOW_SPEC_KEY, false)
                || powerManager.isPowerSaveMode();

        if (window == null || dimmingController == null) {
            return;
        }

        dimmingController.invalidate();
    }

    private void attach() {
        invalidateLowSpec();

        if (window != null) {
            window.setBackgroundDrawable(background);
            window.setDimAmount(0f);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        activity.registerReceiver(batterySaverReceiver,
                new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED));
        settings.registerOnSharedPreferenceChangeListener(DialogBackgroundDimmingController.this);
    }

    private void detach() {
        activity.unregisterReceiver(batterySaverReceiver);
        settings.unregisterOnSharedPreferenceChangeListener(DialogBackgroundDimmingController.this);
    }

    public interface DimmingController {
        void setFactor(@FloatRange(from = 0, to = 1) float factor);

        void invalidate();
    }

    public class WindowDimmingCallbackWrapper implements Window.Callback {
        private final Window.Callback base;

        WindowDimmingCallbackWrapper(Window.Callback base) {
            this.base = base;
        }

        @Override
        public void onAttachedToWindow() {
            attach();

            if (base != null) {
                base.onAttachedToWindow();
            }
        }

        @Override
        public void onDetachedFromWindow() {
            detach();

            if (base != null) {
                base.onDetachedFromWindow();
            }
        }

        // forward everything else to base

        @Override
        public boolean dispatchGenericMotionEvent(MotionEvent motionEvent) {
            if (base != null) {
                return base.dispatchGenericMotionEvent(motionEvent);
            }

            return false;
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent keyEvent) {
            if (base != null) {
                return base.dispatchKeyEvent(keyEvent);
            }

            return false;
        }

        @Override
        public boolean dispatchKeyShortcutEvent(KeyEvent keyEvent) {
            if (base != null) {
                return base.dispatchKeyShortcutEvent(keyEvent);
            }

            return false;
        }

        @Override
        public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
            if (base != null) {
                return base.dispatchPopulateAccessibilityEvent(accessibilityEvent);
            }

            return false;
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent motionEvent) {
            if (base != null) {
                return base.dispatchTouchEvent(motionEvent);
            }

            return false;
        }

        @Override
        public boolean dispatchTrackballEvent(MotionEvent motionEvent) {
            if (base != null) {
                return base.dispatchTrackballEvent(motionEvent);
            }

            return false;
        }

        @Override
        public void onActionModeFinished(ActionMode actionMode) {
            if (base != null) {
                base.onActionModeFinished(actionMode);
            }
        }

        @Override
        public void onActionModeStarted(ActionMode actionMode) {
            if (base != null) {
                base.onActionModeStarted(actionMode);
            }
        }

        @Override
        public void onContentChanged() {
            if (base != null) {
                base.onContentChanged();
            }
        }

        @Override
        public boolean onCreatePanelMenu(int index, @NonNull Menu menu) {
            if (base != null) {
                return base.onCreatePanelMenu(index, menu);
            }

            return false;
        }

        @Nullable
        @Override
        public View onCreatePanelView(int index) {
            if (base != null) {
                return base.onCreatePanelView(index);
            }

            return null;
        }

        @Override
        public boolean onMenuItemSelected(int index, @NonNull MenuItem menuItem) {
            if (base != null) {
                return base.onMenuItemSelected(index, menuItem);
            }

            return false;
        }

        @Override
        public boolean onMenuOpened(int index, @NonNull Menu menu) {
            if (base != null) {
                return base.onMenuOpened(index, menu);
            }

            return false;
        }

        @Override
        public void onPanelClosed(int index, @NonNull Menu menu) {
            if (base != null) {
                base.onPanelClosed(index, menu);
            }
        }

        @Override
        public boolean onPreparePanel(int index, @Nullable View view, @NonNull Menu menu) {
            if (base != null) {
                return base.onPreparePanel(index, view, menu);
            }

            return false;
        }

        @Override
        public boolean onSearchRequested() {
            if (base != null) {
                return base.onSearchRequested();
            }

            return false;
        }

        @Override
        public boolean onSearchRequested(SearchEvent searchEvent) {
            if (base != null) {
                return base.onSearchRequested(searchEvent);
            }

            return false;
        }

        @Override
        public void onWindowAttributesChanged(WindowManager.LayoutParams layoutParams) {
            if (base != null) {
                base.onWindowAttributesChanged(layoutParams);
            }
        }

        @Override
        public void onWindowFocusChanged(boolean changed) {
            if (base != null) {
                base.onWindowFocusChanged(changed);
            }
        }

        @Nullable
        @Override
        public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
            if (base != null) {
                return base.onWindowStartingActionMode(callback);
            }

            return null;
        }

        @Nullable
        @Override
        public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int index) {
            if (base != null) {
                return base.onWindowStartingActionMode(callback, index);
            }

            return null;
        }
    }
}
