package com.example.config;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central application configuration.
 * All configurable values are defined here — no hardcoded values elsewhere in the codebase.
 * <p>
 * The SQLite database file ({@code hrapp.db}) is stored in the user's home directory
 * so it persists reliably across different working directories.
 */
public final class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    private static final String PROPERTIES_FILE = "app.properties";
    private static final Properties props = new Properties();

    static{
        File file = new File(PROPERTIES_FILE);

        if(file.exists()){

            try (InputStream input = new FileInputStream(file)) {
                props.load(input);
            } catch (IOException e) {
                log.error("Failed to load configuration from '{}': {}", PROPERTIES_FILE, e.getMessage(), e);
                throw new RuntimeException("Failed to load configuration from '" + PROPERTIES_FILE + "'", e);
            }
        }else{

            setDefaultProps();

            try (OutputStream output = new FileOutputStream(file)) {
                props.store(output, "Default properties configuration");
            } catch (IOException e) {
                log.error("Failed to write default configuration to '{}': {}", PROPERTIES_FILE, e.getMessage(), e);
                throw new RuntimeException("Failed to write default configuration to '" + PROPERTIES_FILE + "'", e);
            }

            System.out.println(PROPERTIES_FILE + " created with default values.");
        }

    }

    private static void setDefaultProps() {
        props.setProperty("db.url", "jdbc:sqlite:" + System.getProperty("user.home") + "/hrapp.db");

        props.setProperty("log.dir", System.getProperty("user.home") + "/hrapp-logs");

        props.setProperty("app.title", "HR App");
        props.setProperty("app.width", "1100");
        props.setProperty("app.height", "700");
        props.setProperty("divider.position", "0.35");

        props.setProperty("grade.min", "1");
        props.setProperty("grade.max", "10");

        props.setProperty("max.name.length", "100");
        props.setProperty("max.skill.length", "100");
        props.setProperty("max.task.name.length", "200");
        props.setProperty("max.comment.length", "500");
    }

    public static String getDbUrl() {
        return props.getProperty("db.url");
    }

    public static Path getLogDir() {
        String dir = props.getProperty("log.dir")
                .replace("${user.home}", System.getProperty("user.home"));
        return Paths.get(dir);
    }

    public static String getAppTitle() {
        return props.getProperty("app.title");
    }

    public static int getAppWidth() {
        return Integer.parseInt(props.getProperty("app.width"));
    }

    public static int getAppHeight() {
        return Integer.parseInt(props.getProperty("app.height"));
    }

    public static double getDividerPosition() {
        return Double.parseDouble(props.getProperty("divider.position"));
    }

    public static int getGradeMin() {
        return Integer.parseInt(props.getProperty("grade.min"));
    }

    public static int getGradeMax() {
        return Integer.parseInt(props.getProperty("grade.max"));
    }

    public static int getMaxNameLength() {
        return Integer.parseInt(props.getProperty("max.name.length"));
    }

    public static int getMaxSkillLength() {
        return Integer.parseInt(props.getProperty("max.skill.length"));
    }

    public static int getMaxTaskNameLength() {
        return Integer.parseInt(props.getProperty("max.task.name.length"));
    }

    public static int getMaxCommentLength() {
        return Integer.parseInt(props.getProperty("max.comment.length"));
    }

    private AppConfig(){}
}
