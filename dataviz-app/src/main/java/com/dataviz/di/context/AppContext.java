package com.dataviz.di.context;

import com.dataviz.common.event.DatasetObservable;
import com.dataviz.di.ServiceLocator;
import com.dataviz.di.container.DIContainer;
import com.dataviz.di.exception.DIException;

import com.dataviz.common.config.AppConfig;
import com.dataviz.common.event.EventBus;
import com.dataviz.common.thread.ThreadPoolManager;
import com.dataviz.data.reader.CsvDataReader;
import com.dataviz.data.reader.DataReader;
import com.dataviz.data.reader.JsonDataReader;
import com.dataviz.data.reader.JdbcDataReader;
import com.dataviz.data.reader.ReaderFactory;
import com.dataviz.data.repository.DataRepository;
import com.dataviz.facade.DataVizFacade;
import com.dataviz.service.export.PdfExportStrategy;
import com.dataviz.service.export.PngExportStrategy;
import com.dataviz.service.export.SvgExportStrategy;
import com.dataviz.service.load.DataLoadService;
import com.dataviz.service.chart.ChartService;
import com.dataviz.service.filter.FilterService;
import com.dataviz.service.export.ExportService;
import com.dataviz.service.project.ProjectService;
import com.dataviz.ui.ServiceLocatorHolder;
import com.dataviz.ui.presenter.ImportPresenter;
import com.dataviz.ui.presenter.DashboardPresenter;
import com.dataviz.ui.presenter.ChartEditorPresenter;
import com.dataviz.ui.presenter.FilterPresenter;
import com.dataviz.ui.view.ChartEditorView;
import com.dataviz.ui.view.DashboardView;
import com.dataviz.ui.view.ImportView;

import java.util.logging.Logger;

public final class AppContext implements ServiceLocator {

    private static final Logger LOG = Logger.getLogger(AppContext.class.getName());

    private static final class Holder {
        static final AppContext INSTANCE = new AppContext();
    }

    public static AppContext getInstance() { return Holder.INSTANCE; }

    private final DIContainer container;
    private volatile boolean initialized = false;

    private AppContext() {
        this.container = new DIContainer();
    }

    public synchronized void initialize() {
        if (initialized) {
            throw new IllegalStateException("AppContext is already initialized");
        }
        LOG.info("Initializing DI container...");

        // ── Синглтони інфраструктури ─────────────────────────────────────────
        AppConfig         appConfig  = AppConfig.getInstance();
        EventBus          eventBus   = EventBus.getInstance();
        ThreadPoolManager threadPool = ThreadPoolManager.getInstance();
        DatasetObservable observable = new DatasetObservable();

        container.registerInstance(AppConfig.class,         appConfig);
        container.registerInstance(EventBus.class,          eventBus);
        container.registerInstance(ThreadPoolManager.class, threadPool);
        container.registerInstance(DatasetObservable.class, observable);

        // ── Читачі даних ─────────────────────────────────────────────────────
        container.register(CsvDataReader.class,  "csv");
        container.register(JsonDataReader.class, "json");
        container.register(JdbcDataReader.class, "jdbc");
        container.register(ReaderFactory.class);
        container.register(DataRepository.class);

        // ── Сервіси ──────────────────────────────────────────────────────────
        container.register(DataLoadService.class);
        container.register(ChartService.class);
        container.register(FilterService.class);
        container.register(ExportService.class);
        container.register(ProjectService.class);

        // ── Стратегії експорту ───────────────────────────────────────────────
        container.register(PngExportStrategy.class, "png");
        container.register(PdfExportStrategy.class, "pdf");
        container.register(SvgExportStrategy.class, "svg");

        // ── Фасад ────────────────────────────────────────────────────────────
        container.register(DataVizFacade.class);

        // ── UI Presenters ────────────────────────────────────────────────────
        container.register(FilterPresenter.class);
        container.register(ImportPresenter.class);
        container.register(ChartEditorPresenter.class);
        container.register(DashboardPresenter.class);   // залежить від FilterPresenter

        // ── UI Views ─────────────────────────────────────────────────────────
        container.register(DashboardView.class);
        container.register(ImportView.class);
        container.register(ChartEditorView.class);

        ServiceLocatorHolder.set(container::resolve);
        initialized = true;
        LOG.info("DI container initialized successfully");
    }

    /**
     * Зупиняє всі компоненти у правильному порядку:
     * 1. @PreDestroy через DIContainer.
     * 2. ThreadPoolManager — явно, бо раніше не викликався при shutdown.
     *
     * FIX: раніше {@code ThreadPoolManager.close()} ніколи не викликався —
     * IO executor і ForkJoinPool залишались живими після виходу з програми.
     */
    public void shutdown() {
        if (!initialized) return;
        LOG.info("Shutting down AppContext...");

        try {
            container.shutdown();          // викликає @PreDestroy на всіх компонентах
        } catch (Exception e) {
            LOG.warning("Error during container shutdown: " + e.getMessage());
        }

        try {
            ThreadPoolManager.getInstance().close();   // FIX: явне закриття пулів
            LOG.info("ThreadPoolManager closed");
        } catch (Exception e) {
            LOG.warning("Error closing ThreadPoolManager: " + e.getMessage());
        }

        initialized = false;
        LOG.info("AppContext shutdown complete");
    }

    // -------------------------------------------------------------------------
    // ServiceLocator
    // -------------------------------------------------------------------------

    @Override
    public <T> T get(Class<T> type) {
        ensureInitialized();
        return container.resolve(type);
    }

    public <T> T getNamed(String name, Class<T> type) {
        ensureInitialized();
        return container.resolveNamed(name, type);
    }

    public DIContainer getContainer() {
        ensureInitialized();
        return container;
    }

    // -------------------------------------------------------------------------

    private void ensureInitialized() {
        if (!initialized)
            throw new IllegalStateException(
                    "AppContext is not initialized. Call AppContext.getInstance().initialize() first.");
    }
}