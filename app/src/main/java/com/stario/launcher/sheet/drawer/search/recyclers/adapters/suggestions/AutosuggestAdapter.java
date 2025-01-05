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

package com.stario.launcher.sheet.drawer.search.recyclers.adapters.suggestions;

import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.ViewGroup;
import android.window.SplashScreen;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import com.stario.launcher.R;
import com.stario.launcher.sheet.drawer.search.SearchEngine;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.utils.UiUtils;
import com.stario.launcher.utils.Utils;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AutosuggestAdapter extends SuggestionSearchAdapter {
    private final static String TAG = "com.stario.launcher.WebAdapter";
    private final static int MAX_RESULTS = 5;
    private final static String AUTOSUGGEST_URL = "https://kagi.com/api/autosuggest?q=";
    private final List<SuggestionEntry> suggestionResults;
    private final ThemedActivity activity;

    private CompletableFuture<ArrayList<SuggestionEntry>> runningTask;
    private String currentQuery;

    public AutosuggestAdapter(ThemedActivity activity) {
        super(activity, true);

        this.activity = activity;
        this.suggestionResults = new ArrayList<>();
        this.currentQuery = "";

        setHasStableIds(true);
    }

    @Override
    public void update(String query) {
        if (runningTask != null && !runningTask.isDone()) {
            runningTask.cancel(true);
        }

        if (query != null && !query.isEmpty()) {
            SearchEngine engine = SearchEngine.getEngine(activity);
            String constraint = query.toLowerCase();
            currentQuery = constraint;

            runningTask = Utils.submitTask(() -> {
                ArrayList<SuggestionEntry> results = new ArrayList<>();

                try {
                    URLConnection connection = new URL(AUTOSUGGEST_URL + constraint).openConnection();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                    JSONArray root = new JSONArray(reader.lines().collect(Collectors.joining("\n")));
                    if (root.length() > 1) {
                        Object tester = root.get(1);

                        if (tester instanceof JSONArray) {
                            JSONArray target = (JSONArray) tester;

                            for (int index = 0; index < target.length() &&
                                    index < MAX_RESULTS; index++) {
                                String result = target.getString(index);

                                Uri uri = Uri.parse(engine.getQuery(result));

                                if (uri != null) {
                                    results.add(new SuggestionEntry(result, uri));
                                }
                            }
                        }
                    }
                } catch (Exception exception) {
                    Log.e(TAG, "update: ", exception);
                }

                return results;
            });

            runningTask.thenApply(results -> {
                UiUtils.runOnUIThread(() -> {
                    if (currentQuery.equals(constraint)) {
                        suggestionResults.clear();

                        for (SuggestionEntry entry : results) {
                            suggestionResults.add(0, entry);
                        }

                        notifyInternal();
                    }

                    runningTask = null;
                });

                return results;
            });
        } else {
            currentQuery = "";
            suggestionResults.clear();

            notifyInternal();
        }
    }

    @Override
    public boolean submit() {
        ActivityOptions activityOptions =
                ActivityOptions.makeBasic();

        if (Utils.isMinimumSDK(Build.VERSION_CODES.TIRAMISU)) {
            activityOptions.setSplashScreenStyle(SplashScreen.SPLASH_SCREEN_STYLE_ICON);
        }

        activity.startActivity(
                new Intent(Intent.ACTION_VIEW,
                        Uri.parse(SearchEngine.getEngine(activity)
                                .getQuery(currentQuery))),
                activityOptions.toBundle());

        return true;
    }

    @NonNull
    @Override
    public SuggestionSearchAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup container, int viewType) {
        SuggestionSearchAdapter.ViewHolder holder = super.onCreateViewHolder(container, viewType);

        holder.icon.setLooseClipping(false);
        holder.icon.setIcon(new LayerDrawable(new Drawable[]{
                new ColorDrawable(activity.getAttributeData(com.google.android.material.R.attr.colorSecondaryContainer)),
                AppCompatResources.getDrawable(activity, R.drawable.ic_search)
        }));

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestionSearchAdapter.ViewHolder viewHolder, int index) {
        SuggestionEntry entry = suggestionResults.get(index);

        viewHolder.itemView.setOnClickListener(view -> {
            ActivityOptions activityOptions =
                    ActivityOptions.makeScaleUpAnimation(viewHolder.icon, 0, 0,
                            viewHolder.icon.getWidth(), viewHolder.icon.getHeight());

            if (Utils.isMinimumSDK(Build.VERSION_CODES.TIRAMISU)) {
                activityOptions.setSplashScreenStyle(SplashScreen.SPLASH_SCREEN_STYLE_ICON);
            }

            activity.startActivity(new Intent(Intent.ACTION_VIEW, entry.uri), activityOptions.toBundle());
        });

        viewHolder.label.setText(entry.label);
    }

    @Override
    public int getItemCount() {
        return suggestionResults.size();
    }

    @Override
    public long getItemId(int position) {
        return suggestionResults.get(position).hashCode();
    }

    private static class SuggestionEntry {
        private final String label;
        private final Uri uri;
        private final int hash;

        private SuggestionEntry(String label, Uri uri) {
            this.label = label;
            this.uri = uri;

            this.hash = Objects.hash(label, uri);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
