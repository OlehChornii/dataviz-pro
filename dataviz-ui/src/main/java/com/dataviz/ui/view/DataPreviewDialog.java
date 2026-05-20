package com.dataviz.ui.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;

public final class DataPreviewDialog {

    private DataPreviewDialog() {}

    public static void show(String[][] rows, String[] headers,
                            Window owner, List<String> stylesheets) {
        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getStyleClass().add("preview-table");

        for (int col = 0; col < headers.length; col++) {
            final int idx = col;
            TableColumn<String[], String> column = new TableColumn<>(headers[idx]);
            column.setCellValueFactory(cell -> {
                String[] row = cell.getValue();
                String val = (row != null && idx < row.length) ? row[idx] : "";
                return new SimpleStringProperty(val);
            });
            column.setSortable(false);
            table.getColumns().add(column);
        }

        if (rows != null && rows.length > 0) {
            int limit = Math.min(rows.length, 100);
            List<String[]> data = new ArrayList<>(limit);
            for (int i = 0; i < limit; i++) data.add(rows[i]);
            table.setItems(FXCollections.observableList(data));
        } else {
            table.setPlaceholder(new Label("Немає даних для відображення"));
        }

        int visibleRows = rows != null ? Math.min(rows.length, 10) : 0;
        table.setPrefHeight(30 + visibleRows * 28.0 + 2);
        table.setMaxHeight(300);

        int totalRows = rows != null ? rows.length : 0;
        Label infoLabel = new Label("Показано %d з %d рядків · %d стовпців"
                .formatted(Math.min(totalRows, 100), totalRows, headers.length));
        infoLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#666;");

        Button btnClose = new Button("Закрити");
        btnClose.setDefaultButton(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer = new HBox(infoLabel, spacer, btnClose);
        footer.setStyle("-fx-padding:8 12 8 12; -fx-spacing:12; -fx-alignment:center-left;");

        BorderPane root = new BorderPane(table);
        root.setBottom(footer);
        root.setStyle("-fx-background-color: white;");

        Scene scene = new Scene(root);
        if (stylesheets != null) scene.getStylesheets().addAll(stylesheets);

        Stage stage = new Stage();
        stage.setTitle("Попередній перегляд даних");
        stage.setScene(scene);
        stage.setWidth(Math.min(headers.length * 120 + 40, 900));
        stage.setMinWidth(360);
        stage.setMinHeight(200);

        if (owner != null) {
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
        }

        btnClose.setOnAction(e -> stage.close());
        stage.show();
    }
}