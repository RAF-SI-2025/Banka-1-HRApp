package com.example.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * Repository responsible for grade operations on the {@code grades} table.
 * Each task can have at most one grade (enforced by UNIQUE constraint on task_id).
 */
public class GradeRepository {

    private static final Logger log = LoggerFactory.getLogger(GradeRepository.class);

    private final DatabaseManager dbManager;

    /**
     * Constructs the repository with the required database manager (constructor injection).
     *
     * @param dbManager the shared database manager
     */
    public GradeRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Inserts or replaces the grade for the given task.
     * If a grade already exists for the task it is overwritten.
     *
     * @param grade  the grade value (must be within the configured min/max range)
     * @param taskId the task's database ID
     * @throws SQLException on database error
     */
    public void saveOrUpdate(int grade, long taskId) throws SQLException {
        String sql = "INSERT OR REPLACE INTO grades (grade, task_id) VALUES (?, ?)";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, grade);
            ps.setLong(2, taskId);
            ps.executeUpdate();
        }
        log.info("Saved/updated grade={} for task id={}", grade, taskId);
    }

    /**
     * Returns the grade for the given task, or {@code 0} if no grade has been set.
     *
     * @param taskId the task's database ID
     * @return the grade value, or 0 if absent
     * @throws SQLException on database error
     */
    /**
     * Deletes the grade for the given task, if one exists.
     *
     * @param taskId the task's database ID
     * @throws SQLException on database error
     */
    public void deleteByTaskId(long taskId) throws SQLException {
        String sql = "DELETE FROM grades WHERE task_id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setLong(1, taskId);
            ps.executeUpdate();
        }
        log.info("Deleted grade for task id={}", taskId);
    }

    public int findByTaskId(long taskId) throws SQLException {
        String sql = "SELECT grade FROM grades WHERE task_id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setLong(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("grade") : 0;
            }
        }
    }
}
