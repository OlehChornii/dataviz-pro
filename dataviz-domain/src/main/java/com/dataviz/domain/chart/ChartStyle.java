package com.dataviz.domain.chart;

import java.util.List;

public final class ChartStyle {

    public enum Theme { DEFAULT, DARK, CORPORATE, SCIENTIFIC, MINIMAL }

    private final Theme        theme;
    private final List<String> seriesColors;
    private final String       backgroundColor;
    private final String       gridColor;
    private final String       axisColor;
    private final String       titleFont;
    private final int          titleFontSize;
    private final String       labelFont;
    private final int          labelFontSize;
    private final double       lineWidth;
    private final boolean      showLegend;
    private final boolean      showGrid;
    private final boolean      showTooltips;

    private ChartStyle(Builder b) {
        this.theme           = b.theme;
        this.seriesColors    = List.copyOf(b.seriesColors);
        this.backgroundColor = b.backgroundColor;
        this.gridColor       = b.gridColor;
        this.axisColor       = b.axisColor;
        this.titleFont       = b.titleFont;
        this.titleFontSize   = b.titleFontSize;
        this.labelFont       = b.labelFont;
        this.labelFontSize   = b.labelFontSize;
        this.lineWidth       = b.lineWidth;
        this.showLegend      = b.showLegend;
        this.showGrid        = b.showGrid;
        this.showTooltips    = b.showTooltips;
    }

    public Theme        getTheme()           { return theme; }
    public List<String> getSeriesColors()    { return seriesColors; }
    public String       getBackgroundColor() { return backgroundColor; }
    public String       getGridColor()       { return gridColor; }
    public String       getAxisColor()       { return axisColor; }
    public String       getTitleFont()       { return titleFont; }
    public int          getTitleFontSize()   { return titleFontSize; }
    public String       getLabelFont()       { return labelFont; }
    public int          getLabelFontSize()   { return labelFontSize; }
    public double       getLineWidth()       { return lineWidth; }
    public boolean      isShowLegend()       { return showLegend; }
    public boolean      isShowGrid()         { return showGrid; }
    public boolean      isShowTooltips()     { return showTooltips; }

    public static ChartStyle defaultStyle() {
        return new Builder()
                .theme(Theme.DEFAULT)
                .seriesColors(List.of("#3498DB","#E74C3C","#2ECC71","#F39C12","#9B59B6"))
                .backgroundColor("#FFFFFF")
                .gridColor("#E6E6E6")
                .axisColor("#999999")
                .titleFont("Arial").titleFontSize(14)
                .labelFont("Arial").labelFontSize(11)
                .lineWidth(2.0)
                .showLegend(true).showGrid(true).showTooltips(true)
                .build();
    }

    public static ChartStyle darkStyle() {
        return new Builder()
                .theme(Theme.DARK)
                .seriesColors(List.of("#5DADE2","#EC7063","#58D68D","#F4D03F","#C39BD3"))
                .backgroundColor("#1E1E2E")
                .gridColor("#333355")
                .axisColor("#AAAACC")
                .titleFont("Arial").titleFontSize(14)
                .labelFont("Arial").labelFontSize(11)
                .lineWidth(2.0)
                .showLegend(true).showGrid(true).showTooltips(true)
                .build();
    }

    public static ChartStyle corporateStyle() {
        return new Builder()
                .theme(Theme.CORPORATE)
                .seriesColors(List.of("#1F3864","#2E75B6","#70AD47","#ED7D31","#FFC000"))
                .backgroundColor("#FFFFFF")
                .gridColor("#D9D9D9")
                .axisColor("#595959")
                .titleFont("Calibri").titleFontSize(14)
                .labelFont("Calibri").labelFontSize(11)
                .lineWidth(2.5)
                .showLegend(true).showGrid(true).showTooltips(true)
                .build();
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Theme        theme           = Theme.DEFAULT;
        private List<String> seriesColors    = List.of("#3498DB","#E74C3C","#2ECC71");
        private String       backgroundColor = "#FFFFFF";
        private String       gridColor       = "#E6E6E6";
        private String       axisColor       = "#999999";
        private String       titleFont       = "Arial";
        private int          titleFontSize   = 14;
        private String       labelFont       = "Arial";
        private int          labelFontSize   = 11;
        private double       lineWidth       = 2.0;
        private boolean      showLegend      = true;
        private boolean      showGrid        = true;
        private boolean      showTooltips    = true;

        public Builder theme(Theme v)              { this.theme = v;           return this; }
        public Builder seriesColors(List<String> v){ this.seriesColors = v;    return this; }
        public Builder backgroundColor(String v)   { this.backgroundColor = v; return this; }
        public Builder gridColor(String v)         { this.gridColor = v;       return this; }
        public Builder axisColor(String v)         { this.axisColor = v;       return this; }
        public Builder titleFont(String v)         { this.titleFont = v;       return this; }
        public Builder titleFontSize(int v)        { this.titleFontSize = v;   return this; }
        public Builder labelFont(String v)         { this.labelFont = v;       return this; }
        public Builder labelFontSize(int v)        { this.labelFontSize = v;   return this; }
        public Builder lineWidth(double v)         { this.lineWidth = v;       return this; }
        public Builder showLegend(boolean v)       { this.showLegend = v;      return this; }
        public Builder showGrid(boolean v)         { this.showGrid = v;        return this; }
        public Builder showTooltips(boolean v)     { this.showTooltips = v;    return this; }
        public ChartStyle build()                  { return new ChartStyle(this); }
    }
}