package com.dataviz.ui.view;

import com.dataviz.domain.chart.ChartConfig;
import com.dataviz.domain.dashboard.DashboardSnapshot;
import com.dataviz.domain.filter.FilterCriteria;
import com.dataviz.domain.model.ColumnType;
import com.dataviz.domain.model.DataSet;
import com.dataviz.service.chart.ChartService.ChartRenderResult;
import com.dataviz.ui.ServiceLocatorHolder;
import com.dataviz.ui.chart.ChartFxChartPanel;
import com.dataviz.ui.presenter.DashboardPresenter;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.*;

public class DashboardView {

    private boolean updatingEditor = false;

    @FXML private ToggleGroup   mainTabGroup;
    @FXML private ToggleButton  tabDashboard;
    @FXML private HBox          loadingBox;
    @FXML private ProgressIndicator progressSpinner;
    @FXML private Label         loadingLabel;
    @FXML private Label         datasetInfoLabel;

    @FXML private StackPane     leftStack;
    @FXML private VBox          datasetDrawer;
    @FXML private ListView<String> datasetList;
    @FXML private Label         activeFilterCount;
    @FXML private VBox          filterContainer;
    @FXML private Button        btnApplyFilter;
    @FXML private Button        btnResetFilter;

    @FXML private VBox          importDrawer;
    @FXML private ToggleGroup   importSourceGroup;
    @FXML private RadioButton   rbFile;
    @FXML private RadioButton   rbJdbc;
    @FXML private VBox          importFilePanel;
    @FXML private TextField     importFilePath;
    @FXML private VBox          importDropZone;
    @FXML private VBox          csvOptionsPanel;
    @FXML private ToggleGroup   delimiterGroup;
    @FXML private CheckBox      cbHeaderRow;
    @FXML private ComboBox<String> encodingCombo;
    @FXML private VBox          importJdbcPanel;
    @FXML private ComboBox<String> jdbcDriverCombo;
    @FXML private TextField     jdbcHostField;
    @FXML private TextField     jdbcPortField;
    @FXML private TextField     jdbcDbField;
    @FXML private TextField     jdbcUserField;
    @FXML private PasswordField jdbcPassField;
    @FXML private TextArea      jdbcQueryArea;
    @FXML private Label         jdbcStatusLabel;
    @FXML private TextField     importDatasetName;
    @FXML private VBox          importProgressSection;
    @FXML private Label         importProgressLabel;
    @FXML private ProgressBar   importProgressBar;
    @FXML private Label         importErrorLabel;
    @FXML private Button        btnDoImport;

    @FXML private VBox          emptyState;
    @FXML private VBox          canvasPane;
    @FXML private Button        btnAddChart;
    @FXML private CheckBox      gridToggle;
    @FXML private Label         filterStatusLabel;
    @FXML private FlowPane      chartContainer;

    @FXML private VBox          chartEditorPane;
    @FXML private ToggleGroup   editorTabGroup;
    @FXML private ToggleButton  edTabChart;
    @FXML private VBox          editorNoSelection;
    @FXML private ScrollPane    editorChartScroll;
    @FXML private ScrollPane    editorStyleScroll;
    @FXML private ScrollPane    editorStatsScroll;
    @FXML private VBox          edStatsContainer;
    @FXML private HBox          editorFooter;

    @FXML private ToggleGroup   chartTypeGroup;
    @FXML private TextField     edChartTitle;
    @FXML private ComboBox<String> edXColumn;
    @FXML private TextField     edXLabel;
    @FXML private TextField     edYLabel;
    @FXML private ListView<String> edYColumns;
    @FXML private ComboBox<String> edAddSeriesCombo;

    @FXML private ComboBox<String> edStyleTheme;
    @FXML private CheckBox      edShowLegend;
    @FXML private CheckBox      edShowGrid;
    @FXML private CheckBox      edShowTooltips;
    @FXML private Slider        edLineWidth;
    @FXML private Label         edLineWidthLabel;
    @FXML private ToggleGroup   exportFormatGroup;
    @FXML private ComboBox<String> edResolution;

    @FXML private Label         statusLabel;
    @FXML private Label         rowCountLabel;
    @FXML private Label         memoryLabel;

    private DashboardPresenter presenter;

    private final Map<String, ChartFxChartPanel> chartPanels = new LinkedHashMap<>();
    private final Map<String, VBox>              cardNodes   = new LinkedHashMap<>();

    // FIX: мапи для зберігання filter-widgets і типів колонок —
    // потрібні для collectFilterCriteria()
    private final Map<String, Control>    filterControls    = new LinkedHashMap<>();
    private final Map<String, ColumnType> filterColumnTypes = new LinkedHashMap<>();

    private String selectedCardId = null;

    @FXML
    private void initialize() {
        presenter = ServiceLocatorHolder.get().get(DashboardPresenter.class);
        presenter.attachView(this);

        edLineWidth.valueProperty().addListener((obs, o, n) ->
                edLineWidthLabel.setText("%.1f".formatted(n.doubleValue())));

        disableFilterButtons(true);
    }

    // -------------------------------------------------------------------------
    // FXML handlers
    // -------------------------------------------------------------------------

    @FXML void onTabDashboard(ActionEvent e) {}
    @FXML void onTabData(ActionEvent e)      {}
    @FXML void onTabExport(ActionEvent e)    { presenter.onExportClicked("png"); }
    @FXML void onSaveProject(ActionEvent e)  { presenter.onSaveProjectClicked(); }
    @FXML void onOpenProject(ActionEvent e)  { presenter.onOpenProjectClicked(); }

    @FXML
    void onShowImportDrawer(ActionEvent e) {
        switchLeftDrawer(importDrawer, datasetDrawer);
        clearImportError();
    }

    @FXML void onHideImportDrawer(ActionEvent e) { switchLeftDrawer(datasetDrawer, importDrawer); }

    @FXML
    void onImportSourceChanged(ActionEvent e) {
        boolean isFile = rbFile.isSelected();
        importFilePanel.setVisible(isFile);  importFilePanel.setManaged(isFile);
        importJdbcPanel.setVisible(!isFile); importJdbcPanel.setManaged(!isFile);
        updateImportButtonState();
    }

    @FXML
    void onBrowseImportFile(ActionEvent e) {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Choose data file");
        chooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Data files",
                        "*.csv", "*.tsv", "*.json", "*.ndjson", "*.xlsx"),
                new javafx.stage.FileChooser.ExtensionFilter("All files", "*.*"));
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
        jdbcStatusLabel.setText("Testing…");
        Thread.ofVirtual().start(() -> {
            boolean ok = testJdbc();
            Platform.runLater(() -> jdbcStatusLabel.setText(ok ? "✓ Connected" : "✗ Failed"));
        });
    }

    @FXML void onDoImport(ActionEvent e) {
        clearImportError();
        java.nio.file.Path path = getImportPath();
        if (path == null) { showImportError("Please choose a file or configure JDBC."); return; }
        presenter.startImport(path, new ImportViewAdapter(this));
    }

    @FXML void onDatasetSelected(javafx.scene.input.MouseEvent e) {
        String name = datasetList.getSelectionModel().getSelectedItem();
        if (name != null) presenter.onDatasetSelected(name);
    }

    /**
     * FIX: тепер збирає критерії з UI-фільтрів і передає їх у presenter.
     * Раніше presenter просто оновлював статус і нічого не робив.
     */
    @FXML void onApplyFilter(ActionEvent e) {
        List<FilterCriteria> criteria = collectFilterCriteria();
        presenter.onApplyFilterClicked(criteria);
    }

    @FXML void onResetFilter(ActionEvent e) { presenter.onResetFilterClicked(); }

    @FXML void onAddChart(ActionEvent e)   { presenter.onAddChartClicked("LINE"); }
    @FXML void onToggleGrid(ActionEvent e) { presenter.onToggleGrid(gridToggle.isSelected()); }

    @FXML void onEditorTabChart(ActionEvent e) { switchEditorTab(editorChartScroll); }
    @FXML void onEditorTabStyle(ActionEvent e) { switchEditorTab(editorStyleScroll); }
    @FXML void onEditorTabStats(ActionEvent e) { switchEditorTab(editorStatsScroll); }

    @FXML void onChartTypeChanged(ActionEvent e) {
        if (updatingEditor || chartTypeGroup.getSelectedToggle() == null) return;
        Object ud = chartTypeGroup.getSelectedToggle().getUserData();
        if (ud != null) presenter.onEditorChartTypeChanged(ud.toString());
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

    @FXML void onEdStyleThemeChanged(ActionEvent e) {
        if (edStyleTheme.getValue() != null)
            presenter.onEditorStyleThemeChanged(edStyleTheme.getValue());
    }

    @FXML void onEdStyleOptionChanged(ActionEvent e) {
        presenter.onEditorStyleOptionChanged(edShowLegend.isSelected(),
                edShowGrid.isSelected(), edShowTooltips.isSelected(), edLineWidth.getValue());
    }

    @FXML void onEdExportClicked(ActionEvent e) {
        if (exportFormatGroup.getSelectedToggle() == null) return;
        presenter.onEditorExportClicked(exportFormatGroup.getSelectedToggle().getUserData().toString());
    }

    @FXML void onEdApply(ActionEvent e)  { presenter.onEditorApply(); }
    @FXML void onEdRevert(ActionEvent e) { presenter.onEditorRevert(); }

    // -------------------------------------------------------------------------
    // Public API (викликається з presenter)
    // -------------------------------------------------------------------------

    public void setStatus(String text) { statusLabel.setText(text); }

    public void updateDatasetInfo(String name, long rows, int cols) {
        datasetInfoLabel.setText("%s · %,d rows".formatted(name, rows));
        rowCountLabel.setText("%,d rows · %d cols".formatted(rows, cols));
        disableFilterButtons(false);
    }

    public void addDatasetToList(String name) {
        if (!datasetList.getItems().contains(name)) datasetList.getItems().add(name);
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

    public void addChartPanel(ChartRenderResult result) {
        if (result == null) return;
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

        Label badgeLbl = new Label(config.getChartType().name().toLowerCase());
        badgeLbl.getStyleClass().addAll("chart-type-badge",
                "badge-" + config.getChartType().name().toLowerCase());

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(6, new VBox(2, titleLbl, metaLbl), headerSpacer, badgeLbl);
        header.getStyleClass().add("chart-card-header");

        VBox chartContainer_ = chartPanel.getContainer();
        VBox.setVgrow(chartContainer_, Priority.ALWAYS);

        Button btnEdit      = new Button("✎ Edit");
        btnEdit.getStyleClass().addAll("card-mini-btn", "edit-active");
        Button btnDuplicate = new Button("⧉ Duplicate");
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
        card.setPrefSize(420, 360);
        card.setUserData(panelId);
        cardNodes.put(panelId, card);

        card.setOnMouseClicked(ev -> selectCard(panelId, chartPanel.getCurrentConfig()));
        btnEdit.setOnAction(ev -> { ev.consume(); selectCard(panelId, chartPanel.getCurrentConfig()); });
        btnDuplicate.setOnAction(ev -> { ev.consume(); presenter.onDuplicateChart(panelId); });
        btnRemove.setOnAction(ev -> { ev.consume(); removeCard(panelId); });

        this.chartContainer.getChildren().add(card);
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
        }
    }

    public void applyFilterToAllCharts(List<Integer> activeIndices) {
        chartPanels.values().forEach(p -> p.applyFilter(activeIndices));
    }

    public void setLoading(boolean loading, String message) {
        loadingBox.setVisible(loading); loadingBox.setManaged(loading);
        if (loading) loadingLabel.setText(message);
    }

    public void setMemoryLabel(String text)   { memoryLabel.setText(text); }
    public void addRecentProject(String path) {}  // залишається порожнім — recent projects не реалізовано

    public void setGridVisible(boolean on) {
        chartPanels.values().forEach(p -> {
            // getFxChart() повертає null для PieChart — instanceof безпечно це обробляє
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

    /**
     * Будує UI фільтрів і зберігає віджети у {@code filterControls} /
     * {@code filterColumnTypes} для подальшого читання в {@link #collectFilterCriteria()}.
     *
     * FIX: раніше будувався тільки UI, але зворотній шлях до FilterCriteria був відсутній.
     */
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
                slider.setStyle("-fx-accent: -accent;");
                Label valLbl = new Label("≥ %.1f".formatted(min));
                valLbl.getStyleClass().add("muted-label");
                slider.valueProperty().addListener((o, ov, nv) -> {
                    valLbl.setText("≥ %.1f".formatted(nv.doubleValue()));
                    updateActiveFilterCount();
                });
                row.getChildren().addAll(slider, valLbl);
                filterControls.put(col.getName(), slider);   // FIX: зберігаємо slider
            } else {
                ComboBox<String> combo = new ComboBox<>();
                combo.getStyleClass().add("compact-combo");
                combo.setMaxWidth(Double.MAX_VALUE);
                combo.getItems().add("All");
                col.getValues().stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .distinct()
                        .limit(200)
                        .forEach(combo.getItems()::add);
                combo.getSelectionModel().selectFirst();
                combo.setOnAction(ev -> updateActiveFilterCount());
                row.getChildren().add(combo);
                filterControls.put(col.getName(), combo);    // FIX: зберігаємо combo
            }
            filterContainer.getChildren().add(row);
        });
        updateActiveFilterCount();
    }

    /**
     * Читає поточний стан всіх filter-widgets і конвертує в {@link FilterCriteria}.
     * Нейтральні значення (slider на мінімумі, combo = "All") пропускаються.
     *
     * FIX: цей метод повністю новий — раніше зворотній шлях був відсутній.
     */
    @SuppressWarnings("unchecked")
    public List<FilterCriteria> collectFilterCriteria() {
        List<FilterCriteria> result = new ArrayList<>();

        for (Map.Entry<String, Control> entry : filterControls.entrySet()) {
            String     colName = entry.getKey();
            Control    control = entry.getValue();
            ColumnType type    = filterColumnTypes.getOrDefault(colName, ColumnType.CATEGORICAL);

            if (type == ColumnType.NUMERIC && control instanceof Slider slider) {
                double min = slider.getMin();
                double val = slider.getValue();
                if (val > min) {                         // slider рухали — є обмеження
                    result.add(FilterCriteria.numericRange(colName, val, slider.getMax()));
                }
            } else if (control instanceof ComboBox<?> combo) {
                String selected = combo.getValue() == null ? null : combo.getValue().toString();
                if (selected != null && !"All".equals(selected)) {
                    if (type == ColumnType.BOOLEAN) {
                        result.add(FilterCriteria.booleanEquals(colName,
                                Boolean.parseBoolean(selected)));
                    } else {
                        result.add(FilterCriteria.categoricalIn(colName, List.of(selected)));
                    }
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
        if (config == null) { showEditorNoSelection(); return; }
        updatingEditor = true;
        try {
            editorNoSelection.setVisible(false); editorNoSelection.setManaged(false);
            editorChartScroll.setVisible(true);  editorChartScroll.setManaged(true);
            editorStyleScroll.setVisible(false); editorStyleScroll.setManaged(false);
            editorStatsScroll.setVisible(false); editorStatsScroll.setManaged(false);
            editorFooter.setVisible(true);       editorFooter.setManaged(true);
            edTabChart.setSelected(true);

            chartTypeGroup.getToggles().forEach(t -> {
                Object ud = t.getUserData();
                if (ud != null && ud.toString().equals(config.getChartType().name()))
                    t.setSelected(true);
            });

            edChartTitle.setText(config.getTitle());
            String xCol = config.getXColumn();
            if (xCol != null && !edXColumn.getItems().contains(xCol))
                edXColumn.getItems().add(0, xCol);
            edXColumn.setValue(xCol);
            edYColumns.getSelectionModel().clearSelection();
            edYColumns.getItems().setAll(config.getYColumns());

            if (config.getStyle() != null && edStyleTheme != null)
                edStyleTheme.setValue(config.getStyle().getTheme().name());

            if (edXLabel != null) edXLabel.setText(config.getXLabel() != null ? config.getXLabel() : "");
            if (edYLabel != null) edYLabel.setText(config.getYLabel() != null ? config.getYLabel() : "");
        } finally {
            updatingEditor = false;
        }
    }

    public void populateEditorStats(DataSet ds, ChartConfig config) {
        if (edStatsContainer == null || ds == null) return;
        edStatsContainer.getChildren().clear();

        List<String> relevant = new ArrayList<>();
        if (config.getXColumn() != null) relevant.add(config.getXColumn());
        relevant.addAll(config.getYColumns());

        ds.getColumns().stream()
                .filter(col -> relevant.contains(col.getName()))
                .forEach(col -> {
                    VBox card = new VBox(3);
                    card.getStyleClass().add("ed-stats-card");
                    card.setStyle("-fx-padding: 8 10 10 10;");

                    Label nameL = new Label(col.getName());
                    nameL.getStyleClass().add("ed-stat-col-name");
                    card.getChildren().add(nameL);

                    GridPane grid = new GridPane();
                    grid.setHgap(10); grid.setVgap(3);
                    addStatRow(grid, 0, "Rows",  "%,d".formatted(ds.getRowCount()));
                    addStatRow(grid, 1, "Nulls", "%,d".formatted(col.getNullCount()));
                    if (col.getMin() != null)  addStatRow(grid, 2, "Min",  "%.2f".formatted(col.getMin()));
                    if (col.getMax() != null)  addStatRow(grid, 3, "Max",  "%.2f".formatted(col.getMax()));
                    if (col.getMean() != null) addStatRow(grid, 4, "Mean", "%.2f".formatted(col.getMean()));
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
        importProgressLabel.setText("Loading… %.0f%%".formatted(value * 100));
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

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void selectCard(String panelId, ChartConfig config) {
        if (selectedCardId != null) {
            VBox prev = cardNodes.get(selectedCardId);
            if (prev != null) prev.getStyleClass().remove("selected-card");
        }
        selectedCardId = panelId;
        VBox card = cardNodes.get(panelId);
        if (card != null && !card.getStyleClass().contains("selected-card"))
            card.getStyleClass().add("selected-card");
        populateEditorWithConfig(config);
        presenter.onEditorCardSelected(panelId, config);
    }

    private void removeCard(String panelId) {
        VBox card = cardNodes.remove(panelId);
        if (card != null) chartContainer.getChildren().remove(card);
        ChartFxChartPanel panel = chartPanels.remove(panelId);
        if (panel != null) panel.getContainer().getChildren().clear();
        if (panelId.equals(selectedCardId)) {
            selectedCardId = null;
            showEditorNoSelection();
        }
    }

    private void showEditorNoSelection() {
        editorNoSelection.setVisible(true);  editorNoSelection.setManaged(true);
        editorChartScroll.setVisible(false); editorChartScroll.setManaged(false);
        editorStyleScroll.setVisible(false); editorStyleScroll.setManaged(false);
        editorStatsScroll.setVisible(false); editorStatsScroll.setManaged(false);
        editorFooter.setVisible(false);      editorFooter.setManaged(false);
    }

    private void switchEditorTab(ScrollPane show) {
        for (ScrollPane sp : List.of(editorChartScroll, editorStyleScroll, editorStatsScroll)) {
            sp.setVisible(false); sp.setManaged(false);
        }
        show.setVisible(true); show.setManaged(true);
    }

    private void switchLeftDrawer(VBox show, VBox hide) {
        hide.setVisible(false); hide.setManaged(false);
        show.setVisible(true);  show.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(160), show);
        ft.setFromValue(0.0); ft.setToValue(1.0); ft.play();
    }

    private void disableFilterButtons(boolean disable) {
        btnApplyFilter.setDisable(disable);
        btnResetFilter.setDisable(disable);
    }

    private void updateActiveFilterCount() {
        long active = filterControls.entrySet().stream().filter(e -> {
            Control    c = e.getValue();
            ColumnType t = filterColumnTypes.getOrDefault(e.getKey(), ColumnType.CATEGORICAL);
            if (t == ColumnType.NUMERIC && c instanceof Slider s)
                return s.getValue() > s.getMin();
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
        String qry  = jdbcQueryArea  != null ? jdbcQueryArea.getText()  : "";
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

    private static void addStatRow(GridPane g, int row, String label, String value) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("ed-stat-label");
        Label val = new Label(value);
        val.setStyle("-fx-font-size:12px; -fx-text-fill:-text0; -fx-font-family:'Courier New',monospace;");
        g.add(lbl, 0, row); g.add(val, 1, row);
    }
}