module dataviz.ui {

    requires dataviz.domain;
    requires dataviz.service;
    requires dataviz.data;

    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    requires de.gsi.chartfx.chart;
    requires de.gsi.chartfx.dataset; 
    requires de.gsi.chartfx.math;
    requires org.jfree.jfreechart;
    requires jfreechart.fx;

    requires org.controlsfx.controls;

    requires java.logging;
    requires java.sql;
    requires java.rmi;
    requires java.desktop;

    opens com.dataviz.ui.view;
    opens com.dataviz.ui.presenter;
    opens com.dataviz.ui.chart;

    exports com.dataviz.ui;
    exports com.dataviz.ui.view;
    exports com.dataviz.ui.presenter;
    exports com.dataviz.ui.chart;
}