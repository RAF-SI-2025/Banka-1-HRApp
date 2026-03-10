package com.example.repository;

import java.sql.SQLException;

/**
 * Abstraction for database transaction management used by the service layer.
 * <p>
 * The service layer depends only on this interface — it never touches
 * infrastructure classes such as {@code DatabaseManager} or {@code Connection}.
 * The concrete implementation lives in the repository (infrastructure) layer.
 */
public interface TransactionManager {

    /**
     * Begins a new transaction by disabling auto-commit on the underlying connection.
     *
     * @throws SQLException on database error
     */
    void beginTransaction() throws SQLException;

    /**
     * Commits the current transaction.
     *
     * @throws SQLException on database error
     */
    void commit() throws SQLException;

    /**
     * Rolls back the current transaction.
     *
     * @throws SQLException on database error
     */
    void rollback() throws SQLException;
}

