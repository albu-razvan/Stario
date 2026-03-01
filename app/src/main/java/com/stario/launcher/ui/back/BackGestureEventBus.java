/*
 * Copyright (C) 2026 Răzvan Albu
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

package com.stario.launcher.ui.back;

import java.util.concurrent.CopyOnWriteArrayList;

public class BackGestureEventBus {
    private static final BackGestureEventBus INSTANCE = new BackGestureEventBus();

    private final CopyOnWriteArrayList<BackEventListener> listeners = new CopyOnWriteArrayList<>();

    public static BackGestureEventBus getInstance() {
        return INSTANCE;
    }

    public void addListener(BackEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(BackEventListener listener) {
        listeners.remove(listener);
    }

    public void postEvent(BackEvent event) {
        for (BackEventListener listener : listeners) {
            if (listener.origin == null || event.origin == listener.origin) {
                listener.onBackEvent(event);
            }
        }
    }

    public static abstract class BackEventListener {
        private final Class<?> origin;

        public BackEventListener() {
            this.origin = null;
        }

        public BackEventListener(Class<?> origin) {
            this.origin = origin;
        }

        public abstract void onBackEvent(BackEvent event);
    }
}