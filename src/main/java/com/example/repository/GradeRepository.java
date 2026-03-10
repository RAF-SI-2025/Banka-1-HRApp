package com.example.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for CRUD operations on the {@code grades} table.
 * Each grade is tied to exactly one completed task (UNIQUE constraint on task_id).
 */
public class GradeRepository {

    private static final Logger log = LoggerFactory.getLogger(GradeRepository.class);

    private final DatabaseManager dbManager;

    public GradeRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Inserts a grade for the given task / member pair.
     *
     * @param grade    grade value
     * @param memberId owning member's DB id
     * @param taskId   the completed task's DB id
     * @throws SQLException on database error
     */
    public void saveForTask(int grade, long memberId, long taskId) throws SQLException {
        String sql = "INSERT INTO grades (grade, member_id, task_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, grade);
            ps.setLong(2, memberId);
            ps.setLong(3, taskId);
            ps.executeUpdate();
        }
        log.info("Saved grade={} for member id={} task id={}", grade, memberId, taskId);
    }

    /**
     * Returns the grade for a specific task, or {@code null} if none exists.
     *
     * @param taskId the task's DB id
     * @throws SQLException on database error
     */
    public Integer findByTaskId(long taskId) throws SQLException {
        String sql = "SELECT grade FROM grades WHERE task_id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setLong(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("grade") : null;
            }
        }
    }

    /**
     * Returns true if a grade already exists for the given task.
     *
     * @param taskId the task's DB id
     * @throws SQLException on database error
     */
    public boolean existsForTask(long taskId) throws SQLException {
        String sql = "SELECT 1 FROM grades WHERE task_id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setLong(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Returns all grade values for a member (used for average calculation).
     *
     * @param memberId the owning member's DB id
     * @throws SQLException on database error
     */
    public List<Integer> findByMemberId(long memberId) throws SQLException {
        String sql = "SELECT grade FROM grades WHERE member_id = ? ORDER BY id";
        List<Integer> grades = new ArrayList<>();
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) grades.add(rs.getInt("grade"));
            }
        }
        return grades;
    }

    /**
     * Updates the grade value for an existing grade row.
     *
     * @param taskId   the task whose grade to update
     * @param newGrade the new grade value
     * @throws SQLException on database error
     */
    public void updateByTaskId(long taskId, int newGrade) throws SQLException {
        String sql = "UPDATE grades SET grade = ? WHERE task_id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, newGrade);
            ps.setLong(2, taskId);
            ps.executeUpdate();
        }
        log.info("Updated grade for task id={} to value={}", taskId, newGrade);
    }

    /**
     * Deletes the grade for a given task (e.g. task moved away from COMPLETED).
     *
     * @param taskId the task's DB id
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
}
