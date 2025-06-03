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

package com.stario.launcher.activities;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.util.Pair;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bosphere.fadingedgelayout.FadingEdgeLayout;
import com.stario.launcher.R;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.sheet.SheetDialogFragment;
import com.stario.launcher.sheet.SheetType;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.common.DragShadowBuilder;
import com.stario.launcher.ui.utils.UiUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class PageManager extends ThemedActivity {
    private static final String TAG = "PageManager";

    private final Hashtable<View, Class<? extends SheetDialogFragment>> pages;
    private final List<Pair<ConstraintLayout, SheetType>> placeholders;
    private final LocalBroadcastManager broadcastManager;

    private ConstraintLayout pageContainer;
    private SharedPreferences preferences;
    private LayoutInflater inflater;
    private boolean dragging;
    private View homePage;

    public PageManager() {
        this.broadcastManager = LocalBroadcastManager.getInstance(this);
        this.placeholders = new ArrayList<>();
        this.pages = new Hashtable<>();
        this.dragging = false;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.page_manager);

        preferences = getSharedPreferences(Entry.SHEET);

        inflater = LayoutInflater.from(this);
        UiUtils.Notch.applyNotchMargin(getRoot(), UiUtils.Notch.INVERSE);

        pageContainer = findViewById(R.id.page_container);
        homePage = findViewById(R.id.home);

        invalidateViews();

        homePage.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft,
                                            oldTop, oldRight, oldBottom) -> {
            homePage.post(() -> {
                for (View page : pages.keySet()) {
                    measure(page);
                }
            });
        });

        loadPlaceholders();
        loadPages(pages);
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

                                Intent intent = new Intent(Launcher.ACTION_MOVE_SHEET);
                                intent.putExtra(Launcher.INTENT_SHEET_CLASS_EXTRA, clazz);
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

                    dragging = false;
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                }
            });
        }
    }

    private void loadPages(Map<View, Class<? extends SheetDialogFragment>> pages) {
        List<Pair<SheetType, Class<? extends SheetDialogFragment>>> list =
                SheetType.getActiveSheets(getSharedPreferences(Entry.SHEET));

        for (Pair<SheetType, Class<? extends SheetDialogFragment>> pair : list) {
            pages.put(inflatePage(pair.first, pair.second), pair.second);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration configuration) {
        super.onConfigurationChanged(configuration);

        invalidateViews();
    }

    private View inflatePage(SheetType type, Class<? extends SheetDialogFragment> clazz) {
        View page = inflater.inflate(R.layout.system_page, pageContainer, false);
        page.setClipToOutline(true);
        measure(page);

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

            ((TextView) page.findViewById(R.id.name)).setText(name);
        }

        View gradient = page.findViewById(R.id.gradient_background);
        gradient.setOnDragListener((v, event) -> {
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

                    Intent intent = new Intent(Launcher.ACTION_MOVE_SHEET);
                    intent.putExtra(Launcher.INTENT_SHEET_CLASS_EXTRA,
                            new Class[]{pages.get(draggedPage), pages.get(otherPage)});
                    broadcastManager.sendBroadcastSync(intent);
                }

                draggedPage.setVisibility(View.VISIBLE);

                dragging = false;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

                return true;
            }

            if (event.getAction() == DragEvent.ACTION_DROP) {
                ViewParent pageParent = page.getParent();
                if (!(pageParent instanceof ViewGroup)) {
                    return false;
                }

                ViewGroup dropTargetContainer = (ViewGroup) pageParent;
                View draggedPage = (View) event.getLocalState();
                ViewParent parent = draggedPage.getParent();

                if (!(parent instanceof ViewGroup) ||
                        parent.equals(dropTargetContainer)) {
                    return false;
                }

                ViewGroup originalContainer = (ViewGroup) parent;
                View pageInDropTarget = dropTargetContainer.getChildAt(0);

                dropTargetContainer.removeView(pageInDropTarget);
                originalContainer.addView(pageInDropTarget);

                for (Pair<ConstraintLayout, SheetType> pair : placeholders) {
                    if (pair.first.equals(originalContainer)) {
                        //noinspection DataFlowIssue
                        preferences.edit()
                                .putString(pages.get(pageInDropTarget)
                                        .getName(), pair.second.toString())
                                .apply();
                        break;
                    }
                }

                originalContainer.removeView(draggedPage);
                dropTargetContainer.addView(draggedPage);

                for (Pair<ConstraintLayout, SheetType> pair : placeholders) {
                    if (pair.first.equals(dropTargetContainer)) {
                        //noinspection DataFlowIssue
                        preferences.edit()
                                .putString(pages.get(draggedPage)
                                        .getName(), pair.second.toString())
                                .apply();
                        break;
                    }
                }

                Intent intent = new Intent(Launcher.ACTION_MOVE_SHEET);
                intent.putExtra(Launcher.INTENT_SHEET_CLASS_EXTRA,
                        new Class[]{pages.get(draggedPage), pages.get(pageInDropTarget)});
                broadcastManager.sendBroadcastSync(intent);
                
                dragging = false;
                draggedPage.setVisibility(View.VISIBLE);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

                return true;
            }

            return false;
        });
        gradient.setOnTouchListener(new View.OnTouchListener() {
            private final Point touchPoint;

            {
                this.touchPoint = new Point();

                gradient.setOnLongClickListener(view -> {
                    ClipData dragData = new ClipData(
                            clazz.getName(),
                            new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN},
                            new ClipData.Item((CharSequence) page.getTag())
                    );

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

        int edgeLength = Math.min(params.height, params.width) / 3;
        ((FadingEdgeLayout) page.findViewById(R.id.fader))
                .setFadeSizes(edgeLength, edgeLength, edgeLength, edgeLength);

        page.setLayoutParams(params);
    }

    private void invalidateViews() {
        ConstraintLayout.LayoutParams params =
                (ConstraintLayout.LayoutParams) pageContainer.getLayoutParams();

        if (Measurements.isLandscape()) {
            params.height = ConstraintLayout.LayoutParams.MATCH_PARENT;
            params.width = 0;
            params.dimensionRatio = "H,9:16";
        } else {
            params.width = ConstraintLayout.LayoutParams.MATCH_PARENT;
            params.height = 0;
            params.dimensionRatio = "W,16:9";
        }

        pageContainer.setLayoutParams(params);
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
