package com.dataviz.app;

import com.dataviz.di.context.AppContext;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Logger;

public class DataVizApplication extends Application {

    private static final Logger LOG = Logger.getLogger(DataVizApplication.class.getName());
    private AppContext appContext;

    @Override
    public void init() throws Exception {
        super.init();

        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");

        this.appContext = AppContext.getInstance();
        this.appContext.initialize();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Thread.currentThread().setUncaughtExceptionHandler((t, ex) -> {
            System.err.println("FX thread error: " + ex.getMessage());
            ex.printStackTrace();
        });

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/dashboard.fxml"));

        loader.setControllerFactory(controllerClass -> {
            try {
                return appContext.getContainer().resolve(controllerClass);
            } catch (Exception e) {
                LOG.severe("DI resolve failed for controller: " + controllerClass.getName());
                e.printStackTrace();
                try {
                    return controllerClass.getDeclaredConstructor().newInstance();
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to create controller: " + controllerClass.getName(), ex);
                }
            }
        });

        Parent root = loader.load();
        Scene scene = new Scene(root, 1440, 800);

        scene.getStylesheets().add(
                getClass().getResource("/css/dataviz.css").toExternalForm());
        scene.getStylesheets().add(
                getClass().getResource("/css/dataviz-chartfx.css").toExternalForm());

        primaryStage.setTitle("DataViz Pro");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(600);
        Platform.setImplicitExit(true);
        primaryStage.setOnCloseRequest(event -> {
            LOG.info("Primary stage closing, exiting application.");
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();

        LOG.info("Application started successfully (ChartFX rendering)");
    }

    @Override
    public void stop() throws Exception {
        if (appContext != null) appContext.shutdown();
        LOG.info("Application stopped");
        super.stop();
    }

    public static void main(String[] args) throws IOException {

        System.setOut(new java.io.PrintStream(System.out) {
            @Override public void println(String x) {
                if (x == null) return;
                if (x.contains("thisModule")          || x.contains("methodModule")   ||
                        x.contains("m = public")          || x.contains("getModuleMethod") ||
                        x.contains("JavaFX launchApp")    || x.contains("Loaded library")  ||
                        x.contains("SLF4J:")              || x.contains("de.gsi.chart")    ||
                        x.contains("chartfx")) return;
                super.println(x);
            }
        });

        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            String errorMsg = "Uncaught exception in thread: " + thread.getName();
            System.err.println(errorMsg);
            ex.printStackTrace();
        });

        launch(args);
    }
}