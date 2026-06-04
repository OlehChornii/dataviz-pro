package com.dataviz.service.project;

import com.dataviz.domain.chart.ChartConfig;
import com.dataviz.domain.filter.FilterCriteria;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProjectState {

    private final String               id;
    private final String               name;
    private final LocalDateTime        savedAt;
    private final String               dataSetPath;
    private final List<ChartConfig>    chartConfigs;
    private final List<FilterCriteria> activeFilters;
    private final String               layoutJson;

    private ProjectState(Builder b) {
        this.id            = Objects.requireNonNull(b.id);
        this.name          = b.name != null ? b.name : "Untitled";
        this.savedAt       = b.savedAt != null ? b.savedAt : LocalDateTime.now();
        this.dataSetPath   = b.dataSetPath;
        this.chartConfigs  = List.copyOf(b.chartConfigs);
        this.activeFilters = List.copyOf(b.activeFilters);
        this.layoutJson    = b.layoutJson != null ? b.layoutJson : "{}";
    }

    public String               getId()            { return id; }
    public String               getName()          { return name; }
    public LocalDateTime        getSavedAt()       { return savedAt; }
    public String               getDataSetPath()   { return dataSetPath; }
    public List<ChartConfig>    getChartConfigs()  { return chartConfigs; }
    public List<FilterCriteria> getActiveFilters() { return activeFilters; }
    public String               getLayoutJson()    { return layoutJson; }

    public String toXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<project id=\"").append(escape(id))
                .append("\" name=\"").append(escape(name))
                .append("\" savedAt=\"").append(savedAt).append("\">\n");

        sb.append("  <dataSetPath>")
                .append(dataSetPath != null ? escape(dataSetPath) : "")
                .append("</dataSetPath>\n");

        sb.append("  <layout>").append(escape(layoutJson)).append("</layout>\n");

        sb.append("  <chartConfigs>\n");
        for (ChartConfig cfg : chartConfigs) {
            sb.append("    <chart");
            sb.append(" id=\"").append(escape(cfg.getId())).append("\"");
            sb.append(" type=\"").append(cfg.getChartType()).append("\"");
            sb.append(" title=\"").append(escape(cfg.getTitle())).append("\"");
            sb.append(" xCol=\"").append(escape(cfg.getXColumn())).append("\"");
            if (cfg.getYColumns() != null && !cfg.getYColumns().isEmpty()) {
                sb.append(" yCols=\"")
                        .append(escape(String.join(",", cfg.getYColumns())))
                        .append("\"");
            }
            if (cfg.getColorColumn() != null)
                sb.append(" colorCol=\"").append(escape(cfg.getColorColumn())).append("\"");
            if (cfg.getXLabel() != null)
                sb.append(" xLabel=\"").append(escape(cfg.getXLabel())).append("\"");
            if (cfg.getYLabel() != null)
                sb.append(" yLabel=\"").append(escape(cfg.getYLabel())).append("\"");
            sb.append(" />\n");
        }
        sb.append("  </chartConfigs>\n");

        sb.append("  <activeFilters>\n");
        for (FilterCriteria f : activeFilters) {
            sb.append("    <filter");
            sb.append(" col=\"").append(escape(f.getColumnName())).append("\"");
            sb.append(" type=\"").append(f.getFilterType()).append("\"");
            sb.append(" negated=\"").append(f.isNegated()).append("\"");
            if (f.getMinValue() != null)
                sb.append(" min=\"").append(f.getMinValue()).append("\"");
            if (f.getMaxValue() != null)
                sb.append(" max=\"").append(f.getMaxValue()).append("\"");
            if (f.getSearchString() != null)
                sb.append(" search=\"").append(escape(f.getSearchString())).append("\"");
            if (f.getBoolValue() != null)
                sb.append(" bool=\"").append(f.getBoolValue()).append("\"");
            if (f.getAllowedValues() != null && !f.getAllowedValues().isEmpty())
                sb.append(" allowed=\"")
                        .append(escape(f.getAllowedValues().stream()
                                .map(Object::toString)
                                .collect(java.util.stream.Collectors.joining("|"))))
                        .append("\"");
            sb.append(" />\n");
        }
        sb.append("  </activeFilters>\n");

        sb.append("</project>\n");
        return sb.toString();
    }

    public static ProjectState fromXml(String xml) {
        Builder b = new Builder();

        b.id(attr(xml, "project", "id", UUID.randomUUID().toString()));
        b.name(attr(xml, "project", "name", "Restored Project"));

        String savedAtStr = attr(xml, "project", "savedAt", null);
        if (savedAtStr != null) {
            try { b.savedAt(LocalDateTime.parse(savedAtStr)); }
            catch (Exception ignored) {}
        }

        b.dataSetPath(tag(xml, "dataSetPath"));

        String layout = tag(xml, "layout");
        if (layout != null) b.layoutJson(layout);

        String cfgBlock = block(xml, "chartConfigs");
        if (cfgBlock != null) {
            List<ChartConfig> configs = new ArrayList<>();
            Matcher cm = Pattern.compile("<chart([^/]*)/?>").matcher(cfgBlock);
            while (cm.find()) {
                String el = cm.group(1);
                ChartConfig.ChartType type = parseEnum(
                        ChartConfig.ChartType.class,
                        inlineAttr(el, "type"), null);
                if (type == null) continue;

                ChartConfig.Builder cb = ChartConfig.builder()
                        .id(inlineAttr(el, "id"))
                        .chartType(type)
                        .title(inlineAttr(el, "title"))
                        .xColumn(inlineAttr(el, "xCol"));

                String yCols = inlineAttr(el, "yCols");
                if (yCols != null && !yCols.isBlank())
                    cb.yColumns(List.of(yCols.split(",")));

                String colorCol = inlineAttr(el, "colorCol");
                if (colorCol != null) cb.colorColumn(colorCol);

                String xLabel = inlineAttr(el, "xLabel");
                if (xLabel != null) cb.xLabel(xLabel);

                String yLabel = inlineAttr(el, "yLabel");
                if (yLabel != null) cb.yLabel(yLabel);

                configs.add(cb.build());
            }
            b.chartConfigs(configs);
        }

        String filterBlock = block(xml, "activeFilters");
        if (filterBlock != null) {
            List<FilterCriteria> filters = new ArrayList<>();
            Matcher fm = Pattern.compile("<filter([^/]*)/?>").matcher(filterBlock);
            while (fm.find()) {
                String el   = fm.group(1);
                String col  = inlineAttr(el, "col");
                String type = inlineAttr(el, "type");
                if (col == null || type == null) continue;

                FilterCriteria fc = buildFilter(col, type, el);
                if (fc == null) continue;

                boolean negated = "true".equalsIgnoreCase(inlineAttr(el, "negated"));
                filters.add(negated ? fc.negate() : fc);
            }
            b.activeFilters(filters);
        }

        return b.build();
    }

    private static FilterCriteria buildFilter(String col, String type, String el) {
        try {
            FilterCriteria.FilterType ft =
                    FilterCriteria.FilterType.valueOf(type.toUpperCase());
            return switch (ft) {
                case NUMERIC_RANGE -> FilterCriteria.numericRange(col,
                        parseDouble(inlineAttr(el, "min")),
                        parseDouble(inlineAttr(el, "max")));
                case CATEGORICAL_IN -> {
                    String allowed = inlineAttr(el, "allowed");
                    yield FilterCriteria.categoricalIn(col,
                            allowed != null ? List.of(allowed.split("\\|")) : List.of());
                }
                case STRING_CONTAINS ->
                        FilterCriteria.stringContains(col, inlineAttr(el, "search"));
                case BOOLEAN_EQUALS ->
                        FilterCriteria.booleanEquals(col,
                                Boolean.parseBoolean(inlineAttr(el, "bool")));
                case IS_NOT_NULL ->
                        FilterCriteria.isNotNull(col);
            };
        } catch (Exception e) {
            return null;
        }
    }

    private static Double parseDouble(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return null; }
    }

    private static String attr(String xml, String tag, String attr, String fallback) {
        Matcher m = Pattern.compile(
                "<" + tag + "[^>]*\\s" + attr + "=\"([^\"]*)\"").matcher(xml);
        return m.find() ? unescape(m.group(1)) : fallback;
    }

    private static String tag(String xml, String tag) {
        Matcher m = Pattern.compile(
                "<" + tag + ">([^<]*)</" + tag + ">").matcher(xml);
        return m.find() ? unescape(m.group(1).trim()) : null;
    }

    private static String block(String xml, String tag) {
        Matcher m = Pattern.compile(
                "<" + tag + ">(.*?)</" + tag + ">",
                Pattern.DOTALL).matcher(xml);
        return m.find() ? m.group(1) : null;
    }

    private static String inlineAttr(String attrs, String name) {
        Matcher m = Pattern.compile("\\s" + name + "=\"([^\"]*)\"").matcher(attrs);
        return m.find() ? unescape(m.group(1)) : null;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> cls, String val, E fallback) {
        if (val == null) return fallback;
        try { return Enum.valueOf(cls, val.toUpperCase()); }
        catch (IllegalArgumentException e) { return fallback; }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String unescape(String s) {
        if (s == null) return null;
        return s.replace("&quot;", "\"")
                .replace("&gt;", ">")
                .replace("&lt;", "<")
                .replace("&amp;", "&");
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String               id            = UUID.randomUUID().toString();
        private String               name;
        private LocalDateTime        savedAt;
        private String               dataSetPath;
        private List<ChartConfig>    chartConfigs  = List.of();
        private List<FilterCriteria> activeFilters = List.of();
        private String               layoutJson;

        public Builder id(String v)                        { this.id = v;            return this; }
        public Builder name(String v)                      { this.name = v;          return this; }
        public Builder savedAt(LocalDateTime v)            { this.savedAt = v;       return this; }
        public Builder dataSetPath(String v)               { this.dataSetPath = v;   return this; }
        public Builder chartConfigs(List<ChartConfig> v)   { this.chartConfigs = v;  return this; }
        public Builder activeFilters(List<FilterCriteria> v){ this.activeFilters = v; return this; }
        public Builder layoutJson(String v)                { this.layoutJson = v;    return this; }
        public ProjectState build()                        { return new ProjectState(this); }
    }
}