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

package com.stario.launcher.sheet.drawer.search;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import com.stario.launcher.R;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.themes.ThemedActivity;

public enum SearchEngine {
    GOOGLE("Google", "google.com", "/search?q=", R.drawable.ic_google),
    DUCK_DUCK_GO("DuckDuckGo", "duckduckgo.com", "/?q=", R.drawable.ic_duck),
    BING("Bing", "bing.com", "/search?q=", R.drawable.ic_bing),
    BRAVE("Brave", "search.brave.com", "/search?q=", R.drawable.ic_brave),
    KAGI("Kagi", "kagi.com", "/search?q=", R.drawable.ic_kagi),
    PERPLEXITY("Perplexity AI", "perplexity.ai", "/?s=o&q=", R.drawable.ic_perplexity),
    ECOSIA("Ecosia", "ecosia.org", "/search?q=", R.drawable.ic_ecosia),
    YANDEX("Yandex", "yandex.com", "/search/?text=", R.drawable.ic_yandex),
    YAHOO("Yahoo", "search.yahoo.com", "/search?p=", R.drawable.ic_yahoo);
    public static final String SEARCH_ENGINE = "com.stario.SEARCH_ENGINE";
    private final String label;
    private final String url;
    private final String query;
    private final int drawable;

    SearchEngine(String label, String url, String query, int drawable) {
        this.label = label;
        this.url = url;
        this.query = query;
        this.drawable = drawable;
    }

    public String getUrl() {
        return url;
    }

    public String getQuery(String query) {
        return "https://" + url + this.query + query;
    }

    public Drawable getDrawable(Context context) {
        return AppCompatResources.getDrawable(context, drawable);
    }

    @NonNull
    @Override
    public String toString() {
        return label;
    }

    // Defaults to GOOGLE
    public static SearchEngine getEngine(ThemedActivity activity) {
        SearchEngine engine = GOOGLE;

        String string = activity.getSharedPreferences(Entry.SEARCH)
                .getString(SEARCH_ENGINE, null);

        if (DUCK_DUCK_GO.url.equals(string)) {
            engine = DUCK_DUCK_GO;
        } else if (YANDEX.url.equals(string)) {
            engine = YANDEX;
        } else if (BING.url.equals(string)) {
            engine = BING;
        } else if (YAHOO.url.equals(string)) {
            engine = YAHOO;
        } else if (ECOSIA.url.equals(string)) {
            engine = ECOSIA;
        } else if (PERPLEXITY.url.equals(string)) {
            engine = PERPLEXITY;
        } else if (KAGI.url.equals(string)) {
            engine = KAGI;
        } else if (BRAVE.url.equals(string)) {
            engine = BRAVE;
        }

        return engine;
    }

    public static void setEngine(ThemedActivity activity, SearchEngine engine) {
        activity.getSharedPreferences(Entry.SEARCH)
                .edit()
                .putString(SEARCH_ENGINE, engine.url)
                .apply();
    }
}
