package com.dataviz.ui.view;

import com.dataviz.domain.dashboard.DashboardSnapshot;
import com.dataviz.service.chart.ChartService.ChartRenderResult;
import com.dataviz.ui.ServiceLocatorHolder;
import com.dataviz.ui.chart.ChartFxChartPanel;
import com.dataviz.ui.presenter.ChartEditorPresenter;
import com.dataviz.domain.chart.ChartConfig;
import com.dataviz.domain.chart.ChartTypeSettings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class ChartEditorView implements IChartEditorView {

    @FXML private ToggleGroup      chartTypeGroup;
    @FXML private TextField        chartTitleField;
    @FXML private TextField        xAxisLabelField;
    @FXML private TextField        yAxisLabelField;
    @FXML private ComboBox<String> xColumnCombo;
    @FXML private ListView<String> yColumnsList;
    @FXML private ComboBox<String> styleThemeCombo;
    @FXML private CheckBox         cbShowLegend;
    @FXML private CheckBox         cbShowGrid;
    @FXML private CheckBox         cbShowTooltips;

    @FXML private VBox             lineWidthContainer;
    @FXML private Slider           lineWidthSlider;
    @FXML private Label            lineWidthLabel;
    @FXML private CheckBox         cbSmoothing;

    @FXML private VBox             pointSizeContainer;
    @FXML private Slider           pointSizeSlider;
    @FXML private Label            pointSizeLabel;
    @FXML private VBox             pointShapeContainer;
    @FXML private ComboBox<String> pointShapeCombo;
    @FXML private CheckBox         cbShowTrendLine;
    @FXML private VBox             seriesTransparencyContainer;
    @FXML private Slider           seriesTransparencySlider;
    @FXML private Label            seriesTransparencyLabel;

    @FXML private VBox             barWidthContainer;
    @FXML private Slider           barWidthSlider;
    @FXML private Label            barWidthLabel;
    @FXML private VBox             stackingModeContainer;
    @FXML private ComboBox<String> stackingModeCombo;

    @FXML private CheckBox         cbSliceLabels;
    @FXML private VBox             legendPositionContainer;
    @FXML private ComboBox<String> legendPositionCombo;
    @FXML private CheckBox         cbDonutMode;
    @FXML private VBox             innerRadiusContainer;
    @FXML private Slider           innerRadiusSlider;
    @FXML private Label            innerRadiusLabel;

    @FXML private VBox             colorScaleContainer;
    @FXML private ComboBox<String> colorScaleCombo;
    @FXML private VBox             colorRangeMinContainer;
    @FXML private TextField        colorRangeMinField;
    @FXML private VBox             colorRangeMaxContainer;
    @FXML private TextField        colorRangeMaxField;
    @FXML private CheckBox         cbShowAxisLabels;

    @FXML private VBox             visualStyleControls;
    @FXML private VBox             styleHelpBox;
    @FXML private TextArea         styleHelpText;

    @FXML private ToggleGroup      exportFormatGroup;
    @FXML private ComboBox<String> resolutionCombo;
    @FXML private StackPane        chartPreviewPane;
    @FXML private ProgressIndicator renderSpinner;
    @FXML private VBox             previewEmptyState;
    @FXML private HBox             exportProgressBox;
    @FXML private Label            previewStatusLabel;
    @FXML private Label            renderTimeLabel;
    @FXML private Label            datasetNameLabel;
    @FXML private ToggleButton     btnAutoRefresh;
    @FXML private Button           btnApply;

    private ChartEditorPresenter presenter;
    private ChartFxChartPanel    previewPanel;
    private List<String>         allColumns = List.of();

    @FXML
    private void initialize() {
        presenter = ServiceLocatorHolder.get().get(ChartEditorPresenter.class);
        presenter.setView(this);

        guardSliderFromStrayDrags(lineWidthSlider);
        guardSliderFromStrayDrags(pointSizeSlider);
        guardSliderFromStrayDrags(seriesTransparencySlider);
        guardSliderFromStrayDrags(barWidthSlider);
        guardSliderFromStrayDrags(innerRadiusSlider);

        lineWidthSlider.valueProperty().addListener((obs, o, n) ->
                lineWidthLabel.setText("%.1f".formatted(n.doubleValue())));

        lineWidthSlider.valueChangingProperty().addListener((obs, oldV, newV) -> {
            if (Boolean.FALSE.equals(newV) && presenter != null) {
                presenter.onStyleOptionChanged(
                        cbShowLegend.isSelected(),
                        cbShowGrid.isSelected(),
                        cbShowTooltips.isSelected(),
                        lineWidthSlider.getValue());
            }
        });

        pointSizeSlider.valueProperty().addListener((obs, o, n) -> {
            pointSizeLabel.setText("%.1f".formatted(n.doubleValue()));
            presenter.onPointSizeChanged(n.doubleValue());
        });
        seriesTransparencySlider.valueProperty().addListener((obs, o, n) -> {
            seriesTransparencyLabel.setText((int) (n.doubleValue() * 100) + "%");
            presenter.onSeriesTransparencyChanged(n.doubleValue());
        });
        pointShapeCombo.setOnAction(e -> {
            if (pointShapeCombo.getValue() != null)
                presenter.onPointShapeChanged(pointShapeCombo.getValue());
        });
        cbShowTrendLine.selectedProperty().addListener((obs, o, n) ->
                presenter.onShowTrendLineChanged(n));

        barWidthSlider.valueProperty().addListener((obs, o, n) -> {
            barWidthLabel.setText("%.2f".formatted(n.doubleValue()));
            presenter.onBarWidthChanged(n.doubleValue());
        });
        stackingModeCombo.setOnAction(e -> {
            if (stackingModeCombo.getValue() != null)
                presenter.onStackingModeChanged(stackingModeCombo.getValue());
        });

        cbSmoothing.selectedProperty().addListener((obs, o, n) ->
                presenter.onSmoothingChanged(n));

        cbSliceLabels.selectedProperty().addListener((obs, o, n) ->
                presenter.onSliceLabelsChanged(n));
        legendPositionCombo.setOnAction(e -> {
            if (legendPositionCombo.getValue() != null)
                presenter.onLegendPositionChanged(legendPositionCombo.getValue());
        });
        cbDonutMode.selectedProperty().addListener((obs, o, donut) -> {
            setControlVisible(donut, innerRadiusContainer);
            presenter.onDonutModeChanged(donut);
        });
        innerRadiusSlider.valueProperty().addListener((obs, o, n) -> {
            innerRadiusLabel.setText("%.0f%%".formatted(n.doubleValue()));
            presenter.onInnerRadiusChanged(n.doubleValue());
        });

        colorScaleCombo.setOnAction(e -> {
            if (colorScaleCombo.getValue() != null)
                presenter.onColorScaleChanged(colorScaleCombo.getValue());
        });
        colorRangeMinField.textProperty().addListener((obs, o, n) ->
                presenter.onColorRangeMinChanged(n));
        colorRangeMaxField.textProperty().addListener((obs, o, n) ->
                presenter.onColorRangeMaxChanged(n));
        cbShowAxisLabels.selectedProperty().addListener((obs, o, n) ->
                presenter.onShowAxisLabelsChanged(n));

        xColumnCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String selected = yColumnsList.getSelectionModel().getSelectedItem();
            yColumnsList.getItems().setAll(
                    allColumns.stream().filter(c -> !c.equals(newVal)).toList());
            if (selected != null && !selected.equals(newVal))
                yColumnsList.getSelectionModel().select(selected);
        });

        updateVisibleSettings(ChartConfig.ChartType.LINE);
    }

    public void updateVisibleSettings(ChartConfig.ChartType type) {
        setControlVisible(isApplicable(type, ChartTypeSettings.SettingType.SHOW_GRID),
                cbShowGrid);

        setControlVisible(isApplicable(type, ChartTypeSettings.SettingType.LINE_WIDTH),
                lineWidthContainer);
        setControlVisible(isApplicable(type, ChartTypeSettings.SettingType.SMOOTHING),
                cbSmoothing);

        setControlVisible(isApplicable(type, ChartTypeSettings.SettingType.POINT_SIZE),
                pointSizeContainer);
        setControlVisible(isApplicable(type, ChartTypeSettings.SettingType.POINT_SHAPE),
                pointShapeContainer);
        setControlVisible(isApplicable(type, ChartTypeSettings.SettingType.SHOW_TREND_LINE),
                cbShowTrendLine);
        setControlVisible(isApplicable(type, ChartTypeSettings.SettingType.SERIES_TRANSPARENCY),
                seriesTransparencyContainer);

        setControlVisible(isApplicable(type, ChartTypeSettings.SettingType.BAR_WIDTH),
                barWidthContainer);
        setControlVisible(isApplicable(type, ChartTypeSettings.SettingType.STACKING_MODE),
                stackingModeContainer);

        setControlVisible(isApplicable(type, ChartTypeSettings.SettingType.SLICE_LABELS),
                cbSliceLabels);
        setControlVisible(isApplicable(type, ChartTypeSettings.SettingType.LEGEND_POSITION),
                legendPositionContainer);
        setControlVisible(isApplicable(type, ChartTypeSettings.SettingType.DONUT_MODE),
                cbDonutMode);
        boolean innerRadiusVisible = isApplicable(type, ChartTypeSettings.SettingType.INNER_RADIUS)
                && cbDonutMode.isSelected();
        setControlVisible(innerRadiusVisible, innerRadiusContainer);

        setControlVisible(isApplicable(type, ChartTypeSettings.SettingType.COLOR_SCALE),
                colorScaleContainer);
        setControlVisible(isApplicable(type, ChartTypeSettings.SettingType.COLOR_RANGE_MIN),
                colorRangeMinContainer);
        setControlVisible(isApplicable(type, ChartTypeSettings.SettingType.COLOR_RANGE_MAX),
                colorRangeMaxContainer);
        setControlVisible(isApplicable(type, ChartTypeSettings.SettingType.SHOW_AXIS_LABELS),
                cbShowAxisLabels);

        styleHelpText.setText(helpTextFor(type));
    }

    private String helpTextFor(ChartConfig.ChartType type) {
        return switch (type) {
            case LINE -> """
                    ЛІНІЙНИЙ ГРАФІК

                    Відображає зміну значень у часі або по категоріях.

                    Основні параметри:
                            • Point Size — розмір маркерів (2–15)
                    • Show Grid / Legend / Tooltips

                    Додатково:
                    • Smooth Curves — кубічна інтерполяція між точками (гладкі вигини замість прямих відрізків)""";

            case AREA -> """
                    ПЛОЩИННИЙ ГРАФІК

                    Як лінійний, але з заповненням під лінією.
                    Добре підходить для порівняння об'ємів між серіями.

                    Основні параметри:

                    • Stacking Mode:
                        GROUPED — серії поруч
                        STACKED — серії накладаються
                    • Show Grid / Legend / Tooltips""";

            case BAR -> """
                    СТОВПЧИКОВИЙ ГРАФІК

                    Порівнює категорії по висоті стовпців.

                    Основні параметри:
                    • Bar Width — ширина стовпців (0.3 = вузькі, 1.0 = впритул)
                    • Stacking Mode:
                        GROUPED — групи поруч
                        STACKED — складені один на одного
                    • Show Grid / Legend / Tooltips""";

            case SCATTER -> """
                    ТОЧКОВИЙ ГРАФІК

                    Показує розподіл і кореляцію між двома числовими змінними.

                    Основні параметри:
                    • Point Size — розмір точок (2–15)
                    • Point Shape — форма маркера:
                        CIRCLE / SQUARE / DIAMOND
                    • Show Legend / Tooltips

                    Додатково:
                    • Show Trend Line — лінія лінійної регресії
                    • Transparency — прозорість точок (корисно при великій кількості перекриттів)""";

            case PIE -> """
                    КРУГОВА ДІАГРАМА

                    Показує частки від цілого. Використовуйте лише з однією серією даних.

                    Основні параметри:
                    • Slice Labels — підписи значень/відсотків на секторах
                    • Legend Position — розташування легенди (TOP/BOTTOM/LEFT/RIGHT)
                    • Show Legend / Tooltips

                    Додатково:
                    • Donut Mode — перетворює діаграму на бублик (кільцевий графік)
                    • Inner Radius — розмір внутрішнього отвору, % (10–80)
                      Відображається тільки при увімкненому Donut Mode""";

            case HEATMAP -> """
                    ТЕПЛОВА КАРТА

                    Двовимірна матриця, де значення кодуються кольором.
                    Потребує трьох колонок: X, Y і значення.

                    Основні параметри:
                    • Color Scale — палітра кольорів:
                        VIRIDIS — від синього до жовтого (нейтральна)
                        JET — від синього до червоного (контрастна)
                        COOL_WARM — прохолодний → теплий
                    • Show Axis Labels — підписи рядків/стовпців
                    • Show Tooltips

                    Додатково:
                    • Color Range Min / Max — фіксовані межі шкали
                      (порожньо = авто за даними)""";

            default -> "Оберіть тип графіку, щоб побачити доступні параметри.";
        };
    }

    @FXML
    private void onChartTypeChanged(ActionEvent e) {
        if (chartTypeGroup.getSelectedToggle() == null) return;
        Object ud = chartTypeGroup.getSelectedToggle().getUserData();
        if (ud != null) {
            String typeName = ud.toString();
            try {
                ChartConfig.ChartType type = ChartConfig.ChartType.valueOf(typeName);
                updateVisibleSettings(type);
            } catch (IllegalArgumentException ignored) { }
            if (presenter != null) presenter.onChartTypeChangedByName(typeName);
        }
    }

    @FXML private void onTitleChanged() {
        presenter.onTitleChanged(chartTitleField.getText());
    }

    @FXML private void onXColumnChanged(ActionEvent e) {
        if (xColumnCombo.getValue() != null)
            presenter.onXColumnChanged(xColumnCombo.getValue());
    }

    @FXML private void onStyleThemeChanged(ActionEvent e) {
        if (styleThemeCombo.getValue() != null)
            presenter.onStyleThemeChangedByName(styleThemeCombo.getValue());
    }

    @FXML private void onStyleOptionChanged(ActionEvent e) {
        presenter.onStyleOptionChanged(
                cbShowLegend.isSelected(),
                cbShowGrid.isSelected(),
                cbShowTooltips.isSelected(),
                lineWidthSlider.getValue());
    }

    @FXML private void onLineWidthChanged() {
        onStyleOptionChanged(null);
    }

    @FXML private void onAddYColumn(ActionEvent e) {
        String sel = yColumnsList.getSelectionModel().getSelectedItem();
        if (sel != null) presenter.onAddYColumn(sel);
    }

    @FXML private void onRemoveYColumn(ActionEvent e) {
        String sel = yColumnsList.getSelectionModel().getSelectedItem();
        if (sel != null) presenter.onRemoveYColumn(sel);
    }

    @FXML private void onRefreshPreview(ActionEvent e) { presenter.onRefreshPreview(); }
    @FXML private void onApplyClicked(ActionEvent e)   { presenter.onApplyClicked(); }

    @FXML private void onCancel(ActionEvent e) {
        if (chartPreviewPane.getScene() != null)
            chartPreviewPane.getScene().getWindow().hide();
    }

    @FXML private void onExportClicked(ActionEvent e) {
        String fmt = exportFormatGroup.getSelectedToggle() != null
                ? exportFormatGroup.getSelectedToggle().getUserData().toString()
                : "png";

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Експорт діаграми");
        chooser.setInitialFileName("chart-export." + fmt);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(fmt.toUpperCase() + " файл", "*." + fmt));

        Stage stage = getStage();
        File  file  = stage != null ? chooser.showSaveDialog(stage) : null;
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

        boolean noData = cols.isEmpty();
        setControlVisible(noData, previewEmptyState);
        if (previewPanel != null) previewPanel.getContainer().setVisible(!noData);
    }

    @Override
    public void renderChart(ChartRenderResult result) {
        if (btnAutoRefresh != null && !btnAutoRefresh.isSelected()) return;
        doRender(result);
    }

    @Override
    public void renderChartForced(ChartRenderResult result) {
        doRender(result);
    }

    private void doRender(ChartRenderResult result) {
        javafx.application.Platform.runLater(() -> {
            if (chartPreviewPane == null) return;
            long start = System.currentTimeMillis();

            setControlVisible(false, previewEmptyState);
            renderSpinner.setVisible(true);

            if (previewPanel == null) {
                previewPanel = new ChartFxChartPanel("editor-preview", result);
                previewPanel.getContainer().prefWidthProperty().bind(chartPreviewPane.widthProperty());
                previewPanel.getContainer().prefHeightProperty().bind(chartPreviewPane.heightProperty());
                chartPreviewPane.getChildren().add(previewPanel.getContainer());
            } else {
                previewPanel.updateConfig(result);
            }

            renderSpinner.setVisible(false);
            long ms = System.currentTimeMillis() - start;
            previewStatusLabel.setText("Оновлено");
            renderTimeLabel.setText(ms + " ms");
        });
    }

    @Override
    public DashboardSnapshot getPreviewSnapshot() {
        return previewPanel != null ? previewPanel.toDomainSnapshot() : null;
    }

    public void initWithDataset(com.dataviz.domain.model.DataSet dataSet) {
        if (presenter != null) presenter.initWithDataset(dataSet);
    }

    @Override public void showExportProgress(boolean v) {
        exportProgressBox.setVisible(v);
        exportProgressBox.setManaged(v);
    }

    @Override public void showApplySuccess() {
        previewStatusLabel.setText("✓ Застосовано");
    }

    @Override public void showExportSuccess(Path p) {
        previewStatusLabel.setText("✓ Збережено: " + p.getFileName());
    }

    @Override public void showError(String t, String m) {
        previewStatusLabel.setText("✗ " + t + ": " + m);
    }

    private static boolean isApplicable(ChartConfig.ChartType type,
                                        ChartTypeSettings.SettingType setting) {
        return ChartTypeSettings.isApplicable(type, setting);
    }

    private void setControlVisible(boolean visible, Region... controls) {
        for (Region ctrl : controls) {
            if (ctrl == null) continue;
            ctrl.setVisible(visible);
            ctrl.setManaged(visible);
        }
    }

    private void guardSliderFromStrayDrags(Slider s) {
        if (s == null) return;
        s.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!e.isPrimaryButtonDown()) e.consume();
        });
    }

    private Stage getStage() {
        if (chartPreviewPane == null || chartPreviewPane.getScene() == null) return null;
        javafx.stage.Window w = chartPreviewPane.getScene().getWindow();
        return w instanceof Stage s ? s : null;
    }
}