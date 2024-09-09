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

package com.stario.launcher.sheet.drawer.search.adapters;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.window.SplashScreen;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.apps.LauncherApplicationManager;
import com.stario.launcher.sheet.drawer.search.Searchable;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.icons.AdaptiveIconView;
import com.stario.launcher.utils.UiUtils;
import com.stario.launcher.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class OptionAdapter extends
        RecyclerView.Adapter<OptionAdapter.ViewHolder> implements Searchable {
    private static final String TAG = "com.stario.launcher.OptionAdapter";
    private static final String[] PREDEFINED_URIS = new String[]{"market://search?q=", "geo:?q="};
    private final ArrayList<OptionEntry> options;
    private final ThemedActivity activity;
    private final ViewGroup content;
    private final RecyclerView.LayoutManager layoutManager;
    private final PackageManager packageManager;
    private boolean show;
    private String query;

    public OptionAdapter(ThemedActivity activity, ViewGroup content,
                         RecyclerView.LayoutManager layoutManager) {
        this.options = new ArrayList<>();
        this.show = false;

        this.layoutManager = layoutManager;
        this.activity = activity;
        this.content = content;

        this.packageManager = activity.getPackageManager();

        setHasStableIds(true);

        LauncherApplicationManager manager = LauncherApplicationManager.getInstance();

        Utils.submitTask(() -> {
            for (String uri : PREDEFINED_URIS) {
                List<ResolveInfo> resolvers = packageManager.queryIntentActivities(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)), PackageManager.MATCH_ALL);

                for (ResolveInfo info : resolvers) {
                    LauncherApplication application = manager.get(info.activityInfo.packageName);

                    if (application != LauncherApplication.FALLBACK_APP) {
                        OptionEntry entry = new OptionEntry(application, uri);

                        if (!options.contains(entry)) {
                            options.add(entry);
                        }
                    }
                }
            }

            String[] filters = new String[]{Intent.ACTION_WEB_SEARCH, Intent.ACTION_SEARCH};

            for (String filter : filters) {
                List<ResolveInfo> resolvers = packageManager.queryIntentActivities(new Intent(filter), PackageManager.MATCH_ALL);

                for (ResolveInfo info : resolvers) {
                    LauncherApplication application = manager.get(info.activityInfo.packageName);

                    if (application != LauncherApplication.FALLBACK_APP &&
                            info.activityInfo != null &&
                            info.activityInfo.enabled && info.activityInfo.exported &&
                            !info.activityInfo.name.toLowerCase().contains("redirect") &&
                            (info.activityInfo.permission == null ||
                                    ContextCompat.checkSelfPermission(activity, info.activityInfo.permission) == PackageManager.PERMISSION_GRANTED)) {

                        OptionEntry entry = new OptionEntry(application, info.activityInfo, filter);

                        if (!options.contains(entry)) {
                            options.add(entry);
                        }
                    }
                }
            }

            UiUtils.runOnUIThread(this::notifyDataSetChangedInternal);
        });

        //TODO listen to application list changes
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView label;
        private final AdaptiveIconView icon;

        @SuppressLint("ClickableViewAccessibility")
        public ViewHolder(ViewGroup itemView) {
            super(itemView);

            label = itemView.findViewById(R.id.textView);
            icon = itemView.findViewById(R.id.icon);

            itemView.setHapticFeedbackEnabled(false);
        }
    }

    @Override
    public void update(String query) {
        this.query = query;

        if(query.length() > 0) {
            if(!show) {
                show = true;

                notifyDataSetChangedInternal();
            }
        } else {
            if(show) {
                notifyDataSetChangedInternal();

                show = false;
            }
        }
    }

    @Override
    public boolean submit() {
        View view = layoutManager.findViewByPosition(0);

        if (view != null) {
            view.callOnClick();

            return true;
        } else {
            return false;
        }
    }

    private void notifyDataSetChangedInternal() {
        LayoutTransition transition = content.getLayoutTransition();

        if (transition != null && transition.isRunning()) {
            transition.addTransitionListener(new LayoutTransition.TransitionListener() {
                @Override
                public void startTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
                }

                @Override
                public void endTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
                    notifyDataSetChanged();

                    transition.removeTransitionListener(this);
                }
            });
        } else {
            notifyDataSetChanged();
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int index) {
        OptionEntry entry = options.get(index);

        viewHolder.label.setText(activity.getResources().getString(R.string.search_on) + " " + entry.application.getLabel());

        viewHolder.icon.setIcon(entry.application.getIcon());

        viewHolder.itemView.setOnClickListener(view -> {
            ActivityOptions activityOptions =
                    ActivityOptions.makeScaleUpAnimation(viewHolder.icon, 0, 0,
                            viewHolder.icon.getWidth(), viewHolder.icon.getHeight());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activityOptions.setSplashScreenStyle(SplashScreen.SPLASH_SCREEN_STYLE_SOLID_COLOR);
            }

            Intent intent = entry.getIntent();

            Uri uri = intent.getData();
            if (uri != null) {
                intent.setData(Uri.parse(uri + query));
            } else {
                intent.putExtra(SearchManager.QUERY, query);
            }

            try {
                activity.startActivity(intent, activityOptions.toBundle());
            } catch (Exception exception) {
                Toast.makeText(activity, "Unable to launch activity", Toast.LENGTH_SHORT).show();

                Log.e(TAG, "onBindViewHolder: ", exception);
            }
        });
    }

    @Override
    public int getItemCount() {
        return show ? options.size() : 0;
    }

    @Override
    public long getItemId(int position) {
        return options.get(position).hashCode();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup container, int viewType) {
        return new ViewHolder((ViewGroup) LayoutInflater.from(activity)
                .inflate(R.layout.search_item, container, false));
    }

    private static class OptionEntry {
        private final LauncherApplication application;
        private final Intent referenceIntent;

        private OptionEntry(LauncherApplication application, ActivityInfo info, String filter) {
            this.application = application;

            referenceIntent = new Intent();
            referenceIntent.setComponent(new ComponentName(info.packageName, info.name));
            referenceIntent.setAction(filter);
        }

        public OptionEntry(LauncherApplication application, String uri) {
            this.application = application;

            referenceIntent = new Intent();
            referenceIntent.setPackage(application.getInfo().packageName);
            referenceIntent.setAction(Intent.ACTION_VIEW);
            referenceIntent.setData(Uri.parse(uri));
        }

        private Intent getIntent() {
            return new Intent(referenceIntent);
        }

        @Override
        public int hashCode() {
            return application.getInfo().hashCode();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof OptionEntry) {
                return ((OptionEntry) obj).application.equals(application);
            } else {
                return false;
            }
        }
    }
}
