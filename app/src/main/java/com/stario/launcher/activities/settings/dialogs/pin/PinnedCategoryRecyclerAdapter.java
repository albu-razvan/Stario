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

package com.stario.launcher.activities.settings.dialogs.pin;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.activities.launcher.pins.PinnedCategory;
import com.stario.launcher.apps.Category;
import com.stario.launcher.apps.CategoryManager;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.themes.ThemedActivity;

class PinnedCategoryRecyclerAdapter extends
        RecyclerView.Adapter<PinnedCategoryRecyclerAdapter.ViewHolder> {
    private final CategoryManager categoryManager;
    private final View.OnClickListener listener;
    private final SharedPreferences settings;
    private final LayoutInflater inflater;
    private final ThemedActivity activity;

    public PinnedCategoryRecyclerAdapter(ThemedActivity activity, View.OnClickListener listener) {
        this.settings = activity.getApplicationContext().getSharedPreferences(Entry.PINNED_CATEGORY);
        this.categoryManager = CategoryManager.getInstance();
        this.inflater = LayoutInflater.from(activity);
        this.listener = listener;
        this.activity = activity;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final RecyclerView recyclerView;
        private final TextView label;

        public ViewHolder(View itemView) {
            super(itemView);

            label = itemView.findViewById(R.id.label);
            recyclerView = itemView.findViewById(R.id.recycler);

            recyclerView.setLayoutManager(new LinearLayoutManager(activity,
                    LinearLayoutManager.HORIZONTAL, true));
            recyclerView.setItemAnimator(null);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        Category category = categoryManager.get(position);

        if (category != null) {
            viewHolder.label.setText(CategoryManager.getInstance()
                    .getCategoryName(category.identifier));
            viewHolder.recyclerView.setAdapter(
                    new PinnedCategoryApplicationRecyclerAdapter(activity, category));

            viewHolder.itemView.setOnClickListener(view -> {
                settings.edit()
                        .putString(PinnedCategory.PINNED_CATEGORY, category.identifier.toString())
                        .apply();

                if (listener != null) {
                    listener.onClick(view);
                }
            });
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        holder.recyclerView.setAdapter(null);
    }

    @Override
    public int getItemCount() {
        return categoryManager.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup container, int viewType) {
        return new ViewHolder(inflater.inflate(R.layout.category_item, container, false));
    }
}