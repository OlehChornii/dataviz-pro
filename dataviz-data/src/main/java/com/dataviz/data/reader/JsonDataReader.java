package com.dataviz.data.reader;

import com.dataviz.domain.model.DataSet;
import com.dataviz.domain.model.DataColumn;
import com.dataviz.domain.model.ColumnType;
import com.dataviz.di.annotation.Repository;
import com.dataviz.di.annotation.Singleton;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

/**
 * Reads .json and .ndjson files into a DataSet.
 *
 * FIX 1: {@code setProgressCallback} тепер зберігає callback (раніше метод був порожнім).
 * FIX 2: Прогрес повідомляється кожні 10 000 рядків і після завершення.
 */
@Repository("json")
@Singleton
public final class JsonDataReader implements DataReader {

    private Consumer<Double> progressCallback;

    @Override
    public boolean supports(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".json") || lower.endsWith(".ndjson");
    }

    @Override
    public void setProgressCallback(Consumer<Double> callback) {
        // FIX: раніше цей метод був порожнім — тепер зберігаємо callback
        this.progressCallback = callback;
    }

    @Override
    public DataSet read(Path path) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(path.toFile());

            com.fasterxml.jackson.databind.node.ArrayNode array;
            if (root.isArray()) {
                array = (com.fasterxml.jackson.databind.node.ArrayNode) root;
            } else {
                // Шукаємо перший масив у кореневому об'єкті
                com.fasterxml.jackson.databind.JsonNode found = null;
                var fieldIt = root.fields();
                while (fieldIt.hasNext()) {
                    var entry = fieldIt.next();
                    if (entry.getValue().isArray()) { found = entry.getValue(); break; }
                }
                if (found == null)
                    throw new IllegalArgumentException("JSON не містить масиву об'єктів");
                array = (com.fasterxml.jackson.databind.node.ArrayNode) found;
            }

            if (array.isEmpty()) {
                reportProgress(1.0);
                return DataSet.builder()
                        .id(UUID.randomUUID().toString())
                        .name(path.getFileName().toString())
                        .sourceDescription(path.toAbsolutePath().toString())
                        .rowCount(0)
                        .build();
            }

            // Збираємо назви колонок з першого елемента
            com.fasterxml.jackson.databind.JsonNode first = array.get(0);
            List<String> colNames = new ArrayList<>();
            first.fieldNames().forEachRemaining(colNames::add);

            Map<String, List<Object>> colData = new LinkedHashMap<>();
            colNames.forEach(n -> colData.put(n, new ArrayList<>()));

            int totalRows = array.size();
            int rowIndex  = 0;

            for (com.fasterxml.jackson.databind.JsonNode row : array) {
                for (String col : colNames) {
                    com.fasterxml.jackson.databind.JsonNode val = row.get(col);
                    if (val == null || val.isNull()) {
                        colData.get(col).add(null);
                    } else if (val.isNumber()) {
                        colData.get(col).add(val.doubleValue());
                    } else if (val.isBoolean()) {
                        colData.get(col).add(val.booleanValue());
                    } else {
                        colData.get(col).add(val.asText());
                    }
                }
                rowIndex++;
                // FIX: повідомляємо прогрес кожні 10 000 рядків
                if (rowIndex % 10_000 == 0) {
                    reportProgress((double) rowIndex / totalRows);
                }
            }
            reportProgress(1.0);

            // Будуємо колонки
            List<DataColumn> columns = new ArrayList<>();
            for (String colName : colNames) {
                List<Object> values = colData.get(colName);
                long nullCount = values.stream().filter(Objects::isNull).count();
                columns.add(DataColumn.builder()
                        .name(colName)
                        .type(inferType(values))
                        .values(values)
                        .nullCount(nullCount)
                        .build());
            }

            return DataSet.builder()
                    .id(UUID.randomUUID().toString())
                    .name(path.getFileName().toString())
                    .sourceDescription(path.toAbsolutePath().toString())
                    .columns(columns)
                    .rowCount(totalRows)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to read JSON: " + path, e);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void reportProgress(double value) {
        if (progressCallback != null) {
            progressCallback.accept(value);
        }
    }

    private static ColumnType inferType(List<Object> values) {
        for (Object v : values) {
            if (v == null) continue;
            if (v instanceof Number)  return ColumnType.NUMERIC;
            if (v instanceof Boolean) return ColumnType.BOOLEAN;
            if (v instanceof String s) {
                if (s.matches("\\d{4}-\\d{2}-\\d{2}.*")) return ColumnType.TEMPORAL;
            }
            return ColumnType.CATEGORICAL;
        }
        return ColumnType.CATEGORICAL;
    }
}