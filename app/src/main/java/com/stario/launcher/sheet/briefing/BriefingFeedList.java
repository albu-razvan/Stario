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

package com.stario.launcher.sheet.briefing;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.stario.launcher.preferences.Entry;
import com.stario.launcher.sheet.briefing.feed.Feed;
import com.stario.launcher.themes.ThemedActivity;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class BriefingFeedList {
    private static final String FEEDS_KEY = "com.stario.FEEDS";
    private static BriefingFeedList instance = null;

    private final List<FeedListener> listeners;
    private final SharedPreferences state;
    private final List<Feed> items;

    private BriefingFeedList(ThemedActivity activity) {
        this.items = new ArrayList<>();
        this.listeners = new ArrayList<>();
        this.state = activity.getSharedPreferences(Entry.BRIEFING);

        load(state.getString(FEEDS_KEY, null));
    }

    public static BriefingFeedList from(@NonNull ThemedActivity activity) {
        if (instance == null) {
            instance = new BriefingFeedList(activity);
        }

        return instance;
    }

    public static BriefingFeedList getInstance() {
        if (instance == null) {
            throw new RuntimeException("BriefingFeedList not initialized.");
        }

        return instance;
    }

    public Feed get(int position) {
        return items.get(position);
    }

    public int size() {
        return items.size();
    }

    private void load(String feedsSerial) {
        if (feedsSerial == null) {
            return;
        }

        try {
            JSONArray array = new JSONArray(feedsSerial);

            for (int index = 0; index < array.length(); index++) {
                Feed feed = Feed.deserialize((String) array.get(index));

                if (feed != null && !items.contains(feed)) {
                    items.add(feed);
                    for (FeedListener listener : listeners) {
                        listener.onInserted(size() - 1);
                    }
                }
            }
        } catch (Exception exception) {
            Log.e("BriefingFeedList", "Error loading feeds.", exception);

            state.edit()
                    .remove(FEEDS_KEY)
                    .apply();
        }
    }

    public boolean add(Feed feed) {
        if (feed == null || items.contains(feed)) {
            return false;
        }

        items.add(feed);

        ArrayList<String> serials = new ArrayList<>();
        for (Feed item : items) {
            serials.add(item.serialize());
        }

        state.edit().putString(FEEDS_KEY,
                new JSONArray(serials).toString()).apply();

        for (FeedListener listener : listeners) {
            listener.onInserted(size() - 1);
        }

        return true;
    }

    public void remove(int position) {
        items.remove(position);

        ArrayList<String> serials = new ArrayList<>();
        for (Feed item : items) {
            serials.add(item.serialize());
        }

        state.edit().putString(FEEDS_KEY,
                new JSONArray(serials).toString()).apply();

        for (FeedListener listener : listeners) {
            listener.onRemoved(position);
        }
    }

    public void remove(Feed feed) {
        int index = items.indexOf(feed);

        remove(index);
    }

    public void addOnFeedUpdateListener(FeedListener listener) {
        if (listener != null) {
            this.listeners.add(listener);
        }
    }

    public void removeOnFeedUpdateListener(FeedListener listener) {
        if (listener != null) {
            this.listeners.remove(listener);
        }
    }

    public interface FeedListener {
        default void onInserted(int index) {
        }

        default void onRemoved(int index) {
        }
    }
}
