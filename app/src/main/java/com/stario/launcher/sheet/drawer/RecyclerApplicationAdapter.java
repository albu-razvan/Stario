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

package com.stario.launcher.sheet.drawer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Process;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.stario.launcher.R;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.sheet.drawer.apps.LauncherApplication;
import com.stario.launcher.sheet.drawer.dialog.ApplicationsDialog;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.icons.AdaptiveIconView;
import com.stario.launcher.ui.measurements.Measurements;
import com.stario.launcher.ui.popup.PopupMenu;
import com.stario.launcher.ui.recyclers.async.AsyncRecyclerAdapter;
import com.stario.launcher.utils.animation.Animation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class RecyclerApplicationAdapter
        extends AsyncRecyclerAdapter<RecyclerApplicationAdapter.ViewHolder> {
    private final ThemedActivity activity;

    public RecyclerApplicationAdapter(ThemedActivity activity) {
        super(activity);

        this.activity = activity;

        setHasStableIds(true);
    }

    public class ViewHolder extends AsyncViewHolder {
        public TextView label;
        private AdaptiveIconView icon;
        private View notification;
        private boolean longClicked;

        public ViewHolder() {
            longClicked = false;
        }

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
                    application.launch(activity, icon);
                }
            });

            itemView.setOnTouchListener((view, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        longClicked = false;

                        icon.animate().scaleY(AdaptiveIconView.MAX_SCALE)
                                .scaleX(AdaptiveIconView.MAX_SCALE)
                                .setDuration(ViewConfiguration.getLongPressTimeout());

                        break;
                    }

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL: {
                        if (!longClicked) {
                            icon.animate().scaleY(1)
                                    .scaleX(1)
                                    .setDuration(Animation.MEDIUM.getDuration());
                        }

                        break;
                    }
                }

                return false;
            });

            itemView.setOnLongClickListener(view -> {
                LauncherApplication application = getApplication(getAbsoluteAdapterPosition());

                if (application != LauncherApplication.FALLBACK_APP) {
                    Vibrations.getInstance().vibrate();

                    icon.setScaleY(AdaptiveIconView.MAX_SCALE);
                    icon.setScaleX(AdaptiveIconView.MAX_SCALE);

                    showPopup(icon, application);

                    longClicked = true;

                    return true;
                } else {
                    return false;
                }
            });
        }
    }

    private void showPopup(AdaptiveIconView iconView, LauncherApplication application) {
        LauncherApps launcherApps =
                ((LauncherApps) activity.getSystemService(Context.LAUNCHER_APPS_SERVICE));

        PopupMenu menu = new PopupMenu(activity);

        List<ShortcutInfo> shortcuts = getShortcutForApplication(launcherApps, application);
        menu.addShortcuts(launcherApps, shortcuts);

        Resources resources = activity.getResources();

        menu.add(new PopupMenu.Item(resources.getString(R.string.app_info),
                ResourcesCompat.getDrawable(resources, R.drawable.ic_info, activity.getTheme()),
                view -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + application.getInfo().packageName));

                    activity.startActivity(intent);
                }));

        if (!application.systemPackage) {
            menu.add(new PopupMenu.Item(resources.getString(R.string.uninstall),
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_delete, activity.getTheme()),
                    view -> {
                        Intent intent = new Intent(Intent.ACTION_DELETE);
                        intent.setData(Uri.parse("package:" + application.getInfo().packageName));

                        LocalBroadcastManager.getInstance(activity)
                                .sendBroadcastSync(new Intent(ApplicationsDialog.UNINSTALL_BROADCAST));

                        activity.startActivity(intent);
                    }));
        }

        menu.setOnDismissListener(() -> iconView.animate().scaleX(1)
                .scaleY(1).setDuration(Animation.MEDIUM.getDuration()));

        menu.show(activity, iconView);
    }

    private static List<ShortcutInfo> getShortcutForApplication(LauncherApps launcherApps,
                                                                LauncherApplication application) {
        LauncherApps.ShortcutQuery shortcutQuery = new LauncherApps.ShortcutQuery();
        shortcutQuery.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST |
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC |
                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED);

        shortcutQuery.setPackage(application.getInfo().packageName);

        try {
            return launcherApps.getShortcuts(shortcutQuery, Process.myUserHandle());
        } catch (SecurityException exception) {
            return new ArrayList<>();
        }
    }

    @Override
    public void onBind(@NonNull ViewHolder viewHolder, int index) {
        LauncherApplication application = getApplication(index);

        if (application != LauncherApplication.FALLBACK_APP) {
            viewHolder.label.setText(application.getLabel());
            viewHolder.notification.setVisibility(
                    application.getNotificationCount() > 0 ? View.VISIBLE : View.GONE);

            Drawable appIcon = application.getIcon();

            if (appIcon != null) {
                viewHolder.icon.setIcon(appIcon);

                viewHolder.icon.setTransitionName(DrawerAdapter.SHARED_ELEMENT_PREFIX + index);
            }

            viewHolder.itemView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return getSize();
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

    abstract protected int getSize();
}
