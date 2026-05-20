package com.dataviz.ui.view;

import com.dataviz.domain.dashboard.DashboardSnapshot;
import com.dataviz.service.chart.ChartService.ChartRenderResult;

import java.nio.file.Path;
import java.util.List;

public interface IChartEditorView {

    void populateColumnSelectors(List<String> columnNames);

    void renderChart(ChartRenderResult result);

    void showExportProgress(boolean visible);

    void showApplySuccess();

    void showExportSuccess(Path savedTo);

    void showError(String title, String message);

    DashboardSnapshot getPreviewSnapshot();
}