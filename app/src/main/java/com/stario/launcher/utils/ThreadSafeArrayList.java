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

package com.stario.launcher.utils;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ThreadSafeArrayList<E> extends ArrayList<E> {
    private final Lock readLock;
    private final Lock writeLock;

    public ThreadSafeArrayList() {
        ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        readLock = readWriteLock.readLock();
        writeLock = readWriteLock.writeLock();
    }

    @Override
    public void add(int index, E element) {
        writeLock.lock();

        try {
            super.add(index, element);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean remove(@Nullable Object o) {
        writeLock.lock();

        try {
            return super.remove(o);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public E get(int index) {
        readLock.lock();

        try {
            return super.get(index);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean contains(@Nullable Object o) {
        readLock.lock();

        try {
            return super.contains(o);
        } finally {
            readLock.unlock();
        }
    }
}