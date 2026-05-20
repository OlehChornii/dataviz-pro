package com.dataviz.data.reader;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import com.dataviz.di.annotation.*;

@Component
@Singleton
public final class ReaderFactory {

    private static final Logger LOG = Logger.getLogger(ReaderFactory.class.getName());

    private final List<DataReader> readers;

    @Inject
    public ReaderFactory(CsvDataReader csvReader,
                         JsonDataReader jsonReader,
                         JdbcDataReader jdbcReader) {
        this.readers = List.of(jdbcReader, csvReader, jsonReader);
    }

    public DataReader getReader(String fileNameOrUrl) {
        Objects.requireNonNull(fileNameOrUrl, "fileNameOrUrl must not be null");

        String normalized = fileNameOrUrl.trim().toLowerCase();

        return readers.stream()
                .filter(r -> r.supports(normalized))
                .findFirst()
                .orElseThrow(() -> new UnsupportedFormatException(
                        "Непідтримуваний формат: '%s'. Підтримуються: CSV, JSON, JDBC."
                                .formatted(fileNameOrUrl)));
    }

    public boolean isSupported(String fileNameOrUrl) {
        if (fileNameOrUrl == null) return false;
        String normalized = fileNameOrUrl.trim().toLowerCase();
        return readers.stream().anyMatch(r -> r.supports(normalized));
    }

    public List<DataReader> getRegisteredReaders() {
        return readers;
    }

    public static final class UnsupportedFormatException extends RuntimeException {
        public UnsupportedFormatException(String message) {
            super(message);
        }
    }
}