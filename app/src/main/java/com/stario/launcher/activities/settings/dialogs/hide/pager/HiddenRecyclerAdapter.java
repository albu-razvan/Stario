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

package com.stario.launcher.activities.settings.dialogs.hide.pager;

import android.annotation.SuppressLint;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.apps.ProfileApplicationManager;
import com.stario.launcher.apps.interfaces.LauncherApplicationListener;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.icons.AdaptiveIconView;
import com.stario.launcher.ui.recyclers.async.AsyncRecyclerAdapter;
import com.stario.launcher.ui.recyclers.async.InflationType;

import java.util.function.Supplier;

public class HiddenRecyclerAdapter
        extends AsyncRecyclerAdapter<HiddenRecyclerAdapter.ViewHolder> {
    private static final String TAG = "HiddenRecyclerAdapter";
    private static final float HIDDEN_ALPHA = 0.5f;

    private final ProfileApplicationManager applicationManager;
    private final LauncherApplicationListener listener;

    private RecyclerView recyclerView;

    public HiddenRecyclerAdapter(ThemedActivity activity,
                                 ProfileApplicationManager applicationManager) {
        super(activity, InflationType.ASYNC);

        this.applicationManager = applicationManager;

        listener = new LauncherApplicationListener() {
            @Override
            public void onInserted(LauncherApplication application) {
                recyclerView.post(() -> {
                    notifyItemInsertedInternal(applicationManager.indexOf(application));
                    approximateRecyclerHeight();
                });
            }

            @Override
            public void onRemoved(LauncherApplication application) {
                recyclerView.post(() -> {
                    notifyItemRemovedInternal();
                    approximateRecyclerHeight();
                });
            }

            @Override
            public void onUpdated(LauncherApplication application) {
                recyclerView.post(() -> notifyItemChanged(applicationManager.indexOf(application)));
            }
        };

        setHasStableIds(true);
    }

    public class ViewHolder extends AsyncViewHolder {
        private AdaptiveIconView icon;
        private TextView label;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        protected void onInflated() {
            label = itemView.findViewById(R.id.textView);
            icon = itemView.findViewById(R.id.icon);

            ViewGroup.LayoutParams params = icon.getLayoutParams();
            params.width = Measurements.getIconSize();
            params.height = Measurements.getIconSize();

            label.setLines(2);
        }
    }

    @Override
    public void onBind(@NonNull ViewHolder viewHolder, int index) {
        LauncherApplication application = getApplication(index);

        if (application != LauncherApplication.FALLBACK_APP) {
            boolean hidden = !applicationManager.isVisibleToUser(application);

            viewHolder.icon.setApplication(application);
            viewHolder.icon.setGrayscale(hidden);
            viewHolder.icon.setAlpha(hidden ? HIDDEN_ALPHA : 1f);

            viewHolder.itemView.setOnClickListener(view -> {
                if (applicationManager.isVisibleToUser(application)) {
                    applicationManager.hideApplication(application);
                } else {
                    applicationManager.showApplication(application);
                }

                notifyItemChanged(index);
            });

            viewHolder.label.setText(application.getLabel());
            viewHolder.itemView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected int getLayout() {
        return R.layout.hidden_item;
    }

    @Override
    protected Supplier<ViewHolder> getHolderSupplier() {
        return ViewHolder::new;
    }

    private void notifyItemRemovedInternal() {
        if (recyclerView != null) {
            RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();

            if (manager != null) {
                Parcelable state = manager.onSaveInstanceState();
                notifyItemRangeRemoved(0, getItemCount());
                manager.onRestoreInstanceState(state);
            }
        }
    }

    private void notifyItemInsertedInternal(int position) {
        if (recyclerView != null) {
            RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();

            if (manager != null) {
                Parcelable state = manager.onSaveInstanceState();
                notifyItemInserted(position);
                manager.onRestoreInstanceState(state);
            }
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        this.recyclerView = recyclerView;

        if (listener != null) {
            applicationManager.addApplicationListener(listener);
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);

        if (listener != null) {
            applicationManager.removeApplicationListener(listener);
        }

        this.recyclerView = null;
    }

    protected LauncherApplication getApplication(int index) {
        return applicationManager != null ?
                applicationManager.get(index, true) : LauncherApplication.FALLBACK_APP;
    }

    @Override
    public long getItemId(int position) {
        LauncherApplication application = applicationManager.get(position, true);

        if (application != null) {
            return application.getInfo().packageName.hashCode();
        } else {
            return -1;
        }
    }

    @Override
    public int getTotalItemCount() {
        return applicationManager.getActualSize();
    }
}