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

package com.stario.launcher.sheet.drawer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.apps.ProfileApplicationManager;
import com.stario.launcher.apps.ProfileManager;
import com.stario.launcher.apps.popup.ApplicationCustomizationDialog;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.icons.AdaptiveIconView;
import com.stario.launcher.ui.popup.PopupMenu;
import com.stario.launcher.ui.recyclers.async.AsyncRecyclerAdapter;
import com.stario.launcher.ui.recyclers.async.InflationType;
import com.stario.launcher.ui.utils.animation.Animation;
import com.stario.launcher.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class RecyclerApplicationAdapter
        extends AsyncRecyclerAdapter<RecyclerApplicationAdapter.ApplicationViewHolder> {
    private static final String TAG = "RecyclerApplicationAdapter";

    protected static final int ONLY_ICON_LAYOUT = 1;

    private final boolean showLabels;
    private final ThemedActivity activity;
    private final ItemTouchHelper itemTouchHelper;

    public RecyclerApplicationAdapter(ThemedActivity activity) {
        this(activity, true, null, InflationType.ASYNC);
    }

    public RecyclerApplicationAdapter(ThemedActivity activity, boolean showLabel) {
        this(activity, showLabel, null, InflationType.ASYNC);
    }

    public RecyclerApplicationAdapter(ThemedActivity activity, ItemTouchHelper itemTouchHelper) {
        this(activity, true, itemTouchHelper, InflationType.ASYNC);
    }

    public RecyclerApplicationAdapter(ThemedActivity activity, boolean showLabel,
                                      ItemTouchHelper itemTouchHelper) {
        this(activity, showLabel, itemTouchHelper, InflationType.ASYNC);
    }

    public RecyclerApplicationAdapter(ThemedActivity activity, InflationType type) {
        this(activity, true, null, type);
    }

    public RecyclerApplicationAdapter(ThemedActivity activity, boolean showLabel, InflationType type) {
        this(activity, showLabel, null, type);
    }

    public RecyclerApplicationAdapter(ThemedActivity activity,
                                      ItemTouchHelper itemTouchHelper, InflationType type) {
        this(activity, true, itemTouchHelper, type);
    }

    public RecyclerApplicationAdapter(ThemedActivity activity, boolean showLabel,
                                      ItemTouchHelper itemTouchHelper, InflationType type) {
        super(activity, type);

        this.showLabels = showLabel;
        this.activity = activity;
        this.itemTouchHelper = itemTouchHelper;

        setHasStableIds(true);
    }

    public class ApplicationViewHolder extends AsyncViewHolder {
        private AdaptiveIconView icon;
        private PopupWindow dialog;
        private View notification;
        private TextView label;

        public ApplicationViewHolder() {
            super();
        }

        public ApplicationViewHolder(int viewType) {
            super(viewType);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        protected void onInflated() {
            itemView.setHapticFeedbackEnabled(false);

            label = itemView.findViewById(R.id.label);
            icon = itemView.findViewById(R.id.icon);
            notification = itemView.findViewById(R.id.notification_dot);

            if (label != null) {
                label.setLines(getLabelLineCount());
            }

            View.OnClickListener clickListener = getOnClickListener();
            View.OnLongClickListener longClickListener = getOnLongClickListener();

            itemView.setOnTouchListener(new View.OnTouchListener() {
                private final float MOVE_SLOP = ViewConfiguration.get(activity).getScaledTouchSlop();
                private final int LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();

                private final Handler handler = new Handler(Looper.getMainLooper());

                private boolean hasPerformedLongClick;
                private Runnable longPressRunnable;
                private boolean hasFiredDragEvent;
                private boolean assumesClick;
                private float rawStartX;
                private float rawStartY;
                private float startX;
                private float startY;

                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN: {
                            assumesClick = true;
                            hasFiredDragEvent = false;
                            hasPerformedLongClick = false;

                            startX = event.getX();
                            startY = event.getY();
                            rawStartX = event.getRawX();
                            rawStartY = event.getRawY();

                            dialog = null;

                            if (longClickListener != null) {
                                icon.animate().scaleY(AdaptiveIconView.MAX_SCALE)
                                        .scaleX(AdaptiveIconView.MAX_SCALE)
                                        .setInterpolator(new DecelerateInterpolator())
                                        .setDuration(LONG_PRESS_TIMEOUT);
                            }

                            longPressRunnable = () -> {
                                hasPerformedLongClick = true;

                                if (longClickListener != null) {
                                    Vibrations.getInstance().vibrate();
                                    longClickListener.onLongClick(itemView);
                                }
                            };

                            handler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT);

                            break;
                        }

                        case MotionEvent.ACTION_MOVE: {
                            if (!isAClick(startX, event.getX(), startY, event.getY())) {
                                assumesClick = false;
                            }

                            if (Math.abs(rawStartX - event.getRawX()) > MOVE_SLOP ||
                                    Math.abs(rawStartY - event.getRawY()) > MOVE_SLOP) {
                                handler.removeCallbacks(longPressRunnable);
                            }

                            if (itemTouchHelper != null && dialog != null &&
                                    hasPerformedLongClick && !hasFiredDragEvent) {

                                ((ViewGroup) itemView).requestDisallowInterceptTouchEvent(true);

                                if (Math.abs(rawStartX - event.getRawX()) > MOVE_SLOP ||
                                        Math.abs(rawStartY - event.getRawY()) > MOVE_SLOP) {
                                    ((ViewGroup) itemView).requestDisallowInterceptTouchEvent(false);
                                    itemTouchHelper.startDrag(ApplicationViewHolder.this);
                                    dialog.dismiss();

                                    hasFiredDragEvent = true;
                                }
                            }

                            break;
                        }

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL: {
                            handler.removeCallbacks(longPressRunnable);
                            if (longClickListener != null) {
                                icon.animate().scaleY(1)
                                        .scaleX(1)
                                        .setDuration(Animation.SHORT.getDuration());
                            }

                            if (event.getAction() == MotionEvent.ACTION_UP) {
                                if (assumesClick &&
                                        !hasPerformedLongClick &&
                                        clickListener != null &&
                                        isAClick(startX, event.getX(), startY, event.getY())) {
                                    Vibrations.getInstance().vibrate();
                                    clickListener.onClick(itemView);
                                }

                                assumesClick = false;
                            }

                            hasPerformedLongClick = false;

                            break;
                        }
                    }

                    return true;
                }

                private boolean isAClick(float startX, float endX, float startY, float endY) {
                    return Math.abs(startX - endX) < MOVE_SLOP &&
                            Math.abs(startY - endY) < MOVE_SLOP;
                }
            });
        }

        private void showPopup(LauncherApplication application) {
            LauncherApps launcherApps =
                    ((LauncherApps) activity.getSystemService(Context.LAUNCHER_APPS_SERVICE));

            PopupMenu menu = new PopupMenu(activity);

            if (Utils.isProfileAvailable(activity, application.getProfile())) {
                List<ShortcutInfo> shortcuts = getShortcutForApplication(launcherApps, application);
                menu.addShortcuts(launcherApps, shortcuts);
            }

            Resources resources = activity.getResources();

            menu.add(new PopupMenu.Item(resources.getString(R.string.app_info),
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_info, activity.getTheme()),
                    view -> {
                        List<LauncherActivityInfo> activities =
                                launcherApps.getActivityList(application.getInfo().packageName,
                                        application.getProfile());

                        if (activities.isEmpty()) {
                            return;
                        }

                        try {
                            activity.getSystemService(LauncherApps.class)
                                    .startAppDetailsActivity(activities.get(0).getComponentName(),
                                            application.getProfile(), null, null);
                        } catch (Exception exception) {
                            Log.e(TAG, "Unable to launch settings", exception);
                        }
                    }));

            if (allowApplicationStateEditing()) {
                menu.add(new PopupMenu.Item(resources.getString(R.string.customize),
                        ResourcesCompat.getDrawable(resources, R.drawable.ic_edit, activity.getTheme()),
                        view -> new ApplicationCustomizationDialog(activity, application).show()));

                menu.add(new PopupMenu.Item(resources.getString(R.string.hide),
                        ResourcesCompat.getDrawable(resources, R.drawable.ic_hide, activity.getTheme()),
                        view -> {
                            ProfileApplicationManager manager = ProfileManager.getInstance()
                                    .getProfile(application.getProfile());

                            if (manager != null) {
                                manager.hideApplication(application);
                            }
                        }));

                if (!application.systemPackage) {
                    menu.add(new PopupMenu.Item(resources.getString(R.string.uninstall),
                            ResourcesCompat.getDrawable(resources, R.drawable.ic_delete, activity.getTheme()),
                            view -> {
                                ApplicationInfo info = application.getInfo();

                                try {
                                    // https://github.com/LawnchairLauncher/lawnchair/blob/d69b89e5e1367117690580deb331ed5fb63e9068/res/values/config.xml#L25
                                    Intent intent = Intent.parseUri("#Intent;action=android.intent.action.DELETE;launchFlags=0x10800000;end", 0)
                                            .setData(Uri.fromParts("package", info.packageName, info.name))
                                            .putExtra(Intent.EXTRA_USER, application.getProfile());
                                    activity.startActivity(intent);
                                } catch (Exception exception) {
                                    Log.e(TAG, "Unable to uninstall application", exception);
                                }
                            }));
                }
            }

            ((ViewGroup) itemView).requestDisallowInterceptTouchEvent(true);
            dialog = menu.show(activity, icon,
                    new Rect(
                            Measurements.isLandscape() ?
                                    ((label != null ? label.getMeasuredWidth() : 0) -
                                            icon.getMeasuredWidth()) / 2
                                    : 0,
                            Measurements.isLandscape() ? 0
                                    : (
                                    label == null ? 0
                                            : (
                                            label.getMeasuredHeight()
                                                    * label.getLineCount()
                                                    / label.getMaxLines())
                            ) + Measurements.dpToPx(10),
                            Measurements.isLandscape() ?
                                    ((label != null ? label.getMeasuredWidth() : 0) -
                                            icon.getMeasuredWidth()) / 2
                                    : 0,
                            0),
                    Measurements.isLandscape() ?
                            PopupMenu.PIVOT_CENTER_VERTICAL
                            : PopupMenu.PIVOT_DEFAULT,
                    itemTouchHelper == null);
        }

        public View.OnLongClickListener getOnLongClickListener() {
            return view -> {
                int index = getBindingAdapterPosition();
                if (index == RecyclerView.NO_POSITION) {
                    return false;
                }

                LauncherApplication application = getApplication(index);

                if (application != LauncherApplication.FALLBACK_APP) {
                    icon.animate().scaleY(1)
                            .scaleX(1)
                            .setDuration(Animation.SHORT.getDuration());

                    showPopup(application);

                    return true;
                } else {
                    return false;
                }
            };
        }

        public View.OnClickListener getOnClickListener() {
            return view -> {
                int index = getBindingAdapterPosition();
                if (index == RecyclerView.NO_POSITION) {
                    return;
                }

                LauncherApplication application = getApplication(index);

                if (application != LauncherApplication.FALLBACK_APP) {
                    application.launch(activity);
                }
            };
        }

        public void setIcon(Drawable drawable) {
            icon.setIcon(drawable);
        }

        public void setLabel(CharSequence sequence) {
            if (label != null) {
                label.setText(sequence);
            }
        }

        public void hideLabel() {
            if (label != null) {
                label.animate().alpha(0)
                        .setDuration(Animation.SHORT.getDuration());
            }
        }

        public void showLabel() {
            if (label != null) {
                label.animate().alpha(1f)
                        .setDuration(Animation.SHORT.getDuration());
            }
        }
    }

    private List<ShortcutInfo> getShortcutForApplication(LauncherApps launcherApps,
                                                         LauncherApplication application) {
        LauncherApps.ShortcutQuery shortcutQuery = new LauncherApps.ShortcutQuery();
        shortcutQuery.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST |
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC |
                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED);

        shortcutQuery.setPackage(application.getInfo().packageName);

        try {
            return launcherApps.getShortcuts(shortcutQuery, application.getProfile());
        } catch (SecurityException exception) {
            return new ArrayList<>();
        }
    }

    @Override
    public void onBind(@NonNull ApplicationViewHolder viewHolder, int index) {
        LauncherApplication application = getApplication(index);

        if (application != LauncherApplication.FALLBACK_APP) {
            viewHolder.setLabel(application.getLabel());

            // TODO: notification dots
            if (viewHolder.notification != null) {
                viewHolder.notification.setVisibility(false ? View.VISIBLE : View.GONE);
            }

            viewHolder.icon.setApplication(application);
            viewHolder.icon.setTransitionName(application.getInfo().packageName);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (!showLabels) {
            return ONLY_ICON_LAYOUT;
        }

        return super.getItemViewType(position);
    }

    @Override
    protected int getLayout(int viewType) {
        if (viewType == ONLY_ICON_LAYOUT) {
            return R.layout.recycler_application_item_only_icon;
        }

        return R.layout.recycler_application_item;
    }

    @Override
    protected Supplier<ApplicationViewHolder> getHolderSupplier(int viewType) {
        return () -> new ApplicationViewHolder(viewType);
    }

    @Override
    public long getItemId(int position) {
        LauncherApplication application = getApplication(position);

        return application != null ? application.getInfo()
                .packageName.hashCode() : 0;
    }

    protected int getLabelLineCount() {
        return 2;
    }

    abstract protected LauncherApplication getApplication(int index);

    abstract protected boolean allowApplicationStateEditing();
}
