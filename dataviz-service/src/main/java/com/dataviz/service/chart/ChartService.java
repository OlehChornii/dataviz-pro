package com.dataviz.service.chart;

import com.dataviz.domain.chart.ChartConfig;
import com.dataviz.domain.filter.FilterResult;
import com.dataviz.domain.model.DataSet;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import com.dataviz.di.annotation.*;

@Service
@Singleton
public final class ChartService {

    private static final Logger LOG = Logger.getLogger(ChartService.class.getName());

    @Inject
    public ChartService() {}

    public ChartRenderResult buildChart(DataSet dataSet, ChartConfig config) {
        Objects.requireNonNull(dataSet, "dataSet must not be null");
        Objects.requireNonNull(config, "config must not be null");

        LOG.fine(() -> "Building chart: type=%s, xCol=%s, yCols=%s"
                .formatted(config.getChartType(), config.getXColumn(), config.getYColumns()));

        validateColumns(dataSet, config);

        return new ChartRenderResult(dataSet, config);
    }

    public ChartRenderResult buildChartFromFilter(FilterResult filterResult,
                                                  ChartConfig config) {
        Objects.requireNonNull(filterResult, "filterResult must not be null");
        Objects.requireNonNull(config, "config must not be null");

        LOG.fine(() -> "Building chart from filter: %s rows".formatted(
                filterResult.getMatchedCount()));

        return new ChartRenderResult(filterResult.getSource(), config,
                filterResult.getMatchedIndices());
    }

    private void validateColumns(DataSet ds, ChartConfig cfg) {
        if (!ds.hasColumn(cfg.getXColumn())) {
            throw new IllegalArgumentException(
                    "X column not found: '%s'".formatted(cfg.getXColumn()));
        }
        List<String> missingY = cfg.getYColumns().stream()
                .filter(col -> !ds.hasColumn(col))
                .toList();
        if (!missingY.isEmpty()) {
            throw new IllegalArgumentException("Y columns not found: " + missingY);
        }
    }

    public static final class ChartRenderResult {

        private final DataSet      dataSet;
        private final ChartConfig  config;
        private final List<Integer> activeIndices;

        ChartRenderResult(DataSet dataSet, ChartConfig config) {
            this(dataSet, config, null);
        }

        ChartRenderResult(DataSet dataSet, ChartConfig config,
                          List<Integer> activeIndices) {
            this.dataSet       = dataSet;
            this.config        = config;
            this.activeIndices = activeIndices != null ? List.copyOf(activeIndices) : null;
        }

        public DataSet      getDataSet()      { return dataSet; }
        public ChartConfig  getConfig()       { return config; }
        public List<Integer> getActiveIndices() { return activeIndices; }
        public boolean hasFilter()            { return activeIndices != null; }
    }
}