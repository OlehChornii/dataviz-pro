package com.dataviz.di;

/**
 * Мінімальний контракт ServiceLocator для View-контролерів.
 * Реалізується AppContext у dataviz-app.
 * View знає лише про цей інтерфейс — не про AppContext напряму.
 */
public interface ServiceLocator {
    <T> T get(Class<T> type);
}