package com.dataviz.ui.chart;

import com.dataviz.domain.chart.ChartConfig;
import com.dataviz.domain.chart.ChartStyle;
import com.dataviz.domain.model.DataColumn;
import com.dataviz.domain.model.DataSet;
import com.dataviz.service.chart.ChartService.ChartRenderResult;
import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.CategoryAxis;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.DataPointTooltip;
import de.gsi.chart.plugins.Panner;
import de.gsi.chart.plugins.Screenshot;
import de.gsi.chart.plugins.TableViewer;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.LineStyle;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.chart.renderer.spi.ReducingLineRenderer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.DefaultXYZDataset;
import java.awt.Paint;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Перетворює {@link ChartRenderResult} у JavaFX-вузол для відображення.
 *
 * Важливо:
 * - Колір серій тут не прокидається через metaInfo("color"), бо для ChartFX це не є надійним способом стилізації.
 * - Стилі серій мають керуватися CSS-класами default-colorX.
 * - Додано мінімальні розміри, щоб графіки не стискалися в картках.
 */
public final class ChartFxFactory {

    private static final int MIN_CHART_WIDTH = 180;
    private static final int MIN_CHART_HEIGHT = 120;
    private static final int PREF_CHART_HEIGHT = 260;

    private ChartFxFactory() {
    }

    // -------------------------------------------------------------------------
    // Головна точка входу
    // -------------------------------------------------------------------------

    /**
     * Будує Node з {@link ChartRenderResult}.
     * Делегує до конкретного будівника за {@link ChartConfig.ChartType}.
     */
    public static Node create(ChartRenderResult result) {
        Objects.requireNonNull(result, "result must not be null");

        DataSet ds = result.getDataSet();
        ChartConfig config = result.getConfig();
        List<Integer> indices = result.getActiveIndices();

        return switch (config.getChartType()) {
            case LINE -> buildLineChart(ds, config, indices, false);
            case AREA -> buildLineChart(ds, config, indices, true);
            case BAR -> buildBarChart(ds, config, indices);
            case SCATTER -> buildScatterChart(ds, config, indices);
            case PIE -> buildPieChart(ds, config, indices);
            case HEATMAP -> buildHeatmap(ds, config, indices);
        };
    }

    /**
     * Альтернативний виклик — для зворотньої сумісності зі старим кодом,
     * що передає DataSet і ChartConfig напряму.
     */
    public static Node create(DataSet dataSet, ChartConfig config, List<Integer> activeIndices) {
        Objects.requireNonNull(dataSet, "dataSet must not be null");
        Objects.requireNonNull(config, "config must not be null");

        return switch (config.getChartType()) {
            case LINE -> buildLineChart(dataSet, config, activeIndices, false);
            case AREA -> buildLineChart(dataSet, config, activeIndices, true);
            case BAR -> buildBarChart(dataSet, config, activeIndices);
            case SCATTER -> buildScatterChart(dataSet, config, activeIndices);
            case PIE -> buildPieChart(dataSet, config, activeIndices);
            case HEATMAP -> buildHeatmap(dataSet, config, activeIndices);
        };
    }

    // -------------------------------------------------------------------------
    // Line / Area
    // -------------------------------------------------------------------------

    private static XYChart buildLineChart(DataSet ds, ChartConfig cfg, List<Integer> indices, boolean filled) {
        DefaultNumericAxis xAxis = buildNumericAxis(cfg.getXLabel(), cfg.getXColumn());
        DefaultNumericAxis yAxis = buildNumericAxis(cfg.getYLabel(), "");

        XYChart chart = new XYChart(xAxis, yAxis);
        applyBaseStyle(chart, cfg);

        final de.gsi.chart.renderer.Renderer renderer;
        if (filled) {
            ErrorDataSetRenderer areaRenderer = new ErrorDataSetRenderer();
            areaRenderer.setPolyLineStyle(LineStyle.AREA);
            areaRenderer.setDrawMarker(false);
            chart.getRenderers().setAll(areaRenderer);
            renderer = areaRenderer;
        } else {
            ReducingLineRenderer lineRenderer = new ReducingLineRenderer();
            lineRenderer.setMaxPoints(2000);
            // lineRenderer.setDrawMarker(false);
            chart.getRenderers().setAll(lineRenderer);
            renderer = lineRenderer;
        }

        addInteractionPlugins(chart, cfg);

        for (int i = 0; i < cfg.getYColumns().size(); i++) {
            String yColName = cfg.getYColumns().get(i);
            if (!ds.hasColumn(yColName)) {
                continue;
            }

            DataColumn xCol = ds.hasColumn(cfg.getXColumn()) ? ds.getColumn(cfg.getXColumn()) : null;
            DataColumn yCol = ds.getColumn(yColName);

            ChartFxDataSetAdapter adapter = ChartFxDataSetAdapter.from(yColName, xCol, yCol, indices);
            renderer.getDatasets().add(adapter);
        }

        return chart;
    }

    // -------------------------------------------------------------------------
    // Bar
    // -------------------------------------------------------------------------

    private static XYChart buildBarChart(DataSet ds, ChartConfig cfg, List<Integer> indices) {
        DataColumn xCol = ds.hasColumn(cfg.getXColumn()) ? ds.getColumn(cfg.getXColumn()) : null;

        final boolean categorical = xCol != null
                && xCol.getType() == com.dataviz.domain.model.ColumnType.CATEGORICAL;

        var xAxis = categorical
                ? new CategoryAxis(cfg.getXColumn())
                : buildNumericAxis(cfg.getXLabel(), cfg.getXColumn());

        DefaultNumericAxis yAxis = buildNumericAxis(cfg.getYLabel(), "");

        XYChart chart = new XYChart(xAxis, yAxis);
        applyBaseStyle(chart, cfg);

        ErrorDataSetRenderer renderer = new ErrorDataSetRenderer();
        renderer.setDrawBars(true);
        renderer.setDrawMarker(false);
        renderer.setPolyLineStyle(LineStyle.NONE);
        renderer.setBarWidth(computeBarWidth(cfg.getYColumns().size()));

        chart.getRenderers().setAll(renderer);
        addInteractionPlugins(chart, cfg);

        for (int i = 0; i < cfg.getYColumns().size(); i++) {
            String yColName = cfg.getYColumns().get(i);
            if (xCol == null || !ds.hasColumn(yColName)) {
                continue;
            }

            ChartFxDataSetAdapter adapter = ChartFxDataSetAdapter.from(
                    yColName,
                    xCol,
                    ds.getColumn(yColName),
                    indices
            );
            renderer.getDatasets().add(adapter);
        }

        return chart;
    }

    // -------------------------------------------------------------------------
    // Scatter
    // -------------------------------------------------------------------------

    private static XYChart buildScatterChart(DataSet ds, ChartConfig cfg, List<Integer> indices) {
        DefaultNumericAxis xAxis = buildNumericAxis(cfg.getXLabel(), cfg.getXColumn());
        DefaultNumericAxis yAxis = buildNumericAxis(cfg.getYLabel(), "");

        XYChart chart = new XYChart(xAxis, yAxis);
        applyBaseStyle(chart, cfg);

        ErrorDataSetRenderer renderer = new ErrorDataSetRenderer();
        renderer.setPolyLineStyle(LineStyle.NONE);
        renderer.setDrawMarker(true);
        renderer.setMarkerSize(7);

        chart.getRenderers().setAll(renderer);
        addInteractionPlugins(chart, cfg);

        for (int i = 0; i < cfg.getYColumns().size(); i++) {
            String yColName = cfg.getYColumns().get(i);
            if (!ds.hasColumn(cfg.getXColumn()) || !ds.hasColumn(yColName)) {
                continue;
            }

            ChartFxDataSetAdapter adapter = ChartFxDataSetAdapter.from(
                    yColName,
                    ds.getColumn(cfg.getXColumn()),
                    ds.getColumn(yColName),
                    indices
            );
            renderer.getDatasets().add(adapter);
        }

        return chart;
    }

    // -------------------------------------------------------------------------
    // Pie
    // -------------------------------------------------------------------------

    private static Node buildPieChart(DataSet ds, ChartConfig cfg, List<Integer> activeIndices) {
        DefaultPieDataset dataset = new DefaultPieDataset();

        if (!cfg.getYColumns().isEmpty() && ds.hasColumn(cfg.getYColumns().get(0))) {
            DataColumn valueCol = ds.getColumn(cfg.getYColumns().get(0));
            DataColumn labelCol = ds.hasColumn(cfg.getXColumn()) ? ds.getColumn(cfg.getXColumn()) : null;

            if (labelCol != null) {
                Map<String, Double> aggregated = new LinkedHashMap<>();
                int limit = Math.min(labelCol.size(), valueCol.size());

                if (activeIndices != null) {
                    for (int idx : activeIndices) {
                        if (idx < 0 || idx >= limit) {
                            continue;
                        }
                        addPieValue(aggregated, labelCol, valueCol, idx);
                    }
                } else {
                    for (int i = 0; i < limit; i++) {
                        addPieValue(aggregated, labelCol, valueCol, i);
                    }
                }

                final int maxSlices = 25;
                if (aggregated.size() > maxSlices) {
                    var sorted = aggregated.entrySet().stream()
                            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                            .toList();

                    double otherSum = sorted.stream()
                            .skip(maxSlices - 1)
                            .mapToDouble(Map.Entry::getValue)
                            .sum();

                    Map<String, Double> trimmed = new LinkedHashMap<>();
                    sorted.stream()
                            .limit(maxSlices - 1)
                            .forEach(e -> trimmed.put(e.getKey(), e.getValue()));
                    trimmed.put("Other", otherSum);
                    aggregated = trimmed;
                }

                aggregated.forEach(dataset::setValue);
            }
        }

        JFreeChart chart = ChartFactory.createPieChart(
                cfg.getTitle(),
                dataset,
                cfg.getStyle() == null || cfg.getStyle().isShowLegend(),
                true,
                false);

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(java.awt.Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setLabelGenerator(new org.jfree.chart.labels.StandardPieSectionLabelGenerator("{0}: {1} ({2})"));
        plot.setSimpleLabels(true);
        plot.setInteriorGap(0.04);

        ChartViewer viewer = new ChartViewer(chart);
        viewer.setMinSize(MIN_CHART_WIDTH, MIN_CHART_WIDTH);
        viewer.setPrefSize(PREF_CHART_HEIGHT, PREF_CHART_HEIGHT);
        viewer.setStyle("-fx-background-color: transparent;");
        return viewer;
    }

    private static Node buildHeatmap(DataSet ds, ChartConfig cfg, List<Integer> activeIndices) {
        List<String> rowNames = cfg.getYColumns().stream()
                .filter(ds::hasColumn)
                .toList();

        List<Integer> rowIndices = activeIndices != null
                ? activeIndices
                : IntStream.range(0, ds.getColumns().isEmpty() ? 0 : ds.getColumns().get(0).size()).boxed().toList();

        DataColumn xCol = ds.hasColumn(cfg.getXColumn()) ? ds.getColumn(cfg.getXColumn()) : null;
        List<String> xValues = xCol != null
                ? xCol.getValues().stream().map(v -> v == null ? "" : v.toString()).toList()
                : IntStream.range(0, rowIndices.size()).mapToObj(Integer::toString).toList();

        if (rowNames.isEmpty() || xValues.isEmpty()) {
            Label placeholder = new Label("Heatmap requires X column and at least one Y series.");
            placeholder.getStyleClass().add("chart-placeholder");
            return new StackPane(placeholder);
        }

        List<String> orderedX = xValues.stream().distinct().toList();
        if (orderedX.isEmpty()) {
            Label placeholder = new Label("Heatmap requires X column and at least one Y series.");
            placeholder.getStyleClass().add("chart-placeholder");
            return new StackPane(placeholder);
        }

        double minValue = Double.POSITIVE_INFINITY;
        double maxValue = Double.NEGATIVE_INFINITY;
        var dataPoints = new java.util.ArrayList<double[]>();

        for (int rowIndex = 0; rowIndex < rowNames.size(); rowIndex++) {
            DataColumn yCol = ds.getColumn(rowNames.get(rowIndex));
            LinkedHashMap<String, java.util.List<Double>> groups = new LinkedHashMap<>();

            for (int dataIndex : rowIndices) {
                if (dataIndex < 0 || dataIndex >= yCol.size() || dataIndex >= xValues.size()) {
                    continue;
                }
                String xKey = xValues.get(dataIndex);
                Object rawY = yCol.getValues().get(dataIndex);
                if (rawY == null) {
                    continue;
                }
                Double value;
                try {
                    value = ((Number) rawY).doubleValue();
                } catch (ClassCastException ex) {
                    try {
                        value = Double.parseDouble(rawY.toString());
                    } catch (NumberFormatException ignore) {
                        continue;
                    }
                }
                groups.computeIfAbsent(xKey, k -> new java.util.ArrayList<>()).add(value);
            }

            for (int col = 0; col < orderedX.size(); col++) {
                String xKey = orderedX.get(col);
                var values = groups.get(xKey);
                if (values == null || values.isEmpty()) {
                    continue;
                }
                double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
                if (!Double.isFinite(avg)) {
                    continue;
                }
                minValue = Math.min(minValue, avg);
                maxValue = Math.max(maxValue, avg);
                dataPoints.add(new double[]{col, rowIndex, avg});
            }
        }

        if (dataPoints.isEmpty()) {
            Label placeholder = new Label("Heatmap requires numeric Y values.");
            placeholder.getStyleClass().add("chart-placeholder");
            return new StackPane(placeholder);
        }

        DefaultXYZDataset dataset = new DefaultXYZDataset();
        double[] xs = new double[dataPoints.size()];
        double[] ys = new double[dataPoints.size()];
        double[] zs = new double[dataPoints.size()];
        for (int i = 0; i < dataPoints.size(); i++) {
            xs[i] = dataPoints.get(i)[0];
            ys[i] = dataPoints.get(i)[1];
            zs[i] = dataPoints.get(i)[2];
        }
        dataset.addSeries("Heatmap", new double[][]{xs, ys, zs});

        SymbolAxis xAxis = new SymbolAxis(cfg.getXLabel(), orderedX.toArray(String[]::new));
        xAxis.setAutoRange(false);
        xAxis.setRange(-0.5, orderedX.size() - 0.5);
        xAxis.setGridBandsVisible(false);

        SymbolAxis yAxis = new SymbolAxis(cfg.getYLabel(), rowNames.toArray(String[]::new));
        yAxis.setAutoRange(false);
        yAxis.setRange(-0.5, rowNames.size() - 0.5);
        yAxis.setGridBandsVisible(false);

        XYBlockRenderer renderer = new XYBlockRenderer();
        renderer.setPaintScale(new HeatmapPaintScale(minValue, maxValue));
        renderer.setBlockWidth(1.0);
        renderer.setBlockHeight(1.0);

        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);
        plot.setBackgroundPaint(java.awt.Color.WHITE);

        JFreeChart chart = new JFreeChart(cfg.getTitle(), JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        chart.setBackgroundPaint(java.awt.Color.WHITE);

        ChartViewer viewer = new ChartViewer(chart);
        viewer.setMinSize(MIN_CHART_WIDTH, MIN_CHART_HEIGHT);
        viewer.setPrefSize(PREF_CHART_HEIGHT, PREF_CHART_HEIGHT);
        viewer.setStyle("-fx-background-color: transparent;");
        return viewer;
    }

    private static double normalize(double value, double min, double max) {
        if (!Double.isFinite(value) || !Double.isFinite(min) || !Double.isFinite(max) || max <= min) {
            return 0.0;
        }
        return (value - min) / (max - min);
    }

    private static String toHeatColor(double normalized) {
        double hue = 240 - normalized * 240;
        return Color.hsb(hue, 0.75, 0.95).toString().replace("0x", "#");
    }

    private static final class HeatmapPaintScale implements PaintScale {
        private final double lowerBound;
        private final double upperBound;

        HeatmapPaintScale(double lowerBound, double upperBound) {
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
        }

        @Override
        public double getLowerBound() {
            return lowerBound;
        }

        @Override
        public double getUpperBound() {
            return upperBound;
        }

        @Override
        public Paint getPaint(double value) {
            double ratio = (upperBound > lowerBound)
                    ? (value - lowerBound) / (upperBound - lowerBound)
                    : 0.0;
            ratio = Math.max(0.0, Math.min(1.0, ratio));
            float hue = (float) (0.66 - 0.66 * ratio);
            return java.awt.Color.getHSBColor(hue, 0.8f, 0.95f);
        }
    }

    private static void addPieValue(Map<String, Double> aggregated, DataColumn labelCol, DataColumn valueCol, int index) {
        Object lbl = labelCol.getValues().get(index);
        Object val = valueCol.getValues().get(index);

        if (lbl == null || val == null) {
            return;
        }

        double dval;
        try {
            dval = ((Number) val).doubleValue();
        } catch (ClassCastException e) {
            try {
                dval = Double.parseDouble(val.toString());
            } catch (NumberFormatException ex) {
                return;
            }
        }

        if (dval > 0) {
            aggregated.merge(lbl.toString(), dval, Double::sum);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void applyBaseStyle(XYChart chart, ChartConfig cfg) {
        chart.setTitle(cfg.getTitle());
        chart.setMinWidth(MIN_CHART_WIDTH);
        chart.setMinHeight(MIN_CHART_HEIGHT);
        chart.setPrefHeight(PREF_CHART_HEIGHT);
        chart.setStyle("-fx-background-color: transparent;");

        ChartStyle style = cfg.getStyle();

        if (style == null) {
            chart.setLegendVisible(true);
            chart.getGridRenderer().setDrawOnTop(false);
            chart.horizontalGridLinesVisibleProperty().set(true);
            chart.verticalGridLinesVisibleProperty().set(true);
            chart.getStyleClass().addAll("chartfx-theme-default", "dataviz-chart", "chart-wrapper");
            chart.setPadding(new Insets(10, 12, 10, 12));
            return;
        }

        chart.setLegendVisible(style.isShowLegend());
        chart.getGridRenderer().setDrawOnTop(false);
        chart.horizontalGridLinesVisibleProperty().set(style.isShowGrid());
        chart.verticalGridLinesVisibleProperty().set(style.isShowGrid());

        String themeClass = switch (style.getTheme()) {
            case DARK -> "chartfx-theme-dark";
            case CORPORATE -> "chartfx-theme-corporate";
            default -> "chartfx-theme-default";
        };

        chart.getStyleClass().addAll(themeClass, "dataviz-chart", "chart-wrapper");
        chart.setPadding(new Insets(10, 12, 10, 12));
    }

    private static void addInteractionPlugins(XYChart chart, ChartConfig cfg) {
        ChartStyle style = cfg.getStyle();

        Zoomer zoomer = new Zoomer();
        zoomer.setAutoZoomEnabled(true);

        chart.getPlugins().addAll(
                zoomer,
                new Panner(),
                (style == null || style.isShowTooltips()) ? new DataPointTooltip() : null,
                new TableViewer(),
                new Screenshot()
        );

        chart.getPlugins().removeIf(Objects::isNull);
    }

    private static DefaultNumericAxis buildNumericAxis(String label, String fallback) {
        String axisLabel = (label != null && !label.isBlank()) ? label : fallback;
        DefaultNumericAxis axis = new DefaultNumericAxis(axisLabel);
        axis.setAutoRanging(true);
        axis.setForceZeroInRange(false);
        return axis;
    }

    private static int computeBarWidth(int seriesCount) {
        if (seriesCount <= 1) {
            return 65;
        }
        if (seriesCount <= 3) {
            return 50;
        }
        return 35;
    }
}