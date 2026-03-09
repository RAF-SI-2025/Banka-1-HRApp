package com.example.repository;

import com.example.model.Task;
import com.example.model.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository responsible for all CRUD operations on the {@code tasks} table.
 * Uses soft-delete: rows are never physically removed.
 */
public class TaskRepository {

    private static final Logger log = LoggerFactory.getLogger(TaskRepository.class);

    private final DatabaseManager dbManager;

    /**
     * Constructs the repository with the required database manager (constructor injection).
     *
     * @param dbManager the shared database manager
     */
    public TaskRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Inserts a new task for the given member and populates the generated {@code id}.
     *
     * @param task     the task to insert
     * @param memberId the owning member's database ID
     * @throws SQLException on database error
     */
    public void save(Task task, long memberId) throws SQLException {
        String sql = "INSERT INTO tasks (task_name, comment, status, member_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = dbManager.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, task.getTaskName());
            ps.setString(2, task.getComment());
            ps.setString(3, task.getStatus().name());
            ps.setLong(4, memberId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    task.setId(keys.getLong(1));
                }
            }
        }
        log.info("Saved task id={} '{}' for member id={}", task.getId(), task.getTaskName(), memberId);
    }

    /**
     * Updates a task's name, comment, and status.
     *
     * @param task the task with updated values (must have a valid id)
     * @throws SQLException on database error
     */
    public void update(Task task) throws SQLException {
        String sql = "UPDATE tasks SET task_name = ?, comment = ?, status = ? WHERE id = ? AND is_deleted = 0";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, task.getTaskName());
            ps.setString(2, task.getComment());
            ps.setString(3, task.getStatus().name());
            ps.setLong(4, task.getId());
            ps.executeUpdate();
        }
        log.info("Updated task id={}", task.getId());
    }

    /**
     * Soft-deletes a task by setting {@code is_deleted = 1}.
     *
     * @param taskId the task's database ID
     * @throws SQLException on database error
     */
    public void softDelete(long taskId) throws SQLException {
        String sql = "UPDATE tasks SET is_deleted = 1 WHERE id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setLong(1, taskId);
            ps.executeUpdate();
        }
        log.info("Soft-deleted task id={}", taskId);
    }

    /**
     * Soft-deletes all non-deleted tasks belonging to the given member.
     * Called when a member is soft-deleted so their tasks are also hidden.
     *
     * @param memberId the owning member's database ID
     * @throws SQLException on database error
     */
    public void softDeleteByMemberId(long memberId) throws SQLException {
        String sql = "UPDATE tasks SET is_deleted = 1 WHERE member_id = ? AND is_deleted = 0";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setLong(1, memberId);
            ps.executeUpdate();
        }
        log.info("Soft-deleted all tasks for member id={}", memberId);
    }

    /**
     * Returns all non-deleted tasks for a given member, ordered by insertion.
     *
     * @param memberId the owning member's database ID
     * @return list of tasks (without a {@link com.example.model.TeamMember} reference)
     * @throws SQLException on database error
     */
    public List<Task> findByMemberId(long memberId) throws SQLException {
        String sql = "SELECT id, task_name, comment, status FROM tasks WHERE member_id = ? AND is_deleted = 0 ORDER BY id";
        List<Task> tasks = new ArrayList<>();
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Task task = new Task(rs.getString("task_name"));
                    task.setId(rs.getLong("id"));
                    task.setComment(rs.getString("comment"));
                    task.setStatus(TaskStatus.valueOf(rs.getString("status")));
                    tasks.add(task);
                }
            }
        }
        return tasks;
    }
}
