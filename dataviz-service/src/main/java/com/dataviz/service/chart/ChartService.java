package com.dataviz.service.chart;

import com.dataviz.domain.chart.ChartConfig;
import com.dataviz.domain.chart.ChartStyle;
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

        ChartConfig adjusted = adjustSeriesColors(config);
        return new ChartRenderResult(dataSet, adjusted);
    }

    public ChartRenderResult buildChartFromFilter(FilterResult filterResult,
                                                  ChartConfig config) {
        Objects.requireNonNull(filterResult, "filterResult must not be null");
        Objects.requireNonNull(config, "config must not be null");

        LOG.fine(() -> "Building chart from filter: %s rows".formatted(
                filterResult.getMatchedCount()));

        ChartConfig adjusted = adjustSeriesColors(config);
        return new ChartRenderResult(filterResult.getSource(), adjusted,
            filterResult.getMatchedIndices());
    }

        private ChartConfig adjustSeriesColors(ChartConfig cfg) {
        if (cfg == null) return null;
        ChartStyle style = cfg.getStyle();
        if (style == null) return cfg;
        int need = cfg.getYColumns() == null ? 0 : cfg.getYColumns().size();
        List<String> current = style.getSeriesColors();
        if (current != null && current.size() == need) return cfg;

        List<String> base = (current != null && !current.isEmpty())
            ? current
            : ChartStyle.defaultStyle().getSeriesColors();
        List<String> newColors = new java.util.ArrayList<>();
        for (int i = 0; i < need; i++) newColors.add(base.get(i % base.size()));

        ChartStyle newStyle = style.toBuilder().seriesColors(newColors).build();
        return ChartConfig.builder()
            .id(cfg.getId())
            .chartType(cfg.getChartType())
            .title(cfg.getTitle())
            .xColumn(cfg.getXColumn())
            .yColumns(cfg.getYColumns())
            .xLabel(cfg.getXLabel())
            .yLabel(cfg.getYLabel())
            .colorColumn(cfg.getColorColumn())
            .style(newStyle)
            .build();
        }

    private void validateColumns(DataSet ds, ChartConfig cfg) {
        if (!ds.hasColumn(cfg.getXColumn())) {
            throw new IllegalArgumentException(
                    "X column not found: '%s'".formatted(cfg.getXColumn()));
        }
        if (cfg.getYColumns() == null || cfg.getYColumns().isEmpty()) {
            throw new IllegalArgumentException("At least one Y column is required");
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