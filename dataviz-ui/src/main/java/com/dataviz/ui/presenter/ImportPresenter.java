package com.dataviz.ui.presenter;

import com.dataviz.common.event.DataLoadProgressEvent;
import com.dataviz.common.event.DatasetObservable;
import com.dataviz.common.event.EventBus;
import com.dataviz.di.annotation.Component;
import com.dataviz.di.annotation.Inject;
import com.dataviz.domain.model.DataColumn;
import com.dataviz.domain.model.DataSet;
import com.dataviz.service.load.DataLoadService;
import com.dataviz.ui.view.IImportView;
import javafx.application.Platform;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

@Component
public final class ImportPresenter {

    private static final Logger LOG = Logger.getLogger(ImportPresenter.class.getName());
    private static final long   MAX_FILE_SIZE_BYTES = 2L * 1024 * 1024 * 1024;
    private static final int    PREVIEW_ROW_LIMIT   = 100;

    private IImportView view;

    private final DataLoadService   loadService;
    private final EventBus          eventBus;
    private final DatasetObservable datasetObservable;

    private String currentJobId;

    @Inject
    public ImportPresenter(DataLoadService   loadService,
                           EventBus          eventBus,
                           DatasetObservable datasetObservable) {
        this.loadService       = Objects.requireNonNull(loadService);
        this.eventBus          = Objects.requireNonNull(eventBus);
        this.datasetObservable = Objects.requireNonNull(datasetObservable);

        // FIX: підписуємось один раз назавжди — не відписуємось між імпортами,
        // тому прогрес коректно приходить для кожної наступної операції.
        this.eventBus.subscribe(DataLoadProgressEvent.class, this::onProgress);
    }

    public void attachView(IImportView view) {
        this.view = Objects.requireNonNull(view);
    }

    // -------------------------------------------------------------------------
    // Public API (викликається з view)
    // -------------------------------------------------------------------------

    public void onStartImportClicked() {
        Path path = view.getSelectedFilePath();
        if (path == null) {
            view.showError("Файл не вибрано", "Будь ласка, вкажіть файл для завантаження.");
            return;
        }

        long fileSize = path.toFile().length();
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            view.showError("Файл завеликий",
                    "Максимальний розмір файлу — 2 ГБ. Обраний файл: " + formatBytes(fileSize));
            return;
        }

        if (currentJobId != null && loadService.isRunning(currentJobId)) {
            view.showError("Імпорт триває", "Попереднє завдання ще виконується.");
            return;
        }

        if (!loadService.isSupported(path.toString())) {
            view.showError("Непідтримуваний формат",
                    "Підтримуються: CSV, TSV, JSON, NDJSON, JDBC.");
            view.setNextButtonEnabled(true);
            return;
        }

        LOG.info(() -> "User initiated import: " + path);

        view.setNextButtonEnabled(false);
        view.setProgressVisible(true);
        view.setStatusText("Завантаження…");

        currentJobId = loadService.loadAsync(path, this::onLoadSuccess, this::onLoadError);
    }

    public void onStartJdbcImportClicked(Path jdbcPath) {
        view.setProgressVisible(true);
        currentJobId = loadService.loadAsync(jdbcPath, this::onLoadSuccess, this::onLoadError);
    }

    public void onCancelClicked() {
        if (currentJobId != null) {
            loadService.cancel(currentJobId);
            currentJobId = null;
            Platform.runLater(() -> {
                view.setProgressVisible(false);
                view.setNextButtonEnabled(true);
                view.setStatusText("Завантаження скасовано.");
            });
        }
    }

    // -------------------------------------------------------------------------
    // Приватні обробники (worker thread → Platform.runLater для UI)
    // -------------------------------------------------------------------------

    private void onProgress(DataLoadProgressEvent event) {
        // Ігноруємо події від інших задач
        if (!event.getJobId().equals(currentJobId)) return;

        Platform.runLater(() -> {
            view.updateProgress(event.getProgress());
            view.setStatusText("Завантаження… %.0f%%".formatted(event.getProgress() * 100));
        });
    }

    private void onLoadSuccess(DataSet dataSet) {
        LOG.info(() -> "Import success: %s — %,d rows, %d cols"
                .formatted(dataSet.getName(), dataSet.getRowCount(), dataSet.getColumnCount()));

        // FIX 1: будуємо реальні рядки preview (раніше передавався new String[0][0])
        List<DataColumn> cols     = dataSet.getColumns();
        String[]         headers  = cols.stream().map(DataColumn::getName).toArray(String[]::new);
        int              rowCount = Math.min(dataSet.getRowCount(), PREVIEW_ROW_LIMIT);
        String[][]       rows     = new String[rowCount][cols.size()];

        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < cols.size(); c++) {
                Object val = cols.get(c).getValues().get(r);
                rows[r][c] = val != null ? val.toString() : "";
            }
        }

        // FIX 2: повідомляємо DashboardPresenter / FilterPresenter через observable
        datasetObservable.notifyDatasetLoaded(dataSet);

        // FIX 3: UI — строго в JavaFX Application Thread
        Platform.runLater(() -> {
            view.setProgressVisible(false);
            view.setStatusText("Завантажено: %,d рядків, %d стовпців"
                    .formatted(dataSet.getRowCount(), dataSet.getColumnCount()));
            view.showDataPreview(rows, headers);
            view.goToNextStep();
        });
    }

    private void onLoadError(Throwable error) {
        LOG.severe(() -> "Import error: " + error.getMessage());
        Platform.runLater(() -> {
            view.setProgressVisible(false);
            view.setNextButtonEnabled(true);
            view.showError("Помилка завантаження", error.getMessage());
        });
    }

    // -------------------------------------------------------------------------
    // Утиліти
    // -------------------------------------------------------------------------

    private static String formatBytes(long bytes) {
        if (bytes >= 1_073_741_824L) return "%.1f ГБ".formatted(bytes / 1_073_741_824.0);
        if (bytes >= 1_048_576L)     return "%.1f МБ".formatted(bytes / 1_048_576.0);
        return "%.1f КБ".formatted(bytes / 1_024.0);
    }
}