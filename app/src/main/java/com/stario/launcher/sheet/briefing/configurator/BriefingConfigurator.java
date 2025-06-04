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
import android.text.Editable;
import android.text.TextWatcher;
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
import com.stario.launcher.sheet.briefing.BriefingFeedList;
import com.stario.launcher.sheet.briefing.feed.Feed;
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

public class BriefingConfigurator extends ActionDialog {
    private static final String TAG = "BriefingConfigurator";
    private final BriefingFeedList list;

    private WoodstoxAbstractRssReader.CancellableStreamFuture<Item> lastStreamFuture;
    private Future<?> runningTask;
    private ViewGroup contentView;
    private PulsingTextView limit;
    private LinearLayout preview;
    private TextView title;
    private EditText query;
    private Feed feed;

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

        query.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable sequence) {
            }

            @Override
            public void beforeTextChanged(CharSequence sequence, int arg1,
                                          int arg2, int arg3) {
            }

            @Override
            public void onTextChanged(CharSequence sequence, int start, int before,
                                      int count) {
                update(sequence);
            }
        });

        contentView.findViewById(R.id.add).setOnClickListener((view) -> {
            view.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.bounce_small));

            if (feed != null && feed.getTitle() != null &&
                    !feed.getTitle().isEmpty()) {
                boolean added = list.add(feed);

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

    private void update(CharSequence sequence) {
        preview.setVisibility(View.GONE);

        limit.setPulsating(true);
        limit.setText(R.string.searching);
        limit.setVisibility(View.VISIBLE);

        Resources resources = activity.getResources();

        if (sequence.length() == 0) {
            checkFeed(null);
        } else if (!Utils.isNetworkAvailable(activity)) {
            limit.setPulsating(false);
            limit.setText(resources.getString(R.string.no_connection));
            limit.setVisibility(View.VISIBLE);
        } else {
            if (sequence.length() > 0) {
                if (URLUtil.isValidUrl(sequence.toString()) &&
                        Patterns.WEB_URL.matcher(sequence).matches()) {

                    checkFeed(sequence.toString());
                } else {
                    sequence = "https://" + sequence;

                    if (URLUtil.isValidUrl(sequence.toString()) &&
                            Patterns.WEB_URL.matcher(sequence).matches()) {

                        checkFeed(sequence.toString());
                    } else {
                        limit.setPulsating(false);
                        limit.setText(resources.getString(R.string.invalid_url));
                        limit.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    }

    private void checkFeed(String query) {
        if (lastStreamFuture != null && !lastStreamFuture.isDone()) {
            lastStreamFuture.cancel(true);
        }

        if (runningTask != null && !runningTask.isDone()) {
            runningTask.cancel(true);
        }

        if (query != null) {
            runningTask = Utils.submitTask(() -> {
                try {
                    lastStreamFuture = RssParser.futureParse(query);

                    if (lastStreamFuture != null) {
                        Stream<Item> stream = lastStreamFuture
                                .get(10, TimeUnit.SECONDS);

                        if (stream != null) {
                            Optional<Item> target = stream.findFirst();

                            if (target.isPresent()) {
                                Channel channel = target.get().getChannel();

                                if (channel != null &&
                                        channel.getLink() != null) {
                                    feed = new Feed(channel.getTitle(), query);

                                    contentView.post(() -> {
                                        title.setText(feed.getTitle());
                                        preview.setVisibility(View.VISIBLE);

                                        limit.setVisibility(View.GONE);
                                    });

                                    return;
                                }
                            }
                        }
                    }

                    Document document = Jsoup.connect(query)
                            .userAgent(Utils.USER_AGENT)
                            .get();
                    Elements nodes = document.getElementsByTag("atom:link");
                    nodes.addAll(document.getElementsByTag("atom:link"));
                    nodes.addAll(document.getElementsByTag("link"));

                    for (Element node : nodes) {
                        if (node.hasAttr("type") && node.hasAttr("href") &&
                                (node.attr("type").contains("rss") || node.attr("type").contains("atom"))) {
                            checkFeed(node.attr("href"));

                            return;
                        }
                    }

                    Log.e(TAG, "checkFeed: Could not get feed from: " + query);

                    contentView.post(() -> {
                        limit.setPulsating(false);
                        limit.setText(activity.getResources().getString(R.string.invalid_rss));
                        limit.setVisibility(View.VISIBLE);
                    });
                } catch (InterruptedException exception) {
                    Log.i(TAG, "checkFeed: Parsing interrupted for: " + query);
                } catch (Exception exception) {
                    Log.e(TAG, "checkFeed: Could not get feed from: " + query);

                    contentView.post(() -> {
                        limit.setPulsating(false);
                        limit.setText(activity.getResources().getString(R.string.invalid_rss));
                        limit.setVisibility(View.VISIBLE);
                    });
                }
            });
        } else {
            feed = null;

            limit.setPulsating(false);
            limit.setText(activity.getResources().getString(R.string.invalid_rss));
            limit.setVisibility(View.VISIBLE);
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
