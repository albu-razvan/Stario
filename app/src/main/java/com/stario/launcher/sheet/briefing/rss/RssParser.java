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

import com.apptasticsoftware.rssreader.Item;

import java.util.stream.Stream;

public class RssParser {
    private static RssReader reader;

    private RssParser() {
    }

    public static WoodstoxAbstractRssReader.CancellableStreamFuture<Item> futureParse(String rss) {
        if (reader == null) {
            reader = new RssReader();
        }

        return reader.readAsync(rss);
    }

    public static Stream<Item> parse(String rss) {
        if (reader == null) {
            reader = new RssReader();
        }

        return reader.read(rss);
    }
}
