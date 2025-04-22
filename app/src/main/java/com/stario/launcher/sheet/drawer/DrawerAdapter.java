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

import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.stario.launcher.apps.LauncherApplicationManager;
import com.stario.launcher.apps.ProfileApplicationManager;
import com.stario.launcher.apps.interfaces.LauncherProfileListener;
import com.stario.launcher.sheet.drawer.category.Categories;
import com.stario.launcher.sheet.drawer.list.List;

import java.util.ArrayList;
import java.util.Objects;

public class DrawerAdapter extends FragmentPagerAdapter {
    public static final String SHARED_ELEMENT_PREFIX = "SharedElementApp";
    public static final int CATEGORIES_POSITION = 1;

    private static final int PAGES = 3; // category page + 2 empty pages for transitioning

    private final FragmentManager fragmentManager;
    private final ArrayList<Fragment> fragments;

    public DrawerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);

        this.fragmentManager = fragmentManager;
        this.fragments = new ArrayList<>();

        LauncherProfileListener listener = new LauncherProfileListener() {
            @Override
            public void onInserted(UserHandle handle) {
                notifyDataSetChanged();
            }

            @Override
            public void onRemoved(UserHandle handle) {
                for (int index = 0; index < fragments.size(); index++) {
                    Fragment fragment = fragments.get(index);

                    if (fragment instanceof List &&
                            handle.equals(((List) fragment).getUserHandle())) {
                        fragments.set(index, null);
                        break;
                    }
                }

                notifyDataSetChanged();
            }

            @Override
            public int hashCode() {
                return -1;
            }

            /** @noinspection EqualsWhichDoesntCheckParameterClass*/
            @Override
            public boolean equals(@Nullable Object object) {
                return object != null &&
                        hashCode() == object.hashCode();
            }
        };

        LauncherApplicationManager manager = LauncherApplicationManager.getInstance();

        // will have the same hash, therefore remove then add the updated one
        manager.removeLauncherProfileListener(listener);
        manager.addLauncherProfileListener(listener);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        while (position >= fragments.size()) {
            fragments.add(null);
        }

        if (fragments.get(position) == null ||
                (position > 0 && position < getCount() - 1 && // case where profile could not be loaded
                        fragments.get(position).getClass() == Fragment.class)) {

            if (position == 0 || position == getCount() - 1) {
                fragments.set(position, new Fragment());
            } else if (position == CATEGORIES_POSITION) {
                fragments.set(position, new Categories());
            } else {
                ProfileApplicationManager manager =
                        LauncherApplicationManager.getInstance()
                                .getProfile(position - 2);
                if (manager == null) {
                    fragments.set(position, new Fragment());
                } else {
                    fragments.set(position, new List(manager));
                }
            }
        }

        return Objects.requireNonNull(fragments.get(position));
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        //noinspection SuspiciousMethodCalls
        int index = fragments.indexOf(object);

        return index >= 0 ? index : POSITION_NONE;
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

    /**
     * @noinspection BooleanMethodIsAlwaysInverted
     */
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
