package com.dataviz.service.export;

import com.dataviz.domain.dashboard.DashboardSnapshot;
import com.dataviz.di.annotation.*;

import java.nio.file.Path;
import com.dataviz.service.export.ExportException;
import java.util.*;
import java.util.logging.Logger;

@Service
@Singleton
public final class ExportService {

    private static final Logger LOG = Logger.getLogger(ExportService.class.getName());

    private final Map<String, ExportStrategy> strategies;

    public ExportService() {
        this.strategies = new LinkedHashMap<>();
    }

    @Inject
    public ExportService(List<ExportStrategy> strategies) {
        Map<String, ExportStrategy> map = new LinkedHashMap<>();
        for (ExportStrategy s : strategies) {
            map.put(s.getFormat().toLowerCase(), s);
        }
        this.strategies = new LinkedHashMap<>(map);
        LOG.info("ExportService: registered formats: " + map.keySet());
    }

    public void registerStrategy(String format, ExportStrategy strategy) {
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(strategy, "strategy");
        strategies.put(format.toLowerCase(), strategy);
    }

    public void export(DashboardSnapshot snapshot,
                       String format,
                       Path output,
                       ExportOptions options) throws ExportException {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(options, "options");

        String key = format.toLowerCase();
        ExportStrategy strategy = strategies.get(key);
        if (strategy == null) {
            throw new ExportException("Unsupported format: " + format
                    + ". Available: " + strategies.keySet());
        }
        LOG.info(() -> "Exporting as %s to: %s".formatted(format, output));
        try {
            strategy.export(snapshot, output, options);
        } catch (Exception e) {
            throw new ExportException("Export failed: " + e.getMessage(), e);
        }
    }

    public Set<String> getSupportedFormats() { return strategies.keySet(); }
}