module dataviz.data {
    requires dataviz.domain;
    requires com.fasterxml.jackson.databind;
    requires java.sql;
    requires org.apache.commons.csv;

    exports com.dataviz.data.reader;
    exports com.dataviz.data.repository;
}