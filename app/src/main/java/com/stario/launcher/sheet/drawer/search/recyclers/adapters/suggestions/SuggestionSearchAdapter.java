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

package com.stario.launcher.sheet.drawer.search.recyclers.adapters.suggestions;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.sheet.drawer.search.recyclers.adapters.AbstractSearchListAdapter;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.icons.AdaptiveIconView;

public abstract class SuggestionSearchAdapter extends
        AbstractSearchListAdapter<SuggestionSearchAdapter.ViewHolder> {
    private final Activity activity;
    private final boolean hasLinkArrow;

    public SuggestionSearchAdapter(ThemedActivity activity, boolean hasLinkArrow) {
        this.activity = activity;
        this.hasLinkArrow = hasLinkArrow;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView label;
        final AdaptiveIconView icon;

        @SuppressLint("ClickableViewAccessibility")
        public ViewHolder(ViewGroup itemView) {
            super(itemView);

            label = itemView.findViewById(R.id.textView);
            icon = itemView.findViewById(R.id.icon);

            if (!hasLinkArrow) {
                itemView.findViewById(R.id.target_arrow).setVisibility(View.GONE);
            }

            itemView.setHapticFeedbackEnabled(false);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup container, int viewType) {
        return new ViewHolder((ViewGroup) LayoutInflater.from(activity)
                .inflate(R.layout.suggestion_item, container, false));
    }
}
