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

package com.stario.launcher.ui.recyclers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.DecelerateInterpolator;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.utils.animation.Animation;

import java.util.ArrayList;

public class RecyclerItemAnimator extends DefaultItemAnimator {
    public static int APPEARANCE = 0x10;
    public static int DISAPPEARANCE = 0x01;
    private final ArrayList<RecyclerView.ViewHolder> pendingRemovals;
    private final ArrayList<RecyclerView.ViewHolder> pendingAdditions;
    private final ArrayList<ViewPropertyAnimator> removeAnimations;
    private final ArrayList<ViewPropertyAnimator> addAnimations;
    private final Animation animation;
    private final int flags;

    public RecyclerItemAnimator(int flags, Animation animation) {
        this.flags = flags;

        this.animation = animation;

        this.pendingRemovals = new ArrayList<>();
        this.pendingAdditions = new ArrayList<>();
        this.removeAnimations = new ArrayList<>();
        this.addAnimations = new ArrayList<>();
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
    public void endAnimations() {
        for (ViewPropertyAnimator animation: removeAnimations) {
            animation.cancel();
        }

        for (ViewPropertyAnimator animation: addAnimations) {
            animation.cancel();
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
        }

        pendingAdditions.add(holder);

        return true;
    }

    protected void animateAddImplementation(RecyclerView.ViewHolder holder) {
        View view = holder.itemView;

        final ViewPropertyAnimator animation = view.animate();
        addAnimations.add(animation);

        animation.alpha(getTargetAlpha())
                .scaleY(getTargetScaleY())
                .scaleX(getTargetScaleX())
                .setDuration((flags & APPEARANCE) == APPEARANCE ? this.animation.getDuration() : 0)
                .setInterpolator(new DecelerateInterpolator(3))
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animator) {
                        dispatchAddStarting(holder);
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {
                        view.setAlpha(getTargetAlpha());
                        view.setScaleY(getTargetScaleY());
                        view.setScaleX(getTargetScaleX());
                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        animation.setListener(null);
                        dispatchAddFinished(holder);
                        addAnimations.remove(holder);

                        if (!isRunning()) {
                            dispatchAnimationsFinished();
                        }
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
        }

        pendingRemovals.add(holder);

        return true;
    }

    protected void animateRemoveImplementation(RecyclerView.ViewHolder holder) {
        View view = holder.itemView;

        final ViewPropertyAnimator animation = view.animate();
        removeAnimations.add(animation);

        animation.alpha(getRemovedAlpha())
                .scaleY(getRemovedScaleY())
                .scaleX(getRemovedScaleX())
                .setDuration((flags & DISAPPEARANCE) == DISAPPEARANCE ? this.animation.getDuration() : 0)
                .setInterpolator(new DecelerateInterpolator(3))
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animator) {
                        dispatchRemoveStarting(holder);
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {
                        view.setAlpha(getRemovedAlpha());
                        view.setScaleX(getRemovedScaleX());
                        view.setScaleY(getRemovedScaleY());
                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        animation.setListener(null);
                        dispatchRemoveFinished(holder);
                        removeAnimations.remove(holder);

                        if (!isRunning()) {
                            dispatchAnimationsFinished();
                        }
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
