package com.dataviz.common.event;

import com.dataviz.domain.chart.ChartConfig;
import com.dataviz.domain.filter.FilterResult;
import com.dataviz.domain.model.DataSet;
import java.time.Instant;
import java.util.Objects;

public record DatasetChangeEvent(
        EventType    type,
        DataSet      source,
        FilterResult filterResult,
        ChartConfig  chartConfig,
        Instant      occurredAt
) {
    public enum EventType {
        DATASET_LOADED,
        FILTER_APPLIED,
        FILTER_RESET,
        DATASET_REMOVED,
        CHART_APPLIED
    }

    public DatasetChangeEvent {
        Objects.requireNonNull(type,   "type must not be null");
        Objects.requireNonNull(source, "source must not be null");
        if (occurredAt == null) occurredAt = Instant.now();
        if (type == EventType.FILTER_APPLIED && filterResult == null) {
            throw new IllegalArgumentException("filterResult required for FILTER_APPLIED");
        }
    }

    public static DatasetChangeEvent loaded(DataSet ds) {
        return new DatasetChangeEvent(EventType.DATASET_LOADED, ds, null, null, Instant.now());
    }
    public static DatasetChangeEvent filterApplied(DataSet ds, FilterResult result) {
        return new DatasetChangeEvent(EventType.FILTER_APPLIED, ds, result, null, Instant.now());
    }
    public static DatasetChangeEvent filterReset(DataSet ds) {
        return new DatasetChangeEvent(EventType.FILTER_RESET, ds, null, null, Instant.now());
    }
    public static DatasetChangeEvent removed(DataSet ds) {
        return new DatasetChangeEvent(EventType.DATASET_REMOVED, ds, null, null, Instant.now());
    }

    public static DatasetChangeEvent chartApplied(DataSet ds, ChartConfig config) {
        return new DatasetChangeEvent(EventType.CHART_APPLIED, ds, null, config, Instant.now());
    }
}