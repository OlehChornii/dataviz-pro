package com.dataviz.common.config;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import java.util.logging.Logger;

public final class AppConfig {

    private static final Logger   LOG          = Logger.getLogger(AppConfig.class.getName());
    private static final Path     CONFIG_FILE  = Path.of(
            System.getProperty("user.home"), ".dataviz", "app.properties");

    private static volatile AppConfig instance;

    private final Properties props = new Properties();

    private AppConfig() {
        load();
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            synchronized (AppConfig.class) {
                if (instance == null) instance = new AppConfig();
            }
        }
        return instance;
    }

    public String  get(String key, String  defaultValue) { return props.getProperty(key, defaultValue); }
    public int     getInt(String key, int   defaultValue) {
        try { return Integer.parseInt(props.getProperty(key)); }
        catch (Exception e) { return defaultValue; }
    }
    public boolean getBool(String key, boolean def) {
        String v = props.getProperty(key);
        return v == null ? def : Boolean.parseBoolean(v);
    }
    public void set(String key, String value) {
        props.setProperty(key, value);
        save();
    }

    public int     parallelThreshold()    { return getInt("performance.parallel_threshold", 100_000); }
    public int     ioThreads()            { return getInt("performance.io_threads", 4); }
    public int     computeThreads()       { return getInt("performance.compute_threads",
            Runtime.getRuntime().availableProcessors()); }
    public boolean darkTheme()            { return getBool("ui.dark_theme", false); }
    public String  defaultExportPath()    { return get("export.default_path",
            System.getProperty("user.home")); }
    public int     autoSaveIntervalSec()  { return getInt("project.autosave_interval", 300); }

    private void load() {
        if (Files.exists(CONFIG_FILE)) {
            try (InputStream is = Files.newInputStream(CONFIG_FILE)) {
                props.load(is);
                LOG.info("Config loaded from: " + CONFIG_FILE);
            } catch (IOException e) {
                LOG.warning("Failed to load config, using defaults: " + e.getMessage());
            }
        }
        props.putIfAbsent("performance.parallel_threshold", "100000");
        props.putIfAbsent("performance.io_threads",         "4");
        props.putIfAbsent("ui.dark_theme",                  "false");
        props.putIfAbsent("project.autosave_interval",      "300");
    }

    private void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            try (OutputStream os = Files.newOutputStream(CONFIG_FILE)) {
                props.store(os, "DataViz Pro configuration");
            }
        } catch (IOException e) {
            LOG.severe("Failed to save config: " + e.getMessage());
        }
    }
}