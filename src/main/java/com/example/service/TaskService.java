package com.example.service;

import com.example.dto.CreateUpdateTaskDTO;
import com.example.dto.TaskDTO;
import com.example.exception.HRAppException;
import com.example.model.Task;
import com.example.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for task operations.
 * <p>
 * Keeps task business logic separate from team member concerns.
 * The service does not access the database directly — all DB calls
 * go through the injected {@link TaskRepository}.
 */
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepo;

    /**
     * Constructs the service with the required repository (constructor injection).
     *
     * @param taskRepo repository for task data
     */
    public TaskService(TaskRepository taskRepo) {
        this.taskRepo = taskRepo;
    }

    /**
     * Returns all non-deleted tasks for the given member.
     *
     * @param memberId the owning member's database ID
     * @return list of task DTOs
     * @throws HRAppException on database error
     */
    public List<TaskDTO> getTasksForMember(long memberId) {
        try {
            return taskRepo.findByMemberId(memberId).stream()
                    .map(t -> new TaskDTO(t.getId(), t.getTaskName(), t.getStatus(), t.getComment()))
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            log.error("Failed to load tasks for member id={}", memberId, e);
            throw new HRAppException("Failed to load tasks.", e);
        }
    }

    /**
     * Creates a new task for the given member and returns the persisted task DTO.
     *
     * @param memberId the owning member's database ID
     * @param dto      validated input DTO
     * @return the created task as a DTO (with generated id)
     * @throws HRAppException on database error
     */
    public TaskDTO addTask(long memberId, CreateUpdateTaskDTO dto) {
        try {
            Task task = new Task(dto.getTaskName());
            task.setComment(dto.getComment());
            task.setStatus(dto.getStatus());
            taskRepo.save(task, memberId);
            log.info("Added task '{}' to member id={}", dto.getTaskName(), memberId);
            return new TaskDTO(task.getId(), task.getTaskName(), task.getStatus(), task.getComment());
        } catch (SQLException e) {
            log.error("Failed to add task for member id={}", memberId, e);
            throw new HRAppException("Failed to add task.", e);
        }
    }

    /**
     * Updates a task's name, comment, and status, and returns the refreshed DTO.
     *
     * @param taskId the task's database ID
     * @param dto    validated input DTO with new values
     * @return the updated task as a DTO
     * @throws HRAppException on database error
     */
    public TaskDTO updateTask(long taskId, CreateUpdateTaskDTO dto) {
        try {
            Task task = new Task(dto.getTaskName());
            task.setId(taskId);
            task.setComment(dto.getComment());
            task.setStatus(dto.getStatus());
            taskRepo.update(task);
            log.info("Updated task id={}", taskId);
            return new TaskDTO(task.getId(), task.getTaskName(), task.getStatus(), task.getComment());
        } catch (SQLException e) {
            log.error("Failed to update task id={}", taskId, e);
            throw new HRAppException("Failed to update task.", e);
        }
    }

    /**
     * Soft-deletes a task.
     *
     * @param taskId the task's database ID
     * @throws HRAppException on database error
     */
    public void deleteTask(long taskId) {
        try {
            taskRepo.softDelete(taskId);
            log.info("Deleted task id={}", taskId);
        } catch (SQLException e) {
            log.error("Failed to delete task id={}", taskId, e);
            throw new HRAppException("Failed to delete task.", e);
        }
    }
}
