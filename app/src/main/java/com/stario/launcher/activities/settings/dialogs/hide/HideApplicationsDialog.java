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

package com.stario.launcher.activities.settings.dialogs.hide;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.ogaclejapan.smarttablayout.SmartTabLayout;
import com.stario.launcher.R;
import com.stario.launcher.activities.settings.dialogs.hide.pager.HideApplicationsPage;
import com.stario.launcher.activities.settings.dialogs.hide.pager.HideApplicationsPagerAdapter;
import com.stario.launcher.apps.ProfileManager;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.dialogs.ActionDialog;
import com.stario.launcher.ui.recyclers.overscroll.OverScrollEffect;
import com.stario.launcher.ui.recyclers.overscroll.OverScrollRecyclerView;
import com.stario.launcher.ui.utils.animation.Animation;

public class HideApplicationsDialog extends DialogFragment {
    private OnHideListener hideListener;
    private ThemedActivity activity;
    private float moveSlop;

    @Override
    public void onAttach(@NonNull Context context) {
        if (!(context instanceof ThemedActivity)) {
            throw new RuntimeException("Parent activity is not of type ThemedActivity.");
        }

        activity = (ThemedActivity) context;
        moveSlop = ViewConfiguration.get(activity).getScaledTouchSlop();

        super.onAttach(context);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new ActionDialog(activity) {
            @NonNull
            @Override
            protected View inflateContent(LayoutInflater inflater) {
                View root = inflater.inflate(R.layout.pop_up_hide, null);

                ViewPager pager = root.findViewById(R.id.pager);
                HideApplicationsPagerAdapter adapter = new HideApplicationsPagerAdapter(
                        getChildFragmentManager(), activity.getResources());

                View tabsContainer = root.findViewById(R.id.tabs_container);
                SmartTabLayout tabLayout = root.findViewById(R.id.tabs);

                pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                    private final OverScrollEffect.OnOverScrollListener overScrollListener;
                    private final RecyclerView.OnScrollListener scrollListener;

                    private OverScrollEffect.OverScrollState overScrollState;
                    private OverScrollRecyclerView recyclerView;
                    private boolean hidden;

                    {
                        this.scrollListener = new RecyclerView.OnScrollListener() {
                            @Override
                            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                                if (dy > 0) {
                                    hideTooltip();
                                } else if (dy < 0) {
                                    showTooltip();
                                }
                            }
                        };
                        this.overScrollListener = new OverScrollEffect.OnOverScrollListener() {
                            @Override
                            public void onOverScrollStateChanged(int edge, @NonNull OverScrollEffect.OverScrollState state) {
                                overScrollState = state;
                            }
                        };

                        this.overScrollState = OverScrollEffect.OverScrollState.IDLE;
                        this.hidden = tabsContainer.getTranslationY() != 0;

                        pager.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft,
                                                         oldTop, oldRight, oldBottom) -> {
                            HideApplicationsPage fragment =
                                    adapter.getRegisteredFragment(pager.getCurrentItem());

                            if (fragment != null) {
                                updateObservedRecycler(fragment.getRecycler());
                            }
                        });
                    }

                    @SuppressLint("ClickableViewAccessibility")
                    private void updateObservedRecycler(OverScrollRecyclerView recyclerView) {
                        if (this.recyclerView != null) {
                            if (this.recyclerView == recyclerView) {
                                return;
                            }

                            this.recyclerView.removeOnOverScrollListener(overScrollListener);
                            this.recyclerView.removeOnScrollListener(scrollListener);
                            this.recyclerView.setOnTouchListener(null);
                            this.overScrollState = OverScrollEffect.OverScrollState.IDLE;
                        }

                        recyclerView.addOnOverScrollListener(overScrollListener);
                        recyclerView.addOnScrollListener(scrollListener);
                        recyclerView.setOnTouchListener(new View.OnTouchListener() {
                            float totalDelta;
                            float lastY;

                            @Override
                            public boolean onTouch(View v, MotionEvent event) {
                                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                    totalDelta = 0;
                                } else {
                                    float delta = lastY - event.getRawY();

                                    if (Math.signum(totalDelta) != Math.signum(delta)) {
                                        totalDelta = delta;
                                    } else {
                                        totalDelta += delta;
                                    }

                                    if (overScrollState == OverScrollEffect.OverScrollState.IDLE) {
                                        if (totalDelta > moveSlop) {
                                            hideTooltip();
                                        } else if (totalDelta < -moveSlop) {
                                            showTooltip();
                                        }
                                    } else {
                                        hideTooltip();
                                    }

                                }

                                lastY = event.getRawY();
                                return false;
                            }
                        });
                        this.recyclerView = recyclerView;
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {
                        if (state != ViewPager.SCROLL_STATE_IDLE) {
                            showTooltip();
                        }
                    }

                    @Override
                    public void onPageSelected(int position) {
                        updateObservedRecycler(adapter.getRegisteredFragment(position).getRecycler());
                    }

                    private void hideTooltip() {
                        if (!hidden) {
                            tabsContainer.animate()
                                    .translationY(tabsContainer.getMeasuredHeight())
                                    .setDuration(Animation.MEDIUM.getDuration());

                            hidden = true;
                        }
                    }

                    private void showTooltip() {
                        if (hidden) {
                            tabsContainer.animate()
                                    .translationY(0)
                                    .setDuration(Animation.MEDIUM.getDuration());

                            hidden = false;
                        }
                    }
                });

                pager.setAdapter(adapter);
                if (ProfileManager.from(activity.getApplicationContext(), false)
                        .getProfiles().size() > 1) {
                    tabLayout.setViewPager(pager);
                } else {
                    tabLayout.setVisibility(View.GONE);
                }

                return root;
            }

            @Override
            protected int getDesiredInitialState() {
                return BottomSheetBehavior.STATE_EXPANDED;
            }

            @Override
            protected boolean blurBehind() {
                return true;
            }

            @Override
            public void hide() {
                if (hideListener != null) {
                    hideListener.onHide();
                }
            }
        };
    }

    @Override
    public void onStop() {
        dismissAllowingStateLoss();

        super.onStop();
    }

    public void show() {
        if (getDialog() != null) {
            getDialog().show();
        }
    }

    public void setOnHideListener(OnHideListener listener) {
        this.hideListener = listener;
    }

    public interface OnHideListener {
        void onHide();
    }
}
