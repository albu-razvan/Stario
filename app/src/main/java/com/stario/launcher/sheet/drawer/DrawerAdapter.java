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

package com.stario.launcher.sheet.drawer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.stario.launcher.apps.LauncherApplicationManager;
import com.stario.launcher.sheet.drawer.category.Categories;
import com.stario.launcher.sheet.drawer.list.List;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @noinspection deprecation
 */
public class DrawerAdapter extends FragmentPagerAdapter {
    public static final String SHARED_ELEMENT_PREFIX = "SharedElementApp";
    public static final int CATEGORIES_POSITION = 1;

    private static final int PAGES = 3; // category page + 2 empty pages for transitioning

    private final FragmentManager fragmentManager;
    private final Map<Integer, Fragment> fragments;

    public DrawerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);

        this.fragmentManager = fragmentManager;
        this.fragments = new HashMap<>();
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        if (fragments.getOrDefault(position, null) == null) {
            if (position == 0 || position == getCount() - 1) {
                fragments.put(position, new Fragment());
            } else if (position == CATEGORIES_POSITION) {
                fragments.put(position, new Categories());
            } else {
                fragments.put(position, new List(LauncherApplicationManager.getInstance()
                        .getUserHandle(position - 2)));
            }
        }

        return Objects.requireNonNull(fragments.get(position));
    }

    @Override
    public int getCount() {
        return PAGES + LauncherApplicationManager.getInstance().size();
    }

    public void reset() {
        collapse();

        if (!fragmentManager.isDestroyed()) {
            for (Fragment fragment : fragmentManager.getFragments()) {
                if (fragment instanceof ScrollToTop) {
                    ((ScrollToTop) fragment).scrollToTop();
                }
            }
        }
    }

    public boolean collapse() {
        if (!fragmentManager.isDestroyed() && !isTransitioning()) {
            return fragmentManager.popBackStackImmediate(Categories.STACK_ID,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
        } else {
            return false;
        }
    }

    public boolean isTransitioning() {
        if (!fragmentManager.isDestroyed()) {
            for (Fragment fragment : fragmentManager.getFragments()) {
                if (fragment instanceof DrawerPage &&
                        ((DrawerPage) fragment).isTransitioning()) {
                    return true;
                }
            }
        }

        return false;
    }
}
