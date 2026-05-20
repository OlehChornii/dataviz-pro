package com.dataviz.ui.presenter;

import com.dataviz.common.event.DatasetChangeEvent;
import com.dataviz.common.event.DatasetObservable;
import com.dataviz.common.event.DatasetObserver;
import com.dataviz.di.annotation.*;
import com.dataviz.domain.filter.FilterCriteria;
import com.dataviz.domain.filter.FilterResult;
import com.dataviz.domain.model.DataSet;
import com.dataviz.service.filter.FilterService;

import java.util.List;
import java.util.logging.Logger;

@Component
public final class FilterPresenter implements DatasetObserver {

    private static final Logger LOG = Logger.getLogger(FilterPresenter.class.getName());

    private final FilterService     filterService;
    private final DatasetObservable observable;

    private DataSet currentDataSet;

    @Inject
    public FilterPresenter(FilterService filterService, DatasetObservable observable) {
        this.filterService = filterService;
        this.observable    = observable;
    }

    @PostConstruct
    private void subscribe() { observable.addObserver(this); }

    @PreDestroy
    private void unsubscribe() { observable.removeObserver(this); }

    @Override
    public void onDatasetChanged(DatasetChangeEvent event) {
        if (event.type() == DatasetChangeEvent.EventType.DATASET_LOADED) {
            this.currentDataSet = event.source();
            LOG.info("FilterPresenter: dataset updated → " + currentDataSet.getName());
        }
    }

    public void onApplyFilterClicked(List<FilterCriteria> criteria) {
        if (currentDataSet == null) {
            LOG.warning("FilterPresenter: no dataset, ignoring");
            return;
        }

        if (criteria == null || criteria.isEmpty()) {
            LOG.info("FilterPresenter: empty criteria → reset");
            observable.notifyFilterReset(currentDataSet);
            return;
        }

        LOG.info(() -> "FilterPresenter: applying %d criteria".formatted(criteria.size()));
        try {
            FilterResult result = filterService.filter(currentDataSet, criteria);
            LOG.info(() -> "FilterPresenter: %,d/%,d rows matched"
                    .formatted(result.getMatchedCount(), result.getTotalCount()));
            observable.notifyFilterApplied(currentDataSet, result);
        } catch (Exception e) {
            LOG.severe("FilterPresenter: error: " + e.getMessage());
            observable.notifyFilterReset(currentDataSet);
        }
    }

    public void onResetFilterClicked() {
        if (currentDataSet != null) {
            LOG.info("FilterPresenter: reset");
            observable.notifyFilterReset(currentDataSet);
        }
    }

    public DataSet getCurrentDataSet() { return currentDataSet; }
}