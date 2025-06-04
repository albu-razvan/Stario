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

package com.stario.launcher.ui.recyclers.overscroll;

import android.graphics.Canvas;
import android.view.MotionEvent;
import android.widget.EdgeEffect;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class OverScrollRecyclerView extends RecyclerView implements OverScroll {
    private ArrayList<OverScrollEffect.OnOverScrollListener> overScrollListeners;
    private ArrayList<OverScrollEffect<OverScrollRecyclerView>> edgeEffects;
    private ArrayList<OnScrollListener> onScrollListeners;
    private ArrayList<OverScrollContract> contracts;
    @OverScrollEffect.Edge
    private int pullEdges;

    public OverScrollRecyclerView(android.content.Context context) {
        super(context);

        init();
    }

    public OverScrollRecyclerView(android.content.Context context, android.util.AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public OverScrollRecyclerView(android.content.Context context, android.util.AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init();
    }

    private void init() {
        this.contracts = new ArrayList<>();
        this.overScrollListeners = new ArrayList<>();
        this.onScrollListeners = new ArrayList<>();
        this.edgeEffects = new ArrayList<>();
        this.pullEdges = OverScrollEffect.PULL_EDGE_BOTTOM | OverScrollEffect.PULL_EDGE_TOP;

        super.setEdgeEffectFactory(new EdgeEffectFactory() {
            @NonNull
            @Override
            protected EdgeEffect createEdgeEffect(@NonNull RecyclerView view, int direction) {
                if (view instanceof OverScroll) {
                    OverScrollEffect<OverScrollRecyclerView> effect =
                            new OverScrollEffect<>(OverScrollRecyclerView.this, pullEdges);

                    for (OverScrollEffect.OnOverScrollListener listener : overScrollListeners) {
                        effect.addOnOverScrollListener(listener);
                    }

                    edgeEffects.add(effect);

                    return effect;
                }

                return new EdgeEffect(view.getContext());
            }
        });
    }

    public void addOnOverScrollListener(OverScrollEffect.OnOverScrollListener listener) {
        if (listener != null) {
            overScrollListeners.add(listener);

            for (OverScrollEffect<OverScrollRecyclerView> effect : edgeEffects) {
                effect.addOnOverScrollListener(listener);
            }
        }
    }

    public void removeOnOverScrollListener(OverScrollEffect.OnOverScrollListener listener) {
        if (listener != null) {
            overScrollListeners.remove(listener);

            for (OverScrollEffect<OverScrollRecyclerView> effect : edgeEffects) {
                effect.removeOnOverScrollListener(listener);
            }
        }
    }

    public void setOverscrollPullEdges(@OverScrollEffect.Edge int edges) {
        this.pullEdges = edges;

        for (OverScrollEffect<OverScrollRecyclerView> effect : edgeEffects) {
            effect.setPullEdges(edges);
        }
    }

    public int getOverscrollPullEdges() {
        return pullEdges;
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        return false;
    }

    @Override
    public void addOnScrollListener(@NonNull OnScrollListener listener) {
        onScrollListeners.add(listener);

        super.addOnScrollListener(listener);
    }

    @Override
    public void removeOnScrollListener(@NonNull OnScrollListener listener) {
        onScrollListeners.remove(listener);

        super.removeOnScrollListener(listener);
    }

    @Override
    public void scrollToPosition(int position) {
        for (OnScrollListener listener : onScrollListeners) {
            listener.onScrolled(this, 0, 0);
        }

        super.scrollToPosition(position);
    }

    @Override
    public void addOverScrollContract(@NonNull OverScrollContract contract) {
        this.contracts.add(contract);
    }

    @Override
    public void setEdgeEffectFactory(@NonNull EdgeEffectFactory edgeEffectFactory) {
        // override to disable external custom edge effects
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        for (OverScrollEffect<OverScrollRecyclerView> effect : edgeEffects) {
            effect.onTouchEvent(event);
        }

        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        if (!contracts.isEmpty()) {
            for (OverScrollContract contract : contracts) {
                if (contract.prepare(canvas)) {
                    break;
                }
            }
        }

        super.dispatchDraw(canvas);
    }
}