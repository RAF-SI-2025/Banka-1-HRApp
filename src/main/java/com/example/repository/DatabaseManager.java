package com.example.repository;

import com.example.config.AppConfig;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages the single JDBC connection to the SQLite database.
 * <p>
 * On first access, Flyway runs all pending schema migrations before the
 * application connection is opened. This ensures the schema is always
 * up-to-date regardless of which version of the DB file is loaded.
 * <p>
 * The database file ({@code hrapp.db}) lives in the working directory and
 * can be freely shared between users — Flyway will apply any missing
 * migrations automatically when a colleague opens it with a newer build.
 */
public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        runMigrations();
        openConnection();
    }

    /**
     * Returns the singleton instance, creating it on first call.
     *
     * @return the shared {@link DatabaseManager} instance
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Runs Flyway migrations against the database file.
     * New migrations are applied automatically; already-applied ones are skipped.
     */
    private void runMigrations() {
        log.info("Running Flyway migrations on: {}", AppConfig.DB_URL);
        Flyway flyway = Flyway.configure()
                .dataSource(AppConfig.DB_URL, null, null)
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
        log.info("Flyway migrations complete.");
    }

    /**
     * Opens the JDBC connection used throughout the application's lifetime
     * and enables SQLite foreign key enforcement.
     */
    private void openConnection() {
        try {
            connection = DriverManager.getConnection(AppConfig.DB_URL);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }
            log.info("Database connection opened.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open database connection", e);
        }
    }

    /**
     * Returns the active JDBC connection.
     *
     * @return the {@link Connection} to the SQLite database
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Closes the database connection.  Call this when the application exits.
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                log.info("Database connection closed.");
            }
        } catch (SQLException e) {
            log.error("Error closing database connection", e);
        }
    }
}
