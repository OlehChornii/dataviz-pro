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

    private Node        fxNode;
    private Chart       fxChart;

    private ChartConfig currentConfig;
    private DataSet     currentDataSet;

    private final VBox  container;

    public ChartFxChartPanel(String id, ChartRenderResult result) {
        this.id = Objects.requireNonNull(id);

        container = new VBox();
        container.getStyleClass().add("chart-panel");
        container.setMinSize(120, 90);
        container.setMaxWidth(Double.MAX_VALUE);
        container.setMaxHeight(Double.MAX_VALUE);
        container.setPadding(new Insets(0));
        VBox.setVgrow(container, Priority.ALWAYS);

        render(result);
    }

    public void render(ChartRenderResult result) {
        Objects.requireNonNull(result);
        assertFxThread();

        this.currentConfig  = result.getConfig();
        this.currentDataSet = result.getDataSet();

        if (fxNode != null) {
            if (fxNode instanceof Region oldRegion) {
                oldRegion.prefWidthProperty().unbind();
                oldRegion.prefHeightProperty().unbind();
            }
            container.getChildren().remove(fxNode);
            if (fxChart != null) disposeChart(fxChart);
        }

        try {
            fxNode = ChartFxFactory.create(
                    result.getDataSet(),
                    result.getConfig(),
                    result.getActiveIndices()
            );

            fxChart = (fxNode instanceof Chart c) ? c : null;

            if (fxNode instanceof Region region) {
                region.prefWidthProperty().bind(container.widthProperty());
                region.prefHeightProperty().bind(container.heightProperty());
                region.setMaxWidth(Double.MAX_VALUE);
                region.setMaxHeight(Double.MAX_VALUE);
            }

            VBox.setVgrow(fxNode, Priority.ALWAYS);

            container.getChildren().add(fxNode);

            LOG.fine(() -> "Rendered chart '%s' (%s)"
                    .formatted(currentConfig.getTitle(), currentConfig.getChartType()));
        } catch (Exception e) {
            LOG.severe("Failed to render chart '%s' (%s): %s"
                    .formatted(currentConfig.getTitle(), currentConfig.getChartType(), e.getMessage()));
            e.printStackTrace();
            
            fxNode = createErrorNode("Failed to render: " + e.getMessage());
            container.getChildren().add(fxNode);
        }
    }

    private javafx.scene.Node createErrorNode(String message) {
        javafx.scene.control.Label errorLabel = new javafx.scene.control.Label(message);
        errorLabel.setStyle("-fx-text-fill: #ff0000; -fx-font-size: 12; -fx-padding: 10;");
        errorLabel.setWrapText(true);
        return errorLabel;
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
                region.setMaxWidth(Double.MAX_VALUE);
                region.setMaxHeight(Double.MAX_VALUE);
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
                || !Objects.equals(currentConfig.getXColumn(), newCfg.getXColumn())
                || !Objects.equals(currentConfig.getYColumns(), newCfg.getYColumns())
                || !Objects.equals(currentConfig.getStyle(), newCfg.getStyle());

        if (needFullRebuild) {
            render(result);
            return;
        }

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
            } else if (fxNode instanceof javafx.scene.chart.PieChart pc) {
                pc.setTitle(newCfg.getTitle() != null ? newCfg.getTitle() : "");
                if (newCfg.getStyle() != null) {
                    pc.setLegendVisible(newCfg.getStyle().isShowLegend());
                    pc.setLabelsVisible(newCfg.getStyle().isSliceLabels());
                }
            }
        }

        this.currentConfig = newCfg;
    }

    public byte[] toSnapshot() {
        Node target = fxNode != null ? fxNode : (fxChart instanceof Node n ? n : null);
        if (target == null) return new byte[0];

        try {
            int w = Math.max(1, (int) target.getBoundsInLocal().getWidth());
            int h = Math.max(1, (int) target.getBoundsInLocal().getHeight());

            WritableImage fxImage = target.snapshot(
                    new SnapshotParameters(), new WritableImage(w, h));

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

    public String      getId()             { return id; }
    public VBox        getContainer()      { return container; }
    public Chart       getFxChart()        { return fxChart; }
    public Node        getFxNode()         { return fxNode; }
    public ChartConfig getCurrentConfig()  { return currentConfig; }
    public DataSet     getCurrentDataSet() { return currentDataSet; }
    public double      getWidth()          { return container.getWidth(); }
    public double      getHeight()         { return container.getHeight(); }

    private static void disposeChart(Chart chart) {
        try {
            if (chart instanceof XYChart xyChart) {
                xyChart.getRenderers().forEach(r -> r.getDatasets().clear());
                xyChart.getRenderers().clear();
                xyChart.getPlugins().removeIf(Objects::isNull);
                if (!xyChart.getPlugins().isEmpty()) {
                    xyChart.getPlugins().clear();
                }
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