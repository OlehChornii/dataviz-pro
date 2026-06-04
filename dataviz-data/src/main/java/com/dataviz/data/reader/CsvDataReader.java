package com.dataviz.data.reader;

import com.dataviz.domain.model.ColumnType;
import com.dataviz.domain.model.DataColumn;
import com.dataviz.domain.model.DataSet;
import com.dataviz.di.annotation.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

@Repository("csv")
@Singleton
public final class CsvDataReader implements DataReader {

    @Override
    public boolean supports(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".csv") || lower.endsWith(".tsv");
    }

    @Override
    public DataSet read(Path path) {
        String fileName = path.getFileName().toString();
        char delimiter = fileName.endsWith(".tsv") ? '\t' : ',';

        List<String> colNames = new ArrayList<>();
        List<List<Object>> colData  = new ArrayList<>();

        try (var reader = java.nio.file.Files.newBufferedReader(path, java.nio.charset.StandardCharsets.UTF_8);
             var csvParser = new org.apache.commons.csv.CSVParser(reader,
                     org.apache.commons.csv.CSVFormat.DEFAULT
                             .withDelimiter(delimiter)
                             .withFirstRecordAsHeader()
                             .withIgnoreHeaderCase()
                             .withTrim())) {

            colNames.addAll(csvParser.getHeaderNames());
            for (int i = 0; i < colNames.size(); i++) colData.add(new ArrayList<>());

            int rowIndex = 0;
            for (org.apache.commons.csv.CSVRecord record : csvParser) {
                for (int i = 0; i < colNames.size(); i++) {
                    String value = i < record.size() ? record.get(i) : "";
                    colData.get(i).add(value);
                }
                rowIndex++;
                if (rowIndex % 10_000 == 0) progressCallback.accept(Math.min(0.99, rowIndex / 1_000_000.0));
            }
            progressCallback.accept(1.0);

            DataSet.Builder dsBuilder = DataSet.builder()
                    .id(java.util.UUID.randomUUID().toString())
                    .name(fileName)
                    .sourceDescription(path.toAbsolutePath().toString())
                    .rowCount(rowIndex);

            for (int i = 0; i < colNames.size(); i++) {
                dsBuilder.addColumn(DataColumn.builder()
                        .name(colNames.get(i))
                        .type(ColumnType.CATEGORICAL)
                        .values(colData.get(i))
                        .build());
            }
            return dsBuilder.build();

        } catch (java.io.IOException e) {
            throw new RuntimeException("CSV read failed: " + e.getMessage(), e);
        }
    }

    private Consumer<Double> progressCallback = p -> {};

    @Override
    public void setProgressCallback(Consumer<Double> callback) {
        this.progressCallback = Objects.requireNonNull(callback);
    }
}