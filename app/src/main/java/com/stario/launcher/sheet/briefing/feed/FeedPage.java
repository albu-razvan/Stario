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

package com.stario.launcher.sheet.briefing.feed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.apptasticsoftware.rssreader.Item;
import com.stario.launcher.R;
import com.stario.launcher.activities.Launcher;
import com.stario.launcher.sheet.briefing.BriefingFeedList;
import com.stario.launcher.sheet.briefing.rss.RssParser;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.common.scrollers.CustomSwipeRefreshLayout;
import com.stario.launcher.ui.recyclers.RecyclerItemAnimator;
import com.stario.launcher.ui.recyclers.managers.AccurateScrollComputeLinearLayoutManager;
import com.stario.launcher.ui.recyclers.overscroll.OverScrollEffect;
import com.stario.launcher.ui.recyclers.overscroll.OverScrollRecyclerView;
import com.stario.launcher.ui.utils.UiUtils;
import com.stario.launcher.ui.utils.animation.Animation;
import com.stario.launcher.utils.Utils;

import java.util.concurrent.Future;
import java.util.stream.Stream;

public class FeedPage extends Fragment {
    private static final String TAG = "FeedUpdate";
    public static final String FEED_POSITION = "com.stario.FeedTab.FEED_POSITION";

    private static final float UPDATE_SCALE = 0.9f;

    private AccurateScrollComputeLinearLayoutManager manager;
    private CustomSwipeRefreshLayout swipeRefreshLayout;
    private OverScrollRecyclerView recyclerView;
    private ThemedActivity activity;
    private FeedPageAdapter adapter;
    private Future<?> runningTask;
    private ViewGroup exception;
    private int position;
    private View title;
    private View tabs;

    public FeedPage() {
        // default
    }

    public FeedPage(int position) {
        this.position = position;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(FEED_POSITION, position);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        if (!(context instanceof ThemedActivity)) {
            throw new RuntimeException("Parent activity is not of type ThemedActivity.");
        }

        this.activity = (ThemedActivity) context;

        super.onAttach(context);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            position = savedInstanceState.getInt(FEED_POSITION, -1);
        }

        View root = inflater.inflate(R.layout.articles, container, false);

        assert container != null;
        View containerRoot = container.getRootView();
        title = containerRoot.findViewById(R.id.title_feeds);
        tabs = containerRoot.findViewById(R.id.tabs);

        recyclerView = root.findViewById(R.id.recycler_view);
        swipeRefreshLayout = root.findViewById(R.id.refresh);
        exception = root.findViewById(R.id.exception);

        recyclerView.setItemAnimator(new RecyclerItemAnimator(RecyclerItemAnimator.APPEARANCE |
                RecyclerItemAnimator.CHANGING, Animation.EXTENDED));
        recyclerView.setOverscrollPullEdges(OverScrollEffect.PULL_EDGE_BOTTOM);

        invalidateLayoutPadding();
        Measurements.addNavListener(value ->
                recyclerView.setPadding(0, recyclerView.getPaddingTop(), 0, value));
        title.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop,
                                         oldRight, oldBottom) -> invalidateLayoutPadding());
        tabs.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop,
                                        oldRight, oldBottom) -> invalidateLayoutPadding());

        Measurements.addStatusBarListener(object ->
                recyclerView.setPadding(0, recyclerView.getPaddingTop(),
                        0, Measurements.getNavHeight()));

        if (adapter == null) {
            adapter = new FeedPageAdapter((Launcher) recyclerView.getContext(), recyclerView);
        } else {
            adapter.updateAttributes(recyclerView);
        }

        manager = new AccurateScrollComputeLinearLayoutManager(activity);
        manager.setItemPrefetchEnabled(true);
        manager.setInitialPrefetchItemCount(4);

        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(this::update);
        swipeRefreshLayout.setOnEngageListener(engaged -> manager.setScrollEnabled(!engaged));
        swipeRefreshLayout.setSize(SwipeRefreshLayout.LARGE);
        swipeRefreshLayout.setOverScrollMode(View.OVER_SCROLL_NEVER);

        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                activity.getAttributeData(com.google.android.material.R.attr.colorSurfaceContainer)
        );

        swipeRefreshLayout.setColorSchemeColors(
                activity.getAttributeData(com.google.android.material.R.attr.colorSecondary),
                activity.getAttributeData(com.google.android.material.R.attr.colorTertiary),
                activity.getAttributeData(com.google.android.material.R.attr.colorPrimary)
        );

        root.findViewById(R.id.refresh_button)
                .setOnClickListener(v ->
                        swipeRefreshLayout.post(() -> {
                            swipeRefreshLayout.setRefreshing(true);
                            update();
                        })
                );

        Measurements.addNavListener(bottomInset ->
                recyclerView.setPadding(0, recyclerView.getPaddingTop(), 0, bottomInset));

        return root;
    }

    public void invalidateLayoutPadding() {
        int titleHeight = title.getMeasuredHeight();
        int tabsHeight = tabs.getMeasuredHeight();

        recyclerView.setPadding(0, Measurements.dpToPx(15) +
                titleHeight + tabsHeight, 0, Measurements.getNavHeight());
        exception.setPadding(0, (titleHeight + tabsHeight) / 2, 0, 0);
        swipeRefreshLayout.setProgressViewOffset(true,
                titleHeight + tabsHeight, (int) ((titleHeight + tabsHeight) * 1.5f));
    }

    @Override
    public void onStop() {
        reset();

        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();

        swipeRefreshLayout.post(() -> {
            swipeRefreshLayout.setRefreshing(true);
            update();
        });
    }

    public void update() {
        if (runningTask == null || runningTask.isDone()) {
            if (position >= 0 &&
                    position < BriefingFeedList.from(activity).size() &&
                    adapter.shouldUpdate()) {
                manager.setScrollEnabled(false);

                exception.setVisibility(View.GONE);
                recyclerView.animate()
                        .alpha(0)
                        .scaleX(UPDATE_SCALE)
                        .scaleY(UPDATE_SCALE)
                        .setDuration(Animation.MEDIUM.getDuration())
                        .setInterpolator(new FastOutSlowInInterpolator());

                runningTask = Utils.submitTask(() -> {
                    Stream<Item> stream = RssParser
                            .parse(BriefingFeedList.getInstance()
                                    .get(position).getRSSLink());

                    if (stream != null) {
                        Item[] items = stream.toArray(Item[]::new);

                        UiUtils.runOnUIThread(() -> {
                            adapter.update(items);

                            if (adapter.getItemCount() == 0) {
                                exception.setVisibility(View.VISIBLE);
                            } else {
                                recyclerView.animate()
                                        .alpha(1)
                                        .scaleX(1f)
                                        .scaleY(1f);

                                manager.setScrollEnabled(true);
                            }
                        });
                    } else {
                        exception.setVisibility(View.GONE);
                    }

                    UiUtils.runOnUIThread(() -> swipeRefreshLayout.setRefreshing(false));
                });
            } else {
                swipeRefreshLayout.setRefreshing(false);
            }
        } else {
            swipeRefreshLayout.setRefreshing(true);
        }
    }

    public RecyclerView getRecycler() {
        return recyclerView;
    }

    public void reset() {
        swipeRefreshLayout.setRefreshing(false);

        recyclerView.post(() -> recyclerView.scrollBy(0, -Integer.MAX_VALUE));
    }
}
