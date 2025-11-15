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

package com.stario.launcher.sheet.briefing.dialog.page.feed;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.Objects;

public class Feed implements Serializable {
    private static final String TAG = "com.stario.FeedItem";
    private static final String FEED_TITLE = "com.stario.FEED_TITLE";
    private static final String FEED_RSS = "com.stario.FEED_RSS";

    private final String rss;

    String title;

    public Feed(@NonNull String title, @NonNull String rss) {
        this.title = title;
        this.rss = rss;
    }

    public String getTitle() {
        return title;
    }

    public String getRSSLink() {
        return rss;
    }

    public static Feed deserialize(String data) {
        try {
            JSONObject jsonObject = new JSONObject(data);

            return new Feed(jsonObject.getString(FEED_TITLE),
                    jsonObject.getString(FEED_RSS));
        } catch (Exception exception) {
            Log.e(TAG, "deserialize: Serialized object has corrupt data.");

            return null;
        }
    }

    public String serialize() {
        if (rss.isEmpty()) {
            return null;
        } else {
            return "{" +
                    "\"" + FEED_TITLE + "\":\"" + title + "\"," +
                    "\"" + FEED_RSS + "\":\"" + rss + "\"" +
                    "}";
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        return Objects.equals(rss, ((Feed) object).rss);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, rss);
    }
}