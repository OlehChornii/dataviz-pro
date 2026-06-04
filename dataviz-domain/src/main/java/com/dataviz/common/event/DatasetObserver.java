package com.dataviz.common.event;

@FunctionalInterface
public interface DatasetObserver {
    void onDatasetChanged(DatasetChangeEvent event);
}