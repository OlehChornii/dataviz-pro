package com.dataviz.domain.filter;

import java.util.List;
import java.util.Objects;

public final class FilterCriteria {

    public enum FilterType {
        NUMERIC_RANGE,
        CATEGORICAL_IN,
        STRING_CONTAINS,
        BOOLEAN_EQUALS,
        IS_NOT_NULL
    }

    private final String     columnName;
    private final FilterType filterType;
    private final Object     minValue;
    private final Object     maxValue;
    private final List<?>    allowedValues;
    private final String     searchString;
    private final Boolean    boolValue;
    private final boolean    negated;

    private FilterCriteria(Builder b) {
        this.columnName    = Objects.requireNonNull(b.columnName, "columnName");
        this.filterType    = Objects.requireNonNull(b.filterType, "filterType");
        this.minValue      = b.minValue;
        this.maxValue      = b.maxValue;
        this.allowedValues = b.allowedValues != null ? List.copyOf(b.allowedValues) : List.of();
        this.searchString  = b.searchString;
        this.boolValue     = b.boolValue;
        this.negated       = b.negated;
    }

    public static FilterCriteria numericRange(String column, double min, double max) {
        return new Builder(column, FilterType.NUMERIC_RANGE)
                .minValue(min).maxValue(max).build();
    }

    public static FilterCriteria categoricalIn(String column, List<?> allowed) {
        return new Builder(column, FilterType.CATEGORICAL_IN)
                .allowedValues(allowed).build();
    }

    public static FilterCriteria stringContains(String column, String substring) {
        return new Builder(column, FilterType.STRING_CONTAINS)
                .searchString(substring).build();
    }

    public static FilterCriteria booleanEquals(String column, boolean value) {
        return new Builder(column, FilterType.BOOLEAN_EQUALS)
                .boolValue(value).build();
    }

    public static FilterCriteria isNotNull(String column) {
        return new Builder(column, FilterType.IS_NOT_NULL).build();
    }

    public String     getColumnName()   { return columnName; }
    public FilterType getFilterType()   { return filterType; }
    public Object     getMinValue()     { return minValue; }
    public Object     getMaxValue()     { return maxValue; }
    public List<?>    getAllowedValues() { return allowedValues; }
    public String     getSearchString() { return searchString; }
    public Boolean    getBoolValue()    { return boolValue; }
    public boolean    isNegated()       { return negated; }

    public FilterCriteria negate() {
        return new Builder(columnName, filterType)
                .minValue(minValue).maxValue(maxValue)
                .allowedValues(allowedValues)
                .searchString(searchString)
                .boolValue(boolValue)
                .negated(!negated)
                .build();
    }

    @Override
    public String toString() {
        return "FilterCriteria{col='%s', type=%s, negated=%b}"
                .formatted(columnName, filterType, negated);
    }

    public static final class Builder {
        private final String     columnName;
        private final FilterType filterType;
        private Object           minValue;
        private Object           maxValue;
        private List<?>          allowedValues;
        private String           searchString;
        private Boolean          boolValue;
        private boolean          negated = false;

        public Builder(String columnName, FilterType filterType) {
            this.columnName = columnName;
            this.filterType = filterType;
        }

        public Builder minValue(Object v)       { this.minValue = v;      return this; }
        public Builder maxValue(Object v)       { this.maxValue = v;      return this; }
        public Builder allowedValues(List<?> v) { this.allowedValues = v; return this; }
        public Builder searchString(String v)   { this.searchString = v;  return this; }
        public Builder boolValue(Boolean v)     { this.boolValue = v;     return this; }
        public Builder negated(boolean v)       { this.negated = v;       return this; }
        public FilterCriteria build()           { return new FilterCriteria(this); }
    }
}