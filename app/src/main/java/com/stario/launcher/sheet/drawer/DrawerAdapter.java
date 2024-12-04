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

package com.stario.launcher.sheet.drawer;

import androidx.annotation.NonNull;
import androidx.annotation.Size;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.stario.launcher.sheet.drawer.category.Categories;
import com.stario.launcher.sheet.drawer.list.List;
import com.stario.launcher.ui.recyclers.ScrollToTop;

/**
 * @noinspection deprecation
 */
public class DrawerAdapter extends FragmentPagerAdapter {
    public static final String SHARED_ELEMENT_PREFIX = "SharedElementApp";
    public static final int LIST_POSITION = 1;
    public static final int CATEGORIES_POSITION = 2;
    public static final int PAGES = 4;
    private final FragmentManager fragmentManager;
    private final @Size(PAGES) Fragment[] fragments;

    public DrawerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);

        this.fragmentManager = fragmentManager;
        this.fragments = new Fragment[PAGES];
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        if (fragments[position] == null) {
            if (position == LIST_POSITION) {
                fragments[position] = new List();
            } else if (position == CATEGORIES_POSITION) {
                fragments[position] = new Categories();
            } else {
                fragments[position] = new Fragment();
            }
        }

        return fragments[position];
    }

    @Override
    public int getCount() {
        return PAGES;
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
