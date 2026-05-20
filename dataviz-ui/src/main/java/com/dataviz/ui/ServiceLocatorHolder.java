package com.dataviz.ui;

import com.dataviz.di.ServiceLocator;

public final class ServiceLocatorHolder {

    private static ServiceLocator instance;

    private ServiceLocatorHolder() {}

    public static void set(ServiceLocator locator) {
        if (instance == null) {
            instance = locator;
        }
    }

    public static ServiceLocator get() {
        if (instance == null) throw new IllegalStateException(
                "ServiceLocatorHolder not initialized. Call set() before loading FXML.");
        return instance;
    }
}