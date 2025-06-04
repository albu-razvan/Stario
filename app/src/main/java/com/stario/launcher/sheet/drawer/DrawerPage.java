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

package com.stario.launcher.sheet.drawer;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.sheet.SheetDialogFragment;
import com.stario.launcher.sheet.SheetType;
import com.stario.launcher.sheet.drawer.dialog.ApplicationsDialog;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.recyclers.overscroll.OverScrollEffect;
import com.stario.launcher.ui.recyclers.overscroll.OverScrollRecyclerView;

public abstract class DrawerPage extends Fragment implements ScrollToTop {
    private int currentlyRunningAnimations;
    private RelativeLayout titleContainer;
    private boolean selected;

    protected OverScrollRecyclerView drawer;
    protected ThemedActivity activity;
    protected EditText search;
    protected TextView title;

    public DrawerPage() {
        super();

        this.selected = false;
        this.currentlyRunningAnimations = 0;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        if (!(context instanceof ThemedActivity)) {
            throw new RuntimeException("Parent activity is not of type ThemedActivity.");
        }

        activity = (ThemedActivity) context;

        super.onAttach(context);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(getLayoutResID(), container, false);

        titleContainer = root.findViewById(R.id.title_container);
        drawer = root.findViewById(R.id.drawer);
        title = root.findViewById(R.id.title);

        assert container != null;
        search = container.getRootView().findViewById(R.id.search);
        drawer.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
        drawer.addOnLayoutChangeListener((v, left, top, right, bottom,
                                          oldLeft, oldTop, oldRight, oldBottom) -> updateTitleTransforms(drawer));
        drawer.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                updateTitleTransforms(drawer);
            }
        });

        SheetType type = SheetType.getSheetTypeForSheetDialogFragment(activity, ApplicationsDialog.class);
        if (type == SheetType.BOTTOM_SHEET) {
            drawer.setOverscrollPullEdges(OverScrollEffect.PULL_EDGE_BOTTOM);
        }

        drawer.addOnOverScrollListener(new OverScrollEffect.OnOverScrollListener() {
            private static final float HIDE_THRESHOLD_DP = 100;

            private boolean receivedBottomOverscrollEvent;
            private boolean receivedTopOverscrollEvent;
            private float factor;

            {
                this.receivedTopOverscrollEvent = false;
                this.receivedBottomOverscrollEvent = false;
                this.factor = 0;
            }

            @Override
            public void onOverScrollStateChanged(int edge, @NonNull OverScrollEffect.OverScrollState state) {
                if (state == OverScrollEffect.OverScrollState.SETTLING) {
                    if ((receivedTopOverscrollEvent && edge == OverScrollEffect.PULL_EDGE_TOP) ||
                            (receivedBottomOverscrollEvent && edge == OverScrollEffect.PULL_EDGE_BOTTOM)) {
                        if (factor * drawer.getMeasuredHeight() >
                                Measurements.dpToPx(HIDE_THRESHOLD_DP)) {
                            Fragment fragment = getParentFragment();
                            if (fragment instanceof SheetDialogFragment) {
                                ((SheetDialogFragment) fragment).hide(true);
                            }
                        }
                    }

                    receivedBottomOverscrollEvent = false;
                    receivedTopOverscrollEvent = false;
                } else if (state == OverScrollEffect.OverScrollState.IDLE) {
                    receivedBottomOverscrollEvent = false;
                    receivedTopOverscrollEvent = false;
                } else if (state == OverScrollEffect.OverScrollState.OVER_SCROLLING) {
                    if (edge == OverScrollEffect.PULL_EDGE_TOP) {
                        receivedTopOverscrollEvent = true;
                    } else if (edge == OverScrollEffect.PULL_EDGE_BOTTOM) {
                        receivedBottomOverscrollEvent = true;
                    }
                }
            }

            @Override
            public void onOverScrolled(int edge, float factor) {
                this.factor = factor;
            }
        });

        titleContainer.getLayoutParams().height =
                Measurements.dpToPx(Measurements.HEADER_SIZE_DP) + Measurements.spToPx(8);

        Measurements.addStatusBarListener(value -> {
            drawer.setPadding(drawer.getPaddingLeft(),
                    value + (Measurements.isLandscape() ? Measurements.getDefaultPadding() :
                            Measurements.dpToPx(Measurements.HEADER_SIZE_DP) + Measurements.getDefaultPadding()),
                    drawer.getPaddingRight(),
                    drawer.getPaddingBottom());

            ((ViewGroup.MarginLayoutParams) titleContainer.getLayoutParams()).topMargin = value;
        });

        View searchContainer = (View) search.getParent();

        drawer.setPadding(drawer.getPaddingLeft(),
                drawer.getPaddingTop(), drawer.getPaddingRight(),
                searchContainer.getPaddingBottom() + (search.getBottom() - search.getTop()));

        search.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                drawer.setPadding(drawer.getPaddingLeft(),
                        drawer.getPaddingTop(), drawer.getPaddingRight(),
                        searchContainer.getPaddingBottom() + (bottom - top)));

        updateTitleTransforms(drawer);

        return root;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    protected void updateTitleTransforms(@NonNull RecyclerView recyclerView) {
        titleContainer.post(() -> {
            int translation = recyclerView.computeVerticalScrollOffset();

            titleContainer.setTranslationY(-translation / 2f);

            float alpha = 1f - translation / (Measurements.dpToPx(Measurements.HEADER_SIZE_DP) / 2f);
            if (alpha > 0 && !Measurements.isLandscape()) {
                titleContainer.setAlpha(alpha);
                titleContainer.setVisibility(View.VISIBLE);
            } else {
                titleContainer.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        updateTitleTransforms(drawer);
    }

    @Override
    public void onResume() {
        updateTitleTransforms(drawer);

        super.onResume();
    }

    @Override
    public void scrollToTop() {
        if (drawer != null) {
            drawer.scrollToPosition(0);
        }
    }

    protected abstract int getLayoutResID();

    // Whoever designed the fragment transition API, I'm coming after you...
    // Really ugly workaround to waiting for all transitions to finish
    // For some reason, not specifying int res transitions, but androidx or
    // platform transition and/or SharedElement transitions in transactions
    // results in FragmentTransaction.show() to not show because the visibility
    // of the fragment does not change until the end of the animation;
    // therefore, new transitions will not run on stack pop and current transitions
    // will NOT be cancelled either????? Basically, hidden fragment just "disappears".

    @Override
    public void setEnterTransition(@Nullable Object transition) {
        super.setEnterTransition(transition);

        if (transition instanceof androidx.transition.Transition) {
            setTransitionAndroidXListener((androidx.transition.Transition) transition);
        } else if (transition instanceof android.transition.Transition) {
            setTransitionListener((android.transition.Transition) transition);
        }
    }

    @Override
    public void setExitTransition(@Nullable Object transition) {
        super.setExitTransition(transition);

        if (transition instanceof androidx.transition.Transition) {
            setTransitionAndroidXListener((androidx.transition.Transition) transition);
        } else if (transition instanceof android.transition.Transition) {
            setTransitionListener((android.transition.Transition) transition);
        }
    }

    @Override
    public void setReenterTransition(@Nullable Object transition) {
        super.setReenterTransition(transition);

        if (transition instanceof androidx.transition.Transition) {
            setTransitionAndroidXListener((androidx.transition.Transition) transition);
        } else if (transition instanceof android.transition.Transition) {
            setTransitionListener((android.transition.Transition) transition);
        }
    }

    @Override
    public void setReturnTransition(@Nullable Object transition) {
        super.setReturnTransition(transition);

        if (transition instanceof androidx.transition.Transition) {
            setTransitionAndroidXListener((androidx.transition.Transition) transition);
        } else if (transition instanceof android.transition.Transition) {
            setTransitionListener((android.transition.Transition) transition);
        }
    }

    @Override
    public void setSharedElementEnterTransition(@Nullable Object transition) {
        super.setSharedElementEnterTransition(transition);

        if (transition instanceof androidx.transition.Transition) {
            setTransitionAndroidXListener((androidx.transition.Transition) transition);
        } else if (transition instanceof android.transition.Transition) {
            setTransitionListener((android.transition.Transition) transition);
        }
    }

    @Override
    public void setSharedElementReturnTransition(@Nullable Object transition) {
        super.setSharedElementReturnTransition(transition);

        if (transition instanceof androidx.transition.Transition) {
            setTransitionAndroidXListener((androidx.transition.Transition) transition);
        } else if (transition instanceof android.transition.Transition) {
            setTransitionListener((android.transition.Transition) transition);
        }
    }

    private void setTransitionAndroidXListener(androidx.transition.Transition transition) {
        transition.addListener(new androidx.transition.Transition.TransitionListener() {
            @Override
            public void onTransitionStart(@NonNull androidx.transition.Transition transition) {
                currentlyRunningAnimations++;
            }

            @Override
            public void onTransitionEnd(@NonNull androidx.transition.Transition transition) {
                currentlyRunningAnimations--;
            }

            @Override
            public void onTransitionCancel(@NonNull androidx.transition.Transition transition) {
                currentlyRunningAnimations--;
            }

            @Override
            public void onTransitionPause(@NonNull androidx.transition.Transition transition) {
                currentlyRunningAnimations--;
            }

            @Override
            public void onTransitionResume(@NonNull androidx.transition.Transition transition) {
                currentlyRunningAnimations++;
            }
        });
    }

    private void setTransitionListener(android.transition.Transition transition) {
        transition.addListener(new android.transition.TransitionListenerAdapter() {
            @Override
            public void onTransitionStart(android.transition.Transition transition) {
                currentlyRunningAnimations++;
            }

            @Override
            public void onTransitionEnd(android.transition.Transition transition) {
                currentlyRunningAnimations--;
            }

            @Override
            public void onTransitionCancel(android.transition.Transition transition) {
                currentlyRunningAnimations--;
            }

            @Override
            public void onTransitionPause(android.transition.Transition transition) {
                currentlyRunningAnimations--;
            }

            @Override
            public void onTransitionResume(android.transition.Transition transition) {
                currentlyRunningAnimations++;
            }
        });
    }

    public boolean isTransitioning() {
        return currentlyRunningAnimations != 0;
    }
}
