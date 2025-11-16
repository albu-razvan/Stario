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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.prof18.rssparser.model.RssItem;

import java.util.List;
import java.util.Objects;

public class RssItemDiffUtil extends DiffUtil.Callback {
    private final List<RssItem> oldList;
    private final List<RssItem> newList;

    public RssItemDiffUtil(@NonNull List<RssItem> oldList, @NonNull List<RssItem> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return areContentsTheSame(oldItemPosition, newItemPosition);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        RssItem first = oldList.get(oldItemPosition);
        RssItem second = oldList.get(newItemPosition);

        return Objects.equals(first.getTitle(), second.getTitle())
                && Objects.equals(first.getDescription(), second.getDescription())
                && Objects.equals(first.getAuthor(), second.getAuthor())
                && Objects.equals(first.getCategories(), second.getCategories());
    }
}
