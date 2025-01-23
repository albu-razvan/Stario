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

package com.stario.launcher.sheet.drawer.search;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
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
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.sheet.drawer.search.recyclers.OnSearchRecyclerVisibilityChangeListener;
import com.stario.launcher.sheet.drawer.search.recyclers.OnVisibilityChangeListener;
import com.stario.launcher.sheet.drawer.search.recyclers.SearchRecyclerItemAnimator;
import com.stario.launcher.sheet.drawer.search.recyclers.adapters.AppAdapter;
import com.stario.launcher.sheet.drawer.search.recyclers.adapters.WebAdapter;
import com.stario.launcher.sheet.drawer.search.recyclers.adapters.suggestions.AutosuggestAdapter;
import com.stario.launcher.sheet.drawer.search.recyclers.adapters.suggestions.OptionAdapter;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.keyboard.ImeAnimationController;
import com.stario.launcher.ui.keyboard.KeyboardHeightProvider;
import com.stario.launcher.ui.recyclers.DividerItemDecorator;
import com.stario.launcher.ui.recyclers.RecyclerItemAnimator;
import com.stario.launcher.ui.recyclers.async.InflationType;
import com.stario.launcher.utils.UiUtils;
import com.stario.launcher.utils.Utils;
import com.stario.launcher.utils.animation.Animation;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SearchFragment extends Fragment {
    public static final String TAG = "SearchFragment";
    public static final int MAX_APP_QUERY_ITEMS = 4;
    private SearchLayoutTransition searchLayoutTransition;
    private KeyboardHeightProvider heightProvider;
    private SharedPreferences searchPreferences;
    private ImeAnimationController controller;
    private KeyPreImeListeningEditText search;
    private ConstraintLayout searchContainer;
    private ThemedActivity activity;
    private ViewGroup content;
    private View webContainer;
    private View base;

    public SearchFragment() {
        super();

        if (Utils.isMinimumSDK(Build.VERSION_CODES.R)) {
            this.controller = new ImeAnimationController();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        if (!(context instanceof ThemedActivity)) {
            throw new RuntimeException("Parent activity is not of type ThemedActivity.");
        }

        activity = (ThemedActivity) context;
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        searchPreferences = activity.getSharedPreferences(Entry.SEARCH);

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
        View unauthorized = root.findViewById(R.id.unauthorized);
        View searching = root.findViewById(R.id.searching);
        View hint = root.findViewById(R.id.result_hint);
        FadingEdgeLayout fader = root.findViewById(R.id.fader);
        searchContainer = root.findViewById(R.id.search_container);
        webContainer = root.findViewById(R.id.web_container);
        search = root.findViewById(R.id.search);
        content = root.findViewById(R.id.content);
        base = root.findViewById(R.id.base);

        searchLayoutTransition = new SearchLayoutTransition();
        LayoutTransition nativeTransitionCast = searchLayoutTransition.getUnrefinedTransition();

        nativeTransitionCast.setDuration(LayoutTransition.CHANGING, Animation.MEDIUM.getDuration());

        content.setLayoutTransition(nativeTransitionCast);

        RecyclerView apps = root.findViewById(R.id.apps);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(activity, MAX_APP_QUERY_ITEMS);

        apps.setLayoutManager(gridLayoutManager);
        apps.setItemAnimator(null);

        AppAdapter appAdapter = new AppAdapter(activity);
        appAdapter.setInflationType(InflationType.SYNCED);
        appAdapter.setOnVisibilityChangeListener(new OnSearchRecyclerVisibilityChangeListener(searchLayoutTransition));

        apps.setAdapter(appAdapter);

        RecyclerView suggestions = root.findViewById(R.id.suggestions);

        LinearLayoutManager suggestionsLinearLayoutManager = new LinearLayoutManager(activity);

        suggestions.setLayoutManager(suggestionsLinearLayoutManager);
        suggestions.addItemDecoration(new DividerItemDecorator(activity, MaterialDividerItemDecoration.VERTICAL));
        suggestions.setItemAnimator(new SearchRecyclerItemAnimator(Animation.MEDIUM));

        AutosuggestAdapter autosuggestAdapter = new AutosuggestAdapter(activity);
        autosuggestAdapter.setOnVisibilityChangeListener(
                new OnSearchRecyclerVisibilityChangeListener(searchLayoutTransition)
        );

        suggestions.setAdapter(autosuggestAdapter);

        RecyclerView options = root.findViewById(R.id.options);

        options.setClipToOutline(true);

        LinearLayoutManager optionsLinearLayoutManager = new LinearLayoutManager(activity);

        options.setLayoutManager(optionsLinearLayoutManager);
        options.addItemDecoration(new DividerItemDecorator(activity, MaterialDividerItemDecoration.VERTICAL));
        options.setItemAnimator(new SearchRecyclerItemAnimator(Animation.MEDIUM));

        OptionAdapter optionAdapter = new OptionAdapter(activity);
        optionAdapter.setOnVisibilityChangeListener(new OnSearchRecyclerVisibilityChangeListener(searchLayoutTransition));

        options.setAdapter(optionAdapter);

        RecyclerView web = root.findViewById(R.id.web);

        LinearLayoutManager webLinearLayoutManager = new LinearLayoutManager(activity);

        web.setLayoutManager(webLinearLayoutManager);
        web.addItemDecoration(new DividerItemDecorator(activity,
                MaterialDividerItemDecoration.VERTICAL, Measurements.dpToPx(10)));
        web.setItemAnimator(new RecyclerItemAnimator(RecyclerItemAnimator.APPEARANCE, Animation.MEDIUM));

        //noinspection ExtractMethodRecommender
        WebAdapter webAdapter = new WebAdapter(activity);
        webAdapter.setOnVisibilityChangeListener(new OnVisibilityChangeListener() {
            private final OnVisibilityChangeListener listener =
                    new OnSearchRecyclerVisibilityChangeListener(searchLayoutTransition);

            @Override
            public void onPreChange(View view, int visibility) {
                listener.onPreChange(view, visibility);
            }

            @Override
            public void onChange(View view, int visibility) {
                listener.onChange(view, visibility);

                if (visibility == View.VISIBLE) {
                    searching.setVisibility(View.GONE);
                } else {
                    searching.setVisibility(View.VISIBLE);
                }

                unauthorized.setVisibility(View.GONE);
            }
        });
        webAdapter.setUnauthorizedListener(() -> {
            searching.setVisibility(View.GONE);
            unauthorized.setVisibility(View.VISIBLE);
        });

        web.setAdapter(webAdapter);

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

                if (isKeyboardVisible()) {
                    scrollView.smoothScrollTo(0, 0, Animation.LONG.getDuration());
                }

                content.setPadding(content.getPaddingLeft(), content.getPaddingTop(),
                        content.getPaddingRight(), height + hint.getHeight());

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
            AtomicBoolean isAScroll = new AtomicBoolean(false);

            scrollView.setOnPreScrollListener(new PreEventNestedScrollView.PreEvent() {
                @Override
                public boolean onPreScroll(int delta) {
                    if (controller.isAnimationInProgress() &&
                            !controller.isSettleAnimationInProgress() &&
                            ((scrollView.getScrollY() == 0 && delta > 0 && !controller.isCurrentPositionFullyShown()) ||
                                    (delta < 0 && !controller.isCurrentPositionFullyHidden()))) {

                        return controller.insetBy(-delta) != 0;
                    }

                    boolean isKeyboardVisible = isKeyboardVisible();

                    if (delta < 0 && !isKeyboardVisible) {
                        return false;
                    }

                    if ((delta > 0 && isKeyboardVisible) || scrollView.getScrollY() > 0) {
                        return false;
                    }

                    return !isAScroll.get() || controller.isRequestPending();
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

                    boolean isKeyboardVisible = isKeyboardVisible();

                    if (velocity > 0 && !isKeyboardVisible) {
                        return false;
                    }

                    if ((velocity < 0 && isKeyboardVisible) || scrollView.getScrollY() > 0) {
                        return false;
                    }

                    return !isAScroll.get() || controller.isRequestPending();
                }
            });

            scrollView.setOnTouchListener(new View.OnTouchListener() {
                private Float x = null;
                private Float y = null;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN ||
                            event.getAction() == MotionEvent.ACTION_MOVE) {
                        isPointerDown.set(true);

                        if (x == null && y == null) {
                            x = event.getRawX();
                            y = event.getRawY();
                        } else if (x != null && y != null) {
                            float absDeltaX = Math.abs(event.getRawX() - x);
                            float absDeltaY = Math.abs(event.getRawY() - y);

                            float slop = ViewConfiguration.get(activity)
                                    .getScaledTouchSlop();

                            if (absDeltaX > absDeltaY && absDeltaX > slop) {
                                x = null;
                                isAScroll.set(true);
                            } else if (absDeltaY > absDeltaX && absDeltaY > slop) {
                                y = null;
                                isAScroll.set(true);

                                if (!controller.isAnimationInProgress() &&
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
                                    });
                                }
                            }
                        }
                    }

                    if (event.getAction() == MotionEvent.ACTION_UP ||
                            event.getAction() == MotionEvent.ACTION_CANCEL) {
                        isPointerDown.set(false);
                        isAScroll.set(false);

                        scrollView.post(() -> {
                            if (controller.isAnimationInProgress() &&
                                    !controller.isSettleAnimationInProgress()) {
                                controller.finish();
                            }
                        });

                        x = null;
                        y = null;
                    }

                    return false;
                }
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
                            if (height != startBottom) {
                                endBottom = height;

                                heightProvider.removeKeyboardHeightObserver(this);
                            }
                        }
                    });

                    return bounds;
                }

                @NonNull
                @Override
                public WindowInsetsCompat onProgress(@NonNull WindowInsetsCompat insets, @NonNull List<WindowInsetsAnimationCompat> runningAnimations) {
                    if (imeAnimation == null) {
                        for (WindowInsetsAnimationCompat animation : runningAnimations) {
                            if ((animation.getTypeMask() & WindowInsetsCompat.Type.ime()) != 0) {
                                imeAnimation = animation;

                                break;
                            }
                        }
                    }

                    if (imeAnimation != null) {
                        if (imeAnimation.getDurationMillis() > 0) {
                            float delta = endBottom - startBottom;
                            float translation = delta * ((delta < 0 ? 1 : 0) - imeAnimation.getInterpolatedFraction());

                            updateContentTranslation(translation);
                        } else if (controller.isAnimationInProgress()) {
                            float delta = endBottom - startBottom;
                            float fraction = controller.getExpandedFraction();
                            float translation = Math.abs(delta) * -fraction;

                            updateContentTranslation(translation);
                        }
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
                if (searchPreferences.getBoolean(WebAdapter.SEARCH_RESULTS, false)) {
                    if (webContainer.getVisibility() != View.VISIBLE) {
                        searchLayoutTransition.setAnimate(false);
                        searchLayoutTransition.cancel();

                        base.setVisibility(View.GONE);
                        webContainer.setVisibility(View.VISIBLE);
                        searching.setVisibility(View.VISIBLE);
                        unauthorized.setVisibility(View.GONE);
                    }

                    Editable text = search.getText();
                    if (text != null && text.length() > 0) {
                        webAdapter.update(text.toString());

                        return true;
                    }
                }

                if (base.getVisibility() == View.VISIBLE) {
                    return appAdapter.submit() || autosuggestAdapter.submit() || optionAdapter.submit();
                }
            }

            return false;
        });

        if (!Utils.isMinimumSDK(Build.VERSION_CODES.TIRAMISU)) {
            search.addOnKeyUp(KeyEvent.KEYCODE_BACK, this::onBackPressed);
        }

        search.addTextChangedListener(new TextWatcher() {
            private static final int SUGGESTION_PROCESS_INTERVAL = 200;
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

                hint.setVisibility(!filteredQuery.isEmpty() &&
                        searchPreferences.getBoolean(WebAdapter.SEARCH_RESULTS, false) ?
                        View.VISIBLE : View.GONE);
                hint.post(() ->
                        content.setPadding(content.getPaddingLeft(), content.getPaddingTop(), content.getPaddingRight(),
                                (Utils.isMinimumSDK(Build.VERSION_CODES.R) ? 0 : heightProvider.getKeyboardHeight()) +
                                        hint.getVisibility() == View.VISIBLE ? hint.getHeight() : 0));

                if (filteredQuery.equals(query)) {
                    if (base.getVisibility() == View.VISIBLE) {
                        appAdapter.update(query);
                        optionAdapter.update(query);

                        long timeStamp = System.currentTimeMillis();

                        if (query.isBlank()) {
                            autosuggestAdapter.update(query);
                        } else {
                            // don't process text changes too often
                            search.postDelayed(() -> {
                                if (timeStamp == lastRegisteredTimestamp) {
                                    autosuggestAdapter.update(query);
                                }
                            }, SUGGESTION_PROCESS_INTERVAL);
                        }

                        lastRegisteredTimestamp = System.currentTimeMillis();
                    }
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

    @RequiresApi(Build.VERSION_CODES.R)
    private void updateContentTranslation(float translation) {
        if (translation != searchContainer.getTranslationY()) {
            content.setTranslationY(-translation);
            searchContainer.setTranslationY(translation);
        }
    }

    private boolean isKeyboardVisible() {
        View view = getView();

        if (view != null) {
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(getView());

            if (insets != null) {
                return insets.isVisible(WindowInsetsCompat.Type.ime());
            }
        }

        return false;
    }

    /**
     * @return true if this instance wants to prevent the back event
     */
    public boolean onBackPressed() {
        if (!isKeyboardVisible()) {
            return false;
        }

        UiUtils.hideKeyboard(search);

        return true;
    }

    @Override
    public void onStop() {
        UiUtils.hideKeyboard(search);

        super.onStop();

        search.setText(null);
        base.setVisibility(View.VISIBLE);
        webContainer.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();

        SearchEngine engine = SearchEngine.getEngine(activity);
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