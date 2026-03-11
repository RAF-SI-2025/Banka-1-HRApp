package com.example.dto;

import com.example.model.TaskStatus;

/**
 * Read-only data transfer object representing a single task with its grade.
 * Used to pass task data from the service layer to the view layer.
 */
public class TaskDTO {

    private final long id;
    private final String taskName;
    private final TaskStatus status;
    private final String comment;
    private final int grade;

    /**
     * Constructs a TaskDTO.
     *
     * @param id       the task's database ID
     * @param taskName the task name
     * @param status   the current task status
     * @param comment  optional comment
     * @param grade    the grade (0 means no grade has been assigned)
     */
    public TaskDTO(long id, String taskName, TaskStatus status, String comment, int grade) {
        this.id       = id;
        this.taskName = taskName;
        this.status   = status;
        this.comment  = comment;
        this.grade    = grade;
    }

    /** @return the task's database ID */
    public long getId() { return id; }

    /** @return the task name */
    public String getTaskName() { return taskName; }

    /** @return the current task status */
    public TaskStatus getStatus() { return status; }

    /** @return the task comment */
    public String getComment() { return comment; }

    /** @return the grade, or 0 if no grade has been assigned */
    public int getGrade() { return grade; }
}
