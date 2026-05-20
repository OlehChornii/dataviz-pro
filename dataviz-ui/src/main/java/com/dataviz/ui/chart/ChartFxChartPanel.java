package com.dataviz.ui.chart;

import com.dataviz.domain.chart.ChartConfig;
import com.dataviz.domain.dashboard.DashboardSnapshot;
import com.dataviz.domain.model.DataSet;
import com.dataviz.service.chart.ChartService.ChartRenderResult;
import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public final class ChartFxChartPanel {

    private static final Logger LOG = Logger.getLogger(ChartFxChartPanel.class.getName());

    private final String id;

    /**
     * Вузол, що фактично додається до сцени.
     * Може бути XYChart, StackPane(PieChart), або інший Node.
     */
    private Node        fxNode;

    /**
     * Посилання на de.gsi.chart.Chart, якщо вузол є XYChart.
     * null для PieChart та інших не-Chart вузлів.
     */
    private Chart       fxChart;

    private ChartConfig currentConfig;
    private DataSet     currentDataSet;

    private final VBox  container;

    public ChartFxChartPanel(String id, ChartRenderResult result) {
        this.id = Objects.requireNonNull(id);

        container = new VBox();
        container.getStyleClass().add("chart-panel");
        container.setPrefSize(360, 280);
        container.setMinSize(320, 240);
        container.setPadding(new Insets(0));
        VBox.setVgrow(container, Priority.ALWAYS);

        render(result);
    }

    public void render(ChartRenderResult result) {
        Objects.requireNonNull(result);
        assertFxThread();

        this.currentConfig  = result.getConfig();
        this.currentDataSet = result.getDataSet();

        // Видаляємо старий вузол зі сцени і чистимо ресурси
        if (fxNode != null) {
            // FIX: знімаємо binding перед видаленням, щоб уникнути leak
            if (fxNode instanceof Region oldRegion) {
                oldRegion.prefWidthProperty().unbind();
                oldRegion.prefHeightProperty().unbind();
            }
            container.getChildren().remove(fxNode);
            if (fxChart != null) disposeChart(fxChart);
        }

        // ChartFxFactory.create() повертає Node (може бути XYChart або StackPane з PieChart)
        fxNode = ChartFxFactory.create(
                result.getDataSet(),
                result.getConfig(),
                result.getActiveIndices()
        );

        // Якщо вузол є Chart — зберігаємо посилання для додаткових операцій
        fxChart = (fxNode instanceof Chart c) ? c : null;

        // FIX: bind prefSize для будь-якого Region (XYChart і StackPane з PieChart),
        // а не тільки для Chart. Попередній код викликав fxNode.prefWidth(double) —
        // це getter (обчислює preferred width), а не setter, тому розмір лишався 0×0,
        // що призводило до нескінченного layout-pass і зависання FX Thread.
        if (fxNode instanceof Region region) {
            region.prefWidthProperty().bind(container.widthProperty());
            region.prefHeightProperty().bind(container.heightProperty());
        }

        // VBox.setVgrow забезпечує розтягування вузла по висоті всередині контейнера
        VBox.setVgrow(fxNode, Priority.ALWAYS);

        container.getChildren().add(fxNode);

        LOG.fine(() -> "Rendered chart '%s' (%s)"
                .formatted(currentConfig.getTitle(), currentConfig.getChartType()));
    }

    public void applyFilter(List<Integer> activeIndices) {
        assertFxThread();
        if (fxChart instanceof XYChart xyChart) {
            xyChart.getRenderers().forEach(renderer ->
                    renderer.getDatasets().forEach(ds -> {
                        if (ds instanceof ChartFxDataSetAdapter adapter) {
                            adapter.applyFilter(activeIndices);
                        }
                    })
            );
            return;
        }

        // PieChart та інші не-XY-графіки не підтримують live-фільтрацію через адаптер,
        // тому rebuild з новими індексами.
        if (currentDataSet != null && currentConfig != null) {
            if (fxNode instanceof Region oldRegion) {
                oldRegion.prefWidthProperty().unbind();
                oldRegion.prefHeightProperty().unbind();
            }
            container.getChildren().remove(fxNode);
            if (fxChart != null) disposeChart(fxChart);

            fxNode = ChartFxFactory.create(currentDataSet, currentConfig, activeIndices);
            fxChart = (fxNode instanceof Chart c) ? c : null;
            if (fxNode instanceof Region region) {
                region.prefWidthProperty().bind(container.widthProperty());
                region.prefHeightProperty().bind(container.heightProperty());
            }
            VBox.setVgrow(fxNode, Priority.ALWAYS);
            container.getChildren().add(fxNode);
        }
    }

    public void updateConfig(ChartRenderResult result) {
        Objects.requireNonNull(result);
        assertFxThread();

        ChartConfig newCfg = result.getConfig();
        boolean needFullRebuild = currentConfig == null
                || currentConfig.getChartType() != newCfg.getChartType()
                || !currentConfig.getXColumn().equals(newCfg.getXColumn())
                || !currentConfig.getYColumns().equals(newCfg.getYColumns());

        if (needFullRebuild) {
            render(result);
            return;
        }

        // Легке оновлення — тільки стиль і заголовок
        if (fxNode != null) {
            if (fxChart != null) {
                fxChart.setTitle(newCfg.getTitle());

                if (fxChart instanceof XYChart xyChart && newCfg.getStyle() != null) {
                    xyChart.setLegendVisible(newCfg.getStyle().isShowLegend());
                    xyChart.horizontalGridLinesVisibleProperty()
                            .set(newCfg.getStyle().isShowGrid());
                    xyChart.verticalGridLinesVisibleProperty()
                            .set(newCfg.getStyle().isShowGrid());
                }
            }
        }

        this.currentConfig = newCfg;
    }

    /**
     * Повертає PNG-байти знімка поточного вузла графіку.
     *
     * FIX: попередній код читав пікселі через подвійний цикл getArgb(x, y) —
     * це N×M окремих JNI-викликів прямо на FX Thread, що блокувало UI
     * на сотні мілісекунд або більше для великих графіків.
     * Замінено на batch-виклик getPixels(), який виконується за один перехід.
     */
    public byte[] toSnapshot() {
        // Знімок робимо з fxNode, а не з fxChart —
        // так PieChart у StackPane теж потрапляє на скріншот
        Node target = fxNode != null ? fxNode : (fxChart instanceof Node n ? n : null);
        if (target == null) return new byte[0];

        try {
            int w = Math.max(1, (int) target.getBoundsInLocal().getWidth());
            int h = Math.max(1, (int) target.getBoundsInLocal().getHeight());

            WritableImage fxImage = target.snapshot(
                    new SnapshotParameters(), new WritableImage(w, h));

            // FIX: один batch-виклик замість N×M getArgb() на FX Thread
            int[] pixels = new int[w * h];
            fxImage.getPixelReader().getPixels(
                    0, 0, w, h,
                    PixelFormat.getIntArgbInstance(),
                    pixels, 0, w);

            BufferedImage buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            buffered.setRGB(0, 0, w, h, pixels, 0, w);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(buffered, "PNG", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            LOG.warning("Snapshot failed: " + e.getMessage());
            return new byte[0];
        }
    }

    public DashboardSnapshot toDomainSnapshot() {
        double w = fxNode != null ? fxNode.getBoundsInLocal().getWidth()  : container.getWidth();
        double h = fxNode != null ? fxNode.getBoundsInLocal().getHeight() : container.getHeight();
        return new DashboardSnapshot(id,
                currentConfig != null ? currentConfig.getTitle() : "",
                w, h, toSnapshot(), List.of());
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String      getId()             { return id; }
    public VBox        getContainer()      { return container; }
    /** Повертає de.gsi.chart.Chart якщо це XY-графік, або null для PieChart. */
    public Chart       getFxChart()        { return fxChart; }
    /** Повертає фактичний JavaFX-вузол (завжди не null після render). */
    public Node        getFxNode()         { return fxNode; }
    public ChartConfig getCurrentConfig()  { return currentConfig; }
    public DataSet     getCurrentDataSet() { return currentDataSet; }
    public double      getWidth()          { return container.getWidth(); }
    public double      getHeight()         { return container.getHeight(); }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static void disposeChart(Chart chart) {
        try {
            if (chart instanceof XYChart xyChart) {
                xyChart.getRenderers().forEach(r -> r.getDatasets().clear());
                xyChart.getRenderers().clear();
                xyChart.getPlugins().clear();
            }
        } catch (Exception e) {
            LOG.fine("Chart dispose warning: " + e.getMessage());
        }
    }

    private static void assertFxThread() {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException(
                    "ChartFxChartPanel must be used on FX Application Thread");
        }
    }
}