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

package com.stario.launcher.sheet.briefing.dialog;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.PagerAdapter;

import com.stario.launcher.sheet.briefing.dialog.page.FeedPage;
import com.stario.launcher.sheet.briefing.dialog.page.feed.BriefingFeedList;
import com.stario.launcher.sheet.briefing.dialog.page.feed.Feed;
import com.stario.launcher.themes.ThemedActivity;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class BriefingAdapter extends FragmentPagerAdapter {
    private final Map<Integer, WeakReference<FeedPage>> registeredFragments;
    private final BriefingFeedList list;

    public BriefingAdapter(ThemedActivity activity, FragmentManager fragmentManager) {
        super(fragmentManager);

        this.registeredFragments = new HashMap<>();
        this.list = BriefingFeedList.from(activity);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return new FeedPage(position);
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return PagerAdapter.POSITION_NONE;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        FeedPage fragment = (FeedPage) super.instantiateItem(container, position);
        registeredFragments.put(position, new WeakReference<>(fragment));

        return fragment;
    }

    @Override
    public long getItemId(int position) {
        Feed feed = list.get(position);

        return feed != null ? feed.getRSSLink().hashCode() : position;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        registeredFragments.remove(position);

        super.destroyItem(container, position, object);
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return list.get(position).getTitle();
    }

    public FeedPage getRegisteredFragment(int position) {
        WeakReference<FeedPage> page = registeredFragments.get(position);

        if (page != null) {
            return page.get();
        } else {
            return null;
        }
    }

    public void reset(int... skipPositions) {
        for (Map.Entry<Integer, WeakReference<FeedPage>> entry: registeredFragments.entrySet()) {
            boolean skipped = false;

            for (int skippedPosition : skipPositions) {
                if (entry.getKey() == skippedPosition) {
                    skipped = true;

                    break;
                }
            }

            if (!skipped) {
                WeakReference<FeedPage> fragmentReference = entry.getValue();

                if (fragmentReference != null &&
                        fragmentReference.get() != null) {
                    fragmentReference.get().reset();
                }
            }
        }
    }
}
