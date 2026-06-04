module dataviz.app {
    requires dataviz.domain;
    requires dataviz.data;
    requires dataviz.service;
    requires dataviz.ui;

    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.logging;

    opens com.dataviz.app to javafx.graphics;
}
