package com.dataviz.ui.chart;

import com.dataviz.domain.model.DataColumn;
import com.dataviz.domain.model.DataSet;
import de.gsi.dataset.event.UpdatedDataEvent;
import de.gsi.dataset.spi.AbstractDataSet;
import de.gsi.dataset.DataSet2D;

import java.util.List;
import java.util.Objects;

public final class ChartFxDataSetAdapter extends AbstractDataSet<ChartFxDataSetAdapter>
        implements DataSet2D {


    private final List<Double> xValues;
    private final List<Double> yValues;
    private final String[]     xLabels;

    private List<Integer> activeIndices;

    public ChartFxDataSetAdapter(String name,
                                 List<Double> xValues,
                                 List<Double> yValues,
                                 List<Integer> activeIndices) {
        super(name, 2);
        this.xValues      = Objects.requireNonNull(xValues);
        this.yValues      = Objects.requireNonNull(yValues);
        this.xLabels      = null;
        this.activeIndices = activeIndices;
    }

    public ChartFxDataSetAdapter(String name,
                                 String[] labels,
                                 List<Double> yValues) {
        super(name, 2);
        this.xLabels = Objects.requireNonNull(labels);
        this.xValues = buildIndexList(yValues.size());
        this.yValues = Objects.requireNonNull(yValues);
        this.activeIndices = null;
    }

    @Override
    public int getDataCount() {
        return activeIndices != null ? activeIndices.size() : xValues.size();
    }

    @Override
    public double get(int dimIndex, int index) {
        int realIdx = activeIndices != null ? activeIndices.get(index) : index;
        return switch (dimIndex) {
            case DIM_X -> safeGet(xValues, realIdx, (double) realIdx);
            case DIM_Y -> safeGet(yValues, realIdx, Double.NaN);
            default    -> Double.NaN;
        };
    }

    @Override
    public String getDataLabel(int index) {
        if (xLabels == null) return null;
        int realIdx = activeIndices != null ? activeIndices.get(index) : index;
        return realIdx < xLabels.length ? xLabels[realIdx] : String.valueOf(realIdx);
    }

    @Override
    public ChartFxDataSetAdapter set(de.gsi.dataset.DataSet other, boolean copy) {
        throw new UnsupportedOperationException("ChartFxDataSetAdapter is read-only");
    }

    public void applyFilter(List<Integer> indices) {
        this.activeIndices = indices;
        fireInvalidated(new UpdatedDataEvent(this));
    }

    public static ChartFxDataSetAdapter from(String seriesName,
                                             DataColumn xCol,
                                             DataColumn yCol,
                                             List<Integer> activeIndices) {
        Objects.requireNonNull(xCol);
        Objects.requireNonNull(yCol);

        List<Double> yVals = toDoubles(yCol);

        return switch (xCol.getType()) {
            case CATEGORICAL -> {
                String[] labels = xCol.getValues().stream()
                        .map(v -> v != null ? v.toString() : "")
                        .toArray(String[]::new);
                yield new ChartFxDataSetAdapter(seriesName, labels, yVals);
            }
            case NUMERIC, TEMPORAL, BOOLEAN -> {
                List<Double> xVals = toDoubles(xCol);
                yield new ChartFxDataSetAdapter(seriesName, xVals, yVals, activeIndices);
            }
        };
    }

    private static List<Double> toDoubles(DataColumn col) {
        return col.getValues().stream()
                .map(v -> {
                    if (v == null)          return Double.NaN;
                    if (v instanceof Number n) return n.doubleValue();
                    try { return Double.parseDouble(v.toString()); }
                    catch (NumberFormatException e) { return Double.NaN; }
                })
                .toList();
    }

    private static List<Double> buildIndexList(int size) {
        java.util.ArrayList<Double> list = new java.util.ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add((double) i);
        return list;
    }

    private static double safeGet(List<Double> list, int idx, double fallback) {
        if (idx < 0 || idx >= list.size()) return fallback;
        Double v = list.get(idx);
        return v != null ? v : Double.NaN;
    }
}