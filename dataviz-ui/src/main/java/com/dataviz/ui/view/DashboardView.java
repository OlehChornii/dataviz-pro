package com.dataviz.ui.view;

import com.dataviz.domain.chart.ChartConfig;
import com.dataviz.domain.chart.ChartStyle;
import com.dataviz.domain.chart.ChartTypeSettings;
import com.dataviz.domain.dashboard.DashboardSnapshot;
import com.dataviz.domain.filter.FilterCriteria;
import com.dataviz.domain.model.ColumnType;
import com.dataviz.domain.model.DataSet;
import com.dataviz.service.chart.ChartService.ChartRenderResult;
import com.dataviz.ui.ServiceLocatorHolder;
import com.dataviz.ui.chart.ChartFxChartPanel;
import com.dataviz.ui.presenter.ChartEditorPresenter;
import com.dataviz.ui.presenter.DashboardPresenter;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import java.net.URL;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.OverrunStyle;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.*;

public class DashboardView {

    private boolean updatingEditor = false;
    private boolean languageEnglish = false;
    private String currentDatasetName = "";
    private long   currentDatasetRows = 0;
    private int    currentDatasetCols = 0;

    @FXML private HBox              loadingBox;
    @FXML private ProgressIndicator progressSpinner;
    @FXML private Label             loadingLabel;
    @FXML private Label             datasetInfoLabel;

    @FXML private StackPane         leftStack;
    @FXML private VBox              datasetDrawer;
    @FXML private TextField         datasetSearchField;
    @FXML private ListView<String>  datasetList;
    @FXML private Label             activeFilterCount;
    @FXML private VBox              filterContainer;
    @FXML private Button            btnApplyFilter;
    @FXML private Button            btnResetFilter;

    @FXML private VBox              importDrawer;
    @FXML private ToggleGroup       importSourceGroup;
    @FXML private RadioButton       rbFile;
    @FXML private RadioButton       rbJdbc;
    @FXML private VBox              importFilePanel;
    @FXML private TextField         importFilePath;
    @FXML private VBox              importDropZone;
    @FXML private VBox              csvOptionsPanel;
    @FXML private ToggleGroup       delimiterGroup;
    @FXML private CheckBox          cbHeaderRow;
    @FXML private ComboBox<String>  encodingCombo;
    @FXML private VBox              importJdbcPanel;
    @FXML private ComboBox<String>  jdbcDriverCombo;
    @FXML private TextField         jdbcHostField;
    @FXML private TextField         jdbcPortField;
    @FXML private TextField         jdbcDbField;
    @FXML private TextField         jdbcUserField;
    @FXML private PasswordField     jdbcPassField;
    @FXML private TextArea          jdbcQueryArea;
    @FXML private Label             jdbcStatusLabel;
    @FXML private TextField         importDatasetName;
    @FXML private VBox              importProgressSection;
    @FXML private Label             importProgressLabel;
    @FXML private ProgressBar       importProgressBar;
    @FXML private Label             importErrorLabel;
    @FXML private Button            btnDoImport;

    @FXML private VBox              emptyState;
    @FXML private VBox              canvasPane;
    @FXML private Button            btnAddChart;
    @FXML private Button            btnOpenFullEditor;
    @FXML private Button            btnToggleLanguage;
    @FXML private CheckBox          gridToggle;
    @FXML private Label             filterStatusLabel;
    @FXML private FlowPane          chartContainer;

    @FXML private VBox              chartEditorPane;
    @FXML private SplitPane         centerSplit;
    @FXML private ToggleGroup       editorTabGroup;
    @FXML private ToggleButton      edTabChart;
    @FXML private VBox              editorNoSelection;
    @FXML private ScrollPane        editorChartScroll;
    @FXML private ScrollPane        editorStatsScroll;
    @FXML private VBox              edStatsContainer;
    @FXML private HBox              editorFooter;

    @FXML private ToggleGroup       chartTypeGroup;
    @FXML private TextField         edChartTitle;
    @FXML private ComboBox<String>  edXColumn;
    @FXML private TextField         edXLabel;
    @FXML private TextField         edYLabel;
    @FXML private ListView<String>  edYColumns;
    @FXML private ComboBox<String>  edAddSeriesCombo;

    @FXML private CheckBox          edShowLegend;
    @FXML private CheckBox          edShowGrid;
    @FXML private CheckBox          edShowTooltips;

    @FXML private VBox              edLineWidthContainer;
    @FXML private Slider            edLineWidth;
    @FXML private Label             edLineWidthLabel;
    @FXML private CheckBox          edCbSmoothing;

    @FXML private VBox              edPointSizeContainer;
    @FXML private Slider            edPointSizeSlider;
    @FXML private Label             edPointSizeLabel;
    @FXML private VBox              edPointShapeContainer;
    @FXML private ComboBox<String>  edPointShapeCombo;
    @FXML private CheckBox          edCbShowTrendLine;
    @FXML private VBox              edSeriesTransparencyContainer;
    @FXML private Slider            edSeriesTransparencySlider;
    @FXML private Label             edSeriesTransparencyLabel;

    @FXML private VBox              edBarWidthContainer;
    @FXML private Slider            edBarWidthSlider;
    @FXML private Label             edBarWidthLabel;
    @FXML private VBox              edStackingModeContainer;
    @FXML private ComboBox<String>  edStackingModeCombo;

    @FXML private CheckBox          edCbSliceLabels;
    @FXML private VBox              edLegendPositionContainer;
    @FXML private ComboBox<String>  edLegendPositionCombo;
    @FXML private CheckBox          edCbDonutMode;
    @FXML private VBox              edInnerRadiusContainer;
    @FXML private Slider            edInnerRadiusSlider;
    @FXML private Label             edInnerRadiusLabel;

    @FXML private VBox              edColorScaleContainer;
    @FXML private ComboBox<String>  edColorScaleCombo;
    @FXML private VBox              edColorRangeMinContainer;
    @FXML private TextField         edColorRangeMinField;
    @FXML private VBox              edColorRangeMaxContainer;
    @FXML private TextField         edColorRangeMaxField;
    @FXML private CheckBox          edCbShowAxisLabels;

    @FXML private ToggleGroup       exportFormatGroup;
    @FXML private ComboBox<String>  edResolution;

    @FXML private Label             statusLabel;
    @FXML private Label             rowCountLabel;
    @FXML private Label             colCountLabel;
    @FXML private Label             memoryLabel;

    private DashboardPresenter presenter;

    private final Map<String, ChartFxChartPanel> chartPanels       = new LinkedHashMap<>();
    private final Map<String, VBox>              cardNodes          = new LinkedHashMap<>();
    private final List<String>                   allDatasets        = new ArrayList<>();
    private final Map<String, Control>           filterControls     = new LinkedHashMap<>();
    private final Map<String, ColumnType>        filterColumnTypes  = new LinkedHashMap<>();

    private String selectedCardId = null;

    public String getSelectedCardId() { return selectedCardId; }
    @FXML
    private void initialize() {
        presenter = ServiceLocatorHolder.get().get(DashboardPresenter.class);
        presenter.attachView(this);

        if (chartEditorPane != null) {
            chartEditorPane.setVisible(false);
            chartEditorPane.setManaged(false);
            if (centerSplit != null && centerSplit.getItems().contains(chartEditorPane))
                centerSplit.getItems().remove(chartEditorPane);
        }
        if (btnOpenFullEditor != null) btnOpenFullEditor.setText(t("Show editor", "Повний редактор"));
        if (chartContainer != null) chartContainer.setAlignment(Pos.CENTER);

        System.out.println("[DashboardView] initialize: editorChartScroll=" + (editorChartScroll != null)
                + " editorStatsScroll=" + (editorStatsScroll != null)
                + " editorFooter=" + (editorFooter != null));

        guardSliderFromStrayDrags(edLineWidth);
        if (edPointSizeSlider != null) guardSliderFromStrayDrags(edPointSizeSlider);
        if (edSeriesTransparencySlider != null) guardSliderFromStrayDrags(edSeriesTransparencySlider);
        if (edBarWidthSlider != null) guardSliderFromStrayDrags(edBarWidthSlider);
        if (edInnerRadiusSlider != null) guardSliderFromStrayDrags(edInnerRadiusSlider);

        edLineWidth.valueProperty().addListener((obs, o, n) ->
                edLineWidthLabel.setText("%.1f".formatted(n.doubleValue())));
        edLineWidth.valueChangingProperty().addListener((obs, oldV, newV) -> {
            if (Boolean.FALSE.equals(newV) && presenter != null)
                presenter.onEditorStyleOptionChanged(
                        edShowLegend.isSelected(), edShowGrid.isSelected(),
                        edShowTooltips.isSelected(), edLineWidth.getValue());
        });

        edCbSmoothing.selectedProperty().addListener((obs, o, n) ->
                presenter.onEditorSmoothingChanged(n));

        if (edPointSizeSlider != null) {
            edPointSizeSlider.valueProperty().addListener((obs, o, n) -> {
                edPointSizeLabel.setText("%.1f".formatted(n.doubleValue()));
                presenter.onEditorPointSizeChanged(n.doubleValue());
            });
        }
        if (edSeriesTransparencySlider != null) {
            edSeriesTransparencySlider.valueProperty().addListener((obs, o, n) -> {
                edSeriesTransparencyLabel.setText((int) (n.doubleValue() * 100) + "%");
                presenter.onEditorSeriesTransparencyChanged(n.doubleValue());
            });
        }
        edPointShapeCombo.setOnAction(e -> {
            if (!updatingEditor && edPointShapeCombo.getValue() != null)
                presenter.onEditorPointShapeChanged(edPointShapeCombo.getValue());
        });
        if (edCbShowTrendLine != null) {
            edCbShowTrendLine.selectedProperty().addListener((obs, o, n) ->
                    presenter.onEditorShowTrendLineChanged(n));
        }

        if (edBarWidthSlider != null) {
            edBarWidthSlider.valueProperty().addListener((obs, o, n) -> {
                edBarWidthLabel.setText("%.2f".formatted(n.doubleValue()));
                presenter.onEditorBarWidthChanged(n.doubleValue());
            });
        }
        if (edStackingModeCombo != null) {
            edStackingModeCombo.setOnAction(e -> {
                if (!updatingEditor && edStackingModeCombo.getValue() != null)
                    presenter.onEditorStackingModeChanged(edStackingModeCombo.getValue());
            });
        }

        if (edCbSliceLabels != null) {
            edCbSliceLabels.selectedProperty().addListener((obs, o, n) ->
                    presenter.onEditorSliceLabelsChanged(n));
        }
        if (edLegendPositionCombo != null) {
            edLegendPositionCombo.setOnAction(e -> {
                if (!updatingEditor && edLegendPositionCombo.getValue() != null)
                    presenter.onEditorLegendPositionChanged(edLegendPositionCombo.getValue());
            });
        }
        if (edCbDonutMode != null) {
            edCbDonutMode.selectedProperty().addListener((obs, o, donut) -> {
                boolean applicable = ChartTypeSettings.isApplicable(
                        currentEditorType(), ChartTypeSettings.SettingType.INNER_RADIUS);
                setEdControlVisible(applicable && donut, edInnerRadiusContainer);
                presenter.onEditorDonutModeChanged(donut);
            });
        }
        if (edInnerRadiusSlider != null) {
            edInnerRadiusSlider.valueProperty().addListener((obs, o, n) -> {
                edInnerRadiusLabel.setText("%.0f%%".formatted(n.doubleValue()));
                presenter.onEditorInnerRadiusChanged(n.doubleValue());
            });
        }

        if (edColorScaleCombo != null) {
            edColorScaleCombo.setOnAction(e -> {
                if (!updatingEditor && edColorScaleCombo.getValue() != null)
                    presenter.onEditorColorScaleChanged(edColorScaleCombo.getValue());
            });
        }
        if (edColorRangeMinField != null) {
            edColorRangeMinField.textProperty().addListener((obs, o, n) -> {
                if (!updatingEditor) presenter.onEditorColorRangeMinChanged(n);
            });
        }
        if (edColorRangeMaxField != null) {
            edColorRangeMaxField.textProperty().addListener((obs, o, n) -> {
                if (!updatingEditor) presenter.onEditorColorRangeMaxChanged(n);
            });
        }
        if (edCbShowAxisLabels != null) {
            edCbShowAxisLabels.selectedProperty().addListener((obs, o, n) ->
                    presenter.onEditorShowAxisLabelsChanged(n));
        }

        updateEditorVisibleSettings(ChartConfig.ChartType.LINE);
        disableFilterButtons(true);
        if (centerSplit != null) attachDividerListener();

        if (leftStack != null) {
            javafx.scene.shape.Rectangle leftClip = new javafx.scene.shape.Rectangle();
            leftClip.widthProperty().bind(leftStack.widthProperty());
            leftClip.heightProperty().bind(leftStack.heightProperty());
            leftStack.setClip(leftClip);
        }
        updateLanguage();
    }

    @FXML void onToggleLanguage(ActionEvent e) {
        languageEnglish = !languageEnglish;
        updateLanguage();
    }

    private void updateLanguage() {
        if (btnToggleLanguage != null) {
            btnToggleLanguage.setText(languageEnglish ? "UA" : "EN");
            btnToggleLanguage.setTooltip(new Tooltip(languageEnglish
                    ? "Switch interface language"
                    : "Перемкнути мову інтерфейсу"));
        }
        if (btnOpenFullEditor != null) {
            btnOpenFullEditor.setText(languageEnglish
                    ? (chartEditorPane != null && chartEditorPane.isVisible() ? "Hide editor" : "Show editor")
                    : (chartEditorPane != null && chartEditorPane.isVisible() ? "Приховати редактор" : "Повний редактор"));
        }
        if (!currentDatasetName.isBlank()) {
            updateDatasetInfo(currentDatasetName, currentDatasetRows, currentDatasetCols);
        }
        if (importProgressSection != null && importProgressSection.isVisible()) {
            updateImportProgress(importProgressBar.getProgress());
        }
        chartContainer.getChildren().forEach(node -> {
            if (node instanceof VBox card) {
                HBox footer = (HBox) card.lookup(".chart-card-footer");
                if (footer != null && footer.getChildren().size() >= 2) {
                    javafx.scene.Node edit = footer.getChildren().get(0);
                    javafx.scene.Node dup  = footer.getChildren().get(1);
                    if (edit instanceof Button editBtn) editBtn.setText("✎ " + t("Edit", "Редагувати"));
                    if (dup instanceof Button dupBtn) dupBtn.setText("⧉ " + t("Duplicate", "Дублювати"));
                }
            }
        });
    }

    private String t(String english, String ukrainian) {
        return languageEnglish ? english : ukrainian;
    }

    public String localize(String english, String ukrainian) {
        return t(english, ukrainian);
    }

    private void attachDividerListener() {
        if (centerSplit == null || chartContainer == null) return;
        var divs = centerSplit.getDividers();
        divs.addListener((javafx.collections.ListChangeListener<javafx.scene.control.SplitPane.Divider>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    change.getAddedSubList().forEach(this::attachDividerPositionListener);
                }
            }
        });
        if (!divs.isEmpty()) {
            attachDividerPositionListener(divs.get(0));
        }
    }

    private void attachDividerPositionListener(javafx.scene.control.SplitPane.Divider divider) {
        if (divider == null || chartContainer == null) return;
        divider.positionProperty().addListener((obs, o, n) -> updateCardTransforms(n.doubleValue()));
    }

    private void updateCardTransforms(double position) {
        double min = 0.58;
        double max = 1.0;
        double frac = (position - min) / (max - min);
        if (frac < 0) frac = 0; if (frac > 1) frac = 1;
        double scale   = 0.87 + 0.13 * frac;
        double ty      = -7.0 * (1.0 - frac);
        double opacity = 0.82 + 0.18 * frac;
        chartContainer.getChildren().forEach(node -> {
            node.setScaleX(scale);
            node.setScaleY(scale);
            node.setTranslateY(ty);
            node.setOpacity(opacity);
        });
    }

    public void updateEditorVisibleSettings(ChartConfig.ChartType type) {
        boolean isLine    = type == ChartConfig.ChartType.LINE;
        boolean isArea    = type == ChartConfig.ChartType.AREA;
        boolean isBar     = type == ChartConfig.ChartType.BAR;
        boolean isScatter = type == ChartConfig.ChartType.SCATTER;
        boolean isPie     = type == ChartConfig.ChartType.PIE;
        boolean isHeatmap = type == ChartConfig.ChartType.HEATMAP;

        setEdControlVisible(!isScatter && !isPie && !isHeatmap, edShowGrid);

        setEdControlVisible(isLine, edLineWidthContainer);
        setEdControlVisible(isLine, edCbSmoothing);

        setEdControlVisible(isScatter, edPointSizeContainer);
        setEdControlVisible(isScatter, edPointShapeContainer);
        setEdControlVisible(isScatter, edCbShowTrendLine);
        setEdControlVisible(isScatter, edSeriesTransparencyContainer);

        setEdControlVisible(isBar, edBarWidthContainer);
        setEdControlVisible(isBar, edStackingModeContainer);

        setEdControlVisible(isPie, edCbSliceLabels);

        setEdControlVisible(isHeatmap, edCbShowAxisLabels);
    }

    private ChartConfig.ChartType currentEditorType() {
        if (chartTypeGroup == null || chartTypeGroup.getSelectedToggle() == null)
            return ChartConfig.ChartType.LINE;
        try {
            return ChartConfig.ChartType.valueOf(
                    chartTypeGroup.getSelectedToggle().getUserData().toString());
        } catch (Exception e) {
            return ChartConfig.ChartType.LINE;
        }
    }

    @FXML void onSaveProject(ActionEvent e)  { presenter.onSaveProjectClicked(); }
    @FXML void onOpenProject(ActionEvent e)  { presenter.onOpenProjectClicked(); }

    @FXML void onExportDashboard(ActionEvent e) {
        if (exportFormatGroup.getSelectedToggle() == null) return;
        presenter.onExportClicked(exportFormatGroup.getSelectedToggle().getUserData().toString());
    }

    @FXML void onShowImportDrawer(ActionEvent e) {
        switchLeftDrawer(importDrawer, datasetDrawer);
        clearImportError();
    }
    @FXML void onHideImportDrawer(ActionEvent e) { switchLeftDrawer(datasetDrawer, importDrawer); }

    @FXML void onImportSourceChanged(ActionEvent e) {
        boolean isFile = rbFile.isSelected();
        importFilePanel.setVisible(isFile);  importFilePanel.setManaged(isFile);
        importJdbcPanel.setVisible(!isFile); importJdbcPanel.setManaged(!isFile);
        updateImportButtonState();
    }

    @FXML void onBrowseImportFile(ActionEvent e) {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle(t("Choose data file", "Оберіть файл даних"));
        chooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter(t("Data files", "Файли даних"),
                        "*.csv", "*.tsv", "*.json", "*.ndjson", "*.xlsx"),
                new javafx.stage.FileChooser.ExtensionFilter(t("All files", "Усі файли"), "*.*"));
        java.io.File file = chooser.showOpenDialog(importFilePath.getScene().getWindow());
        if (file != null) {
            importFilePath.setText(file.getAbsolutePath());
            if (importDatasetName.getText().isBlank())
                importDatasetName.setText(file.getName().replaceFirst("[.][^.]+$", ""));
            boolean csv = file.getName().endsWith(".csv") || file.getName().endsWith(".tsv");
            csvOptionsPanel.setVisible(csv); csvOptionsPanel.setManaged(csv);
            updateImportButtonState();
        }
    }

    @FXML void onImportDragOver(javafx.scene.input.DragEvent e) {
        if (e.getDragboard().hasFiles()) e.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
        e.consume();
    }

    @FXML void onImportDragDropped(javafx.scene.input.DragEvent e) {
        var db = e.getDragboard();
        if (db.hasFiles() && !db.getFiles().isEmpty()) {
            java.io.File file = db.getFiles().get(0);
            importFilePath.setText(file.getAbsolutePath());
            if (importDatasetName.getText().isBlank())
                importDatasetName.setText(file.getName().replaceFirst("[.][^.]+$", ""));
            boolean csv = file.getName().endsWith(".csv") || file.getName().endsWith(".tsv");
            csvOptionsPanel.setVisible(csv); csvOptionsPanel.setManaged(csv);
            updateImportButtonState();
        }
        e.setDropCompleted(true); e.consume();
    }

    @FXML void onJdbcDriverChanged(ActionEvent e) {
        if (jdbcDriverCombo.getValue() == null) return;
        boolean sqlite = "SQLite".equals(jdbcDriverCombo.getValue());
        jdbcHostField.setDisable(sqlite); jdbcPortField.setDisable(sqlite);
        jdbcPortField.setPromptText("MySQL".equals(jdbcDriverCombo.getValue()) ? "3306" : "5432");
    }

    @FXML void onTestJdbcConnection(ActionEvent e) {
        jdbcStatusLabel.setText(t("Testing…", "Тестування…"));
        Thread.ofVirtual().start(() -> {
            boolean ok = testJdbc();
            Platform.runLater(() -> jdbcStatusLabel.setText(ok
                    ? t("✓ Connected", "✓ Підключено")
                    : t("✗ Failed", "✗ Помилка")));
        });
    }

    @FXML void onDoImport(ActionEvent e) {
        clearImportError();
        java.nio.file.Path path = getImportPath();
        if (path == null) {
            showImportError(t("Please choose a file or configure JDBC.", "Оберіть файл або налаштуйте JDBC."));
            return;
        }
        presenter.startImport(path, new ImportViewAdapter(this));
    }

    @FXML void onDatasetSelected(MouseEvent e) {
        String name = datasetList.getSelectionModel().getSelectedItem();
        if (name != null) presenter.onDatasetSelected(name);
    }

    @FXML void onDatasetSearch(javafx.scene.input.KeyEvent e) {
        filterDatasets(datasetSearchField == null ? "" : datasetSearchField.getText());
    }

    @FXML void onLoadExample(ActionEvent e) {
        setStatus(t("Load example is not implemented yet.", "Завантаження прикладу не реалізовано."));
    }

    @FXML void onSetGridView(ActionEvent e) {
        chartContainer.getStyleClass().remove("list-view");
        if (!chartContainer.getStyleClass().contains("grid-view"))
            chartContainer.getStyleClass().add("grid-view");
    }
    @FXML void onSetListView(ActionEvent e) {
        chartContainer.getStyleClass().remove("grid-view");
        if (!chartContainer.getStyleClass().contains("list-view"))
            chartContainer.getStyleClass().add("list-view");
    }

    @FXML void onApplyFilter(ActionEvent e) { presenter.onApplyFilterClicked(collectFilterCriteria()); }
    @FXML void onResetFilter(ActionEvent e) { presenter.onResetFilterClicked(); }

    @FXML void onAddChart(ActionEvent e)   { presenter.onAddChartClicked("LINE"); }
    @FXML void onToggleGrid(ActionEvent e) {
        if (gridToggle != null) presenter.onToggleGrid(gridToggle.isSelected());
    }

    @FXML void onOpenFullEditor(ActionEvent e) {
        try {
            boolean isOpen = chartEditorPane != null && chartEditorPane.isVisible();
            System.out.println("[DashboardView] onOpenFullEditor called, isOpen=" + isOpen);
            if (isOpen) hideInlineEditor(); else showInlineEditor();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void showInlineEditor() {
        if (chartEditorPane != null) {
            if (centerSplit != null && !centerSplit.getItems().contains(chartEditorPane)) {
                int idx = Math.min(centerSplit.getItems().size(), 1);
                centerSplit.setMinWidth(0);
                chartEditorPane.setMinWidth(0);
                centerSplit.getItems().add(idx, chartEditorPane);
            }
            chartEditorPane.setVisible(true);
            chartEditorPane.setManaged(true);
        }
        if (centerSplit != null && !centerSplit.getDividers().isEmpty()) {
            centerSplit.getDividers().get(0).setPosition(1.0);
            updateCardTransforms(1.0);
        }
        if (chartEditorPane != null) {
            chartEditorPane.setOpacity(0.0);
            chartEditorPane.setScaleX(1.0);
            chartEditorPane.setScaleY(0.97);
            chartEditorPane.setTranslateX(0);
            Timeline fadeIn = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(chartEditorPane.opacityProperty(),  0.0),
                    new KeyValue(chartEditorPane.scaleYProperty(),   0.97)),
                new KeyFrame(Duration.millis(300),
                    new KeyValue(chartEditorPane.opacityProperty(),  1.0, Interpolator.EASE_OUT),
                    new KeyValue(chartEditorPane.scaleYProperty(),   1.0, Interpolator.EASE_OUT))
            );
            fadeIn.play();
        }
        animateDividerTo(0.58, null);
        if (btnOpenFullEditor != null) btnOpenFullEditor.setText(t("Hide editor", "Приховати редактор"));

        if (presenter != null) {
            var ds  = presenter.getCurrentDataSet();
            var cfg = presenter.getSelectedConfig();
            System.out.println("[DashboardView] showInlineEditor: dataset="
                    + (ds == null ? "null" : ds.getRowCount() + " rows")
                    + ", config=" + (cfg == null ? "null" : cfg.getTitle()));
            if (ds != null) {
                populateColumnSelectors(ds.getColumns().stream().map(c -> c.getName()).toList());
                if (cfg != null) {
                    populateEditorWithConfig(cfg);
                    populateEditorStats(ds, cfg);
                } else {
                    showEditorNoSelection();
                    setStatus(t("No chart selected — click a chart card to edit.",
                            "Діаграму не вибрано — натисніть картку, щоб редагувати."));
                }
            } else {
                showEditorNoSelection();
                setStatus(t("No dataset loaded — import data first.",
                        "Набір даних не завантажено — спочатку імпортуйте дані."));
            }
        }
    }

    private void hideInlineEditor() {
        if (centerSplit == null) {
            if (chartEditorPane != null) { chartEditorPane.setVisible(false); chartEditorPane.setManaged(false); }
            if (btnOpenFullEditor != null) btnOpenFullEditor.setText(t("Show editor", "Повний редактор"));
            return;
        }
        if (!centerSplit.getDividers().isEmpty()) {
            if (chartEditorPane != null) {
                Timeline fadeOut = new Timeline(
                    new KeyFrame(Duration.ZERO,
                        new KeyValue(chartEditorPane.opacityProperty(),  1.0),
                        new KeyValue(chartEditorPane.scaleYProperty(),   1.0)),
                    new KeyFrame(Duration.millis(220),
                        new KeyValue(chartEditorPane.opacityProperty(),  0.0, Interpolator.EASE_IN),
                        new KeyValue(chartEditorPane.scaleYProperty(),   0.97, Interpolator.EASE_IN))
                );
                fadeOut.play();
            }
            Runnable remover = () -> {
                if (chartEditorPane != null) {
                    chartEditorPane.setVisible(false);
                    chartEditorPane.setManaged(false);
                    chartEditorPane.setOpacity(1.0);
                    chartEditorPane.setScaleY(1.0);
                    chartEditorPane.setTranslateX(0);
                }
                if (centerSplit.getItems().contains(chartEditorPane))
                    centerSplit.getItems().remove(chartEditorPane);
            };
            animateDividerTo(1.0, remover);
        } else {
            if (chartEditorPane != null) { chartEditorPane.setVisible(false); chartEditorPane.setManaged(false); }
            if (centerSplit.getItems().contains(chartEditorPane))
                centerSplit.getItems().remove(chartEditorPane);
        }
        if (btnOpenFullEditor != null) btnOpenFullEditor.setText(t("Show editor", "Повний редактор"));
    }

    private void animateDividerTo(double target, Runnable onFinished) {
        if (centerSplit == null) {
            if (onFinished != null) onFinished.run();
            return;
        }
        var divs = centerSplit.getDividers();
        if (divs.isEmpty()) {
            updateCardTransforms(target);
            if (onFinished != null) onFinished.run();
            return;
        }
        var divider = divs.get(0);
        Interpolator interp = (target < divider.getPosition())
                ? Interpolator.EASE_OUT
                : Interpolator.EASE_IN;
        Timeline tl = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(divider.positionProperty(), divider.getPosition())),
            new KeyFrame(Duration.millis(350),
                new KeyValue(divider.positionProperty(), target, interp))
        );
        tl.setOnFinished(e -> { if (onFinished != null) onFinished.run(); });
        tl.play();
    }

    @FXML void onEditorTabChart(ActionEvent e) { switchEditorTab(editorChartScroll); }
    @FXML void onEditorTabStats(ActionEvent e) { switchEditorTab(editorStatsScroll); }

    @FXML void onChartTypeChanged(ActionEvent e) {
        if (updatingEditor || chartTypeGroup.getSelectedToggle() == null) return;
        Object ud = chartTypeGroup.getSelectedToggle().getUserData();
        if (ud == null) return;
        try {
            ChartConfig.ChartType type = ChartConfig.ChartType.valueOf(ud.toString());
            updateEditorVisibleSettings(type);
        } catch (IllegalArgumentException ignored) {}
        presenter.onEditorChartTypeChanged(ud.toString());
    }

    @FXML void onEdTitleChanged(javafx.scene.input.KeyEvent e) {
        presenter.onEditorTitleChanged(edChartTitle.getText());
        if (selectedCardId != null) {
            VBox card = cardNodes.get(selectedCardId);
            if (card != null) {
                Label lbl = (Label) card.lookup(".chart-card-title");
                if (lbl != null) lbl.setText(edChartTitle.getText());
            }
        }
    }

    @FXML void onEdXColumnChanged(ActionEvent e) {
        if (!updatingEditor && edXColumn.getValue() != null)
            presenter.onEditorXColumnChanged(edXColumn.getValue());
    }

    @FXML void onEdAddYColumn(ActionEvent e) {
        String sel = edAddSeriesCombo.getValue();
        if (sel != null && !sel.isBlank()) {
            presenter.onEditorAddYColumn(sel);
            if (!edYColumns.getItems().contains(sel)) edYColumns.getItems().add(sel);
            edAddSeriesCombo.getSelectionModel().clearSelection();
        }
    }

    @FXML void onEdRemoveYColumn(ActionEvent e) {
        String sel = edYColumns.getSelectionModel().getSelectedItem();
        if (sel != null) {
            edYColumns.getSelectionModel().clearSelection();
            presenter.onEditorRemoveYColumn(sel);
            edYColumns.getItems().remove(sel);
        }
    }

    @FXML void onEdStyleOptionChanged(Event e) {
        if (updatingEditor) return;
        presenter.onEditorStyleOptionChanged(
                edShowLegend.isSelected(), edShowGrid.isSelected(),
                edShowTooltips.isSelected(), edLineWidth.getValue());
    }

    @FXML void onEdExportClicked(ActionEvent e) {
        if (exportFormatGroup.getSelectedToggle() == null) return;
        presenter.onEditorExportClicked(
                exportFormatGroup.getSelectedToggle().getUserData().toString());
    }

    @FXML void onEdApply(ActionEvent e)  { presenter.onEditorApply(); }
    @FXML void onEdRevert(ActionEvent e) { presenter.onEditorRevert(); }

    public void setStatus(String text) { if (statusLabel != null) statusLabel.setText(text); }

    public void updateDatasetInfo(String name, long rows, int cols) {
        currentDatasetName = name;
        currentDatasetRows = rows;
        currentDatasetCols = cols;
        datasetInfoLabel.setText("%s · %,d %s".formatted(name, rows,
                t("rows", "рядків")));
        rowCountLabel.setText("%,d %s · %d %s".formatted(rows,
                t("rows", "рядків"), cols, t("cols", "стовпців")));
        disableFilterButtons(false);
    }

    public void addDatasetToList(String name) {
        if (!allDatasets.contains(name)) allDatasets.add(name);
        filterDatasets(datasetSearchField == null ? "" : datasetSearchField.getText());
        datasetList.getSelectionModel().select(name);
    }

    public void setDashboardVisible(boolean visible) {
        emptyState.setVisible(!visible); emptyState.setManaged(!visible);
        canvasPane.setVisible(visible);  canvasPane.setManaged(visible);
    }

    public void clearCharts() {
        chartPanels.values().forEach(p -> {
            if (p.getFxNode() != null) p.getContainer().getChildren().clear();
        });
        chartContainer.getChildren().clear();
        chartPanels.clear();
        cardNodes.clear();
        selectedCardId = null;
        showEditorNoSelection();
    }

    public String addChartPanel(ChartRenderResult result) {
        if (result == null) return null;
        ChartConfig config  = result.getConfig();
        String      panelId = config.getId() != null
                ? config.getId() : UUID.randomUUID().toString();

        ChartFxChartPanel chartPanel = new ChartFxChartPanel(panelId, result);
        chartPanels.put(panelId, chartPanel);

        Label titleLbl = new Label(config.getTitle());
        titleLbl.getStyleClass().add("chart-card-title");

        Label metaLbl = new Label("X: %s · Y: %s".formatted(
                config.getXColumn(), String.join(", ", config.getYColumns())));
        metaLbl.getStyleClass().add("chart-card-meta");

        HBox legendStrip = buildCardLegendStrip(config);

        Label badgeLbl = new Label(config.getChartType().name().toLowerCase());
        badgeLbl.getStyleClass().addAll("chart-type-badge",
                "badge-" + config.getChartType().name().toLowerCase());

        VBox titleBlock = new VBox(2, titleLbl, metaLbl);
        if (legendStrip != null) titleBlock.getChildren().add(legendStrip);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(6, titleBlock, headerSpacer, badgeLbl);
        header.getStyleClass().add("chart-card-header");

        VBox chartContainer_ = chartPanel.getContainer();
        VBox.setVgrow(chartContainer_, Priority.ALWAYS);

        Button btnEdit      = new Button("✎ " + t("Edit", "Редагувати"));
        btnEdit.getStyleClass().addAll("card-mini-btn", "edit-active");
        Button btnDuplicate = new Button("⧉ " + t("Duplicate", "Дублювати"));
        btnDuplicate.getStyleClass().add("card-mini-btn");
        Button btnRemove    = new Button("✕");
        btnRemove.getStyleClass().add("card-mini-btn");
        btnRemove.setStyle("-fx-text-fill: -accent3;");
        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        HBox footer = new HBox(2, btnEdit, btnDuplicate, footerSpacer, btnRemove);
        footer.getStyleClass().add("chart-card-footer");

        VBox card = new VBox(header, chartContainer_, footer);
        card.getStyleClass().add("chart-card");
        card.setMinWidth(240);
        card.setMinHeight(180);
        javafx.beans.binding.DoubleBinding safeW =
                javafx.beans.binding.Bindings.min(
                        chartContainer.widthProperty().subtract(56), 900.0);
        card.prefWidthProperty().bind(safeW);
        card.maxWidthProperty().bind(safeW);
        card.prefHeightProperty().bind(
                javafx.beans.binding.Bindings.min(safeW.multiply(0.58), 440.0));
        card.setMaxHeight(Region.USE_PREF_SIZE);
        applyCardStyle(card, false, config.getChartType());
        card.setUserData(panelId);
        cardNodes.put(panelId, card);

        card.setOnMouseClicked(ev -> selectCard(panelId, chartPanel.getCurrentConfig()));
        btnEdit.setOnAction(ev -> { ev.consume(); selectCard(panelId, chartPanel.getCurrentConfig()); });
        btnDuplicate.setOnAction(ev -> { ev.consume(); presenter.onDuplicateChart(panelId); });
        btnRemove.setOnAction(ev -> { ev.consume(); removeCard(panelId); });

        javafx.scene.shape.Rectangle cardClip = new javafx.scene.shape.Rectangle();
        cardClip.setArcWidth(40);
        cardClip.setArcHeight(40);
        cardClip.widthProperty().bind(card.widthProperty());
        cardClip.heightProperty().bind(card.heightProperty());
        card.setClip(cardClip);

        this.chartContainer.getChildren().add(card);

        card.setOpacity(0.0);
        card.setScaleX(0.92);
        card.setScaleY(0.92);
        card.setTranslateY(16);
        double targetScale = 1.0;
        if (centerSplit != null && !centerSplit.getDividers().isEmpty()) {
            double pos = centerSplit.getDividers().get(0).getPosition();
            double min = 0.58; double max = 1.0;
            double frac = (pos - min) / (max - min);
            if (frac < 0) frac = 0; if (frac > 1) frac = 1;
            targetScale = 0.87 + 0.13 * frac;
        }
        Timeline entrance = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(card.opacityProperty(),    0.0),
                new KeyValue(card.scaleXProperty(),     0.92),
                new KeyValue(card.scaleYProperty(),     0.92),
                new KeyValue(card.translateYProperty(), 16.0)
            ),
            new KeyFrame(Duration.millis(340),
                new KeyValue(card.opacityProperty(),    1.0,  Interpolator.EASE_OUT),
                new KeyValue(card.scaleXProperty(),     targetScale,  Interpolator.EASE_OUT),
                new KeyValue(card.scaleYProperty(),     targetScale,  Interpolator.EASE_OUT),
                new KeyValue(card.translateYProperty(), 0.0,  Interpolator.EASE_OUT)
            )
        );
        entrance.play();
        Platform.runLater(() -> {
            try {
                chartContainer.applyCss();
                chartContainer.requestLayout();
                card.applyCss();
                card.requestLayout();
            } catch (Exception ignored) { }
        });
        return panelId;
    }

    public void updateChartCard(String panelId, ChartRenderResult result) {
        ChartFxChartPanel panel = chartPanels.get(panelId);
        VBox card = cardNodes.get(panelId);
        if (panel == null || result == null) return;
        panel.updateConfig(result);
        if (card != null) {
            Label badge = (Label) card.lookup(".chart-type-badge");
            if (badge != null) badge.setText(result.getConfig().getChartType().name().toLowerCase());
            Label meta  = (Label) card.lookup(".chart-card-meta");
            if (meta != null) meta.setText("X: %s · Y: %s".formatted(
                    result.getConfig().getXColumn(),
                    String.join(", ", result.getConfig().getYColumns())));
            Label title = (Label) card.lookup(".chart-card-title");
            if (title != null) title.setText(result.getConfig().getTitle());
            javafx.scene.Node headerNode = card.lookup(".chart-card-header");
            if (headerNode instanceof HBox header && !header.getChildren().isEmpty()) {
                javafx.scene.Node titleNode = header.getChildren().get(0);
                if (titleNode instanceof VBox titleBlock) {
                    titleBlock.getChildren().removeIf(node ->
                            node instanceof HBox hbox && hbox.getStyleClass().contains("chart-legend-strip"));
                    HBox legendStrip = buildCardLegendStrip(result.getConfig());
                    if (legendStrip != null) titleBlock.getChildren().add(legendStrip);
                }
            }
            boolean isSelected = panelId.equals(selectedCardId);
            applyCardStyle(card, isSelected, result.getConfig().getChartType());
        }
    }

    public void applyFilterToAllCharts(List<Integer> activeIndices) {
        chartPanels.values().forEach(p -> p.applyFilter(activeIndices));
    }

    public void setLoading(boolean loading, String message) {
        loadingBox.setVisible(loading); loadingBox.setManaged(loading);
        if (loading) loadingLabel.setText(message);
    }

    public void setMemoryLabel(String text)   { if (memoryLabel != null) memoryLabel.setText(text); }
    public void addRecentProject(String path) {}

    public void setGridVisible(boolean on) {
        chartPanels.values().forEach(p -> {
            if (p.getFxChart() instanceof de.gsi.chart.XYChart xyChart) {
                xyChart.horizontalGridLinesVisibleProperty().set(on);
                xyChart.verticalGridLinesVisibleProperty().set(on);
            }
        });
    }

    public void toggleDataPanel() {
        if (leftStack.getScene() == null) return;
        javafx.scene.Parent root = leftStack.getScene().getRoot();
        if (root instanceof BorderPane bp) {
            javafx.scene.Node left = bp.getLeft();
            if (left != null) { left.setVisible(!left.isVisible()); left.setManaged(!left.isManaged()); }
        }
    }

    public void toggleFilterPanel() {
        filterContainer.setVisible(!filterContainer.isVisible());
        filterContainer.setManaged(!filterContainer.isManaged());
    }

    public DashboardSnapshot createSnapshot() {
        if (chartPanels.isEmpty()) return null;
        List<DashboardSnapshot> children = chartPanels.values().stream()
                .map(ChartFxChartPanel::toDomainSnapshot).toList();
        double w = chartPanels.values().stream().mapToDouble(ChartFxChartPanel::getWidth).sum();
        double h = chartPanels.values().stream().mapToDouble(ChartFxChartPanel::getHeight).max().orElse(0);
        byte[] rootPng = children.stream()
                .map(DashboardSnapshot::getPngBytes)
                .filter(b -> b != null && b.length > 0)
                .findFirst().orElse(new byte[0]);
        return new DashboardSnapshot("dashboard", "Dashboard", w, h, rootPng, children);
    }

    public void buildFilterPanel(DataSet ds) {
        filterContainer.getChildren().clear();
        filterControls.clear();
        filterColumnTypes.clear();
        if (ds == null || ds.getColumns().isEmpty()) return;

        ds.getColumns().forEach(col -> {
            VBox row = new VBox(3);
            row.getStyleClass().add("filter-row-box");
            row.setStyle("-fx-padding: 6 10 8 10;");
            Label colLabel = new Label(col.getName());
            colLabel.getStyleClass().add("filter-col-label");
            row.getChildren().add(colLabel);
            filterColumnTypes.put(col.getName(), col.getType());

            if (col.getType() == ColumnType.NUMERIC) {
                double min = col.getMin() != null ? col.getMin() : 0;
                double max = col.getMax() != null ? col.getMax() : 100;
                Slider slider = new Slider(min, max, min);
                slider.setShowTickMarks(true);
                guardSliderFromStrayDrags(slider);
                slider.setStyle("-fx-accent: -accent;");
                Label valLbl = new Label("≥ %.1f".formatted(min));
                valLbl.getStyleClass().add("muted-label");
                slider.valueProperty().addListener((o, ov, nv) -> {
                    valLbl.setText("≥ %.1f".formatted(nv.doubleValue()));
                    updateActiveFilterCount();
                });
                row.getChildren().addAll(slider, valLbl);
                filterControls.put(col.getName(), slider);
            } else {
                ComboBox<String> combo = new ComboBox<>();
                combo.getStyleClass().add("compact-combo");
                combo.setMaxWidth(Double.MAX_VALUE);
                combo.getItems().add("All");
                col.getValues().stream()
                        .filter(Objects::nonNull).map(Object::toString)
                        .distinct().limit(200).forEach(combo.getItems()::add);
                combo.getSelectionModel().selectFirst();
                combo.setOnAction(ev -> updateActiveFilterCount());
                row.getChildren().add(combo);
                filterControls.put(col.getName(), combo);
            }
            filterContainer.getChildren().add(row);
        });
        updateActiveFilterCount();
    }

    @SuppressWarnings("unchecked")
    public List<FilterCriteria> collectFilterCriteria() {
        List<FilterCriteria> result = new ArrayList<>();
        for (Map.Entry<String, Control> entry : filterControls.entrySet()) {
            String     colName = entry.getKey();
            Control    control = entry.getValue();
            ColumnType type    = filterColumnTypes.getOrDefault(colName, ColumnType.CATEGORICAL);
            if (type == ColumnType.NUMERIC && control instanceof Slider slider) {
                if (slider.getValue() > slider.getMin())
                    result.add(FilterCriteria.numericRange(colName, slider.getValue(), slider.getMax()));
            } else if (control instanceof ComboBox<?> combo) {
                String selected = combo.getValue() == null ? null : combo.getValue().toString();
                if (selected != null && !"All".equals(selected)) {
                    if (type == ColumnType.BOOLEAN)
                        result.add(FilterCriteria.booleanEquals(colName, Boolean.parseBoolean(selected)));
                    else
                        result.add(FilterCriteria.categoricalIn(colName, List.of(selected)));
                }
            }
        }
        return result;
    }

    public void setFilterStatus(String text) {
        filterStatusLabel.setText(text);
        filterStatusLabel.setVisible(!text.isBlank());
        filterStatusLabel.setManaged(!text.isBlank());
    }

    public void setActiveFilterCount(int count) {
        if (count > 0) {
            activeFilterCount.setText(count + " active");
            activeFilterCount.setVisible(true); activeFilterCount.setManaged(true);
        } else {
            activeFilterCount.setVisible(false); activeFilterCount.setManaged(false);
        }
    }

    public void populateEditorWithConfig(ChartConfig config) {
        System.out.println("[DashboardView] populateEditorWithConfig: config="
                + (config == null ? "null" : config.getTitle() + " / " + config.getChartType()));
        if (config == null) { showEditorNoSelection(); return; }

        updatingEditor = true;
        try {
            showEditorContent();

            if (chartTypeGroup != null) {
                chartTypeGroup.getToggles().forEach(t -> {
                    Object ud = t.getUserData();
                    if (ud != null && ud.toString().equals(config.getChartType().name()))
                        t.setSelected(true);
                });
            }

            if (edChartTitle != null) edChartTitle.setText(config.getTitle());
            String xCol = config.getXColumn();
            if (edXColumn != null) {
                if (xCol != null && !edXColumn.getItems().contains(xCol))
                    edXColumn.getItems().add(0, xCol);
                edXColumn.setValue(xCol);
            }
            if (edYColumns != null) {
                edYColumns.getSelectionModel().clearSelection();
                edYColumns.getItems().setAll(config.getYColumns());
            }
            if (edXLabel != null) edXLabel.setText(config.getXLabel() != null ? config.getXLabel() : "");
            if (edYLabel != null) edYLabel.setText(config.getYLabel() != null ? config.getYLabel() : "");

            ChartStyle style = config.getStyle();
            if (style != null) {
                if (edShowLegend   != null) edShowLegend.setSelected(style.isShowLegend());
                if (edShowGrid     != null) edShowGrid.setSelected(style.isShowGrid());
                if (edShowTooltips != null) edShowTooltips.setSelected(style.isShowTooltips());
                if (edLineWidth    != null) edLineWidth.setValue(style.getLineWidth());

                if (edCbSmoothing != null) edCbSmoothing.setSelected(style.isSmoothing());

                if (edPointSizeSlider          != null) edPointSizeSlider.setValue(style.getPointSize());
                if (edPointShapeCombo          != null) edPointShapeCombo.setValue(style.getPointShape());
                if (edCbShowTrendLine          != null) edCbShowTrendLine.setSelected(style.isShowTrendLine());
                if (edSeriesTransparencySlider != null) edSeriesTransparencySlider.setValue(style.getSeriesTransparency());

                if (edBarWidthSlider    != null) edBarWidthSlider.setValue(style.getBarWidth());
                if (edStackingModeCombo != null) edStackingModeCombo.setValue(style.getStackingMode());

                if (edCbSliceLabels       != null) edCbSliceLabels.setSelected(style.isSliceLabels());
                if (edLegendPositionCombo != null) edLegendPositionCombo.setValue(style.getLegendPosition());
                if (edCbDonutMode         != null) edCbDonutMode.setSelected(style.isDonutMode());
                if (edInnerRadiusSlider   != null) edInnerRadiusSlider.setValue(style.getInnerRadius());

                if (edColorScaleCombo    != null) edColorScaleCombo.setValue(style.getColorScale());
                if (edColorRangeMinField != null) edColorRangeMinField.setText(
                        style.getColorRangeMin() != null ? style.getColorRangeMin() : "");
                if (edColorRangeMaxField != null) edColorRangeMaxField.setText(
                        style.getColorRangeMax() != null ? style.getColorRangeMax() : "");
                if (edCbShowAxisLabels   != null) edCbShowAxisLabels.setSelected(style.isShowAxisLabels());
            }
        } finally {
            updatingEditor = false;
        }

        updateEditorVisibleSettings(config.getChartType());
    }

    public void populateEditorStats(DataSet ds, ChartConfig config) {
        if (edStatsContainer == null || ds == null) return;
        edStatsContainer.getChildren().clear();
        List<String> relevant = new ArrayList<>();
        if (config.getXColumn() != null) relevant.add(config.getXColumn());
        relevant.addAll(config.getYColumns());
        ds.getColumns().stream().filter(col -> relevant.contains(col.getName())).forEach(col -> {
            VBox card = new VBox(3);
            card.getStyleClass().add("ed-stats-card");
            card.setStyle("-fx-padding: 8 10 10 10;");
            Label nameL = new Label(col.getName());
            nameL.getStyleClass().add("ed-stat-col-name");
            card.getChildren().add(nameL);
            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(3);
            addStatRow(grid, 0, t("Rows", "Рядки"),  "%,d".formatted(ds.getRowCount()));
            addStatRow(grid, 1, t("Nulls", "Порожні"), "%,d".formatted(col.getNullCount()));
            if (col.getMin()  != null) addStatRow(grid, 2, t("Min", "Мін"),  "%.2f".formatted(col.getMin()));
            if (col.getMax()  != null) addStatRow(grid, 3, t("Max", "Макс"),  "%.2f".formatted(col.getMax()));
            if (col.getMean() != null) addStatRow(grid, 4, t("Mean", "Середнє"), "%.2f".formatted(col.getMean()));
            card.getChildren().add(grid);
            edStatsContainer.getChildren().add(card);
        });
    }

    public void populateColumnSelectors(List<String> columns) {
        updatingEditor = true;
        try {
            edXColumn.getItems().setAll(columns);
            edAddSeriesCombo.getItems().setAll(columns);
            if (!columns.isEmpty() && edXColumn.getValue() == null)
                edXColumn.setValue(columns.get(0));
        } finally {
            updatingEditor = false;
        }
    }

    public void showImportProgress(boolean visible) {
        importProgressSection.setVisible(visible); importProgressSection.setManaged(visible);
    }
    public void updateImportProgress(double value) {
        importProgressBar.setProgress(value);
        importProgressLabel.setText(t("Loading… %.0f%%".formatted(value * 100),
                "Завантаження… %.0f%%".formatted(value * 100)));
    }
    public void showImportError(String message) {
        importErrorLabel.setText(message);
        importErrorLabel.setVisible(true); importErrorLabel.setManaged(true);
    }
    public void clearImportError() {
        importErrorLabel.setVisible(false); importErrorLabel.setManaged(false);
    }
    public void onImportSuccess() {
        showImportProgress(false);
        switchLeftDrawer(datasetDrawer, importDrawer);
    }
    public void setImportButtonEnabled(boolean enabled) {
        if (btnDoImport != null) btnDoImport.setDisable(!enabled);
    }

    private void selectCard(String panelId, ChartConfig config) {
        System.out.println("[DashboardView] selectCard: panelId=" + panelId
                + ", config=" + (config == null ? "null" : config.getTitle()));
        if (selectedCardId != null) {
            VBox prev = cardNodes.get(selectedCardId);
            if (prev != null) {
                prev.getStyleClass().remove("selected-card");
                ChartFxChartPanel prevPanel = chartPanels.get(selectedCardId);
                ChartConfig.ChartType prevType = (prevPanel != null && prevPanel.getCurrentConfig() != null)
                        ? prevPanel.getCurrentConfig().getChartType() : null;
                applyCardStyle(prev, false, prevType);
            }
        }
        selectedCardId = panelId;
        VBox card = cardNodes.get(panelId);
        if (card != null) {
            if (!card.getStyleClass().contains("selected-card"))
                card.getStyleClass().add("selected-card");
            ChartFxChartPanel panel = chartPanels.get(panelId);
            ChartConfig.ChartType type = (panel != null && panel.getCurrentConfig() != null)
                    ? panel.getCurrentConfig().getChartType() : null;
            applyCardStyle(card, true, type);
        }
        populateEditorWithConfig(config);
        presenter.onEditorCardSelected(panelId, config);
    }

    private void removeCard(String panelId) {
        VBox card = cardNodes.remove(panelId);
        if (card != null) {
            Timeline exit = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(card.opacityProperty(),    1.0),
                    new KeyValue(card.scaleXProperty(),     card.getScaleX()),
                    new KeyValue(card.scaleYProperty(),     card.getScaleY())
                ),
                new KeyFrame(Duration.millis(220),
                    new KeyValue(card.opacityProperty(),    0.0,  Interpolator.EASE_IN),
                    new KeyValue(card.scaleXProperty(),     0.88, Interpolator.EASE_IN),
                    new KeyValue(card.scaleYProperty(),     0.88, Interpolator.EASE_IN),
                    new KeyValue(card.translateYProperty(), 8.0,  Interpolator.EASE_IN)
                )
            );
            VBox finalCard = card;
            exit.setOnFinished(e -> chartContainer.getChildren().remove(finalCard));
            exit.play();
        }
        ChartFxChartPanel panel = chartPanels.remove(panelId);
        if (panel != null) panel.getContainer().getChildren().clear();
        if (panelId.equals(selectedCardId)) {
            selectedCardId = null;
            showEditorNoSelection();
        }
    }

    private void showEditorNoSelection() {
        setEdControlVisible(true,  editorNoSelection);
        setEdControlVisible(false, editorChartScroll, editorStatsScroll, editorFooter);
    }

    private void showEditorContent() {
        setEdControlVisible(false, editorNoSelection, editorStatsScroll);
        setEdControlVisible(true,  editorChartScroll, editorFooter);
        if (edTabChart != null) edTabChart.setSelected(true);
    }

    private void switchEditorTab(ScrollPane show) {
        if (show == null) return;
        setEdControlVisible(false, editorChartScroll, editorStatsScroll);
        setEdControlVisible(true, show);
    }

    private void switchLeftDrawer(VBox show, VBox hide) {
        FadeTransition ftOut = new FadeTransition(Duration.millis(160), hide);
        ftOut.setFromValue(1.0); ftOut.setToValue(0.0);
        ftOut.setInterpolator(Interpolator.EASE_IN);
        TranslateTransition ttOut = new TranslateTransition(Duration.millis(160), hide);
        ttOut.setFromX(0); ttOut.setToX(-18);
        ttOut.setInterpolator(Interpolator.EASE_IN);
        ParallelTransition ptOut = new ParallelTransition(ftOut, ttOut);
        ptOut.setOnFinished(ev -> {
            hide.setVisible(false); hide.setManaged(false);
            hide.setTranslateX(0);  hide.setOpacity(1.0);
            show.setTranslateX(22); show.setOpacity(0.0);
            show.setVisible(true);  show.setManaged(true);
            FadeTransition ftIn = new FadeTransition(Duration.millis(200), show);
            ftIn.setFromValue(0.0); ftIn.setToValue(1.0);
            ftIn.setInterpolator(Interpolator.EASE_OUT);
            TranslateTransition ttIn = new TranslateTransition(Duration.millis(220), show);
            ttIn.setFromX(22); ttIn.setToX(0);
            ttIn.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(ftIn, ttIn).play();
        });
        ptOut.play();
    }

    private void disableFilterButtons(boolean disable) {
        btnApplyFilter.setDisable(disable);
        btnResetFilter.setDisable(disable);
    }

    private void filterDatasets(String query) {
        String term = query == null ? "" : query.trim().toLowerCase();
        datasetList.getItems().setAll(allDatasets.stream()
                .filter(name -> term.isBlank() || name.toLowerCase().contains(term)).toList());
    }

    private void updateActiveFilterCount() {
        long active = filterControls.entrySet().stream().filter(e -> {
            Control    c = e.getValue();
            ColumnType t = filterColumnTypes.getOrDefault(e.getKey(), ColumnType.CATEGORICAL);
            if (t == ColumnType.NUMERIC && c instanceof Slider s) return s.getValue() > s.getMin();
            if (c instanceof ComboBox<?> cb)
                return cb.getValue() != null && !"All".equals(cb.getValue().toString());
            return false;
        }).count();
        setActiveFilterCount((int) active);
    }

    private void updateImportButtonState() {
        boolean ok = rbFile.isSelected()
                ? !importFilePath.getText().isBlank()
                : jdbcHostField != null && !jdbcHostField.getText().isBlank()
                && jdbcQueryArea != null && !jdbcQueryArea.getText().isBlank();
        btnDoImport.setDisable(!ok);
    }

    private java.nio.file.Path getImportPath() {
        if (rbFile.isSelected()) {
            String t = importFilePath.getText();
            return t.isBlank() ? null : java.nio.file.Path.of(t);
        }
        if (jdbcHostField == null || jdbcHostField.getText().isBlank()) return null;
        String drv  = jdbcDriverCombo.getValue() != null ? jdbcDriverCombo.getValue().toLowerCase() : "postgresql";
        String host = jdbcHostField.getText(), port = jdbcPortField.getText();
        String db   = jdbcDbField.getText(),   user = jdbcUserField.getText();
        String pass = jdbcPassField != null ? jdbcPassField.getText() : "";
        String qry  = jdbcQueryArea != null ? jdbcQueryArea.getText() : "";
        return java.nio.file.Path.of("jdbc:%s://%s:%s/%s?user=%s&password=%s&query=%s"
                .formatted(drv, host, port, db, user, pass, qry));
    }

    private boolean testJdbc() {
        try {
            java.nio.file.Path p = getImportPath();
            if (p == null) return false;
            String url = p.toString();
            int qi = url.indexOf("&query=");
            String cu = qi > 0 ? url.substring(0, qi) : url;
            if (!cu.startsWith("jdbc:")) cu = "jdbc:" + cu;
            try (java.sql.Connection c = java.sql.DriverManager.getConnection(cu)) {
                return c.isValid(3);
            }
        } catch (Exception e) { return false; }
    }

    private static String chartTypeAccentColor(ChartConfig.ChartType type) {
        if (type == null) return "#6366F1";
        return switch (type) {
            case LINE    -> "#3B82F6";
            case BAR     -> "#10B981";
            case AREA    -> "#8B5CF6";
            case PIE     -> "#F59E0B";
            case SCATTER -> "#EF4444";
            case HEATMAP -> "#EC4899";
        };
    }

    private static void applyCardStyle(VBox card, boolean selected,
                                       ChartConfig.ChartType type) {
        if (selected) {
            card.setStyle(
                "-fx-border-color: #2563EB;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 14;" +
                "-fx-background-radius: 14;" +
                "-fx-background-insets: 0;" +
                "-fx-border-insets: 0;" +
                "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.25), 16, 0, 0, 4);"
            );
        } else {
            String accent = chartTypeAccentColor(type);
            card.setStyle(
                "-fx-border-color: " + accent + " rgba(148,163,200,0.55) " +
                                       "rgba(148,163,200,0.55) rgba(148,163,200,0.55);" +
                "-fx-border-width: 3 1.5 1.5 1.5;" +
                "-fx-border-radius: 14;" +
                "-fx-background-radius: 14;" +
                "-fx-background-insets: 0;" +
                "-fx-border-insets: 0;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.09), 10, 0, 0, 3);"
            );
        }
    }

    private static HBox buildCardLegendStrip(ChartConfig config) {
        if (config.getYColumns().isEmpty()) return null;
        if (config.getStyle() != null && !config.getStyle().isShowLegend()) return null;

        List<String> palette = config.getStyle() != null
                && !config.getStyle().getSeriesColors().isEmpty()
                ? config.getStyle().getSeriesColors()
                : List.of("#1a56db","#0d7377","#c0392b","#b45309",
                          "#6d28d9","#0891b2","#15803d","#9a3412");

        HBox strip = new HBox(10);
        strip.getStyleClass().add("chart-legend-strip");
        strip.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        if (config.getChartType() == ChartConfig.ChartType.PIE) {
            Label dot = new Label("◑");
            dot.setStyle("-fx-text-fill: #c0392b; -fx-font-size:10px;");
            Label lbl = new Label(config.getYColumns().get(0) + " by " + config.getXColumn());
            lbl.getStyleClass().add("legend-series-label");
            strip.getChildren().addAll(dot, lbl);
        } else {
            int maxVisible = 6;
            for (int i = 0; i < Math.min(config.getYColumns().size(), maxVisible); i++) {
                String color = i < palette.size() ? palette.get(i) : "#888888";
                Region swatch = new Region();
                swatch.setMinSize(9, 9);
                swatch.setMaxSize(9, 9);
                swatch.setStyle("-fx-background-color:" + color
                        + "; -fx-background-radius:2px;");
                Label lbl = new Label(config.getYColumns().get(i));
                lbl.getStyleClass().add("legend-series-label");
                lbl.setTextOverrun(OverrunStyle.ELLIPSIS);
                lbl.setMaxWidth(120);
                HBox item = new HBox(5, swatch, lbl);
                item.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                strip.getChildren().add(item);
            }
            if (config.getYColumns().size() > maxVisible) {
                Label more = new Label("+" + (config.getYColumns().size() - maxVisible));
                more.getStyleClass().add("legend-series-label");
                more.setStyle("-fx-text-fill: #a0a09b;");
                strip.getChildren().add(more);
            }
        }
        return strip;
    }

    private void setEdControlVisible(boolean visible, Region... controls) {
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

    private static void addStatRow(GridPane g, int row, String label, String value) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("ed-stat-label");
        Label val = new Label(value);
        val.setStyle("-fx-font-size:12px; -fx-text-fill:-text0;"
                + " -fx-font-family:'Courier New',monospace;");
        g.add(lbl, 0, row);
        g.add(val, 1, row);
    }
}