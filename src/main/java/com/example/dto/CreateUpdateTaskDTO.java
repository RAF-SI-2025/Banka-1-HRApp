package com.example.dto;

import com.example.config.AppConfig;
import com.example.exception.ValidationException;
import com.example.model.TaskStatus;

/**
 * Input DTO for creating or updating a task.
 * Validation is enforced in the factory method {@link #of(String, String, TaskStatus)}.
 */
public class CreateUpdateTaskDTO {

    private final String taskName;
    private final String comment;
    private final TaskStatus status;

    private CreateUpdateTaskDTO(String taskName, String comment, TaskStatus status) {
        this.taskName = taskName;
        this.comment = comment;
        this.status = status;
    }

    /**
     * Creates a validated {@link CreateUpdateTaskDTO}.
     * Trims whitespace before validation.
     *
     * @param taskName the task name
     * @param comment  optional comment (may be {@code null} or blank)
     * @param status   the task status (must not be {@code null})
     * @return a validated DTO
     * @throws ValidationException if task name is blank/too long, comment is too long,
     *                             or status is {@code null}
     */
    public static CreateUpdateTaskDTO of(String taskName, String comment, TaskStatus status) {
        String trimmedName    = taskName != null ? taskName.trim() : "";
        String trimmedComment = comment  != null ? comment.trim()  : "";

        if (trimmedName.isEmpty()) {
            throw new ValidationException("Task name cannot be blank.");
        }
        if (trimmedName.length() > AppConfig.getMaxTaskNameLength()) {
            throw new ValidationException("Task name exceeds maximum length of " + AppConfig.getMaxTaskNameLength() + " characters.");
        }
        if (trimmedComment.length() > AppConfig.getMaxCommentLength()) {
            throw new ValidationException("Comment exceeds maximum length of " + AppConfig.getMaxCommentLength() + " characters.");
        }
        if (status == null) {
            throw new ValidationException("Task status cannot be null.");
        }

        return new CreateUpdateTaskDTO(trimmedName, trimmedComment, status);
    }

    /** @return the validated task name */
    public String getTaskName() { return taskName; }

    /** @return the trimmed comment (never {@code null}) */
    public String getComment() { return comment; }

    /** @return the task status */
    public TaskStatus getStatus() { return status; }
}
