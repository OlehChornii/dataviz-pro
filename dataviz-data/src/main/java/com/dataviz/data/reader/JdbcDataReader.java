package com.dataviz.data.reader;

import com.dataviz.domain.model.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;
import com.dataviz.di.annotation.*;

@Repository("jdbc")
@Singleton
public final class JdbcDataReader implements DataReader {

    private static final Logger LOG      = Logger.getLogger(JdbcDataReader.class.getName());
    private static final int    MAX_ROWS = 5_000_000;

    private Consumer<Double> progressCallback = p -> {};

    @Override
    public boolean supports(String fileName) {
        return fileName.startsWith("jdbc:");
    }

    @Override
    public DataSet read(Path path) {
        String url   = extractUrl(path.toString());
        String query = extractQuery(path.toString());

        LOG.info(() -> "JDBC read: %s | query: %s".formatted(url, query));

        try (Connection conn = DriverManager.getConnection(url);
             Statement  stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                     ResultSet.CONCUR_READ_ONLY)) {

            stmt.setFetchSize(10_000);
            ResultSet rs = stmt.executeQuery(query);
            return buildDataSet(rs, path.getFileName().toString());

        } catch (SQLException e) {
            throw new RuntimeException("JDBC read failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void setProgressCallback(Consumer<Double> callback) {
        this.progressCallback = Objects.requireNonNull(callback);
    }

    private DataSet buildDataSet(ResultSet rs, String name) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();

        List<String>       colNames = new ArrayList<>();
        List<ColumnType>   colTypes = new ArrayList<>();
        List<List<Object>> colData  = new ArrayList<>();

        for (int i = 1; i <= colCount; i++) {
            colNames.add(meta.getColumnLabel(i));
            colTypes.add(mapSqlType(meta.getColumnType(i)));
            colData.add(new ArrayList<>());
        }

        int rowIndex = 0;
        while (rs.next()) {
            if (rowIndex >= MAX_ROWS) {
                LOG.warning("MAX_ROWS (%,d) reached — truncating result".formatted(MAX_ROWS));
                break;
            }
            for (int i = 0; i < colCount; i++) {
                colData.get(i).add(rs.getObject(i + 1));
            }
            rowIndex++;
            if (rowIndex % 100_000 == 0) {
                double progress = Math.min(1.0, (double) rowIndex / MAX_ROWS);
                progressCallback.accept(progress);
            }
        }

        progressCallback.accept(1.0);

        DataSet.Builder dsBuilder = DataSet.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .rowCount(rowIndex);

        for (int i = 0; i < colCount; i++) {
            dsBuilder.addColumn(DataColumn.builder()
                    .name(colNames.get(i))
                    .type(colTypes.get(i))
                    .values(colData.get(i))
                    .build());
        }

        return dsBuilder.build();
    }

    private ColumnType mapSqlType(int sqlType) {
        return switch (sqlType) {
            case Types.INTEGER, Types.BIGINT,
                 Types.FLOAT,   Types.DOUBLE,
                 Types.NUMERIC, Types.DECIMAL -> ColumnType.NUMERIC;
            case Types.DATE, Types.TIMESTAMP,
                 Types.TIME                   -> ColumnType.TEMPORAL;
            case Types.BOOLEAN, Types.BIT     -> ColumnType.BOOLEAN;
            default                           -> ColumnType.CATEGORICAL;
        };
    }

    private String extractUrl(String full) {
        int q = full.indexOf("&query=");
        if (q < 0) q = full.indexOf("?query=");
        return q >= 0 ? full.substring(0, q) : full;
    }

    private String extractQuery(String full) {
        String marker = "query=";
        int idx = full.indexOf(marker);
        if (idx < 0) throw new IllegalArgumentException("JDBC path must contain 'query=' parameter");
        return full.substring(idx + marker.length());
    }
}