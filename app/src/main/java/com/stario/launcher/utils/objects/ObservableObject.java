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

package com.stario.launcher.utils.objects;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class ObservableObject<A> {
    private final ClosedObservableObject<A> observable;

    public ObservableObject(A object) {
        this.observable = new ClosedObservableObject<>(object);
    }

    public void updateObject(A object) {
        if (!((object != null && object.equals(observable.object)) ||
                object == observable.object)) {
            observable.object = object;

            for (ObservableObject.OnSet<A> listener : observable.listeners) {
                if (listener != null) {
                    listener.onSet(object);
                }
            }
        }
    }

    public A getObject() {
        return observable.getObject();
    }

    public void addListener(@NonNull ObservableObject.OnSet<A> listener) {
        observable.addListener(listener);
    }

    public void removeListeners() {
        observable.removeListeners();
    }

    public int getListenerCount() {
        return observable.getListenerCount();
    }

    public ClosedObservableObject<A> close() {
        return observable;
    }

    public static class ClosedObservableObject<B> {
        private B object;
        private final ArrayList<OnSet<B>> listeners;

        public ClosedObservableObject(B object) {
            this.object = object;

            listeners = new ArrayList<>();
        }

        public void addListener(@NonNull OnSet<B> listener) {
            listeners.add(listener);
        }

        public B getObject() {
            return object;
        }

        public void removeListeners() {
            listeners.clear();
        }

        public int getListenerCount() {
            return listeners.size();
        }
    }

    public interface OnSet<G> {
        void onSet(G object);
    }
}