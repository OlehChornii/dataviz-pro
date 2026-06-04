package com.dataviz.di;

/**
 * Мінімальний контракт ServiceLocator для View-контролерів.
 */
public interface ServiceLocator {
    <T> T get(Class<T> type);
}