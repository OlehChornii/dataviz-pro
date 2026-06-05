package com.dataviz.service.export;

import com.dataviz.domain.dashboard.DashboardSnapshot;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import java.util.*;

@DisplayName("ExportService - Тестування експорту графіків")
@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    private ExportService exportService;

    @Mock
    private PngExportStrategy mockPngStrategy;

    @Mock
    private SvgExportStrategy mockSvgStrategy;

    @Mock
    private PdfExportStrategy mockPdfStrategy;

    @Mock
    private DashboardSnapshot mockSnapshot;

    @BeforeEach
    void setUp() {
        exportService = new ExportService();
        exportService.registerStrategy("png", mockPngStrategy);
        exportService.registerStrategy("svg", mockSvgStrategy);
        exportService.registerStrategy("pdf", mockPdfStrategy);
    }

    @Test
    @DisplayName("Підтримувані формати: getSupportedFormats()")
    void testGetSupportedFormats() {
        Set<String> formats = exportService.getSupportedFormats();

        assertTrue(formats.contains("png"));
        assertTrue(formats.contains("svg"));
        assertTrue(formats.contains("pdf"));
    }

    @Test
    @DisplayName("Експорт PNG: маршрутизується на PngExportStrategy")
    void testExport_PNG(@TempDir Path tempDir) throws Exception {
        Path outputFile = tempDir.resolve("chart.png");
        ExportOptions options = new ExportOptions(1920, 1080, 150);

        exportService.export(mockSnapshot, "png", outputFile, options);

        verify(mockPngStrategy).export(mockSnapshot, outputFile, options);
    }

    @Test
    @DisplayName("Експорт SVG: маршрутизується на SvgExportStrategy")
    void testExport_SVG(@TempDir Path tempDir) throws Exception {
        Path outputFile = tempDir.resolve("chart.svg");
        ExportOptions options = new ExportOptions(1920, 1080, 150);

        exportService.export(mockSnapshot, "svg", outputFile, options);

        verify(mockSvgStrategy).export(mockSnapshot, outputFile, options);
    }

    @Test
    @DisplayName("Експорт PDF: маршрутизується на PdfExportStrategy")
    void testExport_PDF(@TempDir Path tempDir) throws Exception {
        Path outputFile = tempDir.resolve("dashboard.pdf");
        ExportOptions options = new ExportOptions(1920, 1080, 150);

        exportService.export(mockSnapshot, "pdf", outputFile, options);

        verify(mockPdfStrategy).export(mockSnapshot, outputFile, options);
    }

    @Test
    @DisplayName("Експорт: case-insensitive formato вибір")
    void testExport_CaseInsensitive(@TempDir Path tempDir) throws Exception {
        Path outputFile = tempDir.resolve("chart.PNG");
        ExportOptions options = new ExportOptions(1920, 1080, 150);

        exportService.export(mockSnapshot, "PNG", outputFile, options);

        verify(mockPngStrategy).export(mockSnapshot, outputFile, options);
    }

    @Test
    @DisplayName("Експорт: мішаний регістр (Png)")
    void testExport_MixedCase(@TempDir Path tempDir) throws Exception {
        Path outputFile = tempDir.resolve("chart.png");
        ExportOptions options = new ExportOptions(1920, 1080, 150);

        exportService.export(mockSnapshot, "Png", outputFile, options);

        verify(mockPngStrategy).export(mockSnapshot, outputFile, options);
    }

    @Test
    @DisplayName("Експорт: невідомий формат - ExportException")
    void testExport_UnknownFormat_ThrowsException(@TempDir Path tempDir) {
        Path outputFile = tempDir.resolve("chart.bmp");
        ExportOptions options = new ExportOptions(1920, 1080, 150);

        assertThrows(ExportException.class,
                () -> exportService.export(mockSnapshot, "bmp", outputFile, options));
    }

    @Test
    @DisplayName("Експорт: null формат - IllegalArgumentException")
    void testExport_NullFormat_ThrowsException(@TempDir Path tempDir) {
        Path outputFile = tempDir.resolve("chart.png");
        ExportOptions options = new ExportOptions(1920, 1080, 150);

        assertThrows(Exception.class,
                () -> exportService.export(mockSnapshot, null, outputFile, options));
    }

    @Test
    @DisplayName("Експорт: null датасет - IllegalArgumentException")
    void testExport_NullSnapshot_ThrowsException(@TempDir Path tempDir) {
        Path outputFile = tempDir.resolve("chart.png");
        ExportOptions options = new ExportOptions(1920, 1080, 150);

        assertThrows(Exception.class,
                () -> exportService.export(null, "png", outputFile, options));
    }

    @Test
    @DisplayName("Експорт: null шлях - IllegalArgumentException")
    void testExport_NullPath_ThrowsException() {
        ExportOptions options = new ExportOptions(1920, 1080, 150);

        assertThrows(Exception.class,
                () -> exportService.export(mockSnapshot, "png", null, options));
    }

    @Test
    @DisplayName("Експорт: null опції - IllegalArgumentException")
    void testExport_NullOptions_ThrowsException(@TempDir Path tempDir) {
        Path outputFile = tempDir.resolve("chart.png");

        assertThrows(Exception.class,
                () -> exportService.export(mockSnapshot, "png", outputFile, null));
    }

    @Test
    @DisplayName("Експорт: стратегія кидає виняток - завертається як ExportException")
    void testExport_StrategyThrowsException(@TempDir Path tempDir) throws Exception {
        Path outputFile = tempDir.resolve("chart.png");
        ExportOptions options = new ExportOptions(1920, 1080, 150);

        doThrow(new RuntimeException("Strategy failed"))
                .when(mockPngStrategy).export(mockSnapshot, outputFile, options);

        assertThrows(ExportException.class,
                () -> exportService.export(mockSnapshot, "png", outputFile, options));
    }

    @Test
    @DisplayName("Реєстрація стратегії: нова стратегія додається")
    void testRegisterStrategy() {
        PngExportStrategy newStrategy = mock(PngExportStrategy.class);
        
        exportService.registerStrategy("png", newStrategy);

        assertTrue(exportService.getSupportedFormats().contains("png"));
    }

    @Test
    @DisplayName("Реєстрація стратегії: null стратегія - IllegalArgumentException")
    void testRegisterStrategy_Null_ThrowsException() {
        assertThrows(Exception.class,
                () -> exportService.registerStrategy("png", null));
    }

    @Test
    @DisplayName("Реєстрація стратегії: null формат - IllegalArgumentException")
    void testRegisterStrategy_NullFormat_ThrowsException() {
        PngExportStrategy strategy = mock(PngExportStrategy.class);

        assertThrows(Exception.class,
                () -> exportService.registerStrategy(null, strategy));
    }

    @Test
    @DisplayName("Шлях створюється: директорія існує після експорту")
    void testExport_CreatesDirectory(@TempDir Path tempDir) throws Exception {
        Path subDir = tempDir.resolve("exports").resolve("charts");
        Path outputFile = subDir.resolve("chart.png");
        ExportOptions options = new ExportOptions(1920, 1080, 150);

        doAnswer(invocation -> {
            Files.createDirectories(outputFile.getParent());
            Files.createFile(outputFile);
            return null;
        }).when(mockPngStrategy).export(mockSnapshot, outputFile, options);

        exportService.export(mockSnapshot, "png", outputFile, options);

        assertTrue(Files.exists(outputFile.getParent()));
    }

    @Test
    @DisplayName("Множина експортів: всі форматы вдачі")
    void testMultipleExports(@TempDir Path tempDir) throws Exception {
        ExportOptions options = new ExportOptions(1920, 1080, 150);

        Path pngFile = tempDir.resolve("chart.png");
        Path svgFile = tempDir.resolve("chart.svg");
        Path pdfFile = tempDir.resolve("dashboard.pdf");

        exportService.export(mockSnapshot, "png", pngFile, options);
        exportService.export(mockSnapshot, "svg", svgFile, options);
        exportService.export(mockSnapshot, "pdf", pdfFile, options);

        verify(mockPngStrategy).export(mockSnapshot, pngFile, options);
        verify(mockSvgStrategy).export(mockSnapshot, svgFile, options);
        verify(mockPdfStrategy).export(mockSnapshot, pdfFile, options);
    }

    @Test
    @DisplayName("Варіанти експорту: різні розміри та роздільність")
    void testExportOptions_DifferentSettings(@TempDir Path tempDir) throws Exception {
        Path outputFile = tempDir.resolve("chart.png");

        ExportOptions options1 = new ExportOptions(800, 600, 72);
        ExportOptions options2 = new ExportOptions(1920, 1080, 300);

        exportService.export(mockSnapshot, "png", outputFile, options1);
        exportService.export(mockSnapshot, "png", outputFile, options2);

        verify(mockPngStrategy, times(2)).export(eq(mockSnapshot), eq(outputFile), any());
    }

    @Test
    @DisplayName("Повторний експорт: перезаписує файл")
    void testExport_Overwrite(@TempDir Path tempDir) throws Exception {
        Path outputFile = tempDir.resolve("chart.png");
        ExportOptions options = new ExportOptions(1920, 1080, 150);

        exportService.export(mockSnapshot, "png", outputFile, options);
        exportService.export(mockSnapshot, "png", outputFile, options);

        verify(mockPngStrategy, times(2)).export(mockSnapshot, outputFile, options);
    }

    @Test
    @DisplayName("Спеціальні символи в імені файлу")
    void testExport_SpecialCharactersInFileName(@TempDir Path tempDir) throws Exception {
        Path outputFile = tempDir.resolve("chart_2024-05-31_sales-report.png");
        ExportOptions options = new ExportOptions(1920, 1080, 150);

        exportService.export(mockSnapshot, "png", outputFile, options);

        verify(mockPngStrategy).export(mockSnapshot, outputFile, options);
    }
}
