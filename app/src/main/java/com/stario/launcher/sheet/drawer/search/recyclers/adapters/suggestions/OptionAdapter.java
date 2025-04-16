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

package com.stario.launcher.sheet.drawer.search.recyclers.adapters.suggestions;

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
import android.view.View;
import android.widget.Toast;
import android.window.SplashScreen;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.apps.LauncherApplicationManager;
import com.stario.launcher.apps.ProfileApplicationManager;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.utils.UiUtils;
import com.stario.launcher.utils.Utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class OptionAdapter extends SuggestionSearchAdapter {
    private static final String TAG = "com.stario.launcher.OptionAdapter";
    private static final String[] PREDEFINED_URIS = new String[]{"market://search?q=", "geo:?q="};

    private final ProfileApplicationManager.ApplicationListener listener;
    private final ProfileApplicationManager applicationManager;
    private final ArrayList<OptionEntry> options;
    private final ThemedActivity activity;
    private final PackageManager packageManager;

    private RecyclerView recyclerView;
    private boolean show;
    private String query;

    public OptionAdapter(ThemedActivity activity) {
        super(activity, false);

        this.options = new ArrayList<>();
        this.show = false;

        this.activity = activity;

        this.applicationManager = LauncherApplicationManager.getInstance()
                .getProfile(null);
        this.packageManager = activity.getPackageManager();

        Utils.submitTask(() -> {
            for (String uri : PREDEFINED_URIS) {
                List<ResolveInfo> resolvers = packageManager.queryIntentActivities(
                        new Intent(Intent.ACTION_VIEW, Uri.parse(uri)), PackageManager.MATCH_ALL
                );

                for (ResolveInfo info : resolvers) {
                    LauncherApplication application = applicationManager.get(info.activityInfo.packageName);

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
                    LauncherApplication application = applicationManager.get(info.activityInfo.packageName);

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

            UiUtils.runOnUIThread(this::notifyInternal);
        });

        listener = new ProfileApplicationManager.ApplicationListener() {
            private void insert(LauncherApplication application) {
                recyclerView.post(() -> {
                    String[] filters = new String[]{Intent.ACTION_WEB_SEARCH, Intent.ACTION_SEARCH};

                    for (String filter : filters) {
                        Intent intent = new Intent(filter);
                        intent.setPackage(application.getInfo().packageName);

                        List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);

                        if (!resolveInfo.isEmpty()) {
                            options.add(new OptionEntry(application, resolveInfo.get(0).activityInfo, filter));

                            notifyInternal();
                        }
                    }
                });
            }

            private void remove(LauncherApplication application) {
                recyclerView.post(() -> {
                    Iterator<OptionEntry> iterator = options.iterator();

                    while (iterator.hasNext()) {
                        OptionEntry entry = iterator.next();

                        if (entry.application.equals(application)) {
                            iterator.remove();

                            return;
                        }
                    }
                });
            }

            @Override
            public void onInserted(LauncherApplication application) {
                insert(application);
            }

            @Override
            public void onShowed(LauncherApplication application) {
                insert(application);
            }

            @Override
            public void onRemoved(LauncherApplication application) {
                remove(application);
            }

            @Override
            public void onHidden(LauncherApplication application) {
                remove(application);
            }

            @Override
            public void onUpdated(LauncherApplication application) {
                recyclerView.post(() -> {
                    for (int index = 0; index < options.size(); index++) {
                        OptionEntry entry = options.get(index);

                        if (entry.application.equals(application)) {
                            notifyItemChanged(index);

                            return;
                        }
                    }
                });
            }
        };
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void update(String query) {
        this.query = query;

        if (!query.isEmpty()) {
            if (!show) {
                show = true;

                invalidateRecyclerVisibility();
            }
        } else {
            if (show) {
                show = false;

                invalidateRecyclerVisibility();
            }
        }
    }

    @Override
    public boolean submit() {
        if (recyclerView != null &&
                recyclerView.getLayoutManager() != null &&
                recyclerView.getVisibility() == View.VISIBLE) {
            View view = recyclerView.getLayoutManager()
                    .findViewByPosition(0);

            if (view != null) {
                view.callOnClick();

                return true;
            } else {
                return false;
            }
        }

        return false;
    }

    @Override
    public void onBindViewHolder(SuggestionSearchAdapter.ViewHolder viewHolder, int index) {
        OptionEntry entry = options.get(index);

        viewHolder.label.setText(activity.getResources().getString(R.string.search_on) + " " + entry.application.getLabel());

        viewHolder.icon.setApplication(entry.application);

        viewHolder.itemView.setOnClickListener(view -> {
            ActivityOptions activityOptions =
                    ActivityOptions.makeScaleUpAnimation(viewHolder.icon, 0, 0,
                            viewHolder.icon.getWidth(), viewHolder.icon.getHeight());

            if (Utils.isMinimumSDK(Build.VERSION_CODES.TIRAMISU)) {
                activityOptions.setSplashScreenStyle(SplashScreen.SPLASH_SCREEN_STYLE_SOLID_COLOR);
            }

            Intent intent = entry.getIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

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
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        this.recyclerView = recyclerView;

        if (listener != null) {
            applicationManager.addApplicationListener(listener);
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);

        if (listener != null) {
            applicationManager.removeApplicationListener(listener);
        }

        this.recyclerView = null;
    }

    @Override
    public int getItemCount() {
        return show ? options.size() : 0;
    }

    @Override
    public long getItemId(int position) {
        return options.get(position).hashCode();
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
