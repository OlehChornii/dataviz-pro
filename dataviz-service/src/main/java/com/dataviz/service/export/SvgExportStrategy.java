package com.dataviz.service.export;

import com.dataviz.di.annotation.Component;
import com.dataviz.di.annotation.Singleton;
import com.dataviz.domain.dashboard.DashboardSnapshot;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

@Component("svg")
@Singleton
public final class SvgExportStrategy implements ExportStrategy {

    private static final Logger LOG = Logger.getLogger(SvgExportStrategy.class.getName());

    @Override public String getFormat()    { return "svg"; }
    @Override public String getExtension() { return ".svg"; }

    @Override
    public void export(DashboardSnapshot snapshot, Path output, ExportOptions options)
            throws ExportException {

        List<DashboardSnapshot> leaves = snapshot.allLeaves();
        if (leaves.isEmpty())
            throw new ExportException("Snapshot не містить панелей");

        double totalW = snapshot.getWidth()  > 0 ? snapshot.getWidth()  : 800;
        double totalH = snapshot.getHeight() > 0 ? snapshot.getHeight() : 600;

        try (BufferedWriter writer = Files.newBufferedWriter(output)) {

            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<svg xmlns=\"http://www.w3.org/2000/svg\" ");
            writer.write("xmlns:xlink=\"http://www.w3.org/1999/xlink\" ");
            writer.write("width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\">\n"
                    .formatted((int) totalW, (int) totalH, (int) totalW, (int) totalH));

            writer.write("<defs>\n");
            writer.write("  <style type=\"text/css\">\n");
            writer.write("    text { font-family: Arial, sans-serif; font-size: 12px; }\n");
            writer.write("  </style>\n");
            writer.write("</defs>\n");

            double offsetY = 0;
            for (DashboardSnapshot panel : leaves) {
                byte[] pngBytes = panel.getPngBytes();
                if (pngBytes == null || pngBytes.length == 0) {
                    LOG.warning("Пропущено панель '%s': відсутні PNG-байти"
                            .formatted(panel.getTitle()));
                    continue;
                }

                double pw = panel.getWidth()  > 0 ? panel.getWidth()  : totalW;
                double ph = panel.getHeight() > 0 ? panel.getHeight() : totalH;

                String base64 = Base64.getEncoder().encodeToString(pngBytes);

                if (!panel.getTitle().isBlank()) {
                    writer.write("  <text x=\"0\" y=\"%d\">%s</text>\n"
                            .formatted((int) offsetY + 14,
                                    escapeXml(panel.getTitle())));
                    offsetY += 18;
                }

                writer.write("  <image x=\"0\" y=\"%d\" width=\"%d\" height=\"%d\" "
                        .formatted((int) offsetY, (int) pw, (int) ph));
                writer.write("xlink:href=\"data:image/png;base64,");
                writer.write(base64);
                writer.write("\" />\n");

                offsetY += ph + 8;
            }

            writer.write("</svg>\n");

        } catch (IOException e) {
            throw new ExportException("SVG export failed: " + e.getMessage(), e);
        }
    }

    private static String escapeXml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}