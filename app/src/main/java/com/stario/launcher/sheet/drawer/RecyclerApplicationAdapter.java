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
import android.net.Uri;
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

import com.stario.launcher.R;
import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.apps.popup.ApplicationCustomizationDialog;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.icons.AdaptiveIconView;
import com.stario.launcher.ui.popup.PopupMenu;
import com.stario.launcher.ui.recyclers.async.AsyncRecyclerAdapter;
import com.stario.launcher.ui.recyclers.async.InflationType;
import com.stario.launcher.ui.utils.animation.Animation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class RecyclerApplicationAdapter
        extends AsyncRecyclerAdapter<RecyclerApplicationAdapter.ViewHolder> {
    private static final String TAG = "RecyclerApplicationAdapter";

    private final ThemedActivity activity;
    private final ItemTouchHelper itemTouchHelper;

    public RecyclerApplicationAdapter(ThemedActivity activity) {
        this(activity, null);
    }

    public RecyclerApplicationAdapter(ThemedActivity activity, ItemTouchHelper itemTouchHelper) {
        this(activity, itemTouchHelper, InflationType.ASYNC);
    }

    public RecyclerApplicationAdapter(ThemedActivity activity,
                                      ItemTouchHelper itemTouchHelper, InflationType type) {
        super(activity, type);

        this.activity = activity;
        this.itemTouchHelper = itemTouchHelper;

        setHasStableIds(true);
    }

    public class ViewHolder extends AsyncViewHolder {
        private boolean hasPerformedLongClick;
        private AdaptiveIconView icon;
        private PopupWindow dialog;
        private View notification;
        private TextView label;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        protected void onInflated() {
            label = itemView.findViewById(R.id.textView);
            icon = itemView.findViewById(R.id.icon);
            notification = itemView.findViewById(R.id.notification_dot);

            ViewGroup.LayoutParams params = icon.getLayoutParams();
            params.width = Measurements.getIconSize();
            params.height = Measurements.getIconSize();

            label.setLines(getLabelLineCount());

            itemView.setHapticFeedbackEnabled(false);

            itemView.setOnClickListener(view -> {
                LauncherApplication application = getApplication(getAbsoluteAdapterPosition());

                if (application != LauncherApplication.FALLBACK_APP) {
                    application.launch(activity);
                }
            });

            itemView.setOnTouchListener(new View.OnTouchListener() {
                private final float moveSlop = ViewConfiguration.get(activity).getScaledTouchSlop();

                private boolean hasFiredDragEvent;

                private float x;
                private float y;

                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN: {
                            dialog = null;
                            hasFiredDragEvent = false;
                            hasPerformedLongClick = false;
                            x = event.getRawX();
                            y = event.getRawY();

                            icon.animate().scaleY(AdaptiveIconView.MAX_SCALE)
                                    .scaleX(AdaptiveIconView.MAX_SCALE)
                                    .setInterpolator(new DecelerateInterpolator())
                                    .setDuration(ViewConfiguration.getLongPressTimeout());

                            break;
                        }

                        case MotionEvent.ACTION_MOVE: {
                            if (itemTouchHelper != null && dialog != null &&
                                    hasPerformedLongClick && !hasFiredDragEvent) {

                                ((ViewGroup) itemView).requestDisallowInterceptTouchEvent(true);

                                if (Math.abs(x - event.getRawX()) > moveSlop ||
                                        Math.abs(y - event.getRawY()) > moveSlop) {
                                    ((ViewGroup) itemView).requestDisallowInterceptTouchEvent(false);
                                    itemTouchHelper.startDrag(ViewHolder.this);
                                    dialog.dismiss();

                                    hasFiredDragEvent = true;
                                }
                            }

                            break;
                        }

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL: {
                            hasPerformedLongClick = false;

                            icon.animate().scaleY(1)
                                    .scaleX(1)
                                    .setDuration(Animation.SHORT.getDuration());

                            break;
                        }
                    }

                    return false;
                }
            });

            itemView.setOnLongClickListener(view -> {
                hasPerformedLongClick = true;

                LauncherApplication application = getApplication(getAbsoluteAdapterPosition());

                if (application != LauncherApplication.FALLBACK_APP) {
                    Vibrations.getInstance().vibrate();

                    icon.animate().scaleY(1)
                            .scaleX(1)
                            .setDuration(Animation.SHORT.getDuration());

                    showPopup(application);

                    return true;
                } else {
                    return false;
                }
            });
        }

        private void showPopup(LauncherApplication application) {
            LauncherApps launcherApps =
                    ((LauncherApps) activity.getSystemService(Context.LAUNCHER_APPS_SERVICE));

            PopupMenu menu = new PopupMenu(activity);

            List<ShortcutInfo> shortcuts = getShortcutForApplication(launcherApps, application);
            menu.addShortcuts(launcherApps, shortcuts);

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

            menu.setOnDismissListener(() -> {
                icon.setScaleX(1);
                icon.setScaleY(1);
            });

            ((ViewGroup) itemView).requestDisallowInterceptTouchEvent(true);
            dialog = menu.show(activity, icon,
                    new Rect(Measurements.isLandscape() ? (label.getMeasuredWidth() - icon.getMeasuredWidth()) / 2 : 0,
                            Measurements.isLandscape() ? 0 : label.getMeasuredHeight() * label.getLineCount() / label.getMaxLines() + Measurements.dpToPx(10),
                            Measurements.isLandscape() ? (label.getMeasuredWidth() - icon.getMeasuredWidth()) / 2 : 0, 0),
                    Measurements.isLandscape() ? PopupMenu.PIVOT_CENTER_VERTICAL : PopupMenu.PIVOT_DEFAULT,
                    itemTouchHelper == null);
        }

        public void focus() {
            this.itemView.bringToFront();
            this.label.animate().alpha(0)
                    .setDuration(Animation.SHORT.getDuration());
        }

        public void clearFocus() {
            this.label.animate().alpha(1f)
                    .setDuration(Animation.SHORT.getDuration());
        }
    }

    private static List<ShortcutInfo> getShortcutForApplication(LauncherApps launcherApps,
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
    public void onBind(@NonNull ViewHolder viewHolder, int index) {
        LauncherApplication application = getApplication(index);

        if (application != LauncherApplication.FALLBACK_APP) {
            viewHolder.label.setText(application.getLabel());
            // TODO: notification dots
            viewHolder.notification.setVisibility(false ? View.VISIBLE : View.GONE);

            viewHolder.icon.setApplication(application);
            viewHolder.icon.setTransitionName(DrawerAdapter.SHARED_ELEMENT_PREFIX + index);

            viewHolder.itemView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected int getLayout() {
        return R.layout.list_item;
    }

    @Override
    protected Supplier<ViewHolder> getHolderSupplier() {
        return ViewHolder::new;
    }

    @Override
    public long getItemId(int position) {
        return getApplication(position)
                .getInfo()
                .packageName.hashCode();
    }

    protected int getLabelLineCount() {
        return 2;
    }

    abstract protected LauncherApplication getApplication(int index);

    abstract protected boolean allowApplicationStateEditing();
}
