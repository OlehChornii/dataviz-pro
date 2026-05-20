package com.dataviz.domain.model;

import java.util.*;
import java.time.LocalDateTime;

public final class DataSet {

    private final String id;
    private final String name;
    private final List<DataColumn> columns;
    private final int rowCount;
    private final LocalDateTime loadedAt;
    private final String sourceDescription;

    private volatile Map<String, Integer> columnIndex;

    private DataSet(Builder builder) {
        this.id                = Objects.requireNonNull(builder.id, "id must not be null");
        this.name              = Objects.requireNonNull(builder.name, "name must not be null");
        this.columns           = List.copyOf(builder.columns);
        this.rowCount          = builder.rowCount;
        this.loadedAt          = builder.loadedAt != null ? builder.loadedAt : LocalDateTime.now();
        this.sourceDescription = builder.sourceDescription != null ? builder.sourceDescription : "";
    }

    public String getId()                { return id; }
    public String getName()              { return name; }
    public List<DataColumn> getColumns() { return columns; }
    public int getRowCount()             { return rowCount; }
    public LocalDateTime getLoadedAt()   { return loadedAt; }
    public String getSourceDescription() { return sourceDescription; }

    public int getColumnCount()          { return columns.size(); }

    public DataColumn getColumn(String name) {
        ensureColumnIndex();
        Integer idx = columnIndex.get(name.toLowerCase());
        if (idx == null) {
            throw new NoSuchElementException("Column not found: " + name);
        }
        return columns.get(idx);
    }

    public boolean hasColumn(String name) {
        ensureColumnIndex();
        return columnIndex.containsKey(name.toLowerCase());
    }

    public List<DataColumn> getColumnsByType(ColumnType type) {
        return columns.stream()
                .filter(c -> c.getType() == type)
                .toList();
    }

    public long estimatedMemoryBytes() {
        return columns.stream()
                .mapToLong(DataColumn::estimatedBytes)
                .sum();
    }

    private void ensureColumnIndex() {
        if (columnIndex == null) {
            synchronized (this) {
                if (columnIndex == null) {
                    Map<String, Integer> idx = new HashMap<>(columns.size() * 2);
                    for (int i = 0; i < columns.size(); i++) {
                        idx.put(columns.get(i).getName().toLowerCase(), i);
                    }
                    columnIndex = Collections.unmodifiableMap(idx);
                }
            }
        }
    }

    @Override
    public String toString() {
        return String.format("DataSet{id='%s', name='%s', rows=%d, cols=%d}",
                id, name, rowCount, columns.size());
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String id;
        private String name;
        private List<DataColumn> columns = new ArrayList<>();
        private int rowCount;
        private LocalDateTime loadedAt;
        private String sourceDescription;

        public Builder id(String id)                       { this.id = id; return this; }
        public Builder name(String name)                   { this.name = name; return this; }
        public Builder columns(List<DataColumn> columns)   { this.columns = new ArrayList<>(columns); return this; }
        public Builder addColumn(DataColumn col)           { this.columns.add(col); return this; }
        public Builder rowCount(int rowCount)               { this.rowCount = rowCount; return this; }
        public Builder loadedAt(LocalDateTime t)           { this.loadedAt = t; return this; }
        public Builder sourceDescription(String src)       { this.sourceDescription = src; return this; }

        public DataSet build()                              { return new DataSet(this); }
    }
}