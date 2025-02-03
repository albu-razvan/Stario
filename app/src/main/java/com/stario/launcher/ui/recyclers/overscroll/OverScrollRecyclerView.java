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
import android.widget.EdgeEffect;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class OverScrollRecyclerView extends RecyclerView implements OverScroll {
    private ArrayList<OnScrollListener> onScrollListeners;
    private ArrayList<OverScrollContract> contracts;

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
        this.onScrollListeners = new ArrayList<>();

        setEdgeEffectFactory(new EdgeEffectFactory() {
            @NonNull
            @Override
            protected EdgeEffect createEdgeEffect(@NonNull RecyclerView view, int direction) {
                if (view instanceof OverScroll) {
                    return new OverScrollEffect<>(OverScrollRecyclerView.this);
                }

                return new EdgeEffect(view.getContext());
            }
        });
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