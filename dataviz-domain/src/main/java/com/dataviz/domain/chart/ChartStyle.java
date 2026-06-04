package com.dataviz.domain.chart;

import java.util.List;
import java.util.Objects;

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
    private final double       pointSize;
    private final double       barWidth;
    private final String       stackingMode;
    private final String       pointShape;
    private final boolean      sliceLabels;
    private final String       legendPosition;
    private final String       colorScale;
    private final boolean      showAxisLabels;
    
    private final boolean      smoothing;
    private final boolean      showTrendLine;
    private final double       seriesTransparency;
    private final boolean      donutMode;
    private final double       innerRadius;
    private final String       barOrientation;
    private final String       colorRangeMin;
    private final String       colorRangeMax;

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
        this.pointSize       = b.pointSize;
        this.barWidth        = b.barWidth;
        this.stackingMode    = b.stackingMode;
        this.pointShape      = b.pointShape;
        this.sliceLabels     = b.sliceLabels;
        this.legendPosition  = b.legendPosition;
        this.colorScale      = b.colorScale;
        this.showAxisLabels  = b.showAxisLabels;
        this.smoothing       = b.smoothing;
        this.showTrendLine   = b.showTrendLine;
        this.seriesTransparency = b.seriesTransparency;
        this.donutMode       = b.donutMode;
        this.innerRadius     = b.innerRadius;
        this.barOrientation  = b.barOrientation;
        this.colorRangeMin   = b.colorRangeMin;
        this.colorRangeMax   = b.colorRangeMax;
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
    public double       getPointSize()       { return pointSize; }
    public double       getBarWidth()        { return barWidth; }
    public String       getStackingMode()    { return stackingMode; }
    public String       getPointShape()      { return pointShape; }
    public boolean      isSliceLabels()      { return sliceLabels; }
    public String       getLegendPosition()  { return legendPosition; }
    public String       getColorScale()      { return colorScale; }
    public boolean      isShowAxisLabels()   { return showAxisLabels; }
    public boolean      isSmoothing()        { return smoothing; }
    public boolean      isShowTrendLine()    { return showTrendLine; }
    public double       getSeriesTransparency() { return seriesTransparency; }
    public boolean      isDonutMode()        { return donutMode; }
    public double       getInnerRadius()     { return innerRadius; }
    public String       getBarOrientation()  { return barOrientation; }
    public String       getColorRangeMin()   { return colorRangeMin; }
    public String       getColorRangeMax()   { return colorRangeMax; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChartStyle that)) return false;
        return theme == that.theme &&
                titleFontSize == that.titleFontSize &&
                labelFontSize == that.labelFontSize &&
                showLegend == that.showLegend &&
                showGrid == that.showGrid &&
                showTooltips == that.showTooltips &&
                sliceLabels == that.sliceLabels &&
                showAxisLabels == that.showAxisLabels &&
                smoothing == that.smoothing &&
                showTrendLine == that.showTrendLine &&
                donutMode == that.donutMode &&
                Double.compare(that.lineWidth, lineWidth) == 0 &&
                Double.compare(that.pointSize, pointSize) == 0 &&
                Double.compare(that.barWidth, barWidth) == 0 &&
                Double.compare(that.seriesTransparency, seriesTransparency) == 0 &&
                Double.compare(that.innerRadius, innerRadius) == 0 &&
                Objects.equals(seriesColors, that.seriesColors) &&
                Objects.equals(backgroundColor, that.backgroundColor) &&
                Objects.equals(gridColor, that.gridColor) &&
                Objects.equals(axisColor, that.axisColor) &&
                Objects.equals(titleFont, that.titleFont) &&
                Objects.equals(labelFont, that.labelFont) &&
                Objects.equals(stackingMode, that.stackingMode) &&
                Objects.equals(pointShape, that.pointShape) &&
                Objects.equals(legendPosition, that.legendPosition) &&
                Objects.equals(colorScale, that.colorScale) &&
                Objects.equals(barOrientation, that.barOrientation) &&
                Objects.equals(colorRangeMin, that.colorRangeMin) &&
                Objects.equals(colorRangeMax, that.colorRangeMax);
    }

    @Override
    public int hashCode() {
        return Objects.hash(theme, seriesColors, backgroundColor, gridColor, axisColor,
                titleFont, titleFontSize, labelFont, labelFontSize, lineWidth,
                showLegend, showGrid, showTooltips, pointSize, barWidth, stackingMode,
                pointShape, sliceLabels, legendPosition, colorScale, showAxisLabels,
                smoothing, showTrendLine, seriesTransparency, donutMode, innerRadius,
                barOrientation, colorRangeMin, colorRangeMax);
    }

    public Builder toBuilder() {
        return new Builder()
                .theme(theme)
                .seriesColors(seriesColors)
                .backgroundColor(backgroundColor)
                .gridColor(gridColor)
                .axisColor(axisColor)
                .titleFont(titleFont).titleFontSize(titleFontSize)
                .labelFont(labelFont).labelFontSize(labelFontSize)
                .lineWidth(lineWidth)
                .showLegend(showLegend).showGrid(showGrid).showTooltips(showTooltips)
                .pointSize(pointSize)
                .barWidth(barWidth)
                .stackingMode(stackingMode)
                .pointShape(pointShape)
                .sliceLabels(sliceLabels)
                .legendPosition(legendPosition)
                .colorScale(colorScale)
                .showAxisLabels(showAxisLabels)
                .smoothing(smoothing)
                .showTrendLine(showTrendLine)
                .seriesTransparency(seriesTransparency)
                .donutMode(donutMode)
                .innerRadius(innerRadius)
                .barOrientation(barOrientation)
                .colorRangeMin(colorRangeMin)
                .colorRangeMax(colorRangeMax);
    }

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
                .pointSize(5.0)
                .barWidth(0.7)
                .stackingMode("GROUPED")
                .pointShape("CIRCLE")
                .sliceLabels(true)
                .legendPosition("RIGHT")
                .colorScale("VIRIDIS")
                .showAxisLabels(true)
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
                .pointSize(5.0)
                .barWidth(0.7)
                .stackingMode("GROUPED")
                .pointShape("CIRCLE")
                .sliceLabels(true)
                .legendPosition("RIGHT")
                .colorScale("VIRIDIS")
                .showAxisLabels(true)
                .build();
    }

    public static ChartStyle corporateStyle() {
        return new Builder()
                .theme(Theme.CORPORATE)
                .pointSize(5.0)
                .barWidth(0.7)
                .stackingMode("GROUPED")
                .pointShape("CIRCLE")
                .sliceLabels(true)
                .legendPosition("RIGHT")
                .colorScale("VIRIDIS")
                .showAxisLabels(true)
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
        private double       pointSize       = 5.0;
        private double       barWidth        = 0.7;
        private String       stackingMode    = "GROUPED";
        private String       pointShape      = "CIRCLE";
        private boolean      sliceLabels     = true;
        private String       legendPosition  = "RIGHT";
        private String       colorScale      = "VIRIDIS";
        private boolean      showAxisLabels  = true;
        private boolean      smoothing       = false;
        private boolean      showTrendLine   = false;
        private double       seriesTransparency = 1.0;
        private boolean      donutMode       = false;
        private double       innerRadius     = 40.0;
        private String       barOrientation  = "VERTICAL";
        private String       colorRangeMin   = "#0000FF";
        private String       colorRangeMax   = "#FF0000";

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
        public Builder pointSize(double v)         { this.pointSize = v;       return this; }
        public Builder barWidth(double v)          { this.barWidth = v;        return this; }
        public Builder stackingMode(String v)      { this.stackingMode = v;    return this; }
        public Builder pointShape(String v)        { this.pointShape = v;      return this; }
        public Builder sliceLabels(boolean v)      { this.sliceLabels = v;     return this; }
        public Builder legendPosition(String v)    { this.legendPosition = v;  return this; }
        public Builder colorScale(String v)        { this.colorScale = v;      return this; }
        public Builder showAxisLabels(boolean v)   { this.showAxisLabels = v;  return this; }
        public Builder smoothing(boolean v)        { this.smoothing = v;       return this; }
        public Builder showTrendLine(boolean v)    { this.showTrendLine = v;   return this; }
        public Builder seriesTransparency(double v){ this.seriesTransparency = v; return this; }
        public Builder donutMode(boolean v)        { this.donutMode = v;       return this; }
        public Builder innerRadius(double v)       { this.innerRadius = v;     return this; }
        public Builder barOrientation(String v)    { this.barOrientation = v;  return this; }
        public Builder colorRangeMin(String v)     { this.colorRangeMin = v;   return this; }
        public Builder colorRangeMax(String v)     { this.colorRangeMax = v;   return this; }
        public ChartStyle build()                  { return new ChartStyle(this); }
    }
}