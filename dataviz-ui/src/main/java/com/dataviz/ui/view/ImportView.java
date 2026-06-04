package com.dataviz.ui.view;

import com.dataviz.ui.ServiceLocatorHolder;
import com.dataviz.ui.presenter.ImportPresenter;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ImportView implements IImportView {

    @FXML private ProgressBar   importProgress;
    @FXML private Label         progressLabel;
    @FXML private Button        btnImport;
    @FXML private Label         errorLabel;
    @FXML private VBox          filePanel;
    @FXML private VBox          jdbcPanel;
    @FXML private VBox          progressSection;
    @FXML private VBox          dropZone;
    @FXML private VBox          csvOptions;
    @FXML private TextField     filePathField;
    @FXML private TextField     datasetNameField;
    @FXML private CheckBox      cbHeader;
    @FXML private ComboBox<String> encodingCombo;
    @FXML private ComboBox<String> jdbcDriverCombo;
    @FXML private TextField     jdbcHostField;
    @FXML private TextField     jdbcPortField;
    @FXML private TextField     jdbcDbField;
    @FXML private TextField     jdbcUserField;
    @FXML private PasswordField jdbcPassField;
    @FXML private TextArea      jdbcQueryArea;
    @FXML private Label         connectionStatusLabel;
    @FXML private ToggleGroup   sourceTypeGroup;
    @FXML private ToggleGroup   delimiterGroup;
    @FXML private RadioButton   rbFile;
    @FXML private RadioButton   rbJdbc;

    private ImportPresenter presenter;
    private Path selectedPath;
    private String currentDelimiter = ",";

    @FXML
    private void initialize() {
        ImportPresenter resolvedPresenter = ServiceLocatorHolder.get().get(ImportPresenter.class);
        resolvedPresenter.attachView(this);
        this.presenter = resolvedPresenter;

        ui(() -> {
            hideError();
            setProgressVisible(false);
            updateImportButton();
            updateCsvOptionsVisibility("");

            if (rbFile != null) rbFile.setSelected(true);
            if (filePanel != null) { filePanel.setVisible(true);  filePanel.setManaged(true);  }
            if (jdbcPanel != null) { jdbcPanel.setVisible(false); jdbcPanel.setManaged(false); }
        });
    }

        @FXML
        private void onSelectFileSource(javafx.scene.input.MouseEvent e) {
            if (rbFile != null) rbFile.setSelected(true);
            onSourceTypeChanged(null);
        }

        @FXML
        private void onSelectJdbcSource(javafx.scene.input.MouseEvent e) {
            if (rbJdbc != null) rbJdbc.setSelected(true);
            onSourceTypeChanged(null);
        }

    @FXML
    private void onSourceTypeChanged(ActionEvent event) {
        boolean isFile = rbFile != null && rbFile.isSelected();
        ui(() -> {
            if (filePanel != null) { filePanel.setVisible(isFile);  filePanel.setManaged(isFile);  }
            if (jdbcPanel != null) { jdbcPanel.setVisible(!isFile); jdbcPanel.setManaged(!isFile); }
            updateImportButton();
        });
    }

    @FXML
    private void onBrowseFile(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Оберіть файл даних");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Файли даних", "*.csv", "*.tsv", "*.json", "*.ndjson"),
                new FileChooser.ExtensionFilter("Всі файли", "*.*")
        );

        File file = chooser.showOpenDialog(getWindowOwner());
        if (file != null) {
            selectedPath = file.toPath();
            ui(() -> {
                if (filePathField != null) filePathField.setText(file.getAbsolutePath());
                if (datasetNameField != null && datasetNameField.getText().isBlank())
                    datasetNameField.setText(file.getName().replaceFirst("[.][^.]+$", ""));
                updateCsvOptionsVisibility(file.getName());
                updateImportButton();
            });
        }
    }

    @FXML
    private void onDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) event.acceptTransferModes(TransferMode.COPY);
        event.consume();
    }

    @FXML
    private void onDragDropped(DragEvent event) {
        var db = event.getDragboard();
        if (db.hasFiles() && !db.getFiles().isEmpty()) {
            File file = db.getFiles().get(0);
            selectedPath = file.toPath();
            ui(() -> {
                if (filePathField != null) filePathField.setText(file.getAbsolutePath());
                updateCsvOptionsVisibility(file.getName());
                updateImportButton();
            });
        }
        event.setDropCompleted(true);
        event.consume();
    }

    @FXML
    private void onDelimiterChanged(ActionEvent event) {
        if (delimiterGroup != null && delimiterGroup.getSelectedToggle() != null) {
            Object ud = delimiterGroup.getSelectedToggle().getUserData();
            currentDelimiter = ud != null ? ud.toString() : ",";
        }
    }

    @FXML private void onHeaderToggled(ActionEvent event)  {}
    @FXML private void onEncodingChanged(ActionEvent event) {}

    @FXML
    private void onDriverChanged(ActionEvent event) {
        if (jdbcDriverCombo == null || jdbcDriverCombo.getValue() == null) return;
        ui(() -> {
            switch (jdbcDriverCombo.getValue()) {
                case "PostgreSQL" -> {
                    if (jdbcPortField != null) jdbcPortField.setPromptText("5432");
                    if (jdbcHostField != null) jdbcHostField.setDisable(false);
                    if (jdbcPortField != null) jdbcPortField.setDisable(false);
                }
                case "MySQL" -> {
                    if (jdbcPortField != null) jdbcPortField.setPromptText("3306");
                    if (jdbcHostField != null) jdbcHostField.setDisable(false);
                    if (jdbcPortField != null) jdbcPortField.setDisable(false);
                }
                case "SQLite" -> {
                    if (jdbcHostField != null) jdbcHostField.setDisable(true);
                    if (jdbcPortField != null) jdbcPortField.setDisable(true);
                }
                default -> {
                    if (jdbcHostField != null) jdbcHostField.setDisable(false);
                    if (jdbcPortField != null) jdbcPortField.setDisable(false);
                }
            }
        });
    }

    @FXML
    private void onTestConnection(ActionEvent event) {
        ui(() -> {
            if (connectionStatusLabel != null) {
                connectionStatusLabel.setText("Перевірка…");
                connectionStatusLabel.setStyle("-fx-text-fill: gray;");
                connectionStatusLabel.setVisible(true);
                connectionStatusLabel.setManaged(true);
            }
        });

        Thread.ofVirtual().start(() -> {
            boolean success = testJdbcConnection();
            ui(() -> {
                if (connectionStatusLabel != null) {
                    if (success) {
                        connectionStatusLabel.setText("✓ З'єднання успішне");
                        connectionStatusLabel.setStyle("-fx-text-fill: #2e7d32;");
                    } else {
                        connectionStatusLabel.setText("✗ Не вдалося підключитись");
                        connectionStatusLabel.setStyle("-fx-text-fill: #c62828;");
                    }
                }
                updateImportButton();
            });
        });
    }

    @FXML
        private void onImport(ActionEvent event) {
        hideError();
        if (presenter == null) { showError("Помилка", "Імпорт-представник не ініціалізовано."); return; }
        if (rbFile != null && rbFile.isSelected()) presenter.onStartImportClicked();
        else                                        presenter.onStartJdbcImportClicked(buildJdbcPath());
    }

    @FXML
    private void onCancel(ActionEvent event) {
        if (presenter != null) presenter.onCancelClicked();
        ui(() -> { Stage stage = getStage(); if (stage != null) stage.close(); });
    }

    @Override
    public void setProgressVisible(boolean visible) {
        ui(() -> {
            if (progressSection != null) {
                progressSection.setVisible(visible);
                progressSection.setManaged(visible);
            }
        });
    }

    @Override
    public void updateProgress(double value) {
        ui(() -> {
            if (importProgress != null) importProgress.setProgress(value);
            if (progressLabel  != null)
                progressLabel.setText("Завантаження… %.0f%%".formatted(value * 100));
        });
    }

    @Override
    public void setStatusText(String text) {
        ui(() -> { if (progressLabel != null) progressLabel.setText(text); });
    }

    @Override
    public void showError(String title, String message) {
        ui(() -> {
            if (errorLabel != null) {
                errorLabel.setText(title + ": " + message);
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
        });
    }

    @Override
    public void goToNextStep() {
        ui(() -> { Stage stage = getStage(); if (stage != null) stage.close(); });
    }

    @Override
    public void showDataPreview(String[][] rows, String[] headers) {
        if (headers == null || headers.length == 0) return;
        ui(() -> DataPreviewDialog.show(rows, headers, getWindowOwner(), getOwnerStylesheets()));
    }

    @Override
    public void setNextButtonEnabled(boolean enabled) {
        ui(() -> { if (btnImport != null) btnImport.setDisable(!enabled); });
    }

    @Override
    public Path getSelectedFilePath() { return selectedPath; }

    @Override
    public void startJdbcImport(Path jdbcPath) {
        if (presenter != null) {
            ui(() -> presenter.onStartJdbcImportClicked(jdbcPath));
        }
    }

    private boolean testJdbcConnection() {
        try {
            Path jdbc = buildJdbcPath();
            String url = jdbc.toString();
            int queryIdx = url.indexOf("&query=");
            String connUrl = queryIdx > 0 ? url.substring(0, queryIdx) : url;

            if (!connUrl.startsWith("jdbc:")) connUrl = "jdbc:" + connUrl;

            try (java.sql.Connection conn = java.sql.DriverManager.getConnection(connUrl)) {
                return conn.isValid(3);
            }
        } catch (Exception e) {
            return false;
        }
    }

    private void updateImportButton() {
        boolean canImport = rbFile != null && rbFile.isSelected()
                ? selectedPath != null
                : jdbcHostField != null && jdbcQueryArea != null
                && !jdbcHostField.getText().isBlank()
                && !jdbcQueryArea.getText().isBlank();
        if (btnImport != null) btnImport.setDisable(!canImport);
    }

    private void updateCsvOptionsVisibility(String fileName) {
        boolean isCsv = fileName != null && (fileName.endsWith(".csv") || fileName.endsWith(".tsv"));
        if (csvOptions != null) { csvOptions.setVisible(isCsv); csvOptions.setManaged(isCsv); }
    }

    private void hideError() {
        ui(() -> {
            if (errorLabel != null) { errorLabel.setVisible(false); errorLabel.setManaged(false); }
        });
    }

    private Path buildJdbcPath() {
        String driver = jdbcDriverCombo != null && jdbcDriverCombo.getValue() != null
                ? jdbcDriverCombo.getValue().toLowerCase() : "";
        String host  = jdbcHostField  != null ? jdbcHostField.getText()  : "";
        String port  = jdbcPortField  != null ? jdbcPortField.getText()  : "";
        String db    = jdbcDbField    != null ? jdbcDbField.getText()    : "";
        String user  = jdbcUserField  != null ? jdbcUserField.getText()  : "";
        String pass  = jdbcPassField  != null ? jdbcPassField.getText()  : "";
        String query = jdbcQueryArea  != null ? jdbcQueryArea.getText()  : "";
        return Path.of("jdbc:%s://%s:%s/%s?user=%s&password=%s&query=%s"
                .formatted(driver, host, port, db, user, pass, query));
    }

    private Stage getStage() {
        if (btnImport   != null && btnImport.getScene()   != null) return (Stage) btnImport.getScene().getWindow();
        if (filePathField != null && filePathField.getScene() != null) return (Stage) filePathField.getScene().getWindow();
        return null;
    }

    private javafx.stage.Window getWindowOwner() {
        if (filePathField != null && filePathField.getScene() != null)
            return filePathField.getScene().getWindow();
        return javafx.stage.Window.getWindows().stream()
                .filter(javafx.stage.Window::isShowing)
                .findFirst().orElse(null);
    }

    private List<String> getOwnerStylesheets() {
        javafx.stage.Window owner = getWindowOwner();
        if (owner instanceof Stage s && s.getScene() != null)
            return new ArrayList<>(s.getScene().getStylesheets());
        return List.of();
    }

    private void ui(Runnable action) {
        if (Platform.isFxApplicationThread()) action.run();
        else                                   Platform.runLater(action);
    }
}