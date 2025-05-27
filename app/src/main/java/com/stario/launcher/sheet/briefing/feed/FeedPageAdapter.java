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

package com.stario.launcher.sheet.briefing.feed;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.apptasticsoftware.rssreader.Enclosure;
import com.apptasticsoftware.rssreader.Item;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.stario.launcher.R;
import com.stario.launcher.activities.Launcher;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.ui.utils.UiUtils;
import com.stario.launcher.ui.utils.animation.Animation;
import com.stario.launcher.utils.ComparableDiffUtil;

import java.util.ArrayList;
import java.util.Optional;

class FeedPageAdapter extends RecyclerView.Adapter<FeedPageAdapter.ViewHolder> {
    private static final long UPDATE_TIME_THRESHOLD = 900_000;
    private final Launcher activity;
    private ArrayList<Item> items;
    private long lastUpdate = -1;
    private RecyclerView recyclerView;

    public FeedPageAdapter(Launcher activity, RecyclerView recyclerView) {
        this.activity = activity;
        this.recyclerView = recyclerView;

        this.items = new ArrayList<>();
    }

    public void update(@NonNull Item[] items) {
        ArrayList<Item> filteredList = new ArrayList<>();

        for (Item item : items) {
            Optional<String> title = item.getTitle();

            if (title.isPresent() && !title.get().isBlank()) {
                filteredList.add(item);
            }
        }

        if (!filteredList.isEmpty()) {
            DiffUtil.DiffResult diffResult =
                    DiffUtil.calculateDiff(new ComparableDiffUtil<>(this.items, filteredList));

            this.items = filteredList;

            diffResult.dispatchUpdatesTo(this);
            getItemCount();

            lastUpdate = System.currentTimeMillis();
        }
    }

    public boolean shouldUpdate() {
        return items == null || System.currentTimeMillis() - lastUpdate > UPDATE_TIME_THRESHOLD;
    }

    public void updateAttributes(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    protected class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final ImageView display;
        private final ViewGroup representative;
        private final TextView title;
        private final TextView description;
        private final TextView author;
        private final TextView category;

        public ViewHolder(View itemView) {
            super(itemView);

            display = itemView.findViewById(R.id.display);
            representative = itemView.findViewById(R.id.representative);
            title = itemView.findViewById(R.id.title);
            description = itemView.findViewById(R.id.description);
            author = itemView.findViewById(R.id.author);
            category = itemView.findViewById(R.id.category);

            itemView.setClipToOutline(true);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            Item item = items.get(getAbsoluteAdapterPosition());

            Vibrations.getInstance().vibrate();

            if (item.getLink().isPresent()) {
                Intent intent = new Intent(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(item.getLink().get())));

                activity.startActivity(intent);
            } else if (item.getGuid().isPresent()) {
                Intent intent = new Intent(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(item.getGuid().get())));

                activity.startActivity(intent);
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        Item item = items.get(position);

        viewHolder.title.setVisibility(View.GONE);
        viewHolder.author.setVisibility(View.GONE);
        viewHolder.category.setVisibility(View.GONE);
        viewHolder.display.setAlpha(0f);

        Optional<Enclosure> optionalEnclosure = item.getEnclosure();

        if (optionalEnclosure.isPresent()) {
            viewHolder.representative.setVisibility(View.VISIBLE);
            Enclosure enclosure = optionalEnclosure.get();

            Glide.with(viewHolder.display)
                    .load(enclosure.getUrl())
                    .listener(new RequestListener<>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException exception, Object model,
                                                    @NonNull Target<Drawable> target, boolean isFirstResource) {
                            viewHolder.representative.setVisibility(View.GONE);

                            return false;
                        }

                        @Override
                        public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target,
                                                       @NonNull DataSource dataSource, boolean isFirstResource) {
                            viewHolder.representative.setVisibility(View.VISIBLE);
                            viewHolder.display.animate()
                                    .alpha(1)
                                    .setDuration(Animation.MEDIUM.getDuration());

                            if (item.getCategories().size() > 0) {
                                String text = item.getCategories().get(0);

                                if (text.length() > 0) {
                                    viewHolder.category.setText(text);
                                    viewHolder.category.setVisibility(View.VISIBLE);
                                }
                            }

                            return false;
                        }
                    })
                    .into(viewHolder.display);
        } else {
            viewHolder.representative.setVisibility(View.GONE);
        }

        if (item.getTitle().isPresent() && item.getTitle().get().length() > 0) {
            viewHolder.title.setText(item.getTitle().get());
            viewHolder.title.setVisibility(View.VISIBLE);
        }

        if (item.getDescription().isPresent() && item.getDescription().get().length() > 0) {
            viewHolder.description.setText(HtmlCompat.fromHtml(
                    HtmlCompat.fromHtml(item.getDescription().get(),
                                    HtmlCompat.FROM_HTML_MODE_LEGACY)
                            .toString(),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
            ));
            viewHolder.description.setVisibility(View.VISIBLE);
        }

        if (item.getAuthor().isPresent() && item.getAuthor().get().length() > 0) {
            viewHolder.author.setText(item.getAuthor().get());
            viewHolder.author.setVisibility(View.VISIBLE);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup container, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(container.getContext());

        return new ViewHolder(inflater.inflate(R.layout.article, container, false));
    }

    @Override
    public int getItemCount() {
        if (!items.isEmpty()) {
            if (recyclerView.getVisibility() == View.INVISIBLE) {
                UiUtils.runOnUIThread(() -> recyclerView.setVisibility(View.VISIBLE));
            }
        } else if (recyclerView.getVisibility() != View.INVISIBLE) {
            UiUtils.runOnUIThread(() -> recyclerView.setVisibility(View.INVISIBLE));
        }

        return items.size();
    }
}