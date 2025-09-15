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

package com.stario.launcher.ui.recyclers.async;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.common.LimitingTranslationFrameLayout;

import java.util.function.Supplier;

/**
 * Recycler adapter that handles data loading and inflation in different threads.
 * Applies height estimations to account for fast scrolling.
 * Use only with fixed height AsyncViewHolder root views.
 */
@SuppressWarnings("rawtypes")
public abstract class AsyncRecyclerAdapter<AVH extends AsyncRecyclerAdapter.AsyncViewHolder>
        extends RecyclerView.Adapter<AVH> {
    protected final static int DEFAULT_VIEW_TYPE = 0;

    private final static int NO_LIMIT = -1;
    private final static int MAX_POOL_SIZE = 20;

    private final AsyncLayoutInflater layoutInflater;
    private final Activity activity;
    private final InflationType type;

    private RecyclerHeightApproximationListener listener;
    private int approximatedRecyclerHeight;
    private RecyclerView recyclerView;
    private int holderHeight;
    private int limit;

    public AsyncRecyclerAdapter(Activity activity) {
        this(activity, InflationType.ASYNC);
    }

    public AsyncRecyclerAdapter(Activity activity, InflationType type) {
        this.approximatedRecyclerHeight = AsyncViewHolder.HEIGHT_UNMEASURED;
        this.limit = type == InflationType.SYNCED ? NO_LIMIT : 1;
        this.layoutInflater = new AsyncLayoutInflater(activity);
        this.holderHeight = AsyncViewHolder.HEIGHT_UNMEASURED;
        this.activity = activity;
        this.type = type;
    }

    public abstract class AsyncViewHolder extends RecyclerView.ViewHolder {
        private static final String TAG = "AsyncViewHolder";
        public static final int HEIGHT_UNMEASURED = -1;

        private InflationListener inflationListener;
        private boolean inflated;

        public AsyncViewHolder() {
            this(DEFAULT_VIEW_TYPE);
        }

        public AsyncViewHolder(int viewType) {
            super(createHolderRoot());

            if (type == InflationType.ASYNC) {
                layoutInflater.inflate(getLayout(viewType), (ViewGroup) itemView,
                        (view, resourceId, parent) -> {
                            ViewGroup.LayoutParams params = itemView.getLayoutParams();
                            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                            itemView.setLayoutParams(params);

                            ((ViewGroup) itemView).addView(view);

                            postInflate();
                        });
            } else if (type == InflationType.SYNCED) {
                ViewGroup.LayoutParams params = itemView.getLayoutParams();
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                itemView.setLayoutParams(params);
                itemView.requestLayout();

                LayoutInflater.from(activity).inflate(getLayout(viewType),
                        (ViewGroup) itemView, true);

                postInflate();
            }
        }

        private void postInflate() {
            onInflated();

            itemView.post(() -> {
                if (holderHeight == HEIGHT_UNMEASURED) {
                    holderHeight = itemView.getMeasuredHeight();

                    approximateRecyclerHeight();
                } else if (holderHeight != itemView.getMeasuredHeight()) {
                    holderHeight = itemView.getMeasuredHeight();

                    Log.w(TAG, "Holder height estimation for " + AsyncRecyclerAdapter.this.getClass() +
                            "async holders changed. New estimation: " + holderHeight);

                    approximateRecyclerHeight();
                }
            });

            if (inflationListener != null) {
                inflationListener.onInflated();
            }

            inflationListener = null;
            inflated = true;
        }

        void setOnInflatedInternal(@NonNull InflationListener listener) {
            if (inflated) {
                listener.onInflated();
            } else {
                this.inflationListener = listener;
            }
        }

        protected abstract void onInflated();
    }

    public int approximateRecyclerHeight() {
        if (holderHeight == AsyncViewHolder.HEIGHT_UNMEASURED) {
            return AsyncViewHolder.HEIGHT_UNMEASURED;
        }

        if (recyclerView == null) {
            return approximatedRecyclerHeight;
        }

        int size = getTotalItemCount();
        int newApproximation;
        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
        if (manager instanceof GridLayoutManager) {
            int spanCount = ((GridLayoutManager) manager).getSpanCount();

            newApproximation = holderHeight * (size / spanCount + (size % spanCount == 0 ? 0 : 1));
        } else {
            newApproximation = holderHeight * size;
        }

        if (approximatedRecyclerHeight == AsyncViewHolder.HEIGHT_UNMEASURED) {
            approximatedRecyclerHeight = newApproximation;

            if (listener != null) {
                listener.onNewApproximation(approximatedRecyclerHeight);
            }
        } else if (approximatedRecyclerHeight != newApproximation) {
            if (listener != null) {
                listener.onNewApproximation(approximatedRecyclerHeight);
            }
        }

        return approximatedRecyclerHeight;
    }

    private View createHolderRoot() {
        ViewGroup root = new LimitingTranslationFrameLayout(activity);

        root.setClipChildren(false);
        root.setClipToPadding(false);
        root.setLayoutTransition(null);

        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                holderHeight != AsyncViewHolder.HEIGHT_UNMEASURED ?
                        holderHeight : Measurements.dpToPx(50)));

        return root;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        this.recyclerView = recyclerView;

        recyclerView.getRecycledViewPool()
                .setMaxRecycledViews(DEFAULT_VIEW_TYPE, MAX_POOL_SIZE);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                removeLimit();
                recyclerView.removeOnScrollListener(this);
            }
        });
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        this.recyclerView = null;
    }

    protected void removeLimit() {
        if (limit != NO_LIMIT) {
            int oldLimit = limit;

            limit = NO_LIMIT;

            Runnable runnable = () -> notifyItemRangeInserted(oldLimit,
                    getTotalItemCount() - oldLimit);
            if (recyclerView != null) {
                recyclerView.post(runnable);
            } else {
                runnable.run();
            }
        }
    }

    @NonNull
    @Override
    public final AVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return getHolderSupplier(viewType).get();
    }

    @Override
    public int getItemViewType(int position) {
        return DEFAULT_VIEW_TYPE;
    }

    @Override
    public final void onBindViewHolder(@NonNull AVH holder, int position) {
        holder.setOnInflatedInternal(() -> {
            if (type == InflationType.ASYNC && limit != NO_LIMIT) {
                limit++;

                if (limit <= getTotalItemCount()) {
                    if (recyclerView != null) {
                        if (recyclerView.isComputingLayout()) {
                            recyclerView.post(() -> notifyItemInserted(limit));
                        } else {
                            notifyItemInserted(limit);
                        }
                    }
                } else {
                    limit = NO_LIMIT;
                }
            }

            onBind(holder, position);
        });
    }

    @Override
    public final int getItemCount() {
        if (type == InflationType.SYNCED) {
            return getTotalItemCount();
        }

        return limit > 0 ? Math.min(limit, getTotalItemCount()) : getTotalItemCount();
    }

    public void setRecyclerHeightApproximationListener(RecyclerHeightApproximationListener listener) {
        this.listener = listener;

        approximateRecyclerHeight();
    }

    public abstract int getTotalItemCount();

    protected abstract void onBind(@NonNull AVH holder, int position);

    protected abstract int getLayout(int viewType);

    protected abstract Supplier<AVH> getHolderSupplier(int viewType);

    private interface InflationListener {
        void onInflated();
    }

    public interface RecyclerHeightApproximationListener {
        void onNewApproximation(int height);
    }
}
