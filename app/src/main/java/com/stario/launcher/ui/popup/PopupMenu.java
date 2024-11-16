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

package com.stario.launcher.ui.popup;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
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
import android.widget.Space;

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
import com.stario.launcher.ui.measurements.Measurements;
import com.stario.launcher.utils.animation.Animation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PopupMenu {
    private static final int GENERAL_ID = 1;
    private static final int SHORTCUT_GROUP_ID = 2;
    private static final int MAX_SHORTCUT_COUNT = 4;
    private static final int PADDING = 10;
    private static final int WIDTH = 200;
    private final HashMap<Integer,
            Map.Entry<RecyclerView, RecyclerAdapter>> recyclers;
    private final ThemedActivity activity;
    private final LinearLayout root;
    private final PopupWindow popupWindow;
    private final LifecycleObserver observer;
    private int shortcutCount;

    public PopupMenu(ThemedActivity activity) {
        this.activity = activity;
        this.recyclers = new HashMap<>();
        this.shortcutCount = 0;

        LayoutInflater inflater =
                (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        this.root = (LinearLayout) inflater.inflate(R.layout.popup_window, null);
        this.popupWindow = new PopupWindow(root, Measurements.dpToPx(WIDTH),
                ViewGroup.LayoutParams.WRAP_CONTENT, true);

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

        if (recyclers.size() > 0) {
            Space space = new Space(activity);
            space.setLayoutParams((new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, padding / 2)));

            root.addView(space);
        }

        root.addView(recycler);

        Map.Entry<RecyclerView, RecyclerAdapter> entry =
                Map.entry(recycler, adapter);

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

                    if (label != null) {
                        adapter.add(new Item(label.toString(),
                                launcherApps.getShortcutIconDrawable(shortcut, Measurements.getDotsPerInch()),
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
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            activity.getLifecycle().removeObserver(observer);

            if (listener != null) {
                listener.onDismiss();
            }
        });
    }

    public void show(Activity activity, View parent) {
        show(activity, parent, null);
    }

    public void show(Activity activity, View parent, Rect margins) {
        Window window = activity.getWindow();

        if (window == null) {
            return;
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

        showAtLocation(parent, gravity, location);
    }

    public void showAtLocation(Activity activity, View parent, float x, float y) {
        Window window = activity.getWindow();

        if (window == null) {
            return;
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

        showAtLocation(parent, gravity, location);
    }

    private void showAtLocation(View parent, int gravity, @Size(2) int[] location) {
        if (popupWindow != null && !popupWindow.isShowing()) {
            root.post(() -> {
                if ((gravity & Gravity.LEFT) == Gravity.LEFT) {
                    root.setPivotX(0);
                } else if ((gravity & Gravity.RIGHT) == Gravity.RIGHT) {
                    root.setPivotX(root.getMeasuredWidth());
                }

                if (!Measurements.isLandscape()) {
                    if ((gravity & Gravity.TOP) == Gravity.TOP) {
                        root.setPivotY(0);
                    } else if ((gravity & Gravity.BOTTOM) == Gravity.BOTTOM) {
                        root.setPivotY(root.getMeasuredHeight());
                    }
                }
            });

            popupWindow.showAtLocation(parent, gravity, location[0], location[1]);
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

            activity.getLifecycle().addObserver(observer);

            activity.requestIgnoreCurrentTouchEvent(false);
        }
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
