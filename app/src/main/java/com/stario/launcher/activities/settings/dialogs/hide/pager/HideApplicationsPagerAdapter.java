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

package com.stario.launcher.activities.settings.dialogs.hide.pager;

import android.content.res.Resources;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.stario.launcher.R;
import com.stario.launcher.apps.ProfileApplicationManager;
import com.stario.launcher.apps.ProfileManager;
import com.stario.launcher.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.List;

public class HideApplicationsPagerAdapter extends FragmentPagerAdapter {
    private final SparseArray<WeakReference<HideApplicationsPage>> registeredFragments;
    private final ProfileManager profileManager;
    private final Resources resources;

    public HideApplicationsPagerAdapter(FragmentManager manager, Resources resources) {
        super(manager);

        this.profileManager = ProfileManager.getInstance();
        this.registeredFragments = new SparseArray<>();
        this.resources = resources;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        List<ProfileApplicationManager> profiles = profileManager.getProfiles();
        if (profiles.size() <= 1) {
            return resources.getString(R.string.apps);
        }

        return resources.getString(
                Utils.isMainProfile(profiles.get(position).handle) ? R.string.personal : R.string.managed);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return new HideApplicationsPage(profileManager.getProfile(position));
    }

    @Override
    public int getCount() {
        return ProfileManager.getInstance().size();
    }

    @Override
    public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        super.setPrimaryItem(container, position, object);

        Fragment fragment = ((Fragment) object);
        View view = fragment.getView();

        if (view != null) {
            View nestedView = view.findViewWithTag("nested");

            if (nestedView != null) {
                nestedView.setNestedScrollingEnabled(true);
            }
        }

        FragmentManager fragmentManager = fragment.getParentFragmentManager();
        for (Fragment otherFragment : fragmentManager.getFragments()) {
            if (!fragment.equals(otherFragment)) {
                view = otherFragment.getView();

                if (view != null) {
                    View nestedView = view.findViewWithTag("nested");

                    if (nestedView != null) {
                        nestedView.setNestedScrollingEnabled(false);
                    }
                }
            }
        }

        container.requestLayout();
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        HideApplicationsPage fragment = (HideApplicationsPage) super.instantiateItem(container, position);
        registeredFragments.put(position, new WeakReference<>(fragment));

        return fragment;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        registeredFragments.remove(position);

        super.destroyItem(container, position, object);
    }

    public HideApplicationsPage getRegisteredFragment(int position) {
        WeakReference<HideApplicationsPage> page = registeredFragments.get(position);

        if (page != null) {
            return page.get();
        } else {
            return null;
        }
    }
}
