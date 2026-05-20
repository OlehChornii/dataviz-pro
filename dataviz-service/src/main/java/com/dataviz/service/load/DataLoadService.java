package com.dataviz.service.load;

import com.dataviz.common.event.DataLoadProgressEvent;
import com.dataviz.common.event.EventBus;
import com.dataviz.common.thread.ThreadPoolManager;
import com.dataviz.data.reader.DataReader;
import com.dataviz.data.reader.ReaderFactory;
import com.dataviz.data.repository.DataRepository;
import com.dataviz.domain.model.DataSet;
import com.dataviz.di.annotation.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

@Service
@Singleton
public class DataLoadService {

    private static final Logger LOG = Logger.getLogger(DataLoadService.class.getName());

    private final ReaderFactory    readerFactory;
    private final DataRepository   repository;
    private final EventBus         eventBus;
    private final ExecutorService  executor;

    public DataRepository getRepository() {
        return repository;
    }

    public Optional<DataSet> findDataSetById(String id) {
        return repository.findById(id);
    }

    public List<DataSet> listDataSets() {
        return repository.findAll();
    }

    public boolean isSupported(String fileNameOrUrl) {
        return readerFactory.isSupported(fileNameOrUrl);
    }

    public List<DataReader> getRegisteredReaders() {
        return readerFactory.getRegisteredReaders();
    }

    public boolean removeDataSet(String id) {
        Optional<DataSet> exists = repository.findById(id);
        boolean result = repository.remove(id);
        return result;
    }

    private final Map<String, Future<?>> activeJobs = new ConcurrentHashMap<>();

    @Inject
    public DataLoadService(ReaderFactory readerFactory,
                           DataRepository repository,
                           EventBus eventBus) {
        this.readerFactory = readerFactory;
        this.repository    = repository;
        this.eventBus      = eventBus;
        this.executor      = ThreadPoolManager.getInstance().getIoExecutor();

        ThreadPoolManager.getInstance().getComputePool();
    }

    public String loadAsync(Path path,
                            Consumer<DataSet> onSuccess,
                            Consumer<Throwable> onError) {
        String jobId = UUID.randomUUID().toString();
        LOG.info(() -> "Scheduling load job %s: %s".formatted(jobId, path));

        Future<?> future = executor.submit(() -> executeLoad(jobId, path, onSuccess, onError));
        activeJobs.put(jobId, future);
        return jobId;
    }

    public void cancel(String jobId) {
        if (jobId == null) return;
        Future<?> future = activeJobs.remove(jobId);
        if (future != null && !future.isDone()) {
            future.cancel(true); // передає interrupt у потік читання
            LOG.info(() -> "Cancelled load job: " + jobId);
        }
    }

    public void cancelAll() {
        activeJobs.forEach((id, future) -> {
            if (!future.isDone()) future.cancel(true);
        });
        activeJobs.clear();
        LOG.info("All load jobs cancelled");
    }

    public boolean isRunning(String jobId) {
        Future<?> f = activeJobs.get(jobId);
        return f != null && !f.isDone();
    }

    private void executeLoad(String jobId,
                             Path path,
                             Consumer<DataSet> onSuccess,
                             Consumer<Throwable> onError) {
        try {
            DataReader reader = readerFactory.getReader(path.toString());

            reader.setProgressCallback(progress -> {
                if (Thread.currentThread().isInterrupted()) return;
                eventBus.publish(DataLoadProgressEvent.ofUnknownSize(jobId, progress));
            });

            DataSet dataSet = reader.read(path);

            if (Thread.currentThread().isInterrupted()) {
                LOG.info(() -> "Job %s interrupted after read".formatted(jobId));
                return;
            }

            repository.save(dataSet);
            eventBus.publish(DataLoadProgressEvent.ofUnknownSize(jobId, 1.0));
            LOG.info(() -> "Job %s completed: %,d rows".formatted(jobId, dataSet.getRowCount()));
            onSuccess.accept(dataSet);

        } catch (CancellationException e) {
            Thread.currentThread().interrupt();
            LOG.info(() -> "Job %s was cancelled".formatted(jobId));
        } catch (Exception e) {
            LOG.severe(() -> "Job %s failed: %s".formatted(jobId, e.getMessage()));
            onError.accept(e);
        } finally {
            activeJobs.remove(jobId);
        }
    }

    @PreDestroy
    private void shutdown() {
        cancelAll();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}