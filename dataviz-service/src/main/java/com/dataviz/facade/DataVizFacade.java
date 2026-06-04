package com.dataviz.facade;

import com.dataviz.common.config.AppConfig;
import com.dataviz.common.event.DatasetObservable;
import com.dataviz.data.reader.DataReader;
import com.dataviz.data.reader.ReaderFactory;
import com.dataviz.domain.chart.ChartConfig;
import com.dataviz.domain.dashboard.DashboardSnapshot;
import com.dataviz.domain.filter.FilterCriteria;
import com.dataviz.domain.model.DataSet;
import com.dataviz.service.chart.ChartService;
import com.dataviz.service.export.*;
import com.dataviz.service.filter.FilterService;
import com.dataviz.service.load.DataLoadService;
import com.dataviz.service.project.ProjectService;
import com.dataviz.service.project.ProjectState;
import com.dataviz.di.annotation.*;

import java.nio.file.Path;
import java.rmi.server.ExportException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

@Service
@Singleton
public final class DataVizFacade {

    private static final Logger LOG = Logger.getLogger(DataVizFacade.class.getName());

    private final DataLoadService    loadService;
    private final FilterService      filterService;
    private final ChartService       chartService;
    private final ExportService      exportService;
    private final ProjectService     projectService;
    private final DatasetObservable  observable;
    private final ReaderFactory      readerFactory;
    private final AppConfig          appConfig;

    @Inject
    public DataVizFacade(DataLoadService    loadService,
                         FilterService      filterService,
                         ChartService       chartService,
                         ExportService      exportService,
                         ProjectService     projectService,
                         DatasetObservable  observable,
                         ReaderFactory      readerFactory,
                         AppConfig          appConfig) {
        this.loadService    = loadService;
        this.filterService  = filterService;
        this.chartService   = chartService;
        this.exportService  = exportService;
        this.projectService = projectService;
        this.observable     = observable;
        this.readerFactory  = readerFactory;
        this.appConfig      = appConfig;
    }

    public String loadFile(Path path,
                           Consumer<DataSet> onSuccess,
                           Consumer<Throwable> onError) {
        return loadService.loadAsync(path,
                dataSet -> {
                    observable.notifyDatasetLoaded(dataSet);
                    onSuccess.accept(dataSet);
                },
                onError);
    }

    public void applyFilters(DataSet dataSet, List<FilterCriteria> criteria) {
        LOG.info(() -> "Applying %d filters to: %s".formatted(criteria.size(), dataSet.getName()));
        var result = filterService.filter(dataSet, criteria);
        observable.notifyFilterApplied(dataSet, result);
    }

    public ChartService.ChartRenderResult buildChart(DataSet dataSet, ChartConfig config) {
        LOG.fine(() -> "Building chart: " + config.getChartType());
        return chartService.buildChart(dataSet, config);
    }

    public void exportDashboard(DashboardSnapshot snapshot,
                                String format,
                                Path output,
                                ExportOptions options) throws ExportException, com.dataviz.service.export.ExportException {
        LOG.info(() -> "Exporting dashboard as %s to: %s".formatted(format, output));
        exportService.export(snapshot, format, output, options);
    }

    public boolean isReaderSupported(String fileNameOrUrl) {
        return readerFactory.isSupported(fileNameOrUrl);
    }

    public List<DataReader> getRegisteredReaders() {
        return readerFactory.getRegisteredReaders();
    }

    public boolean isDarkTheme() {
        return appConfig.darkTheme();
    }

    public String getDefaultExportPath() {
        return appConfig.defaultExportPath();
    }

    public int autoSaveIntervalSec() {
        return appConfig.autoSaveIntervalSec();
    }

    public Set<String> getSupportedExportFormats() {
        return exportService.getSupportedFormats();
    }

    public DataSet getDataSetById(String id) {
        return loadService.getRepository().getById(id);
    }

    public Optional<DataSet> findDataSetById(String id) {
        return loadService.getRepository().findById(id);
    }

    public Optional<DataSet> findDataSetByName(String name) {
        return loadService.getRepository().findAll().stream()
                .filter(ds -> ds.getName() != null && ds.getName().equals(name))
                .findFirst();
    }

    public List<DataSet> listDataSets() {
        return loadService.getRepository().findAll();
    }

    public boolean removeDataSet(String id) {
        Optional<DataSet> maybe = loadService.getRepository().findById(id);
        boolean removed = loadService.getRepository().remove(id);
        maybe.ifPresent(observable::notifyDatasetRemoved);
        return removed;
    }

    public void clearDataSets() {
        loadService.getRepository().clear();
    }

    public int countDataSets() {
        return loadService.getRepository().count();
    }

    public boolean containsDataSet(String id) {
        return loadService.getRepository().contains(id);
    }

    public void cleanRecentProjects() {
        projectService.cleanRecentProjects();
    }

    public List<Path> getRecentProjects() {
        return projectService.getRecentProjects();
    }

    public void saveProject(ProjectState state, Path path) {
        projectService.save(state, path);
    }

    public ProjectState openProject(Path path) {
        return projectService.open(path);
    }
}
