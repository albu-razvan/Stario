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
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.PreScrollListeningNestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bosphere.fadingedgelayout.FadingEdgeLayout;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import com.stario.launcher.R;
import com.stario.launcher.sheet.drawer.search.adapters.AppAdapter;
import com.stario.launcher.sheet.drawer.search.adapters.OptionAdapter;
import com.stario.launcher.sheet.drawer.search.adapters.WebAdapter;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.keyboard.KeyboardHeightProvider;
import com.stario.launcher.ui.measurements.Measurements;
import com.stario.launcher.ui.recyclers.DividerItemDecorator;
import com.stario.launcher.ui.recyclers.RecyclerItemAnimator;
import com.stario.launcher.ui.recyclers.async.InflationType;
import com.stario.launcher.utils.UiUtils;
import com.stario.launcher.utils.animation.Animation;

public class SearchFragment extends Fragment {
    public static final String TAG = "SearchFragment";
    private KeyboardHeightProvider heightProvider;
    private ThemedActivity activity;
    private ViewGroup content;
    private EditText search;

    @Override
    public void onAttach(@NonNull Context context) {
        if (!(context instanceof ThemedActivity)) {
            throw new RuntimeException("Parent activity is not of type ThemedActivity.");
        }

        activity = (ThemedActivity) context;

        super.onAttach(context);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        postponeEnterTransition();

        View root = inflater.inflate(R.layout.search, container, false);

        heightProvider = new KeyboardHeightProvider(activity);

        PreScrollListeningNestedScrollView scrollView = root.findViewById(R.id.scroller);
        FadingEdgeLayout fader = root.findViewById(R.id.fader);
        search = root.findViewById(R.id.search);
        content = root.findViewById(R.id.content);

        LayoutTransition contentLayoutTransition = new LayoutTransition();
        contentLayoutTransition.enableTransitionType(LayoutTransition.CHANGING);
        contentLayoutTransition.setDuration(LayoutTransition.CHANGING, Animation.MEDIUM.getDuration());
        contentLayoutTransition.setInterpolator(LayoutTransition.CHANGING, new DecelerateInterpolator(2));

        content.setLayoutTransition(contentLayoutTransition);

        RecyclerView apps = root.findViewById(R.id.apps);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(activity,
                Measurements.getListColumns());
        gridLayoutManager.setItemPrefetchEnabled(true);

        Measurements.addListColumnsListener(gridLayoutManager::setSpanCount);

        apps.setLayoutManager(gridLayoutManager);
        apps.setItemAnimator(new DefaultItemAnimator());

        AppAdapter appAdapter = new AppAdapter(activity, content, gridLayoutManager);
        appAdapter.setInflationType(InflationType.SYNCED);
        apps.setAdapter(appAdapter);

        RecyclerView web = root.findViewById(R.id.web);

        LinearLayoutManager webLinearLayoutManager = new LinearLayoutManager(activity);

        web.setLayoutManager(webLinearLayoutManager);
        web.addItemDecoration(new DividerItemDecorator(activity, MaterialDividerItemDecoration.VERTICAL));
        web.setItemAnimator(null);

        WebAdapter webAdapter = new WebAdapter(activity, content);
        web.setAdapter(webAdapter);

        RecyclerView options = root.findViewById(R.id.options);

        options.setClipToOutline(true);

        LinearLayoutManager optionsLinearLayoutManager = new LinearLayoutManager(activity);

        options.setLayoutManager(optionsLinearLayoutManager);
        options.addItemDecoration(new DividerItemDecorator(activity, MaterialDividerItemDecoration.VERTICAL));
        options.setItemAnimator(new RecyclerItemAnimator(RecyclerItemAnimator.APPEARANCE &
                RecyclerItemAnimator.DISAPPEARANCE));

        OptionAdapter optionAdapter = new OptionAdapter(activity, content, optionsLinearLayoutManager);
        options.setAdapter(optionAdapter);

        search.setFocusable(true);
        search.setFocusableInTouchMode(true);
        search.setShowSoftInputOnFocus(true);

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
                    value + heightProvider.getKeyboardHeight();
            search.requestLayout();

            fader.setFadeSizes(Measurements.getSysUIHeight(), 0,
                    value + Measurements.getDefaultPadding() +
                            search.getMeasuredHeight(), 0);
        });

        heightProvider.setKeyboardHeightObserver((height) -> {
            content.setPadding(content.getPaddingLeft(), content.getPaddingTop(),
                    content.getPaddingRight(), height);

            ((ViewGroup.MarginLayoutParams) search.getLayoutParams()).bottomMargin =
                    height + Measurements.getNavHeight();
            search.requestLayout();
        });

        search.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            fader.setFadeSizes(Measurements.getSysUIHeight(), 0,
                    Measurements.getNavHeight() + Measurements.getDefaultPadding() +
                            search.getMeasuredHeight(), 0);

            scrollView.setPadding(scrollView.getPaddingLeft(), scrollView.getPaddingTop(),
                    scrollView.getPaddingRight(), Measurements.getNavHeight() +
                            Measurements.getDefaultPadding() + search.getMeasuredHeight());
        });

        heightProvider.start();

        scrollView.setOnPreScrollListener(direction -> {
            if (direction == PreScrollListeningNestedScrollView.DOWN &&
                    scrollView.getScrollY() == 0 && heightProvider.getKeyboardHeight() > 0) {
                UiUtils.hideKeyboard(search);

                return false;
            }

            return true;
        });

        search.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                if (!appAdapter.submit()) {
                    webAdapter.submit();
                }

                return true;
            }

            return false;
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String query = editable.toString();
                String filteredQuery = query.replaceAll("(\\r\\n|\\r|\\n)", "");

                if (filteredQuery.equals(query)) {
                    appAdapter.update(query);
                    webAdapter.update(query);
                    optionAdapter.update(query);
                } else {
                    search.setText(filteredQuery);
                }
            }
        });

        options.post(() -> {
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