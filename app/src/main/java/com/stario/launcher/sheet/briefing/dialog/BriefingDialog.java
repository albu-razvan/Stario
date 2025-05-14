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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.viewpager.widget.ViewPager;

import com.stario.launcher.R;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.sheet.SheetDialogFragment;
import com.stario.launcher.sheet.SheetType;
import com.stario.launcher.sheet.behavior.SheetBehavior;
import com.stario.launcher.sheet.briefing.BriefingFeedList;
import com.stario.launcher.sheet.briefing.configurator.BriefingConfigurator;
import com.stario.launcher.sheet.briefing.feed.BriefingAdapter;
import com.stario.launcher.sheet.briefing.feed.FeedPage;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.common.pager.CustomDurationViewPager;
import com.stario.launcher.ui.popup.PopupMenu;
import com.stario.launcher.ui.common.TabLayout;
import com.stario.launcher.ui.utils.animation.Animation;
import com.stario.launcher.utils.objects.ObservableObject;

public class BriefingDialog extends SheetDialogFragment {
    private static final ObservableObject<Integer> TITLE_HEIGHT = new ObservableObject<>(0);
    private static final ObservableObject<Integer> TABS_HEIGHT = new ObservableObject<>(0);
    private BriefingDialogPageListener listener;
    private CustomDurationViewPager pager;
    private ThemedActivity activity;
    private BriefingAdapter adapter;
    private ViewGroup placeholder;
    private View tabsContainer;
    private ViewGroup main;
    private TabLayout tabs;
    private View title;
    private View root;

    public BriefingDialog() {
        super();
    }

    public BriefingDialog(SheetType type) {
        super(type);
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

        adapter = new BriefingAdapter(activity, getChildFragmentManager());
        pager.setAdapter(adapter);

        BriefingFeedList list = BriefingFeedList.from(activity);
        list.setOnFeedUpdateListener(new BriefingFeedList.FeedListener() {
            private void notifyUpdate() {
                adapter.notifyDataSetChanged();

                tabs.setViewPager(pager);
                updateHeader(FeedPage.getScrollOffsetObservable().getObject());

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
            public void onMoved(int first, int second) {
                notifyUpdate();
            }
        });

        tabs.setOverScrollMode(View.OVER_SCROLL_NEVER);
        tabs.setCustomTabView((viewGroup, position, adapter) -> {
            CharSequence text = adapter.getPageTitle(position);

            TextView textView = (TextView) inflater.inflate(R.layout.tab, viewGroup, false);
            textView.setOnLongClickListener(new View.OnLongClickListener() {
                private void showPopup() {
                    Vibrations.getInstance().vibrate();

                    PopupMenu menu = new PopupMenu(activity);

                    Resources resources = activity.getResources();

                    menu.add(new PopupMenu.Item(resources.getString(R.string.remove),
                            ResourcesCompat.getDrawable(resources, R.drawable.ic_delete, activity.getTheme()),
                            view -> list.remove(position)));

                    menu.show(activity, textView, PopupMenu.PIVOT_CENTER_HORIZONTAL);
                }

                @Override
                public boolean onLongClick(View v) {
                    showPopup();

                    return false;
                }
            });
            textView.setText(text);

            return textView;
        });

        tabs.setViewPager(pager);

        tabsContainer = (View) tabs.getParent();

        FeedPage.getScrollOffsetObservable()
                .addListener(this::updateHeader);

        listener = new BriefingDialogPageListener();

        pager.addOnPageChangeListener(listener);

        setOnBackPressed(() -> {
            SheetBehavior<?> behavior = getBehavior();

            if (behavior != null) {
                getBehavior().setState(SheetBehavior.STATE_COLLAPSED);
            }

            return true;
        });

        title.addOnLayoutChangeListener((v, left, top, right, bottom,
                                         oldLeft, oldTop, oldRight, oldBottom) -> {
            if (title.getMeasuredHeight() > 0 && tabs.getMeasuredHeight() > 0) {
                TABS_HEIGHT.updateObject(tabs.getMeasuredHeight());
                TITLE_HEIGHT.updateObject(title.getMeasuredHeight());

                updateHeader(FeedPage.getScrollOffsetObservable().getObject());
            }
        });

        View.OnClickListener clickListener = new View.OnClickListener() {
            private BriefingConfigurator configurator;

            @Override
            public void onClick(View view) {
                if (configurator == null || !activity.equals(configurator.getContext())) {
                    configurator = new BriefingConfigurator(activity);

                    configurator.setOnShowListener(dialog -> root.animate()
                            .alpha(0)
                            .setInterpolator(new FastOutSlowInInterpolator())
                            .setDuration(Animation.MEDIUM.getDuration()));

                    configurator.setOnDismissListener(dialog -> root.animate()
                            .alpha(1)
                            .setInterpolator(new FastOutSlowInInterpolator())
                            .setDuration(Animation.MEDIUM.getDuration()));
                }

                configurator.show();
            }
        };

        title.setOnClickListener(clickListener);
        placeholder.setOnClickListener(clickListener);
        placeholder.findViewById(R.id.add_button).setOnClickListener(clickListener);

        title.setOnLongClickListener(view -> false);
        title.setOnTouchListener((v, event) -> {
            getBehavior().interceptTouches(event.getAction() != MotionEvent.ACTION_CANCEL &&
                    event.getAction() != MotionEvent.ACTION_UP);

            return false;
        });

        Measurements.addStatusBarListener(value -> root.setPadding(0, value, 0, 0));
        Measurements.addNavListener(value -> placeholder.setPadding(0, 0, 0, value));

        return root;
    }

    private void updateHeader(Float translation) {
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
                        BriefingDialog.TITLE_HEIGHT.getObject() * positionOffset);

                title.setTranslationY((1 - positionOffset));
                title.setAlpha(positionOffset);
            } else {
                tabsContainer.setTranslationY(startingTranslationTabs * (1 - positionOffset) +
                        BriefingDialog.TITLE_HEIGHT.getObject() * positionOffset);

                title.setTranslationY(startingTranslationTitle * (1 - positionOffset));
                title.setAlpha(startingAlphaTitle + (1 - startingAlphaTitle) * positionOffset);
            }

            title.setVisibility(title.getAlpha() > 0 ?
                    View.VISIBLE : View.INVISIBLE);
        }

        @Override
        public void onPageSelected(int position) {
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

    public static ObservableObject.ClosedObservableObject<Integer> getTitleHeightObservable() {
        return TITLE_HEIGHT.close();
    }

    public static ObservableObject.ClosedObservableObject<Integer> getTabsHeightObservable() {
        return TABS_HEIGHT.close();
    }
}