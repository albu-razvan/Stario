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

package com.stario.launcher.sheet.drawer.list;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.sheet.drawer.BumpRecyclerViewAdapter;
import com.stario.launcher.sheet.drawer.RecyclerApplicationAdapter;
import com.stario.launcher.sheet.drawer.apps.LauncherApplication;
import com.stario.launcher.sheet.drawer.apps.LauncherApplicationManager;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.icons.AdaptiveIconView;
import com.stario.launcher.ui.recyclers.FastScroller;
import com.stario.launcher.ui.recyclers.async.AsyncRecyclerAdapter;
import com.stario.launcher.utils.animation.Animation;

public class ListAdapter extends RecyclerApplicationAdapter
        implements FastScroller.OnPopupViewUpdate,
        FastScroller.OnPopupViewReset, BumpRecyclerViewAdapter {
    private final RecyclerView recyclerView;
    private final LauncherApplicationManager applicationManager;
    private boolean limit;
    private int size;
    private int oldScrollerPosition;

    public ListAdapter(ThemedActivity activity, RecyclerView recyclerView) {
        super(activity);

        this.recyclerView = recyclerView;
        this.size = 0;
        this.limit = true;
        this.oldScrollerPosition = -1;
        this.applicationManager = LauncherApplicationManager.getInstance();

        applicationManager.addApplicationListener(new LauncherApplicationManager.ApplicationListener() {
            @Override
            public void onHidden(LauncherApplication application) {
                notifyItemRangeRemoved(0, getItemCount());
            }

            @Override
            public void onInserted(LauncherApplication application) {
                notifyItemInserted(applicationManager.indexOf(application));
            }

            @Override
            public void onRemoved(LauncherApplication application) {
                notifyItemRangeRemoved(0, getItemCount());
            }

            @Override
            public void onShowed(LauncherApplication application) {
                notifyItemInserted(applicationManager.indexOf(application));
            }

            @Override
            public void onUpdated(LauncherApplication application) {
                notifyItemChanged(applicationManager.indexOf(application));
            }
        });
    }

    @Override
    public void onUpdate(int index, @NonNull TextView textView) {
        int size = applicationManager.getSize() - 1;

        if (index > size) {
            index = size;
        }

        if (oldScrollerPosition != index) {
            Vibrations.getInstance().vibrate();
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();

            if (layoutManager != null) {
                View lastView = layoutManager.findViewByPosition(oldScrollerPosition);
                View currentView = layoutManager.findViewByPosition(index);

                if (currentView != null) {
                    currentView.animate().scaleX(AdaptiveIconView.MAX_SCALE)
                            .scaleY(AdaptiveIconView.MAX_SCALE)
                            .setDuration(Animation.MEDIUM.getDuration());
                }

                if (lastView != null) {
                    lastView.animate().scaleX(1).scaleY(1)
                            .setDuration(Animation.MEDIUM.getDuration());
                }
            }
        }

        oldScrollerPosition = index;
        LauncherApplication application = applicationManager.get(index, true);

        if (application != LauncherApplication.FALLBACK_APP) {
            String label = application.getLabel();

            if (label.length() > 0) {
                textView.setText(String.valueOf(label.charAt(0)).toUpperCase());
            }
        }
    }

    @Override
    public void onReset(int index) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();

        if (layoutManager != null) {
            View currentView = layoutManager.findViewByPosition(oldScrollerPosition);
            oldScrollerPosition = -1;

            if (currentView != null) {
                currentView.animate().scaleX(1)
                        .scaleY(1)
                        .setDuration(Animation.MEDIUM.getDuration());
            }
        }
    }

    @Override
    protected LauncherApplication getApplication(int index) {
        return applicationManager.get(index, true);
    }

    @Override
    public long getItemId(int position) {
        LauncherApplication application = applicationManager.get(position, true);

        if (application != null) {
            return application.getInfo()
                    .packageName.hashCode();
        } else {
            return -1;
        }
    }

    @Override
    protected int getSize() {
        return limit ? size : applicationManager.getSize();
    }

    @Override
    public void bump() {
        if (limit) {
            int approximatedHolderHeight = getApproximatedHolderHeight();
            int newSize = size +
                    (approximatedHolderHeight != AsyncRecyclerAdapter.AsyncViewHolder.HEIGHT_UNMEASURED ?
                            Math.round(
                                    Math.max(1,
                                            recyclerView.getMeasuredHeight() / (float) approximatedHolderHeight)
                            ) : 1
                    );

            int inserted = newSize - size;
            size = newSize;

            if (inserted > 0) {
                notifyItemRangeInserted(getItemCount() - inserted, inserted);
            }
        }
    }

    @Override
    public void removeLimit() {
        limit = false;

        int inserted = applicationManager.getSize() - size;
        notifyItemRangeInserted(getItemCount() - inserted, inserted);
    }
}