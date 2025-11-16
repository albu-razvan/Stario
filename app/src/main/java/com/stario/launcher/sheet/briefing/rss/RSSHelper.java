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

package com.stario.launcher.sheet.briefing.rss;

import android.util.Log;

import com.prof18.rssparser.RssParser;
import com.prof18.rssparser.RssParserBuilder;
import com.prof18.rssparser.model.RssChannel;
import com.prof18.rssparser.model.RssItem;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RSSHelper {
    private static final String TAG = "RSSHelper";
    private static RssParser reader;

    private RSSHelper() {
    }

    public static @NotNull CompletableFuture<@NotNull RssChannel> futureParse(String url) {
        if (reader == null) {
            reader = new RssParserBuilder().build();
        }

        return RSSHelperKt.parseFeed(reader, url);
    }

    public static List<RssItem> parse(String url) {
        if (reader == null) {
            reader = new RssParserBuilder().build();
        }

        try {
            RssChannel channel = RSSHelperKt.parseFeed(reader, url).get();

            return channel.getItems();
        } catch (Exception exception) {
            Log.e(TAG, "parse: ", exception);
        }

        return null;
    }
}
