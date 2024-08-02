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

package com.stario.launcher.ui.recyclers.async;

import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.ui.measurements.Measurements;
import com.stario.launcher.utils.animation.Animation;

import java.util.function.Supplier;

public abstract class AsyncRecyclerAdapter<AVH extends AsyncRecyclerAdapter.AsyncViewHolder>
        extends RecyclerView.Adapter<AVH> {
    private final static int VIEW_TYPE = 0;
    private final static int MIN_POOL_SIZE = 4;
    private int initialHolderHeight;
    private final Activity activity;
    private final AsyncLayoutInflater layoutInflater;
    private RecyclerView recyclerView;
    private InflationType type;

    public AsyncRecyclerAdapter(Activity activity) {
        this.activity = activity;
        this.type = InflationType.ASYNC;
        this.layoutInflater = new AsyncLayoutInflater(activity);
        this.initialHolderHeight = Measurements.dpToPx(50);
    }

    public void setInflationType(InflationType type) {
        this.type = type;
    }

    public abstract class AsyncViewHolder extends RecyclerView.ViewHolder {
        private InflationListener listener;
        private boolean inflated;

        public AsyncViewHolder() {
            super(createHolderRoot());

            if (type == InflationType.ASYNC) {
                layoutInflater.inflate(getLayout(), (ViewGroup) itemView,
                        (view, resourceId, parent) -> {
                            ViewGroup.LayoutParams params = itemView.getLayoutParams();
                            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                            itemView.setLayoutParams(params);
                            itemView.requestLayout();

                            ((ViewGroup) itemView).addView(view);

                            postInflate();
                        });
            } else if (type == InflationType.SYNCED) {
                ViewGroup.LayoutParams params = itemView.getLayoutParams();
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                itemView.setLayoutParams(params);
                itemView.requestLayout();

                LayoutInflater.from(activity).inflate(getLayout(), (ViewGroup) itemView, true);

                postInflate();
            }
        }

        private void postInflate() {
            onInflated();

            itemView.post(() -> initialHolderHeight = itemView.getMeasuredHeight());

            if (listener != null) {
                listener.onInflated();
            }

            listener = null;
            inflated = true;
        }

        void setOnInflatedInternal(@NonNull InflationListener listener) {
            if (inflated) {
                listener.onInflated();
            } else {
                this.listener = listener;
            }
        }

        protected abstract void onInflated();
    }

    private View createHolderRoot() {
        ViewGroup root = new FrameLayout(activity);

        root.setClipChildren(false);
        root.setClipToPadding(false);

        LayoutTransition transition = new LayoutTransition();

        transition.disableTransitionType(LayoutTransition.CHANGING);
        transition.disableTransitionType(LayoutTransition.DISAPPEARING);
        transition.disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
        transition.disableTransitionType(LayoutTransition.CHANGE_APPEARING);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(null, "alpha", 0, 1),
                ObjectAnimator.ofFloat(null, "scaleX", 0.8f, 1),
                ObjectAnimator.ofFloat(null, "scaleY", 0.8f, 1)
        );

        LayoutTransition layoutTransition = new LayoutTransition();
        layoutTransition.setAnimator(LayoutTransition.APPEARING, set);
        layoutTransition.setDuration(LayoutTransition.APPEARING, Animation.MEDIUM.getDuration());

        //root.setLayoutTransition(transition);

        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, initialHolderHeight));

        return root;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        this.recyclerView = recyclerView;

        recyclerView.getRecycledViewPool()
                .setMaxRecycledViews(VIEW_TYPE, 20);

        ensureMinimumViewPool();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        this.recyclerView = null;
    }

    @NonNull
    @Override
    public final AVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return getHolderSupplier().get();
    }

    @Override
    public final int getItemViewType(int position) {
        return VIEW_TYPE;
    }

    @Override
    public final void onBindViewHolder(@NonNull AVH holder, int position) {
        holder.setOnInflatedInternal(() -> onBind(holder, position));

        ensureMinimumViewPool();
    }

    private void ensureMinimumViewPool() {
        if (recyclerView != null) {
            RecyclerView.RecycledViewPool pool = recyclerView.getRecycledViewPool();

            int count = pool.getRecycledViewCount(VIEW_TYPE);

            for (; count < MIN_POOL_SIZE; count++) {
                RecyclerView.ViewHolder viewHolder = createViewHolder(recyclerView, VIEW_TYPE);
                pool.putRecycledView(viewHolder);
            }
        }
    }

    protected abstract void onBind(@NonNull AVH holder, int position);

    protected abstract int getLayout();

    protected abstract Supplier<AVH> getHolderSupplier();

    private interface InflationListener {
        void onInflated();
    }
}
