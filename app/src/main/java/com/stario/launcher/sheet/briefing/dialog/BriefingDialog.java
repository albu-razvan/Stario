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

package com.stario.launcher.sheet.briefing.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.stario.launcher.R;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.sheet.SheetDialogFragment;
import com.stario.launcher.sheet.SheetType;
import com.stario.launcher.sheet.briefing.dialog.page.feed.BriefingFeedList;
import com.stario.launcher.sheet.briefing.configurator.BriefingConfigurator;
import com.stario.launcher.sheet.briefing.configurator.FeedConfigurator;
import com.stario.launcher.sheet.briefing.dialog.page.feed.Feed;
import com.stario.launcher.sheet.briefing.dialog.page.FeedPage;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.common.pager.CustomDurationViewPager;
import com.stario.launcher.ui.common.tabs.LeftTabLayout;
import com.stario.launcher.ui.popup.PopupMenu;

import java.util.Objects;

public class BriefingDialog extends SheetDialogFragment {
    private RecyclerView.OnScrollListener scrollListener;
    private View.OnLayoutChangeListener layoutListener;
    private BriefingFeedList.FeedListener feedListener;
    private BriefingDialogPageListener listener;
    private RecyclerView recyclerToBeObserved;
    private CustomDurationViewPager pager;
    private ThemedActivity activity;
    private BriefingAdapter adapter;
    private BriefingFeedList list;
    private ViewGroup placeholder;
    private View tabsContainer;
    private LeftTabLayout tabs;
    private ViewGroup main;
    private View title;
    private View root;

    public BriefingDialog() {
        super();

        init();
    }

    public BriefingDialog(SheetType type) {
        super(type);

        init();
    }

    public static String getName() {
        return "Briefing";
    }

    private void init() {
        this.recyclerToBeObserved = null;
        this.scrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                updateHeader(recyclerView.computeVerticalScrollOffset());
            }
        };
        this.layoutListener = (v, left, top, right, bottom, oldLeft, oldTop,
                               oldRight, oldBottom) -> {
            updateHeader(recyclerToBeObserved);
        };
        this.feedListener = new BriefingFeedList.FeedListener() {
            private void notifyUpdate() {
                adapter.notifyDataSetChanged();
                tabs.setViewPager(pager);

                if (adapter.getCount() > 0) {
                    pager.setCurrentItem(0, true);
                    observePageRecycler(0);
                } else {
                    observePageRecycler(null);
                }

                updateHeader(recyclerToBeObserved);

                if (adapter.getCount() > 0) {
                    placeholder.setVisibility(View.GONE);
                    main.setVisibility(View.VISIBLE);
                } else {
                    placeholder.setVisibility(View.VISIBLE);
                    main.setVisibility(View.GONE);
                }
            }

            @Override
            public void onInserted(int index) {
                notifyUpdate();
            }

            @Override
            public void onRemoved(int index) {
                notifyUpdate();
            }

            @Override
            public void onUpdated(int index) {
                notifyUpdate();
            }
        };
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        activity = (ThemedActivity) context;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.briefing, container, false);

        title = root.findViewById(R.id.title_feeds);
        placeholder = root.findViewById(R.id.placeholder);
        pager = root.findViewById(R.id.articles_container);
        tabs = root.findViewById(R.id.tabs);
        main = root.findViewById(R.id.main);

        title.setHapticFeedbackEnabled(false);
        main.setHapticFeedbackEnabled(false);

        pager.setOffscreenPageLimit(2);
        pager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        pager.setSaveEnabled(false);
        listener = new BriefingDialogPageListener();
        pager.addOnPageChangeListener(listener);

        adapter = new BriefingAdapter(activity, getChildFragmentManager());
        pager.setAdapter(adapter);

        pager.post(() -> {
            if (adapter.getCount() > 0) {
                observePageRecycler(pager.getCurrentItem());
            }
        });

        list = BriefingFeedList.from(activity);
        list.addOnFeedUpdateListener(feedListener);

        tabs.setOverScrollMode(View.OVER_SCROLL_NEVER);
        tabs.setOnTabLongClickListener((tab, position) -> {
            Vibrations.getInstance().vibrate();
            Resources resources = activity.getResources();

            PopupMenu menu = new PopupMenu(activity);

            menu.add(new PopupMenu.Item(resources.getString(R.string.remove),
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_delete, activity.getTheme()),
                    view -> list.remove(position)));

            Feed feed = list.get(position);
            if (feed != null) {
                menu.add(new PopupMenu.Item(resources.getString(R.string.rename_feed),
                        ResourcesCompat.getDrawable(resources, R.drawable.ic_edit, activity.getTheme()),
                        view -> new FeedConfigurator(activity, feed).show()));
            }

            menu.show(activity, tab, PopupMenu.PIVOT_CENTER_HORIZONTAL);
        });
        tabs.setViewPager(pager);

        tabsContainer = (View) tabs.getParent();

        setOnBackPressed(() -> {
            hide(true);

            return true;
        });

        title.addOnLayoutChangeListener((v, left, top, right, bottom,
                                         oldLeft, oldTop, oldRight, oldBottom) -> {
            if (recyclerToBeObserved != null) {
                recyclerToBeObserved.post(() -> updateHeader(recyclerToBeObserved));
            }
        });

        View.OnClickListener clickListener = new View.OnClickListener() {
            private BriefingConfigurator configurator;

            @Override
            public void onClick(View view) {
                if (configurator == null) {
                    configurator = new BriefingConfigurator(activity);
                }

                configurator.show();
            }
        };

        title.setOnClickListener(clickListener);
        placeholder.setOnClickListener(clickListener);
        placeholder.findViewById(R.id.add_button).setOnClickListener(clickListener);

        title.setOnLongClickListener(view -> false);
        title.setOnTouchListener((v, event) -> {
            Objects.requireNonNull(getBehavior())
                    .interceptTouches(event.getAction() != MotionEvent.ACTION_CANCEL &&
                            event.getAction() != MotionEvent.ACTION_UP);

            return false;
        });

        Measurements.addStatusBarListener(value -> root.setPadding(0, value, 0, 0));
        Measurements.addNavListener(value -> placeholder.setPadding(0, 0, 0, value));

        return root;
    }

    @Override
    public void onDestroy() {
        if (list != null) {
            list.removeOnFeedUpdateListener(feedListener);
        }

        super.onDestroy();
    }

    private void observePageRecycler(Integer position) {
        FeedPage page = null;
        if (position != null) {
            page = adapter.getRegisteredFragment(position);
        }

        if (recyclerToBeObserved != null) {
            recyclerToBeObserved.removeOnScrollListener(scrollListener);
            recyclerToBeObserved.removeOnLayoutChangeListener(layoutListener);
        }

        if (page != null) {
            recyclerToBeObserved = page.getRecycler();

            if (recyclerToBeObserved != null) {
                recyclerToBeObserved.addOnScrollListener(scrollListener);
                recyclerToBeObserved.addOnLayoutChangeListener(layoutListener);
            }
        } else {
            recyclerToBeObserved = null;
        }
    }

    private void updateHeader(RecyclerView recycler) {
        if (recycler != null) {
            updateHeader(recycler.computeVerticalScrollOffset());
        } else {
            updateHeader(0);
        }
    }

    private void updateHeader(float translation) {
        int maxTranslation = title.getMeasuredHeight();
        translation = -Math.min(maxTranslation, translation);

        tabsContainer.setTranslationY(maxTranslation + translation);
        title.setTranslationY(translation);

        float alpha = (translation + maxTranslation / 2f) / maxTranslation * 2f;

        title.setAlpha(alpha);
        title.setVisibility(alpha > 0 ? View.VISIBLE : View.INVISIBLE);
    }

    private void updateTitleHeight() {
        if (Measurements.isLandscape()) {
            title.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        } else {
            title.getLayoutParams().height = Measurements.dpToPx(Measurements.HEADER_SIZE_DP);
        }

        if (listener != null) {
            listener.reset();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        updateTitleHeight();
    }

    @Override
    public void onResume() {
        updateTitleHeight();

        if (adapter.getCount() > 0) {
            placeholder.setVisibility(View.GONE);
            main.setVisibility(View.VISIBLE);
        } else {
            placeholder.setVisibility(View.VISIBLE);
            main.setVisibility(View.GONE);
        }

        super.onResume();
    }

    public class BriefingDialogPageListener implements ViewPager.OnPageChangeListener {
        private Float startingTranslationTitle = Float.NaN;
        private Float startingTranslationTabs = Float.NaN;
        private Float startingAlphaTitle = Float.NaN;
        private float currentOffset = 0;
        private int startingPosition = -1;
        private int currentPosition = 0;

        public void reset() {
            startingTranslationTitle = Float.NaN;
            startingTranslationTabs = Float.NaN;
            startingAlphaTitle = Float.NaN;

            onPageScrolled(currentPosition, currentOffset, 0);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            currentPosition = position;
            currentOffset = positionOffset;

            if (position < startingPosition) {
                if (startingPosition - position > 1) {
                    positionOffset = 1;
                } else {
                    positionOffset = 1 - positionOffset;
                }
            } else if (position > startingPosition) {
                positionOffset = 1;
            }

            if (Float.isNaN(startingTranslationTitle) ||
                    Float.isNaN(startingTranslationTabs) ||
                    Float.isNaN(startingAlphaTitle)) {

                tabsContainer.setTranslationY((1 - positionOffset) +
                        title.getMeasuredHeight() * positionOffset);

                title.setTranslationY((1 - positionOffset));
                title.setAlpha(positionOffset);
            } else {
                tabsContainer.setTranslationY(startingTranslationTabs * (1 - positionOffset) +
                        title.getMeasuredHeight() * positionOffset);

                title.setTranslationY(startingTranslationTitle * (1 - positionOffset));
                title.setAlpha(startingAlphaTitle + (1 - startingAlphaTitle) * positionOffset);
            }

            title.setVisibility(title.getAlpha() > 0 ?
                    View.VISIBLE : View.INVISIBLE);
        }

        @Override
        public void onPageSelected(int position) {
            observePageRecycler(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == ViewPager.SCROLL_STATE_DRAGGING
                    || state == ViewPager.SCROLL_STATE_SETTLING) {
                if (startingPosition == -1) {
                    if (currentOffset > 0.5f) {
                        startingPosition = currentPosition + 1;
                    } else {
                        startingPosition = currentPosition;
                    }
                }

                if (Float.isNaN(startingAlphaTitle)) {
                    startingAlphaTitle = title.getAlpha();
                }

                if (Float.isNaN(startingTranslationTitle)) {
                    startingTranslationTitle = title.getTranslationY();
                }

                if (Float.isNaN(startingTranslationTabs)) {
                    startingTranslationTabs = tabsContainer.getTranslationY();
                }
            } else if (state == ViewPager.SCROLL_STATE_IDLE) {
                startingTranslationTitle = Float.NaN;
                startingTranslationTabs = Float.NaN;
                startingAlphaTitle = Float.NaN;
                startingPosition = -1;

                adapter.reset(pager.getCurrentItem());
            }
        }
    }
}