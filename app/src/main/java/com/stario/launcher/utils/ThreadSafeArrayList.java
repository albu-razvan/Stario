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