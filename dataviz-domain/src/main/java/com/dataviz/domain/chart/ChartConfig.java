package com.dataviz.domain.chart;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ChartConfig {

    public enum ChartType { LINE, BAR, PIE, SCATTER, AREA, HEATMAP }

    private final String    id;
    private final ChartType chartType;
    private final String    title;
    private final String    xColumn;
    private final List<String> yColumns;
    private final String    colorColumn;
    private final ChartStyle style;
    private final String xLabel;
    private final String yLabel;

    private ChartConfig(Builder b) {
        this.id          = Objects.requireNonNull(b.id);
        this.chartType   = Objects.requireNonNull(b.chartType);
        this.title       = b.title != null ? b.title : "";
        this.xColumn     = Objects.requireNonNull(b.xColumn);
        this.yColumns    = List.copyOf(b.yColumns);
        this.colorColumn = b.colorColumn;
        this.style       = b.style != null ? b.style : ChartStyle.defaultStyle();
        this.xLabel = b.xLabel;
        this.yLabel = b.yLabel;
    }

    public String       getId()          { return id; }
    public ChartType    getChartType()   { return chartType; }
    public String       getTitle()       { return title; }
    public String       getXColumn()     { return xColumn; }
    public List<String> getYColumns()    { return yColumns; }
    public String       getColorColumn() { return colorColumn; }
    public ChartStyle   getStyle()       { return style; }
    public String getXLabel() { return xLabel; }
    public String getYLabel() { return yLabel; }

    public ChartConfig withTitle(String newTitle) {
        return new Builder()
                .id(id).chartType(chartType).title(newTitle)
                .xColumn(xColumn).yColumns(yColumns)
                .xLabel(xLabel).yLabel(yLabel)
                .colorColumn(colorColumn).style(style)
                .build();
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String       id;
        private ChartType    chartType;
        private String       title;
        private String       xColumn;
        private List<String> yColumns = new ArrayList<>();
        private String       colorColumn;
        private ChartStyle   style;
        private String xLabel;
        private String yLabel;

        public Builder id(String v)               { this.id = v; return this; }
        public Builder chartType(ChartType v)     { this.chartType = v; return this; }
        public Builder title(String v)            { this.title = v; return this; }
        public Builder xColumn(String v)          { this.xColumn = v; return this; }
        public Builder yColumns(List<String> v)   { this.yColumns = new ArrayList<>(v); return this; }
        public Builder addYColumn(String v)       { this.yColumns.add(v); return this; }
        public Builder colorColumn(String v)      { this.colorColumn = v; return this; }
        public Builder style(ChartStyle v)        { this.style = v; return this; }
        public ChartConfig build()                { return new ChartConfig(this); }
        public Builder xLabel(String v) { this.xLabel = v; return this; }
        public Builder yLabel(String v) { this.yLabel = v; return this; }
    }
}