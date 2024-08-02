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

package com.stario.launcher.sheet.briefing.configurator;

import android.content.res.Resources;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.core.widget.NestedScrollView;

import com.apptasticsoftware.rssreader.Channel;
import com.apptasticsoftware.rssreader.Item;
import com.google.android.material.button.MaterialButton;
import com.stario.launcher.R;
import com.stario.launcher.sheet.briefing.BriefingFeedList;
import com.stario.launcher.sheet.briefing.feed.Feed;
import com.stario.launcher.sheet.briefing.rss.Parser;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.dialogs.ActionDialog;
import com.stario.launcher.ui.keyboard.KeyboardHeightProvider;
import com.stario.launcher.ui.measurements.Measurements;
import com.stario.launcher.utils.Utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.InterruptedIOException;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public class BriefingConfigurator extends ActionDialog {
    private static final String TAG = "BriefingConfigurator";
    private final BriefingFeedList list;
    private KeyboardHeightProvider heightProvider;
    private NestedScrollView scroller;
    private Future<?> runningTask;
    private ViewGroup contentView;
    private LinearLayout preview;
    private TextView title;
    private TextView limit;
    private EditText query;
    private Feed feed;

    public BriefingConfigurator(@NonNull ThemedActivity activity) {
        super(activity);

        this.list = BriefingFeedList.from(activity);
    }

    @Override
    protected @NonNull View inflateContent(LayoutInflater inflater) {
        contentView = (ViewGroup) inflater.inflate(R.layout.briefing_configurator, null);

        MaterialButton add = contentView.findViewById(R.id.add);
        scroller = contentView.findViewById(R.id.scroller);
        query = contentView.findViewById(R.id.query);
        preview = contentView.findViewById(R.id.preview);
        title = contentView.findViewById(R.id.title);
        limit = contentView.findViewById(R.id.limit);

        heightProvider = new KeyboardHeightProvider(activity);
        heightProvider.setKeyboardHeightObserver((height) -> {
            scroller.setPadding(0, 0, 0, Measurements.getNavHeight() + height);
        });

        Measurements.addNavListener(value ->
                scroller.setPadding(0, 0, 0, value + heightProvider.getKeyboardHeight()));

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
                if (sequence.length() == 0) {
                    checkFeed(null);
                } else {
                    Resources resources = activity.getResources();

                    if (sequence.length() > 0) {
                        if (URLUtil.isValidUrl(sequence.toString()) &&
                                Patterns.WEB_URL.matcher(sequence).matches()) {

                            if (Utils.isNetworkAvailable(activity))
                                checkFeed(sequence.toString());
                            else {
                                preview.setVisibility(View.GONE);
                                limit.setText(resources.getString(R.string.no_connection));
                                limit.setVisibility(View.VISIBLE);
                            }
                        } else {
                            sequence = "https://" + sequence;

                            if (URLUtil.isValidUrl(sequence.toString()) &&
                                    Patterns.WEB_URL.matcher(sequence).matches()) {

                                if (Utils.isNetworkAvailable(activity)) {
                                    checkFeed(sequence.toString());
                                } else {
                                    preview.setVisibility(View.GONE);
                                    limit.setText(resources.getString(R.string.no_connection));
                                    limit.setVisibility(View.VISIBLE);
                                }
                            } else {
                                preview.setVisibility(View.GONE);
                                limit.setText(resources.getString(R.string.invalid_url));
                                limit.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                }
            }
        });

        add.setOnClickListener((view) -> {
            view.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.bounce_small));

            if (feed != null &&
                    feed.getTitle() != null &&
                    feed.getTitle().length() > 0) {
                boolean added = list.add(feed);

                if (added) {
                    dismiss();
                } else {
                    Toast.makeText(activity,
                            R.string.already_subscribed, Toast.LENGTH_LONG).show();
                }
            }
        });

        return contentView;
    }

    @Override
    protected boolean blurBehind() {
        return false;
    }

    private void checkFeed(String url) {
        if (url != null) {
            if (runningTask != null && !runningTask.isDone()) {
                runningTask.cancel(true);
            }

            runningTask = Utils.submitTask(() -> execute(url));
        }
    }

    private void execute(String query) {
        contentView.post(() -> {
            limit.setVisibility(View.GONE);
            preview.setVisibility(View.GONE);
        });

        try {
            Stream<Item> stream = Parser.parseData(query);

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

            Document document = Jsoup.connect(query)
                    .userAgent(Utils.USER_AGENT)
                    .get();
            Elements nodes = document.getElementsByTag("atom:link");
            nodes.addAll(document.getElementsByTag("atom:link"));
            nodes.addAll(document.getElementsByTag("link"));

            for (Element node :
                    nodes) {
                if (node.hasAttr("type") && node.hasAttr("href") &&
                        (node.attr("type").contains("rss") || node.attr("type").contains("atom"))) {
                    checkFeed(node.attr("href"));

                    return;
                }
            }

            throw new Exception("Could not get feed from: " + query);
        } catch (InterruptedIOException exception) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            contentView.post(() -> {
                preview.setVisibility(View.GONE);

                limit.setText(activity.getResources().getString(R.string.invalid_rss));
                limit.setVisibility(View.VISIBLE);
            });
        }
    }

    @Override
    public void show() {
        super.show();

        contentView.post(() -> scroller.setPadding(0, 0, 0, Measurements.getNavHeight()));
        heightProvider.start();
    }

    @Override
    public void dismiss() {
        heightProvider.close();
        query.setText(null);

        super.dismiss();
    }
}
