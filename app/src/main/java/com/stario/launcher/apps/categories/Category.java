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

package com.stario.launcher.apps.categories;

import androidx.annotation.Nullable;

import com.stario.launcher.apps.LauncherApplication;

import java.util.ArrayList;

public class Category {
    public final int id;
    final ArrayList<LauncherApplication> applications;
    final ArrayList<CategoryItemListener> listeners;

    public Category(int categoryID) {
        this.id = categoryID;
        this.applications = new ArrayList<>();
        this.listeners = new ArrayList<>();
    }

    public int getSize() {
        return applications.size();
    }

    @Nullable
    public LauncherApplication get(int index) {
        if (index < applications.size()) {
            return applications.get(index);
        } else {
            return LauncherApplication.FALLBACK_APP;
        }
    }

    synchronized void addApplication(LauncherApplication applicationToAdd) {
        int left = 0;
        int right = applications.size() - 1;

        while (left <= right) {
            int middle = (left + right) / 2;

            LauncherApplication application = applications.get(middle);
            int compareValue = application.getLabel()
                    .compareTo(applicationToAdd.getLabel());

            if (compareValue < 0) {
                left = middle + 1;
            } else if (compareValue > 0) {
                right = middle - 1;
            } else {
                return;
            }
        }

        for (CategoryItemListener listener : listeners) {
            listener.onPrepareInsertion(applicationToAdd);
        }

        applications.add(left, applicationToAdd);

        for (CategoryItemListener listener : listeners) {
            listener.onInserted(applicationToAdd);
        }
    }

    synchronized void removeApplication(String packageName) {
        for (int index = 0; index < applications.size(); index++) {
            LauncherApplication application = applications.get(index);

            if (application.getInfo().packageName.equals(packageName)) {
                for (CategoryItemListener listener : listeners) {
                    listener.onPrepareRemoval(application);
                }

                applications.remove(index);

                for (CategoryItemListener listener : listeners) {
                    listener.onRemoved(application);
                }

                return;
            }
        }
    }

    public int indexOf(LauncherApplication application) {
        return applications.indexOf(application);
    }

    public void addCategoryItemListener(CategoryItemListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeCategoryItemListener(CategoryItemListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(@Nullable Object object) {
        return object instanceof Category && ((Category) object).id == id;
    }

    /**
     * These callbacks do not guarantee that will be run on the UI thread
     */
    public interface CategoryItemListener {
        /**
         * This will always be called before {@link #onInserted(LauncherApplication)}
         */
        default void onPrepareInsertion(LauncherApplication application) {
        }

        /**
         * This will always be called after {@link #onPrepareInsertion(LauncherApplication)}
         */
        default void onInserted(LauncherApplication application) {
        }

        /**
         * This will always be called before {@link #onRemoved(LauncherApplication)}
         */
        default void onPrepareRemoval(LauncherApplication application) {
        }

        /**
         * This will always be called after {@link #onPrepareRemoval(LauncherApplication)}
         */
        default void onRemoved(LauncherApplication application) {
        }

        default void onUpdated(LauncherApplication application) {
        }
    }
}