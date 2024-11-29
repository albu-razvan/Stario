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

package com.stario.launcher.sheet.drawer.search.recyclers.adapters;

import android.app.ActivityOptions;
import android.content.Intent;
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

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.XMLReader;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class WebAdapter extends AbstractSearchListAdapter {
    private final static String TAG = "com.stario.launcher.WebAdapter";
    private final static int MAX_RESULTS = 4;
    private final static String TOOLBAR = "https://suggestqueries.google.com/complete/search?output=toolbar&q=";
    private final static String ENCODING = "ISO-8859-1";
    private final List<WebEntry> searchResults;
    private final ThemedActivity activity;
    private String currentQuery;

    public WebAdapter(ThemedActivity activity) {
        super(activity, true);

        this.activity = activity;
        this.searchResults = new ArrayList<>();

        setHasStableIds(true);

        currentQuery = "";
    }

    @Override
    public void update(String query) {
        if (query != null && query.length() > 0) {
            String constraint = query.toLowerCase();
            currentQuery = constraint;

            Utils.submitTask(() -> {
                try {
                    SearchEngine engine = SearchEngine.engineFor(activity);

                    ArrayList<WebEntry> results = new ArrayList<>();

                    InputSource inputSource =
                            new InputSource(new URL(TOOLBAR + constraint).openStream());
                    inputSource.setEncoding(ENCODING);

                    SAXParserFactory factory = SAXParserFactory.newInstance();
                    SAXParser saxParser = factory.newSAXParser();
                    XMLReader xmlReader = saxParser.getXMLReader();

                    xmlReader.setContentHandler(new ContentHandler() {
                        @Override
                        public void setDocumentLocator(Locator locator) {

                        }

                        @Override
                        public void startDocument() {

                        }

                        @Override
                        public void endDocument() {
                            UiUtils.runOnUIThread(() -> {
                                if (currentQuery.equals(constraint)) {
                                    searchResults.clear();

                                    for (WebEntry entry : results) {
                                        searchResults.add(0, entry);

                                        if (searchResults.size() >= MAX_RESULTS) {
                                            break;
                                        }
                                    }

                                    notifyInternal();
                                }
                            });
                        }

                        @Override
                        public void startPrefixMapping(String prefix, String uri) {

                        }

                        @Override
                        public void endPrefixMapping(String prefix) {

                        }

                        @Override
                        public void startElement(String uriString, String localName,
                                                 String queryName, Attributes attributes) {
                            String result = attributes.getValue("data");

                            if (result != null) {
                                Uri uri = Uri.parse(engine.getQuery(result));

                                if (uri != null) {
                                    results.add(new WebEntry(result, uri));
                                }
                            }
                        }

                        @Override
                        public void endElement(String uri, String localName, String qName) {
                        }

                        @Override
                        public void characters(char[] ch, int start, int length) {
                        }

                        @Override
                        public void ignorableWhitespace(char[] ch, int start, int length) {

                        }

                        @Override
                        public void processingInstruction(String target, String data) {

                        }

                        @Override
                        public void skippedEntity(String name) {

                        }
                    });

                    xmlReader.parse(inputSource);
                } catch (Exception exception) {
                    Log.e(TAG, "update: ", exception);
                }
            });
        } else {
            currentQuery = "";
            searchResults.clear();

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
                        Uri.parse(SearchEngine.engineFor(activity)
                                .getQuery(currentQuery))),
                activityOptions.toBundle());

        return true;
    }

    @NonNull
    @Override
    public AbstractSearchListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup container, int viewType) {
        AbstractSearchListAdapter.ViewHolder holder = super.onCreateViewHolder(container, viewType);

        holder.icon.setIcon(AppCompatResources.getDrawable(activity, R.drawable.ic_search));

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull AbstractSearchListAdapter.ViewHolder viewHolder, int index) {
        WebEntry entry = searchResults.get(index);

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
        return searchResults.size();
    }

    @Override
    public long getItemId(int position) {
        return searchResults.get(position).uri.hashCode();
    }

    private static class WebEntry {
        private final String label;
        private final Uri uri;

        private WebEntry(String label, Uri uri) {
            this.label = label;
            this.uri = uri;
        }
    }
}
