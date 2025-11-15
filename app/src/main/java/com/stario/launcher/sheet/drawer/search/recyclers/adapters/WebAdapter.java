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

package com.stario.launcher.sheet.drawer.search.recyclers.adapters;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.window.SplashScreen;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.stario.launcher.R;
import com.stario.launcher.exceptions.Unauthorized;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.utils.UiUtils;
import com.stario.launcher.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WebAdapter extends AbstractSearchListAdapter<WebAdapter.ViewHolder> {
    public static final String SEARCH_RESULTS = "com.stario.SEARCH_RESULTS";
    public static final String KAGI_API_KEY = "com.stario.KAGI_API_KEY";

    private final static String TAG = "com.stario.launcher.WebAdapter";
    private final static String RESULTS_URL = "https://kagi.com/api/v0/search?limit=6&q=";
    private final static Pattern BASE_URL_REGEX = Pattern.compile("^(?:https?://)?(?:www\\.)?([^/?:]+)");
    private final static Pattern PATH_REGEX = Pattern.compile("^(?:https?://)?(?:www\\.)?[^/]+(/[^/?#]+)+");

    private final SharedPreferences preferences;
    private final List<WebEntry> searchResults;
    private final ThemedActivity activity;

    private CompletableFuture<ArrayList<WebEntry>> runningTask;
    private UnauthorizedListener listener;

    public WebAdapter(ThemedActivity activity) {
        this.activity = activity;
        this.searchResults = new ArrayList<>();
        this.preferences = activity.getApplicationContext()
                .getSharedPreferences(Entry.SEARCH);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView snippet;
        private final TextView breadcrumbs;
        private final ImageView favicon;

        @SuppressLint("ClickableViewAccessibility")
        public ViewHolder(ViewGroup itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.title);
            breadcrumbs = itemView.findViewById(R.id.breadcrumbs);
            snippet = itemView.findViewById(R.id.snippet);
            favicon = itemView.findViewById(R.id.favicon);

            itemView.setHapticFeedbackEnabled(false);
        }
    }

    @Override
    public void update(String query) {
        searchResults.clear();
        notifyInternal();

        if (runningTask != null && !runningTask.isDone()) {
            runningTask.cancel(true);
        }

        boolean searchEnabled = preferences.getBoolean(SEARCH_RESULTS, false);
        String apiKey = preferences.getString(KAGI_API_KEY, null);

        if (searchEnabled && apiKey != null &&
                !apiKey.isEmpty() && query != null && !query.isEmpty()) {
            String constraint = query.toLowerCase();

            runningTask = Utils.submitTask(() -> {
                ArrayList<WebEntry> results = new ArrayList<>();

                JSONObject object;
                try {
                    object = getData(constraint, apiKey);
                } catch (Unauthorized exception) {
                    UiUtils.post(() -> {
                        if (listener != null) {
                            listener.onDenied();
                        }
                    });

                    return results;
                }

                if (object != null && object.has("data")) {
                    Object testObject = object.get("data");

                    if (testObject instanceof JSONArray) {
                        JSONArray queryResults = (JSONArray) testObject;

                        for (int index = 0; index < queryResults.length(); index++) {
                            Object testEntry = queryResults.get(index);

                            if (testEntry instanceof JSONObject) {
                                JSONObject entry = (JSONObject) testEntry;

                                String url = null;
                                String title = null;
                                String snippet = null;

                                if (entry.has("url")) {
                                    Object testUrl = entry.get("url");

                                    if (testUrl instanceof String) {
                                        url = (String) testUrl;
                                    }
                                }

                                if (entry.has("title")) {
                                    Object testTitle = entry.get("title");

                                    if (testTitle instanceof String) {
                                        title = (String) testTitle;
                                    }
                                }

                                if (entry.has("snippet")) {
                                    Object testSnippet = entry.get("snippet");

                                    if (testSnippet instanceof String) {
                                        snippet = (String) testSnippet;
                                    }
                                }

                                if (url != null && title != null) {
                                    results.add(new WebEntry(url, title, snippet));
                                }
                            }
                        }
                    }
                }

                return results;
            });

            runningTask.thenApply(results -> {
                UiUtils.post(() -> {
                    searchResults.clear();

                    for (WebEntry entry : results) {
                        searchResults.add(0, entry);
                    }

                    notifyInternal();

                    runningTask = null;
                });

                return results;
            });
        }
    }

    private JSONObject getData(String query, String key) throws Unauthorized {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(RESULTS_URL + query).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bot " + key);
            connection.addRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");

            if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 400) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                return new JSONObject(reader.lines().collect(Collectors.joining("\n")));
            } else if (connection.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw new Unauthorized();
            }
        } catch (IOException | JSONException exception) {
            Log.e(TAG, "getData: ", exception);
        }

        return null;
    }

    @Override
    public boolean submit() {
        return true;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup container, int viewType) {
        return new ViewHolder((ViewGroup) LayoutInflater.from(activity)
                .inflate(R.layout.search_item, container, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int index) {
        WebEntry entry = searchResults.get(index);

        viewHolder.title.setText(Html.fromHtml(entry.title, Html.FROM_HTML_MODE_LEGACY));

        if (entry.snippet != null) {
            viewHolder.snippet.setText(Html.fromHtml(entry.snippet, Html.FROM_HTML_MODE_LEGACY));
            viewHolder.snippet.setVisibility(View.VISIBLE);
        } else {
            viewHolder.snippet.setVisibility(View.GONE);
        }

        if (entry.url != null) {
            viewHolder.favicon.setVisibility(View.GONE);

            Matcher baseMatcher = BASE_URL_REGEX.matcher(entry.url);

            if (baseMatcher.find() && !activity.isDestroyed()) {
                Glide.with(activity)
                        .load("https://f" + (int) Math.floor(Math.random() * 9 + 1) + ".allesedv.com/32/" + baseMatcher.group(1))
                        .listener(new RequestListener<>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                                viewHolder.favicon.setVisibility(View.VISIBLE);
                                viewHolder.favicon.setImageDrawable(resource);

                                return true;
                            }
                        })
                        .into(viewHolder.favicon);

                viewHolder.breadcrumbs.setText(getBreadcrumbString(baseMatcher.group(1), entry.url));
            }

            viewHolder.itemView.setOnClickListener(view -> {
                ActivityOptions activityOptions =
                        ActivityOptions.makeClipRevealAnimation(viewHolder.itemView, 0, 0,
                                viewHolder.itemView.getWidth(), viewHolder.itemView.getHeight());

                if (Utils.isMinimumSDK(Build.VERSION_CODES.TIRAMISU)) {
                    activityOptions.setSplashScreenStyle(SplashScreen.SPLASH_SCREEN_STYLE_ICON);
                }

                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(entry.url)), activityOptions.toBundle());
            });
        }
    }

    @NonNull
    private static String getBreadcrumbString(String baseUrl, String url) {
        StringBuilder breadcrumb = new StringBuilder(baseUrl);
        Matcher pathMatcher = PATH_REGEX.matcher(url);

        if (pathMatcher.find()) {
            String path = pathMatcher.group(1);

            if (path != null) {
                for (String segment : path.split("/")) {
                    if (!segment.isEmpty()) {
                        breadcrumb.append(" > ").append(segment);
                    }
                }
            }
        }

        return breadcrumb.toString();
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    public void setUnauthorizedListener(UnauthorizedListener listener) {
        this.listener = listener;
    }

    public interface UnauthorizedListener {
        void onDenied();
    }

    private static class WebEntry {
        private final String url;
        private final String title;
        private final String snippet;
        private final int hash;

        private WebEntry(String url, String title, String snippet) {
            this.url = url;
            this.title = title;
            this.snippet = snippet;

            this.hash = Objects.hash(url, title, snippet);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
