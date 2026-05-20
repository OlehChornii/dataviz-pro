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
        Platform.runLater(() -> handleDatasetLoaded(dataSet));
    }

    public void initWithConfig(DataSet dataSet, ChartConfig config) {
        this.currentDataSet = dataSet;
        this.currentConfig  = config;
        Platform.runLater(() -> {
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
        ChartStyle style = ChartStyle.builder()
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
                .build();
        currentConfig = ChartConfig.builder()
                .id(currentConfig.getId()).chartType(currentConfig.getChartType())
                .title(currentConfig.getTitle()).xColumn(currentConfig.getXColumn())
                .yColumns(currentConfig.getYColumns()).style(style).build();
        refreshPreview();
    }

    public void onAddYColumn(String columnName) {
        if (currentConfig == null || columnName == null) return;
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
        view.renderChart(result);
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
            currentConfig = ChartConfig.builder()
                    .id(UUID.randomUUID().toString())
                    .chartType(ChartConfig.ChartType.LINE)
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