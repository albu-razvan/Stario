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

package com.stario.launcher.activities.pages;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.util.Pair;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.stario.launcher.R;
import com.stario.launcher.activities.launcher.sheets.LauncherSheets;
import com.stario.launcher.activities.pages.insert.InsertPageDialog;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.sheet.SheetDialogFragment;
import com.stario.launcher.sheet.SheetType;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.common.DragShadowBuilder;
import com.stario.launcher.ui.utils.UiUtils;
import com.stario.launcher.ui.utils.animation.Animation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class PageManager extends ThemedActivity {
    private static final String TAG = "PageManager";
    private static final float ADD_BUTTON_ANIMATION_SCALE_FACTOR = 0.5f;

    private final Hashtable<View, Class<? extends SheetDialogFragment>> pages;
    private final List<Pair<ConstraintLayout, SheetType>> placeholders;

    private LocalBroadcastManager broadcastManager;
    private ConstraintLayout pagesContainer;
    private SharedPreferences preferences;
    private LayoutInflater inflater;
    private ViewGroup container;
    private boolean dragging;
    private View addLabel;
    private View homePage;
    private ViewGroup add;

    public PageManager() {
        this.broadcastManager = null;
        this.placeholders = new ArrayList<>();
        this.pages = new Hashtable<>();
        this.dragging = false;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.page_manager);

        broadcastManager = LocalBroadcastManager.getInstance(this);
        preferences = getApplicationContext().getSharedPreferences(Entry.SHEET);

        inflater = LayoutInflater.from(this);

        pagesContainer = findViewById(R.id.pages_container);
        container = findViewById(R.id.container);
        homePage = findViewById(R.id.home);
        add = findViewById(R.id.add);
        addLabel = add.findViewById(R.id.add_label);

        add.setOnClickListener(new View.OnClickListener() {
            private InsertPageDialog dialog;
            private boolean showing = false;

            @Override
            public void onClick(View view) {
                if (dialog == null) {
                    dialog = new InsertPageDialog(PageManager.this, item -> {
                        SheetType type = getAvailableSpace(item.first);

                        preferences.edit()
                                .putString(item.second.getName(), type.toString())
                                .apply();

                        pages.put(inflatePage(type, item.second), item.second);

                        if (pages.size() == SheetDialogFragment.IMPLEMENTATIONS.size() ||
                                pages.size() == placeholders.size()) {
                            hideAddButton();
                        }

                        Intent intent = new Intent(LauncherSheets.ACTION_ADD_SHEET);
                        intent.putExtra(LauncherSheets.INTENT_SHEET_CLASS_EXTRA, item.second);
                        broadcastManager.sendBroadcastSync(intent);
                    });

                    dialog.setOnDismissListener(dialog -> showing = false);
                }

                if (!showing) {
                    dialog.setItems(getItems());
                    dialog.show();

                    showing = true;
                }
            }

            private SheetType getAvailableSpace(SheetType desiredLocation) {
                SheetType firstFreeSpace = SheetType.UNDEFINED;

                for (Pair<ConstraintLayout, SheetType> pair : placeholders) {
                    if (pair.first.getChildCount() == 0) {
                        if (desiredLocation == pair.second) {
                            firstFreeSpace = pair.second;
                            break;
                        } else if (firstFreeSpace == SheetType.UNDEFINED) {
                            firstFreeSpace = pair.second;
                        }
                    }
                }

                return firstFreeSpace;
            }

            private List<Pair<SheetType, Class<? extends SheetDialogFragment>>> getItems() {
                List<Pair<SheetType, Class<? extends SheetDialogFragment>>> items = new ArrayList<>();

                for (Class<? extends SheetDialogFragment> clazz : SheetDialogFragment.IMPLEMENTATIONS) {
                    boolean isActive = false;

                    for (Class<? extends SheetDialogFragment> tester : pages.values()) {
                        if (clazz.equals(tester)) {
                            isActive = true;
                            break;
                        }
                    }

                    if (!isActive) {
                        items.add(new Pair<>(SheetType.getDefaultSheetTypeForSheetDialogFragment(
                                PageManager.this, clazz), clazz));
                    }
                }

                return items;
            }
        });

        loadParams();

        homePage.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft,
                                            oldTop, oldRight, oldBottom) -> homePage.post(() -> {
            for (View page : pages.keySet()) {
                measure(page);
            }
        }));

        loadPlaceholders();
        loadPages(pages);

        if (pages.size() == SheetDialogFragment.IMPLEMENTATIONS.size() ||
                pages.size() == placeholders.size()) {
            add.setVisibility(View.GONE);
        }

        findViewById(R.id.gradient).animate().alpha(0.5f).setDuration(Animation.EXTENDED.getDuration());
        UiUtils.Notch.applyNotchMargin(getRoot(), UiUtils.Notch.Treatment.INVERSE);
    }

    @SuppressLint("FindViewByIdCast")
    private void loadPlaceholders() {
        placeholders.add(new Pair<>(findViewById(R.id.left), SheetType.LEFT_SHEET));
        placeholders.add(new Pair<>(findViewById(R.id.top), SheetType.TOP_SHEET));
        placeholders.add(new Pair<>(findViewById(R.id.right), SheetType.RIGHT_SHEET));
        placeholders.add(new Pair<>(findViewById(R.id.bottom), SheetType.BOTTOM_SHEET));

        for (Pair<ConstraintLayout, SheetType> pair : placeholders) {
            ConstraintLayout view = pair.first;
            SheetType type = pair.second;

            view.setClipChildren(false);
            view.setClipToOutline(false);
            view.setClipToPadding(false);

            view.setOnDragListener(new View.OnDragListener() {
                @Override
                public boolean onDrag(View v, DragEvent event) {
                    int action = event.getAction();
                    View draggedView = (View) event.getLocalState();

                    if (action == DragEvent.ACTION_DRAG_STARTED) {
                        dragging = true;

                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

                        return true;
                    } else if (action == DragEvent.ACTION_DROP) {
                        ViewParent parent = draggedView.getParent();

                        if (parent instanceof ViewGroup && !parent.equals(view)) {
                            ViewGroup group = (ViewGroup) parent;

                            group.removeView(draggedView);
                            view.addView(draggedView);

                            Class<? extends SheetDialogFragment> clazz = pages.get(draggedView);

                            if (clazz != null) {
                                preferences.edit()
                                        .putString(clazz.getName(), type.toString())
                                        .apply();

                                Intent intent = new Intent(LauncherSheets.ACTION_MOVE_SHEET);
                                intent.putExtra(LauncherSheets.INTENT_SHEET_CLASS_EXTRA, clazz);
                                broadcastManager.sendBroadcastSync(intent);
                            }
                        }

                        reset(draggedView);

                        return true;
                    } else if (action == DragEvent.ACTION_DRAG_ENDED) {
                        reset(draggedView);

                        return true;
                    }

                    return false;
                }

                private void reset(View view) {
                    view.setVisibility(View.VISIBLE);
                    View removeButton = view.findViewById(R.id.remove);
                    if (removeButton != null) {
                        removeButton.setVisibility(View.VISIBLE);
                    }

                    dragging = false;
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                }
            });
        }
    }

    private void loadPages(Map<View, Class<? extends SheetDialogFragment>> pages) {
        List<Pair<SheetType, Class<? extends SheetDialogFragment>>> list =
                SheetType.getStoredSheets(this);

        for (Pair<SheetType, Class<? extends SheetDialogFragment>> pair : list) {
            if (pair.first != SheetType.UNDEFINED) {
                pages.put(inflatePage(pair.first, pair.second), pair.second);
            }
        }
    }

    private void showAddButton() {
        if (add.getVisibility() == View.VISIBLE) {
            return;
        }

        add.setOnTouchListener(null);
        add.animate().cancel();

        add.setAlpha(0f);
        add.setScaleX(ADD_BUTTON_ANIMATION_SCALE_FACTOR);
        add.setScaleY(ADD_BUTTON_ANIMATION_SCALE_FACTOR);

        add.setVisibility(View.VISIBLE);

        add.animate()
                .alpha(1f)
                .scaleY(1f)
                .scaleX(1f)
                .setDuration(Animation.MEDIUM.getDuration())
                .setInterpolator(new FastOutSlowInInterpolator())
                .setListener(null)
                .start();
    }

    private void hideAddButton() {
        if (add.getVisibility() == View.GONE) {
            return;
        }

        // noinspection ClickableViewAccessibility
        add.setOnTouchListener((v, ev) -> false);
        add.animate()
                .alpha(0f)
                .scaleY(ADD_BUTTON_ANIMATION_SCALE_FACTOR)
                .scaleX(ADD_BUTTON_ANIMATION_SCALE_FACTOR)
                .setDuration(Animation.MEDIUM.getDuration())
                .setInterpolator(new FastOutSlowInInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        add.setVisibility(View.GONE);
                    }
                })
                .start();
    }

    private View inflatePage(SheetType type, Class<? extends SheetDialogFragment> clazz) {
        ViewGroup page = (ViewGroup) inflater.inflate(R.layout.system_page,
                pagesContainer, false);
        page.setClipToOutline(false);
        page.setClipChildren(false);
        measure(page);

        View remove = page.findViewById(R.id.remove);
        remove.setOnClickListener(v -> {
            ViewParent parent = page.getParent();

            if (parent != null) {
                ((ViewGroup) parent).removeView(page);
                pages.remove(page);

                showAddButton();

                preferences.edit()
                        .putString(clazz.getName(), SheetType.UNDEFINED.toString())
                        .apply();

                Intent intent = new Intent(LauncherSheets.ACTION_REMOVE_SHEET);
                intent.putExtra(LauncherSheets.INTENT_SHEET_CLASS_EXTRA, clazz);
                broadcastManager.sendBroadcastSync(intent);
            }
        });

        String name = null;
        try {
            Method method = clazz.getMethod("getName");
            name = (String) method.invoke(null);
        } catch (NoSuchMethodException | InvocationTargetException exception) {
            Log.e(TAG, "inflatePage: " + clazz.getName() +
                    " does not implement getName(). Defaulting to class name...");
        } catch (IllegalAccessException exception) {
            Log.e(TAG, "inflatePage: " + clazz.getName() +
                    " getName() method is not publicly visible. Defaulting to class name...");
        } catch (ClassCastException exception) {
            Log.e(TAG, "inflatePage: " + clazz.getName() +
                    " getName() return type is not " + String.class.getName() + ". Defaulting to class name...");
        } finally {
            if (name == null) {
                name = clazz.getSimpleName();
            }

            ((TextView) page.findViewById(R.id.name))
                    .setText(name.replace(" ", System.lineSeparator()));
        }

        View pageContainer = page.findViewById(R.id.page_container);
        pageContainer.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                if (event.getAction() == DragEvent.ACTION_DROP) {
                    ViewParent pageParent = page.getParent();
                    if (!(pageParent instanceof ViewGroup)) {
                        return false;
                    }

                    View draggedPage = (View) event.getLocalState();
                    ViewParent parent = draggedPage.getParent();
                    ViewGroup view = (ViewGroup) pageParent;

                    if (parent instanceof ViewGroup && !parent.equals(view)) {
                        ViewGroup group = (ViewGroup) parent;

                        View otherPage = view.getChildAt(0);
                        view.removeView(otherPage);
                        group.addView(otherPage);

                        for (Pair<ConstraintLayout, SheetType> pair : placeholders) {
                            if (pair.first.equals(group)) {
                                preferences.edit()
                                        .putString(pages.get(otherPage).getName(), pair.second.toString())
                                        .apply();

                                break;
                            }
                        }

                        group.removeView(draggedPage);
                        view.addView(draggedPage);

                        for (Pair<ConstraintLayout, SheetType> pair : placeholders) {
                            if (pair.first.equals(view)) {
                                preferences.edit()
                                        .putString(pages.get(draggedPage).getName(), pair.second.toString())
                                        .apply();

                                break;
                            }
                        }

                        Intent intent = new Intent(LauncherSheets.ACTION_MOVE_SHEET);
                        intent.putExtra(LauncherSheets.INTENT_SHEET_CLASS_EXTRA,
                                new Class[]{pages.get(draggedPage), pages.get(otherPage)});
                        broadcastManager.sendBroadcastSync(intent);
                    }

                    reset(draggedPage);

                    return true;
                }

                return false;
            }

            private void reset(View view) {
                view.setVisibility(View.VISIBLE);
                View removeButton = view.findViewById(R.id.remove);
                if (removeButton != null) {
                    removeButton.setVisibility(View.VISIBLE);
                }

                dragging = false;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        });
        pageContainer.setOnTouchListener(new View.OnTouchListener() {
            private final Point touchPoint;

            {
                this.touchPoint = new Point();

                pageContainer.setHapticFeedbackEnabled(false);
                pageContainer.setOnLongClickListener(view -> {
                    Vibrations.getInstance().vibrate();

                    ClipData dragData = new ClipData(
                            clazz.getName(),
                            new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN},
                            new ClipData.Item((CharSequence) page.getTag())
                    );

                    remove.setVisibility(View.INVISIBLE);
                    View.DragShadowBuilder shadowBuilder = new DragShadowBuilder(page, touchPoint);
                    page.startDragAndDrop(dragData, shadowBuilder, page, 0);
                    page.setVisibility(View.INVISIBLE);


                    return true;
                });
            }

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                touchPoint.x = (int) event.getX();
                touchPoint.y = (int) event.getY();

                return false;
            }
        });

        for (Pair<ConstraintLayout, SheetType> pair : placeholders) {
            if (pair.second == type) {
                pair.first.addView(page);

                break;
            }
        }

        return page;
    }

    private void measure(View page) {
        ViewGroup.LayoutParams params = page.getLayoutParams();

        params.height = homePage.getMeasuredHeight();
        params.width = homePage.getMeasuredWidth();

        page.setLayoutParams(params);
    }

    private void loadParams() {
        ConstraintLayout.LayoutParams params =
                (ConstraintLayout.LayoutParams) pagesContainer.getLayoutParams();

        if (Measurements.isLandscape()) {
            params.height = ConstraintLayout.LayoutParams.MATCH_PARENT;
            params.width = 0;
            params.dimensionRatio = "H,9:16";
        } else {
            params.width = ConstraintLayout.LayoutParams.MATCH_PARENT;
            params.height = 0;
            params.dimensionRatio = "W,16:9";
        }

        if (Measurements.isLandscape()) {
            addLabel.setVisibility(View.VISIBLE);
        } else {
            addLabel.setVisibility(View.GONE);
        }

        pagesContainer.setLayoutParams(params);
        pagesContainer.forceLayout();
        container.requestLayout();
        add.forceLayout();
    }

    @Override
    protected boolean isOpaque() {
        return true;
    }

    @Override
    protected boolean isAffectedByBackGesture() {
        return !dragging;
    }
}