package com.dataviz.domain.model;

import java.util.*;
import java.util.stream.Collectors;

public final class DataColumn {

    private final String     name;
    private final ColumnType type;
    private final List<?>    values;
    private final long       nullCount;

    private final Double min;
    private final Double max;
    private final Double mean;

    private DataColumn(Builder builder) {
        this.name      = Objects.requireNonNull(builder.name);
        this.type      = Objects.requireNonNull(builder.type);
        this.values    = List.copyOf(builder.values);
        this.nullCount = builder.nullCount;
        this.min       = builder.min;
        this.max       = builder.max;
        this.mean      = builder.mean;
    }

    public String     getName()      { return name; }
    public ColumnType getType()      { return type; }
    public List<?>    getValues()    { return values; }
    public int        size()         { return values.size(); }
    public long       getNullCount() { return nullCount; }
    public Double     getMin()       { return min; }
    public Double     getMax()       { return max; }
    public Double     getMean()      { return mean; }

    public double nullRatio() {
        return values.isEmpty() ? 0.0 : (double) nullCount / values.size();
    }

    public long estimatedBytes() {
        return switch (type) {
            case NUMERIC    -> (long) values.size() * 8;
            case BOOLEAN    -> (long) values.size();
            case TEMPORAL   -> (long) values.size() * 12;
            case CATEGORICAL -> values.stream()
                    .filter(Objects::nonNull)
                    .mapToLong(v -> ((String) v).length() * 2L)
                    .sum();
        };
    }

    /**
     * 🔥 НОВЕ: унікальні значення як String (зручно для UI)
     */
    public Set<String> getDistinctValuesAsString() {
        return values.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public String toString() {
        return String.format("DataColumn{name='%s', type=%s, size=%d}", name, type, values.size());
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String     name;
        private ColumnType type;
        private List<?>    values    = List.of();
        private long       nullCount = 0;
        private Double     min, max, mean;

        public Builder name(String n)     { this.name = n; return this; }
        public Builder type(ColumnType t) { this.type = t; return this; }
        public Builder values(List<?> v)  { this.values = v; return this; }
        public Builder nullCount(long n)  { this.nullCount = n; return this; }
        public Builder min(Double v)      { this.min = v; return this; }
        public Builder max(Double v)      { this.max = v; return this; }
        public Builder mean(Double v)     { this.mean = v; return this; }
        public DataColumn build()         { return new DataColumn(this); }
    }
}