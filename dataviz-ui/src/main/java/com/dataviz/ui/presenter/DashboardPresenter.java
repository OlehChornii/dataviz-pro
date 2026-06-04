package com.dataviz.ui.presenter;

import com.dataviz.common.event.DatasetChangeEvent;
import com.dataviz.common.event.DatasetObservable;
import com.dataviz.common.event.DatasetObserver;
import com.dataviz.di.annotation.*;
import com.dataviz.domain.chart.ChartConfig;
import com.dataviz.domain.chart.ChartStyle;
import com.dataviz.domain.dashboard.DashboardSnapshot;
import com.dataviz.domain.filter.FilterCriteria;
import com.dataviz.domain.filter.FilterResult;
import com.dataviz.domain.model.DataSet;
import com.dataviz.facade.DataVizFacade;
import com.dataviz.service.chart.ChartService;
import com.dataviz.service.chart.ChartService.ChartRenderResult;
import com.dataviz.service.export.ExportOptions;
import com.dataviz.service.project.ProjectService.ProjectException;
import com.dataviz.service.project.ProjectState;
import com.dataviz.ui.ServiceLocatorHolder;
import com.dataviz.ui.view.DashboardView;
import com.dataviz.ui.view.ImportViewAdapter;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.logging.Logger;

@Component
public final class DashboardPresenter implements DatasetObserver {

    private static final Logger LOG = Logger.getLogger(DashboardPresenter.class.getName());

    private final DatasetObservable observable;
    private final ChartService      chartService;
    private final DataVizFacade     facade;
    private final FilterPresenter   filterPresenter;

    private DataSet      currentDataSet;
    private FilterResult currentFilterResult;
    private Path         currentProjectPath;

    private String      selectedCardId;
    private ChartConfig selectedConfig;

    private final Map<String, ChartConfig> activeConfigs = new LinkedHashMap<>();

    private DashboardView view;

    @Inject
    public DashboardPresenter(DatasetObservable observable,
                              ChartService       chartService,
                              DataVizFacade      facade,
                              FilterPresenter    filterPresenter) {
        this.observable      = observable;
        this.chartService    = chartService;
        this.facade          = facade;
        this.filterPresenter = filterPresenter;
    }

    public void attachView(DashboardView view) { this.view = view; }

    @PostConstruct
    private void subscribe() {
        observable.addObserver(this);
        facade.cleanRecentProjects();
    }

    @PreDestroy
    private void unsubscribe() { observable.removeObserver(this); }

    @Override
    public void onDatasetChanged(DatasetChangeEvent event) {
        switch (event.type()) {
            case DATASET_LOADED  -> handleDatasetLoaded(event.source());
            case FILTER_APPLIED  -> handleFilterApplied(event.filterResult());
            case FILTER_RESET    -> handleFilterReset(event.source());
            case DATASET_REMOVED -> {}
            case CHART_APPLIED   -> {}
        }
    }

    private void handleDatasetLoaded(DataSet ds) {
        this.currentDataSet      = ds;
        this.currentFilterResult = null;
        LOG.info(() -> "Dataset loaded: %s (%,d rows, %d cols)"
                .formatted(ds.getName(), ds.getRowCount(), ds.getColumnCount()));
        Platform.runLater(() -> {
            if (view == null) return;
            view.updateDatasetInfo(ds.getName(), ds.getRowCount(), ds.getColumnCount());
            view.addDatasetToList(ds.getName());
            view.setDashboardVisible(true);
            view.setStatus(view.localize("Loaded: " + ds.getName(), "Завантажено: " + ds.getName()));
            view.clearCharts();
            activeConfigs.clear();
            view.buildFilterPanel(ds);
            view.populateColumnSelectors(
                    ds.getColumns().stream().map(c -> c.getName()).toList());
            long mb = ds.estimatedMemoryBytes() / (1024 * 1024);
            view.setMemoryLabel(mb + " MB in memory");
        });
    }

    private void handleFilterApplied(FilterResult fr) {
        this.currentFilterResult = fr;
        Platform.runLater(() -> {
            if (view == null) return;
            view.setFilterStatus(view.localize(
                    "Filter: %,d / %,d rows".formatted(fr.getMatchedCount(), fr.getTotalCount()),
                    "Фільтр: %,d / %,d рядків".formatted(fr.getMatchedCount(), fr.getTotalCount())));
            view.setStatus(view.localize("Filter applied", "Фільтр застосовано"));
            reRenderAllCards();
        });
    }

    private void handleFilterReset(DataSet ds) {
        this.currentFilterResult = null;
        Platform.runLater(() -> {
            if (view == null) return;
            view.setFilterStatus("");
            view.setStatus(view.localize("Filter cleared", "Фільтр скинуто"));
            reRenderAllCards();
        });
    }

    public void onDatasetSelected(String name) {
        if (currentDataSet != null && currentDataSet.getName().equals(name)) return;
        facade.findDataSetByName(name).ifPresent(this::handleDatasetLoaded);
    }

    public void startImport(Path path, ImportViewAdapter adapter) {
        try {
            com.dataviz.service.load.DataLoadService loadService =
                    ServiceLocatorHolder.get().get(com.dataviz.service.load.DataLoadService.class);
            if (!loadService.isSupported(path.toString())) {
                view.showImportError(view.localize(
                        "Unsupported format. Supported: CSV, JSON, XLSX.",
                        "Непідтримуваний формат. Підтримуються CSV, JSON, XLSX.")); return;
            }
            view.showImportProgress(true);
            view.setStatus(view.localize("Loading data…", "Завантаження даних…"));
            loadService.loadAsync(path,
                    ds -> {
                        observable.notifyDatasetLoaded(ds);
                        Platform.runLater(() -> { view.showImportProgress(false); view.onImportSuccess(); });
                    },
                    err -> Platform.runLater(() -> {
                        view.showImportProgress(false);
                        view.showImportError(view.localize(
                                "Load error: " + err.getMessage(),
                                "Помилка завантаження: " + err.getMessage()));
                    }));
        } catch (Exception e) {
            LOG.severe("Import failed: " + e.getMessage());
            view.showImportError(view.localize(
                    "Import failed: " + e.getMessage(),
                    "Імпорт не вдався: " + e.getMessage()));
        }
    }

    public void onSaveProjectClicked() {
        if (currentDataSet == null) { showError(view.localize("No data to save.", "Немає даних для збереження.")); return; }
        Path target = currentProjectPath != null ? currentProjectPath : chooseSavePath();
        if (target == null) return;
        try {
            ProjectState state = ProjectState.builder()
                    .name(currentDataSet.getName())
                    .dataSetPath(currentDataSet.getSourceDescription())
                    .chartConfigs(new ArrayList<>(activeConfigs.values()))
                    .activeFilters(List.of()).build();
            facade.saveProject(state, target);
            currentProjectPath = target;
            Platform.runLater(() -> { if (view != null) view.setStatus(
                    view.localize("Project saved: " + target.getFileName(),
                            "Проект збережено: " + target.getFileName())); });
        } catch (ProjectException e) {
            LOG.warning("Save failed: " + e.getMessage());
            showError(e.getMessage());
        }
    }

    public void onOpenProjectClicked() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Відкрити проект");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("DataViz project (*.dvp)", "*.dvp"));
        File file = chooser.showOpenDialog(getPrimaryStage());
        if (file == null) return;
        try {
            ProjectState state = facade.openProject(file.toPath());
            currentProjectPath = file.toPath();
            if (state.getDataSetPath() != null && !state.getDataSetPath().isBlank())
                loadDataSetFromPath(Path.of(state.getDataSetPath()));
            Platform.runLater(() -> {
                if (view != null) {
                            view.setStatus(view.localize("Project opened: " + state.getName(),
                            "Проект відкрито: " + state.getName()));
                    state.getChartConfigs().forEach(this::addChartWithConfig);
                }
            });
        } catch (ProjectException e) {
            LOG.warning("Open failed: " + e.getMessage());
            showError(e.getMessage());
        }
    }

    public void onAddChartClicked(String type) {
        if (currentDataSet == null) { showError(view.localize("Import data first.", "Спочатку імпортуйте дані.")); return; }
        selectedConfig = buildDefaultConfig(type);
        addChartWithConfig(selectedConfig);
    }

    public void onDuplicateChart(String panelId) {
        if (currentDataSet == null) return;
        ChartConfig orig = activeConfigs.getOrDefault(panelId, buildDefaultConfig("LINE"));
        ChartConfig copy = ChartConfig.builder()
                .id(UUID.randomUUID().toString())
                .chartType(orig.getChartType())
                .title(orig.getTitle() + " (copy)")
                .xColumn(orig.getXColumn())
                .yColumns(orig.getYColumns())
                .xLabel(orig.getXLabel())
                .yLabel(orig.getYLabel())
                .style(orig.getStyle())
                .build();
        addChartWithConfig(copy);
    }

    public void onEditorCardSelected(String panelId, ChartConfig config) {
        this.selectedCardId = panelId;
        this.selectedConfig = config;
        if (currentDataSet != null)
            Platform.runLater(() -> view.populateEditorStats(currentDataSet, config));
    }

    public void onEditorChartTypeChanged(String typeName) {
        if (selectedConfig == null) return;
        try {
            ChartConfig.ChartType type = ChartConfig.ChartType.valueOf(typeName);
            selectedConfig = ChartConfig.builder()
                    .id(selectedConfig.getId()).chartType(type)
                    .title(selectedConfig.getTitle()).xColumn(selectedConfig.getXColumn())
                    .yColumns(selectedConfig.getYColumns())
                    .xLabel(selectedConfig.getXLabel()).yLabel(selectedConfig.getYLabel())
                    .style(selectedConfig.getStyle()).build();
        } catch (IllegalArgumentException ignored) {}
    }

    public void onEditorTitleChanged(String title) {
        if (selectedConfig != null) selectedConfig = selectedConfig.withTitle(title);
    }

    public void onEditorXColumnChanged(String column) {
        if (selectedConfig == null) return;
        selectedConfig = ChartConfig.builder()
                .id(selectedConfig.getId()).chartType(selectedConfig.getChartType())
                .title(selectedConfig.getTitle()).xColumn(column)
                .yColumns(selectedConfig.getYColumns())
                .xLabel(selectedConfig.getXLabel()).yLabel(selectedConfig.getYLabel())
                .style(selectedConfig.getStyle()).build();
    }

    public void onEditorAddYColumn(String column) {
        if (selectedConfig == null || column == null || column.isBlank()) return;
        if (selectedConfig.getYColumns().contains(column)) return;
        List<String> newY = new ArrayList<>(selectedConfig.getYColumns());
        newY.add(column);
        selectedConfig = ChartConfig.builder()
                .id(selectedConfig.getId()).chartType(selectedConfig.getChartType())
                .title(selectedConfig.getTitle()).xColumn(selectedConfig.getXColumn())
                .yColumns(newY)
                .xLabel(selectedConfig.getXLabel()).yLabel(selectedConfig.getYLabel())
                .style(selectedConfig.getStyle()).build();
    }

    public void onEditorRemoveYColumn(String column) {
        if (selectedConfig == null) return;
        List<String> newY = new ArrayList<>(selectedConfig.getYColumns());
        newY.remove(column);
        selectedConfig = ChartConfig.builder()
                .id(selectedConfig.getId()).chartType(selectedConfig.getChartType())
                .title(selectedConfig.getTitle()).xColumn(selectedConfig.getXColumn())
                .yColumns(newY)
                .xLabel(selectedConfig.getXLabel()).yLabel(selectedConfig.getYLabel())
                .style(selectedConfig.getStyle()).build();
    }

    public void onEditorStyleOptionChanged(boolean legend, boolean grid,
                                           boolean tooltips, double lineWidth) {
        updateSelectedStyle(b -> b
                .showLegend(legend)
                .showGrid(grid)
                .showTooltips(tooltips)
                .lineWidth(lineWidth));
    }

    public void onEditorStyleThemeChanged(String themeName) {
        if (selectedConfig == null) return;
        try {
            ChartStyle.Theme theme = ChartStyle.Theme.valueOf(themeName);
            ChartStyle base = switch (theme) {
                case DARK      -> ChartStyle.darkStyle();
                case CORPORATE -> ChartStyle.corporateStyle();
                default        -> ChartStyle.defaultStyle();
            };
            selectedConfig = ChartConfig.builder()
                    .id(selectedConfig.getId()).chartType(selectedConfig.getChartType())
                    .title(selectedConfig.getTitle()).xColumn(selectedConfig.getXColumn())
                    .yColumns(selectedConfig.getYColumns())
                    .xLabel(selectedConfig.getXLabel()).yLabel(selectedConfig.getYLabel())
                    .style(base).build();
        } catch (IllegalArgumentException ignored) {}
    }

    public void onEditorSmoothingChanged(boolean v) {
        updateSelectedStyle(b -> b.smoothing(v));
    }

    public void onEditorPointSizeChanged(double v) {
        updateSelectedStyle(b -> b.pointSize(v));
    }
    public void onEditorPointShapeChanged(String v) {
        updateSelectedStyle(b -> b.pointShape(v));
    }
    public void onEditorShowTrendLineChanged(boolean v) {
        updateSelectedStyle(b -> b.showTrendLine(v));
    }
    public void onEditorSeriesTransparencyChanged(double v) {
        updateSelectedStyle(b -> b.seriesTransparency(v));
    }

    public void onEditorBarWidthChanged(double v) {
        updateSelectedStyle(b -> b.barWidth(v));
    }
    public void onEditorStackingModeChanged(String v) {
        updateSelectedStyle(b -> b.stackingMode(v));
    }

    public void onEditorSliceLabelsChanged(boolean v) {
        updateSelectedStyle(b -> b.sliceLabels(v));
    }
    public void onEditorLegendPositionChanged(String v) {
        updateSelectedStyle(b -> b.legendPosition(v));
    }
    public void onEditorDonutModeChanged(boolean v) {
        updateSelectedStyle(b -> b.donutMode(v));
    }
    public void onEditorInnerRadiusChanged(double v) {
        updateSelectedStyle(b -> b.innerRadius(v));
    }

    public void onEditorColorScaleChanged(String v) {
        updateSelectedStyle(b -> b.colorScale(v));
    }
    public void onEditorColorRangeMinChanged(String v) {
        updateSelectedStyle(b -> b.colorRangeMin(v));
    }
    public void onEditorColorRangeMaxChanged(String v) {
        updateSelectedStyle(b -> b.colorRangeMax(v));
    }
    public void onEditorShowAxisLabelsChanged(boolean v) {
        updateSelectedStyle(b -> b.showAxisLabels(v));
    }

    public void onEditorApply() {
        if (currentDataSet == null || selectedConfig == null) return;
        ChartRenderResult result = currentFilterResult != null
                ? chartService.buildChartFromFilter(currentFilterResult, selectedConfig)
                : chartService.buildChart(currentDataSet, selectedConfig);
        String cardId = selectedCardId;
        // if presenter doesn't have a selected card id (out of sync), try view's selection
        if (cardId == null && view != null) cardId = view.getSelectedCardId();
        // if view selection is also missing, fall back to the selected config id for an existing chart
        if (cardId == null && selectedConfig != null && activeConfigs.containsKey(selectedConfig.getId())) {
            cardId = selectedConfig.getId();
        }
        final String targetCardId = cardId; // must be effectively final for lambda capture
        Platform.runLater(() -> {
            if (view == null) return;
            if (targetCardId != null) {
                view.updateChartCard(targetCardId, result);
                activeConfigs.put(targetCardId, selectedConfig);
                // keep presenter in sync with view
                selectedCardId = targetCardId;
            } else {
                String newId = view.addChartPanel(result);
                selectedCardId = newId;
                activeConfigs.put(selectedConfig.getId() != null ? selectedConfig.getId() : newId,
                        selectedConfig);
                view.setDashboardVisible(true);
            }
            if (currentDataSet != null) {
                view.populateEditorStats(currentDataSet, selectedConfig);
            }
            view.setStatus(view.localize("Chart applied: " + selectedConfig.getTitle(),
                    "Діаграму застосовано: " + selectedConfig.getTitle()));
        });
        LOG.info("Chart applied: " + selectedConfig.getChartType());
    }

    public void onEditorRevert() {
        if (selectedConfig != null && view != null)
            view.populateEditorWithConfig(selectedConfig);
    }

    public void onEditorExportClicked(String format) {
        if (currentDataSet == null) { showError(view.localize("No data to export.", "Немає даних для експорту.")); return; }
        FileChooser chooser = buildExportChooser(format);
        if (chooser == null) return;
        File file = chooser.showSaveDialog(getPrimaryStage());
        if (file == null) return;
        try {
            DashboardSnapshot snapshot = view != null ? view.createSnapshot() : null;
            if (snapshot == null) { showError(view.localize("No charts to export.", "Немає діаграм для експорту.")); return; }
            facade.exportDashboard(snapshot, format, file.toPath(),
                    ExportOptions.builder().dpi(300).build());
            Platform.runLater(() -> { if (view != null) view.setStatus(
                    view.localize("Exported: " + file.getName(), "Експортовано: " + file.getName())); });
        } catch (Exception e) {
            LOG.severe("Export error: " + e.getMessage());
            showError(e.getMessage());
        }
    }

    public void onApplyFilterClicked(List<FilterCriteria> criteria) {
        if (view != null) view.setStatus(view.localize("Applying filter…", "Застосування фільтру…"));
        filterPresenter.onApplyFilterClicked(criteria);
    }

    public void onResetFilterClicked() {
        currentFilterResult = null;
        filterPresenter.onResetFilterClicked();
        Platform.runLater(() -> {
            if (view != null) { view.setFilterStatus(""); view.setStatus(view.localize("Filter cleared", "Фільтр скинуто")); }
        });
    }

    public void onToggleGrid(boolean on)  { if (view != null) view.setGridVisible(on); }
    public void onToggleDataPanel()       { if (view != null) view.toggleDataPanel(); }
    public void onToggleFilterPanel()     { if (view != null) view.toggleFilterPanel(); }

    public DataSet getCurrentDataSet()      { return currentDataSet; }
    public ChartConfig getSelectedConfig()  { return selectedConfig; }

    public void onExportClicked(String fmt) {
        if (currentDataSet == null) { showError(view.localize("No data to export.", "Немає даних для експорту.")); return; }
        FileChooser chooser = buildExportChooser(fmt);
        if (chooser == null) return;
        File file = chooser.showSaveDialog(getPrimaryStage());
        if (file == null) return;
        try {
            DashboardSnapshot snapshot = view != null ? view.createSnapshot() : null;
            if (snapshot == null) { showError(view.localize("No charts to export.", "Немає діаграм для експорту.")); return; }
            facade.exportDashboard(snapshot, fmt, file.toPath(), ExportOptions.forPublication());
            Platform.runLater(() -> { if (view != null) view.setStatus(
                    view.localize("Exported: " + file.getName(), "Експортовано: " + file.getName())); });
        } catch (Exception e) {
            LOG.severe("Export error: " + e.getMessage());
            showError(e.getMessage());
        }
    }

    private void updateSelectedStyle(UnaryOperator<ChartStyle.Builder> fn) {
        if (selectedConfig == null) return;
        ChartStyle newStyle = fn.apply(selectedConfig.getStyle().toBuilder()).build();
        selectedConfig = ChartConfig.builder()
                .id(selectedConfig.getId())
                .chartType(selectedConfig.getChartType())
                .title(selectedConfig.getTitle())
                .xColumn(selectedConfig.getXColumn())
                .yColumns(selectedConfig.getYColumns())
                .xLabel(selectedConfig.getXLabel())
                .yLabel(selectedConfig.getYLabel())
                .style(newStyle)
                .build();
    }

    private void addChartWithConfig(ChartConfig config) {
        if (currentDataSet == null) return;
        ChartRenderResult result = currentFilterResult != null
                ? chartService.buildChartFromFilter(currentFilterResult, config)
                : chartService.buildChart(currentDataSet, config);
        Platform.runLater(() -> {
            if (view != null) {
                String panelId = view.addChartPanel(result);
                activeConfigs.put(config.getId() != null ? config.getId() : panelId, config);
                view.setDashboardVisible(true);
            }
        });
    }

    private void reRenderAllCards() {
        if (view == null) return;
        List<Integer> indices = currentFilterResult != null
                ? currentFilterResult.getMatchedIndices() : null;
        view.applyFilterToAllCharts(indices);
    }

    private ChartConfig buildDefaultConfig(String type) {
        ChartConfig.ChartType chartType;
        try { chartType = ChartConfig.ChartType.valueOf(type); }
        catch (IllegalArgumentException e) { chartType = ChartConfig.ChartType.LINE; }

        List<com.dataviz.domain.model.DataColumn> cols =
                currentDataSet != null ? currentDataSet.getColumns() : List.of();
        String xCol = cols.isEmpty() ? "" : cols.get(0).getName();
        List<String> yCols = cols.size() > 1
                ? cols.subList(1, Math.min(cols.size(), 3)).stream().map(c -> c.getName()).toList()
                : List.of();
        return ChartConfig.builder()
                .id(UUID.randomUUID().toString())
                .chartType(chartType)
                .title(capitalize(type) + " chart")
                .xColumn(xCol)
                .yColumns(yCols)
                .style(ChartStyle.defaultStyle())
                .build();
    }

    private void loadDataSetFromPath(Path path) {
        try {
            com.dataviz.service.load.DataLoadService loadService =
                    ServiceLocatorHolder.get().get(com.dataviz.service.load.DataLoadService.class);
            loadService.loadAsync(path, this::handleDatasetLoaded,
                    err -> showError("Load failed: " + err.getMessage()));
        } catch (Exception e) {
            LOG.warning("Cannot load dataset: " + e.getMessage());
            showError(view.localize("Cannot load: " + path, "Не вдається завантажити: " + path));
        }
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle(view != null ? view.localize("Error", "Помилка") : "Error");
            alert.setHeaderText(null);
            alert.setContentText(message); alert.showAndWait();
        });
    }

    private Stage getPrimaryStage() {
        return javafx.stage.Window.getWindows().stream()
                .filter(javafx.stage.Window::isShowing)
                .filter(w -> w instanceof Stage)
                .map(w -> (Stage) w).findFirst().orElse(null);
    }

    private Path chooseSavePath() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Зберегти проект як…");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("DataViz project (*.dvp)", "*.dvp"));
        File file = chooser.showSaveDialog(getPrimaryStage());
        return file != null ? file.toPath() : null;
    }

    private FileChooser buildExportChooser(String fmt) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Експорт");
        return switch (fmt.toLowerCase()) {
            case "png" -> { chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG зображення",    "*.png")); yield chooser; }
            case "pdf" -> { chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF документ", "*.pdf")); yield chooser; }
            case "svg" -> { chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SVG файл",     "*.svg")); yield chooser; }
            default    -> { showError(view.localize("Unknown format: " + fmt, "Невідомий формат: " + fmt)); yield null; }
        };
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}