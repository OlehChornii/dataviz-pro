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

    @Inject
    public ExportService(List<ExportStrategy> strategies) {
        Map<String, ExportStrategy> map = new LinkedHashMap<>();
        for (ExportStrategy s : strategies) {
            map.put(s.getFormat().toLowerCase(), s);
        }
        this.strategies = Collections.unmodifiableMap(map);
        LOG.info("ExportService: registered formats: " + map.keySet());
    }

    public void export(DashboardSnapshot snapshot,
                       String format,
                       Path output,
                       ExportOptions options) throws ExportException, com.dataviz.service.export.ExportException {
        String key = format.toLowerCase();
        ExportStrategy strategy = strategies.get(key);
        if (strategy == null) {
            throw new ExportException("Unsupported format: " + format
                    + ". Available: " + strategies.keySet());
        }
        LOG.info(() -> "Exporting as %s to: %s".formatted(format, output));
        try {
            strategy.export(snapshot, output, options);
        } catch (java.rmi.server.ExportException e) {
            throw new RuntimeException(e);
        }
    }

    public Set<String> getSupportedFormats() { return strategies.keySet(); }
}