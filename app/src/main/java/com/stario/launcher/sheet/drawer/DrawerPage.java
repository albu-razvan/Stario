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

package com.stario.launcher.sheet.drawer;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.measurements.Measurements;
import com.stario.launcher.ui.recyclers.ScrollToTop;

public abstract class DrawerPage extends Fragment implements ScrollToTop {
    private int currentlyRunningAnimations;

    protected ThemedActivity activity;
    protected RecyclerView drawer;
    protected EditText search;
    protected TextView title;

    public DrawerPage() {
        super();

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

        drawer = root.findViewById(R.id.drawer);
        title = root.findViewById(R.id.title);
        search = container.getRootView().findViewById(R.id.search);

        drawer.setOverScrollMode(View.OVER_SCROLL_NEVER);

        drawer.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                updateTitleTransforms(recyclerView);
                recyclerView.post(() -> updateTitleTransforms(recyclerView)); // make sure it did position itself right
            }
        });

        title.setMinHeight(Measurements.dpToPx(Measurements.HEADER_SIZE_DP) +
                Measurements.spToPx(8));

        Measurements.addSysUIListener(value -> {
            drawer.setPadding(drawer.getPaddingLeft(),
                    value + (Measurements.isLandscape() ? Measurements.getDefaultPadding() :
                            Measurements.dpToPx(Measurements.HEADER_SIZE_DP) + Measurements.getDefaultPadding()),
                    drawer.getPaddingRight(),
                    drawer.getPaddingBottom());

            ((ViewGroup.MarginLayoutParams) title.getLayoutParams()).topMargin = value;
        });

        View searchContainer = (View) search.getParent();

        drawer.setPadding(drawer.getPaddingLeft(),
                drawer.getPaddingTop(), drawer.getPaddingRight(),
                searchContainer.getPaddingBottom() + (search.getBottom() - search.getTop()));

        search.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                drawer.setPadding(drawer.getPaddingLeft(),
                        drawer.getPaddingTop(), drawer.getPaddingRight(),
                        searchContainer.getPaddingBottom() + (bottom - top)));

        updateTitleVisibility();

        return root;
    }

    protected void updateTitleTransforms(RecyclerView recyclerView) {
        title.setTranslationY(-recyclerView.computeVerticalScrollOffset());

        int headerSize = Measurements.dpToPx(Measurements.HEADER_SIZE_DP);
        title.setAlpha(1f - recyclerView.computeVerticalScrollOffset() / (headerSize / 2f));
    }

    protected void updateTitleVisibility() {
        title.post(() -> {
            if (Measurements.isLandscape()) {
                title.setVisibility(View.GONE);
            } else {
                title.setVisibility(View.VISIBLE);

                updateTitleTransforms(drawer);
            }
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        updateTitleVisibility();
    }

    @Override
    public void onResume() {
        updateTitleVisibility();

        super.onResume();
    }

    @Override
    public void scrollToTop() {
        if (drawer != null) {
            drawer.scrollToPosition(0);
        }
    }

    protected abstract int getLayoutResID();

    protected abstract int getPosition();

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
