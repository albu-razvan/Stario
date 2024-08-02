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

package com.stario.launcher.sheet.briefing.feed;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.Serializable;

public class Feed implements Serializable, Comparable<Feed> {
    private static final String TAG = "com.stario.FeedItem";
    private static final String FEED_TITLE = "com.stario.FEED_TITLE";
    private static final String FEED_RSS = "com.stario.FEED_RSS";
    private static final String FEED_POSITION = "com.stario.FEED_POSITION";
    public static final int NO_POSITION = -1;
    private final String title;
    private final String rss;
    private int position;

    public Feed(String title, @NonNull String rss) {
        this.title = title;
        this.rss = rss;
        this.position = NO_POSITION;
    }

    private Feed(String title, String rss, int position) {
        this.title = title;
        this.rss = rss;
        this.position = position;
    }

    public String getTitle() {
        return title;
    }

    public String getRSSLink() {
        return rss;
    }

    public int getListPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public static Feed deserialize(String data) {
        try {
            JSONObject jsonObject = new JSONObject(data);

            return new Feed(jsonObject.getString(FEED_TITLE),
                    jsonObject.getString(FEED_RSS),
                    jsonObject.getInt(FEED_POSITION));
        } catch (Exception exception) {
            Log.e(TAG, "deserialize: Serialized object has corrupt data.");

            return null;
        }
    }

    public String serialize() {
        if (position == NO_POSITION ||
                title == null || title.isEmpty() ||
                rss == null || rss.isEmpty()) {
            return null;
        } else {
            return "{" +
                    "\"" + FEED_TITLE + "\":\"" + title + "\"," +
                    "\"" + FEED_RSS + "\":\"" + rss + "\"," +
                    "\"" + FEED_POSITION + "\":" + position +
                    "}";
        }
    }

    @Override
    public int compareTo(Feed feed) {
        return feed != null ? position - feed.getListPosition() : -1;
    }
}