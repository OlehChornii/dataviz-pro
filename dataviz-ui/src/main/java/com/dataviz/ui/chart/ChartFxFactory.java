package com.dataviz.ui.chart;

import com.dataviz.domain.chart.ChartConfig;
import com.dataviz.domain.chart.ChartStyle;
import com.dataviz.domain.model.DataColumn;
import com.dataviz.domain.model.DataSet;
import com.dataviz.service.chart.ChartService.ChartRenderResult;
import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.CategoryAxis;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.marker.DefaultMarker;
import de.gsi.chart.plugins.ChartPlugin;
import de.gsi.chart.plugins.DataPointTooltip;
import de.gsi.chart.plugins.Panner;
import de.gsi.chart.plugins.Screenshot;
import de.gsi.chart.plugins.TableViewer;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.LineStyle;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.chart.renderer.spi.ReducingLineRenderer;
import de.gsi.chart.renderer.spi.utils.DefaultRenderColorScheme;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.data.xy.DefaultXYZDataset;

import java.awt.Paint;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

public final class ChartFxFactory {

    private static final int MIN_CHART_WIDTH = 120;
    private static final int MIN_CHART_HEIGHT = 90;

    private ChartFxFactory() {
    }

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

    public static Node create(DataSet dataSet, ChartConfig config, List<Integer> activeIndices) {
        Objects.requireNonNull(dataSet, "dataSet must not be null");
        Objects.requireNonNull(config, "config must not be null");

        try {
            return switch (config.getChartType()) {
                case LINE -> buildLineChart(dataSet, config, activeIndices, false);
                case AREA -> buildLineChart(dataSet, config, activeIndices, true);
                case BAR -> buildBarChart(dataSet, config, activeIndices);
                case SCATTER -> buildScatterChart(dataSet, config, activeIndices);
                case PIE -> buildPieChart(dataSet, config, activeIndices);
                case HEATMAP -> buildHeatmap(dataSet, config, activeIndices);
            };
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(ChartFxFactory.class.getName())
                    .severe("Chart creation failed for type " + config.getChartType() + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create chart: " + e.getMessage(), e);
        }
    }

    private static XYChart buildLineChart(DataSet ds, ChartConfig cfg, List<Integer> indices, boolean filled) {
        DefaultNumericAxis xAxis = buildNumericAxis(cfg.getXLabel(), cfg.getXColumn());
        var yAxis = cfg.getYColumns().stream()
                .anyMatch(name -> ds.hasColumn(name)
                        && ds.getColumn(name).getType() == com.dataviz.domain.model.ColumnType.CATEGORICAL)
                ? new CategoryAxis(cfg.getYLabel())
                : buildNumericAxis(cfg.getYLabel(), "");

        XYChart chart = new XYChart(xAxis, yAxis);
        applyBaseStyle(chart, cfg);

        final de.gsi.chart.renderer.Renderer renderer;
        ErrorDataSetRenderer lineRenderer = new ErrorDataSetRenderer();
        if (filled) {
            lineRenderer.setPolyLineStyle(LineStyle.AREA);
        } else {
            lineRenderer.setPolyLineStyle(cfg.getStyle() != null && cfg.getStyle().isSmoothing()
                    ? LineStyle.BEZIER_CURVE
                    : LineStyle.NORMAL);
        }
        lineRenderer.setDrawMarker(false);
        chart.getRenderers().setAll(lineRenderer);
        renderer = lineRenderer;

        addInteractionPlugins(chart, cfg);

        for (int i = 0; i < cfg.getYColumns().size(); i++) {
            String yColName = cfg.getYColumns().get(i);
            if (!ds.hasColumn(yColName)) {
                continue;
            }

            DataColumn xCol = ds.hasColumn(cfg.getXColumn()) ? ds.getColumn(cfg.getXColumn()) : null;
            DataColumn yCol = ds.getColumn(yColName);

            ChartFxDataSetAdapter adapter;
            if (filled && cfg.getStyle() != null && cfg.getStyle().isSmoothing() && xCol != null
                    && xCol.getType() != com.dataviz.domain.model.ColumnType.CATEGORICAL) {
                List<Double> xVals = toDoubles(xCol);
                List<Double> yVals = toDoubles(yCol);
                var densified = densifySeries(xVals, yVals, 4);
                adapter = new ChartFxDataSetAdapter(yColName, densified.get(0), densified.get(1), indices);
            } else {
                adapter = ChartFxDataSetAdapter.from(yColName, xCol, yCol, indices);
            }

            if (cfg.getStyle() != null) {
                double lw = cfg.getStyle().getLineWidth();
                String dsStyle = "strokeWidth: " + lw + ";";
                try {
                    adapter.setStyle(dsStyle);
                } catch (Exception ignore) {
                }
            }

            renderer.getDatasets().add(adapter);
        }

        return chart;
    }

    private static XYChart buildBarChart(DataSet ds, ChartConfig cfg, List<Integer> indices) {
        DataColumn xCol = ds.hasColumn(cfg.getXColumn()) ? ds.getColumn(cfg.getXColumn()) : null;

        final boolean categorical = xCol != null
                && xCol.getType() == com.dataviz.domain.model.ColumnType.CATEGORICAL;

        var categoryAxis = categorical
                ? new CategoryAxis(cfg.getXLabel())
                : buildNumericAxis(cfg.getXLabel(), cfg.getXColumn());
        var valueAxis = buildNumericAxis(cfg.getYLabel(), "");

        var xAxis = categoryAxis;
        var yAxis = valueAxis;

        XYChart chart = new XYChart(xAxis, yAxis);
        applyBaseStyle(chart, cfg);

        ErrorDataSetRenderer renderer = new ErrorDataSetRenderer();
        renderer.setDrawBars(true);
        renderer.setDrawChartDataSets(false);
        renderer.setDrawMarker(false);
        renderer.setPolyLineStyle(LineStyle.NONE);
        renderer.setErrorType(de.gsi.chart.renderer.ErrorStyle.NONE);

        boolean grouped = cfg.getStyle() == null
                || "GROUPED".equalsIgnoreCase(cfg.getStyle().getStackingMode());
        renderer.setShiftBar(grouped);
        if (grouped) {
            renderer.setshiftBarOffset(3);
            renderer.setDynamicBarWidth(true);
        } else {
            renderer.setDynamicBarWidth(true);
        }

        double barWidthPct = cfg.getStyle() != null ? cfg.getStyle().getBarWidth() : 0.7;
        double chartFxBarWidthPct = Math.max(0.0, Math.min(100.0, barWidthPct * 100.0));
        renderer.setBarWidthPercentage(chartFxBarWidthPct);

        chart.getRenderers().setAll(renderer);
        addInteractionPlugins(chart, cfg);

        List<List<Double>> seriesValues = new ArrayList<>();
        for (String yColName : cfg.getYColumns()) {
            if (!ds.hasColumn(yColName)) {
                continue;
            }
            seriesValues.add(new ArrayList<>(toDoubles(ds.getColumn(yColName))));
        }

        if (!seriesValues.isEmpty() && cfg.getStyle() != null
                && "STACKED".equalsIgnoreCase(cfg.getStyle().getStackingMode())) {
            stackSeriesValues(seriesValues);
        }

        int seriesIndex = 0;
        for (int i = 0; i < cfg.getYColumns().size(); i++) {
            String yColName = cfg.getYColumns().get(i);
            if (xCol == null || !ds.hasColumn(yColName)) {
                continue;
            }

            ChartFxDataSetAdapter adapter = ChartFxDataSetAdapter.from(
                    yColName,
                    xCol,
                    seriesValues.get(seriesIndex++),
                    indices
            );

            if (cfg.getStyle() != null) {
                double lw = cfg.getStyle().getLineWidth();
                String dsStyle = "strokeWidth: " + lw + ";";
                try {
                    adapter.setStyle(dsStyle);
                } catch (Exception ignore) {
                }
            }

            renderer.getDatasets().add(adapter);
        }

        return chart;
    }

    private static List<Double> toDoubles(DataColumn col) {
        if (col.getType() == com.dataviz.domain.model.ColumnType.CATEGORICAL) {
            var distinct = col.getDistinctValuesAsString().stream().toList();
            java.util.Map<String, Integer> map = new java.util.LinkedHashMap<>();
            for (int i = 0; i < distinct.size(); i++) {
                map.put(distinct.get(i), i);
            }
            return col.getValues().stream()
                    .map(v -> {
                        if (v == null) {
                            return Double.NaN;
                        }
                        String s = v.toString();
                        Integer idx = map.get(s);
                        return idx != null ? idx.doubleValue() : Double.NaN;
                    })
                    .toList();
        }

        return col.getValues().stream()
                .map(v -> {
                    if (v == null) {
                        return Double.NaN;
                    }
                    if (v instanceof Number n) {
                        return n.doubleValue();
                    }
                    try {
                        return Double.parseDouble(v.toString());
                    } catch (NumberFormatException e) {
                        return Double.NaN;
                    }
                })
                .toList();
    }

    private static void stackSeriesValues(List<List<Double>> seriesValues) {
        if (seriesValues.isEmpty()) {
            return;
        }

        int size = seriesValues.get(0).size();
        for (int index = 0; index < size; index++) {
            double positiveStack = 0.0;
            double negativeStack = 0.0;
            for (List<Double> series : seriesValues) {
                Double current = series.get(index);
                if (current == null || current.isNaN()) {
                    continue;
                }
                if (current >= 0.0) {
                    double value = positiveStack + current;
                    series.set(index, value);
                    positiveStack = value;
                } else {
                    double value = negativeStack + current;
                    series.set(index, value);
                    negativeStack = value;
                }
            }
        }
    }

    private static List<List<Double>> densifySeries(List<Double> x, List<Double> y, int samplesPerSegment) {
        List<Double> nx = new ArrayList<>();
        List<Double> ny = new ArrayList<>();
        if (x == null || y == null || x.size() != y.size() || x.size() < 2) {
            return List.of(x, y);
        }

        for (int i = 0; i < x.size() - 1; i++) {
            double x0 = x.get(i);
            double y0 = y.get(i);
            double x1 = x.get(i + 1);
            double y1 = y.get(i + 1);

            for (int s = 0; s < samplesPerSegment; s++) {
                double t = (double) s / samplesPerSegment;
                double xi = x0 + (x1 - x0) * t;
                double yi = y0 + (y1 - y0) * t;
                nx.add(xi);
                ny.add(yi);
            }
        }

        nx.add(x.get(x.size() - 1));
        ny.add(y.get(y.size() - 1));

        return List.of(nx, ny);
    }

    private static XYChart buildScatterChart(DataSet ds, ChartConfig cfg, List<Integer> indices) {
        DefaultNumericAxis xAxis = buildNumericAxis(cfg.getXLabel(), cfg.getXColumn());
        var yAxis = cfg.getYColumns().stream()
                .anyMatch(name -> ds.hasColumn(name)
                        && ds.getColumn(name).getType() == com.dataviz.domain.model.ColumnType.CATEGORICAL)
                ? new CategoryAxis(cfg.getYLabel())
                : buildNumericAxis(cfg.getYLabel(), "");

        XYChart chart = new XYChart(xAxis, yAxis);
        applyBaseStyle(chart, cfg);

        ErrorDataSetRenderer pointRenderer = new ErrorDataSetRenderer();
        pointRenderer.setPolyLineStyle(LineStyle.NONE);
        pointRenderer.setDrawMarker(true);
        if (cfg.getStyle() != null) {
            pointRenderer.setMarkerSize(cfg.getStyle().getPointSize());
            if (cfg.getStyle().getPointShape() != null && !cfg.getStyle().getPointShape().isBlank()) {
                pointRenderer.setMarker(getMarker(cfg.getStyle().getPointShape()));
            }
        } else {
            pointRenderer.setMarkerSize(7);
        }

        chart.getRenderers().setAll(pointRenderer);
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
            pointRenderer.getDatasets().add(adapter);
        }

        return chart;
    }

    private static Node buildPieChart(DataSet ds, ChartConfig cfg, List<Integer> activeIndices) {
        Map<String, Double> aggregated = new LinkedHashMap<>();

        if (!cfg.getYColumns().isEmpty() && ds.hasColumn(cfg.getYColumns().get(0))) {
            DataColumn valueCol = ds.getColumn(cfg.getYColumns().get(0));
            DataColumn labelCol = ds.hasColumn(cfg.getXColumn()) ? ds.getColumn(cfg.getXColumn()) : null;

            if (valueCol.getType() == com.dataviz.domain.model.ColumnType.CATEGORICAL) {
                int limit = valueCol.size();
                if (activeIndices != null) {
                    for (int idx : activeIndices) {
                        if (idx >= 0 && idx < limit) {
                            Object val = valueCol.getValues().get(idx);
                            if (val != null) {
                                aggregated.merge(val.toString(), 1.0, Double::sum);
                            }
                        }
                    }
                } else {
                    for (int i = 0; i < limit; i++) {
                        Object val = valueCol.getValues().get(i);
                        if (val != null) {
                            aggregated.merge(val.toString(), 1.0, Double::sum);
                        }
                    }
                }
            } else if (labelCol != null) {
                int limit = Math.min(labelCol.size(), valueCol.size());
                if (activeIndices != null) {
                    for (int idx : activeIndices) {
                        if (idx >= 0 && idx < limit) {
                            addPieValue(aggregated, labelCol, valueCol, idx);
                        }
                    }
                } else {
                    for (int i = 0; i < limit; i++) {
                        addPieValue(aggregated, labelCol, valueCol, i);
                    }
                }
            }

            final int maxSlices = 25;
            if (aggregated.size() > maxSlices) {
                List<Map.Entry<String, Double>> sorted = aggregated.entrySet().stream()
                        .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                        .toList();
                double otherSum = sorted.stream().skip(maxSlices - 1).mapToDouble(Map.Entry::getValue).sum();
                Map<String, Double> trimmed = new LinkedHashMap<>();
                sorted.stream().limit(maxSlices - 1).forEach(e -> trimmed.put(e.getKey(), e.getValue()));
                trimmed.put("Other", otherSum);
                aggregated = trimmed;
            }
        }

        javafx.collections.ObservableList<javafx.scene.chart.PieChart.Data> pieData =
                javafx.collections.FXCollections.observableArrayList();
        final Map<String, Double> data = aggregated;
        data.forEach((k, v) -> pieData.add(new javafx.scene.chart.PieChart.Data(k, v)));

        javafx.scene.chart.PieChart chart = new javafx.scene.chart.PieChart(pieData);
        chart.setTitle(cfg.getTitle() != null ? cfg.getTitle() : "");
        chart.setLegendVisible(cfg.getStyle() == null || cfg.getStyle().isShowLegend());
        chart.setLabelsVisible(cfg.getStyle() == null || cfg.getStyle().isSliceLabels());
        chart.setStartAngle(90);
        chart.setClockwise(true);
        chart.getStyleClass().addAll("dataviz-pie-chart", "dataviz-chart");
        chart.setStyle("-fx-background-color: transparent;");
        chart.setMinSize(MIN_CHART_WIDTH, MIN_CHART_HEIGHT);

        return chart;
    }

    private static Node buildHeatmap(DataSet ds, ChartConfig cfg, List<Integer> activeIndices) {
        List<String> rowNames = cfg.getYColumns().stream()
                .filter(ds::hasColumn)
                .toList();

        List<Integer> rowIndices = activeIndices != null
                ? activeIndices
                : IntStream.range(0, ds.getColumns().isEmpty() ? 0 : ds.getColumns().get(0).size())
                .boxed()
                .toList();

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

            java.util.Map<String, Integer> catMap = null;
            if (yCol.getType() == com.dataviz.domain.model.ColumnType.CATEGORICAL) {
                var distinct = yCol.getDistinctValuesAsString().stream().toList();
                catMap = new java.util.LinkedHashMap<>();
                for (int i = 0; i < distinct.size(); i++) {
                    catMap.put(distinct.get(i), i);
                }
            }

            for (int dataIndex : rowIndices) {
                if (dataIndex < 0 || dataIndex >= yCol.size() || dataIndex >= xValues.size()) {
                    continue;
                }

                String xKey = xValues.get(dataIndex);
                Object rawY = yCol.getValues().get(dataIndex);
                if (rawY == null) {
                    continue;
                }

                Double value = null;
                if (catMap != null) {
                    Integer idx = catMap.get(rawY.toString());
                    if (idx != null) {
                        value = idx.doubleValue();
                    }
                } else {
                    try {
                        value = ((Number) rawY).doubleValue();
                    } catch (ClassCastException ex) {
                        try {
                            value = Double.parseDouble(rawY.toString());
                        } catch (NumberFormatException ignore) {
                            // skip non-numeric
                        }
                    }
                }

                if (value == null) {
                    continue;
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

        java.awt.Font dsAxisLabelFont = new java.awt.Font("Monospaced", java.awt.Font.BOLD, 10);
        java.awt.Font dsTickLabelFont = new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 9);
        java.awt.Color dsAxisLabelColor = new java.awt.Color(0x60, 0x60, 0x5c);
        java.awt.Color dsTickLabelColor = new java.awt.Color(0xa0, 0xa0, 0x9b);
        java.awt.Color dsAxisLineColor = new java.awt.Color(0xc8, 0xc4, 0xbc);

        xAxis.setLabelFont(dsAxisLabelFont);
        xAxis.setLabelPaint(dsAxisLabelColor);
        xAxis.setTickLabelFont(dsTickLabelFont);
        xAxis.setTickLabelPaint(dsTickLabelColor);
        xAxis.setAxisLinePaint(dsAxisLineColor);

        yAxis.setLabelFont(dsAxisLabelFont);
        yAxis.setLabelPaint(dsAxisLabelColor);
        yAxis.setTickLabelFont(dsTickLabelFont);
        yAxis.setTickLabelPaint(dsTickLabelColor);
        yAxis.setAxisLinePaint(dsAxisLineColor);

        XYBlockRenderer renderer = new XYBlockRenderer();
        renderer.setPaintScale(new HeatmapPaintScale(minValue, maxValue));
        renderer.setBlockWidth(1.0);
        renderer.setBlockHeight(1.0);

        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);
        plot.setBackgroundPaint(java.awt.Color.WHITE);
        plot.setOutlinePaint(new java.awt.Color(0xe6, 0xe6, 0xe6));
        plot.setOutlineStroke(new java.awt.BasicStroke(0.8f));

        if (cfg.getStyle() != null && !cfg.getStyle().isShowAxisLabels()) {
            xAxis.setTickLabelsVisible(false);
            xAxis.setLabel(null);
            yAxis.setTickLabelsVisible(false);
            yAxis.setLabel(null);
        }

        java.awt.Font dsTitleFont = new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12);
        JFreeChart chart = new JFreeChart(
                cfg.getTitle() != null ? cfg.getTitle() : "",
                dsTitleFont,
                plot,
                true
        );
        chart.setBackgroundPaint(null);
        chart.getTitle().setPaint(new java.awt.Color(0x0d, 0x0d, 0x0c));
        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(new java.awt.Color(255, 255, 255, 230));
            chart.getLegend().setItemFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 9));
            chart.getLegend().setItemPaint(new java.awt.Color(0x60, 0x60, 0x5c));
        }

        ChartViewer viewer = new ChartViewer(chart);
        viewer.setMinSize(MIN_CHART_WIDTH, MIN_CHART_HEIGHT);
        viewer.getStyleClass().addAll("dataviz-chart", "dataviz-heatmap");
        viewer.setStyle("-fx-background-color: transparent;");

        viewer.addEventHandler(ScrollEvent.SCROLL, e -> {
            if (e.getDeltaY() != 0 || e.getDeltaX() != 0) {
                e.consume();
            }
        });

        if (viewer.getCanvas() != null) {
            viewer.getCanvas().addEventHandler(ScrollEvent.SCROLL, e -> {
                if (e.getDeltaY() != 0 || e.getDeltaX() != 0) {
                    e.consume();
                }
            });
        }

        if (chart.getLegend() != null) {
            chart.removeLegend();
        }

        xAxis.setVerticalTickLabels(true);
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

    private static void applyBaseStyle(XYChart chart, ChartConfig cfg) {
        chart.setTitle(cfg.getTitle());
        chart.setMinWidth(MIN_CHART_WIDTH);
        chart.setMinHeight(MIN_CHART_HEIGHT);
        chart.setStyle("-fx-background-color: transparent;");

        ChartStyle style = cfg.getStyle();

        if (style == null) {
            chart.setLegendVisible(true);
            chart.getGridRenderer().setDrawOnTop(false);
            chart.horizontalGridLinesVisibleProperty().set(true);
            chart.verticalGridLinesVisibleProperty().set(false);
            chart.getStyleClass().addAll("chartfx-theme-default", "dataviz-chart", "chart-wrapper");
            chart.setPadding(new Insets(4, 6, 4, 6));
            return;
        }

        chart.setLegendVisible(style.isShowLegend());
        chart.getGridRenderer().setDrawOnTop(false);
        chart.horizontalGridLinesVisibleProperty().set(style.isShowGrid());
        chart.verticalGridLinesVisibleProperty().set(false);

        String themeClass = switch (style.getTheme()) {
            case DARK -> "chartfx-theme-dark";
            case CORPORATE -> "chartfx-theme-corporate";
            default -> "chartfx-theme-default";
        };

        chart.getStyleClass().addAll(themeClass, "dataviz-chart", "chart-wrapper");
        chart.setPadding(new Insets(4, 6, 4, 6));
    }

    private static void addInteractionPlugins(XYChart chart, ChartConfig cfg) {
        ChartStyle style = cfg.getStyle();
        boolean showTooltips = style == null || style.isShowTooltips();

        List<ChartPlugin> plugins = new ArrayList<>(List.of(
                new Zoomer(),
                new Panner(),
                new TableViewer(),
                new Screenshot()
        ));

        if (showTooltips) {
            plugins.add(2, new DataPointTooltip());
        }

        chart.getPlugins().addAll(plugins);
    }

    private static void applyLineWidthClass(XYChart chart, ChartStyle style) {
        chart.getStyleClass().removeIf(c -> c.startsWith("line-width-"));
        int width = Math.max(1, Math.min(6, (int) Math.round(style.getLineWidth())));
        chart.getStyleClass().add("line-width-" + width);
    }

    private static DefaultMarker getMarker(String pointShape) {
        if (pointShape == null || pointShape.isBlank()) {
            return DefaultMarker.CIRCLE;
        }
        try {
            return DefaultMarker.valueOf(pointShape);
        } catch (IllegalArgumentException e) {
            if (pointShape.equalsIgnoreCase("SQUARE")) {
                return DefaultMarker.RECTANGLE;
            }
            return DefaultMarker.CIRCLE;
        }
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