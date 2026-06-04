package com.dataviz.common.event;

import com.dataviz.di.annotation.Component;
import com.dataviz.di.annotation.Singleton;
import com.dataviz.domain.filter.FilterResult;
import com.dataviz.domain.model.DataSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

@Component
@Singleton
public final class DatasetObservable {

    private static final Logger LOG = Logger.getLogger(DatasetObservable.class.getName());

    private final List<DatasetObserver> observers = new CopyOnWriteArrayList<>();

    public void addObserver(DatasetObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
            LOG.fine(() -> "Observer registered: " + observer.getClass().getSimpleName());
        }
    }

    public void removeObserver(DatasetObserver observer) {
        observers.remove(observer);
        LOG.fine(() -> "Observer removed: " + (observer != null
                ? observer.getClass().getSimpleName() : "null"));
    }

    public int observerCount() { return observers.size(); }

    public void notifyDatasetLoaded(DataSet ds) {
        publish(DatasetChangeEvent.loaded(ds));
    }

    public void notifyFilterApplied(DataSet ds, FilterResult result) {
        publish(DatasetChangeEvent.filterApplied(ds, result));
    }

    public void notifyFilterReset(DataSet ds) {
        publish(DatasetChangeEvent.filterReset(ds));
    }

    public void notifyDatasetRemoved(DataSet ds) {
        publish(DatasetChangeEvent.removed(ds));
    }

    public void notifyChartApplied(DataSet ds, com.dataviz.domain.chart.ChartConfig config) {
        publish(DatasetChangeEvent.chartApplied(ds, config));
    }

    private void publish(DatasetChangeEvent event) {
        LOG.fine(() -> "Publishing event: %s to %d observers"
                .formatted(event.type(), observers.size()));
        for (DatasetObserver observer : observers) {
            try {
                observer.onDatasetChanged(event);
            } catch (Exception e) {
                LOG.warning("Observer threw exception: " + e.getMessage());
            }
        }
    }
}