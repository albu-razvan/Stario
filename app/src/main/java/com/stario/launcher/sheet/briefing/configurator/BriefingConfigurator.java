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

package com.stario.launcher.sheet.briefing.configurator;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.apptasticsoftware.rssreader.Channel;
import com.apptasticsoftware.rssreader.Item;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.stario.launcher.R;
import com.stario.launcher.sheet.briefing.dialog.page.feed.BriefingFeedList;
import com.stario.launcher.sheet.briefing.dialog.page.feed.Feed;
import com.stario.launcher.sheet.briefing.rss.RssParser;
import com.stario.launcher.sheet.briefing.rss.WoodstoxAbstractRssReader;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.common.text.PulsingTextView;
import com.stario.launcher.ui.dialogs.ActionDialog;
import com.stario.launcher.ui.utils.UiUtils;
import com.stario.launcher.utils.Utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import carbon.view.SimpleTextWatcher;

public class BriefingConfigurator extends ActionDialog {
    private static final String TAG = "BriefingConfigurator";
    private static final long DEBOUNCE_DELAY = 300;

    private final BriefingFeedList list;

    private volatile Future<?> currentSearchTask;

    private Runnable debounceRunnable;
    private ViewGroup contentView;
    private PulsingTextView limit;
    private LinearLayout preview;
    private Feed validatedFeed;
    private TextView title;
    private EditText query;

    public BriefingConfigurator(@NonNull ThemedActivity activity) {
        super(activity);

        this.list = BriefingFeedList.from(activity);
    }

    @Override
    protected @NonNull View inflateContent(LayoutInflater inflater) {
        contentView = (ViewGroup) inflater.inflate(R.layout.briefing_configurator, null);

        query = contentView.findViewById(R.id.query);
        preview = contentView.findViewById(R.id.preview);
        title = contentView.findViewById(R.id.title);
        limit = contentView.findViewById(R.id.limit);

        query.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(@NonNull CharSequence sequence,
                                      int start, int before, int count) {
                String text = sequence.toString();

                if (debounceRunnable != null) {
                    UiUtils.removeUICallback(debounceRunnable);
                }

                if (text.isEmpty()) {
                    showStatus(null, false);

                    return;
                }

                String validUrl = null;
                if (isValidUrl(text)) {
                    validUrl = text;
                } else {
                    String potentialUrl = "https://" + text;

                    if (isValidUrl(potentialUrl)) {
                        validUrl = potentialUrl;
                    }
                }

                if (validUrl == null) {
                    showStatus(R.string.invalid_url, false);

                    return;
                }

                if (!Utils.isNetworkAvailable(activity)) {
                    showStatus(R.string.no_connection, false);

                    return;
                }

                final String finalValidUrl = validUrl;
                debounceRunnable = () -> startSearch(finalValidUrl);
                UiUtils.postDelayed(debounceRunnable, DEBOUNCE_DELAY);
            }
        });

        contentView.findViewById(R.id.add).setOnClickListener((view) -> {
            view.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.bounce_small));

            if (validatedFeed != null && validatedFeed.getTitle() != null &&
                    !validatedFeed.getTitle().isEmpty()) {
                boolean added = list.add(validatedFeed);

                if (added) {
                    BottomSheetBehavior<?> behavior = getBehavior();
                    behavior.setDraggable(false);
                    behavior.setState(BottomSheetBehavior.STATE_HIDDEN);

                    UiUtils.hideKeyboard(contentView);
                } else {
                    Toast.makeText(activity,
                            R.string.already_subscribed, Toast.LENGTH_LONG).show();
                }
            }
        });

        contentView.findViewById(R.id.paste).setOnClickListener((v) -> {
            ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);

            if (clipboard.hasPrimaryClip()) {
                ClipData clip = clipboard.getPrimaryClip();

                if (clip != null) {
                    if (clip.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                        String text = clip.getItemAt(0)
                                .coerceToText(activity).toString();

                        if (!text.isEmpty()) {
                            query.setText(text);
                            query.setSelection(text.length());
                        }
                    }
                }
            }
        });

        return contentView;
    }

    @Override
    public void show() {
        if (query != null) {
            query.setText(null);
        }

        super.show();
    }

    private void startSearch(String query) {
        if (currentSearchTask != null && !currentSearchTask.isDone()) {
            currentSearchTask.cancel(true);
        }

        showStatus(R.string.searching, true);
        currentSearchTask = Utils.submitTask(new FeedDiscoveryTask(query));
    }

    private boolean isValidUrl(String url) {
        return URLUtil.isValidUrl(url) && Patterns.WEB_URL.matcher(url).matches();
    }

    private void showStatus(Integer messageRes, boolean pulsating) {
        Resources resources = activity.getResources();

        if (preview != null) {
            preview.setVisibility(View.GONE);
        }

        if (limit != null) {
            if (messageRes != null) {
                limit.setText(resources.getString(messageRes));
                limit.setPulsating(pulsating);
                limit.setVisibility(View.VISIBLE);
            } else {
                limit.setVisibility(View.GONE);
            }
        }

        validatedFeed = null;
    }

    private void showPreview(@NonNull Feed feed) {
        validatedFeed = feed;

        title.setText(feed.getTitle());
        preview.setVisibility(View.VISIBLE);
        limit.setVisibility(View.GONE);
    }

    private class FeedDiscoveryTask implements Runnable {
        private final String initialUrl;

        private FeedDiscoveryTask(String url) {
            this.initialUrl = url;
        }

        @Override
        public void run() {
            try {
                Feed feed = attemptParse(initialUrl);

                if (feed != null) {
                    contentView.post(() -> showPreview(feed));

                    return;
                }

                // after a network task, check for interruption
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }

                String discoveredFeedUrl = discoverFeedUrl(initialUrl);

                if (discoveredFeedUrl != null) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }

                    Feed discoveredFeed = attemptParse(discoveredFeedUrl);
                    if (discoveredFeed != null) {
                        contentView.post(() -> showPreview(discoveredFeed));

                        return;
                    }
                }

                contentView.post(() -> showStatus(R.string.invalid_rss, false));

            } catch (Exception exception) {
                Log.e(TAG, "Failed for: " + initialUrl, exception);

                contentView.post(() -> showStatus(R.string.invalid_rss, false));
            }
        }

        private Feed attemptParse(String url) {
            WoodstoxAbstractRssReader.CancellableStreamFuture<Item> streamFuture = null;

            try {
                streamFuture = RssParser.futureParse(url);
                Stream<Item> stream = streamFuture.get(10, TimeUnit.SECONDS);

                if (stream != null) {
                    Optional<Item> target = stream.findFirst();

                    if (target.isPresent()) {
                        Channel channel = target.get().getChannel();

                        if (channel != null && channel.getTitle() != null) {
                            return new Feed(channel.getTitle(), url);
                        }
                    }
                }
            } catch (Exception exception) {
                if (streamFuture != null) {
                    streamFuture.cancel(true);
                }
            }

            return null;
        }

        private String discoverFeedUrl(String url) throws Exception {
            Document document = Jsoup.connect(url)
                    .userAgent(Utils.USER_AGENT)
                    .get();

            Elements nodes = document.select("link[type*=\"rss\"], link[type*=\"atom\"]");

            for (Element node : nodes) {
                if (node.hasAttr("href")) {
                    return node.attr("abs:href");
                }
            }

            return null;
        }
    }

    @Override
    protected boolean blurBehind() {
        return true;
    }

    @Override
    protected int getDesiredInitialState() {
        return BottomSheetBehavior.STATE_EXPANDED;
    }
}
