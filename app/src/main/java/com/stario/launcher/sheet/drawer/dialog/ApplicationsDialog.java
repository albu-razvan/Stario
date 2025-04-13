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

package com.stario.launcher.sheet.drawer.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.transition.Transition;
import android.transition.TransitionListenerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.window.OnBackInvokedDispatcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;

import com.bosphere.fadingedgelayout.FadingEdgeLayout;
import com.stario.launcher.R;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.sheet.SheetDialogFragment;
import com.stario.launcher.sheet.SheetType;
import com.stario.launcher.sheet.behavior.SheetBehavior;
import com.stario.launcher.sheet.drawer.DrawerAdapter;
import com.stario.launcher.sheet.drawer.search.SearchEngine;
import com.stario.launcher.sheet.drawer.search.SearchFragment;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.common.pager.CustomDurationViewPager;
import com.stario.launcher.utils.Utils;
import com.stario.launcher.utils.animation.Animation;
import com.stario.launcher.utils.animation.FragmentTransition;

public class ApplicationsDialog extends SheetDialogFragment {
    private static final String APPLICATIONS_PAGE = "com.stario.APPLICATIONS_PAGE";

    /**
     * Sent via {@link LocalBroadcastManager} with intent filter provided by
     * {@link #getTopic(View)} when that view changes its position in the pages
     */
    public static final String INTENT_EXTRA_PAGE_POSITION = "com.stario.ApplicationsDialog.PagePosition";

    /**
     * Sent via {@link LocalBroadcastManager} when the pager will change its visibility.
     * Register receivers with {@link #getTopic()}
     */
    public static final String INTENT_EXTRA_PAGER_VISIBILITY = "com.stario.ApplicationsDialog.PagerVisibility";
    private LocalBroadcastManager broadcastManager;
    private CustomDurationViewPager pager;
    private ResumeListener listener;
    private DrawerAdapter adapter;
    private EditText search;

    public ApplicationsDialog() {
        super();
    }

    public ApplicationsDialog(SheetType type) {
        super(type);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        broadcastManager = LocalBroadcastManager.getInstance(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.drawer,
                container, false);

        pager = root.findViewById(R.id.pager);
        search = root.findViewById(R.id.search);
        FadingEdgeLayout fader = root.findViewById(R.id.fader);

        pager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        pager.setSaveEnabled(true);

        adapter = new DrawerAdapter(getChildFragmentManager());
        pager.setOffscreenPageLimit(adapter.getCount());
        pager.setAdapter(adapter);

        pager.setPageTransformer(false, (page, position) -> {
            Intent intent = new Intent(getTopic(page));
            intent.putExtra(INTENT_EXTRA_PAGE_POSITION, position);

            broadcastManager.sendBroadcastSync(intent);

            if (Math.abs(position) > 1) {
                page.setTranslationX(-2 * page.getWidth() * Math.signum(position));
            } else {
                page.setTranslationX(0);
            }
        });

        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            float positionOffset = 0;
            float selectedPosition = pager.getCurrentItem();
            // skip the first vibration on page selected
            boolean skipVibration = true;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                this.positionOffset = positionOffset;

                if (selectedPosition == 3 && positionOffset == 0) {
                    skipVibration = true;
                    pager.setCurrentItem(1, false);
                }
                if (position == 0 && positionOffset == 0) {
                    skipVibration = true;
                    pager.setCurrentItem(2, false);
                }
            }

            @SuppressWarnings("ConstantConditions")
            @Override
            public void onPageSelected(int position) {
                if (!skipVibration) {
                    Vibrations.getInstance().vibrate();
                }

                this.selectedPosition = position;

                if (position == 3 && positionOffset == 0) {
                    skipVibration = true;
                    pager.setCurrentItem(1, false);
                } else if (position == 1 || position == 2) {
                    activity.getSharedPreferences(Entry.DRAWER)
                            .edit()
                            .putInt(APPLICATIONS_PAGE, position)
                            .apply();
                }

                skipVibration = false;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        setOnBackPressed(() -> {
            if (!adapter.isTransitioning()) {
                if (getChildFragmentManager()
                        .popBackStackImmediate(SearchFragment.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE) ||
                        (adapter.collapse() && pager.getCurrentItem() == DrawerAdapter.CATEGORIES_POSITION)) {
                    return false;
                } else {
                    SheetBehavior<?> behavior = getBehavior();

                    if (behavior != null) {
                        getBehavior().setState(SheetBehavior.STATE_COLLAPSED);
                    }

                    return true;
                }
            } else {
                return false;
            }
        });

        addOnShowListener(dialogInterface -> {
            SheetBehavior<?> behavior = getBehavior();

            if (behavior != null) {
                behavior.addSheetCallback(new SheetBehavior.SheetCallback() {
                    private static final int HASH = 2345678;

                    @Override
                    public void onSlide(@NonNull View sheet, float slideOffset) {
                        search.setTranslationY((1f - slideOffset) * -sheet.getMeasuredHeight() / 7f);
                    }

                    @Override
                    public void onStateChanged(@NonNull View sheet, int newState) {
                        if (newState == SheetBehavior.STATE_COLLAPSED) {
                            try {
                                adapter.reset();
                            } catch (Exception exception) {
                                setOnResumeListener(() -> {
                                    adapter.reset();

                                    setOnResumeListener(null);
                                });
                            }
                        } else if (newState == SheetBehavior.STATE_EXPANDED) {
                            search.setTranslationY(0);
                        }
                    }

                    @Override
                    public int hashCode() {
                        return HASH;
                    }

                    @Override
                    public boolean equals(@Nullable Object object) {
                        if (object instanceof SheetBehavior.SheetCallback) {
                            return object.hashCode() == hashCode();
                        }

                        return false;
                    }
                });
            }
        });

        pager.setCurrentItem(activity
                .getSharedPreferences(Entry.DRAWER)
                .getInt(APPLICATIONS_PAGE, 1));

        Measurements.addNavListener(value -> {
            View searchContainer = (View) search.getParent();

            searchContainer.setPadding(searchContainer.getPaddingLeft(), searchContainer.getPaddingTop(),
                    searchContainer.getPaddingRight(), value + Measurements.dpToPx(20));

            fader.setFadeSizes(Measurements.isLandscape() ?
                            Measurements.getSysUIHeight() : Measurements.dpToPx(Measurements.HEADER_SIZE_DP / 2f),
                    0, value + Measurements.getDefaultPadding() + search.getMeasuredHeight(), 0);
        });

        search.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                fader.setFadeSizes(Measurements.isLandscape() ?
                                Measurements.getSysUIHeight() : Measurements.dpToPx(Measurements.HEADER_SIZE_DP / 2f),
                        0, Measurements.getNavHeight() + Measurements.getDefaultPadding() + search.getMeasuredHeight(), 0));

        search.setInputType(0);

        search.setFocusable(false);
        search.setFocusableInTouchMode(false);

        search.setOnClickListener(new View.OnClickListener() {
            private SearchFragment fragment;

            @Override
            public void onClick(View v) {
                FragmentManager manager = getChildFragmentManager();
                FragmentTransaction transaction = manager.beginTransaction();

                if (fragment == null) {
                    fragment = new SearchFragment();

                    if (Utils.isMinimumSDK(Build.VERSION_CODES.TIRAMISU)) {
                        Dialog dialog = getDialog();

                        if (dialog != null) {
                            dialog.getOnBackInvokedDispatcher()
                                    .registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_OVERLAY,
                                            () -> {
                                                if (!fragment.onBackPressed()) {
                                                    //noinspection deprecation
                                                    dialog.onBackPressed();
                                                }
                                            });
                        }
                    }
                }

                for (Fragment target : manager.getFragments()) {
                    if (target == fragment) {
                        return;
                    }
                }

                Transition transition = new FragmentTransition(true)
                        .excludeTarget(EditText.class, true)
                        .excludeTarget(RelativeLayout.class, true);

                transition.setDuration(Animation.MEDIUM.getDuration());

                transition.addListener(new TransitionListenerAdapter() {
                    @Override
                    public void onTransitionStart(Transition transition) {
                        if (search.getVisibility() == View.VISIBLE) {
                            fader.animate().cancel();
                            fader.setTranslationY(0);
                            fader.setScaleX(1f);
                            fader.setScaleY(1f);
                            fader.setAlpha(1f);

                            fader.animate().alpha(0)
                                    .translationY((float) -Measurements.getHeight() / 2)
                                    .setDuration(transition.getDuration())
                                    .setInterpolator(transition.getInterpolator())
                                    .withEndAction(() -> {
                                        Intent intent = new Intent(getTopic());
                                        intent.putExtra(INTENT_EXTRA_PAGER_VISIBILITY, false);

                                        broadcastManager.sendBroadcastSync(intent);

                                        fader.setTranslationY(0);
                                        fader.setScaleX(0.9f);
                                        fader.setScaleY(0.9f);
                                    });
                            search.setVisibility(View.GONE);
                        } else {
                            fader.animate().cancel();
                            fader.setTranslationY(0);
                            fader.setScaleX(0.9f);
                            fader.setScaleY(0.9f);
                            fader.setAlpha(0f);

                            fader.animate()
                                    .alpha(1)
                                    .scaleY(1)
                                    .scaleX(1)
                                    .translationY(0)
                                    .setDuration(transition.getDuration())
                                    .setInterpolator(transition.getInterpolator())
                                    .withEndAction(() -> {
                                        Intent intent = new Intent(getTopic());
                                        intent.putExtra(INTENT_EXTRA_PAGER_VISIBILITY, true);

                                        broadcastManager.sendBroadcastSync(intent);
                                    });

                            search.setVisibility(View.VISIBLE);
                        }
                    }
                });

                fragment.setEnterTransition(transition);

                transaction.setReorderingAllowed(true)
                        .addToBackStack(SearchFragment.TAG)
                        .add(R.id.root, fragment)
                        .commit();
            }
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        SearchEngine engine = SearchEngine.getEngine(activity);
        search.setCompoundDrawablesWithIntrinsicBounds(
                engine.getDrawable(activity), null, null, null);

        if (!adapter.isTransitioning()) {
            getChildFragmentManager()
                    .popBackStackImmediate(SearchFragment.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        if (listener != null) {
            listener.onResume();
        }
    }

    public static String getTopic() {
        return getTopic(null);
    }

    public static String getTopic(View view) {
        if (view != null) {
            return "ApplicationPagerUpdate" + System.identityHashCode(view);
        }

        return "ApplicationPagerUpdate";
    }

    private void setOnResumeListener(ResumeListener listener) {
        this.listener = listener;
    }

    private interface ResumeListener {
        void onResume();
    }
}
