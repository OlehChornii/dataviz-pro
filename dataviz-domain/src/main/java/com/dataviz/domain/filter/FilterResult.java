package com.dataviz.domain.filter;

import com.dataviz.domain.model.DataSet;
import java.util.List;
import java.util.Objects;

public final class FilterResult {

    private final DataSet    source;
    private final List<Integer> matchedIndices;

    public FilterResult(DataSet source, List<Integer> matchedIndices) {
        this.source         = Objects.requireNonNull(source);
        this.matchedIndices = List.copyOf(matchedIndices);
    }

    public DataSet getSource() { return source; }

    public List<Integer> getMatchedIndices() { return matchedIndices; }

    public int getMatchedCount() { return matchedIndices.size(); }

    public int getTotalCount() { return source.getRowCount(); }

    public double getMatchRatio() {
        return getTotalCount() == 0 ? 0.0
                : (double) getMatchedCount() / getTotalCount();
    }

    public boolean isUnfiltered() {
        return matchedIndices.size() == source.getRowCount();
    }

    @Override
    public String toString() {
        return "FilterResult{matched=%d/%d (%.1f%%)}".formatted(
                getMatchedCount(), getTotalCount(), getMatchRatio() * 100);
    }
}