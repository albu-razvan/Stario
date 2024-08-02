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

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.DecelerateInterpolator;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.utils.animation.Animation;

public class RecyclerItemAnimator extends DefaultItemAnimator {
    public static int APPEARANCE = 0x10;
    public static int DISAPPEARANCE = 0x01;
    private final int flags;

    public RecyclerItemAnimator(int flags) {
        this.flags = flags;
    }

    @Override
    public boolean animateAdd(RecyclerView.ViewHolder holder) {
        if ((flags & APPEARANCE) == APPEARANCE) {
            holder.itemView.setScaleY(0.8f);
            holder.itemView.setScaleX(0.8f);
            holder.itemView.setAlpha(0f);
            holder.itemView.animate()
                    .alpha(1f).scaleY(1f).scaleX(1f)
                    .setDuration(Animation.SHORT.getDuration())
                    .setInterpolator(new DecelerateInterpolator(1));

            return true;
        }

        return super.animateAdd(holder);
    }

    @Override
    public boolean animateRemove(RecyclerView.ViewHolder holder) {
        ViewParent parent = holder.itemView.getParent();

        if (parent instanceof ViewGroup) {
            if ((flags & DISAPPEARANCE) == DISAPPEARANCE) {
                holder.itemView.animate()
                        .alpha(0f).scaleY(0.8f).scaleX(0.8f)
                        .setDuration(Animation.SHORT.getDuration())
                        .setInterpolator(new DecelerateInterpolator(1));

                return true;
            } else {
                holder.itemView.setVisibility(View.GONE);

                return true;
            }
        }

        return super.animateRemove(holder);
    }
}
