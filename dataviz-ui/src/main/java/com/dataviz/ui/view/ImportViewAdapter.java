package com.dataviz.ui.view;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ImportViewAdapter implements IImportView {

    private final DashboardView dashboardView;
    private Path selectedPath;

    public ImportViewAdapter(DashboardView dashboardView) {
        this.dashboardView = dashboardView;
    }

    @Override
    public void setProgressVisible(boolean visible) {
        ui(() -> dashboardView.showImportProgress(visible));
    }

    @Override
    public void updateProgress(double value) {
        ui(() -> dashboardView.updateImportProgress(value));
    }

    @Override
    public void setStatusText(String text) {
        ui(() -> dashboardView.setStatus(text));
    }

    @Override
    public void showError(String title, String message) {
        ui(() -> dashboardView.showImportError(title + ": " + message));
    }

    @Override
    public void goToNextStep() {
        ui(() -> dashboardView.onImportSuccess());
    }

    @Override
    public void showDataPreview(String[][] rows, String[] headers) {
        if (headers == null || headers.length == 0) return;
        ui(() -> DataPreviewDialog.show(rows, headers, getPrimaryStage(), List.of()));
    }

    @Override
    public void setNextButtonEnabled(boolean enabled) {
        ui(() -> dashboardView.setImportButtonEnabled(enabled));
    }

    @Override
    public Path getSelectedFilePath() {
        return selectedPath;
    }

    public void setSelectedPath(Path path) {
        this.selectedPath = path;
    }

    @Override
    public void startJdbcImport(Path jdbcPath) {
        this.selectedPath = jdbcPath;
    }

    private static Stage getPrimaryStage() {
        return Window.getWindows().stream()
                .filter(Window::isShowing)
                .filter(w -> w instanceof Stage)
                .map(w -> (Stage) w)
                .findFirst().orElse(null);
    }

    private void ui(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }
}