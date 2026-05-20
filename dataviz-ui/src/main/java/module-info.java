/*
 * module-info.java для dataviz-ui
 *
 * ChartFX — модульна бібліотека (JPMS-сумісна з Java 11+).
 * Потрібно явно відкрити пакети контролерів для JavaFX reflection.
 */
module dataviz.ui {

    // ── Внутрішні залежності ─────────────────────────────────────────────────
    requires dataviz.domain;
    requires dataviz.service;
    requires dataviz.data;

    // ── JavaFX ───────────────────────────────────────────────────────────────
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    // ── ChartFX ───────────────────────────────────────────────────────────────
    requires de.gsi.chartfx.chart;   // XYChart, XYChart.Renderer, plugins
    requires de.gsi.chartfx.dataset; // DataSet2D, AbstractDataSet, DoubleDataSet
    requires de.gsi.chartfx.math;    // математичні утиліти (опційно)
    requires org.jfree.jfreechart;
    requires jfreechart.fx;

    requires org.controlsfx.controls;

    // ── Логування ─────────────────────────────────────────────────────────────
    requires java.logging;
    requires java.sql;               // JDBC у ImportView / DashboardView
    requires java.rmi;
    requires java.desktop;

    // ── Відкриваємо пакети для reflection (FXMLLoader + DI) ────────────────
    opens com.dataviz.ui.view;
    opens com.dataviz.ui.presenter;
    opens com.dataviz.ui.chart;

    // ── Exports (для DI-контейнера і тестів) ──────────────────────────────────
    exports com.dataviz.ui;
    exports com.dataviz.ui.view;
    exports com.dataviz.ui.presenter;
    exports com.dataviz.ui.chart;
}