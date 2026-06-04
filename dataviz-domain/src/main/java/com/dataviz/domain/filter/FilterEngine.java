package com.dataviz.domain.filter;

import com.dataviz.domain.model.DataColumn;
import com.dataviz.domain.model.DataSet;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class FilterEngine {

    private static final int PARALLEL_THRESHOLD = 100_000;

    private FilterEngine() {}

    public static List<Integer> apply(DataSet dataSet, List<FilterCriteria> criteria) {
        Objects.requireNonNull(dataSet, "dataSet must not be null");

        if (dataSet.getRowCount() == 0) {
            return List.of();
        }
        if (criteria == null || criteria.isEmpty()) {
            return IntStream.range(0, dataSet.getRowCount())
                    .boxed()
                    .collect(Collectors.toList());
        }

        Predicate<Integer> combined = criteria.stream()
                .map(c -> buildPredicate(dataSet, c))
                .reduce(i -> true, Predicate::and);

        IntStream stream = IntStream.range(0, dataSet.getRowCount());
        if (dataSet.getRowCount() > PARALLEL_THRESHOLD) {
            stream = stream.parallel();
        }

        return stream.boxed()
                .filter(combined)
                .collect(Collectors.toList());
    }

    private static Predicate<Integer> buildPredicate(DataSet ds, FilterCriteria c) {
        if (!ds.hasColumn(c.getColumnName())) {
            throw new IllegalArgumentException(
                    "Column not found: '%s'".formatted(c.getColumnName()));
        }
        DataColumn col = ds.getColumn(c.getColumnName());

        Predicate<Integer> predicate = switch (c.getFilterType()) {
            case NUMERIC_RANGE   -> numericRangePredicate(col, c);
            case CATEGORICAL_IN  -> categoricalInPredicate(col, c);
            case STRING_CONTAINS -> stringContainsPredicate(col, c);
            case BOOLEAN_EQUALS  -> booleanEqualsPredicate(col, c);
            case IS_NOT_NULL     -> i -> col.getValues().get(i) != null;
        };

        return c.isNegated() ? predicate.negate() : predicate;
    }

    @SuppressWarnings("unchecked")
    private static Predicate<Integer> numericRangePredicate(DataColumn col, FilterCriteria c) {
        double lo = c.getMinValue() != null
                ? ((Number) c.getMinValue()).doubleValue()
                : Double.NEGATIVE_INFINITY;
        double hi = c.getMaxValue() != null
                ? ((Number) c.getMaxValue()).doubleValue()
                : Double.POSITIVE_INFINITY;

        List<Object> vals = (List<Object>) col.getValues();
        return i -> {
            Object raw = vals.get(i);
            if (raw == null) return false;
            double v = ((Number) raw).doubleValue();
            return v >= lo && v <= hi;
        };
    }

    @SuppressWarnings("unchecked")
    private static Predicate<Integer> categoricalInPredicate(DataColumn col, FilterCriteria c) {
        Set<Object> allowed = new HashSet<>(c.getAllowedValues());
        List<Object> vals   = (List<Object>) col.getValues();
        return i -> {
            Object v = vals.get(i);
            return v != null && allowed.contains(v);
        };
    }

    @SuppressWarnings("unchecked")
    private static Predicate<Integer> stringContainsPredicate(DataColumn col, FilterCriteria c) {
        String pattern = c.getSearchString() != null
                ? c.getSearchString().toLowerCase()
                : "";
        List<Object> vals = (List<Object>) col.getValues();
        return i -> {
            Object raw = vals.get(i);
            return raw != null && raw.toString().toLowerCase().contains(pattern);
        };
    }

    @SuppressWarnings("unchecked")
    private static Predicate<Integer> booleanEqualsPredicate(DataColumn col, FilterCriteria c) {
        Boolean expected  = c.getBoolValue();
        List<Object> vals = (List<Object>) col.getValues();
        return i -> {
            Object raw = vals.get(i);
            if (raw == null || expected == null) return false;
            if (raw instanceof Boolean b) return b.equals(expected);
            return Boolean.parseBoolean(raw.toString()) == expected;
        };
    }
}