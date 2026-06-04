package com.dataviz.data.reader;

import com.dataviz.domain.model.DataSet;
import java.nio.file.Path;
import java.util.function.Consumer;

public interface DataReader {
    boolean supports(String fileName);
    DataSet read(Path path);
    void setProgressCallback(Consumer<Double> callback);
}