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

package com.stario.launcher.sheet.briefing;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.stario.launcher.preferences.Entry;
import com.stario.launcher.sheet.briefing.feed.Feed;
import com.stario.launcher.themes.ThemedActivity;

import java.util.ArrayList;
import java.util.List;

public class BriefingFeedList {
    private static BriefingFeedList instance = null;
    private final SharedPreferences state;
    private final List<Feed> items;
    private FeedListener listener;

    private BriefingFeedList(ThemedActivity activity) {
        this.items = new ArrayList<>();
        this.state = activity.getSharedPreferences(Entry.BRIEFING);

        state.getAll()
                .forEach((rssLink, serial) -> {
                    if (serial instanceof String) {
                        Feed feed = Feed.deserialize((String) serial);

                        if (feed != null) {
                            add(feed);
                        } else {
                            state.edit()
                                    .remove(rssLink)
                                    .apply();
                        }
                    } else {
                        state.edit()
                                .remove(rssLink)
                                .apply();
                    }
                });
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

    public boolean add(Feed feed) {
        if (feed.getListPosition() != Feed.NO_POSITION) {
            for (int index = 0; index < size(); index++) {
                int comparison = items.get(index).compareTo(feed);

                if (comparison == 0) {
                    state.edit()
                            .remove(feed.getRSSLink())
                            .apply();

                    return false;
                } else if (comparison > 0) {
                    items.add(index, feed);

                    if (listener != null) {
                        listener.onInserted(index);
                    }

                    return true;
                }
            }
        } else if (state.contains(feed.getRSSLink())) {
            return false;
        }

        feed.setPosition(size() > 0 ? get(size() - 1).getListPosition() + 1 : 0);
        items.add(feed);

        state.edit()
                .putString(feed.getRSSLink(), feed.serialize())
                .apply();

        if (listener != null) {
            listener.onInserted(items.size() - 1);
        }

        return true;
    }

    public void swap(int first, int second) {
        Feed firstFeed = items.get(first);
        Feed secondFeed = items.get(second);

        firstFeed.setPosition(second);
        secondFeed.setPosition(first);

        items.set(first, secondFeed);
        items.set(second, firstFeed);

        state.edit()
                .putString(firstFeed.getRSSLink(), firstFeed.serialize())
                .putString(secondFeed.getRSSLink(), secondFeed.serialize())
                .apply();

        if (listener != null) {
            listener.onMoved(first, second);
        }
    }

    public void remove(int position) {
        Feed feed = items.remove(position);

        state.edit()
                .remove(feed.getRSSLink())
                .apply();

        if (listener != null) {
            listener.onRemoved(position);
        }
    }

    public void remove(Feed feed) {
        int index = items.indexOf(feed);

        remove(index);
    }

    public void setOnFeedUpdateListener(FeedListener listener) {
        if (listener != null) {
            this.listener = listener;
        }
    }

    public interface FeedListener {
        default void onInserted(int index) {
        }

        default void onRemoved(int index) {
        }

        default void onMoved(int first, int second) {
        }
    }
}
