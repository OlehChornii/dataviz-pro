package com.dataviz.ui.presenter;

import com.dataviz.common.event.DatasetChangeEvent;
import com.dataviz.common.event.DatasetObservable;
import com.dataviz.common.event.DatasetObserver;
import com.dataviz.di.annotation.*;
import com.dataviz.domain.chart.ChartConfig;
import com.dataviz.domain.chart.ChartStyle;
import com.dataviz.domain.model.DataSet;
import com.dataviz.facade.DataVizFacade;
import com.dataviz.service.chart.ChartService.ChartRenderResult;
import com.dataviz.ui.view.ChartEditorView;
import com.dataviz.ui.view.IChartEditorView;
import javafx.application.Platform;

import java.util.*;
import java.util.logging.Logger;

@Component
public final class ChartEditorPresenter implements DatasetObserver {

    private static final Logger LOG = Logger.getLogger(ChartEditorPresenter.class.getName());

    private final DataVizFacade     facade;
    private final DatasetObservable observable;

    private IChartEditorView view;

    private DataSet     currentDataSet;
    private ChartConfig currentConfig;

    @Inject
    public ChartEditorPresenter(DataVizFacade facade, DatasetObservable observable) {
        this.facade     = Objects.requireNonNull(facade);
        this.observable = Objects.requireNonNull(observable);
    }

    public void setView(IChartEditorView view) {
        this.view = Objects.requireNonNull(view);
        if (view instanceof ChartEditorView) {
            observable.addObserver(this);
        }
    }

    public void destroy() {
        unsubscribe();
    }

    private void runLaterOrRunDirectly(Runnable action) {
        try {
            Platform.runLater(action);
        } catch (IllegalStateException e) {
            action.run();
        }
    }

    @PreDestroy
    private void unsubscribe() { observable.removeObserver(this); }

    @Override
    public void onDatasetChanged(DatasetChangeEvent event) {
        switch (event.type()) {
            case DATASET_LOADED -> handleDatasetLoaded(event.source());
            case FILTER_APPLIED -> handleFilterApplied(event);
            case FILTER_RESET   -> handleDatasetLoaded(event.source());
        }
    }

    public void initWithDataset(DataSet dataSet) {
        runLaterOrRunDirectly(() -> handleDatasetLoaded(dataSet));
    }

    public void initWithConfig(DataSet dataSet, ChartConfig config) {
        this.currentDataSet = dataSet;
        this.currentConfig  = config;
        runLaterOrRunDirectly(() -> {
            if (view == null) return;
            List<String> cols = dataSet.getColumns().stream().map(c -> c.getName()).toList();
            view.populateColumnSelectors(cols);
            refreshPreview();
        });
    }

    public void onChartTypeChangedByName(String typeName) {
        try { onChartTypeChanged(ChartConfig.ChartType.valueOf(typeName)); }
        catch (IllegalArgumentException ignored) {}
    }

    private void onChartTypeChanged(ChartConfig.ChartType chartType) {
        if (currentConfig == null) return;
        currentConfig = ChartConfig.builder()
                .id(currentConfig.getId())
                .chartType(chartType)
                .title(currentConfig.getTitle())
                .xColumn(currentConfig.getXColumn())
                .yColumns(currentConfig.getYColumns())
                .style(currentConfig.getStyle())
                .build();
        if (view instanceof ChartEditorView) {
            ((ChartEditorView) view).updateVisibleSettings(chartType);
        }
        refreshPreview();
    }

    public void onXColumnChanged(String columnName) {
        if (currentConfig == null) return;
        currentConfig = ChartConfig.builder()
                .id(currentConfig.getId())
                .chartType(currentConfig.getChartType())
                .title(currentConfig.getTitle())
                .xColumn(columnName)
                .yColumns(currentConfig.getYColumns())
                .style(currentConfig.getStyle())
                .build();
        refreshPreview();
    }

    public void onTitleChanged(String title) {
        if (currentConfig == null) return;
        currentConfig = currentConfig.withTitle(title);
        refreshPreview();
    }

    public void onStyleThemeChangedByName(String themeName) {
        try { onStyleThemeChanged(ChartStyle.Theme.valueOf(themeName)); }
        catch (IllegalArgumentException ignored) {}
    }

    public void onStyleThemeChanged(ChartStyle.Theme theme) {
        if (currentConfig == null) return;
        ChartStyle newStyle = switch (theme) {
            case DARK      -> ChartStyle.darkStyle();
            case CORPORATE -> ChartStyle.corporateStyle();
            default        -> ChartStyle.defaultStyle();
        };
        currentConfig = ChartConfig.builder()
                .id(currentConfig.getId())
                .chartType(currentConfig.getChartType())
                .title(currentConfig.getTitle())
                .xColumn(currentConfig.getXColumn())
                .yColumns(currentConfig.getYColumns())
                .style(newStyle)
                .build();
        refreshPreview();
    }

    public void onStyleOptionChanged(boolean legend, boolean grid,
                                     boolean tooltips, double lineWidth) {
        if (currentConfig == null) return;
        ChartStyle.Builder styleBuilder = ChartStyle.builder()
                .theme(currentConfig.getStyle().getTheme())
                .showLegend(legend).showGrid(grid).showTooltips(tooltips)
                .lineWidth(lineWidth)
                .seriesColors(currentConfig.getStyle().getSeriesColors())
                .backgroundColor(currentConfig.getStyle().getBackgroundColor())
                .gridColor(currentConfig.getStyle().getGridColor())
                .axisColor(currentConfig.getStyle().getAxisColor())
                .titleFont(currentConfig.getStyle().getTitleFont())
                .titleFontSize(currentConfig.getStyle().getTitleFontSize())
                .labelFont(currentConfig.getStyle().getLabelFont())
                .labelFontSize(currentConfig.getStyle().getLabelFontSize())
                .pointSize(currentConfig.getStyle().getPointSize())
                .barWidth(currentConfig.getStyle().getBarWidth())
                .stackingMode(currentConfig.getStyle().getStackingMode())
                .pointShape(currentConfig.getStyle().getPointShape())
                .sliceLabels(currentConfig.getStyle().isSliceLabels())
                .legendPosition(currentConfig.getStyle().getLegendPosition())
                .colorScale(currentConfig.getStyle().getColorScale())
                .showAxisLabels(currentConfig.getStyle().isShowAxisLabels());
        
        ChartStyle style = styleBuilder.build();
        currentConfig = ChartConfig.builder()
                .id(currentConfig.getId()).chartType(currentConfig.getChartType())
                .title(currentConfig.getTitle()).xColumn(currentConfig.getXColumn())
                .yColumns(currentConfig.getYColumns()).style(style).build();
        refreshPreview();
    }

    public void onAddYColumn(String columnName) {
        if (currentConfig == null) return;
        if (columnName == null) return;
        if (currentDataSet == null || currentDataSet.getColumns().stream()
                .noneMatch(column -> columnName.equals(column.getName()))) {
            throw new IllegalArgumentException("Unknown column: " + columnName);
        }
        if (currentConfig.getYColumns().contains(columnName)) return;
        var newY = new ArrayList<>(currentConfig.getYColumns());
        newY.add(columnName);
        currentConfig = ChartConfig.builder()
                .id(currentConfig.getId()).chartType(currentConfig.getChartType())
                .title(currentConfig.getTitle()).xColumn(currentConfig.getXColumn())
                .yColumns(newY).style(currentConfig.getStyle()).build();
        refreshPreview();
    }

    public void onRemoveYColumn(String columnName) {
        if (currentConfig == null || columnName == null) return;
        var newY = new ArrayList<>(currentConfig.getYColumns());
        newY.remove(columnName);
        currentConfig = ChartConfig.builder()
                .id(currentConfig.getId()).chartType(currentConfig.getChartType())
                .title(currentConfig.getTitle()).xColumn(currentConfig.getXColumn())
                .yColumns(newY).style(currentConfig.getStyle()).build();
        refreshPreview();
    }

    public void onLineWidthChanged(double lineWidth) {
        if (currentConfig == null) return;
        updateStyleProperty(style -> style.toBuilder().lineWidth(lineWidth).build());
    }

    public void onPointSizeChanged(double pointSize) {
        if (currentConfig == null) return;
        updateStyleProperty(style -> style.toBuilder().pointSize(pointSize).build());
    }

    public void onBarWidthChanged(double barWidth) {
        if (currentConfig == null) return;
        updateStyleProperty(style -> style.toBuilder().barWidth(barWidth).build());
    }

    public void onStackingModeChanged(String stackingMode) {
        if (currentConfig == null) return;
        updateStyleProperty(style -> style.toBuilder().stackingMode(stackingMode).build());
    }

    public void onPointShapeChanged(String pointShape) {
        if (currentConfig == null) return;
        updateStyleProperty(style -> style.toBuilder().pointShape(pointShape).build());
    }

    public void onSliceLabelsChanged(boolean sliceLabels) {
        if (currentConfig == null) return;
        updateStyleProperty(style -> style.toBuilder().sliceLabels(sliceLabels).build());
    }

    public void onLegendPositionChanged(String legendPosition) {
        if (currentConfig == null) return;
        updateStyleProperty(style -> style.toBuilder().legendPosition(legendPosition).build());
    }

    public void onColorScaleChanged(String colorScale) {
        if (currentConfig == null) return;
        updateStyleProperty(style -> style.toBuilder().colorScale(colorScale).build());
    }

    public void onShowAxisLabelsChanged(boolean showAxisLabels) {
        if (currentConfig == null) return;
        updateStyleProperty(style -> style.toBuilder().showAxisLabels(showAxisLabels).build());
    }

    public void onSmoothingChanged(boolean smoothing) {
        if (currentConfig == null) return;
        updateStyleProperty(style -> style.toBuilder().smoothing(smoothing).build());
    }

    public void onShowTrendLineChanged(boolean showTrendLine) {
        if (currentConfig == null) return;
        updateStyleProperty(style -> style.toBuilder().showTrendLine(showTrendLine).build());
    }

    public void onSeriesTransparencyChanged(double transparency) {
        if (currentConfig == null) return;
        updateStyleProperty(style -> style.toBuilder().seriesTransparency(transparency).build());
    }

    public void onDonutModeChanged(boolean donutMode) {
        if (currentConfig == null) return;
        updateStyleProperty(style -> style.toBuilder().donutMode(donutMode).build());
    }

    public void onInnerRadiusChanged(double innerRadius) {
        if (currentConfig == null) return;
        updateStyleProperty(style -> style.toBuilder().innerRadius(innerRadius).build());
    }

    public void onColorRangeMinChanged(String colorRangeMin) {
        if (currentConfig == null) return;
        updateStyleProperty(style -> style.toBuilder().colorRangeMin(colorRangeMin).build());
    }

    public void onColorRangeMaxChanged(String colorRangeMax) {
        if (currentConfig == null) return;
        updateStyleProperty(style -> style.toBuilder().colorRangeMax(colorRangeMax).build());
    }

    private void updateStyleProperty(java.util.function.Function<ChartStyle, ChartStyle> styleTransform) {
        ChartStyle newStyle = styleTransform.apply(currentConfig.getStyle());
        currentConfig = ChartConfig.builder()
                .id(currentConfig.getId()).chartType(currentConfig.getChartType())
                .title(currentConfig.getTitle()).xColumn(currentConfig.getXColumn())
                .yColumns(currentConfig.getYColumns()).style(newStyle).build();
        refreshPreview();
    }

    public void onRefreshPreview() { refreshPreview(); }

    public void onExportClicked(String format, java.nio.file.Path output) {
        if (view == null) return;
        view.showExportProgress(true);
        try {
            com.dataviz.domain.dashboard.DashboardSnapshot snapshot =
                    view.getPreviewSnapshot();

            if (snapshot == null || snapshot.getPngBytes() == null
                    || snapshot.getPngBytes().length == 0) {
                view.showExportProgress(false);
                view.showError("Export error", "Немає графіку для експорту. " +
                        "Спочатку натисніть 'Apply'.");
                return;
            }

            facade.exportDashboard(snapshot, format, output,
                    com.dataviz.service.export.ExportOptions.forPublication());
            view.showExportProgress(false);
            view.showExportSuccess(output);
        } catch (Exception e) {
            view.showExportProgress(false);
            view.showError("Export error", e.getMessage());
            LOG.severe("Export error: " + e.getMessage());
        }
    }

    public void onApplyClicked() {
        if (currentDataSet == null || currentConfig == null || view == null) return;
        ChartRenderResult result = facade.buildChart(currentDataSet, currentConfig);
        view.renderChartForced(result);
        view.showApplySuccess();
        observable.notifyChartApplied(currentDataSet, currentConfig);
        LOG.info("Chart applied: " + currentConfig.getChartType());
    }

    public ChartConfig getCurrentConfig() { return currentConfig; }

    private void handleDatasetLoaded(DataSet ds) {
        this.currentDataSet = ds;
        if (!ds.getColumns().isEmpty()) {
            String xCol = ds.getColumns().get(0).getName();
            String yCol = ds.getColumns().size() > 1
                    ? ds.getColumns().get(1).getName() : xCol;

            ChartConfig.ChartType preservedType = (currentConfig != null)
                    ? currentConfig.getChartType()
                    : ChartConfig.ChartType.LINE;

            currentConfig = ChartConfig.builder()
                    .id(UUID.randomUUID().toString())
                    .chartType(preservedType)
                    .title(ds.getName())
                    .xColumn(xCol)
                    .addYColumn(yCol)
                    .style(ChartStyle.defaultStyle())
                    .build();
        }
        if (view != null) {
            view.populateColumnSelectors(
                    ds.getColumns().stream().map(c -> c.getName()).toList());
            refreshPreview();
        }
    }

    private void handleFilterApplied(DatasetChangeEvent event) {
        if (currentConfig == null || view == null) return;
        ChartRenderResult result = facade.buildChart(event.source(), currentConfig);
        view.renderChart(result);
    }

    private void refreshPreview() {
        if (currentDataSet == null || currentConfig == null || view == null) return;
        ChartRenderResult result = facade.buildChart(currentDataSet, currentConfig);
        view.renderChart(result);
    }
}