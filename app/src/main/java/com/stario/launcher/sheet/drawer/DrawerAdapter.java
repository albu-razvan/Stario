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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.stario.launcher.apps.ProfileApplicationManager;
import com.stario.launcher.apps.ProfileManager;
import com.stario.launcher.apps.interfaces.LauncherProfileListener;
import com.stario.launcher.sheet.drawer.category.Categories;
import com.stario.launcher.sheet.drawer.list.List;

import java.util.ArrayList;

/**
 * @noinspection deprecation
 */
public class DrawerAdapter extends FragmentPagerAdapter {
    public static final int CATEGORIES_POSITION = 1;

    // category page + 2 empty pages for transitioning
    private static final int PAGES = 3;

    private final FragmentManager fragmentManager;
    private final ArrayList<Fragment> fragments;

    private FragmentTransaction transaction;

    public DrawerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);

        this.fragmentManager = fragmentManager;
        this.fragments = new ArrayList<>();

        LauncherProfileListener listener = new LauncherProfileListener() {
            @Override
            public void onInserted(UserHandle handle) {
                int oldCount = getCount() - 1;

                if (fragments.size() > oldCount - 1) {
                    removeFragment(oldCount - 1);

                    if (fragments.size() > oldCount) {
                        fragments.set(oldCount, fragments.get(oldCount - 1));
                    } else {
                        fragments.add(fragments.get(oldCount - 1));
                    }

                    fragments.set(oldCount - 1, null);
                }

                notifyDataSetChanged();
            }

            @Override
            public void onRemoved(UserHandle handle) {
                for (int index = 0; index < fragments.size(); index++) {
                    Fragment fragment = fragments.get(index);

                    if (fragment instanceof List &&
                            handle.equals(((List) fragment).getUserHandle())) {
                        removeFragment(index);

                        for (int move = index + 1; move < fragments.size(); move++) {
                            Fragment fragmentToRemove = fragments.get(move);

                            if (fragmentToRemove != null) {
                                removeFragment(move);
                            }

                            fragments.set(move - 1, fragments.get(move));
                        }

                        int count = getCount();
                        if (fragments.size() > count) {
                            Fragment fragmentToRemove = fragments.get(count);

                            if (fragmentToRemove != null) {
                                removeFragment(count);
                                fragments.set(getCount(), null);
                            }
                        }

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

        ProfileManager manager = ProfileManager.getInstance();

        // Will have the same hash, therefore remove then add the updated one
        manager.removeLauncherProfileListener(listener);
        manager.addLauncherProfileListener(listener);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        while (position >= fragments.size()) {
            fragments.add(null);
        }

        if (fragments.get(position) == null) {
            if (position == 0 || position == getCount() - 1) {
                fragments.set(position, new Fragment());
            } else if (position == CATEGORIES_POSITION) {
                fragments.set(position, new Categories());
            } else {
                ProfileApplicationManager manager =
                        ProfileManager.getInstance().getProfile(position - 2);

                if (manager == null) {
                    fragments.set(position, new Fragment());
                } else {
                    fragments.set(position, new List(manager));
                }
            }
        }

        return fragments.get(position);
    }

    private void removeFragment(int position) {
        Fragment fragment = fragments.get(position);

        if (fragment != null) {
            Object host = fragment.getHost();

            if (host instanceof FragmentActivity) {
                Lifecycle lifecycle = ((FragmentActivity) host).getLifecycle();

                if (lifecycle.getCurrentState() == Lifecycle.State.RESUMED) {
                    //noinspection DataFlowIssue
                    destroyItem(null, position, fragment);
                    fragmentManager.beginTransaction()
                            .remove(fragment)
                            .commitNow();
                } else {
                    if (transaction == null) {
                        transaction = fragmentManager.beginTransaction();
                    }

                    transaction.remove(fragment);

                    lifecycle.addObserver(new DefaultLifecycleObserver() {
                        @Override
                        public void onDestroy(@NonNull LifecycleOwner owner) {
                            transaction = null;

                            lifecycle.removeObserver(this);
                        }

                        @Override
                        public void onResume(@NonNull LifecycleOwner owner) {
                            if (transaction != null) {
                                transaction.commitNow();
                                transaction = null;
                            }

                            lifecycle.removeObserver(this);
                        }
                    });
                }
            }
        }
    }

    public Fragment getFragment(int position) {
        if (position < 0 || position >= fragments.size()) {
            return null;
        }

        return fragments.get(position);
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        //noinspection SuspiciousMethodCalls
        int index = fragments.indexOf(object);

        return (index >= 0 && index < getCount()) ? index : POSITION_NONE;
    }

    @Override
    @IntRange(from = PAGES)
    public int getCount() {
        return PAGES + ProfileManager.getInstance().size();
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
        if (!fragmentManager.isDestroyed()) {
            return fragmentManager.popBackStackImmediate(Categories.FOLDER_STACK_ID,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
        } else {
            return false;
        }
    }
}
