package com.example.dto;

import com.example.model.TaskStatus;

/**
 * Read-only data transfer object representing a single task.
 * Used to pass task data from the service layer to the view layer.
 */
public class TaskDTO {

    private final long id;
    private final String taskName;
    private final TaskStatus status;
    private final String comment;
    /** The grade awarded for this task, or {@code null} if not yet graded. */
    private final Integer grade;

    /**
     * Constructs a TaskDTO without a grade (task not yet completed/graded).
     */
    public TaskDTO(long id, String taskName, TaskStatus status, String comment) {
        this(id, taskName, status, comment, null);
    }

    /**
     * Constructs a TaskDTO with an optional grade.
     *
     * @param id       the task's database ID
     * @param taskName the task name
     * @param status   the current task status
     * @param comment  optional comment (never {@code null}; use empty string)
     * @param grade    the grade for this task, or {@code null} if not graded
     */
    public TaskDTO(long id, String taskName, TaskStatus status, String comment, Integer grade) {
        this.id = id;
        this.taskName = taskName;
        this.status = status;
        this.comment = comment;
        this.grade = grade;
    }

    /** @return the task's database ID */
    public long getId() { return id; }

    /** @return the task name */
    public String getTaskName() { return taskName; }

    /** @return the current task status */
    public TaskStatus getStatus() { return status; }

    /** @return the task comment (never {@code null}) */
    public String getComment() { return comment; }

    /** @return the grade for this task, or {@code null} if not yet graded */
    public Integer getGrade() { return grade; }
}
