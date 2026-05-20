package com.dataviz.service.export;

import com.dataviz.di.annotation.Component;
import com.dataviz.di.annotation.Singleton;
import com.dataviz.domain.dashboard.DashboardSnapshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component("png")
@Singleton
public final class PngExportStrategy implements ExportStrategy {

    private static final byte[] PNG_SIGNATURE =
            {(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    @Override public String getFormat()    { return "png"; }
    @Override public String getExtension() { return ".png"; }

    @Override
    public void export(DashboardSnapshot snapshot, Path output, ExportOptions options)
            throws ExportException {

        byte[] bytes = snapshot.getPngBytes();
        if (bytes == null || bytes.length == 0) {
            bytes = snapshot.allLeaves().stream()
                    .map(DashboardSnapshot::getPngBytes)
                    .filter(b -> b != null && b.length > 0)
                    .findFirst()
                    .orElse(null);
        }

        if (bytes == null || bytes.length == 0)
            throw new ExportException("Snapshot не містить PNG-даних");

        if (!isPng(bytes))
            throw new ExportException(
                    "Snapshot не є валідним PNG (перевірте toSnapshot() у ChartFxChartPanel)");

        try {
            Files.write(output, bytes);
        } catch (IOException e) {
            throw new ExportException("PNG export failed: " + e.getMessage(), e);
        }
    }

    private static boolean isPng(byte[] bytes) {
        if (bytes.length < PNG_SIGNATURE.length) return false;
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (bytes[i] != PNG_SIGNATURE[i]) return false;
        }
        return true;
    }
}