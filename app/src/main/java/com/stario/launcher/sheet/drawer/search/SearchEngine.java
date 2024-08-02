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
import com.stario.launcher.themes.ThemedActivity;

public enum SearchEngine {
    GOOGLE("Google", "google.com", "/search?q=", R.drawable.ic_google),
    DUCK_DUCK_GO("DuckDuckGo", "duckduckgo.com", "/?q=", R.drawable.ic_duck),
    YANDEX("Yandex", "yandex.com", "/search/?text=", R.drawable.ic_yandex),
    BING("Bing", "bing.com", "/search?q=", R.drawable.ic_bing),
    YAHOO("Yahoo", "search.yahoo.com", "/search?p=", R.drawable.ic_yahoo),
    BRAVE("Brave", "search.brave.com", "/search?q=", R.drawable.ic_brave);
    public static final String PREFERENCE_ENTRY = "com.stario.SEARCH_ENGINE";
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
    public static SearchEngine engineFor(ThemedActivity activity) {
        SearchEngine engine = GOOGLE;

        String string = activity.getSettings()
                .getString(PREFERENCE_ENTRY, null);

        if (DUCK_DUCK_GO.url.equals(string)) {
            engine = DUCK_DUCK_GO;
        } else if (YANDEX.url.equals(string)) {
            engine = YANDEX;
        } else if (BING.url.equals(string)) {
            engine = BING;
        } else if (YAHOO.url.equals(string)) {
            engine = YAHOO;
        } else if (BRAVE.url.equals(string)) {
            engine = BRAVE;
        }

        return engine;
    }

    public void setDefaultFor(ThemedActivity activity) {
        activity.getSettings()
                .edit()
                .putString(PREFERENCE_ENTRY, url)
                .apply();
    }
}
