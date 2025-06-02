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

package com.stario.launcher.sheet.widgets.dialog;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.bosphere.fadingedgelayout.FadingEdgeLayout;
import com.stario.launcher.R;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.sheet.SheetDialogFragment;
import com.stario.launcher.sheet.SheetType;
import com.stario.launcher.sheet.behavior.SheetBehavior;
import com.stario.launcher.sheet.widgets.Widget;
import com.stario.launcher.sheet.widgets.WidgetSize;
import com.stario.launcher.sheet.widgets.configurator.WidgetConfigurator;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.popup.PopupMenu;
import com.stario.launcher.ui.widgets.WidgetGrid;
import com.stario.launcher.ui.widgets.WidgetHost;
import com.stario.launcher.ui.widgets.WidgetScroller;
import com.stario.launcher.ui.utils.UiUtils;
import com.stario.launcher.ui.utils.animation.Animation;

import java.util.PriorityQueue;

public class WidgetsDialog extends SheetDialogFragment {
    private static final String TAG = "WidgetsDialog";
    private static final int HOST_ID = 219672;
    private static final int MAX_COUNT = 15;
    private static final int CONFIGURATION_CODE = 3264614;
    private static int columnSize = 0;
    private ActivityResultLauncher<Intent> bindWidgetRequest;
    private WidgetConfigurator configurator;
    private SharedPreferences widgetStore;
    private WidgetSize pendingWidgetSize;
    private AppWidgetManager manager;
    private ThemedActivity activity;
    private ViewGroup placeholder;
    private LinearLayout content;
    private WidgetHost host;
    private WidgetGrid grid;

    public WidgetsDialog() {
        super();
    }

    public WidgetsDialog(SheetType type) {
        super(type);
    }

    public static String getName() {
        return "Widgets";
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        this.activity = (ThemedActivity) context;
        this.manager = AppWidgetManager.getInstance(activity);
        this.widgetStore = activity.getSharedPreferences(Entry.WIDGETS);

        bindWidgetRequest = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();

                        if (data != null) {
                            int identifier = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

                            if (identifier != -1 && pendingWidgetSize != null) {
                                configureWidget(manager, identifier, pendingWidgetSize);
                            }

                            pendingWidgetSize = null;
                        }
                    }
                });
    }

    @SuppressWarnings("ConstantConditions")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.widget_grid, container, false);

        content = view.findViewById(R.id.content);
        placeholder = view.findViewById(R.id.placeholder);
        grid = view.findViewById(R.id.grid);
        WidgetScroller scroller = view.findViewById(R.id.scroller);
        FadingEdgeLayout fader = view.findViewById(R.id.fader);

        View.OnClickListener showConfiguratorListener = (v) -> showConfigurator();

        placeholder.setOnClickListener(showConfiguratorListener);
        placeholder.findViewById(R.id.add_button)
                .setOnClickListener(showConfiguratorListener);

        content.setOnLongClickListener(v -> {
            showConfigurator();

            return true;
        });

        LayoutTransition transition = new LayoutTransition();

        transition.disableTransitionType(LayoutTransition.CHANGING);

        content.setLayoutTransition(transition);

        Measurements.addNavListener(value -> {
            fader.setFadeSizes(Measurements.getSysUIHeight() +
                            (Measurements.isLandscape() ? 0 : Measurements.getDefaultPadding()),
                    0, value + Measurements.getDefaultPadding(), 0);

            content.setPadding(content.getPaddingLeft(), content.getPaddingBottom(),
                    content.getPaddingRight(), value);
        });

        Measurements.addStatusBarListener(value -> {
            fader.setFadeSizes(value +
                            (Measurements.isLandscape() ? 0 : Measurements.getDefaultPadding()),
                    0, Measurements.getNavHeight() + Measurements.getDefaultPadding(), 0);

            content.setPadding(content.getPaddingLeft(), value,
                    content.getPaddingRight(), content.getPaddingBottom());
        });

        setOnBackPressed(() -> {
            if (scroller.canScrollVertically(1)) {
                scroller.smoothScrollTo(0, 0);
            } else {
                getBehavior().setState(SheetBehavior.STATE_COLLAPSED);
            }

            return false;
        });

        PriorityQueue<Widget> widgets = new PriorityQueue<>();

        for (int identifier : WidgetsDialog.this.getWidgetHost().getAppWidgetIds()) {
            if (widgetStore.contains(String.valueOf(identifier))) {
                String serial = widgetStore.getString(String.valueOf(identifier), null);

                Widget widget = Widget.deserialize(serial);

                if (widget != null) {
                    if (widgets.size() < MAX_COUNT) {
                        widgets.add(widget);
                    } else {
                        WidgetsDialog.this.getWidgetHost().deleteAppWidgetId(identifier);
                    }
                } else {
                    widgetStore.edit().remove(String.valueOf(identifier)).apply();
                }
            } else {
                WidgetsDialog.this.getWidgetHost()
                        .deleteAppWidgetId(identifier);
            }
        }

        UiUtils.runOnUIThread(new AttachRunnable(widgets));

        grid.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                columnSize = grid.computeCellSize());

        return view;
    }

    public static int getWidgetCellSize() {
        return columnSize;
    }

    private void showConfigurator() {
        if (configurator == null || !activity.equals(configurator.getContext())) {
            configurator = new WidgetConfigurator(activity, this::addWidget);

            configurator.setOnShowListener(dialog -> content.animate()
                    .alpha(0)
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .setDuration(Animation.MEDIUM.getDuration()));

            configurator.setOnDismissListener(dialog -> content.animate()
                    .alpha(1)
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .setDuration(Animation.MEDIUM.getDuration()));
        }

        configurator.show();
    }

    private void addWidget(AppWidgetProviderInfo info, WidgetSize size) {
        if (grid.getChildCount() <= MAX_COUNT) {
            int identifier = getWidgetHost().allocateAppWidgetId();
            boolean allowed = manager.bindAppWidgetIdIfAllowed(identifier, info.getProfile(), info.provider, null);

            if (!allowed) {
                if (bindWidgetRequest != null) {
                    Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);

                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, identifier);
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider);

                    pendingWidgetSize = size;
                    bindWidgetRequest.launch(intent);
                }
            } else {
                configureWidget(manager, identifier, size);
            }
        }

        configurator.dismiss();
    }

    private void configureWidget(AppWidgetManager manager, int identifier, WidgetSize size) {
        Widget widget = new Widget(identifier, grid.allocatePosition(), size);
        AppWidgetHostView host = createWidget(manager, widget);

        try {
            boolean result = activity.addOnActivityResultListener(CONFIGURATION_CODE, (resultCode, intent) -> {
                if (resultCode == Activity.RESULT_OK) {
                    String serialized = widget.serialize();

                    widgetStore.edit()
                            .putString(String.valueOf(identifier), serialized)
                            .apply();

                    grid.attach(host, widget);
                    placeholder.setVisibility(View.GONE);
                } else {
                    getWidgetHost().deleteAppWidgetId(host.getAppWidgetId());
                }

                activity.removeOnActivityResultListener(CONFIGURATION_CODE);
            });

            if (!result) {
                getWidgetHost().deleteAppWidgetId(host.getAppWidgetId());
                activity.removeOnActivityResultListener(CONFIGURATION_CODE);
            } else {
                getWidgetHost().startAppWidgetConfigureActivityForResult(activity, identifier,
                        Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED,
                        CONFIGURATION_CODE, null);
            }
        } catch (ActivityNotFoundException exception) {
            activity.removeOnActivityResultListener(CONFIGURATION_CODE);

            String serialized = widget.serialize();

            widgetStore.edit()
                    .putString(String.valueOf(identifier), serialized)
                    .apply();

            grid.attach(host, widget);
            placeholder.setVisibility(View.GONE);

            Log.w(TAG, "No configure activity found for identifier " + identifier);
        }
    }

    private AppWidgetHostView createWidget(AppWidgetManager manager, Widget widget) {
        AppWidgetProviderInfo info = manager.getAppWidgetInfo(widget.id);
        AppWidgetHostView host = getWidgetHost()
                .createView(activity.getApplicationContext(), widget.id, info);

        host.setOnLongClickListener(v -> {
            Vibrations.getInstance().vibrate();

            PopupMenu menu = new PopupMenu(activity);
            Resources resources = getResources();

            menu.add(new PopupMenu.Item(resources.getString(R.string.remove),
                    AppCompatResources.getDrawable(activity, R.drawable.ic_delete),
                    view -> deleteWidget(host))
            );

            menu.add(new PopupMenu.Item(resources.getString(R.string.create_a_widget),
                    AppCompatResources.getDrawable(activity, R.drawable.ic_add),
                    view -> showConfigurator())
            );

            menu.setOnDismissListener(() -> host.animate().scaleY(1)
                    .scaleX(1)
                    .alpha(1)
                    .setDuration(Animation.SHORT.getDuration()));

            menu.show(activity, host, PopupMenu.PIVOT_DEFAULT);

            return true;
        });

        return host;
    }

    private void deleteWidget(AppWidgetHostView host) {
        String identifier = String.valueOf(host.getAppWidgetId());

        if (widgetStore.contains(identifier)) {
            Widget holder =
                    Widget.deserialize(
                            widgetStore.getString(identifier, null));

            if (holder != null) {
                widgetStore.edit()
                        .remove(identifier)
                        .apply();
            }
        }

        getWidgetHost().deleteAppWidgetId(host.getAppWidgetId());
        grid.removeView((View) (host.getParent()));

        placeholder.setVisibility(grid.getChildCount() == 0 ? View.VISIBLE : View.GONE);
    }

    public @NonNull WidgetHost getWidgetHost() {
        if (host == null) {
            host = new WidgetHost(activity, HOST_ID);

            host.startListening();
        }

        return host;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (host != null) {
            host.startListening();
        }
    }

    @Override
    public void onStop() {
        if (host != null) {
            try {
                host.stopListening();
            } catch (Exception exception) {
                Log.e(TAG, "onStop: " + exception.getMessage());
            }
        }

        super.onStop();
    }

    private class AttachRunnable implements Runnable {
        private final PriorityQueue<Widget> widgets;

        public AttachRunnable(@NonNull PriorityQueue<Widget> widgets) {
            this.widgets = widgets;
        }

        @Override
        public void run() {
            AppWidgetManager manager = AppWidgetManager.getInstance(activity);
            Widget widget = widgets.poll();

            if (widget != null) {
                AppWidgetHostView host = createWidget(manager, widget);
                grid.attach(host, widget);

                placeholder.setVisibility(View.GONE);

                UiUtils.runOnUIThreadDelayed(this, Animation.SHORT.getDuration());
            }
        }
    }
}