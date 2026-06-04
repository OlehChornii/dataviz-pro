package com.dataviz.service.export;

import com.dataviz.di.annotation.Component;
import com.dataviz.di.annotation.Singleton;
import com.dataviz.domain.dashboard.DashboardSnapshot;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

@Component("pdf")
@Singleton
public final class PdfExportStrategy implements ExportStrategy {

    private static final Logger LOG = Logger.getLogger(PdfExportStrategy.class.getName());

    @Override public String getFormat()    { return "pdf"; }
    @Override public String getExtension() { return ".pdf"; }

    @Override
    public void export(DashboardSnapshot snapshot, Path output, ExportOptions options)
            throws ExportException {

        List<DashboardSnapshot> leaves = snapshot.allLeaves();
        if (leaves.isEmpty())
            throw new ExportException("Snapshot не містить панелей");

        try (PdfWriter   writer = new PdfWriter(output.toFile());
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document    doc    = new Document(pdfDoc)) {

            pdfDoc.getDocumentInfo().setTitle(options.getTitle());
            pdfDoc.getDocumentInfo().setAuthor(options.getAuthor());

            for (DashboardSnapshot panel : leaves) {
                byte[] pngBytes = panel.getPngBytes();
                if (pngBytes == null || pngBytes.length == 0) {
                    LOG.warning("Пропущено панель '%s': відсутні PNG-байти"
                            .formatted(panel.getTitle()));
                    continue;
                }

                float pageW = (float) Math.max(panel.getWidth(),  100);
                float pageH = (float) Math.max(panel.getHeight(), 100);
                pdfDoc.addNewPage(new PageSize(pageW, pageH));

                if (!panel.getTitle().isBlank())
                    doc.add(new Paragraph(panel.getTitle()).setBold().setFontSize(12));

                Image img = new Image(ImageDataFactory.create(pngBytes));
                img.setWidth(pageW).setAutoScale(true);
                doc.add(img);
            }

        } catch (Exception e) {
            throw new ExportException("PDF export failed: " + e.getMessage(), e);
        }
    }
}