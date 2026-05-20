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
import java.util.logging.Logger;

@Component
public final class DashboardPresenter implements DatasetObserver {

    private static final Logger LOG = Logger.getLogger(DashboardPresenter.class.getName());

    private final DatasetObservable observable;
    private final ChartService      chartService;
    private final DataVizFacade     facade;
    private final FilterPresenter   filterPresenter;  // FIX: підключений FilterPresenter

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
                              FilterPresenter    filterPresenter) {  // FIX: новий параметр
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
            view.setStatus("Loaded: " + ds.getName());
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
            view.setFilterStatus("Filter: %,d / %,d rows"
                    .formatted(fr.getMatchedCount(), fr.getTotalCount()));
            view.setStatus("Filter applied");
            reRenderAllCards();
        });
    }

    private void handleFilterReset(DataSet ds) {
        this.currentFilterResult = null;
        Platform.runLater(() -> {
            if (view == null) return;
            view.setFilterStatus("");
            view.setStatus("Filter cleared");
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
                view.showImportError("Unsupported format. Supported: CSV, JSON, XLSX.");
                return;
            }

            view.showImportProgress(true);
            view.setStatus("Loading data…");

            loadService.loadAsync(path,
                    ds -> {
                        observable.notifyDatasetLoaded(ds);
                        Platform.runLater(() -> {
                            view.showImportProgress(false);
                            view.onImportSuccess();
                        });
                    },
                    err -> Platform.runLater(() -> {
                        view.showImportProgress(false);
                        view.showImportError("Load error: " + err.getMessage());
                    }));
        } catch (Exception e) {
            LOG.severe("Import failed: " + e.getMessage());
            view.showImportError("Import failed: " + e.getMessage());
        }
    }

    public void onSaveProjectClicked() {
        if (currentDataSet == null) { showError("No data to save."); return; }
        Path target = currentProjectPath != null ? currentProjectPath : chooseSavePath();
        if (target == null) return;
        try {
            ProjectState state = ProjectState.builder()
                    .name(currentDataSet.getName())
                    .dataSetPath(currentDataSet.getSourceDescription())
                    .chartConfigs(new ArrayList<>(activeConfigs.values()))
                    .activeFilters(List.of())
                    .build();
            facade.saveProject(state, target);
            currentProjectPath = target;
            Platform.runLater(() -> {
                if (view != null) view.setStatus("Project saved: " + target.getFileName());
            });
        } catch (ProjectException e) {
            LOG.warning("Save failed: " + e.getMessage());
            showError(e.getMessage());
        }
    }

    public void onOpenProjectClicked() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open project");
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
                    view.setStatus("Project opened: " + state.getName());
                    state.getChartConfigs().forEach(this::addChartWithConfig);
                }
            });
        } catch (ProjectException e) {
            LOG.warning("Open failed: " + e.getMessage());
            showError(e.getMessage());
        }
    }

    public void onAddChartClicked(String type) {
        if (currentDataSet == null) { showError("Import data first."); return; }
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

    public void onEditorStyleThemeChanged(String themeName) {
        if (selectedConfig == null) return;
        try {
            ChartStyle.Theme theme = ChartStyle.Theme.valueOf(themeName);
            ChartStyle style = switch (theme) {
                case DARK      -> ChartStyle.darkStyle();
                case CORPORATE -> ChartStyle.corporateStyle();
                default        -> ChartStyle.defaultStyle();
            };
            selectedConfig = ChartConfig.builder()
                    .id(selectedConfig.getId()).chartType(selectedConfig.getChartType())
                    .title(selectedConfig.getTitle()).xColumn(selectedConfig.getXColumn())
                    .yColumns(selectedConfig.getYColumns())
                    .xLabel(selectedConfig.getXLabel()).yLabel(selectedConfig.getYLabel())
                    .style(style).build();
        } catch (IllegalArgumentException ignored) {}
    }

    public void onEditorStyleOptionChanged(boolean legend, boolean grid,
                                           boolean tooltips, double lineWidth) {
        if (selectedConfig == null) return;
        ChartStyle style = ChartStyle.builder()
                .theme(selectedConfig.getStyle().getTheme())
                .showLegend(legend).showGrid(grid).showTooltips(tooltips).lineWidth(lineWidth)
                .seriesColors(selectedConfig.getStyle().getSeriesColors())
                .backgroundColor(selectedConfig.getStyle().getBackgroundColor())
                .gridColor(selectedConfig.getStyle().getGridColor())
                .axisColor(selectedConfig.getStyle().getAxisColor())
                .titleFont(selectedConfig.getStyle().getTitleFont())
                .titleFontSize(selectedConfig.getStyle().getTitleFontSize())
                .labelFont(selectedConfig.getStyle().getLabelFont())
                .labelFontSize(selectedConfig.getStyle().getLabelFontSize())
                .build();
        selectedConfig = ChartConfig.builder()
                .id(selectedConfig.getId()).chartType(selectedConfig.getChartType())
                .title(selectedConfig.getTitle()).xColumn(selectedConfig.getXColumn())
                .yColumns(selectedConfig.getYColumns())
                .xLabel(selectedConfig.getXLabel()).yLabel(selectedConfig.getYLabel())
                .style(style).build();
    }

    public void onEditorApply() {
        if (currentDataSet == null || selectedConfig == null) return;

        ChartRenderResult result = currentFilterResult != null
                ? chartService.buildChartFromFilter(currentFilterResult, selectedConfig)
                : chartService.buildChart(currentDataSet, selectedConfig);

        final String cardId = selectedCardId;
        Platform.runLater(() -> {
            if (view == null) return;
            if (cardId != null) {
                view.updateChartCard(cardId, result);
                activeConfigs.put(cardId, selectedConfig);
            } else {
                view.addChartPanel(result);
                String newId = result.getConfig().getId();
                selectedCardId = newId;
                activeConfigs.put(newId, selectedConfig);
                view.setDashboardVisible(true);
            }
            view.setStatus("Chart applied: " + selectedConfig.getTitle());
        });
        LOG.info("Chart applied: " + selectedConfig.getChartType());
    }

    public void onEditorRevert() {
        if (selectedConfig != null && view != null)
            view.populateEditorWithConfig(selectedConfig);
    }

    public void onEditorExportClicked(String format) {
        if (currentDataSet == null) { showError("No data to export."); return; }
        FileChooser chooser = buildExportChooser(format);
        if (chooser == null) return;
        File file = chooser.showSaveDialog(getPrimaryStage());
        if (file == null) return;
        try {
            DashboardSnapshot snapshot = view.createSnapshot();
            if (snapshot == null) { showError("No charts to export."); return; }
            facade.exportDashboard(snapshot, format, file.toPath(),
                    ExportOptions.builder().dpi(300).build());
            Platform.runLater(() -> view.setStatus("Exported: " + file.getName()));
        } catch (Exception e) {
            LOG.severe("Export error: " + e.getMessage());
            showError(e.getMessage());
        }
    }

    /**
     * FIX: раніше метод лише оновлював статус і нічого не робив.
     * Тепер збирає критерії з view і делегує FilterPresenter.
     */
    public void onApplyFilterClicked(List<FilterCriteria> criteria) {
        if (view != null) view.setStatus("Applying filter…");
        filterPresenter.onApplyFilterClicked(criteria);
    }

    public void onResetFilterClicked() {
        currentFilterResult = null;
        filterPresenter.onResetFilterClicked();
        Platform.runLater(() -> {
            if (view != null) { view.setFilterStatus(""); view.setStatus("Filter cleared"); }
        });
    }

    public void onToggleGrid(boolean on)  { if (view != null) view.setGridVisible(on); }
    public void onToggleDataPanel()       { if (view != null) view.toggleDataPanel(); }
    public void onToggleFilterPanel()     { if (view != null) view.toggleFilterPanel(); }

    public void onExportClicked(String fmt) {
        if (currentDataSet == null) { showError("No data to export."); return; }
        FileChooser chooser = buildExportChooser(fmt);
        if (chooser == null) return;
        File file = chooser.showSaveDialog(getPrimaryStage());
        if (file == null) return;
        try {
            DashboardSnapshot snapshot = view != null ? view.createSnapshot() : null;
            if (snapshot == null) { showError("No charts to export."); return; }
            facade.exportDashboard(snapshot, fmt, file.toPath(), ExportOptions.forPublication());
            Platform.runLater(() -> { if (view != null) view.setStatus("Exported: " + file.getName()); });
        } catch (Exception e) {
            LOG.severe("Export error: " + e.getMessage());
            showError(e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void addChartWithConfig(ChartConfig config) {
        if (currentDataSet == null) return;
        ChartRenderResult result = currentFilterResult != null
                ? chartService.buildChartFromFilter(currentFilterResult, config)
                : chartService.buildChart(currentDataSet, config);
        Platform.runLater(() -> {
            if (view != null) {
                view.addChartPanel(result);
                activeConfigs.put(config.getId(), config);
                view.setDashboardVisible(true);
            }
        });
    }

    private void reRenderAllCards() {
        if (view == null) return;
        List<Integer> indices = currentFilterResult != null
                ? currentFilterResult.getMatchedIndices()
                : null;
        view.applyFilterToAllCharts(indices);
        LOG.fine(() -> "Re-rendered all cards, filter indices: "
                + (indices != null ? indices.size() : "all"));
    }

    private ChartConfig buildDefaultConfig(String type) {
        ChartConfig.ChartType chartType;
        try { chartType = ChartConfig.ChartType.valueOf(type); }
        catch (IllegalArgumentException e) { chartType = ChartConfig.ChartType.LINE; }

        List<com.dataviz.domain.model.DataColumn> cols =
                currentDataSet != null ? currentDataSet.getColumns() : List.of();

        String xCol = cols.isEmpty() ? "" : cols.get(0).getName();
        List<String> yCols = cols.size() > 1
                ? cols.subList(1, Math.min(cols.size(), 3)).stream()
                .map(c -> c.getName()).toList()
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
            loadService.loadAsync(path,
                    this::handleDatasetLoaded,
                    err -> showError("Load failed: " + err.getMessage()));
        } catch (Exception e) {
            LOG.warning("Cannot load dataset: " + e.getMessage());
            showError("Cannot load: " + path);
        }
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Error"); alert.setHeaderText(null);
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
        chooser.setTitle("Save project as…");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("DataViz project (*.dvp)", "*.dvp"));
        File file = chooser.showSaveDialog(getPrimaryStage());
        return file != null ? file.toPath() : null;
    }

    private FileChooser buildExportChooser(String fmt) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export");
        return switch (fmt.toLowerCase()) {
            case "png" -> { chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PNG image", "*.png")); yield chooser; }
            case "pdf" -> { chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF document", "*.pdf")); yield chooser; }
            case "svg" -> { chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("SVG file", "*.svg")); yield chooser; }
            default    -> { showError("Unknown format: " + fmt); yield null; }
        };
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}