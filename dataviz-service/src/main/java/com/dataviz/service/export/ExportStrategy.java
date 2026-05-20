package com.dataviz.service.export;

import com.dataviz.domain.dashboard.DashboardSnapshot;

import java.nio.file.Path;
import java.rmi.server.ExportException;

public interface ExportStrategy {
    String getFormat();

    String getExtension();

    void export(DashboardSnapshot snapshot, Path output, ExportOptions options)
            throws ExportException, com.dataviz.service.export.ExportException;
}