package com.dataviz.service.filter;

import com.dataviz.common.config.AppConfig;
import com.dataviz.common.event.EventBus;
import com.dataviz.domain.filter.*;
import com.dataviz.domain.model.DataSet;
import com.dataviz.di.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

@Service
@Singleton
public final class FilterService {

    private static final Logger LOG = Logger.getLogger(FilterService.class.getName());

    private final EventBus eventBus;

    @Inject(required = false)
    private AppConfig config;

    @Inject
    public FilterService(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @PostConstruct
    private void init() {
        int threshold = config != null ? config.parallelThreshold() : 100_000;
        LOG.info("FilterService initialized (parallel threshold: %,d)".formatted(threshold));
    }

    public FilterResult filter(DataSet dataSet, List<FilterCriteria> criteria) {
        Objects.requireNonNull(criteria, "criteria");

        long startMs = System.currentTimeMillis();
        List<Integer> indices = FilterEngine.apply(dataSet, criteria);
        long elapsed = System.currentTimeMillis() - startMs;
        LOG.fine(() -> "Filtered %d/%d rows in %d ms".formatted(
                indices.size(), dataSet.getRowCount(), elapsed));
        return new FilterResult(dataSet, indices);
    }
}