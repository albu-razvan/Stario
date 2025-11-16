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

package com.stario.launcher.sheet.briefing.dialog.page;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Spanned;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.prof18.rssparser.model.RssItem;
import com.stario.launcher.R;
import com.stario.launcher.Stario;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.ui.common.text.LinkMovementMethodWithFallback;
import com.stario.launcher.ui.utils.animation.Animation;
import com.stario.launcher.utils.Utils;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class FeedPageAdapter extends RecyclerView.Adapter<FeedPageAdapter.ViewHolder> {
    private static final Safelist CONTENT_SAFELIST = new Safelist() {
        {
            addTags(
                    "p", "br", "b", "strong", "i", "em", "u", "strike",
                    "del", "a", "ul", "ol", "li", "h1", "h2", "h3", "h4"
            );
            addAttributes("a", "href");
        }
    };

    private static final long UPDATE_TIME_THRESHOLD = 900_000;

    private final Stario context;
    private List<RssItem> items;

    private volatile long lastUpdate = -1;

    public FeedPageAdapter(Stario context) {
        this.context = context;
        this.items = Collections.synchronizedList(new ArrayList<>());
    }

    public void update(@NonNull List<RssItem> items) {
        List<RssItem> filteredList = new ArrayList<>();

        for (RssItem item : items) {
            String title = item.getTitle();

            if (title != null && !title.isBlank()) {
                filteredList.add(item);
            }
        }

        if (!filteredList.isEmpty()) {
            DiffUtil.DiffResult diffResult =
                    DiffUtil.calculateDiff(new RssItemDiffUtil(this.items, filteredList));

            this.items = Collections.synchronizedList(filteredList);

            diffResult.dispatchUpdatesTo(this);

            lastUpdate = System.currentTimeMillis();
        }
    }

    public boolean shouldUpdate() {
        return (items == null || System.currentTimeMillis() - lastUpdate > UPDATE_TIME_THRESHOLD)
                && Utils.isNetworkAvailable(context);
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

            description.setMovementMethod(new LinkMovementMethodWithFallback() {
                @Override
                public void onClickFallback(View widget) {
                    itemView.performClick();
                }
            });
        }

        @Override
        public void onClick(View view) {
            int index = getBindingAdapterPosition();
            if (index == RecyclerView.NO_POSITION) {
                return;
            }

            RssItem item = items.get(index);
            Vibrations.getInstance().vibrate();

            Intent intent = null;
            if (item.getLink() != null) {
                intent = new Intent(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(item.getLink())));
            } else if (item.getGuid() != null) {
                intent = new Intent(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(item.getGuid())));
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        RssItem item = items.get(position);

        viewHolder.title.setVisibility(View.GONE);
        viewHolder.author.setVisibility(View.GONE);
        viewHolder.category.setVisibility(View.GONE);
        viewHolder.description.setVisibility(View.GONE);
        viewHolder.display.setAlpha(0f);

        String image = item.getImage();
        if (image != null) {
            viewHolder.representative.setVisibility(View.VISIBLE);

            Glide.with(context)
                    .load(image)
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

                            if (!item.getCategories().isEmpty()) {
                                String text = item.getCategories().get(0);

                                if (!text.isEmpty()) {
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

        if (item.getTitle() != null && !item.getTitle().isEmpty()) {
            Spanned title = cleanHtml(item.getTitle());

            if (!title.toString().isEmpty()) {
                viewHolder.title.setText(title);
                viewHolder.title.setVisibility(View.VISIBLE);
            }
        }

        String content = null;
        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            content = item.getDescription();
        } else if (item.getContent() != null && !item.getContent().isEmpty()) {
            content = item.getContent();
        }

        if (content != null) {
            Spanned description = cleanHtml(content);

            if (!description.toString().isEmpty()) {
                viewHolder.description.setText(description);
                viewHolder.description.setVisibility(View.VISIBLE);
            }
        }

        if (item.getAuthor() != null && !item.getAuthor().isEmpty()) {
            Spanned author = cleanHtml(item.getAuthor());

            if (!author.toString().isEmpty()) {
                viewHolder.author.setText(author);
                viewHolder.author.setVisibility(View.VISIBLE);
            }
        }
    }

    private Spanned cleanHtml(String html) {
        if (html == null) {
            return null;
        }

        return HtmlCompat.fromHtml(
                Jsoup.clean(html, CONTENT_SAFELIST),
                HtmlCompat.FROM_HTML_MODE_LEGACY
        );
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup container, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(container.getContext());

        return new ViewHolder(inflater.inflate(R.layout.article, container, false));
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }
}