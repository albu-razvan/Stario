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

package com.stario.launcher.sheet.drawer.list;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Parcelable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.apps.ProfileApplicationManager;
import com.stario.launcher.apps.interfaces.LauncherApplicationListener;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.sheet.drawer.RecyclerApplicationAdapter;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.icons.AdaptiveIconView;
import com.stario.launcher.ui.recyclers.FastScroller;
import com.stario.launcher.ui.utils.animation.Animation;

public class ListAdapter extends RecyclerApplicationAdapter
        implements FastScroller.OnPopupViewUpdate,
        FastScroller.OnPopupViewReset {
    private final LauncherApplicationListener listener;
    private final ProfileApplicationManager applicationManager;
    private RecyclerView recyclerView;
    private int oldScrollerPosition;

    public ListAdapter(ThemedActivity activity, ProfileApplicationManager applicationManager) {
        super(activity);

        this.applicationManager = applicationManager;
        this.oldScrollerPosition = -1;

        listener = new LauncherApplicationListener() {
            @Override
            public void onHidden(LauncherApplication application) {
                recyclerView.post(() -> notifyItemRemovedInternal());
            }

            @Override
            public void onInserted(LauncherApplication application) {
                recyclerView.post(() -> notifyItemInsertedInternal(applicationManager.indexOf(application)));
            }

            @Override
            public void onRemoved(LauncherApplication application) {
                recyclerView.post(() -> notifyItemRemovedInternal());
            }

            @Override
            public void onShowed(LauncherApplication application) {
                recyclerView.post(() -> notifyItemInsertedInternal(applicationManager.indexOf(application)));
            }

            @Override
            public void onUpdated(LauncherApplication application) {
                recyclerView.post(() -> notifyItemChanged(applicationManager.indexOf(application)));
            }
        };
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
    public void onUpdate(int index, @NonNull TextView textView) {
        removeLimit();

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

            if (!label.isEmpty()) {
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
                        .setDuration(Animation.MEDIUM.getDuration())
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationCancel(Animator animation) {
                                currentView.setScaleX(1);
                                currentView.setScaleY(1);
                            }
                        });
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

    @Override
    protected LauncherApplication getApplication(int index) {
        return applicationManager != null ?
                applicationManager.get(index, true) : LauncherApplication.FALLBACK_APP;
    }

    @Override
    protected boolean allowApplicationStateEditing() {
        return true;
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
    protected int getSize() {
        return applicationManager.getSize();
    }
}