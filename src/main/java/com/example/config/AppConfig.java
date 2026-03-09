package com.example.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Central application configuration.
 * All configurable values are defined here — no hardcoded values elsewhere in the codebase.
 * <p>
 * The SQLite database file ({@code hrapp.db}) is stored in the current working directory
 * so it can easily be located, backed up, and shared with colleagues.
 */
public final class
AppConfig {

    /**
     * JDBC connection URL for the SQLite database.
     * The file is placed in the application's working directory for easy sharing.
     */
    public static final String DB_URL = "jdbc:sqlite:hrapp.db";

    /**
     * Directory used for application log files (separate from the shareable DB file).
     */
    public static final Path LOG_DIR = Paths.get(System.getProperty("user.home"), "hrapp-logs");

    /** Main window title. */
    public static final String APP_TITLE = "HR App";

    /** Main window width in pixels. */
    public static final int APP_WIDTH = 1100;

    /** Main window height in pixels. */
    public static final int APP_HEIGHT = 700;

    /** SplitPane divider position (0.0 – 1.0). */
    public static final double DIVIDER_POSITION = 0.35;

    /** Minimum allowed grade value (inclusive). */
    public static final int GRADE_MIN = 1;

    /** Maximum allowed grade value (inclusive). */
    public static final int GRADE_MAX = 10;

    /** Maximum length for a member's first or last name. */
    public static final int MAX_NAME_LENGTH = 100;

    /** Maximum length for a skill name. */
    public static final int MAX_SKILL_LENGTH = 100;

    /** Maximum length for a task name. */
    public static final int MAX_TASK_NAME_LENGTH = 200;

    /** Maximum length for a task comment. */
    public static final int MAX_COMMENT_LENGTH = 500;

    private AppConfig() {
        // Utility class — not instantiable.
    }
}
