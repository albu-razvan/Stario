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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.apptasticsoftware.rssreader.Item;
import com.stario.launcher.R;
import com.stario.launcher.activities.Launcher;
import com.stario.launcher.sheet.briefing.BriefingFeedList;
import com.stario.launcher.sheet.briefing.dialog.BriefingDialog;
import com.stario.launcher.sheet.briefing.rss.Parser;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.recyclers.RecyclerItemAnimator;
import com.stario.launcher.utils.UiUtils;
import com.stario.launcher.utils.Utils;
import com.stario.launcher.utils.animation.Animation;
import com.stario.launcher.utils.objects.ObservableObject;

import java.util.concurrent.Future;
import java.util.stream.Stream;

public class FeedPage extends Fragment {
    private static final ObservableObject<Float> SCROLL_OFFSET = new ObservableObject<>(0f);
    private static final String TAG = "FeedUpdate";
    public static String FEED_POSITION = "com.stario.FeedTab.FEED_POSITION";
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private FeedPageAdapter adapter;
    private Future<?> runningTask;
    private ViewGroup exception;
    private ThemedActivity activity;
    private int position;

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

        recyclerView = root.findViewById(R.id.recycler_view);
        swipeRefreshLayout = root.findViewById(R.id.refresh);
        exception = root.findViewById(R.id.exception);

        recyclerView.setItemAnimator(new RecyclerItemAnimator(RecyclerItemAnimator.APPEARANCE | RecyclerItemAnimator.CHANGING, Animation.MEDIUM));

        final int baseTopPadding = Measurements.dpToPx(15);

        recyclerView.setPadding(0, baseTopPadding +
                BriefingDialog.getTitleHeightObservable().getObject() +
                BriefingDialog.getTabsHeightObservable().getObject(), 0, 0);
        swipeRefreshLayout.setProgressViewOffset(true,
                BriefingDialog.getTitleHeightObservable().getObject() +
                        BriefingDialog.getTabsHeightObservable().getObject(),
                (int) ((BriefingDialog.getTitleHeightObservable().getObject() +
                        BriefingDialog.getTabsHeightObservable().getObject()) * 1.5f));

        //TITLE_HEIGHT is the only one updating
        BriefingDialog.getTitleHeightObservable().addListener(titleHeight -> {
            int tabsHeight = BriefingDialog.getTabsHeightObservable().getObject();

            recyclerView.setPadding(0, baseTopPadding + titleHeight + tabsHeight,
                    0, Measurements.getNavHeight());
            exception.setPadding(0, (titleHeight + tabsHeight) / 2, 0, 0);
            swipeRefreshLayout.setProgressViewOffset(true,
                    titleHeight + tabsHeight, (int) ((titleHeight + tabsHeight) * 1.5f));
        });

        Measurements.addSysUIListener(object ->
                recyclerView.setPadding(0, recyclerView.getPaddingTop(),
                        0, Measurements.getNavHeight()));

        if (adapter == null) {
            adapter = new FeedPageAdapter((Launcher) recyclerView.getContext(), recyclerView);
        } else {
            adapter.updateAttributes(recyclerView);
        }

        LinearLayoutManager manager = new LinearLayoutManager(activity);
        manager.setItemPrefetchEnabled(true);
        manager.setInitialPrefetchItemCount(4);

        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);

        recyclerView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (getUserVisibleHint()) {
                SCROLL_OFFSET.updateObject((float) recyclerView.computeVerticalScrollOffset());
            } else {
                recyclerView.scrollBy(0, -Integer.MAX_VALUE);
            }
        });

        swipeRefreshLayout.setOnRefreshListener(this::update);
        swipeRefreshLayout.setSize(SwipeRefreshLayout.LARGE);
        swipeRefreshLayout.setOverScrollMode(View.OVER_SCROLL_NEVER);

        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                activity.getAttributeData(com.google.android.material.R.attr.colorSurfaceContainer)
        );

        swipeRefreshLayout.setColorSchemeColors(
                activity.getAttributeData(com.google.android.material.R.attr.colorPrimary),
                activity.getAttributeData(com.google.android.material.R.attr.colorSecondary),
                activity.getAttributeData(com.google.android.material.R.attr.colorTertiary)
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
                runningTask = Utils.submitTask(() -> {
                    Stream<Item> stream = Parser
                            .parseData(BriefingFeedList.getInstance()
                                    .get(position).getRSSLink());

                    if (stream != null) {
                        Item[] items = stream.toArray(Item[]::new);

                        UiUtils.runOnUIThread(() -> {
                            adapter.update(items);

                            exception.setVisibility(View.GONE);
                        });
                    } else {
                        if (adapter.getItemCount() == 0) {
                            UiUtils.runOnUIThread(() ->
                                    exception.setVisibility(View.VISIBLE));
                        }
                    }

                    UiUtils.runOnUIThread(() ->
                            swipeRefreshLayout.setRefreshing(false));
                });
            } else {
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    }

    public void reset() {
        swipeRefreshLayout.setRefreshing(false);

        recyclerView.post(() ->
                recyclerView.scrollBy(0, -Integer.MAX_VALUE));
    }

    public static ObservableObject.ClosedObservableObject<Float> getScrollOffsetObservable() {
        return SCROLL_OFFSET.close();
    }
}
