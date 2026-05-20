package com.dataviz.ui.view;

import com.dataviz.domain.dashboard.DashboardSnapshot;
import com.dataviz.service.chart.ChartService.ChartRenderResult;
import com.dataviz.ui.ServiceLocatorHolder;
import com.dataviz.ui.chart.ChartFxChartPanel;
import com.dataviz.ui.presenter.ChartEditorPresenter;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class ChartEditorView implements IChartEditorView {

    @FXML private ToggleGroup      chartTypeGroup;
    @FXML private TextField        chartTitleField;
    @FXML private ComboBox<String> xColumnCombo;
    @FXML private ListView<String> yColumnsList;
    @FXML private ComboBox<String> styleThemeCombo;
    @FXML private CheckBox         cbShowLegend;
    @FXML private CheckBox         cbShowGrid;
    @FXML private CheckBox         cbShowTooltips;
    @FXML private Slider           lineWidthSlider;
    @FXML private Label            lineWidthLabel;
    @FXML private ToggleGroup      exportFormatGroup;
    @FXML private ComboBox<String> resolutionCombo;
    @FXML private StackPane        chartPreviewPane;
    @FXML private ProgressIndicator renderSpinner;
    @FXML private HBox             exportProgressBox;
    @FXML private Label            previewStatusLabel;
    @FXML private Label            renderTimeLabel;
    @FXML private Label            datasetNameLabel;

    private ChartEditorPresenter presenter;
    private ChartFxChartPanel    previewPanel;
    private List<String>         allColumns = List.of();

    @FXML
    private void initialize() {
        presenter = ServiceLocatorHolder.get().get(ChartEditorPresenter.class);
        presenter.setView(this);

        lineWidthSlider.valueProperty().addListener((obs, o, n) ->
                lineWidthLabel.setText("%.1f".formatted(n.doubleValue())));

        xColumnCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String selected = yColumnsList.getSelectionModel().getSelectedItem();
            yColumnsList.getItems().setAll(
                    allColumns.stream().filter(c -> !c.equals(newVal)).toList());
            if (selected != null && !selected.equals(newVal))
                yColumnsList.getSelectionModel().select(selected);
        });
    }

    @FXML void onChartTypeChanged(ActionEvent e) {
        if (chartTypeGroup.getSelectedToggle() == null) return;
        Object ud = chartTypeGroup.getSelectedToggle().getUserData();
        if (ud != null) presenter.onChartTypeChangedByName(ud.toString());
    }

    @FXML void onTitleChanged()                  { presenter.onTitleChanged(chartTitleField.getText()); }
    @FXML void onXColumnChanged(ActionEvent e)   { if (xColumnCombo.getValue() != null)
        presenter.onXColumnChanged(xColumnCombo.getValue()); }
    @FXML void onStyleThemeChanged(ActionEvent e){ if (styleThemeCombo.getValue() != null)
        presenter.onStyleThemeChangedByName(styleThemeCombo.getValue()); }
    @FXML void onStyleOptionChanged(ActionEvent e){ presenter.onStyleOptionChanged(
            cbShowLegend.isSelected(), cbShowGrid.isSelected(),
            cbShowTooltips.isSelected(), lineWidthSlider.getValue()); }
    @FXML void onLineWidthChanged()              { onStyleOptionChanged(null); }

    @FXML void onAddYColumn(ActionEvent e) {
        String sel = yColumnsList.getSelectionModel().getSelectedItem();
        if (sel != null) presenter.onAddYColumn(sel);
    }

    @FXML void onRemoveYColumn(ActionEvent e) {
        String sel = yColumnsList.getSelectionModel().getSelectedItem();
        if (sel != null) presenter.onRemoveYColumn(sel);
    }

    @FXML void onRefreshPreview(ActionEvent e) { presenter.onRefreshPreview(); }
    @FXML void onApplyClicked(ActionEvent e)   { presenter.onApplyClicked(); }

    @FXML void onCancel(ActionEvent e) {
        if (chartPreviewPane != null && chartPreviewPane.getScene() != null)
            chartPreviewPane.getScene().getWindow().hide();
    }

    @FXML void onExportClicked(ActionEvent e) {
        String fmt = exportFormatGroup.getSelectedToggle() != null
                ? exportFormatGroup.getSelectedToggle().getUserData().toString()
                : "png";

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export chart");
        chooser.setInitialFileName("chart-export." + fmt);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(fmt.toUpperCase() + " file", "*." + fmt));

        Stage stage = getStage();
        File file = stage != null
                ? chooser.showSaveDialog(stage)
                : null;

        if (file == null) return;

        presenter.onExportClicked(fmt, file.toPath());
    }

    @Override
    public void populateColumnSelectors(List<String> cols) {
        allColumns = List.copyOf(cols);
        xColumnCombo.getItems().setAll(cols);
        String currentX = xColumnCombo.getValue();
        yColumnsList.getItems().setAll(currentX != null
                ? cols.stream().filter(c -> !c.equals(currentX)).toList()
                : cols);
        if (xColumnCombo.getValue() == null && !cols.isEmpty())
            xColumnCombo.setValue(cols.get(0));
    }

    @Override
    public void renderChart(ChartRenderResult result) {
        javafx.application.Platform.runLater(() -> {
            if (chartPreviewPane == null) return;
            long start = System.currentTimeMillis();

            if (previewPanel == null) {
                previewPanel = new ChartFxChartPanel("editor-preview", result);
                previewPanel.getContainer().prefWidthProperty()
                        .bind(chartPreviewPane.widthProperty());
                previewPanel.getContainer().prefHeightProperty()
                        .bind(chartPreviewPane.heightProperty());
                chartPreviewPane.getChildren().add(previewPanel.getContainer());
            } else {
                previewPanel.updateConfig(result);
            }

            long ms = System.currentTimeMillis() - start;
            if (previewStatusLabel != null) previewStatusLabel.setText("Оновлено");
            if (renderTimeLabel    != null) renderTimeLabel.setText(ms + " ms");
        });
    }

    @Override
    public DashboardSnapshot getPreviewSnapshot() {
        if (previewPanel == null) return null;
        return previewPanel.toDomainSnapshot();
    }

    public void initWithDataset(com.dataviz.domain.model.DataSet dataSet) {
        if (presenter != null) presenter.initWithDataset(dataSet);
    }

    @Override
    public void showExportProgress(boolean v) {
        if (exportProgressBox != null) {
            exportProgressBox.setVisible(v);
            exportProgressBox.setManaged(v);
        }
    }

    @Override public void showApplySuccess() {
        if (previewStatusLabel != null) previewStatusLabel.setText("✓ Застосовано");
    }

    @Override public void showExportSuccess(Path p) {
        if (previewStatusLabel != null)
            previewStatusLabel.setText("✓ Збережено: " + p.getFileName());
    }

    @Override public void showError(String t, String m) {
        if (previewStatusLabel != null)
            previewStatusLabel.setText("✗ " + t + ": " + m);
    }

    private Stage getStage() {
        if (chartPreviewPane == null || chartPreviewPane.getScene() == null) return null;
        javafx.stage.Window window = chartPreviewPane.getScene().getWindow();
        return window instanceof Stage s ? s : null;
    }
}