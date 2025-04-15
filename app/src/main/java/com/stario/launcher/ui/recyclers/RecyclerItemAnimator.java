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

package com.stario.launcher.ui.recyclers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.DecelerateInterpolator;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.ui.utils.animation.Animation;

import java.util.ArrayList;

public class RecyclerItemAnimator extends DefaultItemAnimator {
    public static int APPEARANCE = 0b100;
    public static int DISAPPEARANCE = 0b010;
    public static int CHANGING = 0b001;

    private final ArrayList<RecyclerView.ViewHolder> pendingRemovals;
    private final ArrayList<RecyclerView.ViewHolder> pendingAdditions;
    private final ArrayList<RecyclerView.ViewHolder> removeAnimations;
    private final ArrayList<RecyclerView.ViewHolder> addAnimations;
    private final Animation animation;
    private final int flags;

    public RecyclerItemAnimator(int flags, Animation animation) {
        this.flags = flags;

        this.animation = animation;

        this.pendingRemovals = new ArrayList<>();
        this.pendingAdditions = new ArrayList<>();
        this.removeAnimations = new ArrayList<>();
        this.addAnimations = new ArrayList<>();

        if ((flags & CHANGING) != CHANGING) {
            setChangeDuration(0);
            setMoveDuration(0);
        }
    }

    @Override
    public void runPendingAnimations() {
        super.runPendingAnimations();

        boolean removalsPending = !pendingRemovals.isEmpty();
        boolean additionsPending = !pendingAdditions.isEmpty();

        if (removalsPending || additionsPending) {
            for (RecyclerView.ViewHolder holder : pendingRemovals) {
                animateRemoveImplementation(holder);
            }

            pendingRemovals.clear();

            for (RecyclerView.ViewHolder holder : pendingAdditions) {
                animateAddImplementation(holder);
            }

            pendingAdditions.clear();
        }
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder item) {
        if (pendingRemovals.remove(item)) {
            item.itemView.setAlpha(getTargetAlpha());
            item.itemView.setScaleX(getTargetScaleX());
            item.itemView.setScaleY(getTargetScaleY());

            dispatchRemoveFinished(item);
        }

        if (pendingAdditions.remove(item)) {
            item.itemView.setAlpha(getTargetAlpha());
            item.itemView.setScaleX(getTargetScaleX());
            item.itemView.setScaleY(getTargetScaleY());

            dispatchAddFinished(item);
        }

        removeAnimations.remove(item);
        addAnimations.remove(item);

        super.endAnimation(item);
    }

    @Override
    public void endAnimations() {
        for (RecyclerView.ViewHolder holder : removeAnimations) {
            holder.itemView.setAlpha(getTargetAlpha());
            holder.itemView.setScaleX(getTargetScaleX());
            holder.itemView.setScaleY(getTargetScaleY());

            dispatchAddFinished(holder);

            removeAnimations.remove(holder);
        }

        for (RecyclerView.ViewHolder holder : addAnimations) {
            holder.itemView.setAlpha(getTargetAlpha());
            holder.itemView.setScaleX(getTargetScaleX());
            holder.itemView.setScaleY(getTargetScaleY());

            dispatchRemoveFinished(holder);

            addAnimations.remove(holder);
        }

        super.endAnimations();
    }

    @Override
    public boolean isRunning() {
        return super.isRunning() || !removeAnimations.isEmpty() || !addAnimations.isEmpty();
    }

    @Override
    public final boolean animateAdd(RecyclerView.ViewHolder holder) {
        endAnimation(holder);

        if ((flags & APPEARANCE) == APPEARANCE) {
            holder.itemView.setScaleY(getRemovedScaleY());
            holder.itemView.setScaleX(getRemovedScaleX());
            holder.itemView.setAlpha(getRemovedAlpha());

            holder.itemView.setTranslationZ(0);
        }

        pendingAdditions.add(holder);

        return true;
    }

    protected void animateAddImplementation(RecyclerView.ViewHolder holder) {
        View view = holder.itemView;

        final ViewPropertyAnimator animation = view.animate();
        addAnimations.add(holder);

        animation.alpha(getTargetAlpha())
                .scaleY(getTargetScaleY())
                .scaleX(getTargetScaleX())
                .setDuration((flags & APPEARANCE) == APPEARANCE ? this.animation.getDuration() : 0)
                .setInterpolator(new DecelerateInterpolator(3))
                .setListener(new AnimatorListenerAdapter() {
                    private void cleanup() {
                        animation.setListener(null);

                        view.setAlpha(getTargetAlpha());
                        view.setScaleX(getTargetScaleX());
                        view.setScaleY(getTargetScaleY());

                        dispatchAddFinished(holder);
                        addAnimations.remove(holder);

                        if (!isRunning()) {
                            dispatchAnimationsFinished();
                        }
                    }

                    @Override
                    public void onAnimationStart(Animator animator) {
                        dispatchAddStarting(holder);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        cleanup();
                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        cleanup();
                    }
                });
    }

    @Override
    public final boolean animateRemove(RecyclerView.ViewHolder holder) {
        endAnimation(holder);

        if ((flags & DISAPPEARANCE) == DISAPPEARANCE) {
            holder.itemView.setScaleY(getTargetScaleY());
            holder.itemView.setScaleX(getTargetScaleX());
            holder.itemView.setAlpha(getTargetAlpha());

            // keep in front without adding elevation shadow
            holder.itemView.setTranslationZ(-100_000_000);
        }

        pendingRemovals.add(holder);

        return true;
    }

    protected void animateRemoveImplementation(RecyclerView.ViewHolder holder) {
        View view = holder.itemView;

        final ViewPropertyAnimator animation = view.animate();
        removeAnimations.add(holder);

        animation.alpha(getRemovedAlpha())
                .scaleY(getRemovedScaleY())
                .scaleX(getRemovedScaleX())
                .setDuration((flags & DISAPPEARANCE) == DISAPPEARANCE ? this.animation.getDuration() : 0)
                .setInterpolator(new DecelerateInterpolator(3))
                .setListener(new AnimatorListenerAdapter() {
                    private void cleanup() {
                        animation.setListener(null);

                        view.setAlpha(getTargetAlpha());
                        view.setScaleX(getTargetScaleX());
                        view.setScaleY(getTargetScaleY());

                        view.setTranslationZ(0);

                        dispatchRemoveFinished(holder);
                        removeAnimations.remove(holder);

                        if (!isRunning()) {
                            dispatchAnimationsFinished();
                        }
                    }

                    @Override
                    public void onAnimationStart(Animator animator) {
                        dispatchRemoveStarting(holder);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        cleanup();
                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        cleanup();
                    }
                });
    }

    public float getTargetAlpha() {
        return 1f;
    }

    public float getTargetScaleX() {
        return 1f;
    }

    public float getTargetScaleY() {
        return 1f;
    }

    public float getRemovedAlpha() {
        return 0f;
    }

    public float getRemovedScaleX() {
        return 0.9f;
    }

    public float getRemovedScaleY() {
        return 0.9f;
    }

    public int getFlags() {
        return flags;
    }
}
