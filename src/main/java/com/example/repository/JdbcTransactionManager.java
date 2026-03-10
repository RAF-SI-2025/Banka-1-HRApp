package com.example.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * JDBC-backed implementation of {@link TransactionManager}.
 * <p>
 * Lives in the repository (infrastructure) layer so that the service layer
 * never has a compile-time dependency on {@link DatabaseManager} or {@link Connection}.
 */
public class JdbcTransactionManager implements TransactionManager {

    private static final Logger log = LoggerFactory.getLogger(JdbcTransactionManager.class);

    private final DatabaseManager dbManager;

    /**
     * Constructs the manager with the required database manager (constructor injection).
     *
     * @param dbManager the shared database manager
     */
    public JdbcTransactionManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beginTransaction() throws SQLException {
        Connection connection = dbManager.getConnection();
        connection.setAutoCommit(false);
        log.debug("Transaction started");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commit() throws SQLException {
        Connection connection = dbManager.getConnection();
        connection.commit();
        connection.setAutoCommit(true);
        log.debug("Transaction committed");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rollback() throws SQLException {
        Connection connection = dbManager.getConnection();
        connection.rollback();
        connection.setAutoCommit(true);
        log.debug("Transaction rolled back");
    }
}

