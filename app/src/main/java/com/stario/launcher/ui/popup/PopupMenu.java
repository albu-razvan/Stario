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

package com.stario.launcher.ui.popup;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.VectorDrawable;
import android.transition.Transition;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Size;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.transition.platform.MaterialElevationScale;
import com.stario.launcher.R;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.utils.animation.Animation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PopupMenu {
    public static final short PIVOT_DEFAULT = 0b00;
    public static final short PIVOT_CENTER_VERTICAL = 0b01;
    public static final short PIVOT_CENTER_HORIZONTAL = 0b10;
    private static final int GENERAL_ID = 1;
    private static final int SHORTCUT_GROUP_ID = 2;
    private static final int MAX_SHORTCUT_COUNT = 4;
    private static final float INSET_FRACTION = 0.2f;
    private static final int PADDING = 10;
    private static final int WIDTH = 200;
    private final HashMap<Integer,
            Map.Entry<RecyclerView, RecyclerAdapter>> recyclers;
    private final ThemedActivity activity;
    private final LinearLayout root;
    private final PopupWindow popupWindow;
    private final LifecycleObserver observer;
    private int oldOrientationFlags;
    private int shortcutCount;

    public PopupMenu(ThemedActivity activity) {
        this.activity = activity;
        this.recyclers = new HashMap<>();
        this.shortcutCount = 0;

        LayoutInflater inflater =
                (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        this.root = (LinearLayout) inflater.inflate(R.layout.popup_window, null);
        this.popupWindow = new PopupWindow(root, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);

        popupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);

        Transition enter = new MaterialElevationScale(true);
        enter.setDuration(Animation.SHORT.getDuration());
        enter.setInterpolator(new DecelerateInterpolator());

        popupWindow.setEnterTransition(enter);

        Transition exit = new MaterialElevationScale(false);
        exit.setDuration(Animation.SHORT.getDuration());
        exit.setInterpolator(new AccelerateInterpolator());

        popupWindow.setExitTransition(exit);

        setOnDismissListener(null);

        this.observer = new DefaultLifecycleObserver() {
            @Override
            public void onPause(@NonNull LifecycleOwner owner) {
                popupWindow.dismiss();
            }
        };
    }

    private Map.Entry<RecyclerView, RecyclerAdapter> getRecycler(int identifier) {
        if (recyclers.containsKey(identifier)) {
            return recyclers.get(identifier);
        }

        RecyclerView recycler = new RecyclerView(activity);
        RecyclerAdapter adapter = new RecyclerAdapter(popupWindow, activity);

        recycler.setOverScrollMode(View.OVER_SCROLL_NEVER);

        int padding = Measurements.dpToPx(PADDING);

        recycler.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        recycler.setPadding(padding, 0, padding, 0);

        recycler.setLayoutManager(new LinearLayoutManager(activity));
        recycler.setBackground(AppCompatResources.getDrawable(activity, R.drawable.popup_background));
        recycler.setAdapter(adapter);

        Map.Entry<RecyclerView, RecyclerAdapter> entry = Map.entry(recycler, adapter);
        recyclers.put(identifier, entry);

        return entry;
    }

    public void addShortcuts(LauncherApps launcherApps, List<ShortcutInfo> shortcuts) {
        if (shortcuts.size() == 0) {
            return;
        }

        RecyclerAdapter adapter = getRecycler(SHORTCUT_GROUP_ID).getValue();

        if (adapter != null) {
            for (int index = 0; index < shortcuts.size() &&
                    shortcutCount < MAX_SHORTCUT_COUNT; index++) {
                ShortcutInfo shortcut = shortcuts.get(index);

                if (shortcut != null) {
                    CharSequence label = shortcut.getShortLabel();
                    Drawable icon = launcherApps.getShortcutIconDrawable(shortcut, Measurements.getDotsPerInch());

                    int width = Math.max(1, icon.getIntrinsicWidth());
                    int height = Math.max(1, icon.getIntrinsicHeight());

                    int paddingHorizontal = (Math.max(width, height) - width) / 2;
                    int paddingVertical = (Math.max(width, height) - height) / 2;

                    Bitmap bitmap = Bitmap.createBitmap(width + 2 * paddingHorizontal, height + 2 * paddingVertical, Bitmap.Config.ARGB_8888);

                    Canvas canvas = new Canvas(bitmap);
                    icon.setBounds(paddingHorizontal, paddingVertical,
                            width + paddingHorizontal, height + paddingVertical);
                    icon.draw(canvas);

                    if (icon instanceof BitmapDrawable || icon instanceof VectorDrawable) {
                        int threshold = bitmap.getWidth() / 10;

                        int centerX, centerY, target;
                        centerX = centerY = target = bitmap.getWidth() / 2;

                        while (target > threshold &&
                                Color.alpha(bitmap.getPixel(centerX, target)) == 255 &&
                                Color.alpha(bitmap.getPixel(target, centerY)) == 255 &&
                                Color.alpha(bitmap.getPixel(centerX, width - target)) == 255 &&
                                Color.alpha(bitmap.getPixel(height - target, centerY)) == 255 &&
                                Color.alpha(bitmap.getPixel(centerX / 2 + target, centerY / 2 + target)) == 255 &&
                                Color.alpha(bitmap.getPixel(centerY / 2 + target, centerX / 2 + target)) == 255) {
                            target -= 2;
                        }

                        if (target <= threshold) {
                            icon = new BitmapDrawable(activity.getResources(), bitmap);
                        } else {
                            icon = new LayerDrawable(new Drawable[]{new ColorDrawable(Color.WHITE),
                                    new InsetDrawable(new BitmapDrawable(activity.getResources(), bitmap), INSET_FRACTION)});
                        }
                    } else {
                        icon = new BitmapDrawable(activity.getResources(), bitmap);
                    }

                    if (label != null) {
                        adapter.add(new Item(label.toString(), icon,
                                (view) -> launcherApps.startShortcut(shortcut, null, null)));

                        shortcutCount++;
                    }
                }
            }
        }
    }

    public void add(Item item) {
        RecyclerAdapter adapter = getRecycler(GENERAL_ID).getValue();

        adapter.add(item);
    }

    public void add(List<Item> items) {
        for (Item item : items) {
            add(item);
        }
    }

    public void setOnDismissListener(PopupWindow.OnDismissListener listener) {
        popupWindow.setOnDismissListener(() -> {
            activity.setRequestedOrientation(oldOrientationFlags);
            activity.getLifecycle().removeObserver(observer);

            if (listener != null) {
                listener.onDismiss();
            }
        });
    }

    public PopupWindow show(Activity activity, View parent, short pivotAxis) {
        return show(activity, parent, null, pivotAxis);
    }

    public PopupWindow show(Activity activity, View parent, short pivotAxis, boolean interceptTouches) {
        return show(activity, parent, null, pivotAxis, interceptTouches);
    }

    public PopupWindow show(Activity activity, View parent, Rect margins, short pivotAxis) {
        return show(activity, parent, margins, pivotAxis, false);
    }

    public PopupWindow show(Activity activity, View parent, Rect margins,
                                         short pivotAxis, boolean interceptTouches) {
        Window window = activity.getWindow();

        if (window == null) {
            return null;
        }

        int[] location = new int[2];
        parent.getLocationInWindow(location);

        int width = parent.getMeasuredWidth();
        int height = parent.getMeasuredHeight();

        int gravity = Gravity.NO_GRAVITY;

        if (window.getDecorView().getWidth() / 2 < location[0] + width / 2) {
            gravity = gravity | Gravity.RIGHT;

            location[0] = window.getDecorView().getWidth() -
                    location[0] - (int) (parent.getWidth() * parent.getScaleX());

            if (margins != null && Measurements.isLandscape()) {
                location[0] += margins.right;
            }
        } else {
            gravity = gravity | Gravity.LEFT;

            if (margins != null && Measurements.isLandscape()) {
                location[0] += margins.left;
            }
        }

        if (Measurements.isLandscape()) {
            location[0] += (int) (parent.getWidth() * parent.getScaleX()) +
                    Measurements.dpToPx(PADDING);
        }

        if (window.getDecorView().getHeight() / 2 > location[1] + height / 2) {
            gravity = gravity | Gravity.TOP;

            if (!Measurements.isLandscape()) {
                location[1] += (int) (parent.getHeight() * parent.getScaleY())
                        + Measurements.dpToPx(PADDING);

                if (margins != null) {
                    location[1] += margins.top;
                }
            }
        } else {
            gravity = gravity | Gravity.BOTTOM;

            location[1] = window.getDecorView().getHeight() -
                    location[1];

            if (!Measurements.isLandscape()) {
                location[1] += Measurements.dpToPx(PADDING);

                if (margins != null) {
                    location[1] -= margins.bottom;
                }
            } else {
                location[1] -= (int) (parent.getHeight() * parent.getScaleY());
            }
        }

        root.post(() -> root.setPadding(0, 0, 0, 0));

        return showAtLocation(parent, location, 0, gravity, pivotAxis, interceptTouches);
    }

    public PopupWindow showAtLocation(Activity activity, View parent, float x, float y, short pivotAxis) {
        Window window = activity.getWindow();

        if (window == null) {
            return null;
        }

        int[] location = new int[2];
        parent.getLocationInWindow(location);

        int width = parent.getMeasuredWidth();
        int height = parent.getMeasuredHeight();

        int gravity = Gravity.NO_GRAVITY;

        if (width / 2f < x) {
            gravity = gravity | Gravity.RIGHT;

            location[0] = window.getDecorView().getWidth() -
                    location[0] - (int) x - Measurements.dpToPx(WIDTH) / 2;
        } else {
            gravity = gravity | Gravity.LEFT;

            location[0] = location[0] + (int) x -
                    Measurements.dpToPx(WIDTH) / 2;
        }

        if (height / 2f > y) {
            gravity = gravity | Gravity.TOP;

            location[1] += y;
        } else {
            gravity = gravity | Gravity.BOTTOM;

            location[1] = window.getDecorView().getHeight() -
                    location[1] - (int) y;
        }

        return showAtLocation(parent, location, Measurements.dpToPx(PADDING * 2),
                gravity, pivotAxis, true);
    }

    private PopupWindow showAtLocation(View parent, @Size(2) int[] location, int padding,
                                                    int gravity, short pivotAxis, boolean interceptTouches) {
        for (Map.Entry<RecyclerView, RecyclerAdapter> entry : recyclers.values()) {
            if ((gravity & Gravity.TOP) == Gravity.TOP) { // flip options when popup expands upwards
                root.addView(entry.getKey(), 0);
            } else {
                root.addView(entry.getKey());
            }
        }

        if (popupWindow != null && !popupWindow.isShowing()) {
            root.post(() -> {
                if ((pivotAxis & PIVOT_CENTER_HORIZONTAL) == PIVOT_DEFAULT) {
                    if ((gravity & Gravity.LEFT) == Gravity.LEFT) {
                        root.setPivotX(0);
                    } else if ((gravity & Gravity.RIGHT) == Gravity.RIGHT) {
                        root.setPivotX(root.getMeasuredWidth());
                    }
                }

                if ((pivotAxis & PIVOT_CENTER_VERTICAL) == PIVOT_DEFAULT) {
                    if ((gravity & Gravity.TOP) == Gravity.TOP) {
                        root.setPivotY(0);
                    } else if ((gravity & Gravity.BOTTOM) == Gravity.BOTTOM) {
                        root.setPivotY(root.getMeasuredHeight());
                    }
                }
            });

            popupWindow.setWidth(Measurements.dpToPx(WIDTH) + padding * 2);
            popupWindow.showAtLocation(parent, gravity, location[0], location[1]);
            root.setPadding(padding, padding, padding, padding);

            oldOrientationFlags = activity.getRequestedOrientation();

            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            activity.getLifecycle().addObserver(observer);

            if (interceptTouches) {
                activity.requestIgnoreCurrentTouchEvent(false);
            }

            return popupWindow;
        }

        return null;
    }

    public static class Item {
        public final String label;
        public final Drawable icon;
        public final View.OnClickListener listener;

        public Item(String label, Drawable icon, View.OnClickListener listener) {
            this.label = label;
            this.icon = icon;
            this.listener = listener;
        }
    }
}
