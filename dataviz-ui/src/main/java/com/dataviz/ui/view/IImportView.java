package com.dataviz.ui.view;

import java.nio.file.Path;

public interface IImportView {

    void setProgressVisible(boolean visible);

    void updateProgress(double value);

    void setStatusText(String text);

    void showError(String title, String message);

    void goToNextStep();

    void showDataPreview(String[][] rows, String[] headers);

    void setNextButtonEnabled(boolean enabled);

    Path getSelectedFilePath();

    void startJdbcImport(Path jdbcPath);
}