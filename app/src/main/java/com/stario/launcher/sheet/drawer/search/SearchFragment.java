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

package com.stario.launcher.sheet.drawer.search;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.PreEventNestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bosphere.fadingedgelayout.FadingEdgeLayout;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import com.stario.launcher.R;
import com.stario.launcher.sheet.drawer.search.recyclers.OnSearchRecyclerVisibilityChangeListener;
import com.stario.launcher.sheet.drawer.search.recyclers.SearchRecyclerItemAnimator;
import com.stario.launcher.sheet.drawer.search.recyclers.adapters.AppAdapter;
import com.stario.launcher.sheet.drawer.search.recyclers.adapters.OptionAdapter;
import com.stario.launcher.sheet.drawer.search.recyclers.adapters.WebAdapter;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.keyboard.KeyboardHeightProvider;
import com.stario.launcher.ui.measurements.Measurements;
import com.stario.launcher.ui.recyclers.DividerItemDecorator;
import com.stario.launcher.ui.recyclers.async.InflationType;
import com.stario.launcher.utils.UiUtils;
import com.stario.launcher.utils.Utils;
import com.stario.launcher.utils.animation.Animation;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SearchFragment extends Fragment {
    public static final String TAG = "SearchFragment";
    public static final int MAX_LIST_ITEMS = 4;
    private SearchLayoutTransition searchLayoutTransition;
    private KeyboardHeightProvider heightProvider;
    private ImeAnimationController controller;
    private BackGestureHandleEditText search;
    private ThemedActivity activity;
    private ViewGroup content;

    public SearchFragment() {
        super();

        if (Utils.isMinimumSDK(Build.VERSION_CODES.R)) {
            controller = new ImeAnimationController();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        if (!(context instanceof ThemedActivity)) {
            throw new RuntimeException("Parent activity is not of type ThemedActivity.");
        }

        activity = (ThemedActivity) context;
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        if (Utils.isMinimumSDK(Build.VERSION_CODES.R)) {
            controller.finish();
        } else {
            UiUtils.hideKeyboard(search);
        }

        super.onDetach();

        if (activity != null) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }

        activity = null;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        postponeEnterTransition();

        View root = inflater.inflate(R.layout.search, container, false);

        root.setOnTouchListener((v, event) -> true);

        heightProvider = new KeyboardHeightProvider(activity);

        PreEventNestedScrollView scrollView = root.findViewById(R.id.scroller);
        FadingEdgeLayout fader = root.findViewById(R.id.fader);
        RelativeLayout searchContainer = root.findViewById(R.id.search_container);
        search = root.findViewById(R.id.search);
        content = root.findViewById(R.id.content);

        searchLayoutTransition = new SearchLayoutTransition();
        LayoutTransition nativeTransitionCast = searchLayoutTransition.getUnrefinedTransition();

        nativeTransitionCast.setDuration(LayoutTransition.CHANGING, Animation.MEDIUM.getDuration());

        content.setLayoutTransition(nativeTransitionCast);

        RecyclerView apps = root.findViewById(R.id.apps);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(activity, MAX_LIST_ITEMS);

        apps.setLayoutManager(gridLayoutManager);
        apps.setItemAnimator(null);

        AppAdapter appAdapter = new AppAdapter(activity);
        appAdapter.setInflationType(InflationType.SYNCED);
        appAdapter.setOnVisibilityChangeListener(new OnSearchRecyclerVisibilityChangeListener(searchLayoutTransition));

        apps.setAdapter(appAdapter);

        RecyclerView web = root.findViewById(R.id.web);

        LinearLayoutManager webLinearLayoutManager = new LinearLayoutManager(activity);

        web.setLayoutManager(webLinearLayoutManager);
        web.addItemDecoration(new DividerItemDecorator(activity, MaterialDividerItemDecoration.VERTICAL));
        web.setItemAnimator(new SearchRecyclerItemAnimator(Animation.MEDIUM));

        WebAdapter webAdapter = new WebAdapter(activity);
        webAdapter.setOnVisibilityChangeListener(new OnSearchRecyclerVisibilityChangeListener(searchLayoutTransition));

        web.setAdapter(webAdapter);

        RecyclerView options = root.findViewById(R.id.options);

        options.setClipToOutline(true);

        LinearLayoutManager optionsLinearLayoutManager = new LinearLayoutManager(activity);

        options.setLayoutManager(optionsLinearLayoutManager);
        options.addItemDecoration(new DividerItemDecorator(activity, MaterialDividerItemDecoration.VERTICAL));
        options.setItemAnimator(new SearchRecyclerItemAnimator(Animation.MEDIUM));

        OptionAdapter optionAdapter = new OptionAdapter(activity);
        optionAdapter.setOnVisibilityChangeListener(new OnSearchRecyclerVisibilityChangeListener(searchLayoutTransition));

        options.setAdapter(optionAdapter);

        search.setFocusable(true);
        search.setFocusableInTouchMode(true);
        search.setShowSoftInputOnFocus(true);
        search.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

        Measurements.addSysUIListener(value -> {
            scrollView.setPadding(scrollView.getPaddingLeft(),
                    value + Measurements.getDefaultPadding(),
                    scrollView.getPaddingRight(), scrollView.getPaddingBottom());

            fader.setFadeSizes(value, 0,
                    Measurements.getNavHeight() + Measurements.getDefaultPadding() +
                            search.getMeasuredHeight(), 0);
        });

        Measurements.addNavListener(value -> {
            scrollView.setPadding(scrollView.getPaddingLeft(), scrollView.getPaddingTop(),
                    scrollView.getPaddingRight(), value +
                            Measurements.getDefaultPadding() + search.getMeasuredHeight());

            ((ViewGroup.MarginLayoutParams) search.getLayoutParams()).bottomMargin =
                    value + (Utils.isMinimumSDK(Build.VERSION_CODES.R) ? 0 : heightProvider.getKeyboardHeight());
            search.requestLayout();

            fader.setFadeSizes(Measurements.getSysUIHeight(), 0,
                    value + Measurements.getDefaultPadding() +
                            search.getMeasuredHeight(), 0);
        });

        if (!Utils.isMinimumSDK(Build.VERSION_CODES.R)) {
            heightProvider.addKeyboardHeightObserver((height) -> {
                searchLayoutTransition.setAnimate(false);

                if (height > 0) {
                    scrollView.smoothScrollTo(0, 0, Animation.LONG.getDuration());
                }

                content.setPadding(content.getPaddingLeft(), content.getPaddingTop(),
                        content.getPaddingRight(), height);

                ((ViewGroup.MarginLayoutParams) search.getLayoutParams()).bottomMargin =
                        height + Measurements.getNavHeight();
                search.requestLayout();

                search.post(() -> searchLayoutTransition.setAnimate(true));
            });
        }

        search.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            fader.setFadeSizes(Measurements.getSysUIHeight(), 0,
                    Measurements.getNavHeight() + Measurements.getDefaultPadding() +
                            search.getMeasuredHeight(), 0);

            scrollView.setPadding(scrollView.getPaddingLeft(), scrollView.getPaddingTop(),
                    scrollView.getPaddingRight(), Measurements.getNavHeight() +
                            Measurements.getDefaultPadding() + search.getMeasuredHeight());
        });

        heightProvider.start();

        if (Utils.isMinimumSDK(Build.VERSION_CODES.R)) {
            AtomicReference<Integer> lastVelocity = new AtomicReference<>(null);
            AtomicBoolean isPointerDown = new AtomicBoolean(false);
            AtomicBoolean skipNextAnimationFrame = new AtomicBoolean(false);

            scrollView.setOnPreScrollListener(new PreEventNestedScrollView.PreEvent() {
                @Override
                public boolean onPreScroll(int delta) {
                    if (controller.isAnimationInProgress() &&
                            !controller.isSettleAnimationInProgress() &&
                            ((scrollView.getScrollY() == 0 && delta > 0 && !controller.isCurrentPositionFullyShown()) ||
                                    (delta < 0 && !controller.isCurrentPositionFullyHidden()))) {

                        return controller.insetBy(-delta) != 0;
                    }

                    WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(scrollView);

                    if (insets != null) {
                        boolean isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());

                        if (delta < 0 && !isKeyboardVisible) {
                            return false;
                        }

                        if ((delta > 0 && isKeyboardVisible) || scrollView.getScrollY() > 0) {
                            return false;
                        }
                    }

                    return controller.isRequestPending();
                }

                @Override
                public boolean onPreFling(int velocity) {
                    lastVelocity.set(velocity);

                    if (scrollView.getScrollY() == 0 && controller.isAnimationInProgress()) {
                        if (!controller.isSettleAnimationInProgress()) {
                            controller.finish(-velocity);
                        }

                        return true;
                    }

                    WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(scrollView);

                    if (insets != null) {
                        boolean isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());

                        if (velocity > 0 && !isKeyboardVisible) {
                            return false;
                        }

                        if ((velocity < 0 && isKeyboardVisible) || scrollView.getScrollY() > 0) {
                            return false;
                        }
                    }

                    return controller.isRequestPending();
                }
            });

            scrollView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN ||
                        event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (!isPointerDown.get() && !controller.isAnimationInProgress() &&
                            !controller.isRequestPending()) {
                        controller.startControlRequest(search, new ImeAnimationController.StateListener() {
                            @Override
                            public void onReady() {
                                if (!isPointerDown.get()) {
                                    if (lastVelocity.get() != null) {
                                        if ((scrollView.getScrollY() == 0 && lastVelocity.get() < 0 && !controller.isCurrentPositionFullyShown()) ||
                                                (lastVelocity.get() > 0 && !controller.isCurrentPositionFullyHidden())) {
                                            controller.finish(-lastVelocity.get());
                                        } else {
                                            controller.finish();
                                        }
                                    } else {
                                        controller.finish();
                                    }

                                    lastVelocity.set(null);
                                }
                            }

                            @Override
                            public void onPreFinish() {
                                // prevent animation bug frame after finishing the inset controller animation
                                skipNextAnimationFrame.set(true);
                            }
                        });
                    }

                    isPointerDown.set(true);
                }

                if (event.getAction() == MotionEvent.ACTION_UP ||
                        event.getAction() == MotionEvent.ACTION_CANCEL) {
                    isPointerDown.set(false);

                    scrollView.post(() -> {
                        if (controller.isAnimationInProgress() &&
                                !controller.isSettleAnimationInProgress()) {
                            controller.finish();
                        }
                    });
                }

                return false;
            });

            ViewCompat.setWindowInsetsAnimationCallback(root, new WindowInsetsAnimationCompat
                    .Callback(WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_STOP) {
                private WindowInsetsAnimationCompat imeAnimation;
                private float startBottom;
                private float endBottom;

                @Override
                public void onPrepare(@NonNull WindowInsetsAnimationCompat animation) {
                    startBottom = heightProvider.getKeyboardHeight();
                }

                @NonNull
                @Override
                public WindowInsetsAnimationCompat.BoundsCompat onStart(@NonNull WindowInsetsAnimationCompat animation, @NonNull WindowInsetsAnimationCompat.BoundsCompat bounds) {
                    endBottom = 0;

                    heightProvider.addKeyboardHeightObserver(new KeyboardHeightProvider.KeyboardHeightObserver() {
                        @Override
                        public void onKeyboardHeightChanged(int height) {
                            endBottom = height;

                            heightProvider.removeKeyboardHeightObserver(this);
                        }
                    });

                    return bounds;
                }

                @NonNull
                @Override
                public WindowInsetsCompat onProgress(@NonNull WindowInsetsCompat insets, @NonNull List<WindowInsetsAnimationCompat> runningAnimations) {
                    // prevent controller animation bug frame here
                    if (!skipNextAnimationFrame.get()) {
                        if (imeAnimation == null) {
                            for (WindowInsetsAnimationCompat animation : runningAnimations) {
                                if ((animation.getTypeMask() & WindowInsetsCompat.Type.ime()) != 0) {
                                    imeAnimation = animation;

                                    break;
                                }
                            }
                        }

                        if (imeAnimation != null) {
                            float delta = endBottom - startBottom;
                            float translation = delta * ((delta < 0 ? 1 : 0) - imeAnimation.getInterpolatedFraction());

                            content.setTranslationY(-translation);
                            searchContainer.setTranslationY(translation);
                        }
                    } else {
                        skipNextAnimationFrame.set(false);
                    }

                    return insets;
                }

                @Override
                public void onEnd(@NonNull WindowInsetsAnimationCompat animation) {
                    imeAnimation = null;
                }
            });
        }

        search.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                return appAdapter.submit() || webAdapter.submit() || optionAdapter.submit();
            }

            return false;
        });

        if (Utils.isMinimumSDK(Build.VERSION_CODES.TIRAMISU)) {

        } else {
            search.setOnBackInvoked(() -> (controller != null && (controller.isRequestPending() ||
                    controller.isAnimationInProgress() || controller.isAnimationInProgress())));
        }

        search.addTextChangedListener(new TextWatcher() {
            private static final int INTERVAL = 300;
            private long lastRegisteredTimestamp = 0;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                scrollView.smoothScrollTo(0, 0, Animation.LONG.getDuration());

                String query = editable.toString();
                String filteredQuery = query.replaceAll("(\\r\\n|\\r|\\n)", "");

                if (filteredQuery.equals(query)) {
                    appAdapter.update(query);
                    optionAdapter.update(query);

                    long timeStamp = System.currentTimeMillis();

                    if (query.isBlank() || appAdapter.getItemCount() == 0) {
                        webAdapter.update(query);
                    } else {
                        // don't process text changes too often
                        search.postDelayed(() -> {
                            if (timeStamp == lastRegisteredTimestamp) {
                                webAdapter.update(query);
                            }
                        }, INTERVAL);
                    }

                    lastRegisteredTimestamp = System.currentTimeMillis();
                } else {
                    search.setText(filteredQuery);
                }
            }
        });

        content.post(() -> {
            startPostponedEnterTransition();

            UiUtils.showKeyboard(search);
        });

        return root;
    }

    @Override
    public void onStop() {
        super.onStop();
        search.setText(null);
    }

    @Override
    public void onResume() {
        super.onResume();

        SearchEngine engine = SearchEngine.engineFor(activity);
        search.setCompoundDrawablesWithIntrinsicBounds(
                engine.getDrawable(activity), null, null, null);
    }

    @Override
    public void onDestroy() {
        if (heightProvider != null) {
            heightProvider.close();
        }

        super.onDestroy();
    }
}