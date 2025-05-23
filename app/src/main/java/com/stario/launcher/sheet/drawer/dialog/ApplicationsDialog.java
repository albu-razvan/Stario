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
import android.os.Build;
import android.os.Bundle;
import android.transition.Transition;
import android.transition.TransitionListenerAdapter;
import android.util.Log;
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
import androidx.viewpager.widget.ViewPager;

import com.bosphere.fadingedgelayout.FadingEdgeLayout;
import com.stario.launcher.R;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.sheet.SheetDialogFragment;
import com.stario.launcher.sheet.SheetType;
import com.stario.launcher.sheet.behavior.SheetBehavior;
import com.stario.launcher.sheet.drawer.DrawerAdapter;
import com.stario.launcher.sheet.drawer.DrawerPage;
import com.stario.launcher.sheet.drawer.search.SearchEngine;
import com.stario.launcher.sheet.drawer.search.SearchFragment;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.utils.animation.Animation;
import com.stario.launcher.ui.utils.animation.FragmentTransition;
import com.stario.launcher.utils.Utils;

public class ApplicationsDialog extends SheetDialogFragment {
    private static final String APPLICATIONS_PAGE = "com.stario.APPLICATIONS_PAGE";

    private ThemedActivity activity;
    private ResumeListener listener;
    private DrawerAdapter adapter;
    private ViewPager pager;
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

        activity = (ThemedActivity) context;
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

        setOnBackPressed(() -> {
            if (!adapter.isTransitioning()) {
                if (getChildFragmentManager()
                        .popBackStackImmediate(SearchFragment.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE) ||
                        (pager.getCurrentItem() == DrawerAdapter.CATEGORIES_POSITION && adapter.collapse())) {
                    return false;
                } else {
                    SheetBehavior<?> behavior = getBehavior();

                    if (behavior != null) {
                        behavior.setState(SheetBehavior.STATE_COLLAPSED);
                        behavior.setDraggable(true);
                    }

                    return true;
                }
            } else {
                return false;
            }
        });

        addOnShowListener(() -> {
            SheetBehavior<?> behavior = getBehavior();

            if (behavior != null) {
                behavior.addSheetCallback(new SheetBehavior.SheetCallback() {
                    private static final int HASH = 2345678;

                    @Override
                    public void onSlide(@NonNull View sheet, float slideOffset) {
                        search.setTranslationY((1f - slideOffset) *
                                -(Measurements.dpToPx(SheetBehavior.COLLAPSED_DELTA_DP)
                                        - search.getMeasuredHeight() * 2));
                    }

                    @Override
                    public void onStateChanged(@NonNull View sheet, int newState) {
                        if (newState == SheetBehavior.STATE_COLLAPSED) {
                            if (!adapter.isTransitioning()) {
                                try {
                                    getChildFragmentManager()
                                            .popBackStackImmediate(SearchFragment.TAG,
                                                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
                                    getBehavior().setDraggable(true);
                                } catch (Exception exception) {
                                    Log.e("ApplicationsDialog",
                                            "onStateChanged: " + exception.getMessage());
                                }
                            }

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

                                                    SheetBehavior<?> behavior = getBehavior();
                                                    if (behavior != null) {
                                                        behavior.setDraggable(true);
                                                    }
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
                                        notifySelection(false);

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
                                    .withEndAction(() -> notifySelection(true));

                            search.setVisibility(View.VISIBLE);
                        }
                    }
                });

                fragment.setEnterTransition(transition);

                transaction.setReorderingAllowed(true)
                        .addToBackStack(SearchFragment.TAG)
                        .add(R.id.root, fragment)
                        .commit();

                SheetBehavior<?> behavior = getBehavior();
                if (behavior != null) {
                    behavior.setDraggable(false);
                }
            }
        });

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        pager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        pager.setSaveEnabled(true);

        adapter = new DrawerAdapter(getChildFragmentManager());
        pager.setOffscreenPageLimit(100);
        pager.setAdapter(adapter);

        pager.setPageTransformer(false, (page, position) -> {
            notifySelection(position == (int) position);

            if (Math.abs(position) > adapter.getCount() - 3) {
                page.setTranslationX(-(adapter.getCount() - 2) *
                        page.getWidth() * Math.signum(position));
            } else {
                page.setTranslationX(0);
            }
        });

        pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            float positionOffset = 0;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                this.positionOffset = positionOffset;

                if (position == 0 && positionOffset == 0) {
                    pager.setCurrentItem(adapter.getCount() - 2, false);
                }
                if (position == adapter.getCount() - 1) {
                    pager.setCurrentItem(1, false);
                }
            }

            @SuppressWarnings("ConstantConditions")
            @Override
            public void onPageSelected(int position) {
                if (position > 0 && position < adapter.getCount() - 1) {
                    activity.getSharedPreferences(Entry.DRAWER)
                            .edit()
                            .putInt(APPLICATIONS_PAGE, position)
                            .apply();
                }
            }
        });

        pager.setCurrentItem(activity
                .getSharedPreferences(Entry.DRAWER)
                .getInt(APPLICATIONS_PAGE, DrawerAdapter.CATEGORIES_POSITION), false);
    }

    private void notifySelection(boolean focused) {
        Fragment focusedFragment = adapter.getFragment(pager.getCurrentItem());

        for (int index = 0; index < adapter.getCount(); index++) {
            Fragment fragment = adapter.getFragment(index);

            if (fragment instanceof DrawerPage) {
                ((DrawerPage) fragment).setSelected(focused
                        && fragment.equals(focusedFragment));
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        SearchEngine engine = SearchEngine.getEngine(activity);
        search.setCompoundDrawablesWithIntrinsicBounds(
                engine.getDrawable(activity), null, null, null);

        if (!adapter.isTransitioning()) {
            getChildFragmentManager()
                    .popBackStackImmediate(SearchFragment.TAG,
                            FragmentManager.POP_BACK_STACK_INCLUSIVE);
            getBehavior().setDraggable(true);
        }

        if (listener != null) {
            listener.onResume();
        }
    }

    private void setOnResumeListener(ResumeListener listener) {
        this.listener = listener;
    }

    private interface ResumeListener {
        void onResume();
    }
}
