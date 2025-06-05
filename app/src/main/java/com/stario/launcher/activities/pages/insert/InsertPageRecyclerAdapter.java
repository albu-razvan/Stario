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

package com.stario.launcher.activities.pages.insert;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.sheet.SheetDialogFragment;
import com.stario.launcher.sheet.SheetType;
import com.stario.launcher.themes.ThemedActivity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class InsertPageRecyclerAdapter extends RecyclerView.Adapter<InsertPageRecyclerAdapter.ViewHolder> {
    private static final String TAG = "InsertPageRecyclerAdapter";

    private final List<Pair<SheetType, Class<? extends SheetDialogFragment>>> items;
    private final InsertPageDialog.OnItemSelected listener;
    private final ThemedActivity activity;

    public InsertPageRecyclerAdapter(ThemedActivity activity,
                                     InsertPageDialog.OnItemSelected listener) {
        this.items = new ArrayList<>();
        this.activity = activity;
        this.listener = listener;
    }

    public void setItems(List<Pair<SheetType, Class<? extends SheetDialogFragment>>> items) {
        this.items.clear();
        this.items.addAll(items);

        // noinspection NotifyDataSetChanged
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView position;
        private final TextView label;

        public ViewHolder(View itemView) {
            super(itemView);

            position = itemView.findViewById(R.id.position);
            label = itemView.findViewById(R.id.label);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        Pair<SheetType, Class<? extends SheetDialogFragment>> item = items.get(position);
        Class<? extends SheetDialogFragment> clazz = item.second;

        String name = null;
        try {
            Method method = clazz.getMethod("getName");
            name = (String) method.invoke(null);
        } catch (NoSuchMethodException | InvocationTargetException exception) {
            Log.e(TAG, "inflatePage: " + clazz.getName() +
                    " does not implement getName(). Defaulting to class name...");
        } catch (IllegalAccessException exception) {
            Log.e(TAG, "inflatePage: " + clazz.getName() +
                    " getName() method is not publicly visible. Defaulting to class name...");
        } catch (ClassCastException exception) {
            Log.e(TAG, "inflatePage: " + clazz.getName() +
                    " getName() return type is not " + String.class.getName() + ". Defaulting to class name...");
        } finally {
            if (name == null) {
                name = clazz.getSimpleName();
            }

            viewHolder.label.setText(name);
        }

        Resources resources = activity.getResources();
        viewHolder.position.setText(resources.getText(R.string.default_position) + ": ");

        switch (item.first) {
            case LEFT_SHEET:
                viewHolder.position.append(resources.getText(R.string.left));
                viewHolder.position.setVisibility(View.VISIBLE);
                break;
            case TOP_SHEET:
                viewHolder.position.append(resources.getText(R.string.top));
                viewHolder.position.setVisibility(View.VISIBLE);
                break;
            case RIGHT_SHEET:
                viewHolder.position.append(resources.getText(R.string.right));
                viewHolder.position.setVisibility(View.VISIBLE);
                break;
            case BOTTOM_SHEET:
                viewHolder.position.append(resources.getText(R.string.bottom));
                viewHolder.position.setVisibility(View.VISIBLE);
                break;
            default:
                viewHolder.position.setVisibility(View.GONE);
                break;
        }

        viewHolder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSelect(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup container, int viewType) {
        return new ViewHolder(LayoutInflater.from(activity)
                .inflate(R.layout.insert_item, container, false));
    }
}