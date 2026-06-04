package com.dataviz.common.thread;

import com.dataviz.common.config.AppConfig;
import java.util.concurrent.*;

public final class ThreadPoolManager implements AutoCloseable {

    private static volatile ThreadPoolManager instance;

    private final ExecutorService ioExecutor;
    private final ForkJoinPool    computePool;

    private ThreadPoolManager(AppConfig cfg) {
        this.ioExecutor  = Executors.newFixedThreadPool(
                cfg.ioThreads(),
                Thread.ofVirtual().name("dataviz-io-", 0).factory()
        );
        this.computePool = new ForkJoinPool(cfg.computeThreads());
    }

    public static ThreadPoolManager getInstance() {
        if (instance == null) {
            synchronized (ThreadPoolManager.class) {
                if (instance == null)
                    instance = new ThreadPoolManager(AppConfig.getInstance());
            }
        }
        return instance;
    }

    public ExecutorService getIoExecutor()  { return ioExecutor; }

    public ForkJoinPool    getComputePool() { return computePool; }

    @Override
    public void close() {
        ioExecutor.shutdown();
        computePool.shutdown();
        try {
            if (!ioExecutor.awaitTermination(10, TimeUnit.SECONDS)) ioExecutor.shutdownNow();
            if (!computePool.awaitTermination(10, TimeUnit.SECONDS)) computePool.shutdownNow();
        } catch (InterruptedException e) {
            ioExecutor.shutdownNow();
            computePool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}